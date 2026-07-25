package tidal.shroud.skills

import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import second_in_command.SCData
import second_in_command.specs.SCBaseSkillPlugin

class   MalevolentEnergy : SCBaseSkillPlugin() {

    override fun getAffectsString(): String {
        return "all ships in the fleet"
    }

    override fun addTooltip(data: SCData, tooltip: TooltipMakerAPI) {
        tooltip.addPara("+5%% energy weapon damage", 0f, Misc.getHighlightColor(), Misc.getHighlightColor())
        tooltip.addPara("+10 speed while 0 flux boost is active", 0f, Misc.getHighlightColor(), Misc.getHighlightColor())
        tooltip.addPara("+5%% flux dissipation", 0f, Misc.getHighlightColor(), Misc.getHighlightColor())
    }

    override fun applyEffectsBeforeShipCreation(
        data: SCData,
        stats: MutableShipStatsAPI?,
        variant: ShipVariantAPI,
        hullSize: ShipAPI.HullSize?,
        id: String?
    ) {
        stats ?: return
        val skillId = id ?: "MalevolentEnergy"

        stats.energyWeaponDamageMult.modifyPercent(skillId, 5f)
        stats.zeroFluxSpeedBoost.modifyFlat(skillId, 10f)
        stats.fluxDissipation.modifyMult(skillId, 1.05f)
    }

    override fun applyEffectsAfterShipCreation(
        data: SCData,
        ship: ShipAPI?,
        variant: ShipVariantAPI,
        id: String?
    ) {

    }
}