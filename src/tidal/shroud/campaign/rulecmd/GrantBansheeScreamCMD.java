package tidal.shroud.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetMemberPickerListener;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.BansheeScreamSubsystemHullmod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GrantBansheeScreamCMD extends BaseCommandPlugin {

    public static final String GRANTED_MEMORY_KEY = "$td_banshee_scream_granted";
    private static final String PICKER_TITLE = "Select a ship to receive The Banshee's Scream";
    private static final String PICKER_OK_TEXT = "Imbue selected ship";
    private static final String PICKER_CANCEL_TEXT = "Cancel";
    private static final int PICKER_ROWS = 9;
    private static final int PICKER_COLS = 8;
    private static final float PICKER_ICON_SIZE = 72f;

    @Override
    public boolean execute(String ruleId,
                           InteractionDialogAPI dialog,
                           List<Misc.Token> params,
                           Map<String, MemoryAPI> memoryMap) {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet == null || playerFleet.getFleetData() == null) {
            return false;
        }

        final List<FleetMemberAPI> eligible = getEligibleMembers(playerFleet);
        if (eligible.isEmpty()) {
            if (dialog != null && dialog.getTextPanel() != null) {
                dialog.getTextPanel().addPara(
                        "No eligible ships are available to receive The Banshee's Scream.",
                        Misc.getNegativeHighlightColor()
                );
            }
            return false;
        }

        if (dialog == null) {
            boolean applied = applyHullmodToMember(eligible.get(0), playerFleet);
            if (applied) {
                Global.getSector().getMemoryWithoutUpdate().set(GRANTED_MEMORY_KEY, true);
            }
            return applied;
        }

        dialog.showFleetMemberPickerDialog(
                PICKER_TITLE,
                PICKER_OK_TEXT,
                PICKER_CANCEL_TEXT,
                PICKER_ROWS,
                PICKER_COLS,
                PICKER_ICON_SIZE,
                true,
                false,
                eligible,
                new FleetMemberPickerListener() {
                    @Override
                    public void pickedFleetMembers(List<FleetMemberAPI> members) {
                        if (members == null || members.isEmpty()) {
                            if (dialog.getTextPanel() != null) {
                                dialog.getTextPanel().addPara(
                                        "No ship selected. The manifestation remains unbound.",
                                        Misc.getNegativeHighlightColor()
                                );
                            }
                            return;
                        }

                        FleetMemberAPI selected = members.get(0);
                        boolean applied = applyHullmodToMember(selected, playerFleet);
                        if (applied) {
                            Global.getSector().getMemoryWithoutUpdate().set(GRANTED_MEMORY_KEY, true);
                            if (dialog.getTextPanel() != null) {
                                String shipName = selected.getShipName();
                                if (shipName == null || shipName.isEmpty()) {
                                    shipName = selected.getHullSpec().getHullName();
                                }
                                dialog.getTextPanel().addPara(
                                        "The Banshee's Scream manifestation has been granted to %s.",
                                        Misc.getPositiveHighlightColor(),
                                        shipName
                                );
                            }
                        } else if (dialog.getTextPanel() != null) {
                            dialog.getTextPanel().addPara(
                                    "Failed to bind The Banshee's Scream to the selected ship.",
                                    Misc.getNegativeHighlightColor()
                            );
                        }
                    }

                    @Override
                    public void cancelledFleetMemberPicking() {
                        if (dialog.getTextPanel() != null) {
                            dialog.getTextPanel().addPara(
                                    "Selection cancelled. The manifestation remains unbound.",
                                    Misc.getNegativeHighlightColor()
                            );
                        }
                    }
                }
        );

        return true;
    }

    private static List<FleetMemberAPI> getEligibleMembers(CampaignFleetAPI playerFleet) {
        List<FleetMemberAPI> result = new ArrayList<>();
        for (FleetMemberAPI member : playerFleet.getFleetData().getMembersListCopy()) {
            if (member == null || member.isFighterWing() || member.getVariant() == null) {
                continue;
            }
            result.add(member);
        }
        return result;
    }

    private static boolean applyHullmodToMember(FleetMemberAPI member, CampaignFleetAPI playerFleet) {
        if (member == null || member.getVariant() == null) {
            return false;
        }

        ShipVariantAPI original = member.getVariant();
        ShipVariantAPI working = original;
        if (working.getSource() != VariantSource.REFIT) {
            working = working.clone();
            working.setSource(VariantSource.REFIT);
        }

        if (!working.hasHullMod(BansheeScreamSubsystemHullmod.HULLMOD_ID)) {
            working.addPermaMod(BansheeScreamSubsystemHullmod.HULLMOD_ID);
        }

        if (working != original) {
            member.setVariant(working, false, true);
        }

        member.updateStats();
        playerFleet.getFleetData().setSyncNeeded();
        playerFleet.getFleetData().syncIfNeeded();
        return true;
    }
}
