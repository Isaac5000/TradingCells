package com.cosmocraft.trading_cells.gametest.feature.breeders;

import com.cosmocraft.trading_cells.feature.breeders.adapters.input.VillagerBreederBlockEntity;
import com.cosmocraft.trading_cells.feature.breeders.adapters.output.BreederRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import com.cosmocraft.trading_cells.gametest.shared.GameTestFixtures;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class BreederGameTests {
    private BreederGameTests() { }

    public static List<GameTestCase> tests() {
        return List.of(new GameTestCase("breeder_duplicate_parents_fresh_offspring", 20, BreederGameTests::offspring));
    }

    private static void tick(GameTestHelper helper, VillagerBreederBlockEntity machine) {
        machine.getBlockState().getTicker(helper.getLevel(), BreederRegistrationAdapter.VILLAGER_BREEDER_BLOCK_ENTITY.get())
                .tick(helper.getLevel(), machine.getBlockPos(), machine.getBlockState(), machine);
    }

    private static void offspring(GameTestHelper h) {
        h.setBlock(GameTestFixtures.TEST_POS, BreederRegistrationAdapter.VILLAGER_BREEDER_BLOCK.get());
        var machine = h.getBlockEntity(GameTestFixtures.TEST_POS, VillagerBreederBlockEntity.class);
        ItemStack parent = GameTestFixtures.adultVillagerCapture(h);
        CompoundTag data = CapturedMobStackAdapter.copyData(CapturedMobKind.VILLAGER, parent);
        CompoundTag profession = data.getCompound("VillagerData").orElseThrow();
        profession.putString("profession", "minecraft:librarian");
        profession.putInt("level", 5);
        data.put("VillagerData", profession);
        data.putInt("Xp", 250);
        data.putInt("ForcedAge", 900);
        data.putString("CustomName", "Copied parent");
        for (String key : List.of("Offers", "Brain", "NeoForgeData", "equipment")) {
            CompoundTag marker = new CompoundTag();
            marker.putBoolean("ParentOnly", true);
            data.put(key, marker);
        }
        CapturedMobStackAdapter.setData(CapturedMobKind.VILLAGER, parent, data);
        machine.setItem(1, parent.copy());
        machine.setItem(2, parent.copy());
        machine.setItem(0, new ItemStack(Items.BREAD, 64));
        machine.setItem(4, new ItemStack(CapturedMobStackAdapter.capturerItem(CapturedMobKind.VILLAGER), 2));
        machine.dataAccess().set(2, 1);
        tick(h, machine);
        machine.dataAccess().set(0, machine.dataAccess().get(3) - 1);
        tick(h, machine);
        h.assertValueEqual(machine.getPendingBabies(), 1, "Duplicated adult inputs can breed normally");
        checkData(h, machine.createBabyPreviewStack());

        // Old worlds can contain an unsanitized parent clone in a pending BabyTemplate.
        CompoundTag saved = machine.saveWithFullMetadata(h.getLevel().registryAccess());
        saved.put("BabyTemplate", data.copy());
        var restored = (VillagerBreederBlockEntity) BlockEntity.loadStatic(machine.getBlockPos(),
                machine.getBlockState(), saved, h.getLevel().registryAccess());
        h.assertTrue(restored != null, "Pending offspring survives machine persistence");
        restored.setLevel(h.getLevel());
        tick(h, restored);
        ItemStack child = restored.getItem(5);
        checkData(h, child);
        h.assertValueEqual(restored.getItem(4).getCount(), 1, "One empty capturer consumed");
        h.assertTrue(ItemStack.matches(restored.getItem(1), parent) && ItemStack.matches(restored.getItem(2), parent),
                "Both parent items retain their original data");
        var first = CapturedMobStackAdapter.createVillager(h.getLevel(), child, machine.getBlockPos());
        var second = CapturedMobStackAdapter.createVillager(h.getLevel(), child.copy(), machine.getBlockPos());
        h.assertTrue(first != null && second != null && first.isBaby() && second.isBaby(), "Offspring reconstruct as babies");
        h.assertFalse(first.getUUID().equals(second.getUUID()), "Each reconstructed offspring receives a fresh identity");
        h.assertValueEqual(first.getVillagerXp(), 0, "Offspring has no trading XP");
        h.assertTrue(first.getOffers().isEmpty(), "Offspring has no inherited trades");
        h.assertValueEqual(first.getVillagerData().level(), 1, "Offspring starts at level one");
        h.succeed();
    }

    private static void checkData(GameTestHelper h, ItemStack child) {
        CompoundTag data = CapturedMobStackAdapter.copyData(CapturedMobKind.VILLAGER, child);
        h.assertTrue(data != null, "Valid captured offspring");
        for (String key : List.of("UUID", "CustomName", "Offers", "Brain", "NeoForgeData", "equipment", "Xp", "ForcedAge")) {
            h.assertFalse(data.contains(key), "Offspring must not inherit " + key);
        }
        h.assertValueEqual(data.getIntOr("Age", 0), -24000, "Newborn age");
        h.assertValueEqual(data.getCompound("VillagerData").orElseThrow().getStringOr("profession", ""),
                "minecraft:none", "Offspring starts unemployed");
    }
}
