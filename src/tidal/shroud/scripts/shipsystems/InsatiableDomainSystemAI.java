package tidal.shroud.scripts.shipsystems;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FogOfWarAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class InsatiableDomainSystemAI implements ShipSystemAIScript {

    private static final float THINK_MIN = 0.15f;
    private static final float THINK_MAX = 0.3f;

    private static final float MAX_FLUX_LEVEL_TO_TRIGGER = 0.9f;
    private static final float MIN_SEARCH_RANGE = 1600f;
    private static final float MAX_SEARCH_RANGE = 8500f;
    private static final float WEAPON_RANGE_BUFFER = 450f;
    private static final float ARC_TOLERANCE_DEGREES = 32f;

    private static final float SYSTEM_DAMAGE_MULT = 1.7f;
    private static final float SYSTEM_ROF_MULT = 1.3f;
    private static final float SYSTEM_DPS_MULT = SYSTEM_DAMAGE_MULT * SYSTEM_ROF_MULT;
    private static final float COMMIT_WINDOW_SECONDS = 6.5f;

    private static final float SOFT_COMMIT_CONFIDENCE = 0.62f;
    private static final float HARD_COMMIT_CONFIDENCE = 0.92f;
    private static final float SOFT_COMMIT_SCORE = 1.05f;
    private static final float FINISHER_HULL_LEVEL = 0.22f;
    private static final float FINISHER_COMMIT_CONFIDENCE = 0.45f;

    private static final float SUPPORT_RADIUS = 1650f;
    private static final float SUPPORT_PENALTY_PER_SHIP = 0.12f;

    private final IntervalUtil thinkInterval = new IntervalUtil(THINK_MIN, THINK_MAX);

    private ShipAPI ship;
    private ShipSystemAPI system;
    private ShipwideAIFlags flags;
    private CombatEngineAPI engine;

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.system = system;
        this.flags = flags;
        this.engine = engine;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (engine == null || engine.isPaused() || ship == null || !ship.isAlive()) {
            return;
        }

        thinkInterval.advance(amount);
        if (!thinkInterval.intervalElapsed()) {
            return;
        }

        if (!canUseSystem()) {
            return;
        }

        TargetEvaluation chosen = pickTarget(target);
        if (chosen == null || !shouldCommit(chosen)) {
            return;
        }

        ship.setShipTarget(chosen.target);
        if (flags != null) {
            flags.setFlag(ShipwideAIFlags.AIFlags.TARGET_FOR_SHIP_SYSTEM, 0.75f, chosen.target);
            flags.setFlag(ShipwideAIFlags.AIFlags.DO_NOT_BACK_OFF, 0.75f);
        }
        ship.useSystem();
    }

    private boolean canUseSystem() {
        if (system == null) {
            return false;
        }
        if (!system.canBeActivated()) {
            return false;
        }
        if (system.isOutOfAmmo() || system.isActive() || system.isOn() || system.isChargeup() || system.isChargedown() || system.isCoolingDown()) {
            return false;
        }
        if (ship.getFluxTracker() != null) {
            if (ship.getFluxTracker().isOverloadedOrVenting()) {
                return false;
            }
            if (ship.getFluxTracker().getFluxLevel() > MAX_FLUX_LEVEL_TO_TRIGGER) {
                return false;
            }
        }
        if (flags != null && (flags.hasFlag(ShipwideAIFlags.AIFlags.DO_NOT_USE_FLUX) || flags.hasFlag(ShipwideAIFlags.AIFlags.BACKING_OFF))) {
            return false;
        }
        return true;
    }

    private TargetEvaluation pickTarget(ShipAPI fallback) {
        float searchRange = getSearchRange();
        TargetEvaluation best = null;

        best = chooseBetter(best, evaluateTarget(fallback, searchRange, 0.1f));
        best = chooseBetter(best, evaluateTarget(ship.getShipTarget(), searchRange, 0.14f));

        for (ShipAPI other : engine.getShips()) {
            TargetEvaluation eval = evaluateTarget(other, searchRange, 0f);
            if (eval == null) {
                continue;
            }
            best = chooseBetter(best, eval);
        }
        return best;
    }

    private TargetEvaluation chooseBetter(TargetEvaluation current, TargetEvaluation candidate) {
        if (candidate == null) {
            return current;
        }
        if (current == null) {
            return candidate;
        }
        return candidate.score > current.score ? candidate : current;
    }

    private TargetEvaluation evaluateTarget(ShipAPI target, float searchRange, float priorityBonus) {
        if (!isValidEnemy(target)) {
            return null;
        }

        float distance = Misc.getDistance(ship.getLocation(), target.getLocation());
        if (distance > searchRange) {
            return null;
        }

        float dps = estimateAppliedDps(target, distance);
        if (dps <= 0f) {
            return null;
        }

        float effectiveHitpoints = estimateEffectiveHitpoints(target);
        float predictedDamage = dps * SYSTEM_DPS_MULT * COMMIT_WINDOW_SECONDS;
        float killConfidence = predictedDamage / Math.max(1f, effectiveHitpoints);

        boolean overloaded = target.getFluxTracker() != null && target.getFluxTracker().isOverloaded();
        boolean venting = target.getFluxTracker() != null && target.getFluxTracker().isVenting();
        int supportShips = countNearbySupport(target);

        float score = killConfidence
                + computeVulnerabilityScore(target)
                + priorityBonus
                - (supportShips * SUPPORT_PENALTY_PER_SHIP)
                - computeDistancePenalty(distance, searchRange);

        return new TargetEvaluation(target, score, killConfidence, overloaded || venting);
    }

    private float estimateAppliedDps(ShipAPI target, float distance) {
        float total = 0f;
        Vector2f targetLoc = target != null ? target.getLocation() : null;

        List<WeaponAPI> usableWeapons = ship.getUsableWeapons();
        if (usableWeapons == null) {
            return fallbackHullDps(ship);
        }

        for (WeaponAPI weapon : usableWeapons) {
            if (weapon == null || weapon.isDisabled() || weapon.isDecorative() || weapon.isForceDisabled()) {
                continue;
            }
            if (weapon.getRange() + WEAPON_RANGE_BUFFER < distance) {
                continue;
            }
            if (targetLoc != null && weapon.distanceFromArc(targetLoc) > ARC_TOLERANCE_DEGREES) {
                continue;
            }

            WeaponAPI.DerivedWeaponStatsAPI derived = weapon.getDerivedStats();
            if (derived == null) {
                continue;
            }

            float weaponDps = Math.max(derived.getSustainedDps(), derived.getDps() * 0.8f);
            if (weaponDps <= 0f) {
                continue;
            }

            if (weapon.usesAmmo() && weapon.getMaxAmmo() > 0) {
                float ammoFrac = weapon.getAmmo() / (float) weapon.getMaxAmmo();
                weaponDps *= Math.max(0.25f, ammoFrac);
            }
            if (weapon.isBeam()) {
                weaponDps *= 0.92f;
            }

            total += weaponDps;
        }

        if (total <= 0f) {
            total = fallbackHullDps(ship);
        }

        float ownFlux = ship.getFluxTracker() != null ? ship.getFluxTracker().getFluxLevel() : ship.getFluxLevel();
        if (ownFlux > 0.85f) {
            total *= 0.7f;
        } else if (ownFlux > 0.7f) {
            total *= 0.82f;
        }

        return Math.max(0f, total);
    }

    private float fallbackHullDps(ShipAPI source) {
        if (source == null) {
            return 180f;
        }

        switch (source.getHullSize()) {
            case FIGHTER:
                return 70f;
            case FRIGATE:
                return 160f;
            case DESTROYER:
                return 270f;
            case CRUISER:
                return 430f;
            case CAPITAL_SHIP:
                return 640f;
            default:
                return 240f;
        }
    }

    private float estimateEffectiveHitpoints(ShipAPI target) {
        float hull = Math.max(1f, target.getHitpoints());
        float hullLevel = clamp01(target.getHullLevel());
        float fluxLevel = clamp01(target.getFluxLevel());

        boolean overloaded = target.getFluxTracker() != null && target.getFluxTracker().isOverloaded();
        boolean venting = target.getFluxTracker() != null && target.getFluxTracker().isVenting();
        boolean shieldOn = target.getShield() != null && target.getShield().isOn() && !overloaded && !venting;

        float effective = hull;
        if (shieldOn) {
            float shieldFactor = 1.6f - (0.45f * fluxLevel);
            effective *= clamp(shieldFactor, 1.15f, 1.6f);
        }
        if (overloaded) {
            effective *= 0.48f;
        } else if (venting) {
            effective *= 0.62f;
        }
        if (hullLevel < 0.35f) {
            effective *= (0.84f + (hullLevel * 0.4f));
        }

        return Math.max(1f, effective);
    }

    private float computeVulnerabilityScore(ShipAPI target) {
        float score = 0f;
        float fluxLevel = clamp01(target.getFluxLevel());
        float hullLevel = clamp01(target.getHullLevel());

        boolean overloaded = target.getFluxTracker() != null && target.getFluxTracker().isOverloaded();
        boolean venting = target.getFluxTracker() != null && target.getFluxTracker().isVenting();

        if (overloaded) {
            score += 0.55f;
        } else if (venting) {
            score += 0.38f;
        }

        if (fluxLevel >= 0.85f) {
            score += 0.24f;
        } else if (fluxLevel >= 0.65f) {
            score += 0.12f;
        }

        if (target.getShield() == null || !target.getShield().isOn()) {
            score += 0.15f;
        }

        score += (1f - hullLevel) * 0.3f;
        return score;
    }

    private int countNearbySupport(ShipAPI target) {
        int support = 0;
        for (ShipAPI other : engine.getShips()) {
            if (other == null || other == target) {
                continue;
            }
            if (!other.isAlive() || other.isHulk() || other.isFighter()) {
                continue;
            }
            if (other.getOwner() != target.getOwner()) {
                continue;
            }
            if (Misc.getDistance(target.getLocation(), other.getLocation()) <= SUPPORT_RADIUS) {
                support++;
            }
        }
        return support;
    }

    private float computeDistancePenalty(float distance, float searchRange) {
        float normalized = clamp01(distance / Math.max(1f, searchRange));
        return normalized * 0.25f;
    }

    private float getSearchRange() {
        float maxWeaponRange = 0f;
        List<WeaponAPI> usableWeapons = ship.getUsableWeapons();
        if (usableWeapons == null) {
            return 2500f;
        }

        for (WeaponAPI weapon : usableWeapons) {
            if (weapon == null || weapon.isDecorative() || weapon.isDisabled()) {
                continue;
            }
            maxWeaponRange = Math.max(maxWeaponRange, weapon.getRange());
        }
        if (maxWeaponRange <= 0f) {
            maxWeaponRange = 2500f;
        }
        return clamp(maxWeaponRange + WEAPON_RANGE_BUFFER, MIN_SEARCH_RANGE, MAX_SEARCH_RANGE);
    }

    private boolean shouldCommit(TargetEvaluation evaluation) {
        if (evaluation == null) {
            return false;
        }
        if (evaluation.killConfidence >= HARD_COMMIT_CONFIDENCE) {
            return true;
        }
        if (evaluation.targetOverloadedOrVenting && evaluation.killConfidence >= SOFT_COMMIT_CONFIDENCE) {
            return true;
        }
        if (evaluation.target.getHullLevel() <= FINISHER_HULL_LEVEL && evaluation.killConfidence >= FINISHER_COMMIT_CONFIDENCE) {
            return true;
        }
        return evaluation.score >= SOFT_COMMIT_SCORE && evaluation.killConfidence >= SOFT_COMMIT_CONFIDENCE;
    }

    private boolean isValidEnemy(ShipAPI other) {
        if (other == null || other == ship) {
            return false;
        }
        if (!other.isAlive() || other.isHulk() || other.isFighter()) {
            return false;
        }
        if (other.getOwner() == ship.getOwner()) {
            return false;
        }
        return canSeeTarget(other);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private float clamp01(float value) {
        return clamp(value, 0f, 1f);
    }

    private boolean canSeeTarget(ShipAPI target) {
        if (engine == null || target == null) {
            return false;
        }
        int owner = ship != null ? ship.getOwner() : -1;
        if (owner < 0) {
            return true;
        }

        FogOfWarAPI fog = engine.getFogOfWar(owner);
        if (fog != null) {
            return fog.isVisible(target);
        }
        return engine.isAwareOf(owner, target);
    }

    private static final class TargetEvaluation {
        private final ShipAPI target;
        private final float score;
        private final float killConfidence;
        private final boolean targetOverloadedOrVenting;

        private TargetEvaluation(ShipAPI target, float score, float killConfidence, boolean targetOverloadedOrVenting) {
            this.target = target;
            this.score = score;
            this.killConfidence = killConfidence;
            this.targetOverloadedOrVenting = targetOverloadedOrVenting;
        }
    }
}
