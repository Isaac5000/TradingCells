package com.cosmocraft.trading_cells.platform.neoforge.integration.jade;

import com.cosmocraft.trading_cells.feature.breeders.adapters.input.BreederBlockEntity;
import com.cosmocraft.trading_cells.feature.converter.adapters.input.ConverterBlockEntity;
import com.cosmocraft.trading_cells.feature.experience.adapters.input.ExperienceStorageBlockEntity;
import com.cosmocraft.trading_cells.feature.farmer.adapters.input.FarmerBlockEntity;
import com.cosmocraft.trading_cells.feature.incubators.adapters.input.IncubatorBlockEntity;
import com.cosmocraft.trading_cells.feature.infusion.adapters.input.ArcaneInfuserBlockEntity;
import com.cosmocraft.trading_cells.feature.ironfarm.adapters.input.IronFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.quarry.adapters.input.QuarryBlockEntity;
import com.cosmocraft.trading_cells.feature.skeletonfarm.adapters.input.SkeletonFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.trader.adapters.input.AutotraderBlockEntity;
import com.cosmocraft.trading_cells.feature.trader.adapters.input.VillagerTradingCellBlockEntity;
import com.cosmocraft.trading_cells.feature.zombiefarm.adapters.input.ZombieFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.raiderfarm.adapters.input.RaiderFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.creeperfarm.adapters.input.CreeperFarmBlockEntity;
import com.cosmocraft.trading_cells.feature.configuredmobfarm.adapters.input.ConfiguredMobFarmBlockEntity;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.ContainerData;
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
        return switch (target) {
            case FarmerBlockEntity farmer -> new Progress(
                    farmer.growthTicks(),
                    farmer.growthDurationTicks()
            );
            case QuarryBlockEntity quarry -> fromData(quarry.dataAccess(), 0, 1);
            case IronFarmBlockEntity ironFarm -> fromData(ironFarm.dataAccess(), 0, 3);
            case BreederBlockEntity breeder -> fromData(breeder.dataAccess(), 0, 3);
            case IncubatorBlockEntity incubator -> fromData(incubator.dataAccess(), 0, 1);
            case ConverterBlockEntity converter -> fromData(converter.dataAccess(), 1, 3);
            case SkeletonFarmBlockEntity farm -> new Progress(
                    farm.cycleTicks(),
                    farm.cycleDurationTicks()
            );
            case ZombieFarmBlockEntity farm -> new Progress(
                    farm.cycleTicks(),
                    farm.cycleDurationTicks()
            );
            case RaiderFarmBlockEntity farm -> new Progress(
                    farm.cycleTicks(),
                    farm.cycleDurationTicks()
            );
            case CreeperFarmBlockEntity farm -> new Progress(
                    farm.cycleTicks(),
                    farm.cycleDurationTicks()
            );
            case ConfiguredMobFarmBlockEntity farm -> new Progress(
                    farm.cycleTicks(),
                    farm.cycleDurationTicks()
            );
            default -> Progress.EMPTY;
        };
    }

    private static Progress fromData(ContainerData data, int currentIndex, int maximumIndex) {
        return new Progress(data.get(currentIndex), data.get(maximumIndex));
    }

    private record Progress(int current, int maximum) {
        private static final Progress EMPTY = new Progress(0, 0);
    }
}
