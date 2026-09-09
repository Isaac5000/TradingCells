package com.cosmocraft.trading_cells.feature.logistics.adapters.api;

import com.cosmocraft.trading_cells.feature.logistics.domain.model.LogisticsResourceType;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeFilterRule;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * Versioned transport boundary for resource APIs. Implementations must simulate and commit
 * atomically, and must never load chunks while resolving a handler.
 */
public interface LogisticsResourceAdapter<H, R> {
    int API_VERSION = 1;

    Identifier id();

    LogisticsResourceType type();

    @Nullable H find(ServerLevel level, BlockPos pos, Direction side);

    default @Nullable H findCarried(ServerPlayer player, InteractionHand hand) {
        return findContainer(net.neoforged.neoforge.transfer.access.ItemAccess.forPlayerInteraction(player, hand));
    }

    default @Nullable H findContainer(net.neoforged.neoforge.transfer.access.ItemAccess access) {
        return null;
    }

    List<LogisticsResourceSnapshot<R>> contents(H handler);

    boolean matches(R resource, String componentFingerprint, PipeFilterRule rule);

    long extract(H source, R resource, long maximumAmount, TransactionContext transaction);

    long insert(H destination, R resource, long maximumAmount, TransactionContext transaction);

    default long simulateTransfer(H source, H destination, R resource, long maximumAmount) {
        if (source == null || destination == null || source == destination || resource == null || maximumAmount <= 0) {
            return 0;
        }
        try (Transaction simulation = Transaction.openRoot()) {
            long extracted = extract(source, resource, maximumAmount, simulation);
            if (extracted < 0 || extracted > maximumAmount) {
                return 0;
            }
            long accepted = extracted == 0 ? 0 : insert(destination, resource, extracted, simulation);
            return accepted >= 0 && accepted <= extracted ? accepted : 0;
        } catch (RuntimeException exception) {
            return 0;
        }
    }

    default long transfer(H source, H destination, R resource, long maximumAmount) {
        long accepted = simulateTransfer(source, destination, resource, maximumAmount);
        if (accepted <= 0) {
            return 0;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            if (extract(source, resource, accepted, transaction) != accepted
                    || insert(destination, resource, accepted, transaction) != accepted) {
                return 0;
            }
            transaction.commit();
            return accepted;
        } catch (RuntimeException exception) {
            return 0;
        }
    }

    default boolean hasClientPresentation() {
        return true;
    }
}
