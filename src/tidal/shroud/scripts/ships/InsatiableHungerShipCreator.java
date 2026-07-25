package tidal.shroud.scripts.ships;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.combat.dweller.BaseDwellerShipCreator;
import com.fs.starfarer.api.impl.combat.dweller.DwellerCombatPlugin;
import com.fs.starfarer.api.impl.combat.dweller.DwellerShroud;
import com.fs.starfarer.api.impl.combat.dweller.DwellerShipPart;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.List;

public class InsatiableHungerShipCreator extends BaseDwellerShipCreator {
    public static final float ENERGY_DAMAGE_BONUS = 35f;
    public static final float ENERGY_RANGE_BONUS = 20f;
    public static final float FLUX_DISSIPATION_BONUS = 20f;
    private static final String PRIMORDIAL_MUSIC_SET_ID = "music_td_primordial_hunger";
    private static final float CORE_SIZE_X = 2.25f;
    private static final float CORE_SIZE_Y = 1.75f;
    private static final float CORE_ALPHA = 0.4f;
    private static final float VEIL_ALPHA = 0.12f;

    @Override
    public void initBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        super.initBeforeShipCreation(hullSize, stats, id);
        stats.getEnergyWeaponDamageMult().modifyPercent(id, ENERGY_DAMAGE_BONUS);
        stats.getEnergyWeaponRangeBonus().modifyPercent(id, ENERGY_RANGE_BONUS);
        stats.getFluxDissipation().modifyPercent(id, FLUX_DISSIPATION_BONUS);
    }

    @Override
    protected DwellerCombatPlugin createPlugin(ShipAPI ship) {
        DwellerCombatPlugin plugin = super.createPlugin(ship);
        List<DwellerShipPart> parts = plugin.getParts();
        parts.clear();

        DwellerCombatPlugin.WobblyPart core = new DwellerCombatPlugin.WobblyPart(
                "shrouded_hunger", CORE_SIZE_X, CORE_SIZE_Y, new Vector2f(0f, 0f), 0f
        );
        core.setAlphaMult(CORE_ALPHA);
        parts.add(core);

        Color glow = DwellerCombatPlugin.STANDARD_PART_GLOW_COLOR;

        DwellerCombatPlugin.WobblyPart veil = new DwellerCombatPlugin.WobblyPart(
                "shrouded_hunger", CORE_SIZE_X * 1.12f, CORE_SIZE_Y * 1.08f, new Vector2f(0f, 0f), 0f
        );
        veil.setAlphaMult(VEIL_ALPHA);
        parts.add(veil);

        DwellerCombatPlugin.WobblyPart eyeClusterA = new DwellerCombatPlugin.WobblyPart(
                "shrouded_eye_cluster1", 1.1f, 2, 2, 1.6f, new Vector2f(118f, 0f), 0f
        );
        eyeClusterA.color = glow;
        eyeClusterA.additiveBlend = true;
        eyeClusterA.setAlphaMult(0.7f);
        parts.add(eyeClusterA);

        DwellerCombatPlugin.WobblyPart eyeClusterB = new DwellerCombatPlugin.WobblyPart(
                "shrouded_eye_cluster2", 0.85f, 2, 2, 1.55f, new Vector2f(-48f, 84f), 0f
        );
        eyeClusterB.color = glow;
        eyeClusterB.additiveBlend = true;
        eyeClusterB.setAlphaMult(0.65f);
        parts.add(eyeClusterB);

        DwellerCombatPlugin.WobblyPart eyeClusterC = new DwellerCombatPlugin.WobblyPart(
                "shrouded_eye_cluster3", 0.85f, 2, 2, 1.55f, new Vector2f(-48f, -84f), 0f
        );
        eyeClusterC.color = glow;
        eyeClusterC.additiveBlend = true;
        eyeClusterC.setAlphaMult(0.65f);
        parts.add(eyeClusterC);

        ship.addListener(new PrimordialSpawnMusicListener(ship));

        return plugin;
    }

    @Override
    protected void modifyBaselineShroudParams(ShipAPI ship, DwellerShroud.DwellerShroudParams params) {
        params.maxOffset = 430f;
        params.initialMembers = 280;
        params.baseMembersToMaintain = params.initialMembers;
        params.numToRespawn = 5;
        params.numToFlash = Math.max(6, params.numToFlash * 2);
        params.spawnOffsetMult = 0.7f;
        params.spawnOffsetMultForInitialSpawn = params.spawnOffsetMult;
        params.baseSpriteSize *= 2.3f;
        params.negativeParticleAreaMult = 1.0f;
        params.negativeParticleSizeMult = 1.05f;
    }

    private static final class PrimordialSpawnMusicListener implements AdvanceableListener {
        private final ShipAPI ship;
        private boolean triggered = false;

        private PrimordialSpawnMusicListener(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public void advance(float amount) {
            if (triggered || ship == null) {
                return;
            }

            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null || engine.isPaused()) {
                return;
            }

            if (!ship.isAlive() || ship.isHulk()) {
                triggered = true;
                return;
            }

            triggered = true;
            playTrack();
        }

        private void playTrack() {
            try {
                Global.getSoundPlayer().playCustomMusic(1, 1, PRIMORDIAL_MUSIC_SET_ID, true);
            } catch (Throwable ignored) {
                Global.getSoundPlayer().playCustomMusic(1, 1, PRIMORDIAL_MUSIC_SET_ID);
            }
        }
    }
}
