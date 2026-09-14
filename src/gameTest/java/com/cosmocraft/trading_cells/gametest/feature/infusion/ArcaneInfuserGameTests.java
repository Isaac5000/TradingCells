package com.cosmocraft.trading_cells.gametest.feature.infusion;

import com.cosmocraft.trading_cells.feature.experience.adapters.output.ExperienceStorageRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.infusion.adapters.input.ArcaneInfuserBlockEntity;
import com.cosmocraft.trading_cells.feature.infusion.adapters.input.ArcaneInfuserMenu;
import com.cosmocraft.trading_cells.feature.infusion.adapters.minecraft.ArcaneInfusionRecipe;
import com.cosmocraft.trading_cells.feature.infusion.adapters.output.ArcaneInfuserRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import com.cosmocraft.trading_cells.gametest.shared.GameTestFixtures;
import com.cosmocraft.trading_cells.platform.neoforge.registration.ExperienceFluidRegistration;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Behaviour-oriented GameTests for ArcaneInfuser. */
public final class ArcaneInfuserGameTests {
    private ArcaneInfuserGameTests() {
    }

    public static List<GameTestCase> tests() {
        return List.of(
            new GameTestCase("manual_infuser_contract", 20,
                    ArcaneInfuserGameTests::manualInfuserContract),
            new GameTestCase("arcane_infuser_recipe_book_placement", 20,
                    ArcaneInfuserGameTests::recipeBookPlacement),
            new GameTestCase("arcane_infuser_exact_component_placement", 20,
                    ArcaneInfuserGameTests::exactComponentPlacement),
            new GameTestCase("arcane_infuser_container_remainders", 20,
                    ArcaneInfuserGameTests::containerRemainders)
        );
    }

    private static void containerRemainders(GameTestHelper helper) {
        helper.setBlock(GameTestFixtures.TEST_POS, ArcaneInfuserRegistrationAdapter.BLOCK.get());
        ArcaneInfuserBlockEntity infuser = helper.getBlockEntity(
                GameTestFixtures.TEST_POS,
                ArcaneInfuserBlockEntity.class
        );

        for (Item bucket : List.of(Items.WATER_BUCKET, Items.LAVA_BUCKET)) {
            infuser.clearContent();
            infuser.setItem(ArcaneInfuserBlockEntity.BOTTOM_SLOT, new ItemStack(bucket));
            insertExperience(helper, infuser, 10);
            ItemStack result = infuser.visibleResult();
            helper.assertTrue(result.is(Items.COBBLESTONE), "Dedicated container-remainder fixture must match");
            helper.assertTrue(infuser.takeVisibleResult(result), "Bucket fixture must be craftable");
            helper.assertTrue(infuser.getItem(ArcaneInfuserBlockEntity.BOTTOM_SLOT).is(Items.BUCKET),
                    "Every filled bucket must leave its empty bucket in the same slot");
        }
        helper.succeed();
    }

    private static void recipeBookPlacement(GameTestHelper helper) {
        helper.setBlock(GameTestFixtures.TEST_POS, ArcaneInfuserRegistrationAdapter.BLOCK.get());
        ArcaneInfuserBlockEntity infuser = helper.getBlockEntity(
                GameTestFixtures.TEST_POS,
                ArcaneInfuserBlockEntity.class
        );
        var player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        ArcaneInfuserMenu menu = (ArcaneInfuserMenu) infuser.createMenu(
                1,
                player.getInventory(),
                player
        );
        RecipeHolder<?> recipe = recipe(helper, "trading_cells_gametest:sparse_arcane_infusion");

        player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 3));
        RecipeBookMenu.PostPlaceAction action = menu.handlePlacement(
                false,
                false,
                recipe,
                helper.getLevel(),
                player.getInventory()
        );
        helper.assertValueEqual(action, RecipeBookMenu.PostPlaceAction.NOTHING,
                "Single recipe-book placement action");
        helper.assertValueEqual(infuser.getItem(ArcaneInfuserBlockEntity.CENTER_SLOT).getCount(), 1,
                "A normal click must place one batch");
        helper.assertValueEqual(count(player.getInventory(), Items.DIAMOND), 2,
                "Single placement remaining diamonds");
        assertOnlyCenterOccupied(helper, infuser);

        action = menu.handlePlacement(
                true,
                false,
                recipe,
                helper.getLevel(),
                player.getInventory()
        );
        helper.assertValueEqual(action, RecipeBookMenu.PostPlaceAction.NOTHING,
                "Maximum recipe-book placement action");
        helper.assertValueEqual(infuser.getItem(ArcaneInfuserBlockEntity.CENTER_SLOT).getCount(), 3,
                "Shift must place the maximum complete batches");
        helper.assertValueEqual(count(player.getInventory(), Items.DIAMOND), 0,
                "Maximum placement remaining diamonds");

        infuser.setItem(ArcaneInfuserBlockEntity.CENTER_SLOT, ItemStack.EMPTY);
        infuser.setItem(ArcaneInfuserBlockEntity.TOP_LEFT_SLOT, new ItemStack(Items.COBBLESTONE));
        clearInventory(player.getInventory());
        action = menu.handlePlacement(
                false,
                false,
                recipe,
                helper.getLevel(),
                player.getInventory()
        );
        helper.assertValueEqual(action, RecipeBookMenu.PostPlaceAction.PLACE_GHOST_RECIPE,
                "Missing ingredients must request a ghost recipe");
        helper.assertTrue(infuser.getItem(ArcaneInfuserBlockEntity.TOP_LEFT_SLOT).isEmpty(),
                "A failed placement must return the old input grid");
        helper.assertValueEqual(count(player.getInventory(), Items.COBBLESTONE), 1,
                "A failed placement must preserve returned inputs");
        helper.succeed();
    }

    private static void exactComponentPlacement(GameTestHelper helper) {
        ArcaneInfusionRecipe recipe = (ArcaneInfusionRecipe) recipe(
                helper,
                "trading_cells:silk_touch_two_infusion"
        ).value();
        var enchantments = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        ItemStack silkTouch = EnchantmentHelper.createBook(new EnchantmentInstance(
                enchantments.getOrThrow(Enchantments.SILK_TOUCH),
                1
        ));
        ItemStack fortune = EnchantmentHelper.createBook(new EnchantmentInstance(
                enchantments.getOrThrow(Enchantments.FORTUNE),
                1
        ));

        helper.assertTrue(recipe.matchesPlacementStack(4, silkTouch),
                "The required Silk Touch I book must match placement");
        helper.assertTrue(!recipe.matchesPlacementStack(4, fortune),
                "An enchanted book with different components must not match placement");
        helper.assertTrue(!recipe.isSpecial(),
                "Arcane recipes must be visible in the recipe book");
        helper.assertTrue(!recipe.showNotification(),
                "Pre-unlocked Arcane recipes must not display unlock notifications");
        helper.succeed();
    }

    private static void manualInfuserContract(GameTestHelper helper) {
        helper.setBlock(GameTestFixtures.TEST_POS, ArcaneInfuserRegistrationAdapter.BLOCK.get());
        for (var side : net.minecraft.core.Direction.values()) {
            ResourceHandler<ItemResource> handler = helper.requireCapability(
                    Capabilities.Item.BLOCK,
                    GameTestFixtures.TEST_POS,
                    side
            );
            helper.assertValueEqual(handler.size(), 0, "Automated Infuser slots on " + side);
        }
        assertEntityFarmRecipeLayout(helper);
        helper.succeed();
    }

    private static void assertEntityFarmRecipeLayout(GameTestHelper helper) {
        ArcaneInfuserBlockEntity infuser = helper.getBlockEntity(
                GameTestFixtures.TEST_POS,
                ArcaneInfuserBlockEntity.class
        );
        infuser.setItem(
                ArcaneInfuserBlockEntity.TOP_LEFT_SLOT,
                new ItemStack(Items.IRON_BLOCK)
        );
        infuser.setItem(ArcaneInfuserBlockEntity.TOP_SLOT, new ItemStack(Items.DIAMOND_SWORD));
        infuser.setItem(
                ArcaneInfuserBlockEntity.TOP_RIGHT_SLOT,
                new ItemStack(Items.IRON_BLOCK)
        );
        infuser.setItem(ArcaneInfuserBlockEntity.LEFT_SLOT, new ItemStack(Items.IRON_BARS));
        infuser.setItem(
                ArcaneInfuserBlockEntity.CENTER_SLOT,
                new ItemStack(ExperienceStorageRegistrationAdapter.ITEM.get())
        );
        infuser.setItem(ArcaneInfuserBlockEntity.RIGHT_SLOT, new ItemStack(Items.IRON_BARS));
        infuser.setItem(ArcaneInfuserBlockEntity.BOTTOM_LEFT_SLOT, new ItemStack(Items.QUARTZ_BLOCK));
        infuser.setItem(ArcaneInfuserBlockEntity.BOTTOM_SLOT, new ItemStack(Items.AMETHYST_BLOCK));
        infuser.setItem(ArcaneInfuserBlockEntity.BOTTOM_RIGHT_SLOT, new ItemStack(Items.QUARTZ_BLOCK));

        helper.assertTrue(
                infuser.visibleResult().is(MobFarmRegistrationAdapter.ITEM.get()),
                "The entity-farm recipe must accept Experience Storage in its center"
        );
        infuser.setItem(ArcaneInfuserBlockEntity.CENTER_SLOT, new ItemStack(Items.DIRT));
        helper.assertTrue(
                infuser.visibleResult().isEmpty(),
                "The entity-farm recipe must reject a different center block"
        );
    }

    private static RecipeHolder<?> recipe(GameTestHelper helper, String id) {
        Identifier identifier = Identifier.parse(id);
        return helper.getLevel().getServer().getRecipeManager().getRecipes().stream()
                .filter(holder -> holder.id().identifier().equals(identifier))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing test recipe " + identifier));
    }

    private static Item item(String id) {
        Identifier identifier = Identifier.parse(id);
        return BuiltInRegistries.ITEM.getOptional(identifier)
                .orElseThrow(() -> new IllegalStateException("Missing test item " + identifier));
    }

    private static int count(net.minecraft.world.entity.player.Inventory inventory, net.minecraft.world.item.Item item) {
        return inventory.getNonEquipmentItems().stream()
                .filter(stack -> stack.is(item))
                .mapToInt(ItemStack::getCount)
                .sum();
    }

    private static void clearInventory(net.minecraft.world.entity.player.Inventory inventory) {
        for (int slot = 0; slot < inventory.getNonEquipmentItems().size(); slot++) {
            inventory.setItem(slot, ItemStack.EMPTY);
        }
    }

    private static void setInputs(
            ArcaneInfuserBlockEntity infuser,
            net.minecraft.world.item.Item... items
    ) {
        for (int slot = 0; slot < items.length; slot++) {
            infuser.setItem(slot, new ItemStack(items[slot]));
        }
    }

    private static void insertExperience(
            GameTestHelper helper,
            ArcaneInfuserBlockEntity infuser,
            int amount
    ) {
        try (Transaction transaction = Transaction.openRoot()) {
            long inserted = infuser.fluidHandler().insert(
                    0,
                    FluidResource.of(ExperienceFluidRegistration.SOURCE.get()),
                    amount,
                    transaction
            );
            helper.assertValueEqual(inserted, (long) amount, "Inserted Infuser XP");
            transaction.commit();
        }
    }

    private static void assertOnlyCenterOccupied(
            GameTestHelper helper,
            ArcaneInfuserBlockEntity infuser
    ) {
        for (int slot = 0; slot < ArcaneInfuserBlockEntity.INPUT_SLOT_COUNT; slot++) {
            if (slot != ArcaneInfuserBlockEntity.CENTER_SLOT) {
                helper.assertTrue(infuser.getItem(slot).isEmpty(),
                        "Sparse recipe slot " + slot + " must remain empty");
            }
        }
    }
}
