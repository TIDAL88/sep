package data;

import com.fs.starfarer.api.campaign.SectorAPI;
import data.world.systems.ShroudSite;

public class RedactedSectorGen implements com.fs.starfarer.api.campaign.SectorGeneratorPlugin {
    @Override
    public void generate(SectorAPI sector) {
        if (sector.getStarSystem("shroud_site_system") == null) new ShroudSite().generate(sector);
    }
}