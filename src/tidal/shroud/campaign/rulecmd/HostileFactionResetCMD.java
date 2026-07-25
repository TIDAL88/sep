package tidal.shroud.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import java.util.List;
import java.util.Map;

public class HostileFactionResetCMD extends BaseCommandPlugin {
    private static final float HOSTILE = -0.5f;
    private static final float INHOSPITABLE = -0.25f;
    public static final String KEY_RESET_HEGEMONY = "$shroud_reset_hegemony";
    public static final String KEY_RESET_LEAGUE = "$shroud_reset_league";
    public static final String KEY_RESET_DIKTAT = "$shroud_reset_diktat";

    private static final String FACTION_HEGEMONY = "hegemony";
    private static final String FACTION_LEAGUE = "persean";
    private static final String FACTION_DIKTAT = "sindrian_diktat";

    @Override
    public boolean execute(String ruleId,
                           InteractionDialogAPI dialog,
                           List<Misc.Token> params,
                           Map<String, MemoryAPI> memoryMap) {
        MemoryAPI global = memoryMap != null ? memoryMap.get("global") : null;
        boolean changed = false;

        boolean hasKeySelection = global != null && (
                global.getBoolean(KEY_RESET_HEGEMONY)
                        || global.getBoolean(KEY_RESET_LEAGUE)
                        || global.getBoolean(KEY_RESET_DIKTAT)
        );

        if (hasKeySelection) {
            if (global.getBoolean(KEY_RESET_HEGEMONY)) {
                changed |= resetFactionToInhospitable(FACTION_HEGEMONY);
            }
            if (global.getBoolean(KEY_RESET_LEAGUE)) {
                changed |= resetFactionToInhospitable(FACTION_LEAGUE);
            }
            if (global.getBoolean(KEY_RESET_DIKTAT)) {
                changed |= resetFactionToInhospitable(FACTION_DIKTAT);
            }
            return changed;
        }

        changed |= resetFactionToInhospitable(FACTION_HEGEMONY);
        changed |= resetFactionToInhospitable(FACTION_LEAGUE);
        changed |= resetFactionToInhospitable(FACTION_DIKTAT);
        return changed;
    }

    private boolean resetFactionToInhospitable(String factionId) {
        FactionAPI faction = Global.getSector().getFaction(factionId);
        if (faction == null) {
            return false;
        }

        if (faction.getRelToPlayer().getRel() <= HOSTILE) {
            faction.getRelToPlayer().setRel(INHOSPITABLE);
            return true;
        }

        return false;
    }
}
