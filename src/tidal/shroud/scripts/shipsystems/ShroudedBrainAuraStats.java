package tidal.shroud.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;

import java.awt.Color;

public class ShroudedBrainAuraStats extends BaseShipSystemScript {

    private static final float AURA_RADIUS = 6000f;
    private static final float SPEED_BONUS = 40f;
    private static final float MANEUVER_BONUS = 25f;
    private static final float DISSIPATION_BONUS = 30f;
    private static final float DAMAGE_BONUS = 30f;
    private static final Color AURA_COLOR = new Color(132, 61, 190, 120);
    private final IntervalUtil interval = new IntervalUtil(0.2f, 0.2f);

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (!(stats.getEntity() instanceof ShipAPI)) {
            return;
        }
        ShipAPI source = (ShipAPI) stats.getEntity();
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) {
            return;
        }
        float amount = engine.getElapsedInLastFrame();
        interval.advance(amount);
        String buffId = id + "_" + source.getId();

        boolean showStatus = source == engine.getPlayerShip();
        if (interval.intervalElapsed()) {
            for (ShipAPI other : engine.getShips()) {
                if (!isEligibleTarget(source, other)) {
                    continue;
                }
                if (MathUtils.getDistance(source, other) > AURA_RADIUS) {
                    unapplyToTarget(other, buffId);
                    continue;
                }
                applyToTarget(other, buffId, effectLevel);
            }
        }

        if (showStatus) {
            engine.maintainStatusForPlayerShip(
                    id,
                    "graphics/icons/hullsys/targeting_feed.png",
                    "Neural Web",
                    "Buffing nearby shrouded vessels",
                    false
            );
        }

        float jitterLevel = 0.25f + 0.25f * effectLevel;
        source.setJitter(source, AURA_COLOR, jitterLevel, 6, 0f, 8f);
        source.setJitterUnder(source, AURA_COLOR, jitterLevel, 12, 16f);
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        if (!(stats.getEntity() instanceof ShipAPI)) {
            return;
        }
        ShipAPI source = (ShipAPI) stats.getEntity();
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) {
            return;
        }
        String buffId = id + "_" + source.getId();
        for (ShipAPI other : engine.getShips()) {
            if (isEligibleTarget(source, other)) {
                unapplyToTarget(other, buffId);
            }
        }
        source.setJitter(source, AURA_COLOR, 0f, 0, 0f, 0f);
        source.setJitterUnder(source, AURA_COLOR, 0f, 0, 0f);
    }

    private boolean isEligibleTarget(ShipAPI source, ShipAPI target) {
        if (target == null || target.getHullSpec() == null || target.isFighter() || !target.isAlive()) {
            return false;
        }
        if (source == null || target.getOwner() != source.getOwner()) {
            return false;
        }
        String hullId = target.getHullSpec().getHullId();
        return hullId != null && hullId.startsWith("shrouded_");
    }

    private void applyToTarget(ShipAPI target, String buffId, float effectLevel) {
        MutableShipStatsAPI stats = target.getMutableStats();
        stats.getMaxSpeed().modifyPercent(buffId, SPEED_BONUS * effectLevel);
        stats.getAcceleration().modifyPercent(buffId, MANEUVER_BONUS * effectLevel);
        stats.getDeceleration().modifyPercent(buffId, MANEUVER_BONUS * effectLevel);
        stats.getTurnAcceleration().modifyPercent(buffId, MANEUVER_BONUS * effectLevel);
        stats.getMaxTurnRate().modifyPercent(buffId, MANEUVER_BONUS * effectLevel);
        stats.getFluxDissipation().modifyPercent(buffId, DISSIPATION_BONUS * effectLevel);
        stats.getBallisticWeaponDamageMult().modifyPercent(buffId, DAMAGE_BONUS * effectLevel);
        stats.getEnergyWeaponDamageMult().modifyPercent(buffId, DAMAGE_BONUS * effectLevel);

        float jitterLevel = 0.2f * effectLevel;
        target.setJitter(target, AURA_COLOR, jitterLevel, 4, 0f, 5f);
        target.setJitterUnder(target, AURA_COLOR, jitterLevel, 8, 12f);
    }

    private void unapplyToTarget(ShipAPI target, String buffId) {
        MutableShipStatsAPI stats = target.getMutableStats();
        stats.getMaxSpeed().unmodify(buffId);
        stats.getAcceleration().unmodify(buffId);
        stats.getDeceleration().unmodify(buffId);
        stats.getTurnAcceleration().unmodify(buffId);
        stats.getMaxTurnRate().unmodify(buffId);
        stats.getFluxDissipation().unmodify(buffId);
        stats.getBallisticWeaponDamageMult().unmodify(buffId);
        stats.getEnergyWeaponDamageMult().unmodify(buffId);
    }
}
