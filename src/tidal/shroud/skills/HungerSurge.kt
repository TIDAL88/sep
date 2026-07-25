package tidal.shroud.skills

import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import second_in_command.SCData
import second_in_command.specs.SCBaseSkillPlugin

class HungerSurge: SCBaseSkillPlugin() {
    override fun getAffectsString(): String {
        return "all ships in the fleet"
    }

    override fun addTooltip(data: SCData, tooltip: TooltipMakerAPI) {
        tooltip.addPara(
            "+10%% energy damage if flux > 50%%",
            0f,
            Misc.getHighlightColor(),
            Misc.getHighlightColor()
        )
        tooltip.addPara(
            "+15%% energy damage if overloaded, this damage does not stack with the additional 10%%.",
            0f,
            Misc.getHighlightColor(),
            Misc.getHighlightColor()
        )
    }
    override fun advanceInCombat(data: SCData?, ship: ShipAPI?, amount: Float) {
        if (ship == null) return

        val id = "testskill"
        val stats = ship.mutableStats
        stats.energyWeaponDamageMult.unmodify(id)

        if (ship.fluxLevel > 0.5f) {
            stats.energyWeaponDamageMult.modifyPercent(id, 10f)
        }


        if (ship.fluxTracker.isOverloaded) {
            stats.energyWeaponDamageMult.modifyPercent(id, 15f)
        }
    }
}
