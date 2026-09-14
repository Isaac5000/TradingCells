package com.cosmocraft.trading_cells.feature.mobfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.TagValueInput;
import org.jspecify.annotations.Nullable;

/** A reusable simulation snapshot, never a live entity or a spawnable captured inventory. */
public final class EntityEssenceData {
    private static final String ROOT = "TradingCellsEssence";
    private static final int MAX_DATA_BYTES = 65_536;
    private static final TagKey<EntityType<?>> HIGH_LEVEL = TagKey.create(Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath("trading_cells", "high_level_essence"));
    private static final List<String> VOLATILE = List.of("UUID", "Pos", "Motion", "Rotation", "Passengers",
            "Leash", "leash", "Brain", "Inventory", "Items", "EnderItems", "Offers", "Owner", "OwnerUUID",
            "RootVehicle", "PortalCooldown", "DeathTime", "HurtTime", "HurtByTimestamp", "Fire", "Air");

    private EntityEssenceData() { }

    private static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
                .getCompound(ROOT).orElseGet(CompoundTag::new);
    }

    public static @Nullable Identifier entityTypeId(ItemStack stack) {
        return Identifier.tryParse(data(stack).getStringOr("Type", ""));
    }

    public static boolean isHighLevel(ItemStack stack) { return data(stack).getBooleanOr("HighLevel", false); }

    public static Component displayName(ItemStack stack) {
        Identifier id = entityTypeId(stack);
        return id == null ? Component.translatable("item.trading_cells.entity_essence")
                : BuiltInRegistries.ENTITY_TYPE.getOptional(id).map(EntityType::getDescription)
                        .orElseGet(() -> Component.literal(id.toString()));
    }

    public static ItemStack essenceOf(@Nullable LivingEntity target) {
        if (target == null || target instanceof Player || !target.isAlive()) { return ItemStack.EMPTY; }
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, target.registryAccess());
        target.saveWithoutId(output);
        CompoundTag state = output.buildResult();
        sanitize(state);
        if (state.sizeInBytes() > MAX_DATA_BYTES) { return ItemStack.EMPTY; }
        CompoundTag essence = new CompoundTag();
        essence.putString("Type", BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString());
        essence.putBoolean("HighLevel", target.getMaxHealth() >= 80.0F || target.typeHolder().is(HIGH_LEVEL));
        essence.put("State", state);
        CompoundTag root = new CompoundTag();
        root.put(ROOT, essence);
        ItemStack result = new ItemStack(MobFarmRegistrationAdapter.ENTITY_ESSENCE.get());
        result.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        return result;
    }

    public static ItemStack moduleOf(ItemStack essence) {
        if (!essence.is(MobFarmRegistrationAdapter.ENTITY_ESSENCE.get()) || entityTypeId(essence) == null) {
            return ItemStack.EMPTY;
        }
        ItemStack result = new ItemStack(MobFarmRegistrationAdapter.ENTITY_MODULE.get());
        CompoundTag root = new CompoundTag();
        root.put(ROOT, data(essence));
        result.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        return result;
    }

    public static ItemStack moduleFor(ServerLevel level, Identifier type) {
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(type).orElse(null);
        if (entityType == null || !entityType.isEnabled(level.enabledFeatures())) { return ItemStack.EMPTY; }
        var entity = entityType.create(level, new EntitySpawnRequest(EntitySpawnReason.LOAD, true));
        return entity instanceof LivingEntity living ? moduleOf(essenceOf(living)) : ItemStack.EMPTY;
    }

    public static @Nullable LivingEntity createEntity(Level level, ItemStack stack) {
        if (!stack.is(MobFarmRegistrationAdapter.ENTITY_MODULE.get())
                && !stack.is(MobFarmRegistrationAdapter.ENTITY_ESSENCE.get())) { return null; }
        CompoundTag essence = data(stack);
        Identifier id = Identifier.tryParse(essence.getStringOr("Type", ""));
        if (id == null) { return null; }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        if (type == null || !type.isEnabled(level.enabledFeatures())) { return null; }
        CompoundTag state = essence.getCompound("State").orElseGet(CompoundTag::new).copy();
        if (state.sizeInBytes() > MAX_DATA_BYTES) { return null; }
        sanitize(state);
        try {
            // Detached simulations are allowed in peaceful; they are never added to the world.
            var entity = type.create(level, new EntitySpawnRequest(EntitySpawnReason.LOAD, true));
            if (!(entity instanceof LivingEntity living) || living instanceof Player) { return null; }
            living.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), state));
            living.setHealth(living.getMaxHealth());
            return living;
        } catch (RuntimeException | LinkageError invalidEntity) {
            return null;
        }
    }

    private static void sanitize(CompoundTag state) {
        VOLATILE.forEach(state::remove);
    }
}
