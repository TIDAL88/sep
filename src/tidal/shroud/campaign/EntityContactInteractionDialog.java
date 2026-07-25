package tidal.shroud.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.characters.PersonAPI;

import java.util.Map;

public class EntityContactInteractionDialog implements InteractionDialogPlugin {
    private static final String LEAVE = "leave";

    private final String response;
    private final String personId;
    private InteractionDialogAPI dialog;

    public EntityContactInteractionDialog(String response, String personId) {
        this.response = response;
        this.personId = personId;
    }

    @Override
    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        TextPanelAPI text = dialog.getTextPanel();
        OptionPanelAPI options = dialog.getOptionPanel();
        dialog.setPromptText("Communicating with The Entity");

        PersonAPI person = null;
        if (Global.getSector() != null && personId != null && !personId.isEmpty()) {
            person = Global.getSector().getImportantPeople().getPerson(personId);
        }
        if (person == null) {
            person = EntityContactManager.ensureEntityPerson();
        }
        if (dialog.getVisualPanel() != null) {
            dialog.getVisualPanel().showPersonInfo(person, false, false);
        }

        if (response != null && !response.isEmpty()) {
            text.addPara(response);
        } else {
            text.addPara("\"...\"");
        }

        options.clearOptions();
        options.addOption("Leave", LEAVE);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (LEAVE.equals(optionData) && dialog != null) {
            dialog.dismiss();
        }
    }

    @Override
    public void optionMousedOver(String optionText, Object optionData) {
    }

    @Override
    public void advance(float amount) {
    }

    @Override
    public void backFromEngagement(EngagementResultAPI battleResult) {
    }

    @Override
    public Object getContext() {
        return null;
    }

    @Override
    public Map<String, MemoryAPI> getMemoryMap() {
        return null;
    }
}
