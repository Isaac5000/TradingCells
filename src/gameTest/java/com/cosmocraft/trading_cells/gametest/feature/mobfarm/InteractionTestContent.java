package com.cosmocraft.trading_cells.gametest.feature.mobfarm;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.interaction.DisposableInteractionLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Only the test mod owns these registrations; production has no knowledge of their IDs. */
@EventBusSubscriber(modid = "trading_cells_gametest")
public final class InteractionTestContent {
    public static final Identifier ENTITY_ID = Identifier.fromNamespaceAndPath("trading_cells_gametest", "resource_creature");
    public static EntityType<ResourceCreature> type;
    public static Item tool;
    public static int attempts;
    private InteractionTestContent() { }
    @SubscribeEvent public static void register(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.ENTITY_TYPE)) {
            type = EntityType.Builder.<ResourceCreature>of(ResourceCreature::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F).build(ResourceKey.create(Registries.ENTITY_TYPE, ENTITY_ID));
            event.register(Registries.ENTITY_TYPE, ENTITY_ID, () -> type);
        }
        if (event.getRegistryKey().equals(Registries.ITEM)) {
            Identifier id = Identifier.fromNamespaceAndPath("trading_cells_gametest", "resource_tool");
            tool = new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)).durability(8)) {
                @Override public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
                    if (!(target instanceof ResourceCreature)) { return InteractionResult.PASS; }
                    attempts++;
                    player.getInventory().add(new ItemStack(Items.LAPIS_LAZULI, 2));
                    stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
                    return InteractionResult.SUCCESS;
                }
            };
            event.register(Registries.ITEM, id, () -> tool);
        }
    }
    @SubscribeEvent public static void attributes(EntityAttributeCreationEvent event) { event.put(type, Zombie.createAttributes().build()); }
    @SubscribeEvent public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getLevel() instanceof DisposableInteractionLevel) || !(event.getTarget() instanceof ResourceCreature)
                || !event.getItemStack().is(Items.AMETHYST_SHARD)) { return; }
        attempts++;
        event.getItemStack().shrink(1);
        event.getEntity().getInventory().add(new ItemStack(Items.DIAMOND));
        event.setCancellationResult(InteractionResult.SUCCESS); event.setCanceled(true);
    }
    public static final class ResourceCreature extends Zombie {
        ResourceCreature(EntityType<? extends Zombie> type, Level level) { super(type, level); }
        @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
            ItemStack input = player.getItemInHand(hand);
            if (level() instanceof DisposableInteractionLevel && (input.getCount() != 1 || !player.getOffhandItem().isEmpty()
                    || player.isCreative() || player.isSpectator())) {
                throw new AssertionError("Interaction actor must receive one item in survival and an empty other hand");
            }
            if (input.is(Items.EMERALD)) {
                attempts++;
                input.shrink(1);
                player.getInventory().add(new ItemStack(Items.GOLD_NUGGET, 2));
                spawnAtLocation((ServerLevel) level(), new ItemStack(Items.REDSTONE));
                return InteractionResult.SUCCESS;
            }
            if (input.is(Items.GOLD_BLOCK)) {
                input.shrink(1);
                for (int i = 0; i < 28; i++) { spawnAtLocation((ServerLevel) level(), new ItemStack(Items.MILK_BUCKET)); }
                return InteractionResult.SUCCESS;
            }
            if (input.is(Items.CHEST)) { player.openMenu(null); }
            if (input.is(Items.SADDLE)) { player.startRiding(this); }
            if (input.is(Items.ENDER_PEARL)) { teleportTo(20, 64, 20); }
            if (input.is(Items.CLOCK)) { level().getServer().execute(() -> attempts++); }
            if (input.is(Items.TNT)) { throw new IllegalStateException("Deliberate test failure"); }
            if (input.is(Items.WATER_BUCKET)) {
                player.setItemInHand(hand, new ItemStack(Items.COD_BUCKET)); discard(); return InteractionResult.SUCCESS;
            }
            if (input.is(Items.DIRT)) {
                level().setBlockAndUpdate(blockPosition(), net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState());
            }
            return super.mobInteract(player, hand);
        }
    }
}
