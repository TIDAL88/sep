package tidal.shroud.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import java.util.List;

public class fractalprojectorOnfire implements OnFireEffectPlugin {

    private static final FractalRiftEffect RIFT_VISUAL_EFFECT = new FractalRiftEffect();

    // Attaches the visual/trail effect when the projectile is fired.
    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        if (engine == null || weapon == null || projectile == null) {
            return;
        }

        if (projectile instanceof MissileAPI missile) {
            if (missile.hasTag(FractalRiftEffect.ASSAYING_RIFT)) {
                return;
            }
            // Reuse the same effect pipeline used by rift torpedo trail visuals.
            RIFT_VISUAL_EFFECT.onFire(projectile, weapon, engine);
            missile.addTag(FractalRiftEffect.ASSAYING_RIFT);
            engine.addPlugin(new RemovalRiftFallback(projectile));
        }
    }

    private static class RemovalRiftFallback extends BaseEveryFrameCombatPlugin {
        private final DamagingProjectileAPI projectile;

        private RemovalRiftFallback(DamagingProjectileAPI projectile) {
            this.projectile = projectile;
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null || engine.isPaused()) {
                return;
            }
            if (projectile == null) {
                engine.removePlugin(this);
                return;
            }
            if (projectile.wasRemoved() || projectile.isExpired()) {
                fractalballEffect.spawnRift(engine, projectile, null, null, false, null);
                engine.removePlugin(this);
            }
        }
    }
}
