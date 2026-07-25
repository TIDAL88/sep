package tidal.shroud.scripts.ships;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

import java.awt.Color;
import java.util.List;

import com.fs.starfarer.api.impl.combat.dweller.*;
import org.lwjgl.util.vector.Vector2f;


public class ShroudedBrainShipCreator extends BaseDwellerShipCreator {
    public static float RANGE_BONUS = 300F;
    public static float SHIELD_SPEED = 600F;
    public static float SHIELD_SOFT_FLUX_CONVERSION = 1F;

    public void initBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        super.initBeforeShipCreation(hullSize, stats, id);
        stats.getEnergyWeaponRangeBonus().modifyPercent(id, RANGE_BONUS);
        stats.getShieldUnfoldRateMult().modifyPercent(id, SHIELD_SPEED);
        stats.getShieldSoftFluxConversion().modifyFlat(id, SHIELD_SOFT_FLUX_CONVERSION);
    }
    protected DwellerCombatPlugin createPlugin(ShipAPI ship) {
        DwellerCombatPlugin plugin = super.createPlugin(ship);
        List<DwellerShipPart> parts = plugin.getParts();
        DwellerCombatPlugin.WobblyPart part = new DwellerCombatPlugin.WobblyPart("shrouded_brain", 0.4F, 1.0F, new Vector2f(0.0F, 0.0F), 0.0F);
        parts.add(part);
        Color glow = DwellerCombatPlugin.STANDARD_PART_GLOW_COLOR;
        part = new DwellerCombatPlugin.WobblyPart("shrouded_eye_cluster1", 1.2F, 2, 3, 2.0F, new Vector2f(70.0F, 0.0F), 0.0F);
        part.color = glow;
        part.additiveBlend = true;
        parts.add(part);
        part = new DwellerCombatPlugin.WobblyPart("shrouded_eye_cluster2", 1.35F, 3, 3, 2.0F, new Vector2f(-10.0F, 0.0F), 0.0F);
        part.color = glow;
        part.additiveBlend = true;
        parts.add(part);
        part = new DwellerCombatPlugin.WobblyPart("shrouded_eye_cluster3", 0.6F, 3, 3, 2.0F, new Vector2f(100.0F, 0.0F), 0.0F);
        part.color = glow;
        part.additiveBlend = true;
        parts.add(part);
        parts.add(part);
        ShroudedEyeShipCreator.PlasmaEyePart eyePart = new ShroudedEyeShipCreator.PlasmaEyePart(new Vector2f(0.0F, 0.0F), 0.0F, ship, 110.0F);
        parts.add(eyePart);
        return plugin;
    }

    @Override
    public void initInCombat(ShipAPI ship) {
        super.initInCombat(ship);
        if (ship == null) {
            return;
        }

        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine != null && ship == engine.getPlayerShip()) {
            return;
        }
        ship.setShipAI(new ShroudedBrainGuardianAI(ship));
    }

}
