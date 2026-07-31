package com.cosmocraft.trading_cells.feature.captures.adapters.input;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueOutput;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class PiglinCapturerItem extends AbstractCapturerItem {
    public PiglinCapturerItem(Properties properties) {
        super(properties);
    }

    public static boolean hasCapturedPiglin(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null && !customData.isEmpty();
    }

    public static boolean capturePiglin(ItemStack stack, Piglin piglin) {
        if (hasCapturedPiglin(stack)) {
            return false;
        }

        setCapturedPiglinData(stack, createCapturedPiglinData(piglin));
        return true;
    }

    public static CompoundTag createCapturedPiglinData(Piglin piglin) {
        TagValueOutput output = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING,
                piglin.registryAccess()
        );
        piglin.saveWithoutId(output);
        CompoundTag piglinData = output.buildResult();
        stripVolatileEntityData(piglinData);
        return piglinData;
    }

    public static @Nullable CompoundTag getCapturedPiglinData(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || customData.isEmpty()) {
            return null;
        }
        return customData.copyTag();
    }

    public static void setCapturedPiglinData(ItemStack stack, CompoundTag piglinData) {
        AbstractCapturerItem.configureDurability(stack);
        CompoundTag cleanData = piglinData.copy();
        stripVolatileEntityData(cleanData);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(cleanData));
    }

    public static void clearCapturedPiglin(ItemStack stack) {
        stack.remove(DataComponents.CUSTOM_DATA);
    }

    public static @Nullable Piglin createCapturedPiglin(Level level, ItemStack stack, BlockPos position) {
        CompoundTag piglinData = getCapturedPiglinData(stack);
        if (piglinData == null) {
            return null;
        }
        return createCapturedPiglin(level, piglinData, position);
    }

    public static @Nullable Piglin createCapturedPiglin(Level level, CompoundTag piglinData, BlockPos position) {
        var entity = net.minecraft.world.entity.EntityType.loadEntityRecursive(
                CapturedEntityTypes.piglinType(),
                piglinData.copy(),
                level,
                EntitySpawnReason.LOAD,
                loadedEntity -> loadedEntity
        );
        if (!(entity instanceof Piglin piglin)) {
            return null;
        }

        piglin.setPos(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D);
        piglin.setPersistenceRequired();
        return piglin;
    }

    @Override
    public @NonNull InteractionResult useOn(@NonNull UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (!hasCapturedPiglin(stack)) {
            return InteractionResult.PASS;
        }

        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.FAIL;
        }

        Piglin piglin = createCapturedPiglin(level, stack, context.getClickedPos().relative(context.getClickedFace()));
        if (piglin == null || !level.addFreshEntity(piglin)) {
            return InteractionResult.FAIL;
        }

        clearCapturedPiglin(stack);
        damageAfterRelease(stack, serverLevel, context.getPlayer(), context.getHand());
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean isFoil(@NonNull ItemStack stack) {
        if (stack.has(DataComponents.UNBREAKABLE)) {
            return false;
        }
        return hasCapturedPiglin(stack) || super.isFoil(stack);
    }

    @Override
    public @NonNull Component getName(@NonNull ItemStack stack) {
        if (!hasCapturedPiglin(stack)) {
            return super.getName(stack);
        }

        CompoundTag tag = getCapturedPiglinData(stack);
        if (tag == null) {
            return Component.translatable("entity.minecraft.piglin");
        }

        if (tag.contains("CustomName")) {
            var customNameOpt = tag.getString("CustomName");
            if (customNameOpt.isPresent()) {
                String customName = customNameOpt.get();
                if (!customName.isEmpty()) {
                    if (isBabyPiglin(tag)) {
                        return Component.translatable("piglin.baby", Component.literal(customName));
                    }
                    return Component.literal(customName);
                }
            }
        }

        Component baseComponent = Component.translatable("entity.minecraft.piglin");
        if (isBabyPiglin(tag)) {
            return Component.translatable("piglin.baby", baseComponent);
        }

        return baseComponent;
    }

    public static boolean isBabyPiglin(CompoundTag tag) {
        if (tag.getBoolean("IsBaby").orElse(false)) {
            return true;
        }

        return tag.getInt("Age").map(age -> age < 0).orElse(false);
    }

    private static void stripVolatileEntityData(CompoundTag entityData) {
        entityData.remove("Pos");
        entityData.remove("Motion");
        entityData.remove("Rotation");
        entityData.remove("FallDistance");
        entityData.remove("Air");
        entityData.remove("OnGround");
        entityData.remove("HurtTime");
        entityData.remove("HurtByTimestamp");
        entityData.remove("LastHurtByPlayer");
        entityData.remove("LastHurt");
        entityData.remove("Fire");
        entityData.remove("PortalCooldown");
    }
}
