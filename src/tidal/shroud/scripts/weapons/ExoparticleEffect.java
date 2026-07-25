package tidal.shroud.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.opengl.GL14;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class ExoparticleEffect extends BaseCombatLayeredRenderingPlugin implements OnHitEffectPlugin {

    // ======================
    // CONFIG
    // ======================
    public static final String DOT_KEY = "exo_dot_active";
    protected float elapsed = 0f; //for particules
    protected float maxDuration = 10f;
    public static int NUM_TICKS = 10;
    public static float TOTAL_ARMOR_DAMAGE = 1000f;

    // ======================
    // DATA
    // ======================
    protected DamagingProjectileAPI proj;
    protected ShipAPI target;
    protected Vector2f offset;

    protected int ticks = 0;
    protected IntervalUtil interval;

    protected List<ParticleData> particles = new ArrayList<>();
    protected FaderUtil fader = new FaderUtil(1f, 0.5f, 0.5f);

    protected EnumSet<CombatEngineLayers> layers =
            EnumSet.of(CombatEngineLayers.BELOW_INDICATORS_LAYER);

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit,
                      ApplyDamageResultAPI damageResult,
                      CombatEngineAPI engine) {

        if (shieldHit) return;
        if (!(target instanceof ShipAPI)) return;

        ShipAPI ship = (ShipAPI) target;


        if (ship.getCustomData().containsKey(DOT_KEY)) {
            return;
        }

        Vector2f offset = Vector2f.sub(point, ship.getLocation(), new Vector2f());
        offset = Misc.rotateAroundOrigin(offset, -ship.getFacing());

        ExoparticleEffect effect = new ExoparticleEffect(projectile, ship, offset);

        ship.setCustomData(DOT_KEY, true); // mark active

        CombatEntityAPI e = engine.addLayeredRenderingPlugin(effect);
        e.getLocation().set(point);
    }
    // ======================
    // CONSTRUCTOR
    // ======================
    public ExoparticleEffect(DamagingProjectileAPI proj, ShipAPI target, Vector2f offset) {
        this.proj = proj;
        this.target = target;
        this.offset = offset;

        this.interval = new IntervalUtil(0.5f, 0.5f); // 0.5s per tick
        this.interval.forceIntervalElapsed();
    }

    public ExoparticleEffect() {
    }

    // ======================
    // ADVANCE
    // ======================

    @Override
    public void advance(float amount) {
        elapsed += amount;

        if (elapsed >= maxDuration) {
            target.removeCustomData(DOT_KEY);
            particles.clear();
            return;
        }
        // attach to ship
        Vector2f loc = new Vector2f(offset);
        loc = Misc.rotateAroundOrigin(loc, target.getFacing());
        Vector2f.add(target.getLocation(), loc, loc);
        entity.getLocation().set(loc);

        // particles
        particleInterval.advance(amount);
        if (particleInterval.intervalElapsed()) {
            addParticle();
        }

        // update particles
        List<ParticleData> remove = new ArrayList<>();
        for (ParticleData p : particles) {
            p.advance(amount);
            if (p.elapsed >= p.maxDur) remove.add(p);
        }
        particles.removeAll(remove);

        // damage ticks
        interval.advance(amount);
        if (interval.intervalElapsed() && ticks < NUM_TICKS) {
            dealArmorDamage();
            ticks++;
        }

        // fade out
        if (ticks >= NUM_TICKS || !target.isAlive()) {
            fader.fadeOut();
        }
        fader.advance(amount);

        // sound
        Global.getSoundPlayer().playLoop(
                "disintegrator_loop",
                target,
                1f,
                fader.getBrightness(),
                loc,
                target.getVelocity()
        );
    }

    // ======================
    // DAMAGE (logic)
    // ======================
    protected void dealArmorDamage() {
        CombatEngineAPI engine = Global.getCombatEngine();

        Vector2f point = new Vector2f(entity.getLocation());
        ArmorGridAPI grid = target.getArmorGrid();

        int[] cell = grid.getCellAtLocation(point);
        if (cell == null) return;

        int gridWidth = grid.getGrid().length;
        int gridHeight = grid.getGrid()[0].length;

        float damagePerTick = TOTAL_ARMOR_DAMAGE / NUM_TICKS;

        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {

                int cx = cell[0] + i;
                int cy = cell[1] + j;

                if (cx < 0 || cx >= gridWidth || cy < 0 || cy >= gridHeight) continue;

                float mult = (i == 0 && j == 0) ? 0.15f : 0.05f;
                float damage = damagePerTick * mult;

                float armor = grid.getArmorValue(cx, cy);

                // IGNORE armor damage reduction
                float newArmor = Math.max(0f, armor - damage);
                grid.setArmorValue(cx, cy, newArmor);
            }
        }

        target.syncWithArmorGridState();

        if (Misc.shouldShowDamageFloaty(proj.getSource(), target)) {
            engine.addFloatingDamageText(
                    point,
                    damagePerTick,
                    0f,
                    Misc.FLOATY_ARMOR_DAMAGE_COLOR,
                    target,
                    proj.getSource()
            );
        }
    }

    // ======================
    // PARTICLES
    // ======================
    protected IntervalUtil particleInterval = new IntervalUtil(0.05f, 0.1f);

    protected void addParticle() {
        ParticleData p = new ParticleData(30f, 2f, 2f);
        p.offset = Misc.getPointWithinRadius(new Vector2f(), 20f);
        particles.add(p);
    }

    public static class ParticleData {
        public SpriteAPI sprite = Global.getSettings().getSprite("misc", "nebula_particles");
        public Vector2f offset = new Vector2f();
        public Vector2f vel = new Vector2f();

        public float scale = 1f;
        public float scaleIncreaseRate;
        public float angle;

        public float maxDur;
        public float elapsed = 0f;

        public float baseSize;

        public FaderUtil fader;

        public ParticleData(float baseSize, float maxDur, float endSizeMult) {
            this.baseSize = baseSize;
            this.maxDur = maxDur;
            this.scaleIncreaseRate = endSizeMult / maxDur;

            sprite.setAdditiveBlend();
            sprite.setColor(new Color(255, 50, 50, 60));

            angle = (float) Math.random() * 360f;

            vel = Misc.getUnitVectorAtDegreeAngle((float) Math.random() * 360f);
            vel.scale(20f);

            fader = new FaderUtil(0f, 0.5f, 0.5f);
            fader.fadeIn();
        }

        public void advance(float amount) {
            scale += scaleIncreaseRate * amount;
            offset.x += vel.x * amount;
            offset.y += vel.y * amount;
            angle += 30f * amount;

            elapsed += amount;

            if (maxDur - elapsed < 0.5f) {
                fader.fadeOut();
            }

            fader.advance(amount);
        }
    }

    // ======================
    // RENDER
    // ======================
    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        float alpha = viewport.getAlphaMult();

        GL14.glBlendEquation(GL14.GL_FUNC_ADD);

        for (ParticleData p : particles) {
            float size = p.baseSize * p.scale;

            Vector2f loc = new Vector2f(entity.getLocation());
            Vector2f.add(loc, p.offset, loc);

            p.sprite.setSize(size, size);
            p.sprite.setAngle(p.angle);
            p.sprite.setAlphaMult(alpha * p.fader.getBrightness());

            p.sprite.renderAtCenter(loc.x, loc.y);
        }
    }

    @Override
    public float getRenderRadius() {
        return 500f;
    }

    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return layers;
    }

    @Override
    public boolean isExpired() {
        return elapsed >= maxDuration;
    }
}