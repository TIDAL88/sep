package tidal.shroud.scripts.ships;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAIConfig;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

public class ShroudedBrainGuardianAI implements ShipAIPlugin {

    private static final float BACKLINE_OFFSET = 1800f;
    private static final float MAX_ANCHOR_OFFSET = 2400f;
    private static final float HOLD_DISTANCE = 900f;
    private static final float FRONTLINE_SEARCH_RANGE = 12000f;
    private static final float ENEMY_SEARCH_RANGE = 10000f;
    private static final float MIN_ENEMY_STANDOFF = 3200f;
    private static final float MAP_MARGIN = 280f;
    private static final float TURN_THRESHOLD = 7f;
    private static final float STRAFE_THRESHOLD = 45f;

    private final ShipAPI ship;
    private final ShipwideAIFlags flags;
    private final ShipAIConfig config;
    private ShipAPI targetOverride;

    public ShroudedBrainGuardianAI(ShipAPI ship) {
        this.ship = ship;
        this.config = new ShipAIConfig();
        this.config.personalityOverride = "timid";
        this.config.alwaysStrafeOffensively = false;
        this.config.backingOffWhileNotVentingAllowed = false;
        this.flags = new ShipwideAIFlags();
    }

    @Override
    public void setDoNotFireDelay(float amount) {
    }

    @Override
    public void forceCircumstanceEvaluation() {
    }

    @Override
    public void advance(float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused() || ship == null || !ship.isAlive() || ship.isHulk()) {
            return;
        }

        keepShieldsUp();
        flags.advance(amount);

        Vector2f frontline = findAlliedFrontlineCenter(engine);
        Vector2f reference = frontline != null ? frontline : ship.getLocation();
        ShipAPI threat = findClosestEnemy(engine, reference);
        Vector2f desiredPoint = computeBacklinePoint(frontline, threat, engine);
        moveToward(desiredPoint);

        flags.setFlag(ShipwideAIFlags.AIFlags.KEEP_SHIELDS_ON, 400f);
        flags.setFlag(ShipwideAIFlags.AIFlags.DO_NOT_VENT, 0.50f);
        flags.setFlag(ShipwideAIFlags.AIFlags.DO_NOT_BACK_OFF, 0.5f);

        if (threat != null) {
            ship.setShipTarget(threat);
        }
    }

    private void keepShieldsUp() {
        flags.setFlag(ShipwideAIFlags.AIFlags.KEEP_SHIELDS_ON, 400f);
        flags.setFlag(ShipwideAIFlags.AIFlags.DO_NOT_VENT, 0.25f);
        ship.blockCommandForOneFrame(ShipCommand.VENT_FLUX);

        ShieldAPI shield = ship.getShield();
        if (shield != null && shield.isOff()) {
            shield.toggleOn();
        }
    }

    private Vector2f findAlliedFrontlineCenter(CombatEngineAPI engine) {
        Vector2f sum = new Vector2f();
        int count = 0;

        for (ShipAPI other : engine.getShips()) {
            if (other == null || other == ship) {
                continue;
            }
            if (!other.isAlive() || other.isHulk() || other.isFighter() || other.isDrone() || other.isShuttlePod()) {
                continue;
            }
            if (other.getOwner() != ship.getOwner()) {
                continue;
            }
            if (isBrainShip(other)) {
                continue;
            }
            if (Misc.getDistance(ship.getLocation(), other.getLocation()) > FRONTLINE_SEARCH_RANGE) {
                continue;
            }

            sum.x += other.getLocation().x;
            sum.y += other.getLocation().y;
            count++;
        }

        if (count <= 0) {
            return null;
        }
        sum.scale(1f / count);
        return sum;
    }

    private Vector2f computeBacklinePoint(Vector2f frontline, ShipAPI threat, CombatEngineAPI engine) {
        Vector2f anchor = frontline != null ? new Vector2f(frontline) : new Vector2f(ship.getLocation());


        if (threat == null) {
            return clampToMap(anchor, engine);
        }

        Vector2f rearDirection;

        rearDirection = Vector2f.sub(anchor, threat.getLocation(), new Vector2f());
        if (rearDirection.lengthSquared() < 1f) {
            rearDirection = Misc.getUnitVectorAtDegreeAngle(ship.getFacing() + 180f);
        } else {
            rearDirection.normalise();
        }

        float offset = BACKLINE_OFFSET + ship.getCollisionRadius();
        float standoff = Misc.getDistance(anchor, threat.getLocation());
        if (standoff < MIN_ENEMY_STANDOFF) {
            offset += (MIN_ENEMY_STANDOFF - standoff);
        }
        offset = Math.min(offset, MAX_ANCHOR_OFFSET);

        rearDirection.scale(offset);
        Vector2f desired = Vector2f.add(anchor, rearDirection, new Vector2f());

        if (frontline != null) {
            Vector2f fromFrontline = Vector2f.sub(desired, frontline, new Vector2f());
            float dist = fromFrontline.length();
            if (dist > MAX_ANCHOR_OFFSET) {
                fromFrontline.scale(MAX_ANCHOR_OFFSET / dist);
                desired = Vector2f.add(frontline, fromFrontline, new Vector2f());
            }
        }

        return clampToMap(desired, engine);
    }

    private Vector2f clampToMap(Vector2f point, CombatEngineAPI engine) {
        if (point == null || engine == null) {
            return point;
        }
        float halfW = (engine.getMapWidth() * 0.5f) - MAP_MARGIN;
        float halfH = (engine.getMapHeight() * 0.5f) - MAP_MARGIN;
        if (halfW <= 0f || halfH <= 0f) {
            return point;
        }
        point.x = Math.max(-halfW, Math.min(halfW, point.x));
        point.y = Math.max(-halfH, Math.min(halfH, point.y));
        return point;
    }

    private ShipAPI findClosestEnemy(CombatEngineAPI engine, Vector2f around) {
        ShipAPI best = null;
        float bestDist = ShroudedBrainGuardianAI.ENEMY_SEARCH_RANGE;

        for (ShipAPI other : engine.getShips()) {
            if (other == null || !other.isAlive() || other.isHulk()) {
                continue;
            }
            if (other.getOwner() == ship.getOwner()) {
                continue;
            }
            float distance = Misc.getDistance(around, other.getLocation());
            if (distance < bestDist) {
                bestDist = distance;
                best = other;
            }
        }

        if (targetOverride != null && targetOverride.isAlive() && !targetOverride.isHulk()
                && targetOverride.getOwner() != ship.getOwner()) {
            return targetOverride;
        }
        return best;
    }

    private boolean isBrainShip(ShipAPI other) {
        if (other == null || other.getHullSpec() == null) {
            return false;
        }
        String hullId = other.getHullSpec().getHullId();
        if ("shrouded_brain".equals(hullId)) {
            return true;
        }
        return other.getVariant() != null && other.getVariant().hasHullMod("shroud_brain");
    }

    private void moveToward(Vector2f point) {
        if (point == null) {
            return;
        }

        Vector2f toPoint = Vector2f.sub(point, ship.getLocation(), new Vector2f());
        float distance = toPoint.length();
        float moveAngle = Misc.getAngleInDegrees(ship.getLocation(), point);
        float facingDelta = getShortestRotation(ship.getFacing(), moveAngle);

        turnToward(moveAngle);

        if (distance > HOLD_DISTANCE) {
            if (Math.abs(facingDelta) <= 110f) {
                ship.giveCommand(ShipCommand.ACCELERATE, null, 0);
            } else {
                ship.giveCommand(ShipCommand.ACCELERATE_BACKWARDS, null, 0);
            }

            Vector2f right = Misc.getUnitVectorAtDegreeAngle(ship.getFacing() - 90f);
            float lateral = dot(toPoint, right);
            if (lateral > STRAFE_THRESHOLD) {
                ship.giveCommand(ShipCommand.STRAFE_RIGHT, null, 0);
            } else if (lateral < -STRAFE_THRESHOLD) {
                ship.giveCommand(ShipCommand.STRAFE_LEFT, null, 0);
            }
        } else {
            ship.giveCommand(ShipCommand.DECELERATE, null, 0);
        }
    }

    private void turnToward(float angle) {
        float delta = getShortestRotation(ship.getFacing(), angle);
        if (Math.abs(delta) < TURN_THRESHOLD) {
            return;
        }
        if (delta > 0f) {
            ship.giveCommand(ShipCommand.TURN_LEFT, null, 0);
        } else {
            ship.giveCommand(ShipCommand.TURN_RIGHT, null, 0);
        }
    }

    private float getShortestRotation(float from, float to) {
        float diff = Misc.normalizeAngle(to - from);
        if (diff > 180f) {
            diff -= 360f;
        }
        return diff;
    }

    private float dot(Vector2f a, Vector2f b) {
        return a.x * b.x + a.y * b.y;
    }

    @Override
    public boolean needsRefit() {
        return false;
    }

    @Override
    public ShipwideAIFlags getAIFlags() {
        return flags;
    }

    @Override
    public void cancelCurrentManeuver() {
    }

    @Override
    public ShipAIConfig getConfig() {
        return config;
    }

    @Override
    public void setTargetOverride(ShipAPI target) {
        this.targetOverride = target;
    }
}

