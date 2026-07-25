package tidal.shroud.skills

import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import second_in_command.SCData
import second_in_command.specs.SCBaseSkillPlugin

class UncannyAgility : SCBaseSkillPlugin() {

    override fun getAffectsString(): String {
        return "all ships in the fleet"
    }

    override fun addTooltip(data: SCData, tooltip: TooltipMakerAPI) {
        tooltip.addPara(
            "Increase top speed by 10 flat",
            0f, Misc.getHighlightColor(), Misc.getHighlightColor()
        )
        tooltip.addPara(
            "Increase turn rate by 10%%",
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

        val stats = ship.mutableStats

        stats.maxSpeed.modifyFlat(id, 10f)
        stats.maxTurnRate.modifyPercent(id, 10f)
    }
}