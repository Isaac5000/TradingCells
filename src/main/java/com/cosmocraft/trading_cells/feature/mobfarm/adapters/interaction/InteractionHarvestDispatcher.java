package com.cosmocraft.trading_cells.feature.mobfarm.adapters.interaction;

import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.feature.mobfarm.adapters.input.EntityEssenceData;
import com.cosmocraft.trading_cells.feature.mobfarm.domain.model.InteractionSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class InteractionHarvestDispatcher {
    private static final Map<MinecraftServer, DisposableInteractionLevel> LEVELS = new IdentityHashMap<>();
    private static final List<InteractionHarvestHandler> HANDLERS = new ArrayList<>(List.of(
            new BeeHarvestHandler(), new MooshroomHarvestHandler(), new CreakingHarvestHandler(), new DragonBreathHarvestHandler()));
    private InteractionHarvestDispatcher() { }

    public static void registerHandler(InteractionHarvestHandler handler) { HANDLERS.add(java.util.Objects.requireNonNull(handler)); }
    public static void close(MinecraftServer server) {
        var level = LEVELS.remove(server);
        if (level != null) {
            try { level.close(); } catch (IOException error) { throw new java.io.UncheckedIOException(error); }
        }
    }
    public static InteractionHarvestResult harvest(ServerLevel source, ItemStack module, ItemStack input, InteractionSource channel) {
        if (input.isEmpty()) { return InteractionHarvestResult.rejected(InteractionHarvestResult.Status.EMPTY); }
        if (capturesCreature(input)) { return InteractionHarvestResult.rejected(InteractionHarvestResult.Status.UNSUPPORTED); }
        DisposableInteractionLevel level;
        try {
            level = LEVELS.get(source.getServer());
            if (level == null) { level = DisposableInteractionLevel.create(source); LEVELS.put(source.getServer(), level); }
        } catch (IOException | RuntimeException failure) { return InteractionHarvestResult.rejected(InteractionHarvestResult.Status.ERROR); }
        try {
            level.reset(source);
            return execute(level, module, input, channel);
        } catch (UnsupportedOperationException failure) {
            return InteractionHarvestResult.rejected(InteractionHarvestResult.Status.UNSUPPORTED);
        } catch (RuntimeException | LinkageError failure) {
            return InteractionHarvestResult.rejected(InteractionHarvestResult.Status.ERROR);
        } finally {
            level.finishAttempt();
        }
    }
    private static InteractionHarvestResult execute(DisposableInteractionLevel level, ItemStack module,
            ItemStack input, InteractionSource channel) {
        InteractionActor actor = new InteractionActor(level);
        try (var scope = InteractionIsolation.enter(level)) {
            LivingEntity entity = null;
            try {
                actor.setPos(0.5, 64, 1.5);
                actor.setItemInHand(InteractionHand.MAIN_HAND, input.copyWithCount(1));
                entity = EntityEssenceData.createEntity(level, module.copy());
                if (entity == null) { return InteractionHarvestResult.rejected(InteractionHarvestResult.Status.INVALID_MODULE); }
                entity.setPos(0.5, 64, 0.5);
                entity.setNoGravity(true);
                LivingEntity target = entity;
                var selected = HANDLERS.stream().filter(handler -> handler.supports(target, input)).findFirst();
                if (selected.isPresent()) { selected.get().interact(level, entity, actor); }
                else { actor.interactOn(entity, InteractionHand.MAIN_HAND, Vec3.ZERO); }
                if (scope.rejected() || !entity.isAlive() || entity.isRemoved() || entity.level() != level
                        || actor.isPassenger() || actor.level() != level
                        || entity.position().distanceToSqr(new Vec3(0.5, 64, 0.5)) > 0.01) {
                    return InteractionHarvestResult.rejected(InteractionHarvestResult.Status.UNSUPPORTED);
                }
                List<ItemStack> products = new ArrayList<>(level.capturedDrops());
                ItemStack hand = actor.getMainHandItem().copy();
                if (capturesCreature(hand)) { return InteractionHarvestResult.rejected(InteractionHarvestResult.Status.UNSUPPORTED); }
                actor.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                for (int slot = 0; slot < actor.getInventory().getContainerSize(); slot++) {
                    if (!actor.getInventory().getItem(slot).isEmpty()) { products.add(actor.getInventory().getItem(slot).copy()); }
                }
                ItemStack remainder = hand;
                if (channel == InteractionSource.ITEM && !hand.isEmpty() && !ItemStack.isSameItemSameComponents(hand, input)) {
                    products.add(hand); remainder = ItemStack.EMPTY;
                }
                if (products.stream().anyMatch(InteractionHarvestDispatcher::capturesCreature)) {
                    return InteractionHarvestResult.rejected(InteractionHarvestResult.Status.UNSUPPORTED);
                }
                if (products.isEmpty()) { return InteractionHarvestResult.rejected(InteractionHarvestResult.Status.NO_RESOURCE); }
                return new InteractionHarvestResult(remainder, products, InteractionHarvestResult.Status.READY);
            } finally {
                // Keep removal callbacks inside the same isolation boundary as the interaction.
                try { if (entity != null) { entity.discard(); } }
                finally { actor.getInventory().clearContent(); actor.discard(); }
            }
        }
    }
    private static boolean capturesCreature(ItemStack stack) {
        if (stack.getItem() instanceof net.minecraft.world.item.MobBucketItem
                || stack.has(DataComponents.BUCKET_ENTITY_DATA) || stack.has(DataComponents.ENTITY_DATA)) { return true; }
        for (var kind : CapturedMobKind.values()) { if (CapturedMobStackAdapter.isCapturer(kind, stack)) { return true; } }
        return false;
    }
}
