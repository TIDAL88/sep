package tidal.shroud.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import java.util.List;
import java.util.Map;

public class TTFavorCMD extends BaseCommandPlugin {
    public static final float INHOSPITABLE = -0.25f;
    public static final float NEUTRAL = 0f;

    @Override
    public boolean execute(String ruleId,
                           InteractionDialogAPI dialog,
                           List<Misc.Token> params,
                           Map<String, MemoryAPI> memoryMap) {
        FactionAPI tt = Global.getSector().getFaction("tritachyon");
        if (tt == null) {
            return false;
        }

        float rel = tt.getRelToPlayer().getRel();
        if (rel >= INHOSPITABLE) {
            return false;
        }

        tt.getRelToPlayer().setRel(NEUTRAL);
        return true;
    }
}
