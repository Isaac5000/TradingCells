package com.cosmocraft.trading_cells.feature.mobfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.mobfarm.domain.model.EssenceClassification;
import com.cosmocraft.trading_cells.feature.mobfarm.domain.model.EssenceTier;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
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
import net.minecraft.world.entity.boss.wither.WitherBoss;
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

    public static EssenceTier tier(ItemStack stack) {
        CompoundTag data = data(stack);
        return EssenceTier.fromId(data.getIntOr("Tier", data.getBooleanOr("HighLevel", false) ? 3 : 1));
    }
    public static boolean isHighLevel(ItemStack stack) { return tier(stack).id() >= 3; }
    public static int classificationVersion(ItemStack stack) { return data(stack).getIntOr("ClassificationVersion", 0); }
    public static double threatScore(ItemStack stack) { return data(stack).getDoubleOr("ThreatScore", 0); }
    public static boolean isCharged(ItemStack stack) {
        return data(stack).getCompound("State").map(state -> state.getBooleanOr("powered", false)).orElse(false);
    }

    public static Component displayName(ItemStack stack) {
        Identifier id = entityTypeId(stack);
        if (Identifier.withDefaultNamespace("creeper").equals(id) && isCharged(stack)) {
            return Component.translatable("entity.trading_cells.charged_creeper");
        }
        return id == null ? Component.translatable("item.trading_cells.entity_essence")
                : BuiltInRegistries.ENTITY_TYPE.getOptional(id).map(EntityType::getDescription)
                        .orElseGet(() -> Component.literal(id.toString()));
    }

    public static ItemStack essenceOf(@Nullable LivingEntity target) {
        if (target == null || target instanceof Player || !target.isAlive()) { return ItemStack.EMPTY; }
        try {
        ProblemReporter.Collector problems = new ProblemReporter.Collector();
        TagValueOutput output = TagValueOutput.createWithContext(problems, target.registryAccess());
        target.saveWithoutId(output);
        if (!problems.isEmpty()) { return ItemStack.EMPTY; }
        CompoundTag state = output.buildResult();
        sanitize(state);
        if (state.sizeInBytes() > MAX_DATA_BYTES) { return ItemStack.EMPTY; }
        CompoundTag essence = new CompoundTag();
        essence.putString("Type", BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString());
        writeClassification(essence, EssenceClassifier.classify(target, false));
        essence.put("State", state);
        CompoundTag root = new CompoundTag();
        root.put(ROOT, essence);
        ItemStack result = new ItemStack(MobFarmRegistrationAdapter.ENTITY_ESSENCE.get());
        result.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        return result;
        } catch (RuntimeException | LinkageError invalidEntity) {
            TradingCells.LOGGER.warn("Unable to capture essence for {}", BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()), invalidEntity);
            return ItemStack.EMPTY;
        }
    }

    public static ItemStack rawEssenceOf(LivingEntity target) {
        ItemStack core = essenceOf(target);
        return core.isEmpty() ? ItemStack.EMPTY : copyEssence(core, MobFarmRegistrationAdapter.RAW_ESSENCE.get());
    }

    public static ItemStack coreOf(ItemStack raw) {
        return raw.is(MobFarmRegistrationAdapter.RAW_ESSENCE.get()) && entityTypeId(raw) != null
                ? copyEssence(raw, MobFarmRegistrationAdapter.ENTITY_ESSENCE.get()) : ItemStack.EMPTY;
    }

    private static ItemStack copyEssence(ItemStack source, net.minecraft.world.item.Item item) {
        ItemStack result = new ItemStack(item);
        CompoundTag root = new CompoundTag();
        root.put(ROOT, data(source));
        result.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        return result;
    }

    private static void writeClassification(CompoundTag data, EssenceClassification.Result classification) {
        data.putInt("Tier", classification.tier().id());
        data.putDouble("ThreatScore", classification.score());
        data.putInt("ClassificationVersion", classification.version());
        data.putBoolean("HighLevel", classification.tier().id() >= 3);
    }

    public static boolean ensureClassified(ServerLevel level, ItemStack stack) {
        return entityTypeId(stack) != null && createEntity(level, stack) != null;
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
                && !stack.is(MobFarmRegistrationAdapter.RAW_ESSENCE.get())
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
            if (living instanceof WitherBoss wither) {
                // A detached preview must represent the post-spawn Wither, not its
                // temporary 220-tick construction state.
                wither.setInvulnerableTicks(0);
            }
            if (level instanceof ServerLevel && (essence.getIntOr("ClassificationVersion", 0) < 1
                    || essence.getIntOr("Tier", 0) < 1 || essence.getIntOr("Tier", 0) > 4)) {
                writeClassification(essence, EssenceClassifier.classify(living, essence.getBooleanOr("HighLevel", false)));
                CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                root.put(ROOT, essence);
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
            }
            return living;
        } catch (RuntimeException | LinkageError invalidEntity) {
            return null;
        }
    }

    private static void sanitize(CompoundTag state) {
        VOLATILE.forEach(state::remove);
    }
}
