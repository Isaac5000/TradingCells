package com.cosmocraft.trading_cells.gametest.feature.logistics;

import com.cosmocraft.trading_cells.feature.logistics.adapters.input.LogisticsPipeBlock;
import com.cosmocraft.trading_cells.feature.logistics.adapters.input.PipeConnection;
import com.cosmocraft.trading_cells.feature.logistics.adapters.output.LogisticsRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeKind;
import com.cosmocraft.trading_cells.gametest.shared.GameTestCase;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class LogisticsShapeGameTests {
    private LogisticsShapeGameTests() { }

    public static List<GameTestCase> tests() {
        return List.of(new GameTestCase("logistics_one_voxel_extractor_frame", 20, LogisticsShapeGameTests::extractor));
    }

    private static void extractor(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        for (PipeKind kind : PipeKind.values()) {
            var base = LogisticsRegistrationAdapter.pipeBlock(kind).get().defaultBlockState();
            helper.assertValueEqual(volume(base.getShape(helper.getLevel(), pos)), 216, "Unconnected core is 6x6x6");
            for (Direction side : Direction.values()) {
                var connected = base.setValue(LogisticsPipeBlock.connectionProperty(side), PipeConnection.INSERT);
                var extracting = base.setValue(LogisticsPipeBlock.connectionProperty(side), PipeConnection.EXTRACT);
                VoxelShape frame = Shapes.join(extracting.getShape(helper.getLevel(), pos),
                        connected.getShape(helper.getLevel(), pos), BooleanOp.ONLY_FIRST);
                helper.assertValueEqual(volume(frame), 28, "Frame is 8x8 minus 6x6, one voxel deep on " + side);
                helper.assertValueEqual(Math.round(frame.max(side.getAxis()) * 16 - frame.min(side.getAxis()) * 16),
                        1L, "Frame depth on " + side);
                helper.assertTrue(!Shapes.joinIsNotEmpty(extracting.getShape(helper.getLevel(), pos),
                        extracting.getCollisionShape(helper.getLevel(), pos), BooleanOp.NOT_SAME),
                        "Selection and collision agree");
            }
        }
        helper.succeed();
    }

    private static int volume(VoxelShape shape) {
        double[] volume = {0};
        shape.forAllBoxes((x1, y1, z1, x2, y2, z2) -> volume[0] += (x2 - x1) * (y2 - y1) * (z2 - z1));
        return (int) Math.round(volume[0] * 4096);
    }
}
