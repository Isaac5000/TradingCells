package com.cosmocraft.trading_cells.feature.mobfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.mobfarm.domain.model.EssenceClassification;
import java.util.List;
import java.util.function.ToDoubleFunction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.RangedAttackMob;

/** Instance-specific rules remain separate from attribute sampling and balance math. */
public final class EssenceThreatModifiers {
    private static final List<ToDoubleFunction<LivingEntity>> RULES = List.of(
            entity -> entity.getType().getCategory() == MobCategory.MONSTER ? EssenceClassification.HOSTILE : 0,
            entity -> entity instanceof RangedAttackMob ? EssenceClassification.RANGED : 0,
            entity -> entity instanceof Creeper creeper && creeper.isPowered() ? EssenceClassification.CHARGED : 0);

    private EssenceThreatModifiers() { }
    public static double score(LivingEntity entity) {
        return RULES.stream().mapToDouble(rule -> rule.applyAsDouble(entity)).sum();
    }
}
