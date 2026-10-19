package com.cosmocraft.trading_cells.feature.mobfarm.adapters.interaction;

import com.mojang.serialization.Lifecycle;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.LevelTicks;
import org.jspecify.annotations.Nullable;

/** Never registered as a dimension. No chunks, entities or scheduled ticks reach a playable world. */
public final class DisposableInteractionLevel extends ServerLevel {
    public static final BlockPos ORIGIN = new BlockPos(0, 64, 0);
    private final Path temporaryRoot;
    private final LevelStorageSource.LevelStorageAccess storage;
    private final Map<BlockPos, BlockState> blocks = new HashMap<>();
    private final Map<Long, EmptyLevelChunk> emptyChunks = new HashMap<>();
    private final List<ItemStack> drops = new ArrayList<>();
    private GameRules localRules;
    private FeatureFlagSet localFeatures;

    private DisposableInteractionLevel(ServerLevel source, Path root, LevelStorageSource.LevelStorageAccess storage) {
        super(source.getServer(), Runnable::run, storage,
                createLevelData(),
                ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath("trading_cells", "interaction_sandbox")),
                new LevelStem(source.registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE).getOrThrow(
                        ResourceKey.create(Registries.DIMENSION_TYPE, Identifier.withDefaultNamespace("overworld"))),
                        new FlatLevelSource(new FlatLevelGeneratorSettings(
                        Optional.of(HolderSet.direct()), source.registryAccess().lookupOrThrow(Registries.BIOME)
                                .getOrThrow(Biomes.PLAINS), List.of()))), false, 0, List.of(), false);
        temporaryRoot = root;
        this.storage = storage;
        localFeatures = source.enabledFeatures();
        localRules = source.getGameRules().copy(enabledFeatures());
        noSave = true;
    }

    // The current PrimaryLevelData constructor still requires this deprecated enum.
    @SuppressWarnings("deprecation")
    private static PrimaryLevelData createLevelData() {
        return new PrimaryLevelData(new LevelSettings("interaction", GameType.SURVIVAL,
                LevelSettings.DifficultySettings.DEFAULT, false, WorldDataConfiguration.DEFAULT),
                PrimaryLevelData.SpecialWorldProperty.FLAT, Lifecycle.stable());
    }

    public static DisposableInteractionLevel create(ServerLevel source) throws IOException {
        Path root = Files.createTempDirectory("trading-cells-interactions-");
        LevelStorageSource.LevelStorageAccess access = null;
        try {
            access = LevelStorageSource.createDefault(root).createAccess("disposable");
            return new DisposableInteractionLevel(source, root, access);
        } catch (IOException | RuntimeException | Error failure) {
            try { closeAndDelete(access == null ? () -> { } : access::close, root); }
            catch (IOException | RuntimeException | Error cleanupFailure) { failure.addSuppressed(cleanupFailure); }
            throw failure;
        }
    }
    public void reset(ServerLevel source) {
        finishAttempt();
        localRules = source.getGameRules().copy(enabledFeatures());
    }
    public void finishAttempt() {
        blocks.clear(); drops.clear(); emptyChunks.clear();
        // The next attempt copies current source rules; cleanup needs no replacement copy.
        localRules = null;
    }
    public List<ItemStack> capturedDrops() { return drops.stream().map(ItemStack::copy).toList(); }
    public void placeTemporaryBlock(BlockPos pos, BlockState state) { blocks.put(pos.immutable(), state); }
    @Override public BlockState getBlockState(BlockPos pos) { return blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState()); }
    @Override public FluidState getFluidState(BlockPos pos) { return getBlockState(pos).getFluidState(); }
    @Override public @Nullable BlockEntity getBlockEntity(BlockPos pos) { return null; }
    @Override public boolean setBlock(BlockPos pos, BlockState state, int flags, int recursion) {
        if (!blocks.containsKey(pos)) { InteractionIsolation.deny(); }
        blocks.put(pos.immutable(), state);
        return true;
    }
    @Override public ChunkAccess getChunk(int x, int z, ChunkStatus status, boolean required) {
        if (emptyChunks.size() >= 32 && !emptyChunks.containsKey(ChunkPos.pack(x, z))) { InteractionIsolation.deny(); }
        return emptyChunks.computeIfAbsent(ChunkPos.pack(x, z), ignored -> new EmptyLevelChunk(this, new ChunkPos(x, z),
                registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS)));
    }
    @Override public boolean addFreshEntity(Entity entity) {
        if (!(entity instanceof ItemEntity item) || entity.level() != this || drops.size() >= 256) { InteractionIsolation.deny(); }
        ItemStack stack = ((ItemEntity) entity).getItem();
        if (!stack.isEmpty()) { drops.add(stack.copy()); }
        entity.discard();
        return true;
    }
    @Override public boolean addWithUUID(Entity entity) { return addFreshEntity(entity); }
    @Override public void setBlockEntity(BlockEntity entity) { InteractionIsolation.deny(); }
    @Override public void removeBlockEntity(BlockPos pos) { InteractionIsolation.deny(); }
    @Override public LevelTicks<Block> getBlockTicks() { InteractionIsolation.deny(); return null; }
    @Override public LevelTicks<Fluid> getFluidTicks() { InteractionIsolation.deny(); return null; }
    @Override public void blockEvent(BlockPos pos, Block block, int a, int b) { InteractionIsolation.deny(); }
    @Override public GameRules getGameRules() { return localRules == null ? super.getGameRules() : localRules; }
    @Override public FeatureFlagSet enabledFeatures() { return localFeatures == null ? super.enabledFeatures() : localFeatures; }
    @Override public boolean isFlat() { return true; }
    @Override public void sendBlockUpdated(BlockPos pos, BlockState old, BlockState current, int flags) { }
    @Override public void gameEvent(Holder<GameEvent> event, Vec3 pos, GameEvent.Context context) { }
    @Override public void levelEvent(@Nullable Entity source, int type, BlockPos pos, int data) { }
    @Override public void globalLevelEvent(int type, BlockPos pos, int data) { }
    @Override public void playSeededSound(@Nullable Entity except, double x, double y, double z,
            Holder<SoundEvent> sound, SoundSource source, float volume, float pitch, long seed) { }
    @Override public void playSeededSound(@Nullable Entity except, Entity entity,
            Holder<SoundEvent> sound, SoundSource source, float volume, float pitch, long seed) { }
    @Override public void close() throws IOException {
        try (Closeable cleanup = () -> closeAndDelete(storage::close, temporaryRoot)) {
            super.close();
        }
    }
    private static void closeAndDelete(Closeable storage, Path root) throws IOException {
        // Preserve the close failure and still attempt deletion; suppress secondary failures.
        try (Closeable cleanup = () -> deleteOwnedDirectory(root)) {
            storage.close();
        }
    }
    private static void deleteOwnedDirectory(Path root) throws IOException {
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) { Files.deleteIfExists(path); }
        }
    }
}
