package com.cosmocraft.trading_cells.feature.logistics.adapters.neoforge;

import com.cosmocraft.trading_cells.feature.logistics.adapters.api.LogisticsResourceAdapter;
import com.cosmocraft.trading_cells.feature.logistics.adapters.api.LogisticsResourceSnapshot;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.LogisticsResourceType;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeFilterRule;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class EnergyLogisticsAdapter implements LogisticsResourceAdapter<EnergyHandler, Identifier> {
    private static final Identifier ADAPTER_ID = Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, "energy");
    private static final Identifier ENERGY_ID = Identifier.fromNamespaceAndPath("neoforge", "energy");

    @Override
    public Identifier id() {
        return ADAPTER_ID;
    }

    @Override
    public LogisticsResourceType type() {
        return LogisticsResourceType.ENERGY;
    }

    @Override
    public EnergyHandler find(ServerLevel level, BlockPos pos, Direction side) {
        return level.getCapability(Capabilities.Energy.BLOCK, pos, side);
    }

    @Override
    public EnergyHandler findCarried(ServerPlayer player, InteractionHand hand) {
        return ItemAccess.forPlayerInteraction(player, hand).getCapability(Capabilities.Energy.ITEM);
    }

    @Override
    public EnergyHandler findContainer(ItemAccess access) {
        return access.getCapability(Capabilities.Energy.ITEM);
    }

    @Override
    public List<LogisticsResourceSnapshot<Identifier>> contents(EnergyHandler handler) {
        try {
            long amount = handler.getAmountAsLong();
            if (amount <= 0) {
                return List.of();
            }
            return List.of(new LogisticsResourceSnapshot<>(
                    ENERGY_ID,
                    ENERGY_ID,
                    "",
                    Component.translatable("gui.trading_cells.logistics.energy"),
                    new ItemStack(Items.REDSTONE),
                    amount
            ));
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    @Override
    public boolean matches(Identifier resource, String fingerprint, PipeFilterRule rule) {
        String key = rule.key();
        int separator = key.indexOf('|');
        if (separator >= 0) {
            if (!ADAPTER_ID.toString().equals(key.substring(0, separator))) {
                return false;
            }
            key = key.substring(separator + 1);
        }
        return ENERGY_ID.equals(resource) && rule.matchKind() == PipeFilterRule.MatchKind.ID
                && ENERGY_ID.toString().equals(key)
                && LogisticsComponentData.matches("", rule.componentFingerprint(), rule.componentMatch());
    }

    @Override
    public long extract(EnergyHandler source, Identifier resource, long maximumAmount,
                        net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
        return !ENERGY_ID.equals(resource) || maximumAmount <= 0 ? 0
                : source.extract((int) Math.min(Integer.MAX_VALUE, maximumAmount), transaction);
    }

    @Override
    public long insert(EnergyHandler destination, Identifier resource, long maximumAmount,
                       net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
        return !ENERGY_ID.equals(resource) || maximumAmount <= 0 ? 0
                : destination.insert((int) Math.min(Integer.MAX_VALUE, maximumAmount), transaction);
    }
}
