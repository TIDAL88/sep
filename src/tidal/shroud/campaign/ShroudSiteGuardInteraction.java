package tidal.shroud.campaign;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import data.world.systems.ShroudSite;
import java.util.Map;

public class ShroudSiteGuardInteraction implements InteractionDialogPlugin {
    private static final String ENGAGE = "engage";
    private static final String LEAVE = "leave";

    private final SectorEntityToken site;
    private InteractionDialogAPI dialog;
    private TextPanelAPI text;
    private OptionPanelAPI options;

    public ShroudSiteGuardInteraction(SectorEntityToken site) {
        this.site = site;
    }

    @Override
    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        this.text = dialog.getTextPanel();
        this.options = dialog.getOptionPanel();

        if (!ShroudSite.isGuardFleetAlive()) {
            RuleBasedInteractionDialogPluginImpl plugin = new RuleBasedInteractionDialogPluginImpl();
            dialog.setPlugin(plugin);
            dialog.setInteractionTarget(site);
            plugin.init(dialog);
            return;
        }

        options.clearOptions();
        text.addPara("A powerful Remnant defense fleet moves to intercept before you can approach the site.");
        text.addPara("The automated warship fleet blocks all access to the planet, it must be defeated before you can access it.");
        options.addOption("Engage the defense fleet", ENGAGE);
        options.addOption("Disengage", LEAVE);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (optionData == null) {
            return;
        }

        if (ENGAGE.equals(optionData)) {
            CampaignFleetAPI fleet = ShroudSite.getGuardFleet();
            if (fleet == null || !ShroudSite.isGuardFleetAlive()) {
                RuleBasedInteractionDialogPluginImpl plugin = new RuleBasedInteractionDialogPluginImpl();
                dialog.setPlugin(plugin);
                dialog.setInteractionTarget(site);
                plugin.init(dialog);
                return;
            }

            FleetInteractionDialogPluginImpl plugin = new FleetInteractionDialogPluginImpl();
            dialog.setInteractionTarget(fleet);
            dialog.setPlugin(plugin);
            plugin.init(dialog);
            return;
        }

        if (LEAVE.equals(optionData)) {
            dialog.dismissAsCancel();
        }
    }

    @Override
    public void optionMousedOver(String optionText, Object optionData) {
    }

    @Override
    public void advance(float amount) {
    }

    @Override
    public void backFromEngagement(EngagementResultAPI result) {
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
