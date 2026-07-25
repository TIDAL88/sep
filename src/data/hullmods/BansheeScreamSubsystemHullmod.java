package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import org.magiclib.subsystems.MagicSubsystem;
import org.magiclib.subsystems.MagicSubsystemsManager;
import tidal.shroud.scripts.shipsystems.activators.BansheeScreamActivator;

import java.util.List;

public class BansheeScreamSubsystemHullmod extends BaseHullMod {

    public static final String HULLMOD_ID = "td_banshee_scream_subsystem";

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ensureSubsystem(ship);
    }

    @Override
    public void applyEffectsAfterShipAddedToCombatEngine(ShipAPI ship, String id) {
        ensureSubsystem(ship);
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (amount <= 0f) {
            return;
        }
        ensureSubsystem(ship);
    }

    private void ensureSubsystem(ShipAPI ship) {
        if (ship == null || ship.isFighter() || ship.isDrone()) {
            return;
        }

        List<MagicSubsystem> subsystems = MagicSubsystemsManager.getSubsystemsForShipCopy(ship);
        if (subsystems != null) {
            for (MagicSubsystem subsystem : subsystems) {
                if (subsystem instanceof BansheeScreamActivator) {
                    return;
                }
            }
        }

        MagicSubsystemsManager.addSubsystemToShip(ship, new BansheeScreamActivator(ship));
    }
}
