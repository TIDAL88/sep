package tidal.shroud.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.BaseAICoreOfficerPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Skills;

import java.util.Random;

public class ShroudConsciousnessOfficerPlugin extends BaseAICoreOfficerPluginImpl {
    private static final float ENTITY_AI_CORE_MULT = 4.5f;
    private static final int TARGET_ANALYSIS_LEVEL = 2;

    public static final String SHROUD_CONSCIOUSNESS_KEY = "$shroud_consciousness";

    @Override
    public PersonAPI createPerson(String aiCoreId, String factionId, Random random) {
        if (random == null) random = new Random();

        PersonAPI person = Global.getFactory().createPerson();
        person.setFaction(factionId);
        person.setAICoreId(aiCoreId);

        person.getStats().setSkipRefresh(true);

        person.getName().setFirst("Entity");
        person.getName().setLast("");
        person.setGender(FullName.Gender.ANY);

        person.setPortraitSprite("graphics/portraits/entity.png");

        person.setPersonality("reckless");
        person.setRankId("space_captain");
        person.setPostId(null);

        person.getMemoryWithoutUpdate().set(SHROUD_CONSCIOUSNESS_KEY, true);
        person.getMemoryWithoutUpdate().set(AICoreOfficerPlugin.AUTOMATED_POINTS_MULT, ENTITY_AI_CORE_MULT);

        person.getStats().setLevel(7);
        person.getStats().setSkillLevel("darkness_in_the_night", 2);
        person.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);
        applyTargetAnalysis(person.getStats());
        person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
        person.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
        person.getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 2);
        person.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2);

        person.getStats().setSkipRefresh(false);
        person.getStats().refreshCharacterStatsEffects();

        return person;
    }

    public static void refreshEntityOfficerSkills() {
        if (Global.getSector() == null) {
            return;
        }

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet == null || playerFleet.getFleetData() == null) {
            return;
        }

        for (FleetMemberAPI member : playerFleet.getFleetData().getMembersListCopy()) {
            if (member == null) {
                continue;
            }

            PersonAPI captain = member.getCaptain();
            if (!isEntityConsciousnessOfficer(captain)) {
                continue;
            }

            MutableCharacterStatsAPI stats = captain.getStats();
            if (stats == null) {
                continue;
            }

            stats.setSkipRefresh(true);
            applyTargetAnalysis(stats);
            stats.setSkipRefresh(false);
            stats.refreshCharacterStatsEffects();
        }
    }

    private static boolean isEntityConsciousnessOfficer(PersonAPI person) {
        return person != null
                && person.getMemoryWithoutUpdate() != null
                && person.getMemoryWithoutUpdate().getBoolean(SHROUD_CONSCIOUSNESS_KEY);
    }

    private static void applyTargetAnalysis(MutableCharacterStatsAPI stats) {
        if (stats == null) {
            return;
        }

        stats.setSkillLevel(Skills.TARGET_ANALYSIS, TARGET_ANALYSIS_LEVEL);
    }
}
