package com.cosmocraft.trading_cells.platform.neoforge.machine;

import com.cosmocraft.trading_cells.platform.neoforge.bootstrap.TradingCells;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class MachineBlockProperties {
    private MachineBlockProperties() {
    }

    public static BlockBehaviour.Properties villager(String id) {
        return configure(BlockBehaviour.Properties.of(), id, SoundType.METAL);
    }

    public static BlockBehaviour.Properties piglin(String id) {
        return configure(BlockBehaviour.Properties.of(), id, SoundType.STONE);
    }

    public static BlockBehaviour.Properties villagerCopyOf(String id, Block source) {
        return configure(BlockBehaviour.Properties.ofFullCopy(source), id, SoundType.METAL);
    }

    public static BlockBehaviour.Properties piglinCopyOf(String id, Block source) {
        return configure(BlockBehaviour.Properties.ofFullCopy(source), id, SoundType.STONE);
    }

    private static BlockBehaviour.Properties configure(
            BlockBehaviour.Properties properties,
            String id,
            SoundType soundType
    ) {
        return properties
                .setId(ResourceKey.create(
                        Registries.BLOCK,
                        Identifier.fromNamespaceAndPath(TradingCells.MOD_ID, id)
                ))
                .strength(1.5F, 6.0F)
                .sound(soundType)
                .noOcclusion()
                .isRedstoneConductor((state, getter, pos) -> false)
                .isSuffocating((state, getter, pos) -> false)
                .isViewBlocking((state, getter, pos) -> false);
    }
}
