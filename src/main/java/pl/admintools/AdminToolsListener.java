package pl.admintools;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public final class AdminToolsListener implements Listener {

    private final FreezeManager freeze;
    private final VanishManager vanish;
    private final MuteManager mute;
    private final DeathLogManager deathLog;

    public AdminToolsListener(FreezeManager freeze, VanishManager vanish,
                              MuteManager mute, DeathLogManager deathLog) {
        this.freeze = freeze;
        this.vanish = vanish;
        this.mute = mute;
        this.deathLog = deathLog;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        vanish.ukryjPrzedNowymGraczem(event.getPlayer());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!freeze.jestZamrozony(event.getPlayer().getUniqueId())) {
            return;
        }
        // Pozwól patrzeć w różne strony, ale zablokuj faktyczne przemieszczenie się.
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
            event.setTo(new Location(to.getWorld(), from.getX(), from.getY(), from.getZ(), to.getYaw(), to.getPitch()));
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (freeze.jestZamrozony(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (freeze.jestZamrozony(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        Entity atakujacy = event.getDamager();

        // Bezpośredni atak zamrożonego gracza.
        if (atakujacy instanceof Player p && freeze.jestZamrozony(p.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        // Atak strzałą/pociskiem wystrzelonym przez zamrożonego gracza.
        if (atakujacy instanceof Projectile projectile
                && projectile.getShooter() instanceof Player strzelec
                && freeze.jestZamrozony(strzelec.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!mute.jestWyciszony(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        long minuty = mute.pozostaleMinuty(player.getUniqueId());
        String powod = mute.powod(player.getUniqueId());

        player.sendMessage(Component.text(
                "Jesteś wyciszony jeszcze przez " + minuty + " min. Powód: " + powod, NamedTextColor.RED));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player ofiara = event.getEntity();
        LivingEntity zabojca = ofiara.getKiller();

        String przyczyna;
        if (zabojca instanceof Player p) {
            przyczyna = "zabity przez gracza " + p.getName();
        } else if (zabojca != null) {
            przyczyna = "zabity przez " + zabojca.getType().name();
        } else {
            przyczyna = event.deathMessage() != null
                    ? plainText(event.deathMessage())
                    : "przyczyna nieznana";
        }

        Location loc = ofiara.getLocation();
        deathLog.zapiszZgon(ofiara.getName(), przyczyna, loc.getWorld().getName(),
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    private String plainText(Component component) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component);
    }
}
