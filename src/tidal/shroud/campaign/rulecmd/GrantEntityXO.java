package tidal.shroud.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import second_in_command.SCUtils;
import second_in_command.specs.SCOfficer;
import tidal.shroud.campaign.EntityContactManager;

import java.util.List;
import java.util.Map;

public class GrantEntityXO extends BaseCommandPlugin {

    public static final String XO_GIVEN_FLAG = "$shroud_sic_xo_given";

    @Override
    public boolean execute(String ruleId,
                           InteractionDialogAPI dialog,
                           List<Misc.Token> params,
                           Map<String, MemoryAPI> memoryMap) {

        if (!Global.getSettings().getModManager().isModEnabled("second_in_command")) {
            EntityContactManager.ensureEntityContact();
            return true;
        }

        MemoryAPI global = Global.getSector().getMemoryWithoutUpdate();
        if ("EntityTaken".equalsIgnoreCase(ruleId)) {
            EntityContactManager.ensureEntityContact();
            return true;
        }
        if (!EntityContactManager.isFirstTrialCompleted(global)) {
            EntityContactManager.ensureEntityContact();
            return true;
        }
        if (!global.getBoolean(XO_GIVEN_FLAG)) {
            PersonAPI person = Global.getFactory().createPerson();
            person.setFaction(Global.getSector().getPlayerFleet().getFaction().getId());
            person.setId("resonance_entity");
            person.setName(new FullName("The Entity", "", FullName.Gender.MALE));
            person.setPortraitSprite("graphics/portraits/entity.png");

            SCOfficer officer = new SCOfficer(person, "td_abyssdiver");
            officer.increaseLevel(1);

            SCUtils.getPlayerData().addOfficerToFleet(officer);
            SCUtils.getPlayerData().setOfficerInEmptySlotIfAvailable(officer);

            global.set(XO_GIVEN_FLAG, true);

            if (dialog != null && dialog.getTextPanel() != null) {
                SCUtils.showSkillOverview(dialog, officer);
            }
        }

        EntityContactManager.ensureEntityContact();
        return true;
    }
}
