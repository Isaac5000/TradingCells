package com.cosmocraft.trading_cells.platform.neoforge.experience;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.FeatureComposition;
import com.cosmocraft.trading_cells.platform.neoforge.fluid.ExperienceFluidHandler;
import com.cosmocraft.trading_cells.platform.neoforge.registration.ExperienceFluidRegistration;
import java.util.function.IntSupplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/** Hard capacity and external insertion budget are deliberately independent. */
public final class MachineExperienceAccount {
    public static final int CAPACITY = Integer.MAX_VALUE;
    private int amount;
    private boolean fillStorage;
    private final Runnable changed;

    public MachineExperienceAccount(Runnable changed) { this.changed = changed; }
    public int amount() { return amount; }
    public void setRaw(int value) { amount = Math.max(0, value); }
    public boolean fillStorage() { return fillStorage; }
    public void toggleMode() { fillStorage = !fillStorage; changed.run(); }
    public int inputBudget(int recipeCost) {
        return fillStorage ? CAPACITY - amount : Math.max(0, recipeCost - amount);
    }
    public boolean spend(int points) {
        if (points < 0 || points > amount) { return false; }
        amount -= points;
        changed.run();
        return true;
    }
    public ExperienceFluidHandler handler(IntSupplier recipeCost) {
        return new ExperienceFluidHandler(() -> FluidResource.of(ExperienceFluidRegistration.SOURCE.get()),
                this::amount, this::setRaw, () -> CAPACITY, true, true,
                () -> inputBudget(recipeCost.getAsInt()), changed);
    }
    public void load(ValueInput input) {
        setRaw(input.getIntOr("StoredExperience", 0));
        fillStorage = input.getBooleanOr("FillExperienceStorage", false);
    }
    public void save(ValueOutput output) {
        output.putInt("StoredExperience", amount);
        output.putBoolean("FillExperienceStorage", fillStorage);
    }
    public void clear() { amount = 0; fillStorage = false; }
    public void transfer(ServerPlayer player, int action, int levels) {
        if (action < 0 || action > 3) { return; }
        var service = FeatureComposition.arcaneInfusion();
        int points = switch (action) {
            case 0 -> service.depositLevels(player.experienceLevel, player.experienceProgress, amount, CAPACITY, levels);
            case 1 -> service.depositAll(player.experienceLevel, player.experienceProgress, amount, CAPACITY);
            case 2 -> service.withdrawLevels(player.experienceLevel, player.experienceProgress, amount, levels);
            default -> service.withdrawAll(player.experienceLevel, player.experienceProgress, amount);
        };
        if (points <= 0) { return; }
        int moved = action >= 2 ? PlayerExperienceTransfer.addPoints(player, points)
                : PlayerExperienceTransfer.removePoints(player, points);
        amount = (int) Math.clamp((long) amount + (action >= 2 ? -moved : moved), 0, CAPACITY);
        if (moved > 0) { changed.run(); }
    }
}
