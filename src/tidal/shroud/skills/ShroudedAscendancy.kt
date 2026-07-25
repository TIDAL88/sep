package tidal.shroud.skills

import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import second_in_command.SCData
import second_in_command.specs.SCBaseSkillPlugin

class ShroudedAscendancy : SCBaseSkillPlugin() {
    companion object {
        private val FULL_BONUS_TRIGGER_HULLMODS = setOf(
            "dweller_hullmod",
            "dweller_hullmod_tidal"
        )
        private val PROGRESSIVE_SHROUDED_HULLMODS = setOf(
            "shrouded_thunderhead",
            "shrouded_mantle",
            "shrouded_lens"
        )
    }

    private fun hasFullBonusTriggerHullmod(variant: ShipVariantAPI?): Boolean {
        if (variant == null) return false
        return FULL_BONUS_TRIGGER_HULLMODS.any { variant.hasHullMod(it) }
    }

    private fun getProgressiveShroudedHullmodCount(variant: ShipVariantAPI?): Int {
        if (variant == null) return 0
        return PROGRESSIVE_SHROUDED_HULLMODS.count { variant.hasHullMod(it) }
    }

    override fun getAffectsString(): String = "all ships in the fleet"

    override fun addTooltip(data: SCData, tooltip: TooltipMakerAPI) {

        tooltip.addPara(
            "+5%% flux dissipation",
            0f,
            Misc.getHighlightColor(),
            Misc.getHighlightColor()
        )
        tooltip.addPara(
            " +10%% hull",
            0f,
            Misc.getHighlightColor(),
            Misc.getHighlightColor()
        )
        tooltip.addPara(
            "If one Shrouded Hullmod is installed: increase speed by 10%%",
            0f,
            Misc.getHighlightColor(),
            Misc.getHighlightColor()
        )
        tooltip.addPara(
            "If two Shrouded Hullmods are installed: increase flux cap by 10%%",
            0f,
            Misc.getHighlightColor(),
            Misc.getHighlightColor()
        )
        tooltip.addPara(
            "If three Shrouded Hullmods are installed: increase damage by 10%% and flux dissipation by 5%%",
            0f,
            Misc.getHighlightColor(),
            Misc.getHighlightColor()
        )
        tooltip.addPara("In order to truly recognize the slop,you must first produce the slop yourself... - Mr Noof, art of slop",
        0f,
            Misc.getGrayColor(),
        )
    }
    override fun applyEffectsAfterShipCreation(
        data: SCData?,
        ship: ShipAPI?,
        variant: ShipVariantAPI?,
        id: String?
    ) {
        if (ship == null || id == null) return
        val stats: MutableShipStatsAPI = ship.mutableStats
        val hasFullBonus = hasFullBonusTriggerHullmod(variant)
        val progressiveCount = getProgressiveShroudedHullmodCount(variant)
        val speedBonusId = "${id}_shrouded_speed"
        val fluxBonusId = "${id}_shrouded_flux"
        val damageBonusId = "${id}_shrouded_damage"

        if (hasFullBonus || progressiveCount >= 1) {
            stats.maxSpeed.modifyPercent(speedBonusId, 10f)
        } else {
            stats.maxSpeed.unmodify(speedBonusId)
        }

        if (hasFullBonus || progressiveCount >= 2) {
            stats.fluxCapacity.modifyPercent(fluxBonusId, 10f)
        } else {
            stats.fluxCapacity.unmodify(fluxBonusId)
        }

        if (hasFullBonus || progressiveCount >= 3) {
            stats.energyWeaponDamageMult.modifyMult(damageBonusId, 1.1f)
            stats.fluxDissipation.modifyPercent(id, 5f)
        } else {
            stats.energyWeaponDamageMult.unmodify(damageBonusId)
            stats.fluxDissipation.unmodify(id)
        }
    }

    override fun applyEffectsBeforeShipCreation(
        data: SCData?,
        stats: MutableShipStatsAPI?,
        variant: ShipVariantAPI?,
        hullSize: ShipAPI.HullSize?,
        id: String?
    ) {
        if (stats == null || id == null) return
        stats.hullBonus.modifyPercent(id, 10f)
    }
}
