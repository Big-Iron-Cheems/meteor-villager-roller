package maxsuperman.addons.roller.util;

import maxsuperman.addons.roller.model.RollingEnchantment;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public class ConfigManager {
    public static final Path CONFIG_PATH = MeteorClient.FOLDER.toPath().resolve("VillagerRoller");

    private ConfigManager() {
    }

    public static boolean loadSearchingFromFile(Path path, @NonNull List<RollingEnchantment> searchingEnchants, Consumer<String> errorLogger) {
        if (!Files.exists(path) || !Files.isReadable(path)) {
            errorLogger.accept("File does not exist or can not be loaded");
            return false;
        }
        NbtCompound r;
        try {
            r = NbtIo.read(path);
        } catch (IOException e) {
            errorLogger.accept("Failed to read NBT from file: " + e.getMessage());
            return false;
        }
        if (r == null) {
            errorLogger.accept("Failed to load nbt from file");
            return false;
        }
        NbtList l = r.getListOrEmpty("rolling");
        searchingEnchants.clear();
        for (NbtElement e : l) {
            if (e.getType() != NbtElement.COMPOUND_TYPE) {
                errorLogger.accept("Invalid list element");
                return false;
            }
            searchingEnchants.add(new RollingEnchantment().fromTag((NbtCompound) e));
        }
        return true;
    }

    public static boolean saveSearchingToFile(Path path, @NonNull List<RollingEnchantment> searchingEnchants, Consumer<String> errorLogger) {
        NbtList l = new NbtList();
        for (RollingEnchantment e : searchingEnchants) {
            l.add(e.toTag());
        }
        NbtCompound c = new NbtCompound();
        c.put("rolling", l);

        Path parentDir = path.getParent();
        if (parentDir != null && Files.notExists(parentDir)) {
            try {
                Files.createDirectories(parentDir);
            } catch (IOException e) {
                errorLogger.accept("Failed to make directories: " + e.getMessage());
                return false;
            }
        }

        try {
            NbtIo.write(c, path);
        } catch (IOException e) {
            errorLogger.accept("Failed to write NBT to file: " + e.getMessage());
            return false;
        }
        return true;
    }
}

