package tidal.shroud.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.input.InputEventAPI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.lwjgl.util.vector.Vector2f;

public class DarknessInTheNightSkill implements ShipSkillEffect {

    private static final String SKILL_ID = "darkness_in_the_night";
    private static final String BASE_MOD_ID = "darkness_in_the_night_base";
    private static final String REGISTRAR_KEY = "darkness_in_the_night_registrar";
    private static final float SPEED_BONUS_PER_TRIGGER = 7f;
    private static final float DISSIPATION_BONUS_PER_TRIGGER = 7f;
    private static final float DAMAGE_BONUS_PER_TRIGGER = 5f;
    private static final float HULL_PENALTY_FRIGATE_PERCENT = 20f;
    private static final float HULL_PENALTY_DESTROYER_PERCENT = 35f;
    private static final float HULL_PENALTY_CRUISER_PERCENT = 45f;
    private static final float HULL_PENALTY_CAPITAL_PERCENT = 55f;
    private static final float ARMOR_PENALTY_FRIGATE_PERCENT = 20f;
    private static final float ARMOR_PENALTY_DESTROYER_PERCENT = 40f;
    private static final float ARMOR_PENALTY_CRUISER_PERCENT = 60f;
    private static final float ARMOR_PENALTY_CAPITAL_PERCENT = 80f;
    private static final float FLUX_SHUNT_FRACTION = 0.10f;
    private static final float KILL_ATTRIBUTION_WINDOW = 2f;
    private static final float TARGET_TRACK_TIMEOUT = 12f;
    private static final String CLAIMED_KILL_KEY = "darkness_in_the_night_kill_claimed";

    @Override
    public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
        applyBasePenalties(stats, hullSize);
        ensureCombatRegistrar();
        ShipAPI ship = getShip(stats);
        if (ship == null) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine != null) {
                ship = engine.getPlayerShip();
            }
        }
        if (ship != null) {
            maybeAttachTracker(ship);
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
        ShipAPI ship = getShip(stats);
        unapplyBasePenalties(stats);

        if (ship != null) {
            ship.removeListenerOfClass(DarknessKillTracker.class);
            String bonusId = getBonusId(ship, BASE_MOD_ID);
            ship.getMutableStats().getMaxSpeed().unmodify(bonusId);
            ship.getMutableStats().getFluxDissipation().unmodify(bonusId);
            ship.getMutableStats().getEnergyWeaponDamageMult().unmodify(bonusId);
            ship.getMutableStats().getBallisticWeaponDamageMult().unmodify(bonusId);
            ship.getMutableStats().getMissileWeaponDamageMult().unmodify(bonusId);
            ship.getMutableStats().getBeamWeaponDamageMult().unmodify(bonusId);
        }
    }

    @Override
    public ScopeDescription getScopeDescription() {
        return ScopeDescription.PILOTED_SHIP;
    }

    @Override
    public String getEffectPerLevelDescription() {
        return "";
    }

    @Override
    public String getEffectDescription(float level) {
        return "Trigger: destroyers and cruisers grant +1 stack, capitals grant +2 stacks.\n"
                + "Per stack bonus: +7% top speed, +7% flux stats, and +5% weapon damage.\n"
                + "Downside: hull penalty is 20/35/45/55% and armor penalty is 20/40/60/80%.\n"
                + "On trigger: shunts 15% of max flux once per qualifying kill.";
    }

    private static ShipAPI getShip(MutableShipStatsAPI stats) {
        if (stats == null || !(stats.getEntity() instanceof ShipAPI)) {
            return null;
        }
        return (ShipAPI) stats.getEntity();
    }

    private static void applyBasePenalties(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize) {
        float hullPenalty = -getHullPenaltyPercent(hullSize);
        float armorPenalty = -getArmorPenaltyPercent(hullSize);
        stats.getHullBonus().modifyPercent(BASE_MOD_ID, hullPenalty);
        stats.getEffectiveArmorBonus().modifyPercent(BASE_MOD_ID, armorPenalty);
    }

    private static float getHullPenaltyPercent(ShipAPI.HullSize hullSize) {
        if (hullSize == ShipAPI.HullSize.FRIGATE) {
            return HULL_PENALTY_FRIGATE_PERCENT;
        }
        if (hullSize == ShipAPI.HullSize.DESTROYER) {
            return HULL_PENALTY_DESTROYER_PERCENT;
        }
        if (hullSize == ShipAPI.HullSize.CRUISER) {
            return HULL_PENALTY_CRUISER_PERCENT;
        }
        if (hullSize == ShipAPI.HullSize.CAPITAL_SHIP) {
            return HULL_PENALTY_CAPITAL_PERCENT;
        }
        return HULL_PENALTY_CRUISER_PERCENT;
    }

    private static float getArmorPenaltyPercent(ShipAPI.HullSize hullSize) {
        if (hullSize == ShipAPI.HullSize.FRIGATE) {
            return ARMOR_PENALTY_FRIGATE_PERCENT;
        }
        if (hullSize == ShipAPI.HullSize.DESTROYER) {
            return ARMOR_PENALTY_DESTROYER_PERCENT;
        }
        if (hullSize == ShipAPI.HullSize.CRUISER) {
            return ARMOR_PENALTY_CRUISER_PERCENT;
        }
        if (hullSize == ShipAPI.HullSize.CAPITAL_SHIP) {
            return ARMOR_PENALTY_CAPITAL_PERCENT;
        }
        return ARMOR_PENALTY_CRUISER_PERCENT;
    }

    private static void unapplyBasePenalties(MutableShipStatsAPI stats) {
        stats.getHullBonus().unmodify(BASE_MOD_ID);
        stats.getEffectiveArmorBonus().unmodify(BASE_MOD_ID);
    }

    private static void maybeAttachTracker(ShipAPI ship) {
        if (ship == null) {
            return;
        }

        if (!ship.hasListenerOfClass(DarknessKillTracker.class)) {
            ship.addListener(new DarknessKillTracker(ship, BASE_MOD_ID));
        }
    }

    private static void ensureCombatRegistrar() {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) {
            return;
        }
        if (engine.getCustomData().containsKey(REGISTRAR_KEY)) {
            return;
        }

        engine.addPlugin(new DarknessCombatRegistrar());
        engine.getCustomData().put(REGISTRAR_KEY, Boolean.TRUE);
    }

    private static boolean shipHasDarknessSkill(ShipAPI ship) {
        return ship != null
                && ship.getCaptain() != null
                && ship.getCaptain().getStats() != null
                && ship.getCaptain().getStats().getSkillLevel(SKILL_ID) > 0f;
    }

    private static class DarknessCombatRegistrar extends BaseEveryFrameCombatPlugin {
        private float elapsed = 0f;

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null || engine.isPaused()) {
                return;
            }

            elapsed += amount;
            if (elapsed < 0.25f) {
                return;
            }
            elapsed = 0f;

            for (ShipAPI ship : engine.getShips()) {
                if (ship == null || !ship.isAlive() || ship.isDrone() || ship.isFighter()) {
                    continue;
                }
                if (!shipHasDarknessSkill(ship)) {
                    continue;
                }
                maybeAttachTracker(ship);
            }
        }
    }

    private static String getBonusId(ShipAPI ship, String id) {
        String memberId = ship.getFleetMemberId();
        if (memberId == null || memberId.isEmpty()) {
            memberId = Integer.toString(ship.hashCode());
        }
        return "darkness_in_the_night_bonus_" + id + "_" + memberId;
    }

    private static class DarknessKillTracker implements AdvanceableListener, DamageDealtModifier {
        private final ShipAPI ship;
        private final String bonusId;
        private final Map<ShipAPI, Float> recentlyDamagedTargets = new HashMap<>();

        private int triggerCount = 0;

        private DarknessKillTracker(ShipAPI ship, String id) {
            this.ship = ship;
            this.bonusId = getBonusId(ship, id);
        }

        @Override
        public String modifyDamageDealt(Object param,
                                        CombatEntityAPI target,
                                        DamageAPI damage,
                                        Vector2f point,
                                        boolean shieldHit) {
            if (target == null || shieldHit || !(target instanceof ShipAPI)) {
                return null;
            }

            ShipAPI targetShip = (ShipAPI) target;
            if (!isValidKillTarget(targetShip)) {
                return null;
            }
            if (targetShip.getOwner() == ship.getOwner()) {
                return null;
            }

            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null) {
                return null;
            }

            float now = engine.getTotalElapsedTime(false);
            recentlyDamagedTargets.put(targetShip, now);
            return null;
        }

        @Override
        public void advance(float amount) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null || engine.isPaused() || ship == null) {
                return;
            }

            float now = engine.getTotalElapsedTime(false);
            Iterator<Map.Entry<ShipAPI, Float>> iter = recentlyDamagedTargets.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<ShipAPI, Float> entry = iter.next();
                ShipAPI target = entry.getKey();
                float lastHitTime = entry.getValue();

                if (target == null) {
                    iter.remove();
                    continue;
                }

                if (isTargetDestroyed(target)) {
                    if (!isClaimed(target) && isKillAttributedByRecentDamage(now, lastHitTime)) {
                        claimKill(target);
                        processKill(target, engine);
                    }
                    iter.remove();
                    continue;
                }

                if (now - lastHitTime > TARGET_TRACK_TIMEOUT) {
                    iter.remove();
                    continue;
                }

                if (target.isExpired()) {
                    iter.remove();
                    continue;
                }

                if (target.isAlive() && !target.isHulk()) {
                    continue;
                }

                if (isClaimed(target)) {
                    iter.remove();
                    continue;
                }

                if (isKillAttributedByRecentDamage(now, lastHitTime)) {
                    claimKill(target);
                    processKill(target, engine);
                }

                iter.remove();
            }
        }

        private boolean isKillAttributedByRecentDamage(float now, float lastHit) {
            return now - lastHit <= KILL_ATTRIBUTION_WINDOW;
        }

        private boolean isValidKillTarget(ShipAPI target) {
            return target != null
                    && !target.isDrone()
                    && !target.isFighter()
                    && !target.isStation()
                    && !target.isStationModule();
        }

        private boolean isTargetDestroyed(ShipAPI target) {
            if (target == null) {
                return false;
            }
            return target.isExpired()
                    || !target.isAlive()
                    || target.isHulk()
                    || target.getHitpoints() <= 0f;
        }

        private boolean isClaimed(ShipAPI target) {
            Object claimed = target.getCustomData().get(CLAIMED_KILL_KEY);
            return Boolean.TRUE.equals(claimed);
        }

        private void claimKill(ShipAPI target) {
            target.setCustomData(CLAIMED_KILL_KEY, true);
        }

        private void processKill(ShipAPI target, CombatEngineAPI engine) {
            ShipAPI.HullSize size = target.getHullSize();
            int stacksGained;
            if (size == ShipAPI.HullSize.DESTROYER || size == ShipAPI.HullSize.CRUISER) {
                stacksGained = 1;
            } else if (size == ShipAPI.HullSize.CAPITAL_SHIP) {
                stacksGained = 2;
            } else {
                return;
            }

            triggerCount += stacksGained;
            applyPermanentStatBonus();
            shuntFlux();
        }

        private void applyPermanentStatBonus() {
            float speedBonus = triggerCount * SPEED_BONUS_PER_TRIGGER;
            float dissipationBonus = triggerCount * DISSIPATION_BONUS_PER_TRIGGER;
            float damageBonus = triggerCount * DAMAGE_BONUS_PER_TRIGGER;

            ship.getMutableStats().getMaxSpeed().modifyPercent(bonusId, speedBonus);
            ship.getMutableStats().getFluxDissipation().modifyPercent(bonusId, dissipationBonus);
            ship.getMutableStats().getEnergyWeaponDamageMult().modifyPercent(bonusId, damageBonus);
            ship.getMutableStats().getBallisticWeaponDamageMult().modifyPercent(bonusId, damageBonus);
            ship.getMutableStats().getMissileWeaponDamageMult().modifyPercent(bonusId, damageBonus);
            ship.getMutableStats().getBeamWeaponDamageMult().modifyPercent(bonusId, damageBonus);
        }

        private void shuntFlux() {
            if (ship.getFluxTracker() != null) {
                float shunt = ship.getFluxTracker().getMaxFlux() * FLUX_SHUNT_FRACTION;
                ship.getFluxTracker().decreaseFlux(shunt);
            }
        }

    }
}
