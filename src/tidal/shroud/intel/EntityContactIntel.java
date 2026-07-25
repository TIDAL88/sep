package tidal.shroud.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import tidal.shroud.campaign.EntityContactManager;

import java.util.LinkedHashSet;
import java.util.Set;

public class EntityContactIntel extends BaseIntelPlugin {

    public static final String COMM_BUTTON_ID = "td_entity_contact_comm";

    private final String personId;

    public EntityContactIntel(String personId) {
        this.personId = personId;
    }

    public boolean isForPerson(String id) {
        return personId != null && personId.equals(id);
    }

    private PersonAPI getPerson() {
        if (Global.getSector() == null || personId == null || personId.isEmpty()) {
            return null;
        }
        return Global.getSector().getImportantPeople().getPerson(personId);
    }

    @Override
    public String getName() {
        return "Contact: The Entity";
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, IntelInfoPlugin.ListInfoMode mode) {
        info.addPara(getName(), getTitleColor(mode), 0f);
        info.addPara("The containment unit is stable, allowing communication.", 3f);
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        PersonAPI person = getPerson();
        if (person != null && person.getPortraitSprite() != null && !person.getPortraitSprite().isEmpty()) {
            info.addImage(person.getPortraitSprite(), width, 128f, 0f);
            info.addSpacer(10f);
        }

        info.addPara("The entity remains available at all times.", 0f);
        info.addPara("It responds only when it chooses to.", 3f);
        if (Global.getSector() != null
                && EntityContactManager.isFirstTrialStarted(Global.getSector().getMemoryWithoutUpdate())) {
            info.addPara("The path to power starts with this trial.", 3f);
        }
        info.addSpacer(12f);
        info.addButton(
                "Attempt to communicate with it",
                COMM_BUTTON_ID,
                Misc.getBasePlayerColor(),
                Misc.getDarkPlayerColor(),
                width,
                24f,
                0f
        );
    }

    @Override
    public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
        if (!COMM_BUTTON_ID.equals(buttonId)) {
            return;
        }

        if (!EntityContactManager.hasAwakenedContainmentUnit()) {
            if (ui != null) {
                ui.updateUIForItem(this);
            }
            return;
        }

        EntityContactManager.ensureEntityContact();
        String response = EntityContactManager.attemptCommunication();

        EntityContactManager.showCommunicationDialog(response);

        if (ui != null) {
            ui.updateUIForItem(this);
        }
    }

    @Override
    public String getIcon() {
        PersonAPI person = getPerson();
        if (person != null && person.getPortraitSprite() != null && !person.getPortraitSprite().isEmpty()) {
            return person.getPortraitSprite();
        }
        return "graphics/icons/intel/discovered_entity.png";
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = new LinkedHashSet<String>(super.getIntelTags(map));
        tags.add(Tags.INTEL_CONTACTS);
        return tags;
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
    public boolean shouldRemoveIntel() {
        return getPerson() == null || !EntityContactManager.hasAwakenedContainmentUnit();
    }

    @Override
    public boolean isHidden() {
        return !EntityContactManager.hasAwakenedContainmentUnit();
    }
}
