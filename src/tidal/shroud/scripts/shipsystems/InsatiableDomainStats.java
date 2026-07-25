package tidal.shroud.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;


import java.util.Map;
import java.util.WeakHashMap;
import tidal.shroud.scripts.VFX.DomainVFX;


public class InsatiableDomainStats extends BaseShipSystemScript {

    private static final float DAMAGE_BONUS_PERCENT = 70f;
    private static final float DISSIPATION_BONUS_PERCENT = 25f;
    private static final float FLUX_CAPACITY_BONUS_PERCENT = 25f;
    private static final float FLUX_COST_MULT = 0.85f;
    private static final float FIRE_RATE_BONUS_PERCENT = 15f;
    private static final float COLLAPSE_OVERLOAD_SECONDS = 6f;
    private static final String SHROUDED_HULLMOD_ID = "dweller_hullmod_tidal";

    private final Map<ShipAPI, DomainState> stateByShip = new WeakHashMap<>();

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (!(stats.getEntity() instanceof ShipAPI)) {
            return;
        }
        ShipAPI ship = (ShipAPI) stats.getEntity();
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) {
            return;
        }

        String modIdPrefix = id + "_" + ship.getId();
        applyBuffsToDomainFleet(engine, ship, modIdPrefix, effectLevel);

        DomainState data = stateByShip.computeIfAbsent(ship, s -> new DomainState());
        if (!data.activeCycle && effectLevel > 0f) {
            data.activeCycle = true;
            data.overloadApplied = false;

            if (data.visual == null || data.visual.isExpired()) {
                data.visual = DomainVFX.createBoss(ship);
                engine.addLayeredRenderingPlugin(data.visual);
            }
        }

        if (data.visual != null) {
            data.visual.setMapActiveState(effectLevel);
        }

        if (ship == engine.getPlayerShip()) {
            engine.maintainStatusForPlayerShip(
                    modIdPrefix,
                    "graphics/icons/hullsys/targeting_feed.png",
                    "Forced Reality",
                    "",
                    false
            );
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        if (!(stats.getEntity() instanceof ShipAPI ship)) {
            return;
        }
        String modIdPrefix = id + "_" + ship.getId();
        clearBuffsFromDomainFleet(Global.getCombatEngine(), ship, modIdPrefix);

        DomainState data = stateByShip.computeIfAbsent(ship, s -> new DomainState());
        if (!data.activeCycle) {
            return;
        }
        data.activeCycle = false;

        if (data.visual != null) {
            data.visual.beginCollapse();
        }

        if (!data.overloadApplied && ship.isAlive() && ship.getFluxTracker() != null) {
            ship.getFluxTracker().forceOverload(COLLAPSE_OVERLOAD_SECONDS);
            data.overloadApplied = true;
        }
    }

    private void applyBuffs(MutableShipStatsAPI stats, String modId, float effectLevel) {
        stats.getBallisticWeaponDamageMult().modifyPercent(modId, DAMAGE_BONUS_PERCENT * effectLevel);
        stats.getEnergyWeaponDamageMult().modifyPercent(modId, DAMAGE_BONUS_PERCENT * effectLevel);
        stats.getMissileWeaponDamageMult().modifyPercent(modId, DAMAGE_BONUS_PERCENT * effectLevel);

        stats.getFluxDissipation().modifyPercent(modId, DISSIPATION_BONUS_PERCENT * effectLevel);
        stats.getFluxCapacity().modifyPercent(modId, FLUX_CAPACITY_BONUS_PERCENT * effectLevel);

        float fluxMult = 1f - ((1f - FLUX_COST_MULT) * effectLevel);
        stats.getBallisticWeaponFluxCostMod().modifyMult(modId, fluxMult);
        stats.getEnergyWeaponFluxCostMod().modifyMult(modId, fluxMult);
        stats.getMissileWeaponFluxCostMod().modifyMult(modId, fluxMult);

        stats.getBallisticRoFMult().modifyPercent(modId, FIRE_RATE_BONUS_PERCENT * effectLevel);
        stats.getEnergyRoFMult().modifyPercent(modId, FIRE_RATE_BONUS_PERCENT * effectLevel);
        stats.getMissileRoFMult().modifyPercent(modId, FIRE_RATE_BONUS_PERCENT * effectLevel);
    }

    private void clearBuffs(MutableShipStatsAPI stats, String modId) {
        stats.getBallisticWeaponDamageMult().unmodify(modId);
        stats.getEnergyWeaponDamageMult().unmodify(modId);
        stats.getMissileWeaponDamageMult().unmodify(modId);
        stats.getFluxDissipation().unmodify(modId);
        stats.getFluxCapacity().unmodify(modId);
        stats.getBallisticWeaponFluxCostMod().unmodify(modId);
        stats.getEnergyWeaponFluxCostMod().unmodify(modId);
        stats.getMissileWeaponFluxCostMod().unmodify(modId);
        stats.getBallisticRoFMult().unmodify(modId);
        stats.getEnergyRoFMult().unmodify(modId);
        stats.getMissileRoFMult().unmodify(modId);
    }

    private void applyBuffsToDomainFleet(CombatEngineAPI engine, ShipAPI source, String modIdPrefix, float effectLevel) {
        if (engine == null || source == null) {
            return;
        }


        applyBuffs(source.getMutableStats(), getBuffModId(modIdPrefix, source), effectLevel);

        for (ShipAPI other : engine.getShips()) {
            if (!isEligibleDomainAlly(source, other)) {
                continue;
            }
            applyBuffs(other.getMutableStats(), getBuffModId(modIdPrefix, other), effectLevel);
        }
    }

    private void clearBuffsFromDomainFleet(CombatEngineAPI engine, ShipAPI source, String modIdPrefix) {
        if (source == null) {
            return;
        }

        clearBuffs(source.getMutableStats(), getBuffModId(modIdPrefix, source));

        if (engine == null) {
            return;
        }

        for (ShipAPI other : engine.getShips()) {
            if (other == null || other == source || other.getOwner() != source.getOwner()) {
                continue;
            }
            clearBuffs(other.getMutableStats(), getBuffModId(modIdPrefix, other));
        }
    }

    private boolean isEligibleDomainAlly(ShipAPI source, ShipAPI other) {
        if (source == null || other == null) {
            return false;
        }
        if (other == source || !other.isAlive() || other.isHulk()) {
            return false;
        }
        if (other.getOwner() != source.getOwner()) {
            return false;
        }
        if (other.isFighter() || other.isDrone()) {
            return false;
        }
        return isShroudedShip(other);
    }

    private boolean isShroudedShip(ShipAPI ship) {
        if (ship == null || ship.getHullSpec() == null) {
            return false;
        }

        if (ship.getHullSpec().hasTag("dweller") || ship.getHullSpec().hasTag("shrouded")) {
            return true;
        }

        if (ship.getVariant() != null && ship.getVariant().hasHullMod(SHROUDED_HULLMOD_ID)) {
            return true;
        }

        String hullId = ship.getHullSpec().getBaseHullId();
        return hullId != null && hullId.startsWith("shrouded_");
    }

    private String getBuffModId(String prefix, ShipAPI target) {
        String targetId = target.getId();
        if (targetId == null || targetId.isEmpty()) {
            targetId = Integer.toString(target.hashCode());
        }
        return prefix + "_" + targetId;
    }

    private static class DomainState {
        private boolean activeCycle = false;
        private boolean overloadApplied = false;
        private DomainVFX visual;
    }

}


