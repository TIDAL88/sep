package tidal.shroud.campaign.rulecmd;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import tidal.shroud.campaign.EntityContactManager;

import java.util.List;
import java.util.Map;

public class EntityCommunicationCMD extends BaseCommandPlugin {

    public static final String ACTION_ENSURE_CONTACT = "ensureContact";
    public static final String ACTION_COMMUNICATE = "communicate";

    @Override
    public boolean execute(String ruleId,
                           InteractionDialogAPI dialog,
                           List<Misc.Token> params,
                           Map<String, MemoryAPI> memoryMap) {
        String action = params != null && !params.isEmpty()
                ? params.get(0).getString(memoryMap)
                : ACTION_COMMUNICATE;

        if (ACTION_ENSURE_CONTACT.equalsIgnoreCase(action)) {
            EntityContactManager.ensureEntityContact();
            return true;
        }

        String response = EntityContactManager.attemptCommunication();

        if (dialog != null && dialog.getTextPanel() != null) {
            dialog.getTextPanel().addPara(response);
        } else {
            EntityContactManager.showCommunicationDialog(response);
        }

        return true;
    }
}
