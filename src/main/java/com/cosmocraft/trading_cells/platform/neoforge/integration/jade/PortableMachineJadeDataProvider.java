package com.cosmocraft.trading_cells.platform.neoforge.integration.jade;

import com.cosmocraft.trading_cells.feature.experience.adapters.input.ExperienceStorageBlockEntity;
import com.cosmocraft.trading_cells.feature.infusion.adapters.input.ArcaneInfuserBlockEntity;
import com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.input.SkeletonFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.trader.adapters.input.AutotraderBlockEntity;
import com.cosmocraft.trading_cells.feature.trader.adapters.input.VillagerTradingCellBlockEntity;
import com.cosmocraft.trading_cells.feature.zombiefarm.adapters.input.ZombieFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.raiderfarm.adapters.input.RaiderFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.creeperfarm.adapters.input.CreeperFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.configuredmobfarm.adapters.input.ConfiguredMobFarmBlockEntity;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.cosmocraft.trading_cells.platform.neoforge.machine.MachineDiagnosticSource;
import com.cosmocraft.trading_cells.shared.machines.domain.model.MachineDiagnosticSnapshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/** Sends only the small machine values that Jade actually renders. */
public final class PortableMachineJadeDataProvider implements IServerDataProvider<BlockAccessor> {
    public static final PortableMachineJadeDataProvider INSTANCE = new PortableMachineJadeDataProvider();
    public static final Identifier UID = Identifier.fromNamespaceAndPath(
            TradingCells.MOD_ID,
            "machine_status"
    );
    static final String EXPERIENCE = "TradingCellsExperience";
    static final String PROGRESS = "TradingCellsProgress";
    static final String PROGRESS_MAXIMUM = "TradingCellsProgressMaximum";

    private PortableMachineJadeDataProvider() {
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        Object target = accessor.getBlockEntity();
        int experience = storedExperience(target);
        if (experience >= 0) {
            data.putInt(EXPERIENCE, experience);
        }

        Progress progress = progress(target);
        if (progress.maximum() > 0) {
            data.putInt(PROGRESS, Math.clamp(progress.current(), 0, progress.maximum()));
            data.putInt(PROGRESS_MAXIMUM, progress.maximum());
        }
    }

    @Override
    public Identifier getUid() {
        return UID;
    }

    private static int storedExperience(Object target) {
        return switch (target) {
            case ExperienceStorageBlockEntity storage -> storage.storedExperience();
            case ArcaneInfuserBlockEntity infuser -> infuser.storedExperience();
            case SkeletonFarmBlockEntity farm -> farm.dataAccess().get(4);
            case ZombieFarmBlockEntity farm -> farm.dataAccess().get(4);
            case RaiderFarmBlockEntity farm -> farm.dataAccess().get(4);
            case CreeperFarmBlockEntity farm -> farm.dataAccess().get(4);
            case ConfiguredMobFarmBlockEntity farm -> farm.dataAccess().get(4);
            case AutotraderBlockEntity autotrader -> autotrader.storedExperience();
            case VillagerTradingCellBlockEntity trader -> trader.storedExperience();
            default -> -1;
        };
    }

    private static Progress progress(Object target) {
        if (!(target instanceof MachineDiagnosticSource source)) {
            return Progress.EMPTY;
        }
        MachineDiagnosticSnapshot snapshot = source.machineDiagnosticSnapshot();
        return new Progress(snapshot.progress(), snapshot.progressMaximum());
    }

    private record Progress(int current, int maximum) {
        private static final Progress EMPTY = new Progress(0, 0);
    }
}
