package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.dweller.DwellerCombatStrategyForBothSidesPlugin;
import com.fs.starfarer.api.impl.combat.dweller.DwellerShipCreator;
import tidal.shroud.scripts.ships.InsatiableHungerShipCreator;
import tidal.shroud.scripts.ships.ShroudedBansheeShipCreator;
import tidal.shroud.scripts.ships.ShroudedBrainShipCreator;

import java.util.LinkedHashMap;
import java.util.Map;

public class DwellerHullmodTidal extends BaseHullMod {
    public static Map<String, DwellerShipCreator> SHIP_CREATORS = new LinkedHashMap<>();
    public static String INITED_DWELLER_STUFF;

    static {
        SHIP_CREATORS.put("shrouded_brain", new ShroudedBrainShipCreator());
        SHIP_CREATORS.put("shrouded_banshee", new ShroudedBansheeShipCreator());
        SHIP_CREATORS.put("insatiable_hunger", new InsatiableHungerShipCreator());
        INITED_DWELLER_STUFF = "inited_dweller_stuff";
    }

    protected DwellerShipCreator getShipCreator(String hullId) {
        return (DwellerShipCreator)SHIP_CREATORS.get(hullId);
    }

    protected boolean addStrategyAI() {
        return true;
    }

    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        if (stats != null && stats.getVariant() != null) {
            String hullId = stats.getVariant().getHullSpec().getBaseHullId();
            DwellerShipCreator creator = this.getShipCreator(hullId);
            if (creator != null) {
                creator.initBeforeShipCreation(hullSize, stats, id);
            }

        }
    }

    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship != null && ship.getHullSpec() != null) {
            String hullId = ship.getHullSpec().getBaseHullId();
            DwellerShipCreator creator = this.getShipCreator(hullId);
            if (creator != null) {
                creator.initAfterShipCreation(ship, id);
            }

        }
    }

    public void applyEffectsAfterShipAddedToCombatEngine(ShipAPI ship, String id) {
        if (ship != null && ship.getHullSpec() != null) {
            String hullId = ship.getHullSpec().getBaseHullId();
            DwellerShipCreator creator = this.getShipCreator(hullId);
            if (creator != null) {
                creator.initAfterShipAddedToCombatEngine(ship, id);
            }

            if (this.addStrategyAI()) {
                CombatEngineAPI engine = Global.getCombatEngine();
                if (!engine.hasPluginOfClass(DwellerCombatStrategyForBothSidesPlugin.class)) {
                    engine.addPlugin(new DwellerCombatStrategyForBothSidesPlugin());
                }
            }

        }
    }

    public void advanceInCombat(ShipAPI ship, float amount) {
        if (!(amount <= 0.0F) && ship != null) {
            if (!ship.hasTag(INITED_DWELLER_STUFF)) {
                ship.addTag(INITED_DWELLER_STUFF);
                if (ship != null && ship.getHullSpec() != null) {
                    String hullId = ship.getHullSpec().getBaseHullId();
                    DwellerShipCreator creator = this.getShipCreator(hullId);
                    if (creator != null) {
                        creator.initInCombat(ship);
                    }

                }
            }
        }
    }
}
