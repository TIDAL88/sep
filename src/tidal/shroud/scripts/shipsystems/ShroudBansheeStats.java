package tidal.shroud.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual;
import com.fs.starfarer.api.impl.combat.RiftCascadeEffect;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import tidal.shroud.scripts.shipsystems.ShroudBansheeSystemAI.TeleportSolution;

import java.awt.Color;
import java.util.Map;
import java.util.WeakHashMap;

public class ShroudBansheeStats extends BaseShipSystemScript {

    private static final float DAMAGE_BONUS_PERCENT = 50f;
    private static final float DISSIPATION_BONUS_PERCENT = 20f;
    private static final float RANGE_BONUS = 40F;
    private static final float RETURN_DELAY_SECONDS = 15f;
    private static final float RETURN_FLUX_LEVEL = 0.90f;
    private static final float BRAIN_RETURN_PADDING = 60f;
    private static final float BRAIN_RETURN_CLEARANCE = 25f;
    private static final float REAR_HOLD_DISTANCE = 120f;
    private static final float REAR_APPROACH_DISTANCE = 500f;
    private static final float REAR_STRAFE_THRESHOLD = 45f;
    public static final String KEY = "brainkey";
    private static final Color ORIGIN_RIFT_COLOR = new Color(160, 80, 230, 230);

    private final Map<ShipAPI, TeleportData> dataByShip = new WeakHashMap<>();

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (!(stats.getEntity() instanceof ShipAPI ship)) return;
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) return;

        String modId = id + "_" + ship.getId();
        applyBuffs(stats, modId, effectLevel);

        TeleportData data = dataByShip.computeIfAbsent(ship, s -> new TeleportData());
        if (!data.sequenceStarted && effectLevel > 0f) {
            startSequence(ship, engine, data);
        }

        if (data.didTeleport && !data.returnResolved) {
            maintainRearPressure(ship, data.teleportTarget);
            data.elapsed += engine.getElapsedInLastFrame();
            if (data.elapsed >= RETURN_DELAY_SECONDS || (ship.getFluxTracker() != null && ship.getFluxTracker().getFluxLevel() >= RETURN_FLUX_LEVEL)) {
                executeReturn(ship, engine, data);
            }
        }

        if (ship == engine.getPlayerShip()) {
            String status = data.didTeleport ? "Rift jumped: +50% damage, +20% dissipation" : "No valid jump target";
            engine.maintainStatusForPlayerShip(modId, "graphics/icons/hullsys/targeting_feed.png", "Shroud Banshee", status, false);
        }
    }

    private void startSequence(ShipAPI ship, CombatEngineAPI engine, TeleportData data) {
        data.sequenceStarted = true;
        data.elapsed = 0f;
        data.returnResolved = false;
        data.didTeleport = false;
        float riftRadius = getBansheeRiftRadius(ship);

        ShipAPI brain = findBrainShip(ship, engine);
        if (brain != null) {
            Vector2f brainAnchor = getBrainAnchorPoint(brain, ship, engine);
            data.origin.set(brainAnchor.x, brainAnchor.y);
            spawnRift(engine, brainAnchor, riftRadius, RETURN_DELAY_SECONDS);
        } else {
            data.origin.set(ship.getLocation().x, ship.getLocation().y);
        }

        TeleportSolution solution = ShroudBansheeSystemAI.pickTeleportSolution(ship, engine, false);
        if (solution == null) {
            data.returnResolved = true;
            data.teleportTarget = null;
            return;
        }

        float dx = solution.target().getLocation().x - solution.destination().x;
        float dy = solution.target().getLocation().y - solution.destination().y;
        float facing = (float) Math.toDegrees(Math.atan2(dy, dx));

        ship.getLocation().set(solution.destination().x, solution.destination().y);
        ship.getVelocity().set(0f, 0f);
        ship.setFacing(facing);
        ship.setShipTarget(solution.target());

        data.teleportTarget = solution.target();
        data.didTeleport = true;

        spawnRift(engine, solution.destination(), riftRadius, 1.6f);
        if (brain != null) {
            spawnRift(engine, data.origin, riftRadius, 1.6f);
        }
    }

    private void executeReturn(ShipAPI ship, CombatEngineAPI engine, TeleportData data) {
        data.returnResolved = true;
        ShipAPI brain = findBrainShip(ship, engine);
        if (brain != null) {
            float riftRadius = getBansheeRiftRadius(ship);
            Vector2f returnPoint = getBrainAnchorPoint(brain, ship, engine);
            float facing = brain.getFacing();

            ShipAPI threat = null;
            float bestDist = Float.MAX_VALUE;
            for (ShipAPI other : engine.getShips()) {
                if (other == null || other == ship || !other.isAlive() || other.isHulk() || other.isFighter() || other.getOwner() == ship.getOwner()) continue;
                float dist = Misc.getDistance(returnPoint, other.getLocation());
                if (dist < bestDist) {
                    bestDist = dist;
                    threat = other;
                }
            }
            if (threat != null) {
                float dx = threat.getLocation().x - returnPoint.x;
                float dy = threat.getLocation().y - returnPoint.y;
                facing = (float) Math.toDegrees(Math.atan2(dy, dx));
            }

            ship.getLocation().set(returnPoint.x, returnPoint.y);
            ship.getVelocity().set(0f, 0f);
            ship.setFacing(facing);
            spawnRift(engine, returnPoint, riftRadius, 1.2f);
        }
        data.teleportTarget = null;
    }

    private void maintainRearPressure(ShipAPI ship, ShipAPI target) {
        if (ship == null || target == null || !target.isAlive() || target.isHulk() || target.getOwner() == ship.getOwner()) return;
        ship.setShipTarget(target);

        Vector2f desiredPoint = null;
        float radius = Math.max(ship.getCollisionRadius() + target.getCollisionRadius() + ShroudBansheeSystemAI.TELEPORT_OFFSET, 180f);

        for (float offset : new float[]{0f, 15f, -15f, 30f, -30f}) {
            Vector2f point = MathUtils.getPoint(target.getLocation(), radius, target.getFacing() + 180f + offset);
            if (!ShroudBansheeSystemAI.isRearPoint(target, point)) continue;
            desiredPoint = point;
            break;
        }
        if (desiredPoint == null) desiredPoint = MathUtils.getPoint(target.getLocation(), radius, target.getFacing() + 180f);

        Vector2f toPoint = Vector2f.sub(desiredPoint, ship.getLocation(), new Vector2f());
        float distance = toPoint.length();
        float moveAngle = Misc.getAngleInDegrees(ship.getLocation(), desiredPoint);

        float diff = Misc.normalizeAngle(moveAngle - ship.getFacing());
        float facingDelta = diff > 180f ? diff - 360f : diff;

        if (Math.abs(facingDelta) >= 7f) {
            ship.giveCommand(facingDelta > 0f ? ShipCommand.TURN_LEFT : ShipCommand.TURN_RIGHT, null, 0);
        }

        if (distance > REAR_HOLD_DISTANCE) {
            ship.giveCommand(Math.abs(facingDelta) <= 110f ? ShipCommand.ACCELERATE : ShipCommand.ACCELERATE_BACKWARDS, null, 0);
            if (distance > REAR_APPROACH_DISTANCE) {
                Vector2f right = Misc.getUnitVectorAtDegreeAngle(ship.getFacing() - 90f);
                float lateral = toPoint.x * right.x + toPoint.y * right.y;
                if (lateral > REAR_STRAFE_THRESHOLD) ship.giveCommand(ShipCommand.STRAFE_RIGHT, null, 0);
                else if (lateral < -REAR_STRAFE_THRESHOLD) ship.giveCommand(ShipCommand.STRAFE_LEFT, null, 0);
            }
        } else {
            ship.giveCommand(ShipCommand.DECELERATE, null, 0);
        }
    }

    private Vector2f getBrainAnchorPoint(ShipAPI brain, ShipAPI banshee, CombatEngineAPI engine) {
        float bansheeRadius = banshee != null ? banshee.getCollisionRadius() : 40f;
        float orbitRange = Math.max(80f, brain.getCollisionRadius() + bansheeRadius + BRAIN_RETURN_PADDING);
        float baseAngle = brain.getFacing() + 180f;

        ShipAPI threat = null;
        float bestDist = Float.MAX_VALUE;
        ShipAPI queryRoot = banshee != null ? banshee : brain;
        for (ShipAPI other : engine.getShips()) {
            if (other == null || other == queryRoot || !other.isAlive() || other.isHulk() || other.isFighter() || other.getOwner() == queryRoot.getOwner()) continue;
            float dist = Misc.getDistance(queryRoot.getLocation(), other.getLocation());
            if (dist < bestDist) {
                bestDist = dist;
                threat = other;
            }
        }
        if (threat != null) baseAngle = Misc.getAngleInDegrees(threat.getLocation(), brain.getLocation());

        int hash = (banshee == null || banshee.getId() == null) ? 0 : Math.abs(banshee.getId().hashCode());
        float spread = (hash % 61) - 30f;

        for (float offset : new float[]{spread, spread + 25f, spread - 25f, spread + 50f, spread - 50f, spread + 80f, spread - 80f}) {
            Vector2f point = MathUtils.getPoint(brain.getLocation(), orbitRange, baseAngle + offset);
            if (!ShroudBansheeSystemAI.isPointInsideMap(point, engine)) continue;

            boolean clear = true;
            for (ShipAPI other : engine.getShips()) {
                if (other == null || other == banshee || !other.isAlive() || other.isHulk() || other.isFighter()) continue;
                if (Misc.getDistance(point, other.getLocation()) < (bansheeRadius + other.getCollisionRadius() + BRAIN_RETURN_CLEARANCE)) {
                    clear = false;
                    break;
                }
            }
            if (clear) return point;
        }
        return MathUtils.getPoint(brain.getLocation(), orbitRange, baseAngle + spread);
    }

    private ShipAPI findBrainShip(ShipAPI ship, CombatEngineAPI engine) {
        for (ShipAPI ally : engine.getShips()) {
            if (ally == null || ally.getOwner() != ship.getOwner() || !ally.isAlive() || ally.isHulk() || ally.isFighter()) continue;
            boolean matches = (ally.getHullSpec() != null && "shrouded_brain".equals(ally.getHullSpec().getHullId())) ||
                    (ally.getVariant() != null && ally.getVariant().hasHullMod("shroud_brain"));
            if (!matches) continue;
            Object brainFlag = ally.getCustomData().get(KEY);
            if (brainFlag instanceof Boolean && !((Boolean) brainFlag)) continue;
            return ally;
        }
        return null;
    }

    private void applyBuffs(MutableShipStatsAPI stats, String modId, float effectLevel) {
        stats.getBallisticWeaponDamageMult().modifyPercent(modId, DAMAGE_BONUS_PERCENT * effectLevel);
        stats.getEnergyWeaponDamageMult().modifyPercent(modId, DAMAGE_BONUS_PERCENT * effectLevel);
        stats.getMissileWeaponDamageMult().modifyPercent(modId, DAMAGE_BONUS_PERCENT * effectLevel);
        stats.getFluxDissipation().modifyPercent(modId, DISSIPATION_BONUS_PERCENT * effectLevel);
        stats.getEnergyWeaponRangeBonus().modifyPercent(modId, RANGE_BONUS * effectLevel);
    }

    private void spawnRift(CombatEngineAPI engine, Vector2f location, float radius, float duration) {
        NegativeExplosionVisual.NEParams params = new NegativeExplosionVisual.NEParams();
        params.numRiftsToSpawn = 1;
        params.fadeIn = 0.2f;
        params.fadeOut = Math.max(0.4f, duration);
        params.radius = radius;
        params.thickness = radius * 0.45f;
        params.noiseMag = 1.2f;
        params.noiseMult = 1f;
        params.noisePeriod = 0.1f;
        params.color = ORIGIN_RIFT_COLOR;
        params.underglow = RiftCascadeEffect.EXPLOSION_UNDERCOLOR;

        CombatEntityAPI visual = engine.addLayeredRenderingPlugin(new NegativeExplosionVisual(params));
        if (visual != null) visual.getLocation().set(location.x, location.y);
    }

    private float getBansheeRiftRadius(ShipAPI ship) {
        if (ship == null) return 80f;
        return Math.max(80f, Math.min(220f, ship.getCollisionRadius() * 1.1f));
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        if (!(stats.getEntity() instanceof ShipAPI ship)) return;
        String modId = id + "_" + ship.getId();
        stats.getBallisticWeaponDamageMult().unmodify(modId);
        stats.getEnergyWeaponDamageMult().unmodify(modId);
        stats.getMissileWeaponDamageMult().unmodify(modId);
        stats.getFluxDissipation().unmodify(modId);
        stats.getEnergyWeaponRangeBonus().unmodify(modId);
        dataByShip.remove(ship);
    }

    private static class TeleportData {
        private final Vector2f origin = new Vector2f();
        private float elapsed = 0f;
        private boolean sequenceStarted = false;
        private boolean didTeleport = false;
        private boolean returnResolved = false;
        private ShipAPI teleportTarget = null;
    }
}