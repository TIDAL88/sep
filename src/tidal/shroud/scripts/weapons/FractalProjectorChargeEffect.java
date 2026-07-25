package tidal.shroud.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lwjgl.util.vector.Vector2f;

public class FractalProjectorChargeEffect implements EveryFrameWeaponEffectPlugin {

    private static final String CHARGE_SOUND_ID = "project_redacted_chargeup";

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine == null || weapon == null || engine.isPaused()) {
            return;
        }

        ShipAPI ship = weapon.getShip();
        if (ship == null || !ship.isAlive() || weapon.isDisabled()) {
            return;
        }

        float chargeLevel = weapon.getChargeLevel();
        if (chargeLevel <= 0f || weapon.getCooldownRemaining() > 0f) {
            return;
        }

        Vector2f soundLocation = weapon.getFirePoint(0);
        if (soundLocation == null) {
            soundLocation = weapon.getLocation();
        }

        float pitch = 0.95f + (0.1f * chargeLevel);
        float volume = 0.55f + (0.45f * chargeLevel);
        Global.getSoundPlayer().playLoop(
                CHARGE_SOUND_ID,
                weapon,
                pitch,
                volume,
                soundLocation,
                ship.getVelocity()
        );
    }
}
