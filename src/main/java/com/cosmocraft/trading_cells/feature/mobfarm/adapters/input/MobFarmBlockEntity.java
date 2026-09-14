package com.cosmocraft.trading_cells.feature.mobfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.mobfarm.domain.model.MobFarmRules;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.cosmocraft.trading_cells.platform.neoforge.experience.PlayerExperienceTransfer;
import com.cosmocraft.trading_cells.platform.neoforge.fluid.ExperienceFluidHandler;
import com.cosmocraft.trading_cells.platform.neoforge.fluid.ExperienceFluidHandlers;
import com.cosmocraft.trading_cells.platform.neoforge.machine.MachineInventoryDiagnostics;
import com.cosmocraft.trading_cells.platform.neoforge.mobfarm.MobFarmCatalog;
import com.cosmocraft.trading_cells.platform.neoforge.mobfarm.MobFarmSimulationLoot;
import com.cosmocraft.trading_cells.platform.neoforge.mobfarm.MobFarmSwordTierCatalog;
import com.cosmocraft.trading_cells.platform.neoforge.mobfarm.MobFarmWeaponSnapshot;
import com.cosmocraft.trading_cells.shared.machines.domain.model.MachineDiagnosticSnapshot;
import com.cosmocraft.trading_cells.shared.machines.domain.model.MachineDiagnosticStatus;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class MobFarmBlockEntity extends SimulationInventoryBlockEntity implements MenuProvider {
    public static final int WORKER_SLOT = 0;
    public static final int SWORD_SLOT = 1;
    public static final int MODULE_SLOT = 2;
    public static final int SPEED_SLOT = 3;
    public static final int CAPACITY_SLOT = 4;
    public static final int FIRST_OUTPUT_SLOT = 5;
    public static final int OUTPUT_SLOT_COUNT = 18;
    public static final int CONTAINER_SIZE = FIRST_OUTPUT_SLOT + OUTPUT_SLOT_COUNT;
    private static final int MAX_PENDING_STACKS = 4_096;
    private static final int MAX_OBSERVED_LOOT = 2_048;
    private final Set<Identifier> disabledLoot = new HashSet<>();
    private final Set<Identifier> knownLoot = new LinkedHashSet<>();
    private final Set<Identifier> observedLoot = new LinkedHashSet<>();
    private ItemStack lootModule = ItemStack.EMPTY;
    private final List<ItemStack> pendingLoot = new ArrayList<>();
    private int cycleTicks;
    private int cycleDuration = 1_200;
    private int storedExperience;
    private int kills = 1;
    private int catalogRevision = -1;
    private int lootRevision;
    private int legacyCycleKills;
    private boolean legacyPendingReady;
    private boolean enabled = true;
    private boolean inputsDirty = true;
    private boolean adultWorker;
    private boolean hunting;
    private boolean lootFailure;
    private @Nullable LivingEntity target;
    private @Nullable MobFarmWeaponSnapshot weapon;
    private final ExperienceFluidHandler experience = ExperienceFluidHandlers.source(
            () -> storedExperience, value -> storedExperience = Math.max(0, value), this::markChangedAndSync);
    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> cycleTicks;
                case 1 -> cycleDuration;
                case 2 -> storedExperience;
                case 3 -> kills;
                case 4 -> enabled ? 1 : 0;
                case 5 -> isHunting() ? 1 : 0;
                case 6 -> pendingLoot.isEmpty() ? 0 : 1;
                default -> 0;
            };
        }
        @Override public void set(int index, int value) { }
        @Override public int getCount() { return 7; }
    };

    public MobFarmBlockEntity(BlockPos pos, BlockState state) {
        super(MobFarmRegistrationAdapter.BLOCK_ENTITY.get(), pos, state, CONTAINER_SIZE, FIRST_OUTPUT_SLOT);
    }

    public ContainerData dataAccess() { return data; }
    public ItemStack worker() { return items.get(WORKER_SLOT); }
    public ItemStack creature() { return items.get(MODULE_SLOT); }
    public int cycleTicks() { return cycleTicks; }
    public int cycleDurationTicks() { return cycleDuration; }
    public int storedExperience() { return storedExperience; }
    public int simulatedKills() { return kills; }
    public boolean enabled() { return enabled; }
    public boolean isHunting() { return hunting && enabled && !isPausedByRedstone(); }
    public ExperienceFluidHandler experienceFluidHandler() { return experience; }
    public int lootRevision() { return lootRevision; }
    public boolean lootEnabled(Identifier id) { return !disabledLoot.contains(id); }
    public List<Identifier> lootItems() { return knownLoot.stream().sorted().toList(); }
    public @Nullable LivingEntity simulationTarget() { return target; }

    public void rememberLoot(Iterable<Identifier> ids) {
        for (Identifier id : ids) {
            if (!BuiltInRegistries.ITEM.containsKey(id)) { continue; }
            if (knownLoot.add(id)) { lootRevision++; }
            if (observedLoot.size() < MAX_OBSERVED_LOOT && observedLoot.add(id)) { setChanged(); }
        }
    }

    private void refreshLootModule() {
        if (ItemStack.matches(lootModule, creature())) { return; }
        lootModule = creature().copy();
        observedLoot.clear();
        knownLoot.clear();
    }

    @Override public Component getDisplayName() { return Component.translatable("block.trading_cells.mob_farm"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        refreshInputs();
        return new MobFarmMenu(id, inventory, this, data);
    }

    public void toggleEnabled() { enabled = !enabled; hunting = false; markChangedAndSync(); }
    public void toggleLoot(Identifier id) {
        if (!knownLoot.contains(id)) { return; }
        if (!disabledLoot.remove(id)) { disabledLoot.add(id); }
        lootRevision++;
        markChangedAndSync();
    }
    public void extractExperience(ServerPlayer player) {
        if (!stillValid(player)) { return; }
        storedExperience -= PlayerExperienceTransfer.addPoints(player, storedExperience);
        markChangedAndSync();
    }

    @Override protected boolean acceptsInput(int slot, ItemStack stack) {
        return switch (slot) {
            case WORKER_SLOT -> CapturedMobStackAdapter.isFilledCapturer(CapturedMobKind.VILLAGER, stack)
                    && !CapturedMobStackAdapter.isBaby(CapturedMobKind.VILLAGER, stack);
            case SWORD_SLOT -> MobFarmSwordTierCatalog.isSupported(stack);
            case MODULE_SLOT -> stack.is(MobFarmRegistrationAdapter.ENTITY_MODULE.get()) && EntityEssenceData.entityTypeId(stack) != null;
            case SPEED_SLOT -> MobFarmUpgradeItem.accepts(stack, MobFarmUpgradeItem.Kind.SPEED);
            case CAPACITY_SLOT -> MobFarmUpgradeItem.accepts(stack, MobFarmUpgradeItem.Kind.CAPACITY);
            default -> false;
        };
    }

    @Override protected void slotChanged(int slot) {
        if (slot < FIRST_OUTPUT_SLOT) {
            inputsDirty = true;
            lootFailure = false;
            hunting = false;
            if (slot <= MODULE_SLOT && !legacyPendingReady) {
                cycleTicks = 0;
                legacyCycleKills = 0;
            }
        }
    }

    @Override protected boolean canChangeInput(int slot) {
        return !legacyPendingReady || slot > MODULE_SLOT || items.get(slot).isEmpty();
    }

    private void refreshInputs() {
        if (!(level instanceof ServerLevel server)) { return; }
        int revision = MobFarmCatalog.revision();
        if (!inputsDirty && catalogRevision == revision) { return; }
        inputsDirty = false;
        catalogRevision = revision;
        lootFailure = false;
        adultWorker = acceptsInput(WORKER_SLOT, worker());
        target = EntityEssenceData.createEntity(server, creature());
        if (target != null) {
            target.setPos(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
            weapon = MobFarmWeaponSnapshot.inspect(items.get(SWORD_SLOT), server, target.getType());
        } else { weapon = null; }
        int duration = legacyCycleKills > 0 ? cycleDuration : MobFarmRules.cycleDuration(MobFarmUpgradeItem.tier(items.get(SPEED_SLOT), MobFarmUpgradeItem.Kind.SPEED),
                weapon == null ? 0 : weapon.tierPosition(), weapon == null ? 0 : weapon.effectiveDamageLevel(),
                weapon == null ? 0 : weapon.sweepingEdgeLevel());
        cycleTicks = MobFarmRules.rescaleProgress(cycleTicks, cycleDuration, duration);
        cycleDuration = duration;
        kills = legacyCycleKills > 0 ? legacyCycleKills
                : MobFarmRules.killsPerCycle(MobFarmUpgradeItem.tier(items.get(CAPACITY_SLOT), MobFarmUpgradeItem.Kind.CAPACITY));
        refreshLootModule();
        knownLoot.clear();
        if (target != null) { knownLoot.addAll(MobFarmSimulationLoot.filterItems(target, items.get(SWORD_SLOT))); }
        knownLoot.addAll(observedLoot);
        pendingLoot.forEach(stack -> knownLoot.add(BuiltInRegistries.ITEM.getKey(stack.getItem())));
        lootRevision++;
    }

    @Override public void processTick() {
        if (!(level instanceof ServerLevel server)) { return; }
        refreshInputs();
        hunting = false;
        if (!enabled || lootFailure) { return; }
        if (!pendingLoot.isEmpty() && !legacyPendingReady) {
            if (flushPendingLoot()) { markChangedAndSync(); }
            return;
        }
        if (!adultWorker || target == null || weapon == null || !weapon.supported()) { return; }
        hunting = true;
        if (++cycleTicks < cycleDuration) { setChanged(); return; }
        if (legacyPendingReady) {
            settleCycle(server);
            return;
        }
        List<ItemStack> rolled = new ArrayList<>();
        try {
            MobFarmSimulationLoot.roll(server, target, items.get(SWORD_SLOT), kills, (batch, stack) -> {
                if (stack.isEmpty()) { return; }
                if (rolled.size() >= MAX_PENDING_STACKS) { throw new IllegalStateException("Simulation loot limit exceeded"); }
                rolled.add(stack.copy());
            });
        } catch (RuntimeException | LinkageError failure) {
            lootFailure = true;
            hunting = false;
            cycleTicks = 0;
            TradingCells.LOGGER.warn("Mob simulation at {} failed for {}", worldPosition,
                    EntityEssenceData.entityTypeId(creature()), failure);
            markChangedAndSync();
            return;
        }
        for (ItemStack stack : rolled) {
            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            rememberLoot(List.of(id));
            if (lootEnabled(id)) { pendingLoot.add(stack); }
        }
        settleCycle(server);
    }

    private void settleCycle(ServerLevel server) {
        // Legacy queues were rolled but unpaid; new queues are paid before draining.
        storedExperience = (int) Math.min(Integer.MAX_VALUE, storedExperience + kills * 5L);
        if (!weapon.warriorsTouch()) {
            items.get(SWORD_SLOT).hurtAndBreak(1, server, (LivingEntity) null, ignored -> items.set(SWORD_SLOT, ItemStack.EMPTY));
            inputsDirty = true;
        }
        cycleTicks = 0;
        legacyCycleKills = 0;
        legacyPendingReady = false;
        inputsDirty = true;
        flushPendingLoot();
        markChangedAndSync();
    }

    private boolean flushPendingLoot() {
        boolean changed = false;
        for (ItemStack pending : pendingLoot) {
            for (int pass = 0; pass < 2 && !pending.isEmpty(); pass++) {
                for (int slot = FIRST_OUTPUT_SLOT; slot < CONTAINER_SIZE && !pending.isEmpty(); slot++) {
                    ItemStack existing = items.get(slot);
                    if (pass == 0 && !existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, pending)) {
                        int moved = Math.min(pending.getCount(), Math.max(0, existing.getMaxStackSize() - existing.getCount()));
                        existing.grow(moved);
                        pending.shrink(moved);
                        changed |= moved > 0;
                    } else if (pass == 1 && existing.isEmpty()) {
                        items.set(slot, pending.split(Math.min(pending.getCount(), pending.getMaxStackSize())));
                        changed = true;
                    }
                }
            }
        }
        pendingLoot.removeIf(ItemStack::isEmpty);
        return changed;
    }

    @Override public MachineDiagnosticSnapshot machineDiagnosticSnapshot() {
        var output = MachineInventoryDiagnostics.outputUsage(this, FIRST_OUTPUT_SLOT, OUTPUT_SLOT_COUNT);
        MachineDiagnosticStatus status = !enabled ? MachineDiagnosticStatus.PAUSED
                : !pendingLoot.isEmpty() ? MachineDiagnosticStatus.BLOCKED
                : !adultWorker || target == null || weapon == null || !weapon.supported() || lootFailure
                        ? MachineDiagnosticStatus.INACTIVE : MachineDiagnosticStatus.RUNNING;
        String reason = !enabled ? "manual" : !pendingLoot.isEmpty() ? "output_full"
                : !adultWorker ? "worker_required" : target == null ? "creature_required"
                : weapon == null || !weapon.supported() ? "tool_required" : lootFailure ? "loot_error" : MachineDiagnosticSnapshot.NONE;
        return applyRedstonePause(new MachineDiagnosticSnapshot(status, reason, cycleTicks, cycleDuration,
                storedExperience, output.used(), output.capacity()));
    }

    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        cycleDuration = Math.max(20, input.getIntOr("CycleDurationTicks", 1_200));
        cycleTicks = Math.clamp(input.getIntOr("CycleTicks", 0), 0, cycleDuration);
        storedExperience = Math.max(0, input.getIntOr("StoredExperience", 0));
        enabled = input.getBooleanOr("Enabled", true);
        hunting = input.getBooleanOr("Hunting", false);
        legacyCycleKills = Math.clamp(input.getIntOr("LegacyCycleKills", 0), 0, 256);
        legacyPendingReady = legacyCycleKills > 0 && input.getBooleanOr("LegacyPendingReady", false);
        pendingLoot.clear();
        for (int index = 0, count = Math.clamp(input.getIntOr("PendingLootCount", 0), 0, MAX_PENDING_STACKS); index < count; index++) {
            input.read("PendingLoot" + index, ItemStack.CODEC).filter(stack -> !stack.isEmpty()).ifPresent(pendingLoot::add);
        }
        disabledLoot.clear();
        for (int index = 0, count = Math.clamp(input.getIntOr("DisabledLootCount", 0), 0, 2_048); index < count; index++) {
            Identifier id = Identifier.tryParse(input.getStringOr("DisabledLoot" + index, ""));
            if (id != null) { disabledLoot.add(id); }
        }
        observedLoot.clear();
        for (int index = 0, count = Math.clamp(input.getIntOr("ObservedLootCount", 0), 0, MAX_OBSERVED_LOOT); index < count; index++) {
            Identifier id = Identifier.tryParse(input.getStringOr("ObservedLoot" + index, ""));
            if (id != null && BuiltInRegistries.ITEM.containsKey(id)) { observedLoot.add(id); }
        }
        lootModule = creature().copy();
        inputsDirty = true;
    }

    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        refreshLootModule();
        output.putInt("CycleTicks", cycleTicks);
        output.putInt("CycleDurationTicks", cycleDuration);
        output.putInt("StoredExperience", storedExperience);
        output.putBoolean("Enabled", enabled);
        output.putBoolean("Hunting", isHunting());
        if (legacyCycleKills > 0) {
            output.putInt("LegacyCycleKills", legacyCycleKills);
            output.putBoolean("LegacyPendingReady", legacyPendingReady);
        }
        output.putInt("PendingLootCount", pendingLoot.size());
        for (int index = 0; index < pendingLoot.size(); index++) { output.store("PendingLoot" + index, ItemStack.CODEC, pendingLoot.get(index)); }
        List<Identifier> disabled = disabledLoot.stream().sorted().toList();
        output.putInt("DisabledLootCount", disabled.size());
        for (int index = 0; index < disabled.size(); index++) { output.putString("DisabledLoot" + index, disabled.get(index).toString()); }
        List<Identifier> observed = observedLoot.stream().sorted().toList();
        output.putInt("ObservedLootCount", observed.size());
        for (int index = 0; index < observed.size(); index++) { output.putString("ObservedLoot" + index, observed.get(index).toString()); }
    }

    @Override protected void clearContentsForBlockDrop() {
        super.clearContentsForBlockDrop();
        pendingLoot.clear();
        observedLoot.clear();
        knownLoot.clear();
        lootModule = ItemStack.EMPTY;
        storedExperience = 0;
        cycleTicks = 0;
        hunting = false;
        target = null;
        weapon = null;
        inputsDirty = true;
        legacyCycleKills = 0;
        legacyPendingReady = false;
    }
}
