package tidal.shroud.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import java.awt.Color;
import org.lwjgl.util.vector.Vector2f;

public class EnergyMassLauncherEffect implements EveryFrameWeaponEffectPlugin {

    private float particleTimer = 0f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine == null || weapon == null) {
            return;
        }

        if (engine.isPaused()) {
            return;
        }

        if (weapon.getShip() == null) {
            return;
        }

        if (weapon.isDisabled()) {
            return;
        }

        if (!weapon.isFiring()) {
            return;
        }

        particleTimer += amount;

        if (particleTimer >= 0.05f) {
            particleTimer = 0f;

            Vector2f loc = weapon.getFirePoint(0);
            if (loc == null) {
                loc = weapon.getLocation();
            }

            engine.addSmoothParticle(
                    loc,
                    weapon.getShip().getVelocity(),
                    20f,          // size
                    1f,           // brightness
                    0.35f,        // duration
                    new Color(120, 170, 255, 180)
            );
        }
    }
}