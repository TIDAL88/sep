package tidal.shroud.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import java.util.List;
import java.util.Map;

public class ShroudSiteIntelCMD extends BaseCommandPlugin {

    @Override
    public boolean execute(String ruleId,
                           InteractionDialogAPI dialog,
                           List<Misc.Token> params,
                           Map<String, MemoryAPI> memoryMap) {

        if (dialog == null) {
            return false;
        }

        SectorEntityToken entity = dialog.getInteractionTarget();

        tidal.shroud.intel.ShroudSiteIntel intel =
                new tidal.shroud.intel.ShroudSiteIntel(entity);

        Global.getSector().getIntelManager().addIntel(intel, true);

        MemoryAPI global = memoryMap.get("global");
        if (global != null) {
            global.set("$shroudQuest_stage", 1);
        }

        return true;
    }
}