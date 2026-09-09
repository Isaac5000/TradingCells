package com.cosmocraft.trading_cells.feature.logistics.adapters.input;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = TradingCells.MOD_ID)
public final class LogisticsPipeInteractionAdapter {
    private LogisticsPipeInteractionAdapter() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()
                || !(event.getItemStack().getItem() instanceof PipeWrenchItem)
                || !(event.getLevel().getBlockEntity(event.getPos()) instanceof LogisticsPipeBlockEntity pipe)) {
            return;
        }
        if (!event.getEntity().mayUseItemAt(event.getPos(), event.getFace(), event.getItemStack())
                || !event.getLevel().mayInteract(event.getEntity(), event.getPos())) {
            event.setCanceled(true);
            return;
        }
        if (event.getEntity().isShiftKeyDown()) {
            pipe.cycleSide(PipeInteractionFace.resolve(event.getHitVec()));
        } else {
            var face = PipeInteractionFace.resolve(event.getHitVec());
            var hand = event.getHand();
            event.getEntity().openMenu(new net.minecraft.world.SimpleMenuProvider(
                    (id, inventory, player) -> new PipeConfigurationMenu(id, inventory, pipe, face, hand),
                    net.minecraft.network.chat.Component.translatable("gui.trading_cells.pipe.title")), buffer -> {
                buffer.writeBlockPos(pipe.getBlockPos());
                buffer.writeEnum(face);
                buffer.writeEnum(hand);
                buffer.writeEnum(pipe.kind());
                buffer.writeNbt(pipe.face(face).save());
                buffer.writeVarLong(pipe.revision());
            });
        }
        event.setCancellationResult(event.getLevel().isClientSide()
                ? InteractionResult.SUCCESS
                : InteractionResult.SUCCESS_SERVER);
        event.setCanceled(true);
    }
}
