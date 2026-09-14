package com.cosmocraft.trading_cells.platform.neoforge.mobfarm;

import com.cosmocraft.trading_cells.feature.configuredmobfarm.adapters.input.ConfiguredMobFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.configuredmobfarm.adapters.input.ConfiguredMobFarmTargetCatalog;
import com.cosmocraft.trading_cells.feature.creeperfarm.adapters.input.CreeperFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.creeperfarm.adapters.input.CreeperFarmTargetCatalog;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EntityEssenceData;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.MobFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.raiderfarm.adapters.input.RaiderFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.raiderfarm.adapters.input.RaiderFarmTargetCatalog;
import com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.input.SkeletonFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.input.SkeletonFarmTargetCatalog;
import com.cosmocraft.trading_cells.feature.zombiefarm.adapters.input.ZombieFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.zombiefarm.adapters.input.ZombieFarmTargetCatalog;
import com.cosmocraft.trading_cells.platform.neoforge.machine.AbstractPortableMachineBlock;
import com.cosmocraft.trading_cells.platform.neoforge.machine.PortableMachineBlockEntity;
import com.cosmocraft.trading_cells.shared.mobfarm.domain.model.MobFarmCycleRules;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.ToIntFunction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.jspecify.annotations.Nullable;

/** One-time conversion using the legacy adapters' interpretation of targets and filters. */
public final class LegacyMobFarmMigration {
    private LegacyMobFarmMigration() { }

    public static boolean convert(PortableMachineBlockEntity legacy) {
        if (!(legacy.getLevel() instanceof ServerLevel level) || legacy.isRemoved()
                || level.getBlockEntity(legacy.getBlockPos()) != legacy) { return false; }
        Source source = source(legacy);
        if (source == null || !(legacy instanceof Container inventory)) { return false; }
        var target = MobFarmLootTables.createTarget(level, source.entityType());
        if (target == null) { return false; }
        if (source.charged()) {
            CompoundTag state = new CompoundTag();
            state.putBoolean("powered", true);
            target.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), state));
        }
        ItemStack module = EntityEssenceData.moduleOf(EntityEssenceData.essenceOf(target));
        if (module.isEmpty()) { return false; }

        CompoundTag original = legacy.saveCustomOnly(level.registryAccess());
        CompoundTag converted = original.copy();
        for (int slot = 2; slot < MobFarmBlockEntity.CONTAINER_SIZE; slot++) { converted.remove("Slot" + slot); }
        for (int slot = 2; slot < inventory.getContainerSize(); slot++) {
            var value = original.get("Slot" + slot);
            if (value != null) { converted.put("Slot" + (slot + 3), value.copy()); }
        }
        var moduleOutput = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        moduleOutput.store("Slot2", ItemStack.CODEC, module);
        converted.merge(moduleOutput.buildResult());

        var disabled = new TreeSet<>(source.disabled());
        int mask = original.getIntOr("EnabledLootMask", -1);
        // Resolve category-wide filters once, not on subsequent production ticks.
        for (Item item : BuiltInRegistries.ITEM) {
            int bit = source.categoryBit().applyAsInt(item);
            if (bit != 0 && (mask & bit) == 0) { disabled.add(BuiltInRegistries.ITEM.getKey(item)); }
        }
        converted.putInt("DisabledLootCount", disabled.size());
        int index = 0;
        for (Identifier id : disabled) { converted.putString("DisabledLoot" + index++, id.toString()); }

        boolean pending = original.getBooleanOr("PendingLootReady", original.getIntOr("PendingLootCount", 0) > 0);
        if (pending || original.getIntOr("CycleTicks", 0) > 0) {
            var weapon = MobFarmWeaponSnapshot.inspect(inventory.getItem(1), level, target.getType());
            converted.putInt("LegacyCycleKills", MobFarmCycleRules.simulatedKills(weapon.sweepingEdgeLevel()));
            converted.putBoolean("LegacyPendingReady", pending);
        }
        converted.putString("LegacyFarmId", BuiltInRegistries.BLOCK.getKey(legacy.getBlockState().getBlock()).toString());
        var state = MobFarmRegistrationAdapter.BLOCK.get().defaultBlockState().setValue(AbstractPortableMachineBlock.FACING,
                legacy.getBlockState().getValue(AbstractPortableMachineBlock.FACING));
        var replacement = new MobFarmBlockEntity(legacy.getBlockPos(), state);
        replacement.setLevel(level);
        replacement.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), converted));

        // Prepare and validate the entire replacement before touching the world.
        if (!level.setBlock(legacy.getBlockPos(), state, Block.UPDATE_CLIENTS)) { return false; }
        level.setBlockEntity(replacement);
        replacement.setChanged();
        level.sendBlockUpdated(replacement.getBlockPos(), state, state, Block.UPDATE_CLIENTS);
        level.updateNeighborsAt(replacement.getBlockPos(), state.getBlock());
        for (var player : level.players()) {
            if (!player.containerMenu.stillValid(player)) { player.closeContainer(); }
        }
        return true;
    }

    private static @Nullable Source source(PortableMachineBlockEntity legacy) {
        return switch (legacy) {
            case SkeletonFarmBlockEntity farm -> new Source(farm.selectedTargetId(), false, farm.disabledDynamicLoot(), item -> {
                var category = SkeletonFarmTargetCatalog.category(item);
                return category == null ? 0 : category.bit();
            });
            case ZombieFarmBlockEntity farm -> new Source(farm.selectedTargetId(), false, farm.disabledDynamicLoot(), item -> {
                var category = ZombieFarmTargetCatalog.category(item);
                return category == null ? 0 : category.bit();
            });
            case RaiderFarmBlockEntity farm -> new Source(RaiderFarmTargetCatalog.entityTypeId(farm.selectedTargetId()),
                    false, farm.disabledDynamicLoot(), item -> {
                        var category = RaiderFarmTargetCatalog.category(item);
                        return category == null ? 0 : category.bit();
                    });
            case CreeperFarmBlockEntity farm -> new Source(CreeperFarmTargetCatalog.entityTypeId(farm.selectedTargetId()),
                    CreeperFarmTargetCatalog.CHARGED_CREEPER_ID.equals(farm.selectedTargetId()), farm.disabledDynamicLoot(), item -> {
                        var category = CreeperFarmTargetCatalog.category(item);
                        return category == null ? 0 : category.bit();
                    });
            case ConfiguredMobFarmBlockEntity farm -> new Source(ConfiguredMobFarmTargetCatalog.entityTypeId(farm.selectedTargetId()),
                    false, farm.disabledDynamicLoot(), item -> 0);
            default -> null;
        };
    }

    private record Source(Identifier entityType, boolean charged, Set<Identifier> disabled, ToIntFunction<Item> categoryBit) { }
}
