package tidal.shroud.skills

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import second_in_command.SCData
import second_in_command.specs.SCAptitudeSection
import second_in_command.specs.SCBaseAptitudePlugin

class AptitudeShroudWalker : SCBaseAptitudePlugin() {

    override fun getOriginSkillId(): String {
        return "sc_malevolent_energy"
    }

    override fun createSections() {

        val section1 = SCAptitudeSection(true, 0, "leadership1")
        section1.addSkill("sc_dimensional_whirlpool")
        section1.addSkill("sc_hunger_surge")
        section1.addSkill("sc_dimensional_mastery")
        section1.addSkill("sc_hunger_for_blood")
        section1.addSkill("sc_uncanny_agility")
        addSection(section1)

        val section2 = SCAptitudeSection(false, 3, "leadership3")
        section2.addSkill("sc_veiled_resiliency")
        section2.addSkill("sc_piercing_gaze")
        section2.addSkill("sc_veiled_efficiency")
        addSection(section2)

        val section3 = SCAptitudeSection(false, 4, "leadership3")
        section3.addSkill("sc_shrouded_ascendancy")
        addSection(section3)
    }

    override fun getNPCFleetSpawnWeight(data: SCData, fleet: CampaignFleetAPI): Float {
        return if (fleet.faction.id == "dweller") {
            Float.MAX_VALUE
        } else {
            0f
        }
    }

    override fun addCodexDescription(tooltip: TooltipMakerAPI) {
        tooltip.addPara(
            "The Abyss Diver aptitude focuses on all varieties of high-tech warships and integration with shrouded hullmods. " +
                    "It primarily improves flux stats, shields, speed, and energy weapons in various ways, often with tradeoffs " +
                    "or conditions for a variety of different playstyles.",
            1f,
            Misc.getTextColor(),
            Misc.getHighlightColor()
        )

        tooltip.addSpacer(10f)


        tooltip.addPara(
            "It can be a good idea to utilize Shrouded hullmods as additional effects and synergies are activated when Shrouded hullmods are installed, " +
                    "allowing for unique fleet strategies and specialized setups.",
            0f,
            Misc.getTextColor(),
            Misc.getHighlightColor()
        )
    }
}