package com.cosmocraft.trading_cells.feature.trader.adapters.input;

import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.cosmocraft.trading_cells.feature.trader.adapters.minecraft.VillagerPoiAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.FeatureComposition;
import com.cosmocraft.trading_cells.feature.trader.adapters.output.TraderRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.trader.adapters.minecraft.TemporaryTradeDiscountStore;
import com.cosmocraft.trading_cells.feature.trader.application.port.input.VillagerTraderUseCase;
import com.cosmocraft.trading_cells.feature.trader.domain.service.TradeDiscountPolicy;
import com.cosmocraft.trading_cells.platform.neoforge.network.TradingCellExperiencePayload;
import com.cosmocraft.trading_cells.platform.neoforge.network.TradingCellMenuSyncPayload;
import com.cosmocraft.trading_cells.platform.neoforge.trading.MerchantOfferComparator;
import com.cosmocraft.trading_cells.platform.neoforge.fluid.ExperienceFluidHandler;
import com.cosmocraft.trading_cells.platform.neoforge.registration.ExperienceFluidRegistration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.ReputationEventType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class VillagerTradingCellBlockEntity extends BlockEntity { // NOSONAR - Minecraft fixes this hierarchy and BlockEntity uses identity, not value-based equals.
    private static final String CURE_DISCOUNT_TAG = "TradingCellsCureDiscount";
    private static final String LEGACY_VILLAGER_DATA_TAG = "StoredVillager";
    private static final String ENTITY_KIND_TAG = "StoredEntityKind";
    private static final String ENTITY_DATA_TAG = "StoredEntity";
    private static final String POI_STACK_TAG = "StoredPoi";
    private static final String TRADE_REFRESH_TICKS_TAG = "TradeRefreshTicks";
    private static final String STORED_EXPERIENCE_TAG = "StoredExperience";
    private static final int RESTOCK_CHECK_INTERVAL_TICKS = 300;

    private static final String VILLAGER_DATA_TAG = "VillagerData";
    private static final String PROFESSION_TAG = "profession";
    private static final String LEVEL_TAG = "level";
    private static final String XP_TAG = "Xp";
    private static final String AGE_TAG = "Age";
    private static final String NONE_PROFESSION = "minecraft:none";

    private static final Map<UUID, GlobalPos> OPEN_TRADING_CELLS_BY_PLAYER = new HashMap<>();

    private final VillagerTraderUseCase villagerTradeService = FeatureComposition.villagerTrader();
    private final ExperienceFluidHandler experienceFluidHandler = new ExperienceFluidHandler(
            () -> FluidResource.of(ExperienceFluidRegistration.SOURCE.get()),
            this::getStoredExperienceForFluid,
            this::setStoredExperienceFromFluid,
            () -> Integer.MAX_VALUE,
            false,
            this::markExperienceFluidChanged
    );
    private @Nullable StoredEntityKind storedEntityKind;
    private @Nullable CompoundTag storedEntityData;
    private ItemStack storedPoiStack = ItemStack.EMPTY;
    private @Nullable TradingCellVillager merchantVillager;
    private int tradeRefreshTicks;
    private int storedExperience;
    private int offersRevision;

    void processTick() {
        if (!hasStoredEntity() && merchantVillager == null && tradeRefreshTicks == 0) {
            return;
        }
        tickTradeRefresh();
        tickOpenTradingPriceSync();
    }

    public static void handleResetTradesRequest(Player player, int containerId, int knownOffersRevision) {
        if (!(player.containerMenu instanceof VillagerTradingCellMenu menu)
                || menu.containerId != containerId
                || menu.offersRevision() != knownOffersRevision) {
            return;
        }
        VillagerTradingCellBlockEntity cell = findOpenCell(player);
        if (cell != null && cell.isPlayerStillValid(player)) {
            cell.resetTransientTrades(player);
        }
    }

    public static void handleSelectOfferRequest(
            Player player,
            int containerId,
            int selectedOfferIndex,
            int knownOffersRevision
    ) {
        if (!(player.containerMenu instanceof VillagerTradingCellMenu menu)
                || menu.containerId != containerId
                || menu.offersRevision() != knownOffersRevision) {
            return;
        }
        VillagerTradingCellBlockEntity cell = findOpenCell(player);
        if (cell == null || !cell.isPlayerStillValid(player)) {
            return;
        }
        if (menu.selectOfferFromPacket(player, selectedOfferIndex, knownOffersRevision)) {
            TradingCellVillager villager = cell.getOrCreateMerchantVillager();
            if (villager != null) {
                cell.sendMenuSync(player, villager);
            }
        }
    }

    public static void handleExtractExperienceRequest(Player player, int containerId, byte mode) {
        if (!(player.containerMenu instanceof VillagerTradingCellMenu menu) || menu.containerId != containerId) {
            return;
        }
        VillagerTradingCellBlockEntity cell = findOpenCell(player);
        if (cell != null && cell.isPlayerStillValid(player)) {
            cell.extractStoredExperience(player, mode);
        }
    }

    private static @Nullable VillagerTradingCellBlockEntity findOpenCell(Player player) {
        GlobalPos globalPos = OPEN_TRADING_CELLS_BY_PLAYER.get(player.getUUID());
        MinecraftServer server = player.level().getServer();
        if (globalPos == null || server == null) {
            return null;
        }
        Level level = server.getLevel(globalPos.dimension());
        if (level == null) {
            return null;
        }
        BlockEntity blockEntity = level.getBlockEntity(globalPos.pos());
        if (blockEntity instanceof VillagerTradingCellBlockEntity cell) {
            return cell;
        }
        OPEN_TRADING_CELLS_BY_PLAYER.remove(player.getUUID());
        return null;
    }

    public VillagerTradingCellBlockEntity(BlockPos pos, BlockState blockState) {
        super(TraderRegistrationAdapter.VILLAGER_TRADING_CELL_BLOCK_ENTITY.get(), pos, blockState);
    }

    public boolean hasStoredEntity() {
        return storedEntityKind != null && storedEntityData != null && !storedEntityData.isEmpty();
    }

    public ResourceHandler<FluidResource> experienceFluidHandler() {
        return experienceFluidHandler;
    }

    public boolean hasVillager() {
        return hasStoredEntity() && storedEntityKind == StoredEntityKind.VILLAGER;
    }

    public boolean hasPiglin() {
        return hasStoredEntity() && storedEntityKind == StoredEntityKind.PIGLIN;
    }

    public boolean hasStoredPoi() {
        return !storedPoiStack.isEmpty();
    }

    public ItemStack copyPoiStack() {
        return storedPoiStack.copy();
    }

    public @Nullable CompoundTag copyVillagerData() {
        return copyDataFor(StoredEntityKind.VILLAGER);
    }

    public @Nullable CompoundTag copyPiglinData() {
        return copyDataFor(StoredEntityKind.PIGLIN);
    }

    public @Nullable CompoundTag copyStoredEntityData() {
        CompoundTag entityData = storedEntityData;
        if (storedEntityKind == null || entityData == null || entityData.isEmpty()) {
            return null;
        }
        return entityData.copy();
    }

    private @Nullable CompoundTag copyDataFor(StoredEntityKind kind) {
        if (storedEntityKind != kind || storedEntityData == null || storedEntityData.isEmpty()) {
            return null;
        }
        return storedEntityData.copy();
    }

    public @Nullable String getStoredEntityKindId() {
        return storedEntityKind == null ? null : storedEntityKind.id;
    }

    public @Nullable Entity createStoredEntityForDisplay() {
        Level currentLevel = level;
        StoredEntityKind entityKind = storedEntityKind;
        CompoundTag entityData = storedEntityData;
        if (currentLevel == null || entityKind == null || entityData == null || entityData.isEmpty()) {
            return null;
        }

        return CapturedMobStackAdapter.createEntity(
                capturedKind(entityKind),
                currentLevel,
                entityData,
                worldPosition
        );
    }

    public InteractionResult insertVillagerFromCapturer(ItemStack stack) {
        if (hasStoredEntity()) {
            return InteractionResult.SUCCESS_SERVER;
        }

        CompoundTag villagerData = CapturedMobStackAdapter.copyData(CapturedMobKind.VILLAGER, stack);
        if (villagerData == null) {
            return InteractionResult.PASS;
        }
        if (CapturedMobStackAdapter.isBaby(CapturedMobKind.VILLAGER, villagerData)) {
            return InteractionResult.SUCCESS_SERVER;
        }

        setStoredEntity(StoredEntityKind.VILLAGER, villagerData);
        refreshVillagerProfessionFromPoi();
        CapturedMobStackAdapter.clearData(CapturedMobKind.VILLAGER, stack);
        return InteractionResult.SUCCESS_SERVER;
    }

    public InteractionResult extractVillagerToCapturer(ItemStack stack, Player player) {
        if (!hasStoredEntity()) {
            return InteractionResult.SUCCESS_SERVER;
        }

        if (!hasVillager()) {
            return InteractionResult.SUCCESS_SERVER;
        }

        if (CapturedMobStackAdapter.isFilledCapturer(CapturedMobKind.VILLAGER, stack)) {
            return InteractionResult.SUCCESS_SERVER;
        }

        saveProxyToStoredData();
        CompoundTag villagerData = copyVillagerData();
        if (villagerData == null) {
            return InteractionResult.FAIL;
        }

        ItemStack targetStack = createSingleCapturerTarget(stack);
        CapturedMobStackAdapter.setData(CapturedMobKind.VILLAGER, targetStack, villagerData);
        finishStackedExtraction(player, stack, targetStack);
        clearStoredEntity();
        return InteractionResult.SUCCESS_SERVER;
    }

    public InteractionResult extractPiglinToCapturer(ItemStack stack, Player player) {
        if (!hasStoredEntity()) {
            return InteractionResult.SUCCESS_SERVER;
        }

        if (!hasPiglin()) {
            return InteractionResult.SUCCESS_SERVER;
        }

        if (CapturedMobStackAdapter.isFilledCapturer(CapturedMobKind.PIGLIN, stack)) {
            return InteractionResult.SUCCESS_SERVER;
        }

        CompoundTag piglinData = copyPiglinData();
        if (piglinData == null) {
            return InteractionResult.FAIL;
        }

        ItemStack targetStack = createSingleCapturerTarget(stack);
        CapturedMobStackAdapter.setData(CapturedMobKind.PIGLIN, targetStack, piglinData);
        finishStackedExtraction(player, stack, targetStack);
        clearStoredEntity();
        return InteractionResult.SUCCESS_SERVER;
    }

    public InteractionResult insertPoiFromStack(ItemStack stack, Player player) {
        String professionId = getProfessionForPoiStack(stack);
        if (professionId == null) {
            return InteractionResult.PASS;
        }

        boolean replacingPoi = hasStoredPoi();
        if (replacingPoi) {
            saveProxyToStoredData();
            if (hasPersistentVillagerProfession()) {
                return InteractionResult.SUCCESS_SERVER;
            }

            ItemStack previousPoi = storedPoiStack.copy();
            if (!player.getInventory().add(previousPoi)) {
                return InteractionResult.SUCCESS_SERVER;
            }
        }

        storedPoiStack = stack.copyWithCount(1);
        stack.shrink(1);
        refreshVillagerProfessionFromPoi();
        markChangedAndSync();
        return InteractionResult.SUCCESS_SERVER;
    }

    public InteractionResult extractPoiToPlayer(Player player) {
        if (!hasStoredPoi()) {
            return InteractionResult.SUCCESS_SERVER;
        }

        ItemStack poiToReturn = storedPoiStack.copy();
        if (!player.getInventory().add(poiToReturn)) {
            return InteractionResult.SUCCESS_SERVER;
        }

        storedPoiStack = ItemStack.EMPTY;
        clearTransientVillagerProfession();
        markChangedAndSync();
        return InteractionResult.SUCCESS_SERVER;
    }

    public InteractionResult releaseStoredEntityIntoWorld(BlockPos releasePos) {
        if (!hasStoredEntity()) {
            return InteractionResult.SUCCESS_SERVER;
        }

        if (level == null || level.isClientSide() || storedEntityKind == null || storedEntityData == null) {
            return InteractionResult.SUCCESS;
        }

        saveProxyToStoredData();
        Entity entity = createEntity(level, storedEntityKind, storedEntityData, releasePos);
        if (entity == null || !level.addFreshEntity(entity)) {
            return InteractionResult.FAIL;
        }

        clearStoredEntity();
        return InteractionResult.SUCCESS_SERVER;
    }

    public boolean resetTransientTrades(Player player) {
        if (!hasVillager()) {
            return false;
        }

        saveProxyToStoredData();
        if (storedEntityData == null) {
            return false;
        }
        if (isVillagerBaby(storedEntityData)) {
            return false;
        }
        if (isVillagerProfessionPersistent(storedEntityData)) {
            return false;
        }

        String professionId = getProfessionForPoiStack(storedPoiStack);
        if (professionId == null) {
            return false;
        }

        TradingCellVillager villager = getOrCreateMerchantVillager();
        if (villager == null || villager.getVillagerXp() != 0) {
            return false;
        }

        VillagerTradingCellMenu openMenu = player.containerMenu instanceof VillagerTradingCellMenu menu ? menu : null;
        Optional<VillagerTradingCellMenu.PaymentReturnPlan> paymentReturnPlan = Optional.empty();
        if (openMenu != null) {
            paymentReturnPlan = openMenu.simulatePaymentReturn(player);
            if (paymentReturnPlan.isEmpty()) {
                return false;
            }
        }

        CompoundTag previousStoredEntityData = storedEntityData.copy();
        MerchantOffer previouslySelected = openMenu == null ? null : openMenu.selectedOffer();

        clearVillagerProfession(storedEntityData);
        setVillagerProfession(storedEntityData, professionId);
        reloadMerchantVillagerFromStoredData(villager);
        villager.setTradingPlayer(player);

        MerchantOffers offers = villager.getOffers();
        if (offers.isEmpty()) {
            storedEntityData = previousStoredEntityData;
            reloadMerchantVillagerFromStoredData(villager);
            villager.setTradingPlayer(player);
            return false;
        }
        if (openMenu != null) {
            openMenu.commitPaymentReturn(player, paymentReturnPlan.orElseThrow());
            openMenu.setSelectionHint(MerchantOfferComparator.findEquivalentIndex(offers, previouslySelected));
        }

        saveProxyToStoredData();
        tradeRefreshTicks = 0;
        markOffersChanged();
        sendMenuSync(player, villager);
        return true;
    }

    public InteractionResult openTrade(Player player) {
        if (!hasStoredEntity()) {
            return InteractionResult.SUCCESS_SERVER;
        }

        if (!hasVillager()) {
            return InteractionResult.SUCCESS_SERVER;
        }

        refreshVillagerProfessionFromPoi();
        TradingCellVillager villager = getOrCreateMerchantVillager();
        if (villager == null) {
            return InteractionResult.FAIL;
        }

        if (villager.isBaby()) {
            return InteractionResult.SUCCESS_SERVER;
        }

        MerchantOffers offers = villager.getOffers();
        if (villagerTradeService.infiniteTrades()) {
            resetUsedOffers(offers);
        }
        if (offers.isEmpty()) {
            return InteractionResult.SUCCESS_SERVER;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        registerTradingPlayer(player);
        villager.preparePrices(player);
        villager.setTradingPlayer(player);
        serverPlayer.openMenu(new SimpleMenuProvider(
                (containerId, inventory, menuPlayer) -> new VillagerTradingCellMenu(
                        containerId,
                        inventory,
                        villager
                ),
                villager.getDisplayName()
        )).ifPresent(containerId -> sendMenuSync(player, villager));
        return InteractionResult.SUCCESS_SERVER;
    }

    public boolean canResetTrades() {
        if (!hasVillager() || storedEntityData == null || isVillagerBaby(storedEntityData)) {
            return false;
        }
        TradingCellVillager villager = getOrCreateMerchantVillager();
        return villager != null
                && villager.getVillagerXp() == 0
                && !isVillagerProfessionPersistent(storedEntityData)
                && getProfessionForPoiStack(storedPoiStack) != null;
    }

    private void sendMenuSync(Player player, TradingCellVillager villager) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || !(serverPlayer.containerMenu instanceof VillagerTradingCellMenu menu)) {
            return;
        }
        MerchantOffers currentOffers = villager.getOffers();
        VillagerData currentData = villager.getVillagerData();
        menu.applyServerState(new VillagerTradingCellMenu.ServerState(
                currentOffers,
                new VillagerTradingCellMenu.MerchantState(
                        currentData,
                        currentData.level(),
                        villager.getVillagerXp()
                ),
                new VillagerTradingCellMenu.MenuState(
                        storedExperience,
                        menu.selectedOfferIndex(),
                        villager.showProgressBar(),
                        villager.canRestock(),
                        canResetTrades(),
                        offersRevision
                )
        ));
        PacketDistributor.sendToPlayer(
                serverPlayer,
                new TradingCellMenuSyncPayload(
                        menu.containerId,
                        currentOffers,
                        currentData,
                        currentData.level(),
                        villager.getVillagerXp(),
                        storedExperience,
                        menu.selectedOfferIndex(),
                        villager.showProgressBar(),
                        villager.canRestock(),
                        canResetTrades(),
                        offersRevision
                )
        );
    }

    public void discardContentsAfterBlockDrop() {
        storedEntityKind = null;
        storedEntityData = null;
        storedPoiStack = ItemStack.EMPTY;
        merchantVillager = null;
        tradeRefreshTicks = 0;
        storedExperience = 0;
        setChanged();
    }

    @Override
    protected void loadAdditional(@NonNull ValueInput input) {
        super.loadAdditional(input);
        storedEntityKind = input.getString(ENTITY_KIND_TAG).map(StoredEntityKind::fromId).orElse(null);
        storedEntityData = input.read(ENTITY_DATA_TAG, CompoundTag.CODEC).orElse(null);
        storedPoiStack = input.read(POI_STACK_TAG, ItemStack.CODEC).orElse(ItemStack.EMPTY);
        tradeRefreshTicks = Math.max(0, input.getIntOr(TRADE_REFRESH_TICKS_TAG, 0));
        storedExperience = Math.max(0, input.getIntOr(STORED_EXPERIENCE_TAG, 0));

        // Backwards compatibility with saves made by the earlier villager-only version.
        if (storedEntityData == null) {
            storedEntityData = input.read(LEGACY_VILLAGER_DATA_TAG, CompoundTag.CODEC).orElse(null);
            if (storedEntityData != null && !storedEntityData.isEmpty()) {
                storedEntityKind = StoredEntityKind.VILLAGER;
            }
        }

        if (storedEntityKind == null || storedEntityData == null || storedEntityData.isEmpty()) {
            storedEntityKind = null;
            storedEntityData = null;
        }
        if (storedPoiStack.isEmpty()) {
            storedPoiStack = ItemStack.EMPTY;
        }

        merchantVillager = null;
    }

    @Override
    protected void saveAdditional(@NonNull ValueOutput output) {
        saveProxyToStoredData();
        super.saveAdditional(output);
        if (storedEntityKind != null && storedEntityData != null && !storedEntityData.isEmpty()) {
            output.putString(ENTITY_KIND_TAG, storedEntityKind.id);
            output.store(ENTITY_DATA_TAG, CompoundTag.CODEC, storedEntityData);
        }
        if (!storedPoiStack.isEmpty()) {
            output.store(POI_STACK_TAG, ItemStack.CODEC, storedPoiStack);
        }
        if (tradeRefreshTicks > 0) {
            output.putInt(TRADE_REFRESH_TICKS_TAG, tradeRefreshTicks);
        }
        if (storedExperience > 0) {
            output.putInt(STORED_EXPERIENCE_TAG, storedExperience);
        }
    }

    @Override
    public @NonNull Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    private void setStoredEntity(StoredEntityKind kind, CompoundTag entityData) {
        storedEntityKind = kind;
        storedEntityData = entityData.copy();
        merchantVillager = null;
        tradeRefreshTicks = 0;
        markChangedAndSync();
    }

    private @Nullable TradingCellVillager getOrCreateMerchantVillager() {
        if (merchantVillager != null) {
            return merchantVillager;
        }

        Level currentLevel = level;
        if (currentLevel == null || !hasVillager() || storedEntityData == null) {
            return null;
        }

        TradingCellVillager villager = new TradingCellVillager(currentLevel, this);
        villager.load(TagValueInput.create(
                ProblemReporter.DISCARDING,
                currentLevel.registryAccess(),
                storedEntityData.copy()
        ));
        villager.setPos(worldPosition.getX() + 0.5D, worldPosition.getY(), worldPosition.getZ() + 0.5D);
        villager.setPersistenceRequired();
        merchantVillager = villager;
        return merchantVillager;
    }

    private void reloadMerchantVillagerFromStoredData(TradingCellVillager villager) {
        if (level == null || storedEntityData == null) {
            return;
        }
        villager.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), storedEntityData.copy()));
        villager.setPos(worldPosition.getX() + 0.5D, worldPosition.getY(), worldPosition.getZ() + 0.5D);
        villager.setPersistenceRequired();
        merchantVillager = villager;
    }

    private static @Nullable Entity createEntity(Level level, StoredEntityKind kind, CompoundTag entityData, BlockPos position) {
        return CapturedMobStackAdapter.createEntity(capturedKind(kind), level, entityData, position);
    }

    private void saveProxyToStoredData() {
        if (merchantVillager != null && storedEntityKind == StoredEntityKind.VILLAGER) {
            storedEntityData = CapturedMobStackAdapter.createVillagerData(merchantVillager);
        }
    }

    private static CapturedMobKind capturedKind(StoredEntityKind kind) {
        return kind == StoredEntityKind.VILLAGER
                ? CapturedMobKind.VILLAGER
                : CapturedMobKind.PIGLIN;
    }

    private void refreshVillagerProfessionFromPoi() {
        if (!hasVillager() || storedEntityData == null) {
            return;
        }

        saveProxyToStoredData();
        if (isVillagerProfessionPersistent(storedEntityData)) {
            return;
        }
        boolean changed;
        if (isVillagerBaby(storedEntityData)) {
            changed = clearVillagerProfession(storedEntityData);
        } else {
            String professionId = getProfessionForPoiStack(storedPoiStack);
            changed = professionId == null
                    ? clearVillagerProfession(storedEntityData)
                    : setVillagerProfession(storedEntityData, professionId);
        }
        if (changed) {
            merchantVillager = null;
            tradeRefreshTicks = 0;
            markChangedAndSync();
        }
    }

    private void clearTransientVillagerProfession() {
        if (!hasVillager() || storedEntityData == null) {
            return;
        }

        saveProxyToStoredData();
        if (!isVillagerProfessionPersistent(storedEntityData)
                && clearVillagerProfession(storedEntityData)) {
            merchantVillager = null;
            tradeRefreshTicks = 0;
            markChangedAndSync();
        }
    }

    private boolean hasPersistentVillagerProfession() {
        CompoundTag entityData = storedEntityData;
        return storedEntityKind == StoredEntityKind.VILLAGER
                && entityData != null
                && !entityData.isEmpty()
                && isVillagerProfessionPersistent(entityData);
    }

    private static boolean isVillagerProfessionPersistent(CompoundTag villagerData) {
        if (villagerData.getInt(XP_TAG).orElse(0) > 0) {
            return true;
        }

        return villagerData.getCompound(VILLAGER_DATA_TAG)
                .flatMap(data -> data.getInt(LEVEL_TAG))
                .map(level -> level > 1)
                .orElse(false);
    }

    private static boolean isVillagerBaby(CompoundTag villagerData) {
        return villagerData.getInt(AGE_TAG).orElse(0) < 0;
    }

    private static boolean setVillagerProfession(CompoundTag villagerData, String professionId) {
        CompoundTag data = villagerData.getCompound(VILLAGER_DATA_TAG).map(CompoundTag::copy).orElseGet(CompoundTag::new);
        String previousProfession = data.getString(PROFESSION_TAG).orElse(NONE_PROFESSION);
        boolean changed = !professionId.equals(previousProfession);
        data.putString(PROFESSION_TAG, professionId);
        if (data.getInt(LEVEL_TAG).isEmpty()) {
            data.putInt(LEVEL_TAG, 1);
        }
        villagerData.put(VILLAGER_DATA_TAG, data);
        if (changed) {
            villagerData.remove("Offers");
        }
        return changed;
    }

    private static boolean clearVillagerProfession(CompoundTag villagerData) {
        boolean changed = setVillagerProfession(villagerData, NONE_PROFESSION);
        if (villagerData.getInt(XP_TAG).orElse(0) != 0) {
            villagerData.putInt(XP_TAG, 0);
            changed = true;
        }
        return changed;
    }

    private @Nullable String getProfessionForPoiStack(ItemStack stack) {
        return level == null ? null : VillagerPoiAdapter.professionFor(level, stack);
    }

    private void addStoredExperience(int amount, @Nullable Player tradingPlayer) {
        if (amount <= 0) {
            return;
        }
        storedExperience = (int) Math.min(Integer.MAX_VALUE, (long) storedExperience + amount);
        boolean batchActive = merchantVillager != null && merchantVillager.isTradeBatchActive();
        if (batchActive) {
            return;
        }
        setChanged();
        if (tradingPlayer != null) {
            syncStoredExperience(tradingPlayer);
        }
    }

    private void setStoredExperienceFromFluid(int amount) {
        storedExperience = Math.max(0, amount);
    }

    private int getStoredExperienceForFluid() {
        return storedExperience;
    }

    private void markExperienceFluidChanged() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
        if (merchantVillager != null && merchantVillager.getTradingPlayer() != null) {
            syncStoredExperience(merchantVillager.getTradingPlayer());
        }
    }

    private void extractStoredExperience(Player player, byte mode) {
        if (level == null || level.isClientSide()) {
            return;
        }
        if (storedExperience > 0) {
            int extracted = storedExperience;
            if (mode == com.cosmocraft.trading_cells.platform.neoforge.network.ExtractTradingCellExperiencePayload.NEXT_LEVEL) {
                int needed = Math.max(
                        1,
                        Mth.ceil((1.0F - player.experienceProgress) * player.getXpNeededForNextLevel())
                );
                extracted = Math.min(storedExperience, needed);
            }
            storedExperience -= extracted;
            player.giveExperiencePoints(extracted);
            markChangedAndSync();
        }
        syncStoredExperience(player);
        TradingCellVillager villager = getOrCreateMerchantVillager();
        if (villager != null) {
            sendMenuSync(player, villager);
        }
    }

    private void syncStoredExperience(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(
                    serverPlayer,
                    new TradingCellExperiencePayload(serverPlayer.containerMenu.containerId, storedExperience)
            );
        }
    }

    private void registerTradingPlayer(Player player) {
        if (level != null) {
            OPEN_TRADING_CELLS_BY_PLAYER.put(player.getUUID(), GlobalPos.of(level.dimension(), worldPosition));
        }
    }

    private static ItemStack createSingleCapturerTarget(ItemStack heldStack) {
        if (heldStack.getCount() <= 1) {
            return heldStack;
        }
        return new ItemStack(heldStack.getItem());
    }

    private static void finishStackedExtraction(Player player, ItemStack heldStack, ItemStack targetStack) {
        if (targetStack == heldStack) {
            return;
        }

        heldStack.shrink(1);
        if (!player.getInventory().add(targetStack)) {
            player.drop(targetStack, false);
        }
    }

    private void clearStoredEntity() {
        storedEntityKind = null;
        storedEntityData = null;
        merchantVillager = null;
        tradeRefreshTicks = 0;
        markChangedAndSync();
    }


    private void tickOpenTradingPriceSync() {
        if (!(level instanceof ServerLevel) || merchantVillager == null || merchantVillager.getTradingPlayer() == null) {
            return;
        }
        long gameTime = level.getGameTime();
        if (merchantVillager.hasExpiredTemporaryDiscount(gameTime) || gameTime % 20L == 0L) {
            if (merchantVillager.sendCurrentOffersIfPricesChanged()) {
                saveProxyToStoredData();
            }
        }
    }

    private void tickTradeRefresh() {
        if (!(level instanceof ServerLevel serverLevel) || !hasEmployedAdultVillager()) {
            if (tradeRefreshTicks != 0) {
                tradeRefreshTicks = 0;
                setChanged();
            }
            return;
        }

        TradingCellVillager villager = getOrCreateMerchantVillager();
        if (villager == null) {
            tradeRefreshTicks = 0;
            return;
        }

        if (villagerTradeService.infiniteTrades()) {
            tradeRefreshTicks = 0;
            if (resetUsedOffers(villager.getOffers())) {
                saveProxyToStoredData();
                markOffersChanged();
                villager.sendCurrentOffers();
            }
            return;
        }

        tradeRefreshTicks++;
        if (tradeRefreshTicks < RESTOCK_CHECK_INTERVAL_TICKS) {
            if (tradeRefreshTicks % 20 == 0) {
                setChanged();
            }
            return;
        }

        tradeRefreshTicks = 0;
        boolean restocked = villager.shouldRestock(serverLevel);
        if (restocked) {
            villager.restock();
        }
        // shouldRestock also updates the villager's internal day/restock tracking,
        // so persist it even when this particular check did not replenish stock.
        saveProxyToStoredData();
        if (restocked) {
            markOffersChanged();
            villager.sendCurrentOffers();
        } else {
            markChangedAndSync();
        }
    }

    private static boolean resetUsedOffers(MerchantOffers offers) {
        boolean changed = false;
        for (MerchantOffer offer : offers) {
            if (offer.needsRestock()) {
                offer.resetUses();
                changed = true;
            }
        }
        return changed;
    }

    private boolean hasEmployedAdultVillager() {
        if (!hasVillager() || storedEntityData == null || isVillagerBaby(storedEntityData)) {
            return false;
        }
        String profession = storedEntityData.getCompound(VILLAGER_DATA_TAG)
                .flatMap(data -> data.getString(PROFESSION_TAG))
                .orElse(NONE_PROFESSION);
        return !profession.isBlank() && !NONE_PROFESSION.equals(profession);
    }

    private void markChangedAndSync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void markOffersChanged() {
        offersRevision++;
        markChangedAndSync();
    }

    private boolean isPlayerStillValid(Player player) {
        return !isRemoved()
                && level != null
                && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(
                        worldPosition.getX() + 0.5D,
                        worldPosition.getY() + 0.5D,
                        worldPosition.getZ() + 0.5D
                ) <= 64.0D;
    }

    private enum StoredEntityKind {
        VILLAGER("villager"),
        PIGLIN("piglin");

        private final String id;

        StoredEntityKind(String id) {
            this.id = id;
        }

        private static @Nullable StoredEntityKind fromId(String id) {
            for (StoredEntityKind kind : values()) {
                if (kind.id.equals(id)) {
                    return kind;
                }
            }
            return null;
        }
    }

    private static final class TradingCellVillager extends Villager implements TradeBatchMerchant { // NOSONAR - Minecraft requires Entity identity and framework inheritance for this server-side proxy.
        private final VillagerTradingCellBlockEntity owner;
        private @Nullable UUID currentTradingPlayerId;
        private int tradeBatchDepth;
        private boolean tradeBatchStateChanged;
        private boolean tradeBatchOffersChanged;
        private long nextTemporaryDiscountExpiry = Long.MAX_VALUE;
        private int preparedReputation = Integer.MIN_VALUE;
        private int preparedCareerDiscount = Integer.MIN_VALUE;
        private int preparedCureDiscount = Integer.MIN_VALUE;
        private int preparedHeroAmplifier = Integer.MIN_VALUE;

        private TradingCellVillager(@NonNull Level level, VillagerTradingCellBlockEntity owner) {
            super(CapturedMobStackAdapter.villagerType(), Objects.requireNonNull(level, "level"));
            this.owner = Objects.requireNonNull(owner, "owner");
        }

        @Override
        public void beginTradeBatch() {
            tradeBatchDepth++;
        }

        @Override
        public void endTradeBatch() {
            if (tradeBatchDepth == 0) {
                return;
            }
            tradeBatchDepth--;
            if (tradeBatchDepth != 0) {
                return;
            }

            boolean stateChanged = tradeBatchStateChanged;
            boolean offersChanged = tradeBatchOffersChanged;
            tradeBatchStateChanged = false;
            tradeBatchOffersChanged = false;
            if (!stateChanged) {
                return;
            }

            owner.saveProxyToStoredData();
            if (offersChanged) {
                owner.markOffersChanged();
                Player tradingPlayer = getTradingPlayer();
                if (tradingPlayer != null) {
                    sendCurrentOffers();
                }
            } else {
                owner.markChangedAndSync();
            }
        }

        private boolean isTradeBatchActive() {
            return tradeBatchDepth > 0;
        }

        @Override
        public void setTradingPlayer(
                @Nullable Player player // NOSONAR - Merchant uses null to close the active trading session.
        ) {
            super.setTradingPlayer(player);
            invalidatePreparedPriceContext();
            if (player == null) {
                if (currentTradingPlayerId != null) {
                    unregisterTradingPlayer(currentTradingPlayerId);
                    currentTradingPlayerId = null;
                }
                owner.saveProxyToStoredData();
                owner.markChangedAndSync();
            } else {
                currentTradingPlayerId = player.getUUID();
                owner.registerTradingPlayer(player);
            }
        }

        private static void unregisterTradingPlayer(UUID playerId) {
            OPEN_TRADING_CELLS_BY_PLAYER.remove(playerId);
        }

        @Override
        protected void rewardTradeXp(@NonNull MerchantOffer offer) {
            setVillagerXp(getVillagerXp() + offer.getXp());
            if (!offer.shouldRewardExp()) {
                return;
            }

            int rewardExperience = 3 + getRandom().nextInt(4);
            int currentLevel = getVillagerData().level();
            if (VillagerData.canLevelUp(currentLevel)
                    && getVillagerXp() >= VillagerData.getMaxXpPerLevel(currentLevel)) {
                rewardExperience += 5;
            }
            owner.addStoredExperience(rewardExperience, getTradingPlayer());
        }

        @Override
        public void notifyTrade(@NonNull MerchantOffer offer) {
            Player tradingPlayer = getTradingPlayer();
            boolean infiniteTrades = owner.villagerTradeService.infiniteTrades();
            super.notifyTrade(offer);
            markTemporaryDiscount(offer);
            if (infiniteTrades) {
                offer.resetUses();
            }
            forceCareerLevelUpdate();
            if (tradingPlayer != null && level() instanceof ServerLevel serverLevel) {
                serverLevel.onReputationEvent(ReputationEventType.TRADE, tradingPlayer, this);
                preparePrices(tradingPlayer);
            }
            if (isTradeBatchActive()) {
                tradeBatchStateChanged = true;
                tradeBatchOffersChanged = true;
                return;
            }
            owner.saveProxyToStoredData();
            owner.markOffersChanged();
            if (tradingPlayer != null) {
                sendCurrentOffers();
            }
        }

        private void sendCurrentOffers() {
            Player tradingPlayer = getTradingPlayer();
            if (tradingPlayer == null || getOffers().isEmpty()) {
                return;
            }
            if (owner.villagerTradeService.infiniteTrades()) {
                resetUsedOffers(getOffers());
            }
            preparePrices(tradingPlayer);
            sendPreparedCurrentOffers(tradingPlayer);
        }

        private boolean sendCurrentOffersIfPricesChanged() {
            Player tradingPlayer = getTradingPlayer();
            if (tradingPlayer == null || getOffers().isEmpty()) {
                return false;
            }
            if (!hasExpiredTemporaryDiscount(level().getGameTime())
                    && !priceContextChanged(tradingPlayer)) {
                return false;
            }
            preparePrices(tradingPlayer);
            sendPreparedCurrentOffers(tradingPlayer);
            return true;
        }

        private void sendPreparedCurrentOffers(Player tradingPlayer) {
            if (tradingPlayer.containerMenu instanceof VillagerTradingCellMenu) {
                owner.sendMenuSync(tradingPlayer, this);
            } else {
                tradingPlayer.sendMerchantOffers(
                        tradingPlayer.containerMenu.containerId,
                        getOffers(),
                        getVillagerData().level(),
                        getVillagerXp(),
                        showProgressBar(),
                        canRestock()
                );
            }
        }

        private void preparePrices(Player player) {
            MerchantOffers offers = getOffers();
            for (MerchantOffer offer : offers) {
                offer.resetSpecialPriceDiff();
            }

            int reputation = getPlayerReputation(player);
            int careerDiscount = Math.max(0, getVillagerData().level() - 1);
            int cureDiscount = getPersistentData().getInt(CURE_DISCOUNT_TAG).orElse(0);
            MobEffectInstance hero = player.getEffect(MobEffects.HERO_OF_THE_VILLAGE);
            TemporaryTradeDiscountStore.ActiveDiscounts temporaryDiscounts =
                    TemporaryTradeDiscountStore.activeDiscounts(
                            getPersistentData(),
                            offers,
                            level().getGameTime(),
                            registryAccess()
                    );
            nextTemporaryDiscountExpiry = temporaryDiscounts.nextExpiry();
            for (MerchantOffer offer : offers) {
                if (reputation != 0) {
                    offer.addToSpecialPriceDiff(-Mth.floor(reputation * offer.getPriceMultiplier()));
                }
                if (careerDiscount > 0) {
                    offer.addToSpecialPriceDiff(-careerDiscount);
                }
                if (cureDiscount > 0) {
                    offer.addToSpecialPriceDiff(-cureDiscount);
                }
                if (temporaryDiscounts.appliesTo(offer)) {
                    offer.addToSpecialPriceDiff(-TradeDiscountPolicy.TEMPORARY_DISCOUNT_PER_OFFER);
                }
                if (hero != null) {
                    double modifier = 0.3D + 0.0625D * hero.getAmplifier();
                    int reduction = (int) Math.floor(modifier * offer.getBaseCostA().getCount());
                    offer.addToSpecialPriceDiff(-Math.max(reduction, 1));
                }
            }
            preparedReputation = reputation;
            preparedCareerDiscount = careerDiscount;
            preparedCureDiscount = cureDiscount;
            preparedHeroAmplifier = hero == null ? -1 : hero.getAmplifier();
        }

        private boolean priceContextChanged(Player player) {
            MobEffectInstance hero = player.getEffect(MobEffects.HERO_OF_THE_VILLAGE);
            return preparedReputation != getPlayerReputation(player)
                    || preparedCareerDiscount != Math.max(0, getVillagerData().level() - 1)
                    || preparedCureDiscount != getPersistentData().getInt(CURE_DISCOUNT_TAG).orElse(0)
                    || preparedHeroAmplifier != (hero == null ? -1 : hero.getAmplifier());
        }

        private void invalidatePreparedPriceContext() {
            preparedReputation = Integer.MIN_VALUE;
            preparedCareerDiscount = Integer.MIN_VALUE;
            preparedCureDiscount = Integer.MIN_VALUE;
            preparedHeroAmplifier = Integer.MIN_VALUE;
        }

        private boolean hasExpiredTemporaryDiscount(long gameTime) {
            return gameTime >= nextTemporaryDiscountExpiry;
        }


        private void markTemporaryDiscount(MerchantOffer offer) {
            TemporaryTradeDiscountStore.markOffer(
                    getPersistentData(),
                    getOffers(),
                    offer,
                    level().getGameTime(),
                    registryAccess()
            );
        }

        private void forceCareerLevelUpdate() {
            if (!(level() instanceof ServerLevel serverLevel)) {
                return;
            }

            while (VillagerData.canLevelUp(getVillagerData().level())
                    && getVillagerXp() >= VillagerData.getMaxXpPerLevel(getVillagerData().level())) {
                setVillagerData(getVillagerData().withLevel(getVillagerData().level() + 1));
                updateTrades(serverLevel);
            }
        }

        @Override
        public void notifyTradeUpdated(@NonNull ItemStack stack) {
            super.notifyTradeUpdated(stack);
            if (isTradeBatchActive()) {
                tradeBatchStateChanged = true;
                return;
            }
            owner.saveProxyToStoredData();
            owner.markChangedAndSync();
        }

        @Override
        public boolean stillValid(@NonNull Player player) {
            return owner.isPlayerStillValid(player);
        }
    }
}
