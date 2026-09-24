package pl.admintools;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class DeathLogManager {

    private static final int MAX_WPISOW = 200;
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd.MM HH:mm");

    private final JavaPlugin plugin;
    private final File file;
    private final List<Wpis> zgony = new ArrayList<>();

    public DeathLogManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "zgony.yml");
    }

    public void zapiszZgon(String ofiara, String przyczyna, String swiat, int x, int y, int z) {
        String znacznik = LocalDateTime.now().format(FORMAT);
        zgony.add(new Wpis(znacznik, ofiara, przyczyna, swiat, x, y, z));

        while (zgony.size() > MAX_WPISOW) {
            zgony.remove(0);
        }
        save();
    }

    /** Ostatnie zgony (opcjonalnie tylko danego gracza), najnowsze pierwsze, sformatowane do czatu. */
    public List<String> ostatnie(String filtrGracza, int ile) {
        List<String> wynik = new ArrayList<>();
        for (int i = zgony.size() - 1; i >= 0 && wynik.size() < ile; i--) {
            Wpis w = zgony.get(i);
            if (filtrGracza != null && !w.ofiara.equalsIgnoreCase(filtrGracza)) {
                continue;
            }
            wynik.add("[" + w.czas + "] " + w.ofiara + " - " + w.przyczyna
                    + " (" + w.swiat + " " + w.x + " " + w.y + " " + w.z + ")");
        }
        return wynik;
    }

    private static final class Wpis {
        final String czas;
        final String ofiara;
        final String przyczyna;
        final String swiat;
        final int x, y, z;

        Wpis(String czas, String ofiara, String przyczyna, String swiat, int x, int y, int z) {
            this.czas = czas;
            this.ofiara = ofiara;
            this.przyczyna = przyczyna;
            this.swiat = swiat;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    // ---------- Zapis / odczyt ----------

    public void load() {
        zgony.clear();
        if (!file.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("zgony");
        if (section == null) {
            return;
        }

        List<String> klucze = new ArrayList<>(section.getKeys(false));
        klucze.sort((a, b) -> {
            try {
                return Integer.compare(Integer.parseInt(a), Integer.parseInt(b));
            } catch (NumberFormatException e) {
                return a.compareTo(b);
            }
        });

        for (String key : klucze) {
            ConfigurationSection s = section.getConfigurationSection(key);
            if (s == null) {
                continue;
            }
            zgony.add(new Wpis(
                    s.getString("czas", "?"),
                    s.getString("ofiara", "?"),
                    s.getString("przyczyna", "?"),
                    s.getString("swiat", "?"),
                    s.getInt("x", 0),
                    s.getInt("y", 0),
                    s.getInt("z", 0)
            ));
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (int i = 0; i < zgony.size(); i++) {
            Wpis w = zgony.get(i);
            String path = "zgony." + i;
            yaml.set(path + ".czas", w.czas);
            yaml.set(path + ".ofiara", w.ofiara);
            yaml.set(path + ".przyczyna", w.przyczyna);
            yaml.set(path + ".swiat", w.swiat);
            yaml.set(path + ".x", w.x);
            yaml.set(path + ".y", w.y);
            yaml.set(path + ".z", w.z);
        }

        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Nie udało się zapisać zgony.yml: " + e.getMessage());
        }
    }
}
