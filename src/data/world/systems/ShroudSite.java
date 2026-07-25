package data.world.systems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.NascentGravityWellAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantOfficerGeneratorPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import java.awt.Color;
import java.util.Random;

public class ShroudSite {
    public static final String NASCENT_WELL_KEY = "$shroud_well";
    public static final String SITE_MEMORY_KEY = "$shroud_site";
    public static final String GUARD_FLEET_MEMORY_KEY = "$shroud_site_guard_fleet";
    public static final String SITE_FLAG = "$shroudSite";
    public static final String SYSTEM_ID = "shroud_site_system";
    public static final String SYSTEM_NAME = "Unknown Location";
    public static final String SITE_ID = "shroud_site";

    public void generate(SectorAPI sector) {

        StarSystemAPI system = sector.createStarSystem(SYSTEM_ID);
        LocationAPI hyperspace = Global.getSector().getHyperspace();
        SectorEntityToken center=system.initNonStarCenter();
        system.setLightColor(new Color(225, 170, 255, 255));
        center.addTag("ambient_ls");
        system.setName(SYSTEM_NAME);
        system.setType(StarSystemType.DEEP_SPACE);
        system.addTag("theme_unsafe");
        system.addTag("theme_mysterious");
        system.addTag("theme_special");
        system.setBackgroundTextureFilename("graphics/backgrounds/background4.jpg");
        system.setLightColor(new Color(225, 170, 255, 255));
        system.getLocation().set(3900f, 8000);
        system.autogenerateHyperspaceJumpPoints(false,false, true);
        PlanetAPI site = system.addPlanet(SITE_ID, center, "Unknown Site", "irradiated",
                0.0F, 150.0F, 1200.0F, 40.0F);
        site.setId(SITE_ID);
        site.setCustomDescriptionId(SITE_ID);
        site.getMemoryWithoutUpdate().set(SITE_FLAG, true);

        MarketAPI market = site.getMarket();
        market.addCondition("no_atmosphere");
        market.addCondition("very_cold");
        market.addCondition("irradiated");
        market.addCondition("ruins_widespread");
        market.getMemoryWithoutUpdate().set("$ruinsExplored", true);

        site.setOrbit(null);
        site.setLocation(1200.0F, 300.0F);
        addRemnantFleet(system, site);
        Global.getSector().getMemoryWithoutUpdate().set(SITE_MEMORY_KEY, site);

        MagneticFieldTerrainPlugin.MagneticFieldParams fieldParams =
                new MagneticFieldTerrainPlugin.MagneticFieldParams(
                        150.0F,
                        500.0F,
                        site,
                        350.0F,
                        650.0F,
                        new Color(60, 60, 150, 90),
                        1.0F,
                        new Color(130, 60, 150, 130),
                        new Color(150, 30, 120, 150),
                        new Color(200, 50, 130, 190),
                        new Color(250, 70, 150, 240),
                        new Color(200, 80, 130, 255),
                        new Color(75, 0, 160, 255),
                        new Color(127, 0, 255, 255));
        SectorEntityToken magneticField = system.addTerrain("magnetic_field", fieldParams);
        magneticField.setCircularOrbit(site, 0.0F, 0.0F, 75.0F);

        system.generateAnchorIfNeeded();
        NascentGravityWellAPI well = Global.getSector().createNascentGravityWell(site, 50.0F);
        well.addTag("no_entity_tooltip");
        well.setColorOverride(new Color(181, 22, 62));
        hyperspace.addEntity(well);
        well.autoUpdateHyperLocationBasedOnInSystemEntityAtRadius(site, 0.0F);
        Global.getSector().getMemoryWithoutUpdate().set(NASCENT_WELL_KEY, well);
    }
    private void addRemnantFleet(StarSystemAPI system, PlanetAPI site) {
        CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet("remnant", "patrolLarge", true);
        fleet.setName("Automated Defense Fleet");
        fleet.setNoFactionInName(true);

        addShipVariant(fleet, "radiant_Standard", 1);
        addShipVariant(fleet, "brilliant_Standard", 3);
        addShipVariant(fleet, "apex_Standard", 2);
        addShipVariant(fleet, "fulgent_Assault", 4);
        addShipVariant(fleet, "scintilla_Strike", 2);
        addShipVariant(fleet, "glimmer_Assault", 4);
        addShipVariant(fleet, "lumen_Standard", 3);
        fleet.getFleetData().setSyncNeeded();
        fleet.getFleetData().syncIfNeeded();
        fleet.getFleetData().sort();
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
        }
        assignAICores(fleet);

        fleet.getMemoryWithoutUpdate().set("$cfai_makeHostile", true);
        fleet.getMemoryWithoutUpdate().set("$clearCommands_no_remove", true);
        fleet.getMemoryWithoutUpdate().set("$shroud_site_remnant_fleet", true);
        Global.getSector().getMemoryWithoutUpdate().set(GUARD_FLEET_MEMORY_KEY, fleet);
        fleet.setLocation(site.getLocation().x + 350.0F, site.getLocation().y);
        fleet.setFacing(90.0F);
        system.addEntity(fleet);
        fleet.setCircularOrbit(site, 0.0F, 350.0F, 20.0F);
        fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, site, 1.0E10F, "guarding the site");
    }

    private void addShipVariant(CampaignFleetAPI fleet, String variantId, int count) {
        for (int i = 0; i < count; ++i) {
            fleet.getFleetData().addFleetMember(variantId);
        }
    }

    private void assignAICores(CampaignFleetAPI fleet) {
        int brilliantAlphaAssigned = 0;
        int apexAlphaAssigned = 0;

        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            String hullId = member.getHullSpec().getHullId();
            String coreId = "beta_core";

            if ("radiant".equals(hullId)) {
                coreId = "alpha_core";
            } else if ("brilliant".equals(hullId) && brilliantAlphaAssigned < 2) {
                coreId = "alpha_core";
                brilliantAlphaAssigned++;
            } else if ("apex".equals(hullId) && apexAlphaAssigned < 1) {
                coreId = "alpha_core";
                apexAlphaAssigned++;
            }

            PersonAPI captain = createAICoreOfficer(coreId);
            member.setCaptain(captain);
            RemnantOfficerGeneratorPlugin.integrateAndAdaptCoreForAIFleet(member);
            if ("radiant".equals(hullId)) {
                fleet.setCommander(captain);
            }
        }
    }

    private PersonAPI createAICoreOfficer(String coreId) {
        AICoreOfficerPlugin plugin = Misc.getAICoreOfficerPlugin(coreId);
        if (plugin != null) {
            return plugin.createPerson(coreId, "remnant", new Random());
        }

        PersonAPI person = Global.getFactory().createPerson();
        person.setFaction("remnant");
        person.setAICoreId(coreId);
        return person;
    }

    public static CampaignFleetAPI getGuardFleet() {
        Object fleet = Global.getSector().getMemoryWithoutUpdate().get(GUARD_FLEET_MEMORY_KEY);
        return fleet instanceof CampaignFleetAPI ? (CampaignFleetAPI) fleet : null;
    }

    public static boolean isGuardFleetAlive() {
        CampaignFleetAPI fleet = getGuardFleet();
        return fleet != null
                && fleet.isAlive()
                && !fleet.isDespawning()
                && !fleet.isEmpty()
                && fleet.getContainingLocation() != null;
    }
}
