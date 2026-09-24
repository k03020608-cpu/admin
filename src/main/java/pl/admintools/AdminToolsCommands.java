package pl.admintools;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AdminToolsCommands implements CommandExecutor {

    private final VanishManager vanish;
    private final FreezeManager freeze;
    private final MuteManager mute;
    private final DeathLogManager deathLog;
    private final DziennikManager dziennik;

    public AdminToolsCommands(VanishManager vanish, FreezeManager freeze, MuteManager mute,
                              DeathLogManager deathLog, DziennikManager dziennik) {
        this.vanish = vanish;
        this.freeze = freeze;
        this.mute = mute;
        this.deathLog = deathLog;
        this.dziennik = dziennik;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        return switch (command.getName().toLowerCase()) {
            case "zniknij" -> zniknij(sender);
            case "zamroz" -> zamroz(sender, args);
            case "zgony" -> zgony(sender, args);
            case "kickpowodem" -> kickPowodem(sender, args);
            case "wycisz" -> wycisz(sender, args);
            case "odcisz" -> odcisz(sender, args);
            case "wyciszeni" -> wyciszeni(sender);
            case "dziennik" -> pokazDziennik(sender, args);
            default -> false;
        };
    }

    private boolean zniknij(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Ta komenda jest tylko dla graczy.", NamedTextColor.RED));
            return true;
        }

        boolean teraz = vanish.przelacz(player);
        if (teraz) {
            player.sendMessage(Component.text("Jesteś teraz niewidzialny.", NamedTextColor.GREEN));
            dziennik.wpisz(player.getName() + " włączył tryb niewidzialny.");
        } else {
            player.sendMessage(Component.text("Jesteś znowu widzialny.", NamedTextColor.YELLOW));
            dziennik.wpisz(player.getName() + " wyłączył tryb niewidzialny.");
        }
        return true;
    }

    private boolean zamroz(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(Component.text("Użycie: /zamroz <gracz>", NamedTextColor.YELLOW));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Nie znaleziono gracza \"" + args[0] + "\" (musi być online).", NamedTextColor.RED));
            return true;
        }

        boolean teraz = freeze.przelacz(target.getUniqueId());
        String admin = sender instanceof Player p ? p.getName() : "Konsola";

        if (teraz) {
            sender.sendMessage(Component.text("Zamrożono gracza " + target.getName() + ".", NamedTextColor.GREEN));
            target.sendMessage(Component.text("Zostałeś zamrożony przez administratora.", NamedTextColor.RED));
            dziennik.wpisz(admin + " zamroził gracza " + target.getName() + ".");
        } else {
            sender.sendMessage(Component.text("Odmrożono gracza " + target.getName() + ".", NamedTextColor.GREEN));
            target.sendMessage(Component.text("Zostałeś odmrożony.", NamedTextColor.YELLOW));
            dziennik.wpisz(admin + " odmroził gracza " + target.getName() + ".");
        }
        return true;
    }

    private boolean zgony(CommandSender sender, String[] args) {
        String filtrGracza = null;
        int ilosc = 10;

        if (args.length >= 1) {
            // Jeśli pierwszy argument to liczba, traktujemy go jako ilość (bez filtra gracza).
            try {
                ilosc = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                filtrGracza = args[0];
                if (args.length >= 2) {
                    try {
                        ilosc = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ignored) {
                        // zostaw domyślną ilość
                    }
                }
            }
        }

        List<String> wpisy = deathLog.ostatnie(filtrGracza, Math.max(1, Math.min(ilosc, 50)));

        if (wpisy.isEmpty()) {
            sender.sendMessage(Component.text("Brak zapisanych zgonów.", NamedTextColor.GRAY));
            return true;
        }

        sender.sendMessage(Component.text("Ostatnie zgony:", NamedTextColor.GOLD));
        for (String linia : wpisy) {
            sender.sendMessage(Component.text(" " + linia, NamedTextColor.GRAY));
        }
        return true;
    }

    private boolean kickPowodem(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Użycie: /kickpowodem <gracz> <powód>", NamedTextColor.YELLOW));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Nie znaleziono gracza \"" + args[0] + "\" (musi być online).", NamedTextColor.RED));
            return true;
        }

        String powod = String.join(" ", List.of(args).subList(1, args.length));
        String admin = sender instanceof Player p ? p.getName() : "Konsola";

        target.kick(Component.text(powod, NamedTextColor.RED));
        sender.sendMessage(Component.text("Wyrzucono gracza " + target.getName() + ".", NamedTextColor.GREEN));
        dziennik.wpisz(admin + " wyrzucił gracza " + target.getName() + " - powód: " + powod);
        return true;
    }

    private boolean wycisz(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Użycie: /wycisz <gracz> <minuty> <powód>", NamedTextColor.YELLOW));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Nie znaleziono gracza \"" + args[0] + "\" (musi być online).", NamedTextColor.RED));
            return true;
        }

        if (target.hasPermission("admintools.bypass")) {
            sender.sendMessage(Component.text(
                    target.getName() + " ma uprawnienie chroniące przed wyciszeniem.", NamedTextColor.RED));
            return true;
        }

        long minuty;
        try {
            minuty = Long.parseLong(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Liczba minut musi być liczbą całkowitą.", NamedTextColor.RED));
            return true;
        }
        if (minuty < 1) {
            sender.sendMessage(Component.text("Liczba minut musi być większa od zera.", NamedTextColor.RED));
            return true;
        }

        String powod = String.join(" ", List.of(args).subList(2, args.length));
        String admin = sender instanceof Player p ? p.getName() : "Konsola";

        mute.wycisz(target.getUniqueId(), target.getName(), minuty, powod);
        sender.sendMessage(Component.text(
                "Wyciszono gracza " + target.getName() + " na " + minuty + " min.", NamedTextColor.GREEN));
        target.sendMessage(Component.text(
                "Zostałeś wyciszony na " + minuty + " min. Powód: " + powod, NamedTextColor.RED));
        dziennik.wpisz(admin + " wyciszył gracza " + target.getName() + " na " + minuty + " min - powód: " + powod);
        return true;
    }

    private boolean odcisz(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(Component.text("Użycie: /odcisz <gracz>", NamedTextColor.YELLOW));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Nie znaleziono gracza \"" + args[0] + "\" (musi być online).", NamedTextColor.RED));
            return true;
        }

        String admin = sender instanceof Player p ? p.getName() : "Konsola";

        if (mute.odcisz(target.getUniqueId())) {
            sender.sendMessage(Component.text("Zdjęto wyciszenie graczowi " + target.getName() + ".", NamedTextColor.GREEN));
            target.sendMessage(Component.text("Twoje wyciszenie zostało zdjęte.", NamedTextColor.YELLOW));
            dziennik.wpisz(admin + " zdjął wyciszenie graczowi " + target.getName() + ".");
        } else {
            sender.sendMessage(Component.text(target.getName() + " nie jest wyciszony.", NamedTextColor.YELLOW));
        }
        return true;
    }

    private boolean wyciszeni(CommandSender sender) {
        Map<UUID, MuteManager.Wpis> wszyscy = mute.wszyscy();

        if (wszyscy.isEmpty()) {
            sender.sendMessage(Component.text("Nikt obecnie nie jest wyciszony.", NamedTextColor.GRAY));
            return true;
        }

        sender.sendMessage(Component.text("Wyciszeni (" + wszyscy.size() + "):", NamedTextColor.GOLD));
        for (Map.Entry<UUID, MuteManager.Wpis> entry : wszyscy.entrySet()) {
            long pozostalo = mute.pozostaleMinuty(entry.getKey());
            sender.sendMessage(Component.text(
                    " - " + entry.getValue().nazwa + " (jeszcze " + pozostalo + " min) - " + entry.getValue().powod,
                    NamedTextColor.GRAY));
        }
        return true;
    }

    private boolean pokazDziennik(CommandSender sender, String[] args) {
        int ile = 15;
        if (args.length >= 1) {
            try {
                ile = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
                // zostaw domyślną wartość
            }
        }

        List<String> wpisy = dziennik.ostatnie(Math.max(1, Math.min(ile, 100)));
        if (wpisy.isEmpty()) {
            sender.sendMessage(Component.text("Dziennik jest pusty.", NamedTextColor.GRAY));
            return true;
        }

        sender.sendMessage(Component.text("Ostatnie akcje administracyjne:", NamedTextColor.GOLD));
        for (String linia : wpisy) {
            sender.sendMessage(Component.text(" " + linia, NamedTextColor.GRAY));
        }
        return true;
    }
}
