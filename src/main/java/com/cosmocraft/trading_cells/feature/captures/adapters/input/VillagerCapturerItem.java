package com.cosmocraft.trading_cells.feature.captures.adapters.input;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueOutput;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class VillagerCapturerItem extends Item {
    public VillagerCapturerItem(Properties properties) {
        super(properties);
    }

    public static boolean hasCapturedVillager(ItemStack stack) {
        return PiglinCapturerItem.hasCapturedPiglin(stack);
    }

    public static boolean captureVillager(ItemStack stack, Villager villager) {
        if (hasCapturedVillager(stack)) {
            return false;
        }

        setCapturedVillagerData(stack, createCapturedVillagerData(villager));
        return true;
    }

    public static CompoundTag createCapturedVillagerData(Villager villager) {
        TagValueOutput output = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING,
                villager.registryAccess()
        );
        villager.saveWithoutId(output);
        CompoundTag villagerData = output.buildResult();
        stripVolatileEntityData(villagerData);
        return villagerData;
    }

    public static @Nullable CompoundTag getCapturedVillagerData(ItemStack stack) {
        return PiglinCapturerItem.getCapturedPiglinData(stack);
    }

    public static void setCapturedVillagerData(ItemStack stack, CompoundTag villagerData) {
        CompoundTag cleanData = villagerData.copy();
        stripVolatileEntityData(cleanData);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(cleanData));
    }

    public static void clearCapturedVillager(ItemStack stack) {
        stack.remove(DataComponents.CUSTOM_DATA);
    }

    public static @Nullable Villager createCapturedVillager(Level level, ItemStack stack, BlockPos position) {
        CompoundTag villagerData = getCapturedVillagerData(stack);
        if (villagerData == null) {
            return null;
        }
        return createCapturedVillager(level, villagerData, position);
    }

    public static @Nullable Villager createCapturedVillager(Level level, CompoundTag villagerData, BlockPos position) {
        CompoundTag safeData = villagerData.copy();
        stripVolatileEntityData(safeData);
        var entity = net.minecraft.world.entity.EntityType.loadEntityRecursive(
                VillagerCapturerItem.capturedVillagerType(),
                safeData,
                level,
                EntitySpawnReason.LOAD,
                loadedEntity -> loadedEntity
        );
        if (!(entity instanceof Villager villager)) {
            return null;
        }

        villager.setPos(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D);
        villager.setPersistenceRequired();
        return villager;
    }

    public static EntityType<Villager> capturedVillagerType() {
        return CapturedEntityTypes.villagerType();
    }

    @Override
    public @NonNull InteractionResult useOn(@NonNull UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (!hasCapturedVillager(stack)) {
            return InteractionResult.PASS;
        }

        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        Villager villager = releaseVillager(level, stack, context.getClickedPos().relative(context.getClickedFace()));
        if (villager == null) {
            return InteractionResult.FAIL;
        }
        if (!level.noCollision(villager)) {
            BlockPos fallback = context.getClickedPos().above();
            villager.setPos(fallback.getX() + 0.5D, fallback.getY(), fallback.getZ() + 0.5D);
        }
        if (!level.noCollision(villager) || !level.addFreshEntity(villager)) {
            return InteractionResult.FAIL;
        }

        clearCapturedVillager(stack);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getMaxStackSize(@NonNull ItemStack stack) {
        return hasCapturedVillager(stack) ? 1 : getDefaultMaxStackSize();
    }

    @Override
    public boolean isFoil(@NonNull ItemStack stack) {
        return hasCapturedVillager(stack) || super.isFoil(stack);
    }

    @Override
    public @NonNull Component getName(@NonNull ItemStack stack) {
        if (!hasCapturedVillager(stack)) {
            return super.getName(stack);
        }

        CompoundTag tag = getCapturedVillagerData(stack);
        if (tag == null) {
            return Component.translatable("entity.minecraft.villager.none");
        }

        if (tag.contains("CustomName")) {
            var customNameOpt = tag.getString("CustomName");
            if (customNameOpt.isPresent()) {
                String customName = customNameOpt.get();
                if (!customName.isEmpty()) {
                    if (isBabyVillager(tag)) {
                        return Component.translatable("villager.baby", Component.literal(customName));
                    }
                    return Component.literal(customName);
                }
            }
        }

        Component baseComponent = Component.translatable(getVillagerKeyFromTag(tag));
        if (isBabyVillager(tag)) {
            return Component.translatable("villager.baby", baseComponent);
        }

        return baseComponent;
    }

    private static @Nullable Villager releaseVillager(Level level, ItemStack stack, BlockPos position) {
        return createCapturedVillager(level, stack, position);
    }

    public static boolean isBabyVillager(CompoundTag tag) {
        return tag.getInt("Age").map(age -> age < 0).orElse(false);
    }

    private static String getVillagerKeyFromTag(CompoundTag tag) {
        if (tag.contains("VillagerData")) {
            var villagerDataOpt = tag.getCompound("VillagerData");
            if (villagerDataOpt.isPresent()) {
                CompoundTag villagerData = villagerDataOpt.get();
                var professionIdOpt = villagerData.getString("profession");
                if (professionIdOpt.isPresent()) {
                    Identifier professionId = Identifier.tryParse(professionIdOpt.get());
                    if (professionId != null) {
                        return "entity."
                                + professionId.getNamespace()
                                + ".villager."
                                + professionId.getPath();
                    }
                }
            }
        }

        return "entity.minecraft.villager.none";
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
        entityData.remove("UUID");
        entityData.remove("UUIDMost");
        entityData.remove("UUIDLeast");
        entityData.remove("Passengers");
        entityData.remove("Leash");
    }
}
