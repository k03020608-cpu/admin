package pl.admintools;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class MuteManager {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Wpis> wyciszeni = new LinkedHashMap<>();

    public MuteManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "wyciszenia.yml");
    }

    public void wycisz(UUID uuid, String nazwa, long minuty, String powod) {
        long doCzasu = System.currentTimeMillis() + minuty * 60_000L;
        wyciszeni.put(uuid, new Wpis(nazwa, doCzasu, powod));
        save();
    }

    /** @return true, jeśli gracz faktycznie był wyciszony i zdjęto mu wyciszenie. */
    public boolean odcisz(UUID uuid) {
        boolean byl = wyciszeni.remove(uuid) != null;
        if (byl) {
            save();
        }
        return byl;
    }

    /** Sprawdza, czy gracz jest wyciszony TERAZ (automatycznie usuwa wygasłe wpisy). */
    public boolean jestWyciszony(UUID uuid) {
        Wpis wpis = wyciszeni.get(uuid);
        if (wpis == null) {
            return false;
        }
        if (wpis.doCzasu <= System.currentTimeMillis()) {
            wyciszeni.remove(uuid);
            save();
            return false;
        }
        return true;
    }

    public long pozostaleMinuty(UUID uuid) {
        Wpis wpis = wyciszeni.get(uuid);
        if (wpis == null) {
            return 0;
        }
        return Math.max(0, (wpis.doCzasu - System.currentTimeMillis()) / 60_000L);
    }

    public String powod(UUID uuid) {
        Wpis wpis = wyciszeni.get(uuid);
        return wpis != null ? wpis.powod : "";
    }

    public Map<UUID, Wpis> wszyscy() {
        return wyciszeni;
    }

    public static final class Wpis {
        public final String nazwa;
        public final long doCzasu;
        public final String powod;

        Wpis(String nazwa, long doCzasu, String powod) {
            this.nazwa = nazwa;
            this.doCzasu = doCzasu;
            this.powod = powod;
        }
    }

    // ---------- Zapis / odczyt ----------

    public void load() {
        wyciszeni.clear();
        if (!file.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("gracze");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(key);
            if (s == null) {
                continue;
            }
            try {
                UUID uuid = UUID.fromString(key);
                String nazwa = s.getString("nazwa", "?");
                long doCzasu = s.getLong("do", 0);
                String powod = s.getString("powod", "");
                wyciszeni.put(uuid, new Wpis(nazwa, doCzasu, powod));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Pominięto nieprawidłowy wpis w wyciszenia.yml: " + key);
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Wpis> entry : wyciszeni.entrySet()) {
            String path = "gracze." + entry.getKey();
            yaml.set(path + ".nazwa", entry.getValue().nazwa);
            yaml.set(path + ".do", entry.getValue().doCzasu);
            yaml.set(path + ".powod", entry.getValue().powod);
        }

        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Nie udało się zapisać wyciszenia.yml: " + e.getMessage());
        }
    }
}
