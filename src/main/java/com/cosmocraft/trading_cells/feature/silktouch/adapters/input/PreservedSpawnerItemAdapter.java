package com.cosmocraft.trading_cells.feature.silktouch.adapters.input;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.cosmocraft.trading_cells.feature.silktouch.adapters.mixin.BaseSpawnerAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.TagValueInput;
import org.jspecify.annotations.Nullable;

/** Converts trusted Silk Touch II spawners into faithful, non-duplicating spawn eggs. */
public final class PreservedSpawnerItemAdapter {
    private static final String TRUSTED_EGG_MARKER = "trading_cells:preserved_spawner_egg";
    private static final String ROOT_ENTITY_DATA = "trading_cells:root_entity";
    private static final String SELECTED_ENTITY_PATH = "trading_cells:selected_entity_path";
    private static final TagKey<EntityType<?>> HUMANOID_ARMOR_MODELS = TagKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, "humanoid_armor_models")
    );

    private PreservedSpawnerItemAdapter() {
    }

    public static boolean isTrustedSpawner(ItemStack stack) {
        if (!stack.is(Items.SPAWNER) || !SilkTouchTwoDropAdapter.hasPreservedDataMarker(stack)) {
            return false;
        }
        TypedEntityData<BlockEntityType<?>> blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        return blockEntityData != null
                && blockEntityData.type() == BlockEntityTypes.MOB_SPAWNER
                && rootEntityData(stack).isPresent();
    }

    public static boolean isTrustedEgg(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return stack.getItem() instanceof SpawnEggItem
                && customData != null
                && customData.copyTag().getBoolean(TRUSTED_EGG_MARKER).orElse(false)
                && trustedEggRootEntityData(stack).isPresent();
    }

    public static Optional<CompoundTag> rootEntityData(ItemStack stack) {
        TypedEntityData<BlockEntityType<?>> blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockEntityData == null || blockEntityData.type() != BlockEntityTypes.MOB_SPAWNER) {
            return Optional.empty();
        }
        return rootEntityData(blockEntityData.copyTagWithoutId());
    }

    /** Reads the configured entity from a mob-spawner block entity tag. */
    public static Optional<CompoundTag> rootEntityData(CompoundTag blockEntityData) {
        return blockEntityData
                .getCompound("SpawnData")
                .flatMap(spawnData -> spawnData.getCompound("entity"))
                .filter(PreservedSpawnerItemAdapter::hasEntityId)
                .map(CompoundTag::copy);
    }

    public static Optional<CompoundTag> trustedEggRootEntityData(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return Optional.empty();
        }
        CompoundTag customTag = customData.copyTag();
        if (!customTag.getBoolean(TRUSTED_EGG_MARKER).orElse(false)) {
            return Optional.empty();
        }
        return customTag.getCompound(ROOT_ENTITY_DATA)
                .filter(PreservedSpawnerItemAdapter::hasEntityId)
                .map(CompoundTag::copy);
    }

    public static Optional<ItemStack> createSpawnEgg(Level level, ItemStack spawnerStack) {
        return rootEntityData(spawnerStack).flatMap(rootData -> createSpawnEgg(level, rootData));
    }

    /** Atomically removes the configured entity from a placed spawner and returns its egg. */
    public static Optional<ItemStack> extractSpawnEgg(ServerLevel level, SpawnerBlockEntity spawner) {
        CompoundTag blockEntityData = spawner.saveCustomOnly(level.registryAccess());
        Optional<ItemStack> egg = rootEntityData(blockEntityData)
                .flatMap(rootData -> createSpawnEgg(level, rootData));
        if (egg.isEmpty()) {
            return Optional.empty();
        }

        CompoundTag spawnData = blockEntityData.getCompound("SpawnData").orElseGet(CompoundTag::new);
        spawnData.put("entity", new CompoundTag());
        blockEntityData.put("SpawnData", spawnData);
        blockEntityData.remove("SpawnPotentials");
        spawner.loadCustomOnly(TagValueInput.create(
                ProblemReporter.DISCARDING,
                level.registryAccess(),
                blockEntityData
        ));
        ((BaseSpawnerAccessor) (Object) spawner.getSpawner()).tradingCells$setDisplayEntity(null);
        spawner.setChanged();
        level.sendBlockUpdated(
                spawner.getBlockPos(),
                spawner.getBlockState(),
                spawner.getBlockState(),
                3
        );
        return egg;
    }

    /** Creates the appropriate spawn egg from a configured spawner entity tree. */
    public static Optional<ItemStack> createSpawnEgg(Level level, CompoundTag rootData) {
        CompoundTag sanitizedRoot = sanitizeEntityTree(rootData);
        Entity root = loadEntity(level, sanitizedRoot);
        if (root == null) {
            return Optional.empty();
        }

        EntityNode hierarchy = buildHierarchy(root, sanitizedRoot, List.of());
        EntityNode selected = selectEntity(hierarchy);
        Optional<net.minecraft.core.Holder<Item>> eggItem = SpawnEggItem.byId(selected.entity().getType());
        if (eggItem.isEmpty()) {
            return Optional.empty();
        }

        ItemStack egg = new ItemStack(eggItem.orElseThrow().value());
        if (isPlainEntity(hierarchy)) {
            return Optional.of(egg);
        }

        egg.set(
                DataComponents.ENTITY_DATA,
                TypedEntityData.of(selected.entity().getType(), selected.sourceData().copy())
        );
        egg.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);

        CompoundTag customTag = new CompoundTag();
        customTag.putBoolean(TRUSTED_EGG_MARKER, true);
        customTag.put(ROOT_ENTITY_DATA, sanitizedRoot);
        customTag.putIntArray(
                SELECTED_ENTITY_PATH,
                selected.path().stream().mapToInt(Integer::intValue).toArray()
        );
        egg.set(DataComponents.CUSTOM_DATA, CustomData.of(customTag));
        return Optional.of(egg);
    }

    public static @Nullable Entity createDisplayEntity(Level level, ItemStack stack) {
        Optional<CompoundTag> rootData = isTrustedEgg(stack)
                ? trustedEggRootEntityData(stack)
                : rootEntityData(stack);
        if (rootData.isEmpty()) {
            return null;
        }

        Entity root = loadEntity(level, rootData.orElseThrow());
        if (root == null) {
            return null;
        }

        EntityNode hierarchy = buildHierarchy(root, rootData.orElseThrow(), List.of());
        if (isTrustedEgg(stack)) {
            int[] path = stack.get(DataComponents.CUSTOM_DATA)
                    .copyTag()
                    .getIntArray(SELECTED_ENTITY_PATH)
                    .orElse(new int[0]);
            return findByPath(hierarchy, path).map(EntityNode::entity).orElse(root);
        }
        return selectEntity(hierarchy).entity();
    }

    /** Loads the complete preserved hierarchy for static spawner item previews. */
    public static @Nullable Entity createRootDisplayEntity(Level level, ItemStack stack) {
        Optional<CompoundTag> rootData = rootEntityData(stack);
        return rootData.map(data -> loadEntity(level, data)).orElse(null);
    }

    public static Optional<EntityDescription> describe(Level level, ItemStack stack) {
        Optional<CompoundTag> rootData = isTrustedEgg(stack)
                ? trustedEggRootEntityData(stack)
                : rootEntityData(stack);
        if (rootData.isEmpty()) {
            return Optional.empty();
        }

        return describeEntityTree(level, rootData.orElseThrow());
    }

    /** Describes an active spawner entity tree without requiring an item stack. */
    public static Optional<EntityDescription> describeEntityTree(Level level, CompoundTag rootData) {
        if (!hasEntityId(rootData)) {
            return Optional.empty();
        }

        Entity root = loadEntity(level, rootData);
        if (root == null) {
            return Optional.empty();
        }
        EntityNode hierarchy = buildHierarchy(root, rootData, List.of());
        EntityNode selected = selectEntity(hierarchy);

        List<EntityType<?>> passengers = selected.entity().getIndirectPassengers().iterator().hasNext()
                ? collectPassengers(selected.entity())
                : List.of();
        Entity vehicle = selected.entity().getVehicle();
        return Optional.of(new EntityDescription(
                root.getType(),
                selected.entity(),
                selected.entity() instanceof LivingEntity living ? living.getMainHandItem().copy() : ItemStack.EMPTY,
                !isPlainEntity(hierarchy),
                passengers,
                vehicle == null ? null : vehicle.getType()
        ));
    }

    public static InteractionResult spawnTrustedEgg(
            @Nullable LivingEntity user,
            ItemStack egg,
            ServerLevel level,
            BlockPos spawnPos,
            boolean tryMoveDown,
            boolean movedUp
    ) {
        Optional<CompoundTag> storedRoot = trustedEggRootEntityData(egg);
        if (storedRoot.isEmpty()) {
            return InteractionResult.PASS;
        }

        Entity root = loadEntity(level, storedRoot.orElseThrow());
        if (root == null || !root.getType().canSpawn(level)) {
            return InteractionResult.FAIL;
        }

        double y = spawnPos.getY() + (tryMoveDown && movedUp ? 1.0D : 0.0D);
        float yaw = level.getRandom().nextFloat() * 360.0F;
        root.getSelfAndPassengers().forEach(entity ->
                entity.snapTo(spawnPos.getX() + 0.5D, y, spawnPos.getZ() + 0.5D, yaw, 0.0F)
        );
        if (!level.tryAddFreshEntityWithPassengers(root)) {
            return InteractionResult.FAIL;
        }

        egg.consume(1, user);
        level.gameEvent(user, GameEvent.ENTITY_PLACE, spawnPos);
        return InteractionResult.SUCCESS;
    }

    private static boolean isPlainEntity(EntityNode hierarchy) {
        return hierarchy.children().isEmpty()
                && hierarchy.sourceData().size() == 1
                && hasEntityId(hierarchy.sourceData());
    }

    private static EntityNode selectEntity(EntityNode root) {
        List<EntityNode> riderFirst = new ArrayList<>();
        collectRiderFirst(root, riderFirst);
        for (EntityNode node : riderFirst) {
            if (node.entity().typeHolder().is(HUMANOID_ARMOR_MODELS)) {
                return node;
            }
        }
        return riderFirst.isEmpty() ? root : riderFirst.getFirst();
    }

    private static void collectRiderFirst(EntityNode node, List<EntityNode> output) {
        for (EntityNode child : node.children()) {
            collectRiderFirst(child, output);
        }
        output.add(node);
    }

    private static EntityNode buildHierarchy(Entity entity, CompoundTag sourceData, List<Integer> path) {
        ListTag passengerTags = sourceData.getListOrEmpty("Passengers");
        List<Entity> passengers = entity.getPassengers();
        int childCount = Math.min(passengerTags.size(), passengers.size());
        List<EntityNode> children = new ArrayList<>(childCount);
        for (int index = 0; index < childCount; index++) {
            if (!(passengerTags.get(index) instanceof CompoundTag passengerData)) {
                continue;
            }
            List<Integer> childPath = new ArrayList<>(path.size() + 1);
            childPath.addAll(path);
            childPath.add(index);
            children.add(buildHierarchy(passengers.get(index), passengerData, List.copyOf(childPath)));
        }
        return new EntityNode(entity, sourceData, List.copyOf(path), List.copyOf(children));
    }

    private static Optional<EntityNode> findByPath(EntityNode root, int[] path) {
        EntityNode current = root;
        for (int index : path) {
            if (index < 0 || index >= current.children().size()) {
                return Optional.empty();
            }
            current = current.children().get(index);
        }
        return Optional.of(current);
    }

    private static List<EntityType<?>> collectPassengers(Entity entity) {
        List<EntityType<?>> passengers = new ArrayList<>();
        for (Entity passenger : entity.getIndirectPassengers()) {
            passengers.add(passenger.getType());
        }
        return List.copyOf(passengers);
    }

    private static @Nullable Entity loadEntity(Level level, CompoundTag data) {
        return EntityType.loadEntityRecursive(
                data.copy(),
                level,
                new EntitySpawnRequest(EntitySpawnReason.LOAD, true),
                entity -> entity
        );
    }

    private static CompoundTag sanitizeEntityTree(CompoundTag source) {
        CompoundTag sanitized = source.copy();
        sanitized.remove("UUID");
        sanitized.remove("Pos");
        sanitized.remove("Motion");
        sanitized.remove("Rotation");
        sanitized.remove("PortalCooldown");
        ListTag passengers = sanitized.getListOrEmpty("Passengers");
        if (!passengers.isEmpty()) {
            ListTag sanitizedPassengers = new ListTag();
            for (int index = 0; index < passengers.size(); index++) {
                if (passengers.get(index) instanceof CompoundTag passenger) {
                    sanitizedPassengers.add(sanitizeEntityTree(passenger));
                }
            }
            sanitized.put("Passengers", sanitizedPassengers);
        }
        return sanitized;
    }

    private static boolean hasEntityId(CompoundTag data) {
        String rawId = data.getStringOr("id", "");
        Identifier id = Identifier.tryParse(rawId);
        return id != null && BuiltInRegistries.ENTITY_TYPE.getOptional(id).isPresent();
    }

    private record EntityNode(
            Entity entity,
            CompoundTag sourceData,
            List<Integer> path,
            List<EntityNode> children
    ) {
    }

    public record EntityDescription(
            EntityType<?> rootType,
            Entity selected,
            ItemStack weapon,
            boolean modified,
            List<EntityType<?>> passengers,
            @Nullable EntityType<?> mount
    ) {
        public EntityDescription {
            weapon = weapon.copy();
            passengers = List.copyOf(passengers);
        }
    }
}
