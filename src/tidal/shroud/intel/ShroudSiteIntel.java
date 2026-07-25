package tidal.shroud.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.fs.starfarer.api.util.Misc.getHighlightColor;
import static com.fs.starfarer.api.util.Misc.makeImportant;
import static com.fs.starfarer.api.util.Misc.makeUnimportant;

public class ShroudSiteIntel extends BaseIntelPlugin {

    private static final String IMPORTANT_KEY = "shroud_site";
    private static final String DARK_PAST_COMPLETE_KEY = "$darkpast_completed";
    private static final String SITE_EXPLORED_KEY = "$siteshroudExplored";

    private final SectorEntityToken site;

    public ShroudSiteIntel(SectorEntityToken target) {
        SectorEntityToken stored = null;
        Object memoryTarget = Global.getSector().getMemoryWithoutUpdate().get("$shroud_site");
        if (memoryTarget instanceof SectorEntityToken) {
            stored = (SectorEntityToken) memoryTarget;
        }

        this.site = stored != null ? stored : target;

        if (this.site != null) {
            makeImportant(this.site, IMPORTANT_KEY);
        }
    }

    @Override
    public String getName() {
        return "A Dark Past";
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        info.addPara(getName(), getTitleColor(mode), 0f);

        if (site != null && site.getStarSystem() != null) {
            info.addPara("Arroyo provided the coordinates to this site.",
                    3f, Misc.getTextColor(), getHighlightColor(),
                    site.getStarSystem().getNameWithLowercaseType());
        } else {
            info.addPara("Location provided by Arroyo.", 3f);
        }
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        info.addPara("Arroyo has provided the location of something related to the second AI War.", 0f);

        if (site != null && site.getContainingLocation() != null) {
            info.addSpacer(10f);
            info.addPara("Location: %s", 0f, getHighlightColor(),
                    site.getContainingLocation().getNameWithLowercaseType());
        }

        info.addSpacer(10f);
        info.addPara("Investigate the planet.", 0f);
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        return site;
    }

    @Override
    public List<ArrowData> getArrowData(SectorMapAPI map) {
        List<ArrowData> arrows = new ArrayList<>();
        if (site != null) {
            ArrowData arrow = new ArrowData(Global.getSector().getPlayerFleet(), site);
            arrow.color = getHighlightColor();
            arrow.width = 2f;
            arrows.add(arrow);
        }
        return arrows;
    }

    @Override
    public String getIcon() {
        return "graphics/icons/intel/discovered_entity.png";
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = new LinkedHashSet<>(super.getIntelTags(map));
        tags.add(Tags.INTEL_MISSIONS);
        return tags;
    }

    @Override
    public boolean isImportant() {
        return true;
    }

    @Override
    public boolean hasSmallDescription() {
        return true;
    }

    @Override
    public boolean hasLargeDescription() {
        return false;
    }

    @Override
    protected void notifyEnded() {
        super.notifyEnded();
        if (site != null) {
            makeUnimportant(site, IMPORTANT_KEY);
        }
    }

    @Override
    public boolean shouldRemoveIntel() {
        if (Global.getSector().getMemoryWithoutUpdate().getBoolean(DARK_PAST_COMPLETE_KEY)) {
            return true;
        }
        if (site != null && site.getMemoryWithoutUpdate().getBoolean(SITE_EXPLORED_KEY)) {
            return true;
        }
        return super.shouldRemoveIntel();
    }
}
