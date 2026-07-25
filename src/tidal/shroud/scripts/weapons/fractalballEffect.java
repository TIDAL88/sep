package tidal.shroud.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual;
import com.fs.starfarer.api.impl.combat.RiftCascadeEffect;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.lwjgl.util.vector.Vector2f;
//this is so messy and complicated, but it is how you get this really cool effect, copy it entirely if you want to get the same effect, make sure to credit me
public class fractalballEffect implements OnHitEffectPlugin {
    public static final String RIFT_SPAWNED_TAG = "fractal_rift_spawned";
    private static final String RIFT_SPAWNED_KEY_PREFIX = "fractal_rift_spawned_proj_";
    private static final String BOSS_PROJECTILE_ID = "fractal_ball_boss";
    private static final int DEFAULT_RIFT_END_SUMMON_OWNER = 1;

    private static final float RIFT_DURATION_SECONDS = 15f;
    private static final float BOSS_RIFT_DURATION_SECONDS = 10f;
    private static final float RIFT_RADIUS = 1600f;
    private static final float BOSS_RIFT_RADIUS = 1200f;
    private static final float RIFT_ARC_INTERVAL = 0.75f;
    private static final float RIFT_ARC_DAMAGE = 500f;
    private static final float BOSS_RIFT_ARC_DAMAGE = 900f;
    private static final float RIFT_ARC_EMP = 1000f;
    private static final float BOSS_RIFT_ARC_EMP = 1500f;
    private static final float RIFT_ARC_THICKNESS = 40f;
    private static final float RIFT_ARC_ORIGIN_RADIUS = 90f;

    private static final float RIFT_VISUAL_RADIUS = 600f;
    private static final float BOSS_RIFT_VISUAL_RADIUS = 420f;
    private static final float RIFT_VISUAL_THICKNESS = 260f;
    private static final float BOSS_RIFT_VISUAL_THICKNESS = 200f;
    private static final float RIFT_CLOSE_VISUAL_RADIUS = 320f;
    private static final float BOSS_RIFT_CLOSE_VISUAL_RADIUS = 230f;
    private static final float RIFT_CLOSE_VISUAL_THICKNESS = 340f;
    private static final float BOSS_RIFT_CLOSE_VISUAL_THICKNESS = 230f;
    private static final String RIFT_CLOSE_SOUND_ID = "fractal_rift_closure";
    private static final float RIFT_CLOSE_SOUND_DURATION_SECONDS = 8f;
    private static final float RIFT_MARKER_CORE_SIZE = 340f;
    private static final float BOSS_RIFT_MARKER_CORE_SIZE = 260f;
    private static final float RIFT_MARKER_GLOW_SIZE = 860f;
    private static final float BOSS_RIFT_MARKER_GLOW_SIZE = 620f;

    private static final float RIFT_IMPLOSION_RADIUS = 1300f;
    private static final float BOSS_RIFT_IMPLOSION_RADIUS = 800f;
    private static final float RIFT_IMPLOSION_DAMAGE = 10000f;
    private static final float BOSS_RIFT_IMPLOSION_DAMAGE = 10000f;
    private static final float RIFT_IMPACT_DAMAGE = 10000f;
    private static final float BOSS_RIFT_IMPACT_DAMAGE = 10000f;
    private static final float RIFT_IMPACT_RADIUS = 1000f;
    private static final float BOSS_RIFT_IMPACT_RADIUS = 700f;
    private static final float RIFT_END_SPAWN_BASE_CHANCE = 0.50f;
    private static final float RIFT_END_SPAWN_FAIL_BONUS_STEP = 0.50f;
    private static final String RIFT_END_SPAWN_BONUS_KEY = "fractal_rift_end_spawn_bonus";
    private static final String RIFT_END_SUMMON_TAG = "fractal_rift_end_summon";
    private static final String RIFT_THREAT_LEVEL_KEY = "fractal_rift_threat_level";
    private static final int RIFT_THREAT_MIN = 1;
    private static final int RIFT_THREAT_MAX = 3;
    private static final float RIFT_PAIR_SPLIT_CHANCE = 0.5f;
    private static final int no_spawn_limit = 1;
    private static final SpawnGroup[] RIFT_THREAT_LOW_GROUPS = new SpawnGroup[] {
            new SpawnGroup("shrouded_banshee_tenebrous", 3),
            new SpawnGroup("shrouded_tendril_Roiling", 3)
    };//a 50/50 to get either 2 eyes or maelstroms you dont need a group for the addshiporwing method
    private static final SpawnGroup[] RIFT_THREAT_MEDIUM_GROUPS = new SpawnGroup[] {
            new SpawnGroup("shrouded_eye_Darkened", 2),
            new SpawnGroup("shrouded_maelstrom_Menacing", 2)
    };//this is the highest threat group spawning a maw
    private static final SpawnGroup RIFT_THREAT_HIGH_GROUP = new SpawnGroup("shrouded_maw_Ravenous", 1);
//colors for the rift
    private static final Color RIFT_COLOR = new Color(255, 60, 90, 255);
    private static final Color RIFT_CORE_COLOR = new Color(255, 255, 255, 255);
    private static final Color RIFT_PARTICLE_COLOR = new Color(200, 30, 45, 170);
    private static final Color RIFT_MARKER_CORE_COLOR = new Color(255, 120, 150, 230);
    private static final Color RIFT_MARKER_GLOW_COLOR = new Color(180, 40, 60, 140);
    private static final Color RIFT_CLOSE_COLOR = new Color(255, 140, 175, 255);
    private static final Color RIFT_CLOSE_PARTICLE_COLOR = new Color(220, 70, 95, 190);

    private static final FractalRiftEffect RIFT_VISUAL_EFFECT = new FractalRiftEffect();
    private static final Random RANDOM = new Random();
    //spawn group class definition
    private static class SpawnGroup {
        private final String variantId;
        private final int count;

        private SpawnGroup(String variantId, int count) {
            this.variantId = variantId;
            this.count = count;
        }
    }
//on hit effect this is the complicated part
    @Override
    public void onHit(
            DamagingProjectileAPI projectile,
            CombatEntityAPI target,
            Vector2f point,
            boolean shieldHit,
            ApplyDamageResultAPI damageResult,
            CombatEngineAPI engine
    ) {
        spawnRift(engine, projectile, target, point, shieldHit, damageResult);
    }
    // a bunch of different api stuff to spawn it
    public static void spawnRift(
            CombatEngineAPI engine,
            DamagingProjectileAPI projectile,
            CombatEntityAPI target,
            Vector2f point,
            boolean shieldHit,
            ApplyDamageResultAPI damageResult
    ) {
        if (engine == null || projectile == null) {
            return;
        }
        String spawnedKey = RIFT_SPAWNED_KEY_PREFIX + projectile.hashCode();
        if (Boolean.TRUE.equals(engine.getCustomData().get(spawnedKey))) {
            return;
        }
        if (projectile instanceof MissileAPI missile && missile.hasTag(RIFT_SPAWNED_TAG)) {
            return;
        }

        Vector2f spawnPoint = point != null ? new Vector2f(point) : new Vector2f(projectile.getLocation());
        boolean bossProjectile = isBossProjectile(projectile);
        ShipAPI sourceShip = projectile.getSource();
        int sourceOwner = sourceShip != null ? sourceShip.getOwner() : projectile.getOwner();
        int summonOwner = getSummonOwner(projectile, bossProjectile);
        RIFT_VISUAL_EFFECT.onHit(projectile, target, spawnPoint, shieldHit, damageResult, engine);
        applyImpactDamage(engine, spawnPoint, bossProjectile, sourceOwner, sourceShip);
        spawnPersistentRiftVisual(engine, spawnPoint, bossProjectile);
        engine.addPlugin(new RiftField(spawnPoint, summonOwner, sourceOwner, sourceShip, bossProjectile));

        if (projectile instanceof MissileAPI missile) {
            missile.addTag(RIFT_SPAWNED_TAG);
        }
        engine.getCustomData().put(spawnedKey, true);
    }
    //persistent visual method
    private static void spawnPersistentRiftVisual(CombatEngineAPI engine, Vector2f center, boolean bossProjectile) {
        if (engine == null || center == null) {
            return;
        }
        //visual effects and control the amount of rifts that can spawn
        NegativeExplosionVisual.NEParams params = new NegativeExplosionVisual.NEParams();
        params.numRiftsToSpawn = 1;
        params.fadeIn = 0.15f;
        params.fadeOut = getRiftDurationSeconds(bossProjectile);
        params.radius = getRiftVisualRadius(bossProjectile);
        params.thickness = getRiftVisualThickness(bossProjectile);
        params.noiseMag = 1.2f;
        params.noiseMult = 1f;
        params.noisePeriod = 0.1f;
        params.withHitGlow = false;
        params.withNegativeParticles = false;
        params.color = RIFT_COLOR;
        params.underglow = RiftCascadeEffect.EXPLOSION_UNDERCOLOR;

        CombatEntityAPI visual = engine.addLayeredRenderingPlugin(new NegativeExplosionVisual(params));
        if (visual != null) {
            visual.getLocation().set(center);
        }

        FractalRiftEffect.playShockwaveSound(center, new Vector2f(), 1.15f, 0.92f);
    }
    //this is for the impact damage because the weapon does 0 damage in the csv
    private static void applyImpactDamage(
            CombatEngineAPI engine,
            Vector2f center,
            boolean bossProjectile,
            int sourceOwner,
            ShipAPI sourceShip
    ) {
        if (engine == null || center == null) {
            return;
        }//this is the entity api you need this to generate the damage using engine.applyDamage
        List<CombatEntityAPI> targets = collectTargetsInRadius(engine, center, getImpactRadius(bossProjectile));
        for (CombatEntityAPI target : targets) {
            if (bossProjectile && !shouldDamageTarget(target, sourceOwner, sourceShip)) {
                continue;
            }
            Vector2f hitPoint = getShieldAwareTargetPoint(center, target);
            engine.applyDamage(
                    target,
                    hitPoint,
                    getImpactDamage(bossProjectile),
                    DamageType.ENERGY,
                    0f,
                    false,
                    false,
                    null,
                    false
            );
        }
    }

    private static List<CombatEntityAPI> collectTargetsInRadius(CombatEngineAPI engine, Vector2f center, float radius) {
        Set<CombatEntityAPI> targets = new HashSet<>();

        Iterator<Object> shipIter = engine.getShipGrid().getCheckIterator(center, radius * 2f, radius * 2f);
        while (shipIter.hasNext()) {
            Object obj = shipIter.next();
            if (!(obj instanceof ShipAPI ship)) {
                continue;
            }
            if (!ship.isAlive() || ship.isHulk()) {
                continue;
            }
            if (Misc.getDistance(center, ship.getLocation()) > radius) {
                continue;
            }
            targets.add(ship);
        }

        Iterator<Object> missileIter = engine.getMissileGrid().getCheckIterator(center, radius * 2f, radius * 2f);
        while (missileIter.hasNext()) {
            Object obj = missileIter.next();
            if (!(obj instanceof MissileAPI missile)) {
                continue;
            }
            if (missile.isExpired() || missile.wasRemoved() || missile.isFading()) {
                continue;
            }
            if (Misc.getDistance(center, missile.getLocation()) > radius) {
                continue;
            }
            targets.add(missile);
        }

        return new ArrayList<>(targets);
    }

    private static boolean shouldDamageTarget(CombatEntityAPI target, int sourceOwner, ShipAPI sourceShip) {
        if (target == null) {
            return false;
        }
        if (sourceShip != null && target == sourceShip) {
            return false;
        }

        Integer targetOwner = getEntityOwner(target);
        if (targetOwner != null && targetOwner == sourceOwner) {
            return false;
        }

        if (sourceShip != null && target instanceof ShipAPI targetShip) {
            ShipAPI sourceRoot = getRootShip(sourceShip);
            ShipAPI targetRoot = getRootShip(targetShip);
            if (sourceRoot != null && sourceRoot == targetRoot) {
                return false;
            }
        }

        return true;
    }

    private static boolean shouldArcBossTarget(CombatEntityAPI target, int sourceOwner, ShipAPI sourceShip) {
        if (!(target instanceof ShipAPI)) {
            return false;
        }
        return shouldDamageTarget(target, sourceOwner, sourceShip);
    }

    private static Integer getEntityOwner(CombatEntityAPI entity) {
        if (entity instanceof ShipAPI ship) {
            return ship.getOwner();
        }
        if (entity instanceof MissileAPI missile) {
            return missile.getOwner();
        }
        return null;
    }

    private static ShipAPI getRootShip(ShipAPI ship) {
        if (ship == null) {
            return null;
        }
        ShipAPI current = ship;
        while (current != null && current.isStationModule() && current.getParentStation() != null) {
            current = current.getParentStation();
        }
        return current;
    }

    private static Vector2f getShieldAwareTargetPoint(Vector2f origin, CombatEntityAPI target) {
        if (target == null) {
            return new Vector2f(origin);
        }
        if (target instanceof ShipAPI ship) {
            ShieldAPI shield = ship.getShield();
            if (shield != null && shield.isOn()) {
                Vector2f shieldCenter = shield.getLocation();
                Vector2f dir = Vector2f.sub(origin, shieldCenter, new Vector2f());
                if (dir.lengthSquared() < 1f) {
                    dir = Misc.getUnitVectorAtDegreeAngle(RANDOM.nextFloat() * 360f);
                } else {
                    dir.normalise();
                }

                float r = Math.max(10f, shield.getRadius() - 5f);
                Vector2f shieldPoint = new Vector2f(
                        shieldCenter.x + dir.x * r,
                        shieldCenter.y + dir.y * r
                );
                if (shield.isWithinArc(shieldPoint)) {
                    return shieldPoint;
                }
            }
        }

        float radius = Math.max(10f, target.getCollisionRadius() * 0.35f);
        return Misc.getPointWithinRadius(target.getLocation(), radius);
    }

    private static boolean isBossProjectile(DamagingProjectileAPI projectile) {
        if (projectile == null || projectile.getProjectileSpec() == null) {
            return false;
        }
        return BOSS_PROJECTILE_ID.equals(projectile.getProjectileSpec().getId());
    }

    private static int getSummonOwner(DamagingProjectileAPI projectile, boolean bossProjectile) {
        if (!bossProjectile || projectile == null || projectile.getSource() == null) {
            return DEFAULT_RIFT_END_SUMMON_OWNER;
        }
        return projectile.getSource().getOwner();
    }

    private static float getRiftDurationSeconds(boolean bossProjectile) {
        return bossProjectile ? BOSS_RIFT_DURATION_SECONDS : RIFT_DURATION_SECONDS;
    }

    private static float getRiftRadius(boolean bossProjectile) {
        return bossProjectile ? BOSS_RIFT_RADIUS : RIFT_RADIUS;
    }

    private static float getArcDamage(boolean bossProjectile) {
        return bossProjectile ? BOSS_RIFT_ARC_DAMAGE : RIFT_ARC_DAMAGE;
    }

    private static float getArcEmp(boolean bossProjectile) {
        return bossProjectile ? BOSS_RIFT_ARC_EMP : RIFT_ARC_EMP;
    }

    private static float getRiftVisualRadius(boolean bossProjectile) {
        return bossProjectile ? BOSS_RIFT_VISUAL_RADIUS : RIFT_VISUAL_RADIUS;
    }

    private static float getRiftVisualThickness(boolean bossProjectile) {
        return bossProjectile ? BOSS_RIFT_VISUAL_THICKNESS : RIFT_VISUAL_THICKNESS;
    }

    private static float getRiftCloseVisualRadius(boolean bossProjectile) {
        return bossProjectile ? BOSS_RIFT_CLOSE_VISUAL_RADIUS : RIFT_CLOSE_VISUAL_RADIUS;
    }

    private static float getRiftCloseVisualThickness(boolean bossProjectile) {
        return bossProjectile ? BOSS_RIFT_CLOSE_VISUAL_THICKNESS : RIFT_CLOSE_VISUAL_THICKNESS;
    }

    private static float getMarkerCoreSize(boolean bossProjectile) {
        return bossProjectile ? BOSS_RIFT_MARKER_CORE_SIZE : RIFT_MARKER_CORE_SIZE;
    }

    private static float getMarkerGlowSize(boolean bossProjectile) {
        return bossProjectile ? BOSS_RIFT_MARKER_GLOW_SIZE : RIFT_MARKER_GLOW_SIZE;
    }

    private static float getImplosionRadius(boolean bossProjectile) {
        return bossProjectile ? BOSS_RIFT_IMPLOSION_RADIUS : RIFT_IMPLOSION_RADIUS;
    }

    private static float getImplosionDamage(boolean bossProjectile) {
        return bossProjectile ? BOSS_RIFT_IMPLOSION_DAMAGE : RIFT_IMPLOSION_DAMAGE;
    }

    private static float getImpactDamage(boolean bossProjectile) {
        return bossProjectile ? BOSS_RIFT_IMPACT_DAMAGE : RIFT_IMPACT_DAMAGE;
    }

    private static float getImpactRadius(boolean bossProjectile) {
        return bossProjectile ? BOSS_RIFT_IMPACT_RADIUS : RIFT_IMPACT_RADIUS;
    }
    //rift arcs the arc interval being a value that tells you the interval before a new arc is spawned
    private static class RiftField extends BaseEveryFrameCombatPlugin {
        private final Vector2f center;
        private final int summonOwner;
        private final int damageOwner;
        private final ShipAPI damageSource;
        private final boolean bossProjectile;
        private final Vector2f soundVelocity = new Vector2f();
        private final IntervalUtil arcInterval = new IntervalUtil(RIFT_ARC_INTERVAL, RIFT_ARC_INTERVAL);
        private final IntervalUtil visualInterval = new IntervalUtil(0.18f, 0.22f);
        private final IntervalUtil markerInterval = new IntervalUtil(0.07f, 0.10f);
        private float elapsed = 0f;
        //this is to make the rifts spawn
        private RiftField(Vector2f center, int summonOwner, int damageOwner, ShipAPI damageSource, boolean bossProjectile) {
            this.center = new Vector2f(center);
            this.summonOwner = summonOwner;
            this.damageOwner = damageOwner;
            this.damageSource = damageSource;
            this.bossProjectile = bossProjectile;
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null || engine.isPaused()) {
                return;
            }
            //audio
            Global.getSoundPlayer().playLoop(
                    FractalRiftEffect.FRACTAL_RIFT_SHOCKWAVE_SOUND,
                    this,
                    1f,
                    1f,
                    center,
                    soundVelocity
            );
            //this is for the duration
            elapsed += amount;
            if (elapsed >= getRiftDurationSeconds(bossProjectile)) {
                spawnClosingImplosion(engine);
                engine.removePlugin(this);
                return;
            }

            visualInterval.advance(amount);
            if (visualInterval.intervalElapsed()) {
                spawnRiftParticles(engine);
            }

            markerInterval.advance(amount);
            if (markerInterval.intervalElapsed()) {
                spawnRiftMarker(engine);
            }

            arcInterval.advance(amount);
            if (!arcInterval.intervalElapsed()) {
                return;
            }

            List<CombatEntityAPI> targets = collectTargetsInRadius(engine, getRiftRadius(bossProjectile));
            for (CombatEntityAPI target : targets) {
                if (bossProjectile && !shouldArcBossTarget(target, damageOwner, damageSource)) {
                    continue;
                }
                Vector2f from = Misc.getPointWithinRadius(center, RIFT_ARC_ORIGIN_RADIUS);
                Vector2f to = getShieldAwareTargetPoint(target);

                engine.spawnEmpArcVisual(
                        from,
                        null,
                        to,
                        target,
                        RIFT_ARC_THICKNESS,
                        RIFT_COLOR,
                        RIFT_CORE_COLOR
                );

                // Apply damage directly so arcs always deal damage; boss projectile skips allied targets so it only hit player ships and not dwellers.
                engine.applyDamage(
                        target,
                        to,
                        getArcDamage(bossProjectile),
                        DamageType.ENERGY,
                        getArcEmp(bossProjectile),
                        false,
                        false,
                        null,
                        false
                );
            }
        }

        private void spawnRiftParticles(CombatEngineAPI engine) {
            Vector2f loc = Misc.getPointWithinRadius(center, getRiftRadius(bossProjectile) * 0.50f);
            float remaining = Math.max(1f, (getRiftDurationSeconds(bossProjectile) - elapsed) + 0.1f);
            engine.addNegativeNebulaParticle(
                    loc,
                    new Vector2f(),
                    190f + RANDOM.nextFloat() * 130f,
                    1.15f,
                    0.15f,
                    0.9f,
                    remaining,
                    RIFT_PARTICLE_COLOR
            );
        }

        private void spawnRiftMarker(CombatEngineAPI engine) {
            float pulse = 0.7f + 0.3f * (float) Math.sin(elapsed * 7f);
            engine.addHitParticle(
                    center,
                    new Vector2f(),
                    getMarkerGlowSize(bossProjectile),
                    1.1f * pulse,
                    0.30f,
                    RIFT_MARKER_GLOW_COLOR
            );
            engine.addHitParticle(
                    center,
                    new Vector2f(),
                    getMarkerCoreSize(bossProjectile),
                    1.6f * pulse,
                    0.24f,
                    RIFT_MARKER_CORE_COLOR
            );
        }
        //any rift needs a spectacular implosion effect this is how you do it using negative explosion visuals
        private void spawnClosingImplosion(CombatEngineAPI engine) {
            NegativeExplosionVisual.NEParams params = new NegativeExplosionVisual.NEParams();
            params.numRiftsToSpawn = 1;
            params.fadeIn = 0.05f;
            params.fadeOut = 0.75f;
            params.radius = getRiftCloseVisualRadius(bossProjectile);
            params.thickness = getRiftCloseVisualThickness(bossProjectile);
            params.noiseMag = 1.6f;
            params.noiseMult = 1.2f;
            params.noisePeriod = 0.08f;
            params.withHitGlow = true;
            params.hitGlowSizeMult = 2.8f;
            params.withNegativeParticles = false;
            params.color = RIFT_CLOSE_COLOR;
            params.underglow = RiftCascadeEffect.EXPLOSION_UNDERCOLOR;

            CombatEntityAPI visual = engine.addLayeredRenderingPlugin(new NegativeExplosionVisual(params));
            if (visual != null) {
                visual.getLocation().set(center);
            }

            for (int i = 0; i < 28; i++) {
                Vector2f loc = Misc.getPointWithinRadius(center, getRiftRadius(bossProjectile) * 0.65f);
                Vector2f vel = Vector2f.sub(center, loc, new Vector2f());
                if (vel.lengthSquared() < 1f) {
                    vel = Misc.getUnitVectorAtDegreeAngle(RANDOM.nextFloat() * 360f);
                } else {
                    vel.normalise();
                }
                vel.scale(160f + RANDOM.nextFloat() * 260f);

                engine.addNegativeNebulaParticle(
                        loc,
                        vel,
                        95f + RANDOM.nextFloat() * 65f,
                        1.25f,
                        0.1f,
                        0.95f,
                        0.7f + RANDOM.nextFloat() * 0.35f,
                        RIFT_CLOSE_PARTICLE_COLOR
                );
            }

            startClosingSoundLoop(engine);
            applyImplosionDamage(engine);
            spawnEndSummons(engine);
        }

        private void startClosingSoundLoop(CombatEngineAPI engine) {
            if (engine == null) {
                return;
            }
            engine.addPlugin(new ClosingSoundLoop(center));
        }
        //this is for the damage at the end when the rift implodes same as the method at the top
        private void applyImplosionDamage(CombatEngineAPI engine) {
            List<CombatEntityAPI> targets = collectTargetsInRadius(engine, getImplosionRadius(bossProjectile));
            for (CombatEntityAPI target : targets) {
                if (bossProjectile && !shouldDamageTarget(target, damageOwner, damageSource)) {
                    continue;
                }
                Vector2f point = getShieldAwareTargetPoint(target);
                engine.applyDamage(
                        target,
                        point,
                        getImplosionDamage(bossProjectile),
                        DamageType.ENERGY,
                        0f,
                        false,
                        false,
                        null,
                        false
                );
            }
        }

        private List<CombatEntityAPI> collectTargetsInRadius(CombatEngineAPI engine, float radius) {
            Set<CombatEntityAPI> targets = new HashSet<>();

            Iterator<Object> shipIter = engine.getShipGrid().getCheckIterator(center, radius * 2f, radius * 2f);
            while (shipIter.hasNext()) {
                Object obj = shipIter.next();
                if (!(obj instanceof ShipAPI ship)) {
                    continue;
                }
                if (!ship.isAlive() || ship.isHulk()) {
                    continue;
                }
                if (Misc.getDistance(center, ship.getLocation()) > radius) {
                    continue;
                }
                targets.add(ship);
            }

            Iterator<Object> missileIter = engine.getMissileGrid().getCheckIterator(center, radius * 2f, radius * 2f);
            while (missileIter.hasNext()) {
                Object obj = missileIter.next();
                if (!(obj instanceof MissileAPI missile)) {
                    continue;
                }
                if (missile.isExpired() || missile.wasRemoved() || missile.isFading()) {
                    continue;
                }
                if (Misc.getDistance(center, missile.getLocation()) > radius) {
                    continue;
                }
                targets.add(missile);
            }

            return new ArrayList<>(targets);
        }

        private Vector2f getShieldAwareTargetPoint(CombatEntityAPI target) {
            if (target == null) {
                return new Vector2f(center);
            }
            if (target instanceof ShipAPI ship) {
                ShieldAPI shield = ship.getShield();
                if (shield != null && shield.isOn()) {
                    Vector2f shieldCenter = shield.getLocation();
                    Vector2f dir = Vector2f.sub(center, shieldCenter, new Vector2f());
                    if (dir.lengthSquared() < 1f) {
                        dir = Misc.getUnitVectorAtDegreeAngle(RANDOM.nextFloat() * 360f);
                    } else {
                        dir.normalise();
                    }

                    float r = Math.max(10f, shield.getRadius() - 5f);
                    Vector2f shieldPoint = new Vector2f(
                            shieldCenter.x + dir.x * r,
                            shieldCenter.y + dir.y * r
                    );
                    if (shield.isWithinArc(shieldPoint)) {
                        return shieldPoint;
                    }
                }
            }

            float radius = Math.max(10f, target.getCollisionRadius() * 0.35f);
            return Misc.getPointWithinRadius(target.getLocation(), radius);
        }
        //this is for the summons at the end
        private void spawnEndSummons(CombatEngineAPI engine) {
            if (engine == null) {
                return;
            }
            CombatFleetManagerAPI manager = engine.getFleetManager(summonOwner);
            if (manager == null) {
                return;
            }
            if (hasAliveSpawnedSummons(engine)) {
                return;
            }
            //rng chance for the spawn its a 50/50 and the next hit is guaranteed
            float failBonus = getSpawnFailBonus(engine);
            float spawnChance = Math.min(1f, RIFT_END_SPAWN_BASE_CHANCE + failBonus);
            if (RANDOM.nextFloat() > spawnChance) {
                setSpawnFailBonus(engine, Math.min(1f, failBonus + RIFT_END_SPAWN_FAIL_BONUS_STEP));
                return;
            }
            setSpawnFailBonus(engine, 0f);

            int threatLevel = getThreatLevel(engine);
            SpawnGroup group = pickSpawnGroupForThreat(threatLevel);
            if (group == null) {
                return;
            }
            //this is to make the spawns look like theyre coming out of the rift
            for (int i = 0; i < group.count; i++) {
                Vector2f spawnLoc = Misc.getPointWithinRadius(center, 120f);
                float facing = RANDOM.nextFloat() * 360f;
                ShipAPI spawned = manager.spawnShipOrWing(group.variantId, spawnLoc, facing, 1f);
                if (spawned != null) {
                    spawned.setOwner(summonOwner);
                    spawned.setAlly(false);
                    spawned.addTag(RIFT_END_SUMMON_TAG);
                    spawned.getVelocity().set(0f, 0f);
                    spawned.setCRAtDeployment(1f);
                }
            }//this is the threat level each time the spawns are killed add 1 which moves to the new group so first is low then medium then high then always high
            setThreatLevel(engine, Math.min(RIFT_THREAT_MAX, threatLevel + 1));
        }
        //method to decide which group to pick
        private SpawnGroup pickSpawnGroupForThreat(int threatLevel) {
            if (threatLevel <= 1) {
                return pickLowThreatGroup();
            }
            if (threatLevel == 2) {
                return pickMediumThreatGroup();
            }
            return RIFT_THREAT_HIGH_GROUP;
        }
        //this is the method to decide if its tendrils or banshees
        private SpawnGroup pickLowThreatGroup() {
            // 50/50: banshees or tendrils
            if (RIFT_THREAT_LOW_GROUPS.length < 2) {
                return null;
            }
            return RANDOM.nextFloat() < RIFT_PAIR_SPLIT_CHANCE ? RIFT_THREAT_LOW_GROUPS[0] : RIFT_THREAT_LOW_GROUPS[1];
        }
        //same as above but with eyes or maelstroms
        private SpawnGroup pickMediumThreatGroup() {
            // 50/50: eyes or maelstrom
            if (RIFT_THREAT_MEDIUM_GROUPS.length < 2) {
                return null;
            }
            return RANDOM.nextFloat() < RIFT_PAIR_SPLIT_CHANCE ? RIFT_THREAT_MEDIUM_GROUPS[0] : RIFT_THREAT_MEDIUM_GROUPS[1];
        }

        private float getSpawnFailBonus(CombatEngineAPI engine) {
            Object value = engine.getCustomData().get(RIFT_END_SPAWN_BONUS_KEY);
            if (value instanceof Number number) {
                return Math.max(0f, number.floatValue());
            }
            return 0f;
        }

        private void setSpawnFailBonus(CombatEngineAPI engine, float value) {
            engine.getCustomData().put(RIFT_END_SPAWN_BONUS_KEY, Math.max(0f, value));
        }

        private int getThreatLevel(CombatEngineAPI engine) {
            Object value = engine.getCustomData().get(RIFT_THREAT_LEVEL_KEY);
            if (value instanceof Number number) {
                int threat = number.intValue();
                if (threat < RIFT_THREAT_MIN) {
                    return RIFT_THREAT_MIN;
                }
                return Math.min(threat, RIFT_THREAT_MAX);
            }
            return RIFT_THREAT_MIN;
        }

        private void setThreatLevel(CombatEngineAPI engine, int value) {
            int clamped = Math.max(RIFT_THREAT_MIN, Math.min(value, RIFT_THREAT_MAX));
            engine.getCustomData().put(RIFT_THREAT_LEVEL_KEY, clamped);
        }
        //this is to make sure no new spawns can come out as long as the previous ones are alive
        private boolean hasAliveSpawnedSummons(CombatEngineAPI engine) {
            int alive = 0;
            for (ShipAPI ship : engine.getShips()) {
                if (ship == null) {
                    continue;
                }
                if (!ship.isAlive() || ship.isHulk()) {
                    continue;
                }
                if (!ship.hasTag(RIFT_END_SUMMON_TAG)) {
                    continue;
                }
                alive++;
                if (alive >= no_spawn_limit) {
                    return true;
                }
            }
            return false;
        }
        //this is the sound effect for its end
        private static class ClosingSoundLoop extends BaseEveryFrameCombatPlugin {
            private final Vector2f center;
            private final Vector2f velocity = new Vector2f();
            private float elapsed = 0f;

            private ClosingSoundLoop(Vector2f center) {
                this.center = new Vector2f(center);
            }

            @Override
            public void advance(float amount, List<InputEventAPI> events) {
                CombatEngineAPI engine = Global.getCombatEngine();
                if (engine == null || engine.isPaused()) {
                    return;
                }
                elapsed += amount;
                Global.getSoundPlayer().playLoop(
                        RIFT_CLOSE_SOUND_ID,
                        this,
                        1f,
                        1f,
                        center,
                        velocity
                );
                if (elapsed >= RIFT_CLOSE_SOUND_DURATION_SECONDS) {
                    engine.removePlugin(this);
                }
            }
        }
    }
}
