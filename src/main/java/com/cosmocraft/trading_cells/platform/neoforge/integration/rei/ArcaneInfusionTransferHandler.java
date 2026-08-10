package com.cosmocraft.trading_cells.platform.neoforge.integration.rei;

import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.infusion.adapters.input.ArcaneInfuserBlockEntity;
import com.cosmocraft.trading_cells.feature.infusion.adapters.input.ArcaneInfuserMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandler;
import me.shedaniel.rei.api.client.registry.transfer.simple.SimpleTransferHandler;
import me.shedaniel.rei.api.common.entry.InputIngredient;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.api.common.transfer.info.stack.SlotAccessor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Moves Arcane Infusion ingredients without treating XP as a transferable input. */
final class ArcaneInfusionTransferHandler implements SimpleTransferHandler {
    private static final int CENTER_SLOT = 4;
    private static final Identifier NONE_PROFESSION = Identifier.withDefaultNamespace("none");

    @Override
    public ApplicabilityResult checkApplicable(TransferHandler.Context context) {
        return context.getMenu() instanceof ArcaneInfuserMenu
                && context.getDisplay() instanceof ArcaneInfusionReiDisplay
                && context.getContainerScreen() != null
                ? ApplicabilityResult.createApplicable()
                : ApplicabilityResult.createNotApplicable();
    }

    @Override
    public Iterable<SlotAccessor> getInputSlots(TransferHandler.Context context) {
        return IntStream.range(0, ArcaneInfuserBlockEntity.INPUT_SLOT_COUNT)
                .mapToObj(slot -> SlotAccessor.fromSlot(context.getMenu().getSlot(slot)))
                .toList();
    }

    @Override
    public Iterable<SlotAccessor> getInventorySlots(TransferHandler.Context context) {
        LocalPlayer player = context.getMinecraft().player;
        Inventory inventory = player.getInventory();
        return IntStream.range(0, inventory.getNonEquipmentItems().size())
                .mapToObj(slot -> SlotAccessor.fromPlayerInventory(player, slot))
                .toList();
    }

    @Override
    public List<InputIngredient<ItemStack>> getInputsIndexed(TransferHandler.Context context) {
        List<InputIngredient<ItemStack>> inputs = new ArrayList<>(
                SimpleTransferHandler.super.getInputsIndexed(context)
        );
        if (!(context.getDisplay() instanceof ArcaneInfusionReiDisplay display)
                || !requiresUnemployedVillager(display)) {
            return inputs;
        }

        List<ItemStack> candidates = unemployedVillagerCapturers(context);
        for (int index = 0; index < inputs.size(); index++) {
            InputIngredient<ItemStack> input = inputs.get(index);
            if (input.getIndex() == CENTER_SLOT) {
                inputs.set(index, InputIngredient.of(
                        input.getIndex(),
                        input.getDisplayIndex(),
                        candidates
                ));
                break;
            }
        }
        return inputs;
    }

    private static boolean requiresUnemployedVillager(ArcaneInfusionReiDisplay display) {
        return display.getInputEntries().get(CENTER_SLOT).stream()
                .filter(entry -> entry.getType() == VanillaEntryTypes.ITEM)
                .map(entry -> (ItemStack) entry.getValue())
                .anyMatch(stack -> CapturedMobStackAdapter.hasVillagerProfession(stack, NONE_PROFESSION));
    }

    private List<ItemStack> unemployedVillagerCapturers(TransferHandler.Context context) {
        List<ItemStack> candidates = new ArrayList<>();
        for (SlotAccessor slot : getInventorySlots(context)) {
            ItemStack stack = slot.getItemStack();
            if (CapturedMobStackAdapter.hasVillagerProfession(stack, NONE_PROFESSION)
                    && candidates.stream().noneMatch(existing -> ItemStack.isSameItemSameComponents(existing, stack))) {
                candidates.add(stack.copyWithCount(1));
            }
        }
        return List.copyOf(candidates);
    }
}
