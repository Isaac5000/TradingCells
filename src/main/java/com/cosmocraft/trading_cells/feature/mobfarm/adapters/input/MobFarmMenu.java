package com.cosmocraft.trading_cells.feature.mobfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.mobfarm.MobFarmCatalog;
import com.cosmocraft.trading_cells.platform.neoforge.mobfarm.MobFarmLootTables;
import com.cosmocraft.trading_cells.platform.neoforge.mobfarm.MobFarmSimulationLoot;
import com.cosmocraft.trading_cells.platform.neoforge.mobfarm.MobFarmSwordTierCatalog;
import com.cosmocraft.trading_cells.platform.neoforge.network.MobSimulationLootPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public final class MobFarmMenu extends AbstractContainerMenu {
    public static final int WIDTH = 374;
    public static final int HEIGHT = 246;
    public static final int PLAYER_X = 199;
    public static final int PLAYER_Y = 152;
    private final Container container;
    private final ContainerData data;
    private final Player owner;
    private List<LootEntry> loot = List.of();
    private int snapshotRevision;
    private int farmRevision = -1;
    private int catalogRevision = -1;
    private ItemStack previewSword = ItemStack.EMPTY;
    private ItemStack previewModule = ItemStack.EMPTY;
    private int previewKills = -1;
    private Map<Identifier, MobFarmLootTables.DropSummary> probabilities = Map.of();

    public record LootEntry(ItemStack stack, boolean enabled, int probability, int minimum, int maximum) { }

    public MobFarmMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(MobFarmBlockEntity.CONTAINER_SIZE), new SimpleContainerData(7));
    }

    public MobFarmMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(MobFarmRegistrationAdapter.MENU.get(), id);
        this.container = container;
        this.data = data;
        owner = inventory.player;
        checkContainerSize(container, MobFarmBlockEntity.CONTAINER_SIZE);
        checkContainerDataCount(data, 7);
        for (int index = 0; index < 5; index++) {
            final int input = index;
            addSlot(new Slot(container, index, 199 + index * 36, 35) {
                @Override public int getMaxStackSize() { return 1; }
                @Override public boolean mayPlace(ItemStack stack) {
                    return switch (input) {
                        case 0 -> CapturedMobStackAdapter.isFilledCapturer(CapturedMobKind.VILLAGER, stack)
                                && !CapturedMobStackAdapter.isBaby(CapturedMobKind.VILLAGER, stack);
                        case 1 -> MobFarmSwordTierCatalog.isSupported(stack);
                        case 2 -> stack.is(MobFarmRegistrationAdapter.ENTITY_MODULE.get()) && EntityEssenceData.entityTypeId(stack) != null;
                        case 3 -> MobFarmUpgradeItem.accepts(stack, MobFarmUpgradeItem.Kind.SPEED);
                        case 4 -> MobFarmUpgradeItem.accepts(stack, MobFarmUpgradeItem.Kind.CAPACITY);
                        default -> false;
                    };
                }
            });
        }
        for (int index = 0; index < 18; index++) {
            addSlot(new Slot(container, index + 5, 199 + (index % 9) * 18, 91 + (index / 9) * 18) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
        }
        addStandardInventorySlots(inventory, PLAYER_X, PLAYER_Y);
        addDataSlots(data);
    }

    public int cycleTicks() { return data.get(0); }
    public int cycleDurationTicks() { return data.get(1); }
    public int storedExperience() { return data.get(2); }
    public int simulatedKills() { return data.get(3); }
    public boolean enabled() { return data.get(4) != 0; }
    public boolean pendingOutput() { return data.get(6) != 0; }
    public ItemStack creature() { return container.getItem(2); }
    public List<LootEntry> lootEntries() { return loot; }
    public int lootRevision() { return snapshotRevision; }

    @Override public void broadcastChanges() {
        // Slot changes can reach the menu before the block's next server tick.
        if (container instanceof MobFarmBlockEntity farm) { farm.refreshInputs(); }
        super.broadcastChanges();
        if (!(container instanceof MobFarmBlockEntity farm) || !(owner instanceof ServerPlayer player)
                || !(farm.getLevel() instanceof ServerLevel level)) { return; }
        ItemStack sword = container.getItem(1);
        boolean previewChanged = !ItemStack.matches(sword, previewSword) || !ItemStack.matches(creature(), previewModule)
                || previewKills != simulatedKills() || catalogRevision != MobFarmCatalog.revision();
        if (!previewChanged && farmRevision == farm.lootRevision()) { return; }
        if (previewChanged) {
            previewSword = sword.copy();
            previewModule = creature().copy();
            previewKills = simulatedKills();
            catalogRevision = MobFarmCatalog.revision();
            probabilities = farm.simulationTarget() == null ? Map.of()
                    : MobFarmSimulationLoot.preview(level, farm.simulationTarget(), sword, simulatedKills());
            farm.includePreviewLoot(probabilities.keySet());
        }
        farmRevision = farm.lootRevision();
        var ids = new java.util.TreeSet<>(farm.lootItems());
        ids.addAll(probabilities.keySet());
        List<LootEntry> next = new ArrayList<>();
        for (Identifier id : ids) {
            if (next.size() >= 2_048) { break; }
            BuiltInRegistries.ITEM.getOptional(id).ifPresent(item -> {
                var drop = probabilities.getOrDefault(id, new MobFarmLootTables.DropSummary(-1, 0, 0));
                if (drop.probability() == 0 && drop.maximum() == 0) { return; }
                next.add(new LootEntry(new ItemStack(item), farm.lootEnabled(id), drop.probability(), drop.minimum(), drop.maximum()));
            });
        }
        loot = List.copyOf(next);
        snapshotRevision++;
        if (player.connection != null) {
            PacketDistributor.sendToPlayer(player, new MobSimulationLootPayload(containerId, snapshotRevision, loot));
        }
    }

    public void applyLootSnapshot(int revision, List<LootEntry> entries) {
        if (owner instanceof ServerPlayer) { return; }
        snapshotRevision = revision;
        loot = List.copyOf(entries);
    }

    public void toggleLoot(Player player, int revision, Identifier item) {
        if (player != owner || !stillValid(player) || revision != snapshotRevision
                || !(container instanceof MobFarmBlockEntity farm)
                || loot.stream().noneMatch(entry -> BuiltInRegistries.ITEM.getKey(entry.stack().getItem()).equals(item))) { return; }
        farm.toggleLoot(item);
        broadcastChanges();
    }

    @Override public boolean stillValid(Player player) { return container.stillValid(player); }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (player != owner || !(player instanceof ServerPlayer server)
                || !(container instanceof MobFarmBlockEntity farm) || !stillValid(player)) { return false; }
        if (button == 0) { farm.toggleEnabled(); return true; }
        if (button == 1) { farm.extractExperience(server); return true; }
        return false;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) { return ItemStack.EMPTY; }
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) { return ItemStack.EMPTY; }
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        if (index < 23 ? !moveItemStackTo(stack, 23, 59, true) : !moveItemStackTo(stack, 0, 5, false)) { return ItemStack.EMPTY; }
        if (stack.isEmpty()) { slot.setByPlayer(ItemStack.EMPTY); } else { slot.setChanged(); }
        slot.onTake(player, stack);
        return before;
    }
}
