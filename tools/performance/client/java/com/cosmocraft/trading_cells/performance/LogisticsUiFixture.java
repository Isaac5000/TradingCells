package com.cosmocraft.trading_cells.performance;

import com.cosmocraft.trading_cells.feature.logistics.adapters.input.*;
import com.cosmocraft.trading_cells.feature.logistics.adapters.output.LogisticsRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.logistics.domain.model.*;
import java.util.HashSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;

/** Opt-in screenshot fixture, installed only in a benchmark's disposable world copy. */
final class LogisticsUiFixture {
    private static final HashSet<UUID> PREPARED = new HashSet<>();
    private static boolean inspected;
    private static boolean terminalKeyboardChecked;
    private static int phase;
    private static long nextAction;
    private static long checkStarted;
    private static volatile boolean scenePrepared;
    private LogisticsUiFixture() { }

    static void inspect(net.minecraft.client.Minecraft minecraft) {
        if (worldScene()) {
            return;
        }
        if (!(minecraft.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen)) { return; }
        if (!inspected) {
            inspected = true;
            System.out.println("Logistics UI slot count: " + screen.getMenu().slots.size());
        }
        if (screen instanceof com.cosmocraft.trading_cells.feature.logistics.adapters.output.client.PipeConfigurationScreen pipeScreen) {
            if (System.getProperty("trading_cells.performance.client.uiFixture", "").equals("interactions")) {
                PipeInteractionUiFixture.inspect(pipeScreen);
                return;
            }
            if (System.getProperty("trading_cells.performance.client.uiFixture", "").equals("materials")) {
                phase = 2;
                return;
            }
            checkPipeEditor(pipeScreen);
            return;
        }
        if (!(screen instanceof com.cosmocraft.trading_cells.feature.logistics.adapters.output.client.NetworkTerminalScreen terminal)
                || System.nanoTime() < nextAction || phase >= 8) { return; }
        var menu = terminal.getMenu();
        if (checkStarted == 0) { checkStarted = System.nanoTime(); }
        if (System.nanoTime() - checkStarted > 30_000_000_000L) {
            throw new IllegalStateException("Logistics UI action timed out at phase " + phase);
        }
        int left = (screen.width - 374) / 2, top = (screen.height - 238) / 2;
        switch (phase) {
            case 0 -> {
                if (!terminalKeyboardChecked) {
                    var search = screen.children().stream().filter(net.minecraft.client.gui.components.EditBox.class::isInstance)
                            .map(net.minecraft.client.gui.components.EditBox.class::cast).findFirst().orElseThrow();
                    PipeInteractionUiFixture.checkTyping(screen, search);
                    terminalKeyboardChecked = true;
                    System.out.println("Terminal search consumes inventory, drop and hotbar keys while typing");
                }
                int index = java.util.stream.IntStream.range(0, menu.entries().size())
                        .filter(i -> menu.entries().get(i).icon().is(Items.OAK_LOG)).findFirst().orElse(-1);
                if (index < 0) { return; }
                click(screen, left + 198 + index % 8 * 20 + 8, top + 71 + index / 8 * 20 + 8);
            }
            case 1 -> {
                if (!menu.getCarried().is(Items.OAK_LOG)) { return; }
                click(screen, left + 31, top + 84);
            }
            case 2 -> {
                if (!menu.craftingResult().is(Items.OAK_PLANKS)) { return; }
                click(screen, left + 206, top + 79);
            }
            case 3 -> {
                if (!menu.getCarried().isEmpty()) { return; }
                click(screen, left + 143, top + 104);
            }
            case 4 -> {
                if (!menu.getCarried().is(Items.OAK_PLANKS) || menu.getCarried().getCount() != 4) {
                    return;
                }
                click(screen, left + 206, top + 79);
            }
            case 5 -> {
                System.out.println("Logistics UI pickup, ghost recipe, deposit and crafting verified");
                click(screen, left + 320, top + 34);
            }
            case 6 -> {
                terminal.mouseScrolled(left + 220, top + 110, 0, -3);
                for (int index = 0; index < 36; index++) {
                    var slot = menu.slots.get(index);
                    if (!slot.isActive() || slot.x < 12 || slot.x > 156 || slot.y < 154 || slot.y > 212) {
                        throw new IllegalStateException("Terminal player slot moved outside its inventory: " + index);
                    }
                }
            }
            case 7 -> {
                if (menu.page() != 1 || menu.entries().size() < 80) { return; }
                System.out.println("Logistics smooth-scroll window covers both partial boundary rows");
            }
            default -> { }
        }
        phase++;
        nextAction = System.nanoTime() + 700_000_000L;
    }

    static boolean readyForCapture() {
        if (worldScene()) { return scenePrepared; }
        if (System.getProperty("trading_cells.performance.client.uiFixture", "").equals("interactions")) { return PipeInteractionUiFixture.ready(); }
        if (System.getProperty("trading_cells.performance.client.uiFixture", "").equals("rules")) { return phase >= 8; }
        return System.getProperty("trading_cells.performance.client.uiFixture", "").equals("crafting") ? phase >= 8 : phase >= 2;
    }

    static boolean worldScene() {
        String fixture = System.getProperty("trading_cells.performance.client.uiFixture", "");
        return fixture.equals("connections") || fixture.equals("caps");
    }

    private static void checkPipeEditor(com.cosmocraft.trading_cells.feature.logistics.adapters.output.client.PipeConfigurationScreen screen) {
        boolean advanced = System.getProperty("trading_cells.performance.client.uiFixture", "").equals("rules");
        if (phase >= (advanced ? 8 : 2) || System.nanoTime() < nextAction) { return; }
        int left = (screen.width - 374) / 2, top = (screen.height - 248) / 2;
        if (advanced && phase >= 2) {
            var menu = screen.getMenu();
            if (phase >= 6) {
                var field = screen.children().stream().filter(net.minecraft.client.gui.components.EditBox.class::isInstance)
                        .map(net.minecraft.client.gui.components.EditBox.class::cast).filter(box -> box.getY() == top + 112).findFirst().orElseThrow();
                if (phase == 6) {
                    click(screen, left + 25, top + 120); field.setValue("me"); phase++;
                } else {
                    var suggestions = menu.channelSuggestions();
                    if (suggestions == null || !suggestions.complete() || !suggestions.prefix().equals("almacen/me")) { return; }
                    if (!suggestions.channels().contains("almacen/metales")) { throw new IllegalStateException("Channel completion missed saved rule"); }
                    click(screen, left + 35, top + 136);
                    if (!field.getValue().equals("metales")) { throw new IllegalStateException("Completion duplicated the parent channel"); }
                    System.out.println("Bounded server channel completion and subchannel prefix verified"); phase++;
                }
                nextAction = System.nanoTime() + 700_000_000L;
                return;
            }
            if (phase == 2 || phase == 5) {
                if (phase == 5) {
                    var rule = menu.initial().profile(LogisticsResourceType.ITEM).filters().getFirst();
                    if (!rule.inverted() || rule.target() == null || !rule.routeChannel().equals("almacen/metales")
                            || rule.componentMatch() != PipeFilterRule.ComponentMatch.SUBSET) {
                        throw new IllegalStateException("Advanced pipe rule did not persist");
                    }
                    System.out.println("Advanced pipe rule, marker ghost slot, NBT, inversion and subchannel verified");
                }
                click(screen, left + 90, top + 60);
                click(screen, left + 278, top + 133);
                phase++;
            } else if (phase == 3) {
                screen.children().stream().filter(net.minecraft.client.gui.components.EditBox.class::isInstance)
                        .map(net.minecraft.client.gui.components.EditBox.class::cast).forEach(box -> {
                            if (box.getY() == top + 74) { box.setValue("{\"minecraft:custom_data\":{grade:1}}"); }
                            if (box.getY() == top + 112) { box.setValue("metales"); }
                        });
                click(screen, left + 326, top + 82);
                click(screen, left + 280, top + 216);
                click(screen, left + 128, top + 168);
                if (!(menu.getCarried().getItem() instanceof PipeTargetSelectorItem)) { throw new IllegalStateException("Marker pickup failed"); }
                click(screen, left + 212, top + 172);
                if (!(menu.getCarried().getItem() instanceof PipeTargetSelectorItem)) { throw new IllegalStateException("Ghost destination consumed marker"); }
                click(screen, left + 128, top + 168);
                click(screen, left + 320, top + 238);
                phase++;
            } else {
                screen.onClose(); phase++;
            }
            nextAction = System.nanoTime() + 700_000_000L;
            return;
        }
        if (phase == 0) {
            screen.children().stream().filter(child -> child instanceof net.minecraft.client.gui.components.EditBox)
                    .map(child -> (net.minecraft.client.gui.components.EditBox) child).forEach(box -> {
                        if (box.getY() == top + 28) { box.setValue("100000"); }
                        if (box.getY() == top + 185) { box.setValue("almacen"); }
                    });
            click(screen, left + 25, top + 118);
            click(screen, left + 223, top + 133);
            screen.children().stream().filter(child -> child instanceof net.minecraft.client.gui.components.EditBox)
                    .map(child -> (net.minecraft.client.gui.components.EditBox) child)
                    .filter(box -> box.getY() == top + 36).forEach(box -> box.setValue("minecraft:iron_ingot"));
            click(screen, left + 320, top + 238);
            screen.onClose();
            phase = 1;
        } else {
            var settings = screen.getMenu().initial();
            var profile = settings.profile(LogisticsResourceType.ITEM);
            if (settings.priority() != 100000 || !profile.channel().equals("almacen")
                    || profile.filterMode() != PipeResourceProfile.FilterMode.WHITELIST || profile.filters().size() != 1) {
                throw new IllegalStateException("Pipe editor did not persist edits on close");
            }
            System.out.println("Logistics pipe editor autosave and reopen verified");
            phase = 2;
        }
    }

    private static void click(net.minecraft.client.gui.screens.Screen screen, int x, int y) {
        var event = new net.minecraft.client.input.MouseButtonEvent(x, y, new net.minecraft.client.input.MouseButtonInfo(0, 0));
        screen.mouseClicked(event, false);
        screen.mouseReleased(event);
    }

    static void prepare(ServerPlayer player, BlockPos pos) {
        String fixture = System.getProperty("trading_cells.performance.client.uiFixture", "");
        if (fixture.isEmpty() || !PREPARED.add(player.getUUID())) { return; }
        var level = player.level();
        if (fixture.equals("caps")) {
            for (BlockPos target : BlockPos.betweenClosed(pos.offset(-2, -1, -3), pos.offset(2, 2, 2))) {
                level.setBlockAndUpdate(target, target.getY() == pos.getY() - 1 ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState());
            }
            level.setBlockAndUpdate(pos, com.cosmocraft.trading_cells.feature.experience.adapters.output.ExperienceStorageRegistrationAdapter.BLOCK.get().defaultBlockState());
            level.setBlockAndUpdate(pos.north(), LogisticsRegistrationAdapter.pipeBlock(PipeKind.UNIVERSAL).get().defaultBlockState());
            var pipe = (LogisticsPipeBlockEntity) level.getBlockEntity(pos.north());
            pipe.setMode(Direction.SOUTH, PipeSideMode.EXTRACT);
            pipe.refreshConnectionsAround();
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            scenePrepared = true;
            return;
        }
        if (worldScene()) {
            for (BlockPos target : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(28, 5, 7))) {
                level.setBlockAndUpdate(target, target.getY() == pos.getY() - 1
                        ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState());
            }
            int index = 0;
            for (PipeKind kind : PipeKind.values()) {
                BlockPos start = pos.east(index++ * 6);
                var block = LogisticsRegistrationAdapter.pipeBlock(kind).get();
                for (int x = 0; x <= 4; x++) {
                    for (int z = 0; z <= 4; z++) {
                        if (x % 2 == 0 || z % 2 == 0) {
                            level.setBlockAndUpdate(start.offset(x, 0, z), block.defaultBlockState());
                        }
                    }
                }
                // A vertical elbow and a three-axis joint exercise the non-horizontal UVs.
                level.setBlockAndUpdate(start.offset(2, 1, 2), block.defaultBlockState());
                level.setBlockAndUpdate(start.offset(2, 2, 2), block.defaultBlockState());
                level.setBlockAndUpdate(start.offset(2, 2, 3), block.defaultBlockState());
                level.setBlockAndUpdate(start.offset(2, 2, 4), block.defaultBlockState());
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            scenePrepared = true;
            return;
        }
        level.setBlockAndUpdate(pos, (!fixture.equals("crafting")
                ? LogisticsRegistrationAdapter.pipeBlock(PipeKind.UNIVERSAL).get()
                : LogisticsRegistrationAdapter.CRAFTING_TERMINAL_BLOCK.get()).defaultBlockState());
        level.setBlockAndUpdate(pos.west(), LogisticsRegistrationAdapter.pipeBlock(PipeKind.UNIVERSAL).get().defaultBlockState());
        level.setBlockAndUpdate(pos.west(2), Blocks.BARREL.defaultBlockState());
        var barrel = (BarrelBlockEntity) level.getBlockEntity(pos.west(2));
        var items = java.util.List.of(Items.OAK_LOG, Items.IRON_INGOT, Items.COPPER_INGOT, Items.GOLD_INGOT,
                Items.DIAMOND, Items.REDSTONE, Items.BUCKET, Items.CHEST, Items.CRAFTING_TABLE,
                Items.AMETHYST_BLOCK, Items.QUARTZ, Items.WIND_CHARGE, Items.GLASS_PANE);
        for (int index = 0; index < items.size(); index++) { barrel.setItem(index, new ItemStack(items.get(index), 64)); }
        player.setItemInHand(InteractionHand.MAIN_HAND, LogisticsRegistrationAdapter.WRENCH_ITEM.get().getDefaultInstance());
        for (int index = 0; index < 5; index++) {
            player.getInventory().setItem(9 + index, LogisticsRegistrationAdapter.upgradeItem(PipeUpgradeTier.values()[index + 1]).get().getDefaultInstance());
        }
        if (fixture.equals("rules")) {
            var marker = LogisticsRegistrationAdapter.TARGET_SELECTOR_ITEM.get().getDefaultInstance();
            BlockPos target = pos.west(2);
            PipeTargetSelectorItem.setTarget(marker, new PipeRuleTarget(level.dimension().identifier().toString(), target.getX(), target.getY(), target.getZ()));
            player.getInventory().setItem(15, marker);
        }
        if (fixture.equals("materials")) {
            int index = 0;
            for (PipeKind kind : PipeKind.values()) {
                player.getInventory().setItem(9 + index, LogisticsRegistrationAdapter.pipeBlock(kind).get().asItem().getDefaultInstance());
                player.getInventory().setItem(18 + index, LogisticsRegistrationAdapter.upgradeItem(PipeUpgradeTier.values()[index + 1]).get().getDefaultInstance());
                index++;
            }
        }
        if (level.getBlockEntity(pos) instanceof LogisticsPipeBlockEntity pipe) {
            pipe.setMode(Direction.SOUTH, PipeSideMode.EXTRACT);
            pipe.upgrades(Direction.SOUTH).setItem(0, LogisticsRegistrationAdapter.upgradeItem(PipeUpgradeTier.ULTIMATE).get().getDefaultInstance());
            if (fixture.equals("interactions")) {
                pipe.upgrades(Direction.SOUTH).setItem(0, ItemStack.EMPTY);
                player.getInventory().setItem(9, new ItemStack(LogisticsRegistrationAdapter.upgradeItem(PipeUpgradeTier.INFINITE).get(), 64));
                player.getInventory().setItem(12, ItemStack.EMPTY);
                player.getInventory().setItem(13, ItemStack.EMPTY);
            }
        }
    }

    static boolean openPipe(ServerPlayer player, net.minecraft.world.level.block.entity.BlockEntity entity) {
        if (!(entity instanceof LogisticsPipeBlockEntity pipe)) { return false; }
        var face = Direction.SOUTH;
        var hand = InteractionHand.MAIN_HAND;
        player.setItemInHand(hand, LogisticsRegistrationAdapter.WRENCH_ITEM.get().getDefaultInstance());
        player.openMenu(new SimpleMenuProvider((id, inventory, owner) -> new PipeConfigurationMenu(id, inventory, pipe, face, hand),
                Component.translatable("gui.trading_cells.pipe.title")), buffer -> {
            buffer.writeBlockPos(pipe.getBlockPos());
            buffer.writeEnum(face);
            buffer.writeEnum(hand);
            buffer.writeEnum(pipe.kind());
            buffer.writeNbt(pipe.face(face).save());
            buffer.writeVarLong(pipe.revision());
        });
        return true;
    }
}
