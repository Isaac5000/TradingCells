package com.cosmocraft.trading_cells.platform.neoforge.integration.jade;

import com.cosmocraft.trading_cells.platform.neoforge.item.EntityItemPreviewData;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import snownee.jade.addon.universal.ItemStorageProvider;
import snownee.jade.api.Accessor;
import snownee.jade.api.view.ViewGroup;

/** Decorates Jade's fallback, retaining its locks, loot-table checks and stack grouping. */
final class EntityItemStorageProvider extends ItemStorageProvider.Extension {
    static final EntityItemStorageProvider INSTANCE = new EntityItemStorageProvider();
    private static final Identifier ID = Identifier.fromNamespaceAndPath("trading_cells", "entity_item_preview");

    @Override public Identifier getUid() { return ID; }
    @Override public int getDefaultPriority() { return 9998; }

    @Override public @Nullable List<ViewGroup<ItemStack>> getGroups(Accessor<?> accessor) {
        var groups = super.getGroups(accessor);
        if (groups == null) { return null; }
        boolean changed = false;
        List<ViewGroup<ItemStack>> result = new ArrayList<>(groups.size());
        for (var group : groups) {
            List<ItemStack> views = new ArrayList<>(group.views.size());
            for (var stack : group.views) {
                var preview = EntityItemPreviewData.compact(stack);
                changed |= preview != stack;
                views.add(preview);
            }
            result.add(new ViewGroup<>(views, Optional.ofNullable(group.id), Optional.of(group.getExtraData().copy())));
        }
        return changed ? result : null;
    }
}
