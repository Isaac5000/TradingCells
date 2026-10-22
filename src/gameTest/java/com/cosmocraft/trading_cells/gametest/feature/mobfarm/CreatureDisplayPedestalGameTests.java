package com.cosmocraft.trading_cells.gametest.feature.mobfarm;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.CreatureDisplayPedestalBlockEntity;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EntityEssenceData;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.CreatureDisplayOrientation;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.TagValueInput;

/** Item ownership, saving and display preferences must not create or lose models. */
public final class CreatureDisplayPedestalGameTests {
    private static final BlockPos POS = new BlockPos(1, 1, 1);

    private CreatureDisplayPedestalGameTests() { }

    public static List<GameTestCase> tests() {
        return List.of(
                new GameTestCase("creature_display_pedestal_model_persistence", 40,
                        CreatureDisplayPedestalGameTests::modelPersistence),
                new GameTestCase("creature_display_pedestal_rejects_other_items", 40,
                        CreatureDisplayPedestalGameTests::rejectsOtherItems),
                new GameTestCase("creature_display_pedestal_cardinal_orientation", 40,
                        CreatureDisplayPedestalGameTests::cardinalOrientation));
    }

    private static CreatureDisplayPedestalBlockEntity pedestal(GameTestHelper helper) {
        helper.setBlock(POS, MobFarmRegistrationAdapter.DISPLAY_PEDESTAL.get());
        return helper.getBlockEntity(POS, CreatureDisplayPedestalBlockEntity.class);
    }

    private static void cardinalOrientation(GameTestHelper helper) {
        // A PoseStack positive-Y rotation must face east at +90 and west at +270.
        Direction[] fronts = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        int[] expected = {180, 90, 0, 270};
        for (int i = 0; i < fronts.length; i++) {
            int regular = Math.floorMod(Math.round(CreatureDisplayOrientation.modelYaw(fronts[i], false)), 360);
            int dragon = Math.floorMod(Math.round(CreatureDisplayOrientation.modelYaw(fronts[i], true)), 360);
            helper.assertValueEqual(regular, expected[i], "Ordinary mob faces " + fronts[i]);
            helper.assertValueEqual(dragon, (expected[i] + 180) % 360,
                    "Dragon uses its reversed model axis facing " + fronts[i]);
        }
        helper.succeed();
    }

    private static void rejectsOtherItems(GameTestHelper helper) {
        var stand = pedestal(helper);
        helper.assertFalse(CreatureDisplayPedestalBlockEntity.accepts(Items.STICK.getDefaultInstance()),
                "Ordinary items cannot be displayed");
        helper.assertFalse(stand.insert(Items.STICK.getDefaultInstance()),
                "An invalid item must not fill the pedestal");
        helper.assertTrue(stand.model().isEmpty(), "Pedestal remains empty");
        helper.succeed();
    }

    private static void modelPersistence(GameTestHelper helper) {
        var stand = pedestal(helper);
        ItemStack model = EntityEssenceData.moduleFor(helper.getLevel(),
                Identifier.withDefaultNamespace("cow"));
        helper.assertTrue(CreatureDisplayPedestalBlockEntity.accepts(model),
                "An existing creature module must be supported");
        helper.assertTrue(stand.insert(model), "Model is inserted");
        helper.assertFalse(stand.insert(model), "Second insertion must fail");
        helper.assertTrue(stand.model().getCount() == 1, "Only one item stored");
        helper.assertFalse(stand.rotating(), "Display stays facing its front by default");
        stand.toggleRotation();
        helper.assertTrue(stand.rotating(), "Rotation can be enabled");

        CompoundTag stored = stand.saveWithFullMetadata(helper.getLevel().registryAccess());
        stand.removeModel();
        stand.loadWithComponents(TagValueInput.create(
                ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), stored));
        helper.assertTrue(ItemStack.isSameItemSameComponents(model, stand.model()),
                "Saved module and its creature data survive reload");
        helper.assertTrue(stand.rotating(), "Rotation setting survives reload");
        ItemStack extracted = stand.removeModel();
        helper.assertTrue(ItemStack.isSameItemSameComponents(model, extracted),
                "Extraction returns the same creature module");
        helper.assertTrue(stand.model().isEmpty(), "Extraction empties the pedestal");
        helper.succeed();
    }
}
