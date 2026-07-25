package tidal.shroud;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.BaseCampaignPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.util.IntervalUtil;
import data.RedactedSectorGen;
import data.hullmods.DwellerHullmodTidal;

import data.world.systems.ShroudSite;
import tidal.shroud.campaign.EntityContactManager;
import tidal.shroud.campaign.ShroudSiteGuardInteraction;
import tidal.shroud.campaign.rulecmd.ShroudConsciousnessOfficerPlugin;
import tidal.shroud.scripts.ships.InsatiableHungerShipCreator;
import tidal.shroud.scripts.ships.ShroudedBansheeShipCreator;
import tidal.shroud.scripts.ships.ShroudedBrainShipCreator;

public class sicshroud extends BaseModPlugin {


    @Override
    public void onApplicationLoad() {
        DwellerHullmodTidal.SHIP_CREATORS.put("shrouded_brain", new ShroudedBrainShipCreator());
        DwellerHullmodTidal.SHIP_CREATORS.put("shrouded_banshee", new ShroudedBansheeShipCreator());
        DwellerHullmodTidal.SHIP_CREATORS.put("insatiable_hunger", new InsatiableHungerShipCreator());
    }

    @Override
    public void onGameLoad(boolean newGame) {
        super.onGameLoad(newGame);
        new RedactedSectorGen().generate(Global.getSector());

        Global.getSector().registerPlugin(new BaseCampaignPlugin() {
            @Override
            public PluginPick<AICoreOfficerPlugin> pickAICoreOfficerPlugin(String commodityId) {
                if ("containment_unit_awakened".equals(commodityId)) {
                    return new PluginPick<>(
                            new ShroudConsciousnessOfficerPlugin(),
                            PickPriority.MOD_SPECIFIC
                    );
                }
                return null;
            }

            @Override
            public PluginPick<InteractionDialogPlugin> pickInteractionDialogPlugin(SectorEntityToken interactionTarget) {
                if (interactionTarget != null
                        && interactionTarget.getMemoryWithoutUpdate().getBoolean(ShroudSite.SITE_FLAG)
                        && ShroudSite.isGuardFleetAlive()) {
                    return new PluginPick<>(
                            new ShroudSiteGuardInteraction(interactionTarget),
                            PickPriority.MOD_SPECIFIC
                    );
                }
                return null;
            }
        });

        ShroudConsciousnessOfficerPlugin.refreshEntityOfficerSkills();

        Global.getSector().addScript(new EveryFrameScript() {
            private IntervalUtil entityContactInterval = new IntervalUtil(1f, 2f);

            @Override
            public boolean isDone() {
                return false;
            }

            @Override
            public boolean runWhilePaused() {
                return false;
            }

            @Override
            public void advance(float amount) {
                if (Global.getSector() == null) return;

                if (entityContactInterval == null) {
                    entityContactInterval = new IntervalUtil(1f, 2f);
                }

                entityContactInterval.advance(amount);
                if (entityContactInterval.intervalElapsed()) {
                    EntityContactManager.ensureEntityContact();
                }
            }
        });

        EntityContactManager.ensureEntityContact();
    }

    @Override
    public void onNewGame() {
    }
}