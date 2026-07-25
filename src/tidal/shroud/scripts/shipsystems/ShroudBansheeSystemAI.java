package tidal.shroud.scripts.shipsystems;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class ShroudBansheeSystemAI implements ShipSystemAIScript {

    private static final float THINK_MIN = 0.2f;
    private static final float THINK_MAX = 0.35f;
    public static final float MIN_TRIGGER_RANGE = 350f;
    public static final float MAX_TRIGGER_RANGE = 1400f;
    private static final float MAX_FLUX_LEVEL_TO_TRIGGER = 0.92f;
    public static final float ISOLATION_RADIUS = 1800f;
    public static final int ISOLATION_MAX_SUPPORT = 1;
    public static final float TELEPORT_OFFSET = 140f;
    public static final float REAR_DOT_MAX = -0.55f;
    public static final float TELEPORT_CLEARANCE_PADDING = 35f;
    public static final float TELEPORT_MAP_MARGIN = 220f;

    private static final float[] REAR_ANGLE_OFFSETS = new float[]{0f, 15f, -15f, 30f, -30f};
    private static final float[] ISOLATED_TELEPORT_ANGLE_OFFSETS =
            new float[]{0f, 20f, -20f, 45f, -45f, 70f, -70f, 100f, -100f, 135f, -135f, 180f};

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
        if (engine == null || engine.isPaused() || ship == null || !ship.isAlive()) return;

        thinkInterval.advance(amount);
        if (!thinkInterval.intervalElapsed()) return;

        if (!canUseSystem()) {
            aimAtBestVisibleTarget(target);
            return;
        }

        TeleportSolution solution = pickTeleportSolution(ship, engine, true);
        if (solution != null) {
            activateSystemOnTarget(solution.target);
        } else {
            aimAtBestVisibleTarget(target);
        }
    }

    public static TeleportSolution pickTeleportSolution(ShipAPI ship, CombatEngineAPI engine, boolean checkVisibility) {
        ShipAPI explicitTarget = ship.getShipTarget();

        TeleportSolution explicitRear = evaluateRearTarget(ship, engine, explicitTarget, true, checkVisibility);
        if (explicitRear != null) return explicitRear;

        TeleportSolution explicitIsolated = evaluateIsolatedTarget(ship, engine, explicitTarget, true, checkVisibility);
        if (explicitIsolated != null) return explicitIsolated;

        TeleportSolution bestRear = null;
        for (ShipAPI other : engine.getShips()) {
            bestRear = chooseBetter(bestRear, evaluateRearTarget(ship, engine, other, false, checkVisibility));
        }
        if (bestRear != null) return bestRear;

        TeleportSolution bestIsolated = null;
        for (ShipAPI other : engine.getShips()) {
            bestIsolated = chooseBetter(bestIsolated, evaluateIsolatedTarget(ship, engine, other, false, checkVisibility));
        }
        return bestIsolated;
    }

    private static TeleportSolution evaluateRearTarget(ShipAPI ship, CombatEngineAPI engine, ShipAPI target, boolean explicitTarget, boolean checkVisibility) {
        if (!isValidEnemy(ship, target, engine, checkVisibility)) return null;

        float distance = Misc.getDistance(ship.getLocation(), target.getLocation());
        if (distance < MIN_TRIGGER_RANGE || distance > MAX_TRIGGER_RANGE) return null;

        Vector2f destination = findRearTeleportPoint(ship, target, engine);
        if (destination == null) return null;

        int support = countNearbySupport(ship, target, engine);
        float score = distance + (support * 300f);
        if (explicitTarget) score -= 300f;

        return new TeleportSolution(target, destination, score);
    }

    private static TeleportSolution evaluateIsolatedTarget(ShipAPI ship, CombatEngineAPI engine, ShipAPI target, boolean allowExplicitBonus, boolean checkVisibility) {
        if (!isValidEnemy(ship, target, engine, checkVisibility)) return null;
        int support = countNearbySupport(ship, target, engine);
        if (support > ISOLATION_MAX_SUPPORT) return null;

        float distance = Misc.getDistance(ship.getLocation(), target.getLocation());
        if (distance < MIN_TRIGGER_RANGE || distance > MAX_TRIGGER_RANGE) return null;

        Vector2f destination = findIsolatedTeleportPoint(ship, target, engine);
        if (destination == null) return null;

        float score = distance + (support * 300f);
        if (allowExplicitBonus && target == ship.getShipTarget()) score -= 220f;

        return new TeleportSolution(target, destination, score);
    }

    public static Vector2f findRearTeleportPoint(ShipAPI ship, ShipAPI target, CombatEngineAPI engine) {
        if (target == null) return null;

        float radius = Math.max(ship.getCollisionRadius() + target.getCollisionRadius() + TELEPORT_OFFSET, 180f);
        for (float offset : REAR_ANGLE_OFFSETS) {
            Vector2f point = MathUtils.getPoint(target.getLocation(), radius, target.getFacing() + 180f + offset);
            if (!isRearPoint(target, point)) continue;
            if (!isTeleportPointViable(ship, target, point, engine)) continue;
            return point;
        }
        return null;
    }

    public static Vector2f findIsolatedTeleportPoint(ShipAPI ship, ShipAPI target, CombatEngineAPI engine) {
        Vector2f rear = findRearTeleportPoint(ship, target, engine);
        if (rear != null) return rear;

        float radius = Math.max(ship.getCollisionRadius() + target.getCollisionRadius() + TELEPORT_OFFSET, 180f);
        for (float offset : ISOLATED_TELEPORT_ANGLE_OFFSETS) {
            Vector2f point = MathUtils.getPoint(target.getLocation(), radius, target.getFacing() + 180f + offset);
            if (!isTeleportPointViable(ship, target, point, engine)) continue;
            return point;
        }
        return null;
    }

    public static boolean isRearPoint(ShipAPI target, Vector2f point) {
        if (target == null || point == null) return false;
        Vector2f forward = Misc.getUnitVectorAtDegreeAngle(target.getFacing());
        Vector2f toPoint = Vector2f.sub(point, target.getLocation(), new Vector2f());
        if (toPoint.lengthSquared() < 1f) return false;
        toPoint.normalise();
        return (forward.x * toPoint.x + forward.y * toPoint.y) <= REAR_DOT_MAX;
    }

    public static boolean isTeleportPointViable(ShipAPI ship, ShipAPI target, Vector2f point, CombatEngineAPI engine) {
        if (!isPointInsideMap(point, engine)) return false;

        for (ShipAPI other : engine.getShips()) {
            if (other == null || other == ship || other == target) continue;
            if (!other.isAlive() || other.isHulk() || other.isFighter()) continue;

            float clearance = ship.getCollisionRadius() + other.getCollisionRadius() + TELEPORT_CLEARANCE_PADDING;
            if (Misc.getDistance(point, other.getLocation()) < clearance) return false;
        }
        return true;
    }

    public static boolean isPointInsideMap(Vector2f point, CombatEngineAPI engine) {
        if (engine == null || point == null) return false;
        float halfW = (engine.getMapWidth() * 0.5f) - TELEPORT_MAP_MARGIN;
        float halfH = (engine.getMapHeight() * 0.5f) - TELEPORT_MAP_MARGIN;
        if (halfW <= 0f || halfH <= 0f) return true;
        return Math.abs(point.x) <= halfW && Math.abs(point.y) <= halfH;
    }

    public static int countNearbySupport(ShipAPI ship, ShipAPI target, CombatEngineAPI engine) {
        int support = 0;
        for (ShipAPI other : engine.getShips()) {
            if (other == null || other == target) continue;
            if (!other.isAlive() || other.isHulk() || other.isFighter()) continue;
            if (other.getOwner() != target.getOwner()) continue;
            if (Misc.getDistance(target.getLocation(), other.getLocation()) <= ISOLATION_RADIUS) support++;
        }
        return support;
    }

    public static boolean isValidEnemy(ShipAPI ship, ShipAPI other, CombatEngineAPI engine, boolean checkVisibility) {
        if (other == null || other == ship) return false;
        if (!other.isAlive() || other.isHulk() || other.isFighter()) return false;
        if (other.getOwner() == ship.getOwner()) return false;

        if (checkVisibility && engine != null) {
            int owner = ship.getOwner();
            FogOfWarAPI fog = engine.getFogOfWar(owner);
            return fog != null ? fog.isVisible(other) : engine.isAwareOf(owner, other);
        }
        return true;
    }

    private void aimAtBestVisibleTarget(ShipAPI fallbackTarget) {
        ShipAPI best = null;
        float bestDistance = Float.MAX_VALUE;

        if (isValidEnemy(ship, fallbackTarget, engine, true)) {
            int support = countNearbySupport(ship, fallbackTarget, engine);
            float dist = Misc.getDistance(ship.getLocation(), fallbackTarget.getLocation());
            if (support <= ISOLATION_MAX_SUPPORT && dist >= MIN_TRIGGER_RANGE && dist <= MAX_TRIGGER_RANGE) {
                best = fallbackTarget;
                bestDistance = dist;
            }
        }

        for (ShipAPI other : engine.getShips()) {
            if (!isValidEnemy(ship, other, engine, true)) continue;
            int support = countNearbySupport(ship, other, engine);
            if (support > ISOLATION_MAX_SUPPORT) continue;

            float dist = Misc.getDistance(ship.getLocation(), other.getLocation());
            if (dist < MIN_TRIGGER_RANGE || dist > MAX_TRIGGER_RANGE) continue;

            if (dist < bestDistance) {
                best = other;
                bestDistance = dist;
            }
        }

        if (best != null) ship.setShipTarget(best);
    }

    private void activateSystemOnTarget(ShipAPI target) {
        if (target == null) return;
        if (flags != null && (flags.hasFlag(ShipwideAIFlags.AIFlags.DO_NOT_USE_FLUX) || flags.hasFlag(ShipwideAIFlags.AIFlags.BACKING_OFF))) {
            ship.setShipTarget(target);
            return;
        }
        ship.setShipTarget(target);
        if (flags != null) {
            flags.setFlag(ShipwideAIFlags.AIFlags.TARGET_FOR_SHIP_SYSTEM, 0.75f, target);
        }
        ship.useSystem();
    }

    private boolean canUseSystem() {
        if (system == null || system.isOutOfAmmo() || system.isActive() || system.isOn() || system.isChargeup() || system.isChargedown() || system.isCoolingDown()) return false;
        if (ship.getFluxTracker() != null) {
            if (ship.getFluxTracker().isOverloadedOrVenting()) return false;
            return !(ship.getFluxTracker().getFluxLevel() > MAX_FLUX_LEVEL_TO_TRIGGER);
        }
        return true;
    }

    private static TeleportSolution chooseBetter(TeleportSolution current, TeleportSolution candidate) {
        if (candidate == null) return current;
        if (current == null) return candidate;
        return candidate.score < current.score ? candidate : current;
    }

    public record TeleportSolution(ShipAPI target, Vector2f destination, float score) {}
}