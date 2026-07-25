package tidal.shroud.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.impl.combat.RiftCascadeEffect;
import com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion;
import com.fs.starfarer.api.impl.combat.RiftLanceEffect;
import com.fs.starfarer.api.impl.combat.dweller.DwellerShroud;
import com.fs.starfarer.api.impl.combat.RiftTrailEffect;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

//VISUAL EFFECT ONLY
public class FractalRiftEffect implements OnHitEffectPlugin, OnFireEffectPlugin {
    public static final String ASSAYING_RIFT = "assaying_rift";
    public static final String FRACTAL_RIFT_SHOCKWAVE_SOUND = "fractal_rift_opening";
    public static final String FRACTAL_RIFT_FALLBACK_SOUND = "assaying_rift_explosion";
    private static final float RIFT_POP_RADIUS = 60f;
    private static final float RIFT_POP_THICKNESS = 126f;
    private static final float RIFT_TRAIL_SIZE_MULT = 3.08f;

    // Spawns the rift-pop visual and impact sound at the projectile resolution point.
    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        Color color = RiftCascadeEffect.STANDARD_RIFT_COLOR;
        var spec = projectile.getProjectileSpec();
        if (spec instanceof MissileSpecAPI missileSpec) {
            color = missileSpec.getExplosionColor();
        }

        // Visual rift pop
        var params = RiftCascadeMineExplosion.createStandardRiftParams(color, RIFT_POP_RADIUS);
        params.fadeOut = 1f;
        params.hitGlowSizeMult = 2.4f;
        params.thickness = RIFT_POP_THICKNESS;
        params.noiseMag = 1.2f;
        RiftCascadeMineExplosion.spawnStandardRift(projectile, params);

        Vector2f vel = new Vector2f();
        if (target != null) {
            vel.set(target.getVelocity());
        }
        playShockwaveSound(point, vel, 1f, 1f);
    }

    // Plays the custom takemino shockwave with a fallback to vanilla rift explosion if unavailable.
    public static void playShockwaveSound(Vector2f point, Vector2f vel, float volume, float pitch) {
        Vector2f soundPoint = point != null ? point : new Vector2f();
        Vector2f soundVel = vel != null ? vel : new Vector2f();
        try {
            Global.getSoundPlayer().playSound(FRACTAL_RIFT_SHOCKWAVE_SOUND, volume, pitch, soundPoint, soundVel);
        } catch (Exception ex) {
            Global.getSoundPlayer().playSound(FRACTAL_RIFT_FALLBACK_SOUND, volume, pitch, soundPoint, soundVel);
        }
    }

    // Attaches a rift trail to the projectile and ensures a fallback rift pop on non-hit removal.
    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, final CombatEngineAPI engine) {
        if (!(projectile instanceof MissileAPI missile)) {
            return;
        }
        if (missile.hasTag(ASSAYING_RIFT)) {
            return;
        }

        final MissileAIPlugin proxAI = Global.getCombatEngine().createProximityFuseAI(missile);

        RiftTrailEffect trail = new RiftTrailEffect(missile, null) {
            boolean exploded = false;

            // Advances trail + prox logic and triggers a manual pop if the missile disappears without hit callback.
            @Override
            public void advance(float amount, List<InputEventAPI> events) {
                super.advance(amount, events);
                proxAI.advance(amount);

                if (!exploded && !missile.didDamage() && missile.wasRemoved()) {
                    FractalRiftEffect.this.onHit(missile, null, missile.getLocation(), false, null, engine);
                    exploded = true;
                }
            }

            // Sets the base rift undercolor used by the trail effect.
            @Override
            protected Color getUndercolor() {
                return DwellerShroud.SHROUD_COLOR;
            }

            // Provides the darkening tint derived from the trail undercolor.
            @Override
            protected Color getDarkeningColor() {
                return RiftLanceEffect.getColorForDarkening(getUndercolor());
            }

            // Controls how long each trail particle persists.
            @Override
            protected float getBaseParticleDuration() {
                return 1.5f;
            }

            // Makes the rift trail noticeably thicker.
            @Override
            protected float getBaseParticleSize() {
                return super.getBaseParticleSize() * RIFT_TRAIL_SIZE_MULT;
            }
        };

        missile.setEmpResistance(1000);
        missile.setEccmChanceOverride(1f);
        missile.addTag(ASSAYING_RIFT);
        Global.getCombatEngine().addPlugin(trail);
    }
}
