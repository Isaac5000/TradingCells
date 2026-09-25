package com.cosmocraft.trading_cells.feature.mobfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.mixin.MobExperienceAccessor;
import com.cosmocraft.trading_cells.feature.mobfarm.domain.model.EssenceClassification;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class EssenceClassifier {
    private EssenceClassifier() { }

    public static EssenceClassification.Result classify(LivingEntity entity, boolean legacyElite) {
        int override = 0;
        for (int tier = 4; tier >= 1; tier--) {
            if (entity.typeHolder().is(tag("essence_tier_" + tier))) { override = tier; break; }
        }
        var attributes = new EssenceClassification.Attributes(attribute(entity, Attributes.MAX_HEALTH),
                attribute(entity, Attributes.ARMOR), attribute(entity, Attributes.ARMOR_TOUGHNESS),
                attribute(entity, Attributes.ATTACK_DAMAGE), attribute(entity, Attributes.KNOCKBACK_RESISTANCE),
                entity instanceof MobExperienceAccessor mob ? mob.tradingCells$baseExperience() : 0);
        return EssenceClassification.classify(attributes, EssenceThreatModifiers.score(entity), override,
                legacyElite || entity.typeHolder().is(tag("essence_elite")), entity.typeHolder().is(tag("essence_boss")));
    }

    private static double attribute(LivingEntity entity, Holder<Attribute> attribute) {
        var instance = entity.getAttribute(attribute);
        return instance == null ? 0 : instance.getValue();
    }

    private static TagKey<EntityType<?>> tag(String name) {
        return TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("trading_cells", name));
    }
}
