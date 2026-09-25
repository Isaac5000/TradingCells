package com.cosmocraft.trading_cells.platform.neoforge.integration.jade;

import com.cosmocraft.trading_cells.platform.neoforge.client.screen.MachineScreenUtil;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.JadeIds;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.ui.ScreenDirection;

/** Client presentation for the compact machine snapshot supplied by the server. */
public final class PortableMachineJadeComponentProvider implements IBlockComponentProvider {
    private static final int EXPERIENCE_TEXT_COLOR = 0x80FF20;
    private static final int PROGRESS_BAR_WIDTH = 69;
    private static final int BAR_HEIGHT = 10;
    private static final int BAR_Y_OFFSET = 0;
    public static final PortableMachineJadeComponentProvider INSTANCE =
            new PortableMachineJadeComponentProvider();

    private PortableMachineJadeComponentProvider() {
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        tooltip.remove(JadeIds.UNIVERSAL_FLUID_STORAGE);
        var data = accessor.getServerData();
        data.getInt(PortableMachineJadeDataProvider.EXPERIENCE).ifPresent(experience -> {
            int line = tooltip.size();
            tooltip.add(new ExperienceOrbJadeElement());
            tooltip.append(JadeUI.spacer(3, 1));
            tooltip.append(JadeUI.text(Component.translatable(
                    "jade.trading_cells.experience",
                    experience
            ).withStyle(style -> style.withColor(EXPERIENCE_TEXT_COLOR))).alignSelfCenter());
            tooltip.setLineMargin(line, ScreenDirection.UP, 1);
            tooltip.setLineMargin(line, ScreenDirection.DOWN, 1);
        });

        int maximum = data.getIntOr(PortableMachineJadeDataProvider.PROGRESS_MAXIMUM, 0);
        if (maximum > 0) {
            int current = Math.clamp(
                    data.getIntOr(PortableMachineJadeDataProvider.PROGRESS, 0),
                    0,
                    maximum
            );
            float completion = current / (float) maximum;
            int color = progressColor(accessor);
            tooltip.add(new OffsetTextJadeProgressElement(
                    completion,
                    color,
                    Component.translatable(
                            "jade.trading_cells.remaining_time",
                            formatRemainingTime(maximum - current)
                    ).withStyle(style -> style.withColor(0xFFFFFFFF)),
                    PROGRESS_BAR_WIDTH,
                    BAR_HEIGHT
            ).offset(0, BAR_Y_OFFSET));
        }
    }

    @Override
    public int getDefaultPriority() {
        return 1_010;
    }

    @Override
    public Identifier getUid() {
        return PortableMachineJadeDataProvider.UID;
    }

    private static String formatRemainingTime(int ticks) {
        return MachineScreenUtil.formatDuration(ticks);
    }

    private static int progressColor(BlockAccessor accessor) {
        String path = BuiltInRegistries.BLOCK.getKey(accessor.getBlockState().getBlock()).getPath();
        if (path.contains("iron_farm")) {
            return 0xFFD7D7D7;
        }
        if (path.contains("piglin")) {
            return 0xFFB64949;
        }
        if (path.contains("farmer")) {
            return 0xFF8BA247;
        }
        if (path.contains("quarry")) {
            return 0xFF8EABC1;
        }
        return 0xFFF0B64A;
    }
}
