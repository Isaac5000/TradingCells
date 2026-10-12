package com.cosmocraft.trading_cells.feature.mobfarm.adapters.input;

import com.cosmocraft.trading_cells.feature.mobfarm.adapters.output.MobFarmRegistrationAdapter;
import com.cosmocraft.trading_cells.feature.mobfarm.domain.model.EssenceTier;
import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.Config;
import java.util.Locale;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.component.CustomData;
import org.jspecify.annotations.Nullable;

public final class CreatureGeneratorEggs {
    public static final TagKey<Item> INGREDIENTS = TagKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath("trading_cells", "arcane_infusion_eggs"));
    private CreatureGeneratorEggs() { }

    public static boolean isIngredient(ItemStack stack) { return stack.is(INGREDIENTS); }

    /** Called only by the authoritative machine, never to change essence classification. */
    public static boolean allowed(@Nullable Identifier id) {
        return id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id) && Config.CREATURE_GENERATOR_BLACKLIST.get().stream()
                .map(value -> Identifier.tryParse(value.trim().toLowerCase(Locale.ROOT))).noneMatch(id::equals);
    }

    public static ItemStack create(ItemStack core) {
        Identifier id = EntityEssenceData.entityTypeId(core);
        if (!allowed(id)) { return ItemStack.EMPTY; }
        var type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
        var egg = BuiltInRegistries.ITEM.componentLookup().findMatching(DataComponents.ENTITY_DATA, data -> data.type() == type)
                .filter(holder -> holder.value() instanceof SpawnEggItem).findFirst();
        if (egg.isPresent()) { return new ItemStack(egg.get()); }
        ItemStack fallback = preview(EntityEssenceData.tier(core));
        // Only the registry identity crosses this boundary, never equipment, passengers or arbitrary entity NBT.
        fallback.set(DataComponents.ENTITY_DATA, TypedEntityData.of(type, new CompoundTag()));
        return fallback;
    }

    public static ItemStack preview(EssenceTier tier) {
        ItemStack egg = new ItemStack(MobFarmRegistrationAdapter.GENERIC_SPAWN_EGG.get());
        CompoundTag root = new CompoundTag(), essence = new CompoundTag();
        essence.putInt("Tier", tier.id());
        root.put("TradingCellsEssence", essence);
        egg.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        return egg;
    }
}
