package com.cosmocraft.trading_cells.performance;

import com.cosmocraft.trading_cells.feature.infusion.adapters.input.ArcaneInfuserBlockEntity;
import com.cosmocraft.trading_cells.feature.infusion.adapters.minecraft.ArcaneInfusionRecipe;
import com.cosmocraft.trading_cells.feature.infusion.adapters.output.ArcaneInfuserRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.infusion.adapters.output.client.ArcaneInfuserScreen;
import com.cosmocraft.trading_cells.feature.infusion.domain.model.ArcaneInfusionTransferAction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Native infuser output/ghost tooltips and four placed-model orientations. */
final class InfusionUiFixture {
    private static volatile boolean prepared;
    private static volatile RecipeDisplay display;
    private static boolean listening;
    private static boolean outputRendered;
    private static boolean ghostRendered;
    private static int phase;
    private static long started;

    private InfusionUiFixture() { }
    private static String mode() { return System.getProperty("trading_cells.performance.client.uiFixture", ""); }
    static boolean modelScene() { return mode().equals("infusion-models"); }
    static boolean active() { return mode().equals("infusion") || modelScene(); }
    static boolean ready() { return prepared && (modelScene() || ghostRendered); }

    static void prepare(ServerPlayer player, BlockPos pos) {
        var level = player.level();
        player.getInventory().clearContent();
        if (modelScene()) {
            for (BlockPos target : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(6, 4, 9))) {
                level.setBlockAndUpdate(target, target.getY() == pos.getY() - 1
                        ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
            }
            level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    net.minecraft.world.phys.AABB.encapsulatingFullBlocks(pos.offset(-3, -2, -3), pos.offset(7, 5, 10)))
                    .forEach(net.minecraft.world.entity.item.ItemEntity::discard);
            var facings = new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
            for (int index = 0; index < facings.length; index++) {
                level.setBlockAndUpdate(pos.offset(index % 2 * 2, 0, index / 2 * 2),
                        ArcaneInfuserRegistrationAdapter.BLOCK.get().defaultBlockState()
                                .setValue(BlockStateProperties.HORIZONTAL_FACING, facings[index]));
            }
            player.getInventory().setItem(0, ArcaneInfuserRegistrationAdapter.ITEM.get().getDefaultInstance());
            player.getInventory().setSelectedSlot(0);
            player.inventoryMenu.broadcastChanges();
            prepared = true;
            return;
        }
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos, ArcaneInfuserRegistrationAdapter.BLOCK.get().defaultBlockState());
        var infuser = (ArcaneInfuserBlockEntity) level.getBlockEntity(pos);
        var silk = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
        var ingredients = new ItemStack[]{Items.ECHO_SHARD.getDefaultInstance(), Items.AMETHYST_SHARD.getDefaultInstance(),
                Items.ECHO_SHARD.getDefaultInstance(), Items.TURTLE_EGG.getDefaultInstance(),
                EnchantmentHelper.createBook(new EnchantmentInstance(silk, 1)), Items.TURTLE_EGG.getDefaultInstance(),
                Items.ECHO_SHARD.getDefaultInstance(), Items.NETHER_STAR.getDefaultInstance(), Items.ECHO_SHARD.getDefaultInstance()};
        for (int slot = 0; slot < ingredients.length; slot++) { infuser.setItem(slot, ingredients[slot]); }
        player.experienceLevel = 0;
        player.experienceProgress = 0;
        player.totalExperience = 0;
        player.giveExperiencePoints(75_000);
        infuser.transferExperience(player, ArcaneInfusionTransferAction.DEPOSIT_ALL, 0);
        var recipe = (ArcaneInfusionRecipe) level.getServer().getRecipeManager().getRecipes().stream()
                .filter(holder -> holder.id().identifier().equals(Identifier.parse("trading_cells:silk_touch_two_infusion")))
                .findFirst().orElseThrow().value();
        display = recipe.display().getFirst();
        require(infuser.storedExperience() == 75_000 && infuser.visibleResult().is(Items.ENCHANTED_BOOK), "Infuser setup failed");
        prepared = true;
    }

    static void inspect(Minecraft minecraft) {
        if (!prepared || ready() || !(minecraft.gui.screen() instanceof ArcaneInfuserScreen screen)) { return; }
        if (started == 0) { started = System.nanoTime(); }
        require(System.nanoTime() - started < 30_000_000_000L, "Infuser tooltip verification timed out at phase " + phase);
        if (!listening) {
            minecraft.options.advancedItemTooltips = false;
            NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, InfusionUiFixture::onRender);
            NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, InfusionUiFixture::onTooltip);
            listening = true;
        }
        var menu = screen.getMenu();
        var output = menu.getSlot(ArcaneInfuserBlockEntity.OUTPUT_SLOT);
        if (phase == 0 && outputRendered) {
            var click = new MouseButtonEvent(screen.getLeftPos() + output.x + 8, screen.getTopPos() + output.y + 8,
                    new MouseButtonInfo(0, 1));
            screen.mouseClicked(click, false);
            screen.mouseReleased(click);
            phase = 1;
        } else if (phase == 1 && output.getItem().isEmpty()) {
            ItemStack crafted = ItemStack.EMPTY;
            for (int slot = 0; slot < 36; slot++) {
                var candidate = minecraft.player.getInventory().getItem(slot);
                if (candidate.is(Items.ENCHANTED_BOOK)) { crafted = candidate; break; }
            }
            if (crafted.isEmpty()) { return; }
            verifyBook(crafted);
            // Inventory prediction can precede the server's XP synchronization packet.
            if (menu.storedExperience() != 0) { return; }
            screen.fillGhostRecipe(display);
            phase = 2;
        }
    }

    private static void onRender(ScreenEvent.Render.Pre event) {
        if (!(event.getScreen() instanceof ArcaneInfuserScreen screen)) { return; }
        var output = screen.getMenu().getSlot(ArcaneInfuserBlockEntity.OUTPUT_SLOT);
        // Feed a reproducible hover position to the unchanged production screen.
        event.setCanceled(true);
        screen.extractRenderStateWithTooltipAndSubtitles(event.getGuiGraphics(),
                screen.getLeftPos() + output.x + 8, screen.getTopPos() + output.y + 8, event.getPartialTick());
    }

    private static void onTooltip(RenderTooltipEvent.GatherComponents event) {
        if (!(Minecraft.getInstance().gui.screen() instanceof ArcaneInfuserScreen)
                || !event.getItemStack().is(Items.ENCHANTED_BOOK) || phase == 1) { return; }
        verifyBook(event.getItemStack());
        var lines = event.getTooltipElements().stream().flatMap(element -> element.left().stream())
                .map(FormattedText::getString).toList();
        var expected = Screen.getTooltipFromItem(Minecraft.getInstance(), event.getItemStack());
        require(lines.getFirst().equals(Items.ENCHANTED_BOOK.getDefaultInstance().getHoverName().getString()), "Wrong book title: " + lines);
        String enchantment = expected.get(1).getString();
        require(lines.stream().filter(enchantment::equals).count() == 1, "Enchantment detail missing or repeated: " + lines);
        if (phase == 0 && !outputRendered) {
            System.out.println("Infuser crafted-output tooltip: " + lines);
            outputRendered = true;
        } else if (phase == 2 && !ghostRendered) {
            System.out.println("Infuser ghost-recipe tooltip: " + lines + "; crafted book and 75000 XP consumption verified");
            ghostRendered = true;
        }
    }

    private static void verifyBook(ItemStack stack) {
        var silk = Minecraft.getInstance().level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
        require(!stack.has(DataComponents.CUSTOM_NAME), "Enchantment replaced the book name");
        require(ItemStack.isSameItemSameComponents(stack, EnchantmentHelper.createBook(new EnchantmentInstance(silk, 2))),
                "Infuser result differs from a vanilla Silk Touch II book");
    }

    private static void require(boolean condition, String message) {
        if (!condition) { throw new IllegalStateException(message); }
    }
}
