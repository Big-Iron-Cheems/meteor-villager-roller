package maxsuperman.addons.roller.util;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EnchantmentTags;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class EnchantmentUtils {
    private EnchantmentUtils() {
    }

    /**
     * Retrieves a list of enchantments from the registry.
     *
     * @param onlyTradeable if true, returns only tradeable enchantments
     * @return list of enchantment registry entries
     */
    public static @NonNull List<RegistryEntry<Enchantment>> getEnchants(boolean onlyTradeable) {
        if (mc.world == null) return List.of();

        var reg = mc.world.getRegistryManager().getOptional(RegistryKeys.ENCHANTMENT);
        if (reg.isEmpty()) return List.of();

        var source = onlyTradeable
            ? reg.get().iterateEntries(EnchantmentTags.TRADEABLE)
            : reg.get().getIndexedEntries();

        List<RegistryEntry<Enchantment>> result = new ArrayList<>();
        for (var entry : source) {
            result.add(entry);
        }

        return result;
    }

    /**
     * Calculates the minimum emerald price for an enchantment trade.
     * Accounts for double trade price enchantments (treasure enchantments).
     *
     * @param e the enchantment registry entry (must not be null)
     * @return minimum price in emeralds
     * @see <a href="https://minecraft.wiki/w/Enchanted_Book#cite_ref-librarian_enchant_6-0">
     * Minecraft Wiki: Enchanted Book Price Calculation
     * </a>
     */
    public static int getMinimumPrice(@NonNull RegistryEntry<Enchantment> e) {
        return e.isIn(EnchantmentTags.DOUBLE_TRADE_PRICE) ? (2 + 3 * e.value().getMaxLevel()) * 2 : 2 + 3 * e.value().getMaxLevel();
    }

    /**
     * Extracts enchantments from an ItemStack.
     *
     * @param stack the ItemStack to extract enchantments from
     * @return map of enchantment registry entries to their levels
     */
    public static @NonNull Object2IntMap<RegistryEntry<Enchantment>> getEnchantsFromStack(@NonNull ItemStack stack) {
        var entries = stack.getEnchantments().getEnchantmentEntries();

        Object2IntOpenHashMap<RegistryEntry<Enchantment>> result = new Object2IntOpenHashMap<>(entries.size());

        for (var entry : entries) {
            result.put(entry.getKey(), entry.getIntValue());
        }

        return result;
    }
}

