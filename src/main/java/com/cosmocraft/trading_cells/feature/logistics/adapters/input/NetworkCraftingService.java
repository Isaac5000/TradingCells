package com.cosmocraft.trading_cells.feature.logistics.adapters.input;

import com.cosmocraft.trading_cells.feature.logistics.adapters.neoforge.ItemLogisticsAdapter;
import com.cosmocraft.trading_cells.feature.logistics.adapters.neoforge.LogisticsComponentData;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.LogisticsResourceType;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeFilterRule;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeSideMode;
import java.util.IdentityHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.SimpleContainer;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.PlayerInventoryWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** The whole requested batch, including container remainders, shares one transaction. */
public final class NetworkCraftingService {
    private static final ItemLogisticsAdapter ITEMS = new ItemLogisticsAdapter();

    private NetworkCraftingService() {
    }

    public static ItemStack preview(ServerLevel level, List<ItemStack> grid) {
        CraftingInput input = CraftingInput.of(3, 3, grid);
        return level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level)
                .map(holder -> holder.value().assemble(input)).orElse(ItemStack.EMPTY);
    }

    public static boolean craft(
            ServerLevel level, BlockPos origin, ServerPlayer player, List<ItemStack> grid, int count
    ) {
        return craft(level, origin, player, grid, count, PlayerInventoryWrapper.of(player));
    }

    public static boolean craft(
            ServerLevel level, BlockPos origin, ServerPlayer player, List<ItemStack> grid, int count,
            ResourceHandler<ItemResource> outputTarget
    ) {
        if (grid.size() != 9 || count < 1 || count > 64) {
            return false;
        }
        CraftingInput input = CraftingInput.of(3, 3, grid);
        var recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level).orElse(null);
        if (recipe == null) {
            return false;
        }
        LogisticsNetworkManager network = LogisticsNetworkManager.get(level);
        var sources = network.terminalEndpoints(origin, LogisticsResourceType.ITEM, ITEMS.id());
        var destinations = sources;
        ResourceHandler<ItemResource> inventory = PlayerInventoryWrapper.of(player);
        List<ItemStack> crafted = new ArrayList<>(count);
        var previousCraftingPlayer = CommonHooks.getCraftingPlayer();
        CommonHooks.setCraftingPlayer(player);
        try (Transaction transaction = Transaction.openRoot()) {
            for (int craft = 0; craft < count; craft++) {
                ItemStack output = recipe.value().assemble(input);
                if (output.isEmpty()) {
                    return false;
                }
                for (ItemStack ingredient : input.items()) {
                    if (!ingredient.isEmpty() && move(level, network, origin, sources,
                            ItemResource.of(ingredient), 1, false, transaction) != 1) {
                        return false;
                    }
                }
                for (ItemStack remainder : recipe.value().getRemainingItems(input)) {
                    if (remainder.isEmpty()) {
                        continue;
                    }
                    ItemResource resource = ItemResource.of(remainder);
                    int remaining = remainder.getCount() - move(
                            level, network, origin, destinations, resource, remainder.getCount(), true, transaction);
                    if (remaining > 0 && inventory.insert(resource, remaining, transaction) != remaining) {
                        return false;
                    }
                }
                if (outputTarget.insert(ItemResource.of(output), output.getCount(), transaction) != output.getCount()) {
                    return false;
                }
                crafted.add(output.copy());
            }
            transaction.commit();
        } catch (RuntimeException exception) {
            return false;
        } finally {
            CommonHooks.setCraftingPlayer(previousCraftingPlayer);
        }
        // Irreversible notifications happen only after all inputs, outputs and remainders commit.
        for (ItemStack output : crafted) {
            try {
                output.onCraftedBy(player, output.getCount());
                EventHooks.firePlayerCraftingEvent(player, output,
                        new SimpleContainer(grid.stream().map(ItemStack::copy).toArray(ItemStack[]::new)));
                player.triggerRecipeCrafted(recipe, input.items());
            } catch (RuntimeException exception) {
                com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells.LOGGER.warn(
                        "Crafting notification failed after committed network recipe {}", recipe.id(), exception);
            }
        }
        return true;
    }

    private static int move(
            ServerLevel level, LogisticsNetworkManager network, BlockPos origin,
            List<LogisticsNetworkManager.NetworkEndpoint> endpoints, ItemResource resource,
            int amount, boolean insert, TransactionContext transaction
    ) {
        int moved = 0;
        IdentityHashMap<ResourceHandler<ItemResource>, Boolean> seen = new IdentityHashMap<>();
        var order = network.orderedForTerminal(origin, ITEMS.id(),
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(resource.getItem()), endpoints, insert ? -2 : -1);
        String fingerprint = LogisticsComponentData.fingerprint(resource.getComponentsPatch());
        for (var endpoint : order.endpoints()) {
            if (moved == amount) {
                break;
            }
            boolean allowed = endpoint.configuration().effectiveProfile(LogisticsResourceType.ITEM)
                    .allows(rule -> ITEMS.matches(resource, fingerprint, rule));
            if (!allowed) {
                continue;
            }
            var handler = LogisticsNetworkManager.findHandler(ITEMS, level, endpoint.targetPos(), endpoint.targetSide());
            if (handler == null || seen.put(handler, Boolean.TRUE) != null) {
                continue;
            }
            if (!network.takeOperation()) {
                return moved;
            }
            int transferred = insert ? handler.insert(resource, amount - moved, transaction)
                    : handler.extract(resource, amount - moved, transaction);
            if (transferred < 0 || transferred > amount - moved) {
                throw new IllegalStateException("External crafting provider returned an invalid amount");
            }
            moved += transferred;
            if (transferred > 0) {
                order.transferred(endpoint, transaction);
            }
        }
        return moved;
    }
}
