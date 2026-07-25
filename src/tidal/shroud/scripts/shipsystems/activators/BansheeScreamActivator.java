package tidal.shroud.scripts.shipsystems.activators;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual;
import com.fs.starfarer.api.impl.combat.RiftCascadeEffect;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.subsystems.MagicSubsystem;

import java.awt.Color;

public class BansheeScreamActivator extends MagicSubsystem {

    private static final float DAMAGE_BONUS_PERCENT = 20f;
    private static final float BLINK_DISTANCE = 1400f;
    private static final float TELEPORT_OFFSET = 160f;
    private static final float TELEPORT_CLEARANCE_PADDING = 35f;
    private static final float TELEPORT_MAP_MARGIN = 220f;
    private static final float RIFT_RADIUS_MULT = 1.1f;
    private static final float RIFT_MIN_RADIUS = 80f;
    private static final float RIFT_MAX_RADIUS = 220f;
    private static final float[] TELEPORT_ANGLE_OFFSETS = new float[]{
            0f, 20f, -20f, 45f, -45f, 70f, -70f, 100f, -100f, 135f, -135f, 180f
    };
    private static final Color RIFT_COLOR = new Color(160, 80, 230, 230);
    private static final String STAT_MOD_ID_PREFIX = "td_banshee_scream_subsystem";

    public BansheeScreamActivator(ShipAPI ship) {
        super(ship);
    }

    @Override
    public float getBaseInDuration() {
        return 0.2f;
    }

    @Override
    public float getBaseActiveDuration() {
        return 6f;
    }

    @Override
    public float getBaseOutDuration() {
        return 0.35f;
    }

    @Override
    public float getBaseCooldownDuration() {
        return 40f;
    }

    @Override
    protected float getRange() {
        return BLINK_DISTANCE * 1.8f;
    }

    @Override
    public boolean canActivate() {
        if (ship == null || !ship.isAlive() || ship.isHulk()) {
            return false;
        }
        return ship.getFluxTracker() == null || !ship.getFluxTracker().isOverloadedOrVenting();
    }

    @Override
    public boolean shouldActivateAI(float amount) {
        return false;
    }

    @Override
    public String getDisplayText() {
        return "The Banshee's Scream";
    }

    @Override
    public String getExtraInfoText() {
        return "Space-Time Breach ready";
    }

    @Override
    public void onActivate() {
        if (ship == null || !ship.isAlive() || ship.isHulk()) {
            return;
        }

        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) {
            return;
        }

        Vector2f origin = new Vector2f(ship.getLocation());
        ShipAPI target = pickPreferredTarget(engine);
        Vector2f destination = findTeleportDestination(engine, target);
        if (destination == null) {
            return;
        }

        float facing = ship.getFacing();
        if (isValidEnemy(target)) {
            facing = getAngleDegrees(destination, target.getLocation());
            ship.setShipTarget(target);
        }

        teleportTo(ship, destination, facing);

        float riftRadius = getRiftRadius(ship);
        spawnRift(engine, origin, riftRadius);
        spawnRift(engine, destination, riftRadius);
    }

    @Override
    public void advance(float amount, boolean isPaused) {
        if (ship == null) {
            return;
        }

        String modId = getStatModId(ship);
        if (!ship.isAlive() || ship.isHulk()) {
            clearDamageBonus(modId);
            return;
        }

        float effect = getEffectLevel();
        applyDamageBonus(modId, effect);

        CombatEngineAPI engine = Global.getCombatEngine();
        if (!isPaused && engine != null && ship == engine.getPlayerShip()) {
            engine.maintainStatusForPlayerShip(
                    modId,
                    "graphics/icons/hullsys/targeting_feed.png",
                    "The Banshee's Scream",
                    "Teleport active",
                    false
            );
        }

        if (isOff() && effect <= 0f) {
            clearDamageBonus(modId);
        }
    }

    @Override
    public void onFinished() {
        if (ship == null) {
            return;
        }
        clearDamageBonus(getStatModId(ship));
    }

    @Override
    public void onShipDeath() {
        if (ship == null) {
            return;
        }
        clearDamageBonus(getStatModId(ship));
    }

    private void applyDamageBonus(String id, float effectLevel) {
        float bonus = DAMAGE_BONUS_PERCENT * effectLevel;
        stats.getBallisticWeaponDamageMult().modifyPercent(id, bonus);
        stats.getEnergyWeaponDamageMult().modifyPercent(id, bonus);
        stats.getMissileWeaponDamageMult().modifyPercent(id, bonus);
        stats.getBeamWeaponDamageMult().modifyPercent(id, bonus);
    }

    private void clearDamageBonus(String id) {
        stats.getBallisticWeaponDamageMult().unmodify(id);
        stats.getEnergyWeaponDamageMult().unmodify(id);
        stats.getMissileWeaponDamageMult().unmodify(id);
        stats.getBeamWeaponDamageMult().unmodify(id);
    }

    private ShipAPI pickPreferredTarget(CombatEngineAPI engine) {
        ShipAPI explicit = ship.getShipTarget();
        if (isValidEnemy(explicit)) {
            return explicit;
        }

        ShipAPI best = null;
        float bestDist = Float.MAX_VALUE;
        for (ShipAPI other : engine.getShips()) {
            if (!isValidEnemy(other)) {
                continue;
            }
            float dist = Misc.getDistance(ship.getLocation(), other.getLocation());
            if (dist < bestDist) {
                bestDist = dist;
                best = other;
            }
        }
        return best;
    }

    private boolean isValidEnemy(ShipAPI target) {
        if (target == null || target == ship) {
            return false;
        }
        if (!target.isAlive() || target.isHulk() || target.isFighter()) {
            return false;
        }
        return target.getOwner() != ship.getOwner();
    }

    private Vector2f findTeleportDestination(CombatEngineAPI engine, ShipAPI target) {
        if (isValidEnemy(target)) {
            Vector2f byTarget = findClassicTargetTeleportDestination(target, engine);
            if (byTarget != null) {
                return byTarget;
            }
        }
        return findForwardBlinkDestination(engine);
    }

    private Vector2f findClassicTargetTeleportDestination(ShipAPI target, CombatEngineAPI engine) {
        float radius = Math.max(
                ship.getCollisionRadius() + target.getCollisionRadius() + TELEPORT_OFFSET,
                220f
        );
        float baseAngle = target.getFacing() + 180f;

        for (float offset : TELEPORT_ANGLE_OFFSETS) {
            Vector2f point = getPointOnCircle(target.getLocation(), radius, baseAngle + offset);
            if (isTeleportPointViable(point, engine, target)) {
                return point;
            }
        }
        return null;
    }

    private Vector2f findForwardBlinkDestination(CombatEngineAPI engine) {
        for (float offset : TELEPORT_ANGLE_OFFSETS) {
            Vector2f point = getPointOnCircle(ship.getLocation(), BLINK_DISTANCE, ship.getFacing() + offset);
            if (isTeleportPointViable(point, engine, null)) {
                return point;
            }
        }
        return null;
    }

    private Vector2f getPointOnCircle(Vector2f center, float radius, float angle) {
        Vector2f dir = Misc.getUnitVectorAtDegreeAngle(angle);
        dir.scale(radius);
        return Vector2f.add(center, dir, new Vector2f());
    }

    private boolean isTeleportPointViable(Vector2f point, CombatEngineAPI engine, ShipAPI ignored) {
        if (point == null || engine == null) {
            return false;
        }
        if (!isPointInsideMap(point, engine)) {
            return false;
        }

        for (ShipAPI other : engine.getShips()) {
            if (other == null || other == ship || other == ignored) {
                continue;
            }
            if (!other.isAlive() || other.isHulk() || other.isFighter()) {
                continue;
            }

            float clearance = ship.getCollisionRadius() + other.getCollisionRadius() + TELEPORT_CLEARANCE_PADDING;
            if (Misc.getDistance(point, other.getLocation()) < clearance) {
                return false;
            }
        }
        return true;
    }

    private boolean isPointInsideMap(Vector2f point, CombatEngineAPI engine) {
        float halfW = (engine.getMapWidth() * 0.5f) - TELEPORT_MAP_MARGIN;
        float halfH = (engine.getMapHeight() * 0.5f) - TELEPORT_MAP_MARGIN;
        if (halfW <= 0f || halfH <= 0f) {
            return true;
        }
        return Math.abs(point.x) <= halfW && Math.abs(point.y) <= halfH;
    }

    private float getRiftRadius(ShipAPI ship) {
        float radius = ship.getCollisionRadius() * RIFT_RADIUS_MULT;
        return Math.max(RIFT_MIN_RADIUS, Math.min(RIFT_MAX_RADIUS, radius));
    }

    private void teleportTo(ShipAPI ship, Vector2f location, float facing) {
        ship.getLocation().set(location.x, location.y);
        ship.getVelocity().set(0f, 0f);
        ship.setFacing(facing);
    }

    private float getAngleDegrees(Vector2f from, Vector2f to) {
        float dx = to.x - from.x;
        float dy = to.y - from.y;
        return (float) Math.toDegrees(Math.atan2(dy, dx));
    }

    private void spawnRift(CombatEngineAPI engine, Vector2f location, float radius) {
        NegativeExplosionVisual.NEParams params = new NegativeExplosionVisual.NEParams();
        params.numRiftsToSpawn = 1;
        params.fadeIn = 0.2f;
        params.fadeOut = 1.2f;
        params.radius = radius;
        params.thickness = radius * 0.45f;
        params.noiseMag = 1.2f;
        params.noiseMult = 1f;
        params.noisePeriod = 0.1f;
        params.withHitGlow = false;
        params.withNegativeParticles = false;
        params.color = RIFT_COLOR;
        params.underglow = RiftCascadeEffect.EXPLOSION_UNDERCOLOR;

        CombatEntityAPI visual = engine.addLayeredRenderingPlugin(new NegativeExplosionVisual(params));
        if (visual != null) {
            visual.getLocation().set(location.x, location.y);
        }
    }

    private String getStatModId(ShipAPI ship) {
        String memberId = ship.getFleetMemberId();
        if (memberId == null || memberId.isEmpty()) {
            memberId = Integer.toString(ship.hashCode());
        }
        return STAT_MOD_ID_PREFIX + "_" + memberId;
    }
}
