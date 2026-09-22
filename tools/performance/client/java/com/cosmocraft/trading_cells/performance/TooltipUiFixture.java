package com.cosmocraft.trading_cells.performance;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.platform.neoforge.client.TradingCellsItemTooltips;
import com.mojang.datafixers.util.Either;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.ModList;

/** Exercises ordinary and creative tooltips through the real client event bus. */
final class TooltipUiFixture {
    private static volatile boolean prepared;
    private static boolean opened;
    private static boolean checked;
    private static boolean rendered;
    private static int ownedItems;
    private static long started;

    private TooltipUiFixture() { }

    static boolean active() {
        return System.getProperty("trading_cells.performance.client.uiFixture", "").equals("tooltips");
    }

    static boolean ready() { return checked && rendered; }

    static void prepare(ServerPlayer player) {
        player.setGameMode(GameType.CREATIVE);
        player.getInventory().clearContent();
        player.getInventory().setItem(0, MobFarmRegistrationAdapter.ITEM.get().getDefaultInstance());
        player.inventoryMenu.broadcastChanges();
        prepared = true;
    }

    static void inspect(Minecraft minecraft) {
        if (ready() || !prepared || minecraft.level == null || minecraft.player == null
                || minecraft.gameMode == null || !minecraft.player.hasInfiniteMaterials()
                || !minecraft.player.getInventory().getItem(0).is(MobFarmRegistrationAdapter.ITEM.get())) { return; }
        if (started == 0) { started = System.nanoTime(); }
        require(System.nanoTime() - started < 30_000_000_000L, "Tooltip fixture initialization timed out");
        if (ModList.get().isLoaded("roughlyenoughitems")) {
            var entries = me.shedaniel.rei.api.client.registry.entry.EntryRegistry.getInstance();
            if (entries.isReloading() || entries.size() == 0) { return; }
        }
        if (!opened) {
            if (minecraft.gui.screen() != null) { return; }
            NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, TooltipUiFixture::onGather);
            minecraft.gui.setScreen(new ProbeCreativeScreen(minecraft));
            opened = true;
        }
        if (!checked && minecraft.gui.screen() instanceof ProbeCreativeScreen screen) {
            verifyTooltips(minecraft, screen);
            checked = true;
        }
    }

    private static void verifyTooltips(Minecraft minecraft, ProbeCreativeScreen screen) {
        CreativeModeTabs.tryRebuildTabContents(minecraft.level.enabledFeatures(), true, minecraft.level.registryAccess());
        var tabs = ownedTabTitles();
        boolean originalAdvanced = minecraft.options.advancedItemTooltips;
        int creativeTitles = 0;
        try {
            for (boolean advanced : new boolean[]{false, true}) {
                minecraft.options.advancedItemTooltips = advanced;
                for (var item : BuiltInRegistries.ITEM) {
                    var id = BuiltInRegistries.ITEM.getKey(item);
                    if (!id.getNamespace().equals("trading_cells")) { continue; }
                    require(!net.minecraft.client.resources.language.I18n.get(item.getDescriptionId()).equals(item.getDescriptionId()),
                            "Missing translated item name for " + id);
                    if (!advanced) { ownedItems++; }
                    ItemStack stack = item.getDefaultInstance();
                    verifyOwn(stack, Screen.getTooltipFromItem(minecraft, stack), screen, tabs);
                    var creative = screen.getTooltipFromContainerItem(stack);
                    creativeTitles += (int) creative.stream().filter(tabs::contains).count();
                    verifyOwn(stack, creative, screen, tabs);
                    if (ModList.get().isLoaded("roughlyenoughitems")) {
                        var tooltip = me.shedaniel.rei.api.common.util.EntryStacks.of(stack).getTooltip(
                                me.shedaniel.rei.api.client.gui.widgets.TooltipContext.of(Item.TooltipContext.of(minecraft.level, minecraft.player)), true);
                        require(tooltip != null, "Missing REI tooltip for " + id);
                        var reiLines = tooltip.entries().stream().filter(entry -> entry.isText())
                                .map(entry -> entry.getAsText()).toList();
                        require(reiLines.subList(1, reiLines.size()).stream().filter(line -> line.getString().equals("Trading Cells")).count() == 1,
                                "Duplicate or missing REI mod footer for " + id + ": " + reiLines);
                        verifyOwn(stack, reiLines, screen, tabs);
                    }
                }
            }
            require(ownedItems > 50 && creativeTitles > 20, "Creative tab titles were not exercised");
            minecraft.options.advancedItemTooltips = false;
            ItemStack renamed = MobFarmRegistrationAdapter.ITEM.get().getDefaultInstance();
            renamed.set(DataComponents.CUSTOM_NAME, Component.literal("Trading Cells"));
            renamed.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("Trading Cells"), Component.literal("Keep this lore"))));
            var result = gather(renamed, Screen.getTooltipFromItem(minecraft, renamed), screen);
            require(result.stream().filter(e -> e.left().map(FormattedText::getString).filter("Trading Cells"::equals).isPresent()).count() == 3,
                    "Custom name or lore equal to the mod name was removed");
            require(result.stream().anyMatch(e -> e.left().map(FormattedText::getString).filter("Keep this lore"::equals).isPresent()),
                    "Ordinary lore was removed");
            var vanilla = Items.STONE.getDefaultInstance();
            var vanillaLines = Screen.getTooltipFromItem(minecraft, vanilla);
            var vanillaEvent = new RenderTooltipEvent.GatherComponents(vanilla, screen.width, screen.height, elements(vanillaLines), -1);
            TradingCellsItemTooltips.onGatherTooltip(vanillaEvent);
            require(vanillaEvent.getTooltipElements().equals(elements(vanillaLines)), "Our listener modified a vanilla tooltip");
            require(gather(vanilla, vanillaLines, screen).stream().noneMatch(e -> e.left().map(FormattedText::getString)
                    .filter("Trading Cells"::equals).isPresent()), "Vanilla tooltip received our mod footer");
            var withImage = elements(screen.getTooltipFromContainerItem(renamed));
            var image = Either.<FormattedText, TooltipComponent>right(new TooltipComponent() { });
            withImage.add(1, image);
            var event = new RenderTooltipEvent.GatherComponents(renamed, screen.width, screen.height, withImage, -1);
            NeoForge.EVENT_BUS.post(event);
            require(event.getTooltipElements().contains(image), "Custom tooltip image was removed");
            System.out.println("Tooltip contracts checked: " + ownedItems
                    + " mod items, normal/advanced, creative titles, custom names, lore, images and vanilla isolation; REI="
                    + ModList.get().isLoaded("roughlyenoughitems"));
        } finally {
            minecraft.options.advancedItemTooltips = originalAdvanced;
        }
    }

    private static void verifyOwn(ItemStack stack, List<Component> lines, Screen screen, List<Component> tabs) {
        var result = gather(stack, lines, screen);
        require(!result.isEmpty() && result.getFirst().left().filter(lines.getFirst()::equals).isPresent(), "Item title changed");
        require(result.subList(1, result.size()).stream().filter(e -> e.left().map(FormattedText::getString)
                .filter("Trading Cells"::equals).isPresent()).count() == 1, "Duplicate or missing mod footer for " + stack);
        require(result.stream().noneMatch(e -> e.left().filter(tabs::contains).isPresent()), "Creative tab title survived for " + stack);
        var expectedDetails = elements(lines.subList(1, lines.size()).stream()
                .filter(line -> !tabs.contains(line) && !line.getString().equals("Trading Cells")).toList());
        require(result.containsAll(expectedDetails), "Item details or advanced information removed for " + stack);
    }

    private static List<Either<FormattedText, TooltipComponent>> gather(ItemStack stack, List<Component> lines, Screen screen) {
        var event = new RenderTooltipEvent.GatherComponents(stack, screen.width, screen.height, elements(lines), -1);
        NeoForge.EVENT_BUS.post(event);
        return event.getTooltipElements();
    }

    private static List<Either<FormattedText, TooltipComponent>> elements(List<Component> lines) {
        List<Either<FormattedText, TooltipComponent>> result = new ArrayList<>();
        lines.forEach(line -> result.add(Either.left(line)));
        return result;
    }

    private static List<Component> ownedTabTitles() {
        return BuiltInRegistries.CREATIVE_MODE_TAB.entrySet().stream()
                .filter(entry -> entry.getKey().identifier().getNamespace().equals("trading_cells"))
                .map(entry -> (Component) entry.getValue().getDisplayName().copy().withStyle(ChatFormatting.BLUE)).toList();
    }

    private static void onGather(RenderTooltipEvent.GatherComponents event) {
        if (!checked || rendered || !event.getItemStack().is(MobFarmRegistrationAdapter.ITEM.get())) { return; }
        var lines = event.getTooltipElements().stream().flatMap(e -> e.left().stream()).map(FormattedText::getString).toList();
        require(lines.stream().filter("Trading Cells"::equals).count() == 1, "Rendered tooltip has duplicate footer");
        require(lines.stream().noneMatch(text -> ownedTabTitles().stream().anyMatch(tab -> tab.getString().equals(text))),
                "Rendered tooltip still includes a creative tab title");
        System.out.println("Creative tooltip rendered: " + lines);
        rendered = true;
    }

    private static void require(boolean condition, String message) {
        if (!condition) { throw new IllegalStateException(message); }
    }

    private static final class ProbeCreativeScreen extends CreativeModeInventoryScreen {
        ProbeCreativeScreen(Minecraft minecraft) {
            super(minecraft.player, minecraft.level.enabledFeatures(), true);
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            var slot = getMenu().slots.stream().filter(s -> s.container == minecraft.player.getInventory()
                    && s.getContainerSlot() == 0).findFirst().orElseThrow();
            super.extractRenderState(graphics, leftPos + slot.x + 8, topPos + slot.y + 8, partialTick);
        }
    }
}
