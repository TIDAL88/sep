package tidal.shroud.skills

import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import second_in_command.SCData
import second_in_command.specs.SCBaseSkillPlugin

class DimensionalMastery : SCBaseSkillPlugin() {

    override fun getAffectsString(): String {
        return "all ships in the fleet"
    }

    override fun addTooltip(data: SCData, tooltip: TooltipMakerAPI) {
        tooltip.addPara("+10%% shield efficiency", 0f, Misc.getHighlightColor(), Misc.getHighlightColor())
        tooltip.addPara("+30%% soft flux upkeep for shields", 0f, Misc.getHighlightColor(), Misc.getHighlightColor())
    }

    override fun applyEffectsAfterShipCreation(
        data: SCData?,
        ship: ShipAPI?,
        variant: ShipVariantAPI?,
        id: String?
    ) {
        if (ship == null || id == null) return

        val stats = ship.mutableStats


        stats.shieldDamageTakenMult.modifyMult(id, 0.90f)


        stats.shieldUpkeepMult.modifyMult(id, 1.30f)
    }
}