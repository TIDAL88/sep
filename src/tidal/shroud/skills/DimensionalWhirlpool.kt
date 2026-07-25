package tidal.shroud.skills

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.combat.listeners.AdvanceableListener
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils
import second_in_command.SCData
import second_in_command.specs.SCBaseSkillPlugin
import kotlin.collections.iterator

class DimensionalWhirlpool : SCBaseSkillPlugin() {

    override fun getAffectsString(): String {
        return "all ships"
    }

    override fun addTooltip(data: SCData, tooltip: TooltipMakerAPI) {
        tooltip.addPara("Increase speed by 2%% for each ship in a radius of 1200 su up 6%%,", 0f, Misc.getHighlightColor(), Misc.getHighlightColor())
        tooltip.addPara("Ships equipped with Shrouded hullmods increase speed by 3%% instead", 0f, Misc.getHighlightColor(), Misc.getHighlightColor())
        tooltip.addPara("Reduce top speed for enemy ships in the radius by 2%% for each allied ship, up to 10%%", 0f, Misc.getHighlightColor(), Misc.getHighlightColor())
    }
    override fun applyEffectsBeforeShipCreation(
        data: SCData,
        stats: MutableShipStatsAPI?,
        variant: ShipVariantAPI,
        hullSize: ShipAPI.HullSize?,
        id: String?
    ) {
    }

    override fun applyEffectsAfterShipCreation(data: SCData, ship: ShipAPI?, variant: ShipVariantAPI, id: String?) {
        if (!ship!!.hasListenerOfClass(DimensionalWhirlpoolScript::class.java)) {
            ship.addListener(DimensionalWhirlpoolScript(ship))
        }
    }
}

class DimensionalWhirlpoolScript(var ship: ShipAPI) : AdvanceableListener {

    var interval = IntervalUtil(0.2f, 0.2f)
    var fieldStrength = 0f

    val auraRadius = 1200f

    val shroudedHullmods = listOf(
        "shrouded_thunderhead",
        "shrouded_mantle",
        "shrouded_lens",
        "dweller_hullmod",
        "dweller_hullmod_tidal"
    )

    override fun advance(amount: Float) {

        val engine = Global.getCombatEngine()
        val player = engine.playerShip

        if (player != null) {

            val dist = MathUtils.getDistance(player, ship)

            if (dist <= auraRadius && fieldStrength > 0f) {

                var text = "Field strength: ${fieldStrength.toInt()}%"

                if (player.owner != ship.owner) {
                    text = "Speed disrupted: ${fieldStrength.toInt()}%"
                }

                engine.maintainStatusForPlayerShip(
                    "sc_dimensional_whirlpool",
                    "graphics/secondInCommand/tidal/whirlpool.png",
                    "Dimensional Whirlpool",
                    text,
                    false
                )
            }
        }

        interval.advance(amount)
        if (!interval.intervalElapsed()) return

        var strength = 0f

        var iterator = engine.shipGrid.getCheckIterator(ship.location, 2000f, 2000f)
        for (entry in iterator) {
            if (entry !is ShipAPI) continue
            val ally = entry

            if (!ally.isAlive) continue
            if (ally.owner != ship.owner) continue
            if (MathUtils.getDistance(ally, ship) > auraRadius) continue

            var isShrouded = false
            for (id in shroudedHullmods) {
                if (ally.variant.hasHullMod(id)) {
                    isShrouded = true
                    break
                }
            }

            strength += if (isShrouded) {
                3f
            } else {
                2f
            }
        }

        if (strength > 6f) strength = 6f
        fieldStrength = strength

        iterator = engine.shipGrid.getCheckIterator(ship.location, 2000f, 2000f)
        for (entry in iterator) {
            if (entry !is ShipAPI) continue
            val target = entry

            if (!target.isAlive) continue
            if (MathUtils.getDistance(target, ship) > auraRadius) continue

            target.mutableStats.maxSpeed.unmodify("sc_dimensional_whirlpool")

            if (target.owner == ship.owner) {
                target.mutableStats.maxSpeed.modifyPercent("sc_dimensional_whirlpool", fieldStrength)
            } else {
                target.mutableStats.maxSpeed.modifyPercent("sc_dimensional_whirlpool", -fieldStrength)
            }
        }
    }
}
