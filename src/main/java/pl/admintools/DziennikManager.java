package pl.admintools;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Prosty dziennik akcji administracyjnych (kick, mute, freeze).
 * Najnowsze wpisy są na końcu listy. Trzyma maksymalnie MAX_WPISOW,
 * starsze są usuwane.
 */
public final class DziennikManager {

    private static final int MAX_WPISOW = 300;
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd.MM HH:mm");

    private final JavaPlugin plugin;
    private final File file;
    private final List<String> wpisy = new ArrayList<>();

    public DziennikManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "dziennik.yml");
    }

    public void wpisz(String tekst) {
        String znacznik = LocalDateTime.now().format(FORMAT);
        wpisy.add("[" + znacznik + "] " + tekst);

        while (wpisy.size() > MAX_WPISOW) {
            wpisy.remove(0);
        }

        plugin.getLogger().info("[Dziennik] " + tekst);
        save();
    }

    /** Zwraca ostatnie wpisy, najnowsze pierwsze. */
    public List<String> ostatnie(int ile) {
        List<String> wynik = new ArrayList<>();
        int od = Math.max(0, wpisy.size() - ile);
        for (int i = wpisy.size() - 1; i >= od; i--) {
            wynik.add(wpisy.get(i));
        }
        return wynik;
    }

    public void load() {
        wpisy.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        List<String> zapisane = yaml.getStringList("wpisy");
        wpisy.addAll(zapisane);
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("wpisy", wpisy);
        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Nie udało się zapisać dziennik.yml: " + e.getMessage());
        }
    }
}
