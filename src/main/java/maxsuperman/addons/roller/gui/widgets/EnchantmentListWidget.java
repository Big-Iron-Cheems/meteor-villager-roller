package maxsuperman.addons.roller.gui.widgets;

import maxsuperman.addons.roller.gui.screens.EnchantmentSelectScreen;
import maxsuperman.addons.roller.model.RollingEnchantment;
import maxsuperman.addons.roller.util.ConfigManager;
import maxsuperman.addons.roller.util.EnchantmentUtils;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.input.WDropdown;
import meteordevelopment.meteorclient.gui.widgets.input.WIntEdit;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.utils.misc.Names;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import org.apache.commons.io.FilenameUtils;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class EnchantmentListWidget {
    private EnchantmentListWidget() {
    }

    public static void fillWidget(@NonNull GuiTheme theme, @NonNull WVerticalList list, @NonNull List<RollingEnchantment> searchingEnchants,
                                  boolean sortEnchantments, boolean onlyTradeable,
                                  Consumer<String> infoLogger, Consumer<String> errorLogger) {
        WSection loadDataSection = list.add(theme.section("Config Saving")).expandX().widget();

        WTable control = loadDataSection.add(theme.table()).expandX().widget();

        WTextBox savedConfigName = control.add(theme.textBox("default")).expandWidgetX().expandCellX().expandX().widget();
        WButton save = control.add(theme.button("Save")).expandX().widget();
        save.action = () -> {
            Path savePath = ConfigManager.CONFIG_PATH.resolve(savedConfigName.get() + ".nbt");
            if (ConfigManager.saveSearchingToFile(savePath, searchingEnchants, errorLogger)) {
                infoLogger.accept("Saved successfully");
            } else {
                errorLogger.accept("Save failed");
            }
            list.clear();
            fillWidget(theme, list, searchingEnchants, sortEnchantments, onlyTradeable, infoLogger, errorLogger);
        };
        control.row();

        ArrayList<String> configs = new ArrayList<>();
        if (Files.notExists(ConfigManager.CONFIG_PATH)) {
            try {
                Files.createDirectories(ConfigManager.CONFIG_PATH);
            } catch (IOException e) {
                errorLogger.accept("Failed to create directory [" + ConfigManager.CONFIG_PATH + "]: " + e.getMessage());
            }
        } else {
            try (DirectoryStream<Path> configDir = Files.newDirectoryStream(ConfigManager.CONFIG_PATH)) {
                for (Path config : configDir) {
                    configs.add(FilenameUtils.removeExtension(config.getFileName().toString()));
                }
            } catch (IOException e) {
                errorLogger.accept("Failed to list directory: " + e.getMessage());
            }
        }
        if (!configs.isEmpty()) {
            WDropdown<String> loadedConfigName = control.add(theme.dropdown(configs.toArray(new String[0]), "default")).expandWidgetX().expandCellX().expandX().widget();
            WButton load = control.add(theme.button("Load")).expandX().widget();
            load.action = () -> {
                Path loadPath = ConfigManager.CONFIG_PATH.resolve(loadedConfigName.get() + ".nbt");
                if (ConfigManager.loadSearchingFromFile(loadPath, searchingEnchants, errorLogger)) {
                    list.clear();
                    fillWidget(theme, list, searchingEnchants, sortEnchantments, onlyTradeable, infoLogger, errorLogger);
                    infoLogger.accept("Loaded successfully");
                } else {
                    errorLogger.accept("Failed to load file.");
                }
            };
        }

        WSection enchantments = list.add(theme.section("Enchantments")).expandX().widget();

        WTable table = enchantments.add(theme.table()).expandX().widget();
        table.add(theme.item(Items.BOOK.getDefaultStack()));
        table.add(theme.label("Enchantment"));
        table.add(theme.label("Level"));
        table.add(theme.label("Cost"));
        table.add(theme.label("Enabled"));
        table.add(theme.label("Remove"));
        table.row();
        if (sortEnchantments) {
            searchingEnchants.removeIf(ench -> ench.enchantment == null);
            searchingEnchants.sort(Comparator.comparing(o -> o.enchantment));
        }

        Optional<Registry<Enchantment>> reg;
        if (mc.world != null) {
            reg = mc.world.getRegistryManager().getOptional(RegistryKeys.ENCHANTMENT);
        } else {
            reg = Optional.empty();
        }

        for (int i = 0; i < searchingEnchants.size(); i++) {
            RollingEnchantment e = searchingEnchants.get(i);
            Optional<RegistryEntry.Reference<Enchantment>> en;
            if (reg.isPresent()) {
                en = reg.get().getEntry(e.enchantment);
            } else {
                en = Optional.empty();
            }
            final int si = i;
            ItemStack book = Items.ENCHANTED_BOOK.getDefaultStack();
            int maxlevel = 255;
            if (en.isPresent()) {
                book = EnchantmentHelper.getEnchantedBookWith(new EnchantmentLevelEntry(en.get(), en.get().value().getMaxLevel()));
                maxlevel = en.get().value().getMaxLevel();
            }
            table.add(theme.item(book));

            WHorizontalList label = theme.horizontalList();
            WButton c = label.add(theme.button("Change")).widget();
            c.action = () -> mc.setScreen(new EnchantmentSelectScreen(theme, onlyTradeable, sel -> {
                searchingEnchants.set(si, sel);
                list.clear();
                fillWidget(theme, list, searchingEnchants, sortEnchantments, onlyTradeable, infoLogger, errorLogger);
            }));
            if (en.isPresent()) {
                label.add(theme.label(Names.get(en.get())));
            } else {
                label.add(theme.label(e.enchantment.toString()));
            }
            table.add(label);

            WIntEdit lev = table.add(theme.intEdit(e.minLevel, 0, maxlevel, true)).minWidth(40).expandX().widget();
            lev.action = () -> e.minLevel = lev.get();
            lev.tooltip = "Minimum enchantment level, 0 acts as maximum possible only (for custom 0 acts like 1)";

            WHorizontalList costbox = table.add(theme.horizontalList()).minWidth(50).expandX().widget();
            WIntEdit cost = costbox.add(theme.intEdit(e.maxCost, 0, 64, false)).minWidth(40).expandX().widget();
            cost.action = () -> e.maxCost = cost.get();
            cost.tooltip = "Maximum cost in emeralds, 0 means no limit";

            WButton setOptimal = costbox.add(theme.button("O")).widget();
            setOptimal.tooltip = "Set to optimal price (2 + maxLevel*3) (double if treasure) (if known)";
            setOptimal.action = () -> {
                list.clear();
                en.ifPresent(enchantmentReference -> e.maxCost = EnchantmentUtils.getMinimumPrice(enchantmentReference));
                fillWidget(theme, list, searchingEnchants, sortEnchantments, onlyTradeable, infoLogger, errorLogger);
            };

            WCheckbox enabled = table.add(theme.checkbox(e.enabled)).widget();
            enabled.action = () -> e.enabled = enabled.checked;
            enabled.tooltip = "Enabled?";

            WMinus del = table.add(theme.minus()).widget();
            del.action = () -> {
                list.clear();
                searchingEnchants.remove(e);
                fillWidget(theme, list, searchingEnchants, sortEnchantments, onlyTradeable, infoLogger, errorLogger);
            };
            table.row();
        }

        WTable controls = list.add(theme.table()).expandX().widget();

        WButton removeAll = controls.add(theme.button("Remove all")).expandX().widget();
        removeAll.action = () -> {
            list.clear();
            searchingEnchants.clear();
            fillWidget(theme, list, searchingEnchants, sortEnchantments, onlyTradeable, infoLogger, errorLogger);
        };

        WButton add = controls.add(theme.button("Add")).expandX().widget();
        add.action = () -> mc.setScreen(new EnchantmentSelectScreen(theme, onlyTradeable, e -> {
            e.minLevel = 1;
            e.maxCost = 64;
            e.enabled = true;
            searchingEnchants.add(e);
            list.clear();
            fillWidget(theme, list, searchingEnchants, sortEnchantments, onlyTradeable, infoLogger, errorLogger);
        }));

        WButton addAll = controls.add(theme.button("Add all")).expandX().widget();
        addAll.action = () -> {
            list.clear();
            searchingEnchants.clear();
            if (reg.isPresent()) {
                for (RegistryEntry<Enchantment> e : EnchantmentUtils.getEnchants(onlyTradeable)) {
                    searchingEnchants.add(new RollingEnchantment(reg.get().getId(e.value()), e.value().getMaxLevel(), EnchantmentUtils.getMinimumPrice(e), true));
                }
            }
            fillWidget(theme, list, searchingEnchants, sortEnchantments, onlyTradeable, infoLogger, errorLogger);
        };
        controls.row();

        WButton setOptimalForAll = controls.add(theme.button("Set optimal for all")).expandX().widget();
        setOptimalForAll.action = () -> {
            list.clear();
            if (reg.isPresent()) {
                for (RollingEnchantment e : searchingEnchants) {
                    reg.get().getEntry(e.enchantment).ifPresent(enchantmentReference -> e.maxCost = EnchantmentUtils.getMinimumPrice(enchantmentReference));
                }
            }
            fillWidget(theme, list, searchingEnchants, sortEnchantments, onlyTradeable, infoLogger, errorLogger);
        };

        WButton priceBumpUp = controls.add(theme.button("+1 to price for all")).expandX().widget();
        priceBumpUp.action = () -> {
            list.clear();
            for (RollingEnchantment e : searchingEnchants) {
                if (e.maxCost < 64) e.maxCost++;
            }
            fillWidget(theme, list, searchingEnchants, sortEnchantments, onlyTradeable, infoLogger, errorLogger);
        };

        WButton priceBumpDown = controls.add(theme.button("-1 to price for all")).expandX().widget();
        priceBumpDown.action = () -> {
            list.clear();
            for (RollingEnchantment e : searchingEnchants) {
                if (e.maxCost > 0) e.maxCost--;
            }
            fillWidget(theme, list, searchingEnchants, sortEnchantments, onlyTradeable, infoLogger, errorLogger);
        };
        controls.row();

        WButton setZeroForAll = controls.add(theme.button("Set zero price for all")).expandX().widget();
        setZeroForAll.action = () -> {
            list.clear();
            for (RollingEnchantment e : searchingEnchants) {
                e.maxCost = 0;
            }
            fillWidget(theme, list, searchingEnchants, sortEnchantments, onlyTradeable, infoLogger, errorLogger);
        };

        WButton enableAll = controls.add(theme.button("Enable all")).expandX().widget();
        enableAll.action = () -> {
            list.clear();
            for (RollingEnchantment e : searchingEnchants) {
                e.enabled = true;
            }
            fillWidget(theme, list, searchingEnchants, sortEnchantments, onlyTradeable, infoLogger, errorLogger);
        };

        WButton disableAll = controls.add(theme.button("Disable all")).expandX().widget();
        disableAll.action = () -> {
            list.clear();
            for (RollingEnchantment e : searchingEnchants) {
                e.enabled = false;
            }
            fillWidget(theme, list, searchingEnchants, sortEnchantments, onlyTradeable, infoLogger, errorLogger);
        };
        controls.row();
    }
}

