package com.cosmocraft.trading_cells.feature.logistics.adapters.neoforge;

import com.cosmocraft.trading_cells.feature.logistics.adapters.api.LogisticsResourceAdapter;
import com.cosmocraft.trading_cells.feature.logistics.adapters.api.LogisticsResourceSnapshot;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeFilterRule;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

abstract class AbstractResourceHandlerAdapter<R extends Resource>
        implements LogisticsResourceAdapter<ResourceHandler<R>, R> {
    @Override
    public List<LogisticsResourceSnapshot<R>> contents(ResourceHandler<R> handler) {
        Map<R, Long> totals = new LinkedHashMap<>();
        int size;
        try {
            size = Math.max(0, handler.size());
        } catch (RuntimeException exception) {
            return List.of();
        }
        for (int index = 0; index < size; index++) {
            try {
                R resource = handler.getResource(index);
                long amount = handler.getAmountAsLong(index);
                if (resource != null && !resource.isEmpty() && amount > 0 && accepts(resource)) {
                    totals.merge(resource, amount, AbstractResourceHandlerAdapter::saturatedAdd);
                }
            } catch (RuntimeException ignored) {
                // A broken external slot must not disable the remaining network.
            }
        }
        return totals.entrySet().stream()
                .map(entry -> snapshot(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Override
    public boolean matches(R resource, String fingerprint, PipeFilterRule rule) {
        if (!matchesIdentity(resource, rule)) {
            return false;
        }
        if (rule.componentMatch() == PipeFilterRule.ComponentMatch.IGNORE) {
            return true;
        }
        String data = resource instanceof net.neoforged.neoforge.transfer.resource.DataComponentHolderResource<?> holder
                ? LogisticsComponentData.serialized(holder.getComponentsPatch()) : fingerprint;
        return LogisticsComponentData.matches(data, rule.componentFingerprint(), rule.componentMatch());
    }

    @Override
    public long extract(ResourceHandler<R> source, R resource, long maximumAmount,
                        net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
        return resource == null || resource.isEmpty() || !accepts(resource) || maximumAmount <= 0 ? 0
                : source.extract(resource, (int) Math.min(Integer.MAX_VALUE, maximumAmount), transaction);
    }

    @Override
    public long insert(ResourceHandler<R> destination, R resource, long maximumAmount,
                       net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
        return resource == null || resource.isEmpty() || !accepts(resource) || maximumAmount <= 0 ? 0
                : destination.insert(resource, (int) Math.min(Integer.MAX_VALUE, maximumAmount), transaction);
    }

    protected abstract boolean accepts(R resource);

    protected abstract boolean matchesIdentity(R resource, PipeFilterRule rule);

    protected abstract LogisticsResourceSnapshot<R> snapshot(R resource, long amount);

    protected Identifier parsedIdentifier(String value) {
        int separator = value.indexOf('|');
        if (separator >= 0) {
            if (!id().toString().equals(value.substring(0, separator))) {
                return null;
            }
            value = value.substring(separator + 1);
        }
        return Identifier.tryParse(value);
    }

    private static long saturatedAdd(long left, long right) {
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }
}
