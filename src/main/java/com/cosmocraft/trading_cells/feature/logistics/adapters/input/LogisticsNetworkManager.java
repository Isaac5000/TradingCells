package com.cosmocraft.trading_cells.feature.logistics.adapters.input;

import com.cosmocraft.trading_cells.feature.logistics.adapters.api.LogisticsResourceAdapter;
import com.cosmocraft.trading_cells.feature.logistics.adapters.api.LogisticsResourceAdapters;
import com.cosmocraft.trading_cells.feature.logistics.adapters.api.LogisticsResourceSnapshot;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.LogisticsResourceType;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeFilterRule;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeResourceProfile;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeRoutingMode;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeRuleTarget;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeSideMode;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeUpgradeTier;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.Config;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

public final class LogisticsNetworkManager {
    private static final Map<ServerLevel, LogisticsNetworkManager> MANAGERS = new WeakHashMap<>();
    private static final Comparator<Destination> DESTINATION_ORDER = Comparator
            .comparingInt(Destination::priority).reversed()
            .thenComparingInt(Destination::distance)
            .thenComparingLong(destination -> destination.targetPos().asLong())
            .thenComparingInt(destination -> destination.targetSide().ordinal());

    private final ServerLevel level;
    private final java.util.Random routingRandom = new java.util.Random();
    private final Map<Long, LogisticsPipeBlockEntity> pipes = new LinkedHashMap<>();
    private final Map<RouteKey, RouteScan> routes = new HashMap<>();
    private final Map<RoundRobinKey, RoutingCursor> roundRobinCursors = new HashMap<>();
    private final Map<String, Integer> resourceCursors = new HashMap<>();
    private final Map<String, CandidateProgress> destinationProgress = new HashMap<>();
    private final Set<Long> pendingEndpointRefresh = new java.util.LinkedHashSet<>();
    private long topologyRevision;
    private long budgetTick = Long.MIN_VALUE;
    private int operationsRemaining;
    private int topologyRemaining;
    private int faceOperationsRemaining = Integer.MAX_VALUE;
    private int terminalEndpointsRemaining;
    private final java.util.PriorityQueue<ScheduledExtraction> schedule = new java.util.PriorityQueue<>(
            Comparator.comparingLong(ScheduledExtraction::due).thenComparingLong(ScheduledExtraction::sequence));
    private long scheduleSequence;
    private int scanCursor;

    private LogisticsNetworkManager(ServerLevel level) {
        this.level = level;
    }

    public static synchronized LogisticsNetworkManager get(ServerLevel level) {
        return MANAGERS.computeIfAbsent(level, LogisticsNetworkManager::new);
    }

    public static synchronized void remove(ServerLevel level) {
        MANAGERS.remove(level);
    }

    public void register(LogisticsPipeBlockEntity pipe) {
        pipes.put(pipe.getBlockPos().asLong(), pipe);
        rebuildExtractions(pipe.getBlockPos());
        invalidateTopology();
    }

    public void unregister(BlockPos pos) {
        if (pipes.remove(pos.asLong()) != null) {
            schedule.removeIf(entry -> entry.key().pipePos() == pos.asLong());
            roundRobinCursors.keySet().removeIf(key -> key.origin() == pos.asLong());
            invalidateTopology();
        }
    }

    public void configurationChanged(LogisticsPipeBlockEntity pipe) {
        pipes.put(pipe.getBlockPos().asLong(), pipe);
        rebuildExtractions(pipe.getBlockPos());
        invalidateTopology();
    }

    public void invalidateTopology() {
        topologyRevision++;
        routes.clear();
        resourceCursors.clear();
        destinationProgress.clear();
    }

    public long topologyRevision() {
        return topologyRevision;
    }

    public int registeredPipeCount() {
        return pipes.size();
    }

    public void requestEndpointRefresh(BlockPos pos) {
        pendingEndpointRefresh.add(pos.asLong());
    }

    public void tick() {
        resetBudgets();
        while (!pendingEndpointRefresh.isEmpty() && topologyRemaining > 0) {
            var refreshes = pendingEndpointRefresh.iterator();
            long pos = refreshes.next();
            refreshes.remove();
            topologyRemaining--;
            var pipe = loadedPipe(BlockPos.of(pos));
            if (pipe != null) {
                var previousState = pipe.getBlockState();
                pipe.refreshConnections();
                if (pipe.getBlockState() != previousState) {
                    configurationChanged(pipe);
                }
            }
        }
        // Pending topology traversals share the same bounded work queue as terminal lookups.
        List<Map.Entry<RouteKey, RouteScan>> pending = routes.entrySet().stream()
                .filter(entry -> !entry.getValue().complete).toList();
        int total = pending.size();
        for (int count = 0; total > 0 && topologyRemaining > 0 && count < total; count++) {
            var entry = pending.get(Math.floorMod(scanCursor++, total));
            int share = Math.max(1, topologyRemaining / Math.max(1, total - count));
            topologyRemaining -= advanceScan(entry.getValue(), entry.getKey().type(), null, share);
        }
        long now = level.getGameTime();
        int scheduled = schedule.size();
        for (int index = 0; index < scheduled && operationsRemaining > 0
                && !schedule.isEmpty() && schedule.peek().due() <= now; index++) {
            ScheduledExtraction scheduledEntry = schedule.remove();
            ExtractionKey key = scheduledEntry.key();
            LogisticsPipeBlockEntity pipe = loadedPipe(BlockPos.of(key.pipePos()));
            if (pipe == null || pipe.face(key.face()).mode() != PipeSideMode.EXTRACT
                    || !pipe.face(key.face()).effectiveProfile(key.type()).enabled()) {
                continue;
            }
            PipeUpgradeTier tier = pipe.face(key.face()).upgradeTier();
            var adapter = LogisticsResourceAdapters.byId(key.adapterId());
            if (adapter == null) {
                continue;
            }
            RouteScan scan = route(BlockPos.of(key.pipePos()), key.type());
            long remaining = pipe.transferRate(key.face(), key.type());
            if (scan.complete) {
                int limit = tier == PipeUpgradeTier.INFINITE
                        ? Config.LOGISTICS_INFINITE_FACE_OPERATIONS.get() : 64;
                faceOperationsRemaining = limit;
                try {
                    for (int op = 0; op < limit && remaining > 0 && takeOperation(); op++) {
                        long moved = transferOnce(pipe, key.face(), adapter,
                                scan.ordered(pipe.face(key.face()).effectiveProfile(key.type()).routingMode()), remaining);
                        if (moved <= 0) {
                            break;
                        }
                        remaining -= moved;
                    }
                } finally {
                    faceOperationsRemaining = Integer.MAX_VALUE;
                }
            }
            schedule.add(new ScheduledExtraction(key,
                    now + (scan.complete ? tier.interval(key.type()) : 1), scheduleSequence++));
        }
    }

    public boolean takeOperation() {
        resetBudgets();
        if (operationsRemaining <= 0 || faceOperationsRemaining <= 0) {
            return false;
        }
        operationsRemaining--;
        faceOperationsRemaining--;
        return true;
    }

    private void resetBudgets() {
        long now = level.getGameTime();
        if (budgetTick != now) {
            budgetTick = now;
            operationsRemaining = Config.LOGISTICS_OPERATION_BUDGET.get();
            topologyRemaining = Config.LOGISTICS_TOPOLOGY_VISITS.get();
            terminalEndpointsRemaining = Config.LOGISTICS_TERMINAL_ENDPOINTS.get();
        }
    }

    private RouteScan route(BlockPos origin, LogisticsResourceType type) {
        RouteKey key = new RouteKey(origin.asLong(), Direction.DOWN, type, null);
        var scan = routes.computeIfAbsent(key, ignored -> new RouteScan(topologyRevision, origin));
        scan.transportRequested = true;
        return scan;
    }

    public ChannelSearch searchChannels(BlockPos origin, LogisticsResourceType type, String prefix) {
        return new ChannelSearch(origin.immutable(), type, PipeFilterRule.searchPrefix(prefix));
    }

    public void advanceChannelSearch(ChannelSearch search) {
        resetBudgets();
        if (search.closed) { return; }
        if (search.scan == null || search.scan.revision != topologyRevision) {
            releaseSearchScan(search);
            var key = new RouteKey(search.origin.asLong(), Direction.DOWN, search.type, null);
            search.scan = routes.computeIfAbsent(key, ignored -> new RouteScan(topologyRevision, search.origin));
            search.scan.searchUsers++;
            search.nodes = null; search.results.clear(); search.complete = false;
        }
        if (!search.scan.complete || search.complete) { return; }
        if (search.nodes == null) { search.nodes = search.scan.visited.iterator(); }
        // Only sixteen existing strings are retained; the topology iterator is borrowed, never copied.
        int allowance = Math.min(32, topologyRemaining);
        while (allowance-- > 0 && search.nodes.hasNext()) {
            topologyRemaining--;
            var pipe = loadedPipe(BlockPos.of(search.nodes.next()));
            if (pipe == null) { continue; }
            for (Direction side : Direction.values()) {
                if (pipe.face(side).mode() == PipeSideMode.NONE) { continue; }
                var profile = pipe.face(side).effectiveProfile(search.type);
                search.offer(profile.channel());
                for (var rule : profile.filters()) { search.offer(rule.routeChannel()); }
            }
        }
        search.complete = !search.nodes.hasNext();
    }

    public void closeChannelSearch(ChannelSearch search) {
        releaseSearchScan(search); search.closed = true; search.results.clear(); search.nodes = null;
    }

    private void releaseSearchScan(ChannelSearch search) {
        if (search.scan == null) { return; }
        var scan = search.scan;
        if (--scan.searchUsers == 0 && !scan.transportRequested) {
            routes.remove(new RouteKey(search.origin.asLong(), Direction.DOWN, search.type, null), scan);
        }
        search.scan = null;
    }

    public static final class ChannelSearch {
        private final BlockPos origin;
        private final LogisticsResourceType type;
        private final String prefix;
        private final java.util.NavigableSet<String> results = new java.util.TreeSet<>();
        private @Nullable RouteScan scan;
        private @Nullable Iterator<Long> nodes;
        private boolean complete, closed;

        private ChannelSearch(BlockPos origin, LogisticsResourceType type, String prefix) {
            this.origin = origin; this.type = type; this.prefix = prefix;
        }
        private void offer(String channel) {
            if (channel.isEmpty() || !channel.startsWith(prefix)) { return; }
            if (results.size() == 16 && channel.compareTo(results.last()) >= 0) { return; }
            results.add(channel);
            if (results.size() > 16) { results.pollLast(); }
        }
        public LogisticsResourceType type() { return type; }
        public String prefix() { return prefix; }
        public List<String> results() { return List.copyOf(results); }
        public boolean complete() { return complete; }
    }

    public boolean topologyReady(BlockPos origin, LogisticsResourceType type) {
        return route(origin, type).complete;
    }

    public List<NetworkEndpoint> endpointsFrom(
            BlockPos origin,
            LogisticsResourceType type,
            PipeSideMode mode,
            @Nullable Identifier adapterId
    ) {
        LogisticsPipeBlockEntity start = loadedPipe(origin);
        LogisticsResourceAdapter<?, ?> adapter = adapterId == null ? null : LogisticsResourceAdapters.byId(adapterId);
        if (start == null || !start.kind().supports(type)
                || adapterId != null && (adapter == null || adapter.type() != type)) {
            return List.of();
        }
        return collectEndpoints(origin, type, mode, adapter);
    }

    public List<AggregatedResource> resourcesFrom(BlockPos origin, LogisticsResourceType type) {
        Map<ResourceKey, MutableAggregate> aggregates = new LinkedHashMap<>();
        for (LogisticsResourceAdapter<?, ?> adapter : LogisticsResourceAdapters.forType(type)) {
            List<NetworkEndpoint> endpoints = new ArrayList<>(endpointsFrom(origin, type, PipeSideMode.EXTRACT, adapter.id()));
            endpoints.addAll(endpointsFrom(origin, type, PipeSideMode.INSERT, adapter.id()));
            aggregateTyped(cast(adapter), endpoints, aggregates);
        }
        return aggregates.values().stream()
                .map(MutableAggregate::snapshot)
                .sorted(Comparator.comparing(value -> value.displayName().getString()))
                .toList();
    }

    public List<NetworkEndpoint> terminalEndpoints(BlockPos origin, LogisticsResourceType type, Identifier adapterId) {
        var endpoints = new ArrayList<>(endpointsFrom(origin, type, PipeSideMode.EXTRACT, adapterId));
        endpoints.addAll(endpointsFrom(origin, type, PipeSideMode.INSERT, adapterId));
        endpoints.sort(Comparator.comparingInt(NetworkEndpoint::priority).reversed()
                .thenComparingInt(NetworkEndpoint::distance)
                .thenComparingLong(endpoint -> endpoint.targetPos().asLong())
                .thenComparingInt(endpoint -> endpoint.targetSide().ordinal()));
        return endpoints;
    }

    public ResourceListing beginListing(BlockPos origin, LogisticsResourceType type) {
        return new ResourceListing(origin, type);
    }

    public final class ResourceListing {
        private final long revision = topologyRevision;
        private final ArrayDeque<ListingEndpoint> pending = new ArrayDeque<>();
        private final Map<Identifier, IdentityHashMap<Object, Boolean>> seen = new HashMap<>();
        private final Map<ResourceKey, MutableAggregate> aggregates = new LinkedHashMap<>();
        private List<AggregatedResource> result;

        private ResourceListing(BlockPos origin, LogisticsResourceType type) {
            for (var adapter : LogisticsResourceAdapters.forType(type)) {
                for (PipeSideMode mode : List.of(PipeSideMode.EXTRACT, PipeSideMode.INSERT)) {
                    for (var endpoint : endpointsFrom(origin, type, mode, adapter.id())) {
                        pending.addLast(new ListingEndpoint(adapter, endpoint));
                    }
                }
            }
        }

        public boolean stale() {
            return revision != topologyRevision;
        }

        public boolean advance() {
            resetBudgets();
            while (!pending.isEmpty() && terminalEndpointsRemaining > 0 && !stale()) {
                terminalEndpointsRemaining--;
                var job = pending.removeFirst();
                aggregateTyped(cast(job.adapter()), List.of(job.endpoint()), aggregates,
                        seen.computeIfAbsent(job.adapter().id(), ignored -> new IdentityHashMap<>()));
            }
            if (pending.isEmpty() && result == null) {
                result = aggregates.values().stream().map(MutableAggregate::snapshot)
                        .sorted(Comparator.comparing(value -> value.displayName().getString())).toList();
            }
            return result != null;
        }

        public List<AggregatedResource> result() {
            return result == null ? List.of() : result;
        }
    }

    private record ListingEndpoint(LogisticsResourceAdapter<?, ?> adapter, NetworkEndpoint endpoint) {
    }

    public long transferFromNetwork(
            BlockPos origin,
            Identifier adapterId,
            Identifier resourceId,
            String componentFingerprint,
            Object target,
            long maximum
    ) {
        LogisticsResourceAdapter<?, ?> adapter = LogisticsResourceAdapters.byId(adapterId);
        if (adapter == null || target == null || maximum <= 0) {
            return 0;
        }
        return transferFromNetworkTyped(
                origin,
                cast(adapter),
                resourceId,
                componentFingerprint == null ? "" : componentFingerprint,
                target,
                maximum
        );
    }

    public long transferIntoNetwork(
            BlockPos origin,
            Identifier adapterId,
            Object source,
            long maximum
    ) {
        LogisticsResourceAdapter<?, ?> adapter = LogisticsResourceAdapters.byId(adapterId);
        if (adapter == null || source == null || maximum <= 0) {
            return 0;
        }
        return transferIntoNetworkTyped(origin, cast(adapter), source, maximum);
    }

    public static <H, R> @Nullable H findHandler(
            LogisticsResourceAdapter<H, R> adapter,
            ServerLevel level,
            BlockPos pos,
            Direction side
    ) {
        if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null) {
            return null;
        }
        try {
            return adapter.find(level, pos, side);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void rebuildExtractions(BlockPos pos) {
        schedule.removeIf(entry -> entry.key().pipePos() == pos.asLong());
        LogisticsPipeBlockEntity pipe = pipes.get(pos.asLong());
        if (pipe == null || pipe.isRemoved()) {
            return;
        }
        for (Direction face : Direction.values()) {
            PipeFaceConfiguration configuration = pipe.face(face);
            if (configuration.mode() != PipeSideMode.EXTRACT) {
                continue;
            }
            for (LogisticsResourceType type : pipe.kind().resourceTypes()) {
                if (!configuration.effectiveProfile(type).enabled()) {
                    continue;
                }
                for (LogisticsResourceAdapter<?, ?> adapter : LogisticsResourceAdapters.forType(type)) {
                    ExtractionKey key = new ExtractionKey(pos.asLong(), face, type, adapter.id());
                    schedule.add(new ScheduledExtraction(key, level.getGameTime(), scheduleSequence++));
                }
            }
        }
    }

    private List<NetworkEndpoint> collectEndpoints(
            BlockPos origin, LogisticsResourceType type, PipeSideMode mode,
            @Nullable LogisticsResourceAdapter<?, ?> adapter
    ) {
        RouteScan scan = route(origin, type);
        if (!scan.complete) {
            return List.of();
        }
        return scan.destinations.stream()
                .filter(destination -> destination.configuration().mode() == mode)
                .map(destination -> new NetworkEndpoint(destination.pipePos(), destination.face(),
                        destination.targetPos(), destination.targetSide(), destination.distance(),
                        destination.priority(), destination.configuration())).toList();
    }

    private <H, R> void aggregateTyped(
            LogisticsResourceAdapter<H, R> adapter,
            List<NetworkEndpoint> endpoints,
            Map<ResourceKey, MutableAggregate> aggregates
    ) {
        aggregateTyped(adapter, endpoints, aggregates, new IdentityHashMap<>());
    }

    private <H, R> void aggregateTyped(LogisticsResourceAdapter<H, R> adapter,
            List<NetworkEndpoint> endpoints, Map<ResourceKey, MutableAggregate> aggregates,
            IdentityHashMap<Object, Boolean> seenHandlers) {
        for (NetworkEndpoint endpoint : endpoints) {
            H handler = findHandler(adapter, level, endpoint.targetPos(), endpoint.targetSide());
            if (handler == null || seenHandlers.put(handler, Boolean.TRUE) != null) {
                continue;
            }
            List<LogisticsResourceSnapshot<R>> contents;
            try {
                contents = adapter.contents(handler);
            } catch (RuntimeException ignored) {
                continue;
            }
            PipeResourceProfile profile = endpoint.configuration().effectiveProfile(adapter.type());
            for (LogisticsResourceSnapshot<R> resource : contents) {
                if (!filter(adapter, resource, profile).allowed()) {
                    continue;
                }
                ResourceKey key = new ResourceKey(
                        adapter.id(),
                        resource.resourceId(),
                        resource.componentFingerprint()
                );
                aggregates.compute(key, (ignored, current) -> current == null
                        ? new MutableAggregate(
                                adapter.id(),
                                adapter.type(),
                                resource.resourceId(),
                                resource.componentFingerprint(),
                                resource.displayName(),
                                resource.icon(),
                                resource.amount()
                        )
                        : current.add(resource.amount()));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <H, R> long transferFromNetworkTyped(
            BlockPos origin,
            LogisticsResourceAdapter<H, R> adapter,
            Identifier resourceId,
            String componentFingerprint,
            Object untypedTarget,
            long maximum
    ) {
        H target;
        try {
            target = (H) untypedTarget;
        } catch (ClassCastException exception) {
            return 0;
        }
        List<NetworkEndpoint> endpoints = terminalEndpoints(origin, adapter.type(), adapter.id());
        return transferAcrossEndpoints(
                origin,
                adapter,
                endpoints,
                target,
                resourceId,
                componentFingerprint,
                maximum,
                true
        );
    }

    @SuppressWarnings("unchecked")
    private <H, R> long transferIntoNetworkTyped(
            BlockPos origin,
            LogisticsResourceAdapter<H, R> adapter,
            Object untypedSource,
            long maximum
    ) {
        H source;
        try {
            source = (H) untypedSource;
        } catch (ClassCastException exception) {
            return 0;
        }
        List<LogisticsResourceSnapshot<R>> resources;
        try {
            resources = adapter.contents(source);
        } catch (RuntimeException exception) {
            return 0;
        }
        List<NetworkEndpoint> endpoints = terminalEndpoints(origin, adapter.type(), adapter.id());
        long moved = 0;
        for (LogisticsResourceSnapshot<R> resource : resources) {
            if (moved >= maximum) {
                break;
            }
            moved += transferResourceIntoEndpoints(
                    origin,
                    adapter,
                    source,
                    resource,
                    endpoints,
                    maximum - moved
            );
        }
        return moved;
    }

    private <H, R> long transferAcrossEndpoints(
            BlockPos origin,
            LogisticsResourceAdapter<H, R> adapter,
            List<NetworkEndpoint> endpoints,
            H target,
            Identifier resourceId,
            String componentFingerprint,
            long maximum,
            boolean sourceEndpoints
    ) {
        long moved = 0;
        IdentityHashMap<H, Boolean> seenHandlers = new IdentityHashMap<>();
        EndpointOrder order = orderedForTerminal(origin, adapter.id(), resourceId, endpoints, -1);
        for (NetworkEndpoint endpoint : order.endpoints()) {
            if (moved >= maximum) {
                break;
            }
            H source = findHandler(adapter, level, endpoint.targetPos(), endpoint.targetSide());
            if (source == null || source == target || seenHandlers.put(source, Boolean.TRUE) != null) {
                continue;
            }
            PipeResourceProfile profile = endpoint.configuration().effectiveProfile(adapter.type());
            List<LogisticsResourceSnapshot<R>> contents;
            try {
                contents = adapter.contents(source);
            } catch (RuntimeException ignored) {
                continue;
            }
            for (LogisticsResourceSnapshot<R> resource : contents) {
                if (!resource.resourceId().equals(resourceId)
                        || !resource.componentFingerprint().equals(componentFingerprint)
                        || !filter(adapter, resource, profile).allowed()) {
                    continue;
                }
                long transferred;
                try {
                    if (!takeOperation()) {
                        return moved;
                    }
                    transferred = adapter.transfer(source, target, resource.resource(), maximum - moved);
                } catch (RuntimeException ignored) {
                    transferred = 0;
                }
                moved += Math.max(0, transferred);
                if (transferred > 0) {
                    order.transferred(endpoint, null);
                }
                if (moved >= maximum) {
                    break;
                }
            }
        }
        return moved;
    }

    private <H, R> long transferResourceIntoEndpoints(
            BlockPos origin,
            LogisticsResourceAdapter<H, R> adapter,
            H source,
            LogisticsResourceSnapshot<R> resource,
            List<NetworkEndpoint> endpoints,
            long maximum
    ) {
        long moved = 0;
        IdentityHashMap<H, Boolean> seenHandlers = new IdentityHashMap<>();
        EndpointOrder order = orderedForTerminal(
                origin,
                adapter.id(),
                resource.resourceId(),
                endpoints, -2
        );
        for (NetworkEndpoint endpoint : order.endpoints()) {
            if (moved >= maximum) {
                break;
            }
            PipeResourceProfile profile = endpoint.configuration().effectiveProfile(adapter.type());
            if (!filter(adapter, resource, profile).allowed()) {
                continue;
            }
            H destination = findHandler(adapter, level, endpoint.targetPos(), endpoint.targetSide());
            if (destination == null || destination == source
                    || seenHandlers.put(destination, Boolean.TRUE) != null) {
                continue;
            }
            long transferred;
            try {
                if (!takeOperation()) {
                    return moved;
                }
                transferred = adapter.transfer(source, destination, resource.resource(), maximum - moved);
            } catch (RuntimeException ignored) {
                transferred = 0;
            }
            moved += Math.max(0, transferred);
            if (transferred > 0) {
                order.transferred(endpoint, null);
            }
        }
        return moved;
    }

    EndpointOrder orderedForTerminal(
            BlockPos origin,
            Identifier adapterId,
            Identifier resourceId,
            List<NetworkEndpoint> endpoints
    ) {
        return orderedForTerminal(origin, adapterId, resourceId, endpoints, -1);
    }

    EndpointOrder orderedForTerminal(
            BlockPos origin, Identifier adapterId, Identifier resourceId,
            List<NetworkEndpoint> endpoints, int flow
    ) {
        if (endpoints.size() < 2) {
            return new EndpointOrder(endpoints, Map.of());
        }
        List<NetworkEndpoint> ordered = new ArrayList<>(endpoints);
        Map<NetworkEndpoint, CursorAdvance> advances = new HashMap<>();
        int index = 0;
        while (index < ordered.size()) {
            NetworkEndpoint first = ordered.get(index);
            int end = index + 1;
            while (end < ordered.size()
                    && ordered.get(end).priority() == first.priority()
                    && ordered.get(end).distance() == first.distance()) {
                end++;
            }
            int size = end - index;
            if (size > 1) {
                var key = new RoundRobinKey(origin.asLong(), flow, adapterId, resourceId,
                        new RouteGroup(first.priority(), first.distance()));
                RoutingCursor cursor = roundRobinCursors.computeIfAbsent(key, ignored -> new RoutingCursor());
                int offset = Math.floorMod(cursor.position, size);
                List<NetworkEndpoint> group = new ArrayList<>(ordered.subList(index, end));
                for (int groupIndex = 0; groupIndex < size; groupIndex++) {
                    ordered.set(index + groupIndex, group.get((offset + groupIndex) % size));
                    advances.put(group.get(groupIndex), new CursorAdvance(cursor, (groupIndex + 1) % size));
                }
            }
            index = end;
        }
        return new EndpointOrder(ordered, advances);
    }

    private boolean isDue(ExtractionKey key, PipeUpgradeTier tier) {
        int interval = Math.max(1, tier.interval(key.type()));
        return Math.floorMod(level.getGameTime() + key.hashCode(), interval) == 0;
    }

    private int advanceScan(
            RouteScan scan,
            LogisticsResourceType type,
            @Nullable LogisticsResourceAdapter<?, ?> adapter,
            int maximumVisits
    ) {
        int visits = 0;
        while (!scan.pending.isEmpty() && visits < maximumVisits) {
            Node node = scan.pending.removeFirst();
            visits++;
            LogisticsPipeBlockEntity pipe = loadedPipe(node.pos());
            if (pipe == null || !pipe.kind().supports(type)) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = node.pos().relative(direction);
                LogisticsPipeBlockEntity neighborPipe = loadedPipe(neighborPos);
                if (neighborPipe != null) {
                    if (connected(pipe, neighborPipe, direction, type)
                            && scan.visited.add(neighborPos.asLong())) {
                        scan.pending.addLast(new Node(neighborPos.immutable(), node.distance() + 1));
                    }
                    continue;
                }
                BlockEntity neighborEntity = loadedBlockEntity(neighborPos);
                if (neighborEntity instanceof LogisticsBridgeNode bridge
                        && bridge.supportsLogisticsResource(type)
                        && !pipe.face(direction).pipeDisconnected()) {
                    for (BlockPos bridged : bridge.bridgePipePositions(level, type)) {
                        if (scan.visited.add(bridged.asLong())) {
                            scan.pending.addLast(new Node(bridged.immutable(), node.distance() + 1));
                        }
                    }
                    continue;
                }
                PipeFaceConfiguration configuration = pipe.face(direction);
                if (configuration.mode() != PipeSideMode.NONE
                        && configuration.effectiveProfile(type).enabled()) {
                    scan.destinations.add(new Destination(
                            node.pos(),
                            direction,
                            neighborPos.immutable(),
                            direction.getOpposite(),
                            node.distance(),
                            configuration.priority(),
                            configuration
                    ));
                }
            }
        }
        if (scan.pending.isEmpty()) {
            scan.complete = true;
            scan.destinations.sort(DESTINATION_ORDER);
            // Topology changes elsewhere must not restart equal-route distribution.
            // Discard only groups no longer reachable from this origin and resource type.
            Set<RouteGroup> groups = new HashSet<>();
            scan.destinations.forEach(destination -> groups.add(
                    new RouteGroup(destination.priority(), destination.distance())));
            scan.destinations.forEach(destination -> groups.add(new RouteGroup(destination.priority(), -1)));
            roundRobinCursors.keySet().removeIf(key -> {
                var resourceAdapter = LogisticsResourceAdapters.byId(key.adapter());
                return key.origin() == scan.origin && resourceAdapter != null && resourceAdapter.type() == type
                        && !groups.contains(key.group());
            });
        }
        return visits;
    }

    private long transferOnce(
            LogisticsPipeBlockEntity pipe,
            Direction sourceFace,
            LogisticsResourceAdapter<?, ?> untypedAdapter,
            List<Destination> destinations,
            long maximum
    ) {
        return transferTyped(pipe, sourceFace, cast(untypedAdapter), destinations, maximum);
    }

    private <H, R> long transferTyped(
            LogisticsPipeBlockEntity pipe,
            Direction sourceFace,
            LogisticsResourceAdapter<H, R> adapter,
            List<Destination> destinations,
            long maximum
    ) {
        BlockPos sourcePos = pipe.getBlockPos().relative(sourceFace);
        H source = findHandler(adapter, level, sourcePos, sourceFace.getOpposite());
        if (source == null) {
            return 0;
        }
        List<LogisticsResourceSnapshot<R>> resources;
        try {
            resources = adapter.contents(source);
        } catch (RuntimeException exception) {
            return 0;
        }
        PipeResourceProfile sourceProfile = pipe.face(sourceFace).effectiveProfile(adapter.type());
        String sourceKey = pipe.getBlockPos().asLong() + ":" + sourceFace + ":" + adapter.id();
        int startResource = Math.floorMod(resourceCursors.getOrDefault(sourceKey, 0), Math.max(1, resources.size()));
        for (int offset = 0; offset < resources.size(); offset++) {
            int resourceIndex = (startResource + offset) % resources.size();
            LogisticsResourceSnapshot<R> resource = resources.get(resourceIndex);
            FilterDecision sourceDecision = filter(adapter, resource, sourceProfile);
            if (!sourceDecision.allowed()) {
                continue;
            }
            String routeChannel = sourceDecision.routeChannel().isEmpty()
                    ? sourceProfile.channel()
                    : sourceDecision.routeChannel();
            long moved = tryDestinations(
                    pipe,
                    sourceFace,
                    adapter,
                    source,
                    resource,
                    destinations,
                    routeChannel,
                    sourceDecision.target(),
                    maximum
            );
            if (moved > 0) {
                resourceCursors.put(sourceKey, (resourceIndex + 1) % resources.size());
                return moved;
            }
            if (operationsRemaining <= 0 || faceOperationsRemaining <= 0) {
                resourceCursors.put(sourceKey, resourceIndex);
                return 0;
            }
            resourceCursors.put(sourceKey, (resourceIndex + 1) % resources.size());
        }
        return 0;
    }

    private <H, R> long tryDestinations(
            LogisticsPipeBlockEntity sourcePipe,
            Direction sourceFace,
            LogisticsResourceAdapter<H, R> adapter,
            H source,
            LogisticsResourceSnapshot<R> resource,
            List<Destination> destinations,
            String routeChannel,
            @Nullable PipeRuleTarget targetRestriction,
            long maximum
    ) {
        PipeRoutingMode mode = sourcePipe.face(sourceFace).effectiveProfile(adapter.type()).routingMode();
        if (mode == PipeRoutingMode.EQUAL) {
            return shareResource(sourcePipe, sourceFace, adapter, source, resource, destinations,
                    routeChannel, targetRestriction, Math.min(maximum, resource.amount()));
        }
        String progressKey = sourcePipe.getBlockPos().asLong() + ":" + sourceFace + ":" + adapter.id()
                + ":" + resource.resourceId() + ":" + resource.componentFingerprint();
        CandidateProgress progress = destinationProgress.getOrDefault(progressKey, new CandidateProgress(0, 0));
        int index = progress.groupStart();
        while (index < destinations.size()) {
            Destination first = destinations.get(index);
            int end = index + 1;
            while (end < destinations.size()
                    && destinations.get(end).priority() == first.priority()
                    && (mode == PipeRoutingMode.RANDOM || destinations.get(end).distance() == first.distance())) {
                end++;
            }
            int size = end - index;
            var cursorKey = new RoundRobinKey(sourcePipe.getBlockPos().asLong(), sourceFace.ordinal(),
                    adapter.id(), resource.resourceId(), new RouteGroup(first.priority(), mode == PipeRoutingMode.RANDOM ? -1 : first.distance()));
            RoutingCursor cursor = roundRobinCursors.computeIfAbsent(cursorKey, ignored -> new RoutingCursor());
            int start = index == progress.groupStart() && progress.start() >= 0 ? progress.start()
                    : mode == PipeRoutingMode.RANDOM ? routingRandom.nextInt(size) : Math.floorMod(cursor.position, size);
            IdentityHashMap<H, Boolean> seenHandlers = new IdentityHashMap<>();
            for (int offset = index == progress.groupStart() ? progress.offset() : 0; offset < size; offset++) {
                if (!takeOperation()) {
                    destinationProgress.put(progressKey, new CandidateProgress(index, offset, start));
                    return 0;
                }
                int candidateIndex = index + (start + offset) % size;
                Destination destination = destinations.get(candidateIndex);
                if (destination.configuration().mode() != PipeSideMode.INSERT) {
                    continue;
                }
                PipeResourceProfile destinationProfile = destination.configuration().effectiveProfile(adapter.type());
                FilterDecision decision = filter(adapter, resource, destinationProfile);
                if (!channelsMatch(routeChannel, destinationProfile.channel())
                        || !decision.allowed() || !channelsMatch(routeChannel, decision.routeChannel())
                        || !matchesTarget(targetRestriction, destination) || !matchesTarget(decision.target(), destination)) {
                    continue;
                }
                H target = findHandler(adapter, level, destination.targetPos(), destination.targetSide());
                if (target == null || seenHandlers.put(target, Boolean.TRUE) != null || target == source) {
                    continue;
                }
                long moved;
                try {
                    moved = adapter.transfer(source, target, resource.resource(), maximum);
                } catch (RuntimeException ignored) {
                    moved = 0;
                }
                if (moved > 0) {
                    destinationProgress.remove(progressKey);
                    cursor.advance((start + offset + 1) % size, null);
                    return moved;
                }
            }
            index = end;
        }
        destinationProgress.remove(progressKey);
        return 0;
    }

    private <H, R> long shareResource(
            LogisticsPipeBlockEntity pipe, Direction face, LogisticsResourceAdapter<H, R> adapter,
            H source, LogisticsResourceSnapshot<R> resource, List<Destination> destinations,
            String channel, @Nullable PipeRuleTarget targetRestriction, long maximum
    ) {
        long moved = 0;
        for (int index = 0; index < destinations.size() && moved < maximum;) {
            int priority = destinations.get(index).priority();
            var candidates = new ArrayList<Destination>();
            do {
                Destination destination = destinations.get(index++);
                PipeResourceProfile profile = destination.configuration().effectiveProfile(adapter.type());
                FilterDecision decision = filter(adapter, resource, profile);
                if (destination.configuration().mode() == PipeSideMode.INSERT
                        && channelsMatch(channel, profile.channel()) && channelsMatch(channel, decision.routeChannel())
                        && decision.allowed() && matchesTarget(targetRestriction, destination) && matchesTarget(decision.target(), destination)) {
                    candidates.add(destination);
                }
            } while (index < destinations.size() && destinations.get(index).priority() == priority);
            if (candidates.isEmpty()) { continue; }
            var key = new RoundRobinKey(pipe.getBlockPos().asLong(), face.ordinal(), adapter.id(),
                    resource.resourceId(), new RouteGroup(priority, -1));
            RoutingCursor cursor = roundRobinCursors.computeIfAbsent(key, ignored -> new RoutingCursor());
            int start = Math.floorMod(cursor.position, candidates.size());
            // Equal-priority consumers share each pass. Spare capacity is redistributed in a second pass.
            boolean progress;
            do {
                progress = false;
                IdentityHashMap<H, Boolean> seen = new IdentityHashMap<>();
                for (int offset = 0; offset < candidates.size() && moved < maximum; offset++) {
                    int candidateIndex = (start + offset) % candidates.size();
                    if (!takeOperation()) {
                        cursor.advance(candidateIndex, null);
                        return moved;
                    }
                    Destination destination = candidates.get(candidateIndex);
                    H target = findHandler(adapter, level, destination.targetPos(), destination.targetSide());
                    if (target == null || target == source || seen.put(target, Boolean.TRUE) != null) { continue; }
                    long remaining = maximum - moved;
                    int count = candidates.size() - offset;
                    long share = remaining / count + (remaining % count == 0 ? 0 : 1);
                    long transferred;
                    try {
                        transferred = adapter.transfer(source, target, resource.resource(), share);
                    } catch (RuntimeException exception) {
                        continue;
                    }
                    if (transferred > 0 && transferred <= share) {
                        moved += transferred;
                        progress = true;
                    }
                }
            } while (progress && moved < maximum);
            cursor.advance((start + 1) % candidates.size(), null);
        }
        return moved;
    }

    private static <H, R> FilterDecision filter(
            LogisticsResourceAdapter<H, R> adapter,
            LogisticsResourceSnapshot<R> resource,
            PipeResourceProfile profile
    ) {
        if (profile.filterMode() == PipeResourceProfile.FilterMode.OFF) { return new FilterDecision(true, ""); }
        for (PipeFilterRule rule : profile.filters()) {
            if (adapter.matches(resource.resource(), resource.componentFingerprint(), rule)) {
                boolean allowed = profile.filterMode() == PipeResourceProfile.FilterMode.RULES
                        ? rule.action() == PipeFilterRule.Action.ALLOW : profile.filterMode() == PipeResourceProfile.FilterMode.WHITELIST;
                return new FilterDecision(allowed != rule.inverted(), rule.routeChannel(), rule.target());
            }
        }
        return new FilterDecision(profile.filterMode() != PipeResourceProfile.FilterMode.WHITELIST, "");
    }

    private boolean matchesTarget(@Nullable PipeRuleTarget target, Destination destination) {
        BlockPos pos = destination.targetPos();
        return target == null || target.matches(level.dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ());
    }

    private static boolean channelsMatch(String source, String destination) {
        return source.isEmpty() || destination.isEmpty() || source.equals(destination);
    }

    private static long saturatedAdd(long left, long right) {
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }

    private static boolean connected(
            LogisticsPipeBlockEntity from,
            LogisticsPipeBlockEntity to,
            Direction direction,
            LogisticsResourceType type
    ) {
        return from.kind().supports(type)
                && to.kind().supports(type)
                && from.kind().connectsTo(to.kind())
                && !from.face(direction).pipeDisconnected()
                && !to.face(direction.getOpposite()).pipeDisconnected();
    }

    private LogisticsPipeBlockEntity loadedPipe(BlockPos pos) {
        BlockEntity blockEntity = loadedBlockEntity(pos);
        return blockEntity instanceof LogisticsPipeBlockEntity pipe ? pipe : null;
    }

    private @Nullable BlockEntity loadedBlockEntity(BlockPos pos) {
        var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk == null ? null : chunk.getBlockEntity(pos);
    }

    private Object findHandlerUnchecked(
            LogisticsResourceAdapter<?, ?> adapter,
            BlockPos pos,
            Direction side
    ) {
        return findHandler(cast(adapter), level, pos, side);
    }

    @SuppressWarnings("unchecked")
    private static <H, R> LogisticsResourceAdapter<H, R> cast(LogisticsResourceAdapter<?, ?> adapter) {
        return (LogisticsResourceAdapter<H, R>) adapter;
    }

    public record NetworkEndpoint(
            BlockPos pipePos,
            Direction face,
            BlockPos targetPos,
            Direction targetSide,
            int distance,
            int priority,
            PipeFaceConfiguration configuration
    ) {
    }

    public record AggregatedResource(
            Identifier adapterId,
            LogisticsResourceType type,
            Identifier resourceId,
            String componentFingerprint,
            Component displayName,
            ItemStack icon,
            long amount
    ) {
    }

    private record ExtractionKey(long pipePos, Direction face, LogisticsResourceType type, Identifier adapterId) {
    }

    private record ScheduledExtraction(ExtractionKey key, long due, long sequence) {
    }

    private record RouteKey(long pipePos, Direction face, LogisticsResourceType type, Identifier adapterId) {
    }

    private record Node(BlockPos pos, int distance) {
    }

    private record Destination(
            BlockPos pipePos,
            Direction face,
            BlockPos targetPos,
            Direction targetSide,
            int distance,
            int priority,
            PipeFaceConfiguration configuration
    ) {
    }

    private record FilterDecision(boolean allowed, String routeChannel, @Nullable PipeRuleTarget target) {
        private FilterDecision(boolean allowed, String routeChannel) { this(allowed, routeChannel, null); }
    }

    private record CandidateProgress(int groupStart, int offset, int start) {
        private CandidateProgress(int groupStart, int offset) { this(groupStart, offset, -1); }
    }

    private record RouteGroup(int priority, int distance) {
    }

    private record RoundRobinKey(long origin, int face, Identifier adapter, Identifier resource, RouteGroup group) {
    }

    record EndpointOrder(List<NetworkEndpoint> endpoints, Map<NetworkEndpoint, CursorAdvance> advances) {
        void transferred(NetworkEndpoint endpoint, @Nullable TransactionContext transaction) {
            CursorAdvance advance = advances.get(endpoint);
            if (advance != null) {
                advance.cursor().advance(advance.next(), transaction);
            }
        }
    }

    private record CursorAdvance(RoutingCursor cursor, int next) {
    }

    private static final class RoutingCursor extends SnapshotJournal<Integer> {
        private int position;

        void advance(int next, @Nullable TransactionContext transaction) {
            if (transaction != null) {
                updateSnapshots(transaction);
            }
            position = next;
        }

        @Override
        protected Integer createSnapshot() {
            return position;
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            position = snapshot;
        }
    }

    private record ResourceKey(Identifier adapterId, Identifier resourceId, String componentFingerprint) {
    }

    private static final class MutableAggregate {
        private final Identifier adapterId;
        private final LogisticsResourceType type;
        private final Identifier resourceId;
        private final String componentFingerprint;
        private final Component displayName;
        private final ItemStack icon;
        private long amount;

        private MutableAggregate(
                Identifier adapterId,
                LogisticsResourceType type,
                Identifier resourceId,
                String componentFingerprint,
                Component displayName,
                ItemStack icon,
                long amount
        ) {
            this.adapterId = adapterId;
            this.type = type;
            this.resourceId = resourceId;
            this.componentFingerprint = componentFingerprint;
            this.displayName = displayName;
            this.icon = icon.copy();
            this.amount = Math.max(0, amount);
        }

        private MutableAggregate add(long added) {
            amount = saturatedAdd(amount, Math.max(0, added));
            return this;
        }

        private AggregatedResource snapshot() {
            return new AggregatedResource(
                    adapterId,
                    type,
                    resourceId,
                    componentFingerprint,
                    displayName,
                    icon,
                    amount
            );
        }
    }

    private static final class RouteScan {
        private final long revision;
        private final long origin;
        private final ArrayDeque<Node> pending = new ArrayDeque<>();
        private final Set<Long> visited = new HashSet<>();
        private final List<Destination> destinations = new ArrayList<>();
        private boolean complete;
        private boolean transportRequested;
        private int searchUsers;
        private @Nullable List<Destination> farthest;

        private List<Destination> ordered(PipeRoutingMode mode) {
            if (mode != PipeRoutingMode.FARTHEST) { return destinations; }
            if (farthest == null) {
                var ordered = new ArrayList<>(destinations);
                ordered.sort(Comparator.comparingInt(Destination::priority).reversed()
                        .thenComparing(Comparator.comparingInt(Destination::distance).reversed())
                        .thenComparingLong(destination -> destination.targetPos().asLong())
                        .thenComparingInt(destination -> destination.targetSide().ordinal()));
                farthest = List.copyOf(ordered);
            }
            return farthest;
        }

        private RouteScan(long revision, BlockPos origin) {
            this.revision = revision;
            this.origin = origin.asLong();
            visited.add(origin.asLong());
            pending.add(new Node(origin.immutable(), 0));
        }
    }
}
