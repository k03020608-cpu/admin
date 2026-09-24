package pl.admintools;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Zamrożenie gracza - blokuje ruch, atakowanie i budowanie.
 * Stan w pamięci - jeśli serwer się zrestartuje, wszyscy są automatycznie odmrożeni.
 */
public final class FreezeManager {

    private final Set<UUID> zamrozeni = new HashSet<>();

    public boolean jestZamrozony(UUID uuid) {
        return zamrozeni.contains(uuid);
    }

    /** @return true, jeśli gracz jest teraz zamrożony (czyli właśnie go zamrożono). */
    public boolean przelacz(UUID uuid) {
        if (zamrozeni.contains(uuid)) {
            zamrozeni.remove(uuid);
            return false;
        } else {
            zamrozeni.add(uuid);
            return true;
        }
    }
}
