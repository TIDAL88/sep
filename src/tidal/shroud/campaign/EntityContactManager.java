package tidal.shroud.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseMissionHub;
import tidal.shroud.intel.EntityContactIntel;

import java.util.ArrayList;
import java.util.List;

public class EntityContactManager {

    public static final String AWAKENED_CORE_ID = "containment_unit_awakened";

    public static final String ENTITY_ID = "resonance_entity";
    public static final String ENTITY_NAME = "The Entity";
    public static final String ENTITY_PORTRAIT = "graphics/portraits/entity.png";

    public static final String ENTITY_CONTACT_READY_KEY = "$td_entity_contact_ready";
    public static final String ENTITY_FIRST_COMMUNION_KEY = "$td_entity_first_communion_done";
    public static final String FIRST_TRIAL_STARTED_KEY = "$td_first_trial_started";
    public static final String FIRST_TRIAL_COMPLETED_KEY = "$td_first_trial_completed";
    public static final String PRIMORDIAL_FLEET_DEFEATED_KEY = "$td_primordial_fleet_defeated";
    public static final String FIRST_TRIAL_BOUNTY_FAIL_COUNT_KEY = "$td_entity_first_trial_bounty_fail_count";
    private static final String ENTITY_CONSCIOUSNESS_MEMORY_KEY = "$shroud_consciousness";
    private static final String ENTITY_FIRST_TRIAL_ROLL_CYCLE_KEY = "$td_entity_first_trial_roll_cycle";
    private static final String ENTITY_FIRST_TRIAL_ROLL_MONTH_KEY = "$td_entity_first_trial_roll_month";
    private static final String ENTITY_FIRST_TRIAL_ROLL_SUCCESS_KEY = "$td_entity_first_trial_roll_success";
    private static final String ENTITY_FIRST_TRIAL_ROLL_FAIL_COUNT_KEY = "$td_entity_first_trial_roll_fail_count";
    private static final String ENTITY_FIRST_TRIAL_BOUNTY_FAIL_COUNT_KEY = FIRST_TRIAL_BOUNTY_FAIL_COUNT_KEY;
    private static final String ENTITY_FIRST_TRIAL_FAIL_LINE_SHOWN_KEY = "$td_entity_first_trial_fail_line_shown";
    private static final String ENTITY_POST_TRIAL_ACK_LINE_SHOWN_KEY = "$td_entity_post_trial_ack_line_shown";
    private static final float ENTITY_FIRST_TRIAL_BASE_MONTHLY_CHANCE = 0.10f;
    private static final float ENTITY_FIRST_TRIAL_CHANCE_PER_FAILED_MONTH = 0.10f;

    private static final String ENTITY_TAG = "td_entity_contact";

    public static void ensureEntityContact() {
        if (Global.getSector() == null) {
            return;
        }

        PersonAPI entity = ensureEntityPerson();

        entity.getMemoryWithoutUpdate().unset(BaseMissionHub.CONTACT_SUSPENDED);
        if (BaseMissionHub.get(entity) == null) {
            BaseMissionHub.set(entity, new BaseMissionHub(entity));
        }

        boolean contactReady = hasAwakenedContainmentUnit();
        if (contactReady) {
            ensureEntityIntel(entity);
        } else {
            removeEntityIntel(entity.getId());
        }

        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
        memory.set(ENTITY_CONTACT_READY_KEY, contactReady);
    }

    public static String attemptCommunication() {
        if (Global.getSector() == null) {
            return "\"You are not worthy of my attention\"";
        }

        ensureEntityContact();

        MemoryAPI global = Global.getSector().getMemoryWithoutUpdate();
        if (isFirstTrialCompleted(global)) {
            if (!global.getBoolean(ENTITY_POST_TRIAL_ACK_LINE_SHOWN_KEY)) {
                global.set(ENTITY_POST_TRIAL_ACK_LINE_SHOWN_KEY, true);
                return "\"You have proven yourself sufficient.\"";
            }
            return "\"More will come, be prepared.\"";
        }

        boolean questStarted = isFirstTrialStarted(global);
        if (questStarted) {
            return "\"The path to power starts with this trial.\"";
        }

        if (!rollFirstTrialChanceForCurrentMonth(global)) {
            if (!global.getBoolean(ENTITY_FIRST_TRIAL_FAIL_LINE_SHOWN_KEY)) {
                global.set(ENTITY_FIRST_TRIAL_FAIL_LINE_SHOWN_KEY, true);
                return "\"You are not worthy of my attention.\"";
            }
            return "\"Your persistence is amusing.\"";
        }

        global.set(ENTITY_FIRST_COMMUNION_KEY, true);
        global.set(FIRST_TRIAL_STARTED_KEY, true);
        return "\"Prove yourself worthy by completing your first trial.\"";
    }

    public static boolean isFirstTrialStarted(MemoryAPI global) {
        if (isFirstTrialCompleted(global)) {
            return false;
        }
        return global != null && global.getBoolean(FIRST_TRIAL_STARTED_KEY);
    }

    public static boolean isFirstTrialCompleted(MemoryAPI global) {
        return global != null
                && (global.getBoolean(FIRST_TRIAL_COMPLETED_KEY)
                || global.getBoolean(PRIMORDIAL_FLEET_DEFEATED_KEY));
    }

    public static void markFirstTrialCompleted() {
        if (Global.getSector() == null) {
            return;
        }
        MemoryAPI global = Global.getSector().getMemoryWithoutUpdate();
        if (global == null) {
            return;
        }

        global.set(FIRST_TRIAL_COMPLETED_KEY, true);
        global.set(PRIMORDIAL_FLEET_DEFEATED_KEY, true);
        global.unset(FIRST_TRIAL_STARTED_KEY);
        global.unset(ENTITY_FIRST_TRIAL_ROLL_FAIL_COUNT_KEY);
    }

    private static boolean rollFirstTrialChanceForCurrentMonth(MemoryAPI global) {
        if (global == null || Global.getSector() == null || Global.getSector().getClock() == null) {
            return false;
        }

        int cycle = Global.getSector().getClock().getCycle();
        int month = Global.getSector().getClock().getMonth();

        boolean hasCycle = global.contains(ENTITY_FIRST_TRIAL_ROLL_CYCLE_KEY);
        boolean hasMonth = global.contains(ENTITY_FIRST_TRIAL_ROLL_MONTH_KEY);
        if (hasCycle && hasMonth) {
            int lastCycle = global.getInt(ENTITY_FIRST_TRIAL_ROLL_CYCLE_KEY);
            int lastMonth = global.getInt(ENTITY_FIRST_TRIAL_ROLL_MONTH_KEY);
            if (lastCycle == cycle && lastMonth == month) {
                return global.getBoolean(ENTITY_FIRST_TRIAL_ROLL_SUCCESS_KEY);
            }
        }

        int failedMonths = 0;
        if (global.contains(ENTITY_FIRST_TRIAL_ROLL_FAIL_COUNT_KEY)) {
            failedMonths = Math.max(0, global.getInt(ENTITY_FIRST_TRIAL_ROLL_FAIL_COUNT_KEY));
        }
        int bountyFailures = 0;
        if (global.contains(ENTITY_FIRST_TRIAL_BOUNTY_FAIL_COUNT_KEY)) {
            bountyFailures = Math.max(0, global.getInt(ENTITY_FIRST_TRIAL_BOUNTY_FAIL_COUNT_KEY));
        }

        float chance = ENTITY_FIRST_TRIAL_BASE_MONTHLY_CHANCE
                + ((failedMonths + bountyFailures) * ENTITY_FIRST_TRIAL_CHANCE_PER_FAILED_MONTH);
        chance = Math.max(0f, Math.min(1f, chance));

        boolean success = Math.random() < chance;
        global.set(ENTITY_FIRST_TRIAL_ROLL_CYCLE_KEY, cycle);
        global.set(ENTITY_FIRST_TRIAL_ROLL_MONTH_KEY, month);
        global.set(ENTITY_FIRST_TRIAL_ROLL_SUCCESS_KEY, success);
        global.set(
                ENTITY_FIRST_TRIAL_ROLL_FAIL_COUNT_KEY,
                success ? 0 : failedMonths + 1
        );
        global.set(ENTITY_FIRST_TRIAL_FAIL_LINE_SHOWN_KEY, false);
        return success;
    }

    public static void showCommunicationDialog(String response) {
        if (Global.getSector() == null || Global.getSector().getCampaignUI() == null) {
            return;
        }

        CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
        SectorEntityToken target = Global.getSector().getPlayerFleet();
        String text = (response == null || response.isEmpty()) ? "\"...\"" : response;
        boolean shown = false;

        if (target != null) {
            shown = campaignUI.showInteractionDialogFromCargo(
                    new EntityContactInteractionDialog(text, ENTITY_ID),
                    target,
                    null
            );

            if (!shown && !campaignUI.isShowingDialog()) {
                shown = campaignUI.showInteractionDialog(
                        new EntityContactInteractionDialog(text, ENTITY_ID),
                        target
                );
            }
        }

        if (!shown) {
            campaignUI.showMessageDialog(text);
        }
    }

    public static boolean hasAwakenedContainmentUnit() {
        if (Global.getSector() == null) {
            return false;
        }

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet == null) {
            return false;
        }

        if (hasAwakenedContainmentUnitInCargo(playerFleet.getCargo())) {
            return true;
        }

        return hasAwakenedContainmentUnitInFleet(playerFleet);
    }

    private static boolean hasAwakenedContainmentUnitInCargo(CargoAPI cargo) {
        if (cargo == null) {
            return false;
        }

        if (cargo.getCommodityQuantity(AWAKENED_CORE_ID) > 0f) {
            return true;
        }

        for (CargoStackAPI stack : cargo.getStacksCopy()) {
            if (stack == null) {
                continue;
            }
            if (stack.getCommodityId() != null && AWAKENED_CORE_ID.equals(stack.getCommodityId()) && stack.getSize() > 0f) {
                return true;
            }
            if (!stack.isSpecialStack() || stack.getSpecialDataIfSpecial() == null) {
                continue;
            }
            if (AWAKENED_CORE_ID.equals(stack.getSpecialDataIfSpecial().getId()) && stack.getSize() > 0f) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasAwakenedContainmentUnitInFleet(CampaignFleetAPI fleet) {
        if (fleet == null || fleet.getFleetData() == null) {
            return false;
        }

        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            if (member == null) {
                continue;
            }

            PersonAPI captain = member.getCaptain();
            if (captain == null) {
                continue;
            }

            if (AWAKENED_CORE_ID.equals(captain.getAICoreId())) {
                return true;
            }

            if (captain.getMemoryWithoutUpdate() != null
                    && captain.getMemoryWithoutUpdate().getBoolean(ENTITY_CONSCIOUSNESS_MEMORY_KEY)) {
                return true;
            }
        }

        return false;
    }

    public static PersonAPI ensureEntityPerson() {
        ImportantPeopleAPI importantPeople = Global.getSector().getImportantPeople();
        PersonAPI entity = importantPeople.getPerson(ENTITY_ID);
        if (entity != null) {
            if (entity.getFaction() == null || !Factions.INDEPENDENT.equals(entity.getFaction().getId())) {
                entity.setFaction(Factions.INDEPENDENT);
            }
            if (entity.getPortraitSprite() == null || entity.getPortraitSprite().isEmpty()) {
                entity.setPortraitSprite(ENTITY_PORTRAIT);
            }
            if (entity.getName() == null || entity.getName().getFirst() == null || entity.getName().getFirst().isEmpty()) {
                entity.setName(new FullName(ENTITY_NAME, "", FullName.Gender.ANY));
            }
            entity.addTag(ENTITY_TAG);
            entity.setImportance(PersonImportance.VERY_HIGH);
            return entity;
        }

        entity = Global.getFactory().createPerson();
        entity.setId(ENTITY_ID);
        entity.setFaction(Factions.INDEPENDENT);
        entity.setName(new FullName(ENTITY_NAME, "", FullName.Gender.ANY));
        entity.setPortraitSprite(ENTITY_PORTRAIT);
        entity.setRankId(Ranks.UNKNOWN);
        entity.setPostId(Ranks.POST_UNKNOWN);
        entity.setImportance(PersonImportance.VERY_HIGH);
        entity.addTag(ENTITY_TAG);

        importantPeople.addPerson(entity);
        return entity;
    }

    private static void ensureEntityIntel(PersonAPI entity) {
        for (IntelInfoPlugin intel : Global.getSector().getIntelManager().getIntel(EntityContactIntel.class)) {
            EntityContactIntel contact = (EntityContactIntel) intel;
            if (!contact.isEnding() && !contact.isEnded() && contact.isForPerson(entity.getId())) {
                return;
            }
        }

        Global.getSector().getIntelManager().addIntel(new EntityContactIntel(entity.getId()), false);
    }

    private static void removeEntityIntel(String entityId) {
        List<IntelInfoPlugin> toRemove = new ArrayList<>();
        for (IntelInfoPlugin intel : Global.getSector().getIntelManager().getIntel(EntityContactIntel.class)) {
            EntityContactIntel contact = (EntityContactIntel) intel;
            if (!contact.isForPerson(entityId)) {
                continue;
            }
            toRemove.add(contact);
        }
        for (IntelInfoPlugin intel : toRemove) {
            Global.getSector().getIntelManager().removeIntel(intel);
        }
    }

}
