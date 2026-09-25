package com.cosmocraft.trading_cells.feature.logistics.adapters.output;

import com.cosmocraft.trading_cells.feature.logistics.adapters.api.LogisticsResourceAdapters;
import com.cosmocraft.trading_cells.feature.logistics.adapters.input.LogisticsPipeBlock;
import com.cosmocraft.trading_cells.feature.logistics.adapters.input.LogisticsPipeBlockEntity;
import com.cosmocraft.trading_cells.feature.logistics.adapters.input.NetworkTerminalBlock;
import com.cosmocraft.trading_cells.feature.logistics.adapters.input.NetworkTerminalBlockEntity;
import com.cosmocraft.trading_cells.feature.logistics.adapters.input.NetworkTerminalMenu;
import com.cosmocraft.trading_cells.feature.logistics.adapters.input.PipeUpgradeItem;
import com.cosmocraft.trading_cells.feature.logistics.adapters.input.PipeWrenchItem;
import com.cosmocraft.trading_cells.feature.logistics.adapters.neoforge.EnergyLogisticsAdapter;
import com.cosmocraft.trading_cells.feature.logistics.adapters.neoforge.FluidLogisticsAdapter;
import com.cosmocraft.trading_cells.feature.logistics.adapters.neoforge.ItemLogisticsAdapter;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.LogisticsResourceType;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeKind;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeUpgradeTier;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import com.cosmocraft.trading_cells.platform.neoforge.registration.Registration;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

public final class LogisticsRegistrationAdapter {
    private static final Map<PipeKind, DeferredBlock<LogisticsPipeBlock>> PIPE_BLOCKS =
            new EnumMap<>(PipeKind.class);
    private static final Map<PipeKind, DeferredItem<BlockItem>> PIPE_ITEMS = new EnumMap<>(PipeKind.class);
    private static final Map<PipeUpgradeTier, DeferredItem<PipeUpgradeItem>> UPGRADE_ITEMS =
            new EnumMap<>(PipeUpgradeTier.class);

    static {
        for (PipeKind kind : PipeKind.values()) {
            String id = kind.serializedName() + "_pipe";
            DeferredBlock<LogisticsPipeBlock> block = Registration.BLOCKS.register(
                    id,
                    () -> new LogisticsPipeBlock(pipeProperties(id, kind), kind)
            );
            PIPE_BLOCKS.put(kind, block);
            PIPE_ITEMS.put(kind, Registration.ITEMS.register(
                    id,
                    () -> new BlockItem(block.get(), itemProperties(id))
            ));
        }
        for (PipeUpgradeTier tier : PipeUpgradeTier.values()) {
            if (tier == PipeUpgradeTier.BARE) {
                continue;
            }
            String id = tier.serializedName() + "_pipe_upgrade";
            UPGRADE_ITEMS.put(tier, Registration.ITEMS.register(
                    id,
                    () -> new PipeUpgradeItem(itemProperties(id), tier)
            ));
        }
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LogisticsPipeBlockEntity>>
            PIPE_BLOCK_ENTITY = Registration.BLOCK_ENTITY_TYPES.register(
                    "logistics_pipe",
                    () -> new BlockEntityType<>(
                            LogisticsPipeBlockEntity::new,
                            PIPE_BLOCKS.values().stream().map(DeferredBlock::get).toArray(Block[]::new)
                    )
            );
    public static final DeferredBlock<NetworkTerminalBlock> TERMINAL_BLOCK = Registration.BLOCKS.register(
            "network_terminal",
            () -> new NetworkTerminalBlock(terminalProperties("network_terminal"), false)
    );
    public static final DeferredBlock<NetworkTerminalBlock> CRAFTING_TERMINAL_BLOCK = Registration.BLOCKS.register(
            "network_crafting_terminal",
            () -> new NetworkTerminalBlock(terminalProperties("network_crafting_terminal"), true)
    );
    public static final DeferredItem<BlockItem> TERMINAL_ITEM = Registration.ITEMS.register(
            "network_terminal",
            () -> new BlockItem(TERMINAL_BLOCK.get(), itemProperties("network_terminal"))
    );
    public static final DeferredItem<BlockItem> CRAFTING_TERMINAL_ITEM = Registration.ITEMS.register(
            "network_crafting_terminal",
            () -> new BlockItem(CRAFTING_TERMINAL_BLOCK.get(), itemProperties("network_crafting_terminal"))
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NetworkTerminalBlockEntity>>
            TERMINAL_BLOCK_ENTITY = Registration.BLOCK_ENTITY_TYPES.register(
                    "network_terminal",
                    () -> new BlockEntityType<>(
                            NetworkTerminalBlockEntity::new,
                            TERMINAL_BLOCK.get(),
                            CRAFTING_TERMINAL_BLOCK.get()
                    )
            );
    public static final DeferredHolder<MenuType<?>, MenuType<NetworkTerminalMenu>> TERMINAL_MENU =
            Registration.MENU_TYPES.register(
                    "network_terminal",
                    () -> new MenuType<>((containerId, inventory) ->
                            new NetworkTerminalMenu(containerId, inventory, false), FeatureFlags.VANILLA_SET)
            );
    public static final DeferredHolder<MenuType<?>, MenuType<com.cosmocraft.trading_cells.feature.logistics.adapters.input.PipeConfigurationMenu>> PIPE_MENU =
            Registration.MENU_TYPES.register("pipe_configuration", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(
                    com.cosmocraft.trading_cells.feature.logistics.adapters.input.PipeConfigurationMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<NetworkTerminalMenu>> CRAFTING_TERMINAL_MENU =
            Registration.MENU_TYPES.register(
                    "network_crafting_terminal",
                    () -> new MenuType<>((containerId, inventory) ->
                            new NetworkTerminalMenu(containerId, inventory, true), FeatureFlags.VANILLA_SET)
            );
    public static final DeferredItem<PipeWrenchItem> WRENCH_ITEM = Registration.ITEMS.register(
            "pipe_wrench",
            () -> new PipeWrenchItem(itemProperties("pipe_wrench").stacksTo(1))
    );

    private static boolean adaptersRegistered;

    public static final DeferredItem<com.cosmocraft.trading_cells.feature.logistics.adapters.input.PipeTargetSelectorItem> TARGET_SELECTOR_ITEM = Registration.ITEMS.register(
            "pipe_target_selector", () -> new com.cosmocraft.trading_cells.feature.logistics.adapters.input.PipeTargetSelectorItem(
                    itemProperties("pipe_target_selector").stacksTo(1)));

    private LogisticsRegistrationAdapter() {
    }

    public static DeferredBlock<LogisticsPipeBlock> pipeBlock(PipeKind kind) {
        return PIPE_BLOCKS.get(kind);
    }

    public static DeferredItem<BlockItem> pipeItem(PipeKind kind) {
        return PIPE_ITEMS.get(kind);
    }

    public static DeferredItem<PipeUpgradeItem> upgradeItem(PipeUpgradeTier tier) {
        return UPGRADE_ITEMS.get(tier);
    }

    public static Iterable<DeferredItem<BlockItem>> pipeItems() {
        return PIPE_ITEMS.values();
    }

    public static Iterable<DeferredItem<PipeUpgradeItem>> upgradeItems() {
        return UPGRADE_ITEMS.values();
    }

    public static void load(IEventBus modEventBus) {
        modEventBus.addListener((net.neoforged.neoforge.event.RegisterTooltipAppendersEvent event) ->
                event.registerAppender(net.neoforged.neoforge.common.tooltip.TooltipLocation.POST_CUSTOM,
                        (stack, context, display, player, flag, builder) ->
                                com.cosmocraft.trading_cells.feature.logistics.adapters.input.PipeTargetSelectorItem.appendTooltip(stack, builder)));
        if (!adaptersRegistered) {
            adaptersRegistered = true;
            LogisticsResourceAdapters.register(new ItemLogisticsAdapter());
            LogisticsResourceAdapters.register(new FluidLogisticsAdapter(LogisticsResourceType.FLUID));
            LogisticsResourceAdapters.register(new FluidLogisticsAdapter(LogisticsResourceType.GAS));
            LogisticsResourceAdapters.register(new EnergyLogisticsAdapter());
        }
    }

    private static BlockBehaviour.Properties pipeProperties(String id, PipeKind kind) {
        SoundType sound = switch (kind) {
            case FLUID -> SoundType.COPPER;
            case GAS -> SoundType.CALCITE;
            case UNIVERSAL -> SoundType.METAL;
            case ITEM, ENERGY -> SoundType.METAL;
        };
        return BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, id(id)))
                .strength(1.5F, 6.0F)
                .sound(sound)
                .noOcclusion()
                .pushReaction(PushReaction.BLOCK)
                .isRedstoneConductor((state, getter, pos) -> false)
                .isSuffocating((state, getter, pos) -> false)
                .isViewBlocking((state, getter, pos) -> false);
    }

    private static Item.Properties itemProperties(String id) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id(id)));
    }

    private static BlockBehaviour.Properties terminalProperties(String id) {
        return BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, id(id)))
                .strength(3.0F, 8.0F)
                .sound(SoundType.METAL);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, path);
    }
}
