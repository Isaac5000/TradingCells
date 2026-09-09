package com.cosmocraft.trading_cells.feature.logistics.adapters.neoforge;

import com.cosmocraft.trading_cells.feature.logistics.domain.model.PipeFilterRule;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;

public final class LogisticsComponentData {
    private LogisticsComponentData() {
    }

    public static String fingerprint(DataComponentPatch patch) {
        String data = serialized(patch);
        if (data.length() <= 2_048) {
            return data;
        }
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return "sha256:" + java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public static String serialized(DataComponentPatch patch) {
        Tag encoded = DataComponentPatch.CODEC.encodeStart(NbtOps.INSTANCE, patch).getOrThrow();
        return encoded.toString();
    }

    public static boolean matches(String actual, String expected, PipeFilterRule.ComponentMatch mode) {
        if (mode == PipeFilterRule.ComponentMatch.IGNORE) {
            return true;
        }
        try {
            CompoundTag value = actual.isEmpty() ? new CompoundTag() : TagParser.parseCompoundFully(actual);
            CompoundTag pattern = expected.isEmpty() ? new CompoundTag() : TagParser.parseCompoundFully(expected);
            return mode == PipeFilterRule.ComponentMatch.EXACT
                    ? value.equals(pattern) : NbtUtils.compareNbt(pattern, value, true);
        } catch (CommandSyntaxException exception) {
            return false;
        }
    }
}
