package maxsuperman.addons.roller.model;

import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.NonNull;

/**
 * Represents a configurable enchantment for villager rolling.
 */
public class RollingEnchantment implements ISerializable<RollingEnchantment> {
    /**
     * The enchantment identifier (e.g., "minecraft:protection")
     */
    public Identifier enchantment;
    /**
     * Minimum enchantment level required (0 = max level only)
     */
    public int minLevel;
    /**
     * Maximum cost in emeralds (0 = no limit)
     */
    public int maxCost;
    /**
     * Whether this enchantment is actively being searched for
     */
    public boolean enabled;

    public RollingEnchantment(@NonNull Identifier enchantment, int minLevel, int maxCost, boolean enabled) {
        this.enchantment = enchantment;
        this.minLevel = minLevel;
        this.maxCost = maxCost;
        this.enabled = enabled;
    }

    public RollingEnchantment() {
        enchantment = Identifier.of("minecraft", "protection");
        minLevel = 0;
        maxCost = 0;
        enabled = false;
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        tag.putString("enchantment", enchantment.toString());
        tag.putInt("minLevel", minLevel);
        tag.putInt("maxCost", maxCost);
        tag.putBoolean("enabled", enabled);
        return tag;
    }

    @Override
    public RollingEnchantment fromTag(@NonNull NbtCompound tag) {
        enchantment = Identifier.tryParse(tag.getString("enchantment", ""));
        minLevel = tag.getInt("minLevel", 1);
        maxCost = tag.getInt("maxCost", 64);
        enabled = tag.getBoolean("enabled", true);
        return this;
    }
}

