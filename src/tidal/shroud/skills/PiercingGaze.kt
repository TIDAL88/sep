package tidal.shroud.skills

import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import second_in_command.SCData
import second_in_command.specs.SCBaseSkillPlugin

class PiercingGaze : SCBaseSkillPlugin() {

    companion object {
        private const val SHROUDED_LENS_HULLMOD = "shrouded_lens"
        private val RANGE_EXEMPT_HULLMODS = setOf(
            "dweller_hullmod",
            "dweller_hullmod_tidal"
        )
    }

    private fun hasRangeExemptHullmod(variant: ShipVariantAPI): Boolean {
        return RANGE_EXEMPT_HULLMODS.any { variant.hasHullMod(it) }
    }

    override fun getAffectsString(): String {
        return "all ships in the fleet using energy weapons"
    }

    override fun addTooltip(data: SCData, tooltip: TooltipMakerAPI) {
        tooltip.addPara(
            "Energy weapons deal +20%% damage",
            0f,
            Misc.getHighlightColor(),
            Misc.getHighlightColor()
        )
        tooltip.addPara(
            "Energy weapons have 25%% increased flux cost and weapon range is capped at 400",
            0f,
            Misc.getHighlightColor(),
            Misc.getHighlightColor()
        )
        tooltip.addPara(
            "Additional +20%% damage if Shrouded Lens is installed",
            0f,
            Misc.getHighlightColor(),
            Misc.getHighlightColor()
        )
    }

    override fun applyEffectsBeforeShipCreation(
        data: SCData,
        stats: MutableShipStatsAPI?,
        variant: ShipVariantAPI,
        hullSize: ShipAPI.HullSize?,
        id: String?
    ) {
        if (stats == null || id == null) return

        stats.energyWeaponDamageMult.modifyPercent(id, 20f)
        stats.energyWeaponFluxCostMod.modifyMult(id, 1.25f)

        if (hasRangeExemptHullmod(variant)) {
            stats.weaponRangeThreshold.unmodify(id)
            stats.weaponRangeMultPastThreshold.unmodify(id)
        } else {
            stats.weaponRangeThreshold.modifyFlat(id, 400f)
            stats.weaponRangeMultPastThreshold.modifyMult(id, 0f)
        }
    }

    override fun applyEffectsAfterShipCreation(
        data: SCData,
        ship: ShipAPI?,
        variant: ShipVariantAPI,
        id: String?
    ) {
        if (ship == null) return
        val modId = id ?: "piercingGazeSkill"
        val bonusId = "${modId}_thunderhead"

        if (variant.hasHullMod(SHROUDED_LENS_HULLMOD) || hasRangeExemptHullmod(variant)) {
            ship.mutableStats.energyWeaponDamageMult.modifyMult(bonusId, 1.2f)
        } else {
            ship.mutableStats.energyWeaponDamageMult.unmodify(bonusId)
        }
    }
}
