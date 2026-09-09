package com.cosmocraft.trading_cells.gametest.feature.machinecontrol;

import com.cosmocraft.trading_cells.feature.configuredmobfarm.adapters.input.ConfiguredMobFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.configuredmobfarm.adapters.input.ConfiguredMobFarmTargetCatalog;
import com.cosmocraft.trading_cells.feature.configuredmobfarm.adapters.output.ConfiguredMobFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.configuredmobfarm.domain.model.ConfiguredMobFarmKind;
import com.cosmocraft.trading_cells.gametest.shared.BlockEntityStateFixtures;
import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import com.cosmocraft.trading_cells.gametest.shared.GameTestFixtures;
import com.cosmocraft.trading_cells.platform.neoforge.machine.MachineConfigurationPort;
import com.cosmocraft.trading_cells.shared.machines.domain.model.MachineRedstoneMode;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/** Configuration-copy and redstone contracts shared by automatic machines. */
public final class MachineControlGameTests {
    private static final BlockPos SOURCE = new BlockPos(1, 1, 1);
    private static final BlockPos TARGET = new BlockPos(3, 1, 1);
    private static final BlockPos POWER = new BlockPos(3, 1, 2);
    private static final ConfiguredMobFarmKind KIND = ConfiguredMobFarmKind.AQUATIC;

    private MachineControlGameTests() {
    }

    public static List<GameTestCase> tests() {
        return List.of(
                new GameTestCase("machine_configurator_atomic_settings", 20,
                        MachineControlGameTests::atomicSettings),
                new GameTestCase("machine_redstone_pause_and_comparator", 20,
                        MachineControlGameTests::redstonePauseAndComparator)
        );
    }

    private static void atomicSettings(GameTestHelper helper) {
        helper.setBlock(SOURCE, ConfiguredMobFarmRegistrationAdapter.block(KIND).get());
        helper.setBlock(TARGET, ConfiguredMobFarmRegistrationAdapter.block(KIND).get());
        ConfiguredMobFarmBlockEntity source = farm(helper, SOURCE);
        ConfiguredMobFarmBlockEntity target = farm(helper, TARGET);
        Identifier selected = ConfiguredMobFarmTargetCatalog.targets(KIND).getLast().entityTypeId();
        source.selectTarget(selected);
        source.toggleEnabled();
        source.setRedstoneMode(MachineRedstoneMode.HIGH_SIGNAL_PAUSES);
        source.dataAccess().set(0, 41);

        ItemStack worker = GameTestFixtures.adultVillagerCapture(helper);
        target.setItem(ConfiguredMobFarmBlockEntity.WORKER_SLOT, worker.copy());
        target.setItem(ConfiguredMobFarmBlockEntity.SWORD_SLOT, new ItemStack(Items.IRON_SWORD));
        target.dataAccess().set(0, 17);
        BlockEntityStateFixtures.setInt(helper, target, "StoredExperience", 99);

        CompoundTag configuration = source.exportMachineConfiguration();
        helper.assertTrue(target.canApplyMachineConfiguration(
                MachineConfigurationPort.CONFIGURATION_SCHEMA_VERSION,
                configuration
        ), "A valid same-machine configuration was rejected");
        target.applyMachineConfiguration(MachineConfigurationPort.CONFIGURATION_SCHEMA_VERSION, configuration);
        helper.assertValueEqual(target.selectedTargetId(), selected, "Copied mob-farm target");
        helper.assertValueEqual(target.redstoneMode(), MachineRedstoneMode.HIGH_SIGNAL_PAUSES,
                "Copied redstone mode");
        helper.assertValueEqual(target.dataAccess().get(8), 0, "Copied enabled mode");
        helper.assertValueEqual(target.dataAccess().get(4), 99, "Configurator must not copy or clear XP");
        helper.assertTrue(target.getItem(ConfiguredMobFarmBlockEntity.WORKER_SLOT).is(worker.getItem()),
                "Configurator must not replace the worker");
        helper.assertTrue(target.getItem(ConfiguredMobFarmBlockEntity.SWORD_SLOT).is(Items.IRON_SWORD),
                "Configurator must not replace the tool");
        helper.assertTrue(target.cycleTicks() != source.cycleTicks(),
                "Configurator must not copy source progress");

        Identifier beforeTarget = target.selectedTargetId();
        MachineRedstoneMode beforeMode = target.redstoneMode();
        CompoundTag invalid = configuration.copy();
        invalid.putString("MobTarget", "minecraft:not_a_real_entity");
        helper.assertTrue(!target.canApplyMachineConfiguration(
                MachineConfigurationPort.CONFIGURATION_SCHEMA_VERSION,
                invalid
        ), "An invalid configuration was accepted");
        target.applyMachineConfiguration(MachineConfigurationPort.CONFIGURATION_SCHEMA_VERSION, invalid);
        helper.assertValueEqual(target.selectedTargetId(), beforeTarget, "Atomic rejection target");
        helper.assertValueEqual(target.redstoneMode(), beforeMode, "Atomic rejection redstone mode");
        helper.assertValueEqual(target.dataAccess().get(4), 99, "Atomic rejection XP");
        helper.succeed();
    }

    private static void redstonePauseAndComparator(GameTestHelper helper) {
        helper.setBlock(TARGET, ConfiguredMobFarmRegistrationAdapter.block(KIND).get());
        ConfiguredMobFarmBlockEntity farm = farm(helper, TARGET);
        farm.setItem(ConfiguredMobFarmBlockEntity.WORKER_SLOT, GameTestFixtures.adultVillagerCapture(helper));
        farm.setItem(ConfiguredMobFarmBlockEntity.SWORD_SLOT, new ItemStack(Items.WOODEN_SWORD));
        farm.dataAccess().set(0, 7);
        farm.setRedstoneMode(MachineRedstoneMode.HIGH_SIGNAL_PAUSES);
        helper.setBlock(POWER, Blocks.REDSTONE_BLOCK);

        farm.processServerTick();
        helper.assertValueEqual(farm.cycleTicks(), 7, "Redstone pause must preserve exact progress");
        helper.assertValueEqual(farm.machineDiagnosticSnapshot().progress(), 7,
                "Paused diagnostic progress");
        helper.assertValueEqual(farm.machineDiagnosticSnapshot().reason(), "redstone",
                "Paused diagnostic reason");

        helper.setBlock(POWER, Blocks.AIR);
        farm.processServerTick();
        helper.assertTrue(farm.cycleTicks() > 7, "Machine did not resume after its redstone signal changed");

        BlockEntityStateFixtures.fillIndexedSlots(
                helper,
                farm,
                "Slot",
                ConfiguredMobFarmBlockEntity.FIRST_OUTPUT_SLOT,
                ConfiguredMobFarmBlockEntity.OUTPUT_SLOT_COUNT,
                new ItemStack(Items.COBBLESTONE, 64)
        );
        helper.assertValueEqual(farm.comparatorOutput(), 15, "Full active outputs must emit comparator 15");
        farm.removeItem(ConfiguredMobFarmBlockEntity.FIRST_OUTPUT_SLOT, 64);
        helper.assertValueEqual(farm.comparatorOutput(), 0, "Available active output must emit comparator 0");
        helper.succeed();
    }

    private static ConfiguredMobFarmBlockEntity farm(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos, ConfiguredMobFarmBlockEntity.class);
    }
}
