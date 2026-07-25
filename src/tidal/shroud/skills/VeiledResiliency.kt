package tidal.shroud.skills


import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import second_in_command.SCData
import second_in_command.specs.SCBaseSkillPlugin

class VeiledResiliency : SCBaseSkillPlugin() {
    companion object {
        private const val SHROUDED_MANTLE_HULLMOD = "shrouded_mantle"
        private val CONDITIONAL_BONUS_HULLMODS = setOf(
            "dweller_hullmod",
            "dweller_hullmod_tidal"
        )
    }

    private fun hasMantleBonusHullmod(variant: ShipVariantAPI): Boolean {
        return variant.hasHullMod(SHROUDED_MANTLE_HULLMOD) ||
            CONDITIONAL_BONUS_HULLMODS.any { variant.hasHullMod(it) }
    }

    override fun getAffectsString(): String {
        return "all ships in the fleet"
    }

    override fun addTooltip(data: SCData, tooltip: TooltipMakerAPI) {
        tooltip.addPara(
            " +5%% flux dissipation",
            0f,
            Misc.getHighlightColor(),
            Misc.getHighlightColor()
        )
        tooltip.addPara(
            " +20%% hull",
            0f,
            Misc.getHighlightColor(),
            Misc.getHighlightColor()
        )
        tooltip.addPara(
            " -20%% top speed",
            0f,
            Misc.getHighlightColor(),
            Misc.getHighlightColor()
        )
        tooltip.addPara(
            "If Shrouded Mantle is installed: Shield Efficiency improved by 10%%, and speed reduced by an additional 10%%",
            0f,
            Misc.getHighlightColor(),
            Misc.getHighlightColor()
        )
    }
    override fun applyEffectsAfterShipCreation(
        data: SCData,
        ship: ShipAPI,
        variant: ShipVariantAPI,
        id: String
    ) {
        val stats = ship.mutableStats

        stats.fluxDissipation.modifyPercent(id, 5f)
        stats.maxSpeed.modifyMult(id, 0.80f)

        if (hasMantleBonusHullmod(variant)) {
            stats.shieldDamageTakenMult.modifyMult(id + "_shrouded_mantle", 0.9f)
            stats.maxSpeed.modifyMult(id, 0.70f)
        } else {
            stats.shieldDamageTakenMult.unmodify(id + "_shrouded_mantle")
        }
    }

    override fun applyEffectsBeforeShipCreation(
        data: SCData,
        stats: MutableShipStatsAPI,
        variant: ShipVariantAPI,
        hullSize: ShipAPI.HullSize?,
        id: String
    ) {
        stats.hullBonus.modifyPercent(id, 20f)
    }
}
