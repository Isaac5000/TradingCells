package com.cosmocraft.trading_cells.feature.mobfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public final class EssenceExtractorItem extends Item {
    public static final int RELOAD_TICKS = 24;
    public static final int EXTRACTION_TICKS = 24;
    private static final String LOADED = "TradingCellsSyringeLoaded";
    private static final String EXTRACTION = "TradingCellsSyringeExtraction";
    public EssenceExtractorItem(Properties properties) { super(properties); }

    public static boolean isLoaded(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBooleanOr(LOADED, false);
    }
    public static void setLoaded(ItemStack stack, boolean loaded) {
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (loaded) { data.putBoolean(LOADED, true); } else { data.remove(LOADED); }
        if (data.isEmpty()) { stack.remove(DataComponents.CUSTOM_DATA); }
        else { stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data)); }
    }
    public static int extractionTier(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
                .getCompoundOrEmpty(EXTRACTION).getIntOr("Tier", 0);
    }
    private static void clearExtraction(ItemStack stack) {
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        data.remove(EXTRACTION);
        if (data.isEmpty()) { stack.remove(DataComponents.CUSTOM_DATA); }
        else { stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data)); }
    }
    public static boolean beginExtraction(Player player, LivingEntity target, InteractionHand hand) {
        ItemStack tool = player.getItemInHand(hand);
        if (!target.isAlive() || target instanceof Player || player.isUsingItem()
                || player.getCooldowns().isOnCooldown(tool)) { return false; }
        if (!isLoaded(tool) && !player.getAbilities().instabuild) {
            if (!player.level().isClientSide()) { player.sendOverlayMessage(Component.translatable("message.trading_cells.essence.reload_required")); }
            return false;
        }
        if (!reachable(player, target)) { return false; }
        var data = tool.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        var extraction = new net.minecraft.nbt.CompoundTag();
        extraction.putString("Target", target.getUUID().toString());
        extraction.putInt("Tier", EssenceClassifier.classify(target, false).tier().id());
        data.put(EXTRACTION, extraction);
        tool.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        player.startUsingItem(hand);
        return true;
    }
    private static boolean reachable(Player player, LivingEntity target) {
        return target.isAlive() && target.level() == player.level() && player.hasLineOfSight(target)
                && target.getBoundingBox().distanceToSqr(player.getEyePosition())
                    <= Math.pow(player.entityInteractionRange() + 0.5, 2);
    }
    private static LivingEntity target(ItemStack stack, net.minecraft.server.level.ServerLevel level) {
        String id = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
                .getCompoundOrEmpty(EXTRACTION).getStringOr("Target", "");
        try { return level.getEntity(java.util.UUID.fromString(id)) instanceof LivingEntity entity ? entity : null; }
        catch (IllegalArgumentException ignored) { return null; }
    }
    private static int vialSlot(Player player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(MobFarmRegistrationAdapter.EMPTY_VIAL.get())) { return slot; }
        }
        return -1;
    }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack tool = player.getItemInHand(hand);
        if (isLoaded(tool) || player.getCooldowns().isOnCooldown(tool)) { return InteractionResult.PASS; }
        if (vialSlot(player) < 0 && !player.getAbilities().instabuild) {
            if (!level.isClientSide()) { player.sendOverlayMessage(Component.translatable("message.trading_cells.essence.bottle_required")); }
            return InteractionResult.FAIL;
        }
        clearExtraction(tool);
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }
    @Override public int getUseDuration(ItemStack stack, LivingEntity user) {
        return extractionTier(stack) > 0 ? EXTRACTION_TICKS : RELOAD_TICKS;
    }
    @Override public ItemUseAnimation getUseAnimation(ItemStack stack) { return ItemUseAnimation.NONE; }
    @Override public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (extractionTier(stack) <= 0 || !(level instanceof net.minecraft.server.level.ServerLevel server)
                || !(user instanceof Player player)) { return; }
        var target = target(stack, server);
        if (target == null || !reachable(player, target)) { player.stopUsingItem(); }
    }
    @Override public void onStopUsing(ItemStack stack, LivingEntity user, int remaining) {
        clearExtraction(stack);
    }
    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (level.isClientSide() || !(user instanceof Player player)) { return stack; }
        if (extractionTier(stack) > 0) {
            var target = target(stack, (net.minecraft.server.level.ServerLevel) level);
            clearExtraction(stack);
            if (target == null || !reachable(player, target) || (!isLoaded(stack) && !player.getAbilities().instabuild)) { return stack; }
            ItemStack essence = EntityEssenceData.rawEssenceOf(target);
            if (essence.isEmpty()) {
                player.sendOverlayMessage(Component.translatable("message.trading_cells.essence.unsupported"));
                return stack;
            }
            player.getCooldowns().addCooldown(stack, 40);
            if (!player.getAbilities().instabuild) {
                setLoaded(stack, false);
                stack.hurtAndBreak(1, player, player.getUsedItemHand());
            }
            if (!player.getInventory().add(essence)) { player.drop(essence, false); }
            player.getInventory().setChanged();
            return stack;
        }
        if (isLoaded(stack)) { return stack; }
        int slot = vialSlot(player);
        if (slot < 0 && !player.getAbilities().instabuild) { return stack; }
        if (!player.getAbilities().instabuild) { player.getInventory().removeItem(slot, 1); }
        setLoaded(stack, true);
        player.getInventory().setChanged();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_LOADING_END,
                SoundSource.PLAYERS, 0.5F, 1.4F);
        return stack;
    }
}
