package com.cosmocraft.trading_cells.performance;

import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import jdk.jfr.Configuration;
import jdk.jfr.Recording;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.common.Mod;

/** Development-only frame recorder. This source set is excluded from the published JAR. */
@Mod(value = TradingCellsPerformanceClient.MOD_ID, dist = Dist.CLIENT)
public final class TradingCellsPerformanceClient {
    public static final String MOD_ID = "trading_cells_performance";
    private static final String OUTPUT_PROPERTY = "trading_cells.performance.client.output";
    private static final String WARMUP_PROPERTY = "trading_cells.performance.client.warmupSeconds";
    private static final String MEASURE_PROPERTY = "trading_cells.performance.client.measureSeconds";
    private static final String SCENARIO_PROPERTY = "trading_cells.performance.client.scenario";
    private static final String WIDTH_PROPERTY = "trading_cells.performance.client.width";
    private static final String HEIGHT_PROPERTY = "trading_cells.performance.client.height";
    private static final String REQUIRE_WORLD_PROPERTY = "trading_cells.performance.client.requireWorld";
    private static final String CAMERA_PROPERTY = "trading_cells.performance.client.camera";
    private static final String OPEN_BLOCK_PROPERTY = "trading_cells.performance.client.openBlock";
    private static final String JFR_PROPERTY = "trading_cells.performance.client.jfr";
    private static final int MAX_SAMPLES = 1_000_000;

    private final long warmupNanos;
    private final long measureNanos;
    private final Path outputDirectory;
    private final String scenario;
    private final int expectedWidth;
    private final int expectedHeight;
    private final boolean requireWorld;
    private final CameraPose cameraPose;
    private final BlockPos openBlock;
    private final Path jfrPath;
    private final long[] samples = new long[MAX_SAMPLES];
    private long startedAt;
    private long measurementStartedAt;
    private long captureAtNanos;
    private long shutdownAtNanos;
    private int sampleCount;
    private boolean completed;
    private boolean captureRequested;
    private boolean secondMaterialCapture;
    private Recording recording;
    private long firstOpenAttempt;
    private long lastOpenAttempt;
    private volatile boolean serverCameraPositioned;
    private volatile boolean serverCameraRequested;

    public TradingCellsPerformanceClient() {
        String output = System.getProperty(OUTPUT_PROPERTY, "").trim();
        outputDirectory = output.isEmpty() ? null : Path.of(output).toAbsolutePath();
        warmupNanos = secondsProperty(WARMUP_PROPERTY, 15.0D);
        measureNanos = secondsProperty(MEASURE_PROPERTY, 30.0D);
        scenario = System.getProperty(SCENARIO_PROPERTY, "unspecified");
        expectedWidth = positiveIntProperty(WIDTH_PROPERTY, 1_920);
        expectedHeight = positiveIntProperty(HEIGHT_PROPERTY, 1_080);
        requireWorld = Boolean.parseBoolean(System.getProperty(REQUIRE_WORLD_PROPERTY, "false"));
        cameraPose = CameraPose.parse(System.getProperty(CAMERA_PROPERTY, ""));
        openBlock = parseBlockPos(System.getProperty(OPEN_BLOCK_PROPERTY, ""));
        String jfr = System.getProperty(JFR_PROPERTY, "").trim();
        jfrPath = jfr.isEmpty() ? null : Path.of(jfr).toAbsolutePath();
        if (outputDirectory != null) {
            NeoForge.EVENT_BUS.addListener(this::onFramePre);
            NeoForge.EVENT_BUS.addListener(this::onFramePost);
        }
    }

    private void onFramePre(RenderFrameEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!System.getProperty("trading_cells.performance.client.uiFixture", "").isEmpty()) {
            LogisticsUiFixture.inspect(minecraft);
        }
        enforceWindowSize(minecraft);
        if (cameraPose == null || minecraft.player == null) {
            openConfiguredBlock(minecraft);
            return;
        }
        positionServerCamera(minecraft);
        cameraPose.apply(minecraft.player);
        openConfiguredBlock(minecraft);
    }

    private void positionServerCamera(Minecraft minecraft) {
        var server = minecraft.getSingleplayerServer();
        if (serverCameraPositioned || serverCameraRequested || server == null) {
            return;
        }
        serverCameraRequested = true;
        var playerId = minecraft.player.getUUID();
        server.execute(() -> {
            var player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                player.connection.teleport(cameraPose.position.x, cameraPose.position.y, cameraPose.position.z,
                        cameraPose.yaw, cameraPose.pitch);
                serverCameraPositioned = true;
            }
            serverCameraRequested = false;
        });
    }

    private void onFramePost(RenderFrameEvent.Post event) {
        if (completed) {
            long now = System.nanoTime();
            if (!captureRequested && now >= captureAtNanos) {
                Screenshot.grab(Minecraft.getInstance(), false);
                captureRequested = true;
                shutdownAtNanos = now + 2_000_000_000L;
            } else if (captureRequested && !secondMaterialCapture
                    && (System.getProperty("trading_cells.performance.client.uiFixture", "").equals("materials")
                        || LogisticsUiFixture.worldScene() || MobSimulationUiFixture.modelScene())
                    && now >= shutdownAtNanos - 1_400_000_000L) {
                Screenshot.grab(Minecraft.getInstance(), false);
                secondMaterialCapture = true;
            } else if (captureRequested && now >= shutdownAtNanos) {
                Minecraft.getInstance().stop();
            }
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (requireWorld && (minecraft.level == null
                || minecraft.player == null
                || minecraft.gui.screen() instanceof LevelLoadingScreen)) {
            return;
        }
        if (openBlock != null && !LogisticsUiFixture.worldScene() && !isConfiguredBlockScreenOpen(minecraft)) {
            return;
        }
        if (!System.getProperty("trading_cells.performance.client.uiFixture", "").isEmpty()
                && !LogisticsUiFixture.readyForCapture()) { return; }
        long now = System.nanoTime();
        if (startedAt == 0L) {
            startedAt = now;
        }
        if (now - startedAt < warmupNanos) {
            return;
        }
        if (measurementStartedAt == 0L) {
            startRecording();
            measurementStartedAt = System.nanoTime();
            return;
        }

        long frameTime = Minecraft.getInstance().getFrameTimeNs();
        if (frameTime > 0L && sampleCount < samples.length) {
            samples[sampleCount++] = frameTime;
        }
        if (now - measurementStartedAt >= measureNanos || sampleCount == samples.length) {
            completed = true;
            stopRecording();
            writeResults(now - measurementStartedAt);
            captureAtNanos = now + 1_000_000_000L;
        }
    }

    private void writeResults(long measuredNanos) {
        if (sampleCount == 0) {
            throw new IllegalStateException("No client frame samples were recorded");
        }
        long[] ordered = Arrays.copyOf(samples, sampleCount);
        Arrays.sort(ordered);
        double totalNanos = 0.0D;
        for (long sample : ordered) {
            totalNanos += sample;
        }
        double meanMs = totalNanos / sampleCount / 1_000_000.0D;
        double measuredSeconds = measuredNanos / 1_000_000_000.0D;
        Minecraft minecraft = Minecraft.getInstance();
        String backend = RenderSystem.getDevice().getDeviceInfo().backendName().replace(',', ';');
        String header = "scenario,backend,frames,measured_seconds,mean_frame_ms,p50_frame_ms,p95_frame_ms,p99_frame_ms,frames_per_second,width,height,screen_class";
        String screenClass = minecraft.gui.screen() == null
                ? ""
                : minecraft.gui.screen().getClass().getName().replace(',', ';');
        String row = String.format(
                Locale.ROOT,
                "%s,%s,%d,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%d,%d,%s",
                scenario,
                backend,
                sampleCount,
                measuredSeconds,
                meanMs,
                percentile(ordered, 0.50D) / 1_000_000.0D,
                percentile(ordered, 0.95D) / 1_000_000.0D,
                percentile(ordered, 0.99D) / 1_000_000.0D,
                sampleCount / measuredSeconds,
                minecraft.getWindow().getWidth(),
                minecraft.getWindow().getHeight(),
                screenClass
        );
        try {
            Files.createDirectories(outputDirectory);
            Files.writeString(
                    outputDirectory.resolve("summary.csv"),
                    header + System.lineSeparator() + row + System.lineSeparator(),
                    StandardCharsets.UTF_8
            );
            StringBuilder raw = new StringBuilder("frame,frame_time_ns\n");
            for (int index = 0; index < sampleCount; index++) {
                raw.append(index + 1).append(',').append(samples[index]).append('\n');
            }
            Files.writeString(outputDirectory.resolve("frames.csv"), raw, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not write client performance results", exception);
        }
    }

    private void startRecording() {
        if (jfrPath == null) {
            return;
        }
        try {
            Files.createDirectories(jfrPath.getParent());
            recording = new Recording(Configuration.getConfiguration("profile"));
            recording.setDestination(jfrPath);
            recording.setToDisk(true);
            recording.start();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not start client JFR recording", exception);
        }
    }

    private void stopRecording() {
        if (recording == null) {
            return;
        }
        recording.stop();
        recording.close();
        recording = null;
    }

    private static long percentile(long[] ordered, double fraction) {
        int index = (int) Math.ceil((ordered.length - 1) * fraction);
        return ordered[Math.clamp(index, 0, ordered.length - 1)];
    }

    private static long secondsProperty(String key, double fallback) {
        double seconds = Double.parseDouble(System.getProperty(key, Double.toString(fallback)));
        if (!Double.isFinite(seconds) || seconds < 0.0D) {
            throw new IllegalArgumentException(key + " must be a finite non-negative number");
        }
        return (long) (seconds * 1_000_000_000.0D);
    }

    private static int positiveIntProperty(String key, int fallback) {
        int value = Integer.parseInt(System.getProperty(key, Integer.toString(fallback)));
        if (value <= 0) {
            throw new IllegalArgumentException(key + " must be a positive integer");
        }
        return value;
    }

    private void enforceWindowSize(Minecraft minecraft) {
        if (!minecraft.getWindow().isFullscreen()
                && (minecraft.getWindow().getWidth() != expectedWidth
                || minecraft.getWindow().getHeight() != expectedHeight)) {
            minecraft.getWindow().setWindowed(expectedWidth, expectedHeight);
        }
    }

    private void openConfiguredBlock(Minecraft minecraft) {
        if (LogisticsUiFixture.worldScene() && LogisticsUiFixture.readyForCapture()) { return; }
        if (openBlock == null
                || minecraft.level == null
                || minecraft.player == null
                || minecraft.gameMode == null
                || minecraft.gui.screen() != null) {
            return;
        }
        var chunk = minecraft.level.getChunk(
                openBlock.getX() >> 4,
                openBlock.getZ() >> 4,
                ChunkStatus.FULL,
                false
        );
        if (chunk == null || chunk.getBlockState(openBlock).isAir() && !LogisticsUiFixture.worldScene()) {
            return;
        }
        long now = System.nanoTime();
        if (firstOpenAttempt == 0L) {
            firstOpenAttempt = now;
        }
        if (now - firstOpenAttempt > 30_000_000_000L) {
            throw new IllegalStateException("Configured block menu did not open within 30 seconds: " + openBlock);
        }
        if (now - lastOpenAttempt < 1_000_000_000L) {
            return;
        }
        lastOpenAttempt = now;
        var server = minecraft.getSingleplayerServer();
        if (server != null) {
            var playerId = minecraft.player.getUUID();
            server.execute(() -> {
                var player = server.getPlayerList().getPlayer(playerId);
                if (player == null || player.containerMenu != player.inventoryMenu
                        || !LogisticsUiFixture.worldScene() && player.distanceToSqr(Vec3.atCenterOf(openBlock)) > 64) {
                    return;
                }
                var level = (net.minecraft.server.level.ServerLevel) player.level();
                if (!System.getProperty("trading_cells.performance.client.uiFixture", "").isEmpty()) {
                    LogisticsUiFixture.prepare(player, openBlock);
                    if (LogisticsUiFixture.worldScene()) { return; }
                }
                var loaded = level.getChunkSource().getChunkNow(openBlock.getX() >> 4, openBlock.getZ() >> 4);
                if (loaded != null && net.neoforged.fml.ModList.get().isLoaded("trading_cells")
                        && LogisticsUiFixture.openPipe(player, loaded.getBlockEntity(openBlock))) { return; }
                if (loaded != null && loaded.getBlockEntity(openBlock) instanceof net.minecraft.world.MenuProvider menu) {
                    player.openMenu(menu);
                }
            });
            return;
        }
        minecraft.gameMode.useItemOn(
                minecraft.player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(openBlock), Direction.UP, openBlock, false)
        );
    }

    private static boolean isConfiguredBlockScreenOpen(Minecraft minecraft) {
        return minecraft.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>;
    }

    private static BlockPos parseBlockPos(String value) {
        if (value.isBlank()) {
            return null;
        }
        String[] parts = value.split(",", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException(OPEN_BLOCK_PROPERTY + " must contain x,y,z");
        }
        return new BlockPos(
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2])
        );
    }

    private record CameraPose(Vec3 position, float yaw, float pitch) {
        private static CameraPose parse(String value) {
            if (value.isBlank()) {
                return null;
            }
            String[] parts = value.split(",", -1);
            if (parts.length != 5) {
                throw new IllegalArgumentException(CAMERA_PROPERTY + " must contain x,y,z,yaw,pitch");
            }
            return new CameraPose(
                    new Vec3(
                            Double.parseDouble(parts[0]),
                            Double.parseDouble(parts[1]),
                            Double.parseDouble(parts[2])
                    ),
                    Float.parseFloat(parts[3]),
                    Float.parseFloat(parts[4])
            );
        }

        private void apply(net.minecraft.client.player.LocalPlayer player) {
            player.setPos(position);
            player.setOldPosAndRot(position, yaw, pitch);
            player.setYRot(yaw);
            player.setXRot(pitch);
            player.setDeltaMovement(Vec3.ZERO);
        }
    }
}
