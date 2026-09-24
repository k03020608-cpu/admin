package pl.admintools;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class AdminToolsPlugin extends JavaPlugin {

    private MuteManager mute;
    private DeathLogManager deathLog;
    private DziennikManager dziennik;

    @Override
    public void onEnable() {
        VanishManager vanish = new VanishManager(this);
        FreezeManager freeze = new FreezeManager();

        mute = new MuteManager(this);
        mute.load();

        deathLog = new DeathLogManager(this);
        deathLog.load();

        dziennik = new DziennikManager(this);
        dziennik.load();

        getServer().getPluginManager().registerEvents(
                new AdminToolsListener(freeze, vanish, mute, deathLog), this);

        AdminToolsCommands commands = new AdminToolsCommands(vanish, freeze, mute, deathLog, dziennik);
        String[] nazwyKomend = {
                "zniknij", "zamroz", "zgony", "kickpowodem", "wycisz", "odcisz", "wyciszeni", "dziennik"
        };
        for (String name : nazwyKomend) {
            PluginCommand command = getCommand(name);
            if (command != null) {
                command.setExecutor(commands);
            }
        }

        getLogger().info("AdminTools załadowany.");
    }

    @Override
    public void onDisable() {
        if (mute != null) mute.save();
        if (deathLog != null) deathLog.save();
        if (dziennik != null) dziennik.save();
    }
}
