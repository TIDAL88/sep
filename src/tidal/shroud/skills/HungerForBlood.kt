package tidal.shroud.skills

import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.combat.listeners.AdvanceableListener
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import second_in_command.SCData
import second_in_command.specs.SCBaseSkillPlugin

class HungerForBlood : SCBaseSkillPlugin() {

    override fun getAffectsString(): String {
        return "all ships in the fleet"
    }

    override fun addTooltip(data: SCData, tooltip: TooltipMakerAPI) {
        tooltip.addPara("When below 60%% hull:", 0f, Misc.getHighlightColor(), Misc.getHighlightColor())
        tooltip.addPara("+5%% flux dissipation", 0f, Misc.getHighlightColor(), Misc.getHighlightColor())
        tooltip.addPara("+5%% top speed", 0f, Misc.getHighlightColor(), Misc.getHighlightColor())
    }

    override fun applyEffectsAfterShipCreation(
        data: SCData?,
        ship: ShipAPI?,
        variant: ShipVariantAPI?,
        id: String?
    ) {
        if (ship == null || id == null) return

        if (!ship.hasListenerOfClass(HungerForBloodScript::class.java)) {
            ship.addListener(HungerForBloodScript(ship, id))
        }
    }
}

private class HungerForBloodScript(
    private val ship: ShipAPI,
    private val id: String
) : AdvanceableListener {
    override fun advance(amount: Float) {
        val stats = ship.mutableStats
        stats.fluxDissipation.unmodify(id)
        stats.maxSpeed.unmodify(id)

        if (!ship.isAlive) return

        if (ship.hullLevel < 0.6f) {
            stats.fluxDissipation.modifyPercent(id, 5f)
            stats.maxSpeed.modifyPercent(id, 5f)
        }
    }
}
