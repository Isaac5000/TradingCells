package com.cosmocraft.trading_cells.feature.infusion.adapters.minecraft;

import com.cosmocraft.trading_cells.feature.captures.adapters.api.CapturedMobStackAdapter;
import com.cosmocraft.trading_cells.feature.captures.domain.model.CapturedMobKind;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

/** Data-driven result operation for an Arcane Infusion recipe. */
public record ArcaneInfusionResult(
        Kind type,
        Optional<Holder<Enchantment>> enchantment,
        Optional<Holder<Item>> item,
        int level
) {
    private static final Identifier NONE_PROFESSION = Identifier.withDefaultNamespace("none");
    private static final Identifier NITWIT_PROFESSION = Identifier.withDefaultNamespace("nitwit");

    public static final Codec<ArcaneInfusionResult> CODEC = RecordCodecBuilder.<ArcaneInfusionResult>create(instance ->
            instance.group(
                    Kind.CODEC.fieldOf("type").forGetter(ArcaneInfusionResult::type),
                    Enchantment.CODEC.optionalFieldOf("enchantment")
                            .forGetter(ArcaneInfusionResult::enchantment),
                    Item.CODEC.optionalFieldOf("item").forGetter(ArcaneInfusionResult::item),
                    Codec.intRange(1, Enchantment.MAX_LEVEL).optionalFieldOf("level", 1)
                            .forGetter(ArcaneInfusionResult::level)
            ).apply(instance, ArcaneInfusionResult::new)
    ).validate(ArcaneInfusionResult::validate);

    public static final StreamCodec<RegistryFriendlyByteBuf, ArcaneInfusionResult> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, result) -> {
                        buffer.writeByte(result.type().ordinal());
                        buffer.writeBoolean(result.enchantment().isPresent());
                        result.enchantment().ifPresent(value -> Enchantment.STREAM_CODEC.encode(buffer, value));
                        buffer.writeBoolean(result.item().isPresent());
                        result.item().ifPresent(value -> Item.STREAM_CODEC.encode(buffer, value));
                        buffer.writeVarInt(result.level());
                    },
                    buffer -> {
                        Kind type = Kind.fromOrdinal(buffer.readUnsignedByte());
                        Optional<Holder<Enchantment>> enchantment = buffer.readBoolean()
                                ? Optional.of(Enchantment.STREAM_CODEC.decode(buffer))
                                : Optional.empty();
                        Optional<Holder<Item>> item = buffer.readBoolean()
                                ? Optional.of(Item.STREAM_CODEC.decode(buffer))
                                : Optional.empty();
                        return new ArcaneInfusionResult(type, enchantment, item, buffer.readVarInt());
                    }
            );

    public boolean matchesInput(ArcaneInfusionInput input) {
        return switch (type) {
            case ENCHANTED_BOOK -> ArcaneInfusionRecipe.isPlainBook(input.getItem(4));
            case NITWIT_VILLAGER -> CapturedMobStackAdapter.hasVillagerProfession(
                    input.getItem(4),
                    NONE_PROFESSION
            );
            case ITEM -> true;
        };
    }

    public ItemStack assemble(ArcaneInfusionInput input) {
        return switch (type) {
            case ENCHANTED_BOOK -> EnchantmentHelper.createBook(new EnchantmentInstance(
                    enchantment.orElseThrow(),
                    level
            ));
            case NITWIT_VILLAGER -> CapturedMobStackAdapter.withVillagerProfession(
                    input.getItem(4),
                    NITWIT_PROFESSION
            );
            case ITEM -> new ItemStack(item.orElseThrow());
        };
    }

    public Optional<ItemStack> displayInputOverride(int slot) {
        return type == Kind.NITWIT_VILLAGER && slot == 4
                ? Optional.of(displayVillagerCapturer(NONE_PROFESSION))
                : Optional.empty();
    }

    public ItemStack displayResult() {
        if (type == Kind.ENCHANTED_BOOK) {
            return EnchantmentHelper.createBook(new EnchantmentInstance(enchantment.orElseThrow(), level));
        }
        if (type == Kind.ITEM) {
            return new ItemStack(item.orElseThrow());
        }

        return displayVillagerCapturer(NITWIT_PROFESSION);
    }

    private static ItemStack displayVillagerCapturer(Identifier profession) {
        ItemStack capturer = new ItemStack(CapturedMobStackAdapter.capturerItem(CapturedMobKind.VILLAGER));
        CompoundTag entityData = new CompoundTag();
        CompoundTag villagerData = new CompoundTag();
        villagerData.putString("type", "minecraft:plains");
        villagerData.putString("profession", profession.toString());
        villagerData.putInt("level", 1);
        entityData.put("VillagerData", villagerData);
        entityData.putInt("Age", 0);
        CapturedMobStackAdapter.setData(CapturedMobKind.VILLAGER, capturer, entityData);
        return capturer;
    }

    public boolean usesHappyVillagerParticles() {
        return type == Kind.NITWIT_VILLAGER || enchantment.flatMap(Holder::unwrapKey)
                .map(key -> "farmers_touch".equals(key.identifier().getPath()))
                .orElse(false);
    }

    private static DataResult<ArcaneInfusionResult> validate(ArcaneInfusionResult result) {
        boolean valid = switch (result.type()) {
            case ENCHANTED_BOOK -> result.enchantment().isPresent() && result.item().isEmpty();
            case NITWIT_VILLAGER -> result.enchantment().isEmpty() && result.item().isEmpty();
            case ITEM -> result.enchantment().isEmpty() && result.item().isPresent();
        };
        return valid
                ? DataResult.success(result)
                : DataResult.error(() -> "Arcane infusion result fields do not match type " + result.type());
    }

    public enum Kind {
        ENCHANTED_BOOK,
        NITWIT_VILLAGER,
        ITEM;

        private static final Codec<Kind> CODEC = Codec.STRING.comapFlatMap(
                name -> {
                    try {
                        return DataResult.success(valueOf(name.toUpperCase(Locale.ROOT)));
                    } catch (IllegalArgumentException exception) {
                        return DataResult.error(() -> "Unknown arcane infusion result type: " + name);
                    }
                },
                value -> value.name().toLowerCase(Locale.ROOT)
        );

        private static Kind fromOrdinal(int ordinal) {
            Kind[] values = values();
            if (ordinal < 0 || ordinal >= values.length) {
                throw new IllegalArgumentException("Unknown arcane infusion result ordinal: " + ordinal);
            }
            return values[ordinal];
        }
    }
}
