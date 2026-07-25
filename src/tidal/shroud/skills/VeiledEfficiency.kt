package tidal.shroud.skills

import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import second_in_command.SCData
import second_in_command.specs.SCBaseSkillPlugin

class VeiledEfficiency : SCBaseSkillPlugin() {
    companion object {
        private const val SHROUDED_THUNDERHEAD_HULLMOD = "shrouded_thunderhead"
        private val CONDITIONAL_BONUS_HULLMODS = setOf(
            "dweller_hullmod",
            "dweller_hullmod_tidal"
        )
    }

    private fun hasThunderheadBonusHullmod(variant: ShipVariantAPI?): Boolean {
        if (variant == null) return false
        return variant.hasHullMod(SHROUDED_THUNDERHEAD_HULLMOD) ||
            CONDITIONAL_BONUS_HULLMODS.any { variant.hasHullMod(it) }
    }

    override fun getAffectsString(): String {
        return "all ships in the fleet using energy weapons"
    }

    override fun addTooltip(data: SCData, tooltip: TooltipMakerAPI) {
        tooltip.addPara(
            "-20%% energy weapon damage",
            0f, Misc.getHighlightColor(), Misc.getHighlightColor()
        )
        tooltip.addPara(
            "-20%% flux cost for energy weapons",
            0f, Misc.getHighlightColor(), Misc.getHighlightColor()
        )
        tooltip.addPara(
            "If Shrouded ThunderHead is installed: +20%% range, -20%% damage",
            0f, Misc.getHighlightColor(), Misc.getHighlightColor()
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
        val bonusId = id + "_thunderhead"


        stats.energyWeaponDamageMult.modifyMult(id, 0.8f)
        stats.energyWeaponFluxCostMod.modifyMult(id, 0.8f)


        if (hasThunderheadBonusHullmod(variant ?: ship.variant)) {
            stats.energyWeaponRangeBonus.modifyMult(bonusId, 1.2f)
            stats.energyWeaponDamageMult.modifyMult(bonusId, 0.8f)
        } else {
            stats.energyWeaponRangeBonus.unmodify(bonusId)
            stats.energyWeaponDamageMult.unmodify(bonusId)
        }
    }
}
