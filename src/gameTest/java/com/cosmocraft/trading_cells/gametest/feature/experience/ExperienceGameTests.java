package com.cosmocraft.trading_cells.gametest.feature.experience;

import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import com.cosmocraft.trading_cells.gametest.shared.GameTestFixtures;
import java.util.List;
import com.cosmocraft.trading_cells.feature.experience.adapters.input.ExperienceStorageBlockEntity;
import com.cosmocraft.trading_cells.feature.experience.adapters.output.ExperienceStorageRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.infusion.adapters.input.ArcaneInfuserBlockEntity;
import com.cosmocraft.trading_cells.feature.infusion.adapters.output.ArcaneInfuserRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.trader.adapters.output.AutotraderRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.trader.adapters.output.TraderRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.registration.ExperienceFluidRegistration;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Behaviour-oriented GameTests for Experience. */
public final class ExperienceGameTests {
    private ExperienceGameTests() {
    }

    public static List<GameTestCase> tests() {
        return List.of(
            new GameTestCase("experience_fluid_contracts", 20,
                    ExperienceGameTests::experienceFluidContracts)
        );
    }

    private static void experienceFluidContracts(GameTestHelper helper) {
        FluidResource experience = FluidResource.of(ExperienceFluidRegistration.SOURCE.get());

        helper.setBlock(GameTestFixtures.TEST_POS, ExperienceStorageRegistrationAdapter.BLOCK.get());
        ExperienceStorageBlockEntity storage = helper.getBlockEntity(GameTestFixtures.TEST_POS, ExperienceStorageBlockEntity.class);
        ResourceHandler<FluidResource> storageFluid = helper.requireCapability(
                Capabilities.Fluid.BLOCK,
                GameTestFixtures.TEST_POS,
                null
        );
        try (Transaction transaction = Transaction.openRoot()) {
            helper.assertValueEqual(storageFluid.insert(0, experience, 120, transaction), 120, "XP insertion");
        }
        helper.assertValueEqual(storage.storedExperience(), 0, "Rolled-back XP insertion");
        try (Transaction transaction = Transaction.openRoot()) {
            helper.assertValueEqual(storageFluid.insert(0, experience, 120, transaction), 120, "XP insertion");
            transaction.commit();
        }
        helper.assertValueEqual(storage.storedExperience(), 120, "Committed XP insertion");
        try (Transaction transaction = Transaction.openRoot()) {
            helper.assertValueEqual(storageFluid.extract(0, experience, 20, transaction), 20, "XP extraction");
            transaction.commit();
        }
        helper.assertValueEqual(storage.storedExperience(), 100, "Committed XP extraction");

        for (var side : net.minecraft.core.Direction.values()) {
            var sided = helper.requireCapability(Capabilities.Fluid.BLOCK, GameTestFixtures.TEST_POS, side);
            try (var transaction = Transaction.openRoot()) {
                helper.assertValueEqual(sided.insert(0, experience, 70, transaction), 70, "XP input on " + side);
                transaction.commit();
            }
            try (var transaction = Transaction.openRoot()) {
                helper.assertValueEqual(sided.extract(0, experience, 70, transaction), 70, "XP output on " + side);
            }
            helper.assertValueEqual(storage.storedExperience(), 170, "Sided extraction rollback on " + side);
            try (var transaction = Transaction.openRoot()) {
                helper.assertValueEqual(sided.extract(0, experience, 70, transaction), 70, "XP output on " + side);
                transaction.commit();
            }
            helper.assertValueEqual(storage.storedExperience(), 100, "Sided XP conservation on " + side);
        }

        CompoundTag saved = storage.saveWithFullMetadata(helper.getLevel().registryAccess());
        BlockEntity loaded = BlockEntity.loadStatic(
                helper.absolutePos(GameTestFixtures.TEST_POS),
                storage.getBlockState(),
                saved,
                helper.getLevel().registryAccess()
        );
        helper.assertTrue(loaded instanceof ExperienceStorageBlockEntity, "Experience Storage failed to reload");
        helper.assertValueEqual(
                ((ExperienceStorageBlockEntity) loaded).storedExperience(),
                100,
                "Persisted XP"
        );

        helper.setBlock(GameTestFixtures.TEST_POS, ArcaneInfuserRegistrationAdapter.BLOCK.get());
        ArcaneInfuserBlockEntity infuser = helper.getBlockEntity(GameTestFixtures.TEST_POS, ArcaneInfuserBlockEntity.class);
        ResourceHandler<FluidResource> infuserFluid = helper.requireCapability(
                Capabilities.Fluid.BLOCK,
                GameTestFixtures.TEST_POS,
                null
        );
        try (Transaction transaction = Transaction.openRoot()) {
            helper.assertValueEqual(infuserFluid.insert(0, experience, 30, transaction), 30, "Infuser XP insertion");
            transaction.commit();
        }
        helper.assertValueEqual(infuser.storedExperience(), 30, "Infuser XP storage");
        try (Transaction transaction = Transaction.openRoot()) {
            helper.assertValueEqual(
                    infuserFluid.extract(0, experience, 1, transaction),
                    0,
                    "Infuser must be an XP destination only"
            );
            transaction.commit();
        }
        helper.assertValueEqual(infuser.storedExperience(), 30, "Rejected Infuser XP extraction");

        assertOutputOnlyFluid(helper, TraderRegistrationAdapter.VILLAGER_TRADER_BLOCK.get(), experience);
        assertOutputOnlyFluid(helper, AutotraderRegistrationAdapter.AUTOTRADER_BLOCK.get(), experience);
        helper.succeed();
    }

    private static void assertOutputOnlyFluid(
            GameTestHelper helper,
            Block block,
            FluidResource experience
    ) {
        helper.setBlock(GameTestFixtures.TEST_POS, block);
        ResourceHandler<FluidResource> handler = helper.requireCapability(
                Capabilities.Fluid.BLOCK,
                GameTestFixtures.TEST_POS,
                null
        );
        try (Transaction transaction = Transaction.openRoot()) {
            helper.assertValueEqual(handler.insert(0, experience, 1, transaction), 0, "Output-only XP insertion");
            transaction.commit();
        }
    }
}
