package tidal.shroud.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import java.util.List;
import java.util.Map;

public class GrantParagonClassCMD extends BaseCommandPlugin {
    private static final String PARAGON_VARIANT_ID = "paragon_Elite";

    @Override
    public boolean execute(String ruleId,
                           InteractionDialogAPI dialog,
                           List<Misc.Token> params,
                           Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) {
            return false;
        }

        SectorEntityToken target = dialog.getInteractionTarget();
        if (target == null) {
            return false;
        }

        MarketAPI market = target.getMarket();
        if (market == null || !market.hasSubmarket(Submarkets.SUBMARKET_STORAGE)) {
            return false;
        }

        SubmarketAPI storage = market.getSubmarket(Submarkets.SUBMARKET_STORAGE);
        if (storage == null || storage.getCargo() == null) {
            return false;
        }

        FleetDataAPI mothballed = storage.getCargo().getMothballedShips();
        int before = mothballed.getNumMembers();

        storage.getCargo().addMothballedShip(FleetMemberType.SHIP, PARAGON_VARIANT_ID, null);

        FleetMemberAPI added = null;
        for (FleetMemberAPI member : mothballed.getMembersListCopy()) {
            if (PARAGON_VARIANT_ID.equals(member.getVariant().getHullVariantId())) {
                added = member;
            }
        }

        if (added != null) {
            added.getRepairTracker().setCR(added.getRepairTracker().getMaxCR());
        }

        mothballed.setSyncNeeded();
        mothballed.syncIfNeeded();
        mothballed.sort();

        if (mothballed.getNumMembers() <= before) {
            return false;
        }

        return true;
    }
}
