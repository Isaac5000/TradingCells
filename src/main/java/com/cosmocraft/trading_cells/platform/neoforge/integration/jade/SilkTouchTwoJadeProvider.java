package com.cosmocraft.trading_cells.platform.neoforge.integration.jade;

import com.cosmocraft.trading_cells.feature.silktouch.adapters.input.SilkTouchTwoDropAdapter;
import com.cosmocraft.trading_cells.feature.silktouch.adapters.input.PreservedSpawnerItemAdapter;
import com.cosmocraft.trading_cells.feature.silktouch.adapters.input.SpawnerRedstoneControlAdapter;
import com.cosmocraft.trading_cells.feature.silktouch.adapters.output.client.PreservedSpawnerTooltipAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import snownee.jade.api.JadeIds;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Exposes the special harvesting requirement without duplicating Jade's spawner entity line. */
public final class SilkTouchTwoJadeProvider implements IBlockComponentProvider {
    public static final SilkTouchTwoJadeProvider INSTANCE = new SilkTouchTwoJadeProvider();
    private static final Identifier UID = Identifier.fromNamespaceAndPath(
            TradingCells.MOD_ID,
            "silk_touch_two"
    );

    private SilkTouchTwoJadeProvider() {
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!SilkTouchTwoDropAdapter.isSpecialBlock(accessor.getBlockState())) {
            return;
        }

        List<Component> spawnerLines = new ArrayList<>();
        if (accessor.getBlockEntity() instanceof SpawnerBlockEntity spawner) {
            tooltip.replace(JadeIds.CORE_OBJECT_NAME, accessor.getBlockState().getBlock().getName());
            PreservedSpawnerItemAdapter.rootEntityData(
                    spawner.saveCustomOnly(accessor.getLevel().registryAccess())
            ).flatMap(data ->
                    PreservedSpawnerItemAdapter.describeEntityTree(accessor.getLevel(), data)
            ).ifPresent(description -> spawnerLines.addAll(
                    PreservedSpawnerTooltipAdapter.descriptionLines(description, accessor.showDetails())
            ));
        }
        boolean redstoneControl = SpawnerRedstoneControlAdapter.control(accessor.getBlockEntity())
                .map(control -> control.tradingCells$isRedstoneControlInstalled())
                .orElse(false);
        tooltip.addAll(PreservedSpawnerTooltipAdapter.withSpawnerFeatures(
                spawnerLines,
                redstoneControl,
                accessor.showDetails()
        ));
        tooltip.add(Component.translatable("jade.trading_cells.silk_touch_two"));
    }

    @Override
    public int getDefaultPriority() {
        return 20;
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}
