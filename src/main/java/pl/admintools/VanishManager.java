package pl.admintools;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Tryb niewidzialny (vanish) - ukrywa gracza przed innymi graczami.
 * Stan trzymany tylko w pamięci (czyści się po restarcie serwera - to celowe,
 * żeby admin nie zapomniał, że jest niewidzialny po restarcie).
 */
public final class VanishManager {

    private final Plugin plugin;
    private final Set<UUID> ukryci = new HashSet<>();

    public VanishManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean jestUkryty(UUID uuid) {
        return ukryci.contains(uuid);
    }

    /** @return true, jeśli gracz jest teraz ukryty (czyli właśnie się schował). */
    public boolean przelacz(Player gracz) {
        if (ukryci.contains(gracz.getUniqueId())) {
            pokaz(gracz);
            return false;
        } else {
            ukryj(gracz);
            return true;
        }
    }

    private void ukryj(Player gracz) {
        ukryci.add(gracz.getUniqueId());
        for (Player inny : Bukkit.getOnlinePlayers()) {
            if (!inny.getUniqueId().equals(gracz.getUniqueId())) {
                inny.hidePlayer(plugin, gracz);
            }
        }
    }

    private void pokaz(Player gracz) {
        ukryci.remove(gracz.getUniqueId());
        for (Player inny : Bukkit.getOnlinePlayers()) {
            if (!inny.getUniqueId().equals(gracz.getUniqueId())) {
                inny.showPlayer(plugin, gracz);
            }
        }
    }

    /** Wołane przy dołączeniu nowego gracza, żeby nie widział aktualnie ukrytych adminów. */
    public void ukryjPrzedNowymGraczem(Player nowy) {
        for (UUID uuid : ukryci) {
            Player ukrytyGracz = Bukkit.getPlayer(uuid);
            if (ukrytyGracz != null && !ukrytyGracz.getUniqueId().equals(nowy.getUniqueId())) {
                nowy.hidePlayer(plugin, ukrytyGracz);
            }
        }
    }
}
