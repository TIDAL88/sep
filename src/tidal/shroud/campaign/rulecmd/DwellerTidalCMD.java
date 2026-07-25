package tidal.shroud.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomProductionPickerDelegateImpl;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FactionProductionAPI;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin;
import com.fs.starfarer.api.campaign.FleetInflater;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.SpecialItemPlugin;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode;
import com.fs.starfarer.api.campaign.FactionProductionAPI.ProductionItemType;
import com.fs.starfarer.api.campaign.impl.items.ShroudedHullmodItemPlugin;
import com.fs.starfarer.api.campaign.impl.items.ShroudedSubstratePlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.ShipFilter;
import com.fs.starfarer.api.fleet.ShipRolePick;
import com.fs.starfarer.api.impl.campaign.AbyssalLightEntityPlugin;
import com.fs.starfarer.api.impl.campaign.AbyssalLightEntityPlugin.DespawnType;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.util.ListMap;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import tidal.shroud.campaign.EntityContactManager;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class DwellerTidalCMD extends BaseCommandPlugin {

    public static String SHROUDED_TENDRIL = "shrouded_tendril";
    public static String SHROUDED_EYE = "shrouded_eye";
    public static String SHROUDED_MAELSTROM = "shrouded_maelstrom";
    public static String SHROUDED_MAW = "shrouded_maw";
    private static final String AOTD_VOK_MOD_ID = "aotd_vok";
    private static final String AOTD_SUBSTRATE_ITEM_ID = "aotd_shrouded_substrate";
    private static final String AOTD_BETTER_CONTAINMENT_MEMORY_KEY =
            "$aotd_shroud_better_containment_methods";
    private static final String AOTD_GREAT_FLEET_MEMORY_KEY = "$aotd_shroud_great_fleet";
    private static final String ULTRA_SENSORS_OPTION_ID = "abyssalLight_ultrasensors";
    private static final String PRIMORDIAL_OPTION_ID = "abyssalLight_primordial";
    public static final String PRIMORDIAL_FLEET_DEFEATED_KEY = "$td_primordial_fleet_defeated";
    private static final String PRIMORDIAL_DEFEAT_TRIGGER = "TDPrimordialFleetDefeatedTrigger";
    private static final String VANILLA_SUBSTRATE_ITEM_ID = "shrouded_substrate";
    private static final int PRIMORDIAL_BONUS_SUBSTRATE = 30;
    private static final float DROP_PURGE_AMOUNT = 10000f;
    private static final String VARIANT_BANSHEE_TENEBROUS = "shrouded_banshee_tenebrous";
    private static final String VARIANT_BRAIN_SEER = "shrouded_brain_seer";
    private static final String VARIANT_INSATIABLE_HUNGER_APEX = "insatiable_hunger_apex";
    private static final String VARIANT_TENDRIL_ACCELERATOR = "shrouded_tendril_accelerator";
    private static final String VARIANT_MAW_MASSLAUNCHER = "shrouded_maw_masslauncher";
    private static final float PLAYER_VARIANT_CHANCE = 0.5f;

    public static ListMap<String> GUARANTEED_FIRST_TIME_ITEMS = new ListMap<>();
    public static ListMap<String> DROP_GROUPS = new ListMap<>();

    static {
        GUARANTEED_FIRST_TIME_ITEMS.add(SHROUDED_EYE, "shrouded_lens");
        GUARANTEED_FIRST_TIME_ITEMS.add(SHROUDED_MAELSTROM, "shrouded_thunderhead");
        GUARANTEED_FIRST_TIME_ITEMS.add(SHROUDED_MAW, "shrouded_mantle");

        DROP_GROUPS.add(SHROUDED_EYE, "drops_shrouded_eye");
        DROP_GROUPS.add(SHROUDED_MAELSTROM, "drops_shrouded_maelstrom");
        DROP_GROUPS.add(SHROUDED_MAW, "drops_shrouded_maw");
    }

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params,
                           Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) {
            return false;
        }

        SectorEntityToken entity = dialog.getInteractionTarget();
        long seed = Misc.getSalvageSeed(entity);
        Random random = Misc.getRandom(seed, 11);
        String action = params.get(0).getString(memoryMap);
        MemoryAPI memory = memoryMap.get("local");

        if ("showWeaponPicker".equals(action)) {
            this.showWeaponPicker(dialog, memoryMap);
            return true;
        } else if ("unlockHullmod".equals(action)) {
            this.unlockHullmod(dialog, memoryMap);
            return true;
        } else if ("checkForBetterSurvey".equals(action)) {
            this.checkForBetterSurvey(dialog);
            return true;
        }

        if (memory == null) {
            return false;
        } else if ("smallFleet".equals(action)) {
            return this.engageFleet(dialog, memoryMap, memory, DwellerStrength.LOW, random);
        } else if ("mediumFleet".equals(action)) {
            return this.engageFleet(dialog, memoryMap, memory, DwellerStrength.MEDIUM, random);
        } else if ("largeFleet".equals(action)) {
            return this.engageFleet(dialog, memoryMap, memory, DwellerStrength.HIGH, random);
        } else if ("hugeFleet".equals(action)) {
            return this.engageFleet(dialog, memoryMap, memory, DwellerStrength.EXTREME, random);
        } else if ("insaneFleet".equals(action)) {
            return this.engageFleet(dialog, memoryMap, memory, DwellerStrength.INSANE, random);
        } else if ("primordialFleet".equals(action)) {
            return this.engageFleet(dialog, memoryMap, memory, DwellerStrength.PRIMORDIAL, random);
        }

        return false;
    }

    protected void unlockHullmod(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        String modId = Global.getSector().getPlayerMemoryWithoutUpdate()
                .getString(ShroudedHullmodItemPlugin.SHROUDED_HULLMOD_ID);
        HullModSpecAPI modSpec = Global.getSettings().getHullModSpec(modId);

        Global.getSoundPlayer().playUISound("ui_acquired_hullmod", 1f, 1f);

        TextPanelAPI text = dialog.getTextPanel();
        text.setFontSmallInsignia();
        String str = modSpec.getDisplayName();
        text.addParagraph("Acquired hull mod: " + str, Misc.getPositiveHighlightColor());
        text.highlightInLastPara(Misc.getHighlightColor(), str);
        text.setFontInsignia();

        Global.getSector().getPlayerFaction().addKnownHullMod(modId);
    }

    public static int getSubstrateCost(WeaponSpecAPI spec) {
        if (!spec.hasTag("dweller")) {
            return 0;
        }

        String substrate = "substrate_";
        for (String tag : spec.getTags()) {
            if (tag.startsWith(substrate)) {
                String num = tag.replaceFirst(substrate, "");
                return Integer.parseInt(num);
            }
        }

        return 0;
    }

    protected void showWeaponPicker(final InteractionDialogAPI dialog,
                                    final Map<String, MemoryAPI> memoryMap) {
        final int substrate = Global.getSector().getPlayerMemoryWithoutUpdate()
                .getInt(ShroudedSubstratePlugin.SHROUDED_SUBSTRATE_AVAILABLE);
        final Set<String> weapons = new LinkedHashSet<>();

        for (WeaponSpecAPI spec : Global.getSettings().getAllWeaponSpecs()) {
            int cost = getSubstrateCost(spec);
            if (cost > 0 && cost <= substrate) {
                weapons.add(spec.getWeaponId());
            }
        }

        dialog.showCustomProductionPicker(new BaseCustomProductionPickerDelegateImpl() {
            @Override
            public Set<String> getAvailableFighters() {
                return new LinkedHashSet<>();
            }

            @Override
            public Set<String> getAvailableShipHulls() {
                return new LinkedHashSet<>();
            }

            @Override
            public Set<String> getAvailableWeapons() {
                return weapons;
            }

            @Override
            public float getCostMult() {
                return 1f;
            }

            @Override
            public float getMaximumValue() {
                return substrate;
            }

            @Override
            public String getWeaponColumnNameOverride() {
                return "Weapon";
            }

            @Override
            public String getNoMatchingBlueprintsLabelOverride() {
                return "No matching weapons";
            }

            @Override
            public String getMaximumOrderValueLabelOverride() {
                return "Shrouded Substrate available";
            }

            @Override
            public String getCurrentOrderValueLabelOverride() {
                return "Shrouded Substrate required";
            }

            @Override
            public String getItemGoesOverMaxValueStringOverride() {
                return "Not enough Shrouded Substrate";
            }

            @Override
            public String getCustomOrderLabelOverride() {
                return "Weapon assembly";
            }

            @Override
            public String getNoProductionOrdersLabelOverride() {
                return "No assembly orders";
            }

            @Override
            public boolean withQuantityLimits() {
                return false;
            }

            @Override
            public boolean isUseCreditSign() {
                return false;
            }

            @Override
            public int getCostOverride(Object item) {
                return item instanceof WeaponSpecAPI
                        ? DwellerTidalCMD.getSubstrateCost((WeaponSpecAPI) item)
                        : -1;
            }

            @Override
            public void notifyProductionSelected(FactionProductionAPI production) {
                if (dialog.getPlugin() instanceof RuleBasedInteractionDialogPluginImpl) {
                    RuleBasedInteractionDialogPluginImpl plugin =
                            (RuleBasedInteractionDialogPluginImpl) dialog.getPlugin();

                    if (plugin.getCustom1() instanceof SpecialItemPlugin.RightClickActionHelper) {
                        SpecialItemPlugin.RightClickActionHelper helper =
                                (SpecialItemPlugin.RightClickActionHelper) plugin.getCustom1();

                        int cost = production.getTotalCurrentCost();
                        helper.removeFromClickedStackFirst(cost);

                        int substrate = (int) helper.getNumItems(
                                CargoItemType.SPECIAL,
                                new SpecialItemData("shrouded_substrate", null)
                        );

                        Global.getSector().getPlayerMemoryWithoutUpdate()
                                .set(ShroudedSubstratePlugin.SHROUDED_SUBSTRATE_AVAILABLE, substrate);

                        for (FactionProductionAPI.ItemInProductionAPI item : production.getCurrent()) {
                            if (item.getType() == ProductionItemType.WEAPON) {
                                helper.addItems(CargoItemType.WEAPONS, item.getSpecId(),
                                        (float) item.getQuantity());
                                AddRemoveCommodity.addWeaponGainText(item.getSpecId(),
                                        item.getQuantity(), dialog.getTextPanel());
                            }
                        }

                        FireBest.fire(null, dialog, memoryMap, "SubstrateWeaponsPicked");
                        Global.getSoundPlayer().playUISound("ui_cargo_machinery_drop", 1f, 1f);
                    }
                }
            }
        });
    }

    protected void checkForBetterSurvey(InteractionDialogAPI dialog) {
        if (dialog == null || dialog.getOptionPanel() == null) {
            return;
        }

        MemoryAPI global = Global.getSector() != null ? Global.getSector().getMemoryWithoutUpdate() : null;
        if (global != null && global.getBoolean(EntityContactManager.FIRST_TRIAL_STARTED_KEY)) {
            dialog.getOptionPanel().addOption(
                    "Attempt your trial?",
                    PRIMORDIAL_OPTION_ID,
                    Color.RED,
                    null
            );
            dialog.getOptionPanel().addOptionConfirmation(
                    PRIMORDIAL_OPTION_ID,
                    "Your path starts here, are you certain?",
                    "Proceed",
                    "Abort"
            );
        }

        if (!hasAotdGreatFleetAbility()) {
            return;
        }

        dialog.getOptionPanel().addOption(
                "Use a Shroud Beacon to lure them directly to us",
                ULTRA_SENSORS_OPTION_ID,
                Color.ORANGE,
                null
        );
        dialog.getOptionPanel().addOptionConfirmation(
                ULTRA_SENSORS_OPTION_ID,
                "Are you sure about this?",
                "Proceed",
                "Abort"
        );
    }

    protected boolean engageFleet(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap,
                                  MemoryAPI memory, DwellerStrength str, Random random) {
        CampaignFleetAPI fleet = createDwellerFleet(str, random);
        if (fleet == null) {
            return false;
        }
        if (str == DwellerStrength.PRIMORDIAL) {
            Misc.addDefeatTrigger(fleet, PRIMORDIAL_DEFEAT_TRIGGER);
        }

        CampaignFleetAPI pf = Global.getSector().getPlayerFleet();
        fleet.setContainingLocation(pf.getContainingLocation());

        final SectorEntityToken entity = dialog.getInteractionTarget();
        dialog.setInteractionTarget(fleet);
        Global.getSector().getCampaignUI().restartEncounterMusic(fleet);

        FleetInteractionDialogPluginImpl.FIDConfig config =
                new FleetInteractionDialogPluginImpl.FIDConfig();

        config.delegate = new FleetInteractionDialogPluginImpl.BaseFIDDelegate() {
            @Override
            public void postPlayerSalvageGeneration(InteractionDialogAPI dialog,
                                                    FleetEncounterContext context,
                                                    CargoAPI salvage) {
                if (!(dialog.getInteractionTarget() instanceof CampaignFleetAPI)) {
                    return;
                }

                boolean aotdVokEnabled = isAotdVokEnabled();
                CampaignFleetAPI fleet = (CampaignFleetAPI) dialog.getInteractionTarget();
                FleetEncounterContextPlugin.DataForEncounterSide data = context.getDataFor(fleet);
                List<FleetMemberAPI> losses = new ArrayList<>();

                for (FleetEncounterContextPlugin.FleetMemberData fmd : data.getOwnCasualties()) {
                    losses.add(fmd.getMember());
                }

                float min = 0f;
                float max = 0f;
                boolean gotGuaranteed = false;

                for (FleetMemberAPI member : losses) {
                    if (member.getHullSpec().hasTag("dweller")) {
                        String key = "substrate_";
                        float[] sDrops = Misc.getFloatArray(key + member.getHullSpec().getHullId());

                        if (sDrops == null) {
                            sDrops = Misc.getFloatArray(
                                    key + member.getHullSpec().getHullSize().name());
                        }

                        if (sDrops != null) {
                            float sMin;
                            float sMax;

                            if (sDrops.length >= 4) {
                                sMin = sDrops[1];
                                sMax = sDrops[3];
                            } else if (sDrops.length >= 2) {
                                sMin = sDrops[0];
                                sMax = sDrops[1];
                            } else if (sDrops.length == 1) {
                                sMin = sDrops[0];
                                sMax = sDrops[0];
                            } else {
                                continue;
                            }

                            if (sMax < sMin) {
                                float temp = sMin;
                                sMin = sMax;
                                sMax = temp;
                            }

                            min += sMin;
                            max += sMax;

                            String hullId = member.getHullSpec().getRestoredToHullId();
                            String defeatedKey = "$defeatedDweller_" + hullId;
                            boolean firstTime = !Global.getSector().getPlayerMemoryWithoutUpdate()
                                    .getBoolean(defeatedKey);

                            Global.getSector().getPlayerMemoryWithoutUpdate()
                                    .set(defeatedKey, true);

                            if (!aotdVokEnabled && firstTime && !gotGuaranteed) {
                                for (String itemId : GUARANTEED_FIRST_TIME_ITEMS.get(hullId)) {
                                    SpecialItemData sid = new SpecialItemData(itemId, null);
                                    boolean add = salvage.getQuantity(CargoItemType.SPECIAL, sid) <= 0f;

                                    if (add) {
                                        salvage.addItems(CargoItemType.SPECIAL, sid, 1f);
                                        gotGuaranteed = true;
                                    }
                                }
                            }
                        }
                    }
                }

                long seed = Misc.getSalvageSeed(entity);
                Random random = Misc.getRandom(seed, 50);
                int substrate = 0;

                if (min + max < 1f) {
                    if (random.nextFloat() < (min + max) / 2f) {
                        substrate = 1;
                    }
                } else {
                    substrate = Math.round(min + (max - min) * random.nextFloat());
                }

                if (str == DwellerStrength.PRIMORDIAL) {
                    markPrimordialFleetDefeated();
                    if (aotdVokEnabled) {
                        removeNonSubstrateSpecialDropsForAotd(salvage);
                    }
                    applyPrimordialLoot(salvage, substrate, aotdVokEnabled);
                    return;
                }

                if (aotdVokEnabled) {
                    boolean hasContainmentResearch = hasAotdBetterContainmentMethods();
                    int fixedDrop = getAotdVokVanillaSubstrateDrop(str, hasContainmentResearch);
                    int bonus = 0;
                    if (fixedDrop > 0 && hasContainmentResearch) {
                        bonus = computeAotdContainmentBonus(losses, random);
                    }
                    int substrateToGrant = Math.max(0, fixedDrop + bonus);
                    setSubstrateDropsForAotd(salvage, substrateToGrant);
                    removeNonSubstrateSpecialDropsForAotd(salvage);
                } else if (substrate > 0) {
                    salvage.addItems(
                            CargoItemType.SPECIAL,
                            new SpecialItemData(VANILLA_SUBSTRATE_ITEM_ID, null),
                            (float) substrate
                    );
                }
            }

            @Override
            public void battleContextCreated(InteractionDialogAPI dialog,
                                             BattleCreationContext bcc) {
                bcc.aiRetreatAllowed = false;
                bcc.fightToTheLast = true;
                bcc.objectivesAllowed = false;
                bcc.enemyDeployAll = true;

                if (entity.getCustomPlugin() instanceof AbyssalLightEntityPlugin) {
                    AbyssalLightEntityPlugin plugin =
                            (AbyssalLightEntityPlugin) entity.getCustomPlugin();
                    plugin.despawn(DespawnType.FADE_OUT);
                }
            }
        };

        config.alwaysAttackVsAttack = true;
        config.alwaysHarry = true;
        config.showTransponderStatus = false;
        config.lootCredits = false;
        config.showCommLinkOption = false;
        config.showEngageText = false;
        config.showFleetAttitude = false;
        config.showWarningDialogWhenNotHostile = false;
        config.impactsAllyReputation = false;
        config.impactsEnemyReputation = false;
        config.pullInAllies = false;
        config.pullInEnemies = false;
        config.pullInStations = false;
        config.showCrRecoveryText = false;
        config.firstTimeEngageOptionText = "\"Battle stations!\"";
        config.afterFirstTimeEngageOptionText = "Move in to re-engage";

        if (str == DwellerStrength.LOW) {
            config.firstTimeEngageOptionText = null;
            config.leaveAlwaysAvailable = true;
        } else {
            config.leaveAlwaysAvailable = true;
            config.noLeaveOptionOnFirstEngagement = true;
        }

        long seed = Misc.getSalvageSeed(entity);
        config.salvageRandom = Misc.getRandom(seed, 75);

        Global.getSector().getPlayerMemoryWithoutUpdate().set("$encounteredDweller", true);
        Global.getSector().getPlayerMemoryWithoutUpdate().set("$encounteredMonster", true);
        Global.getSector().getPlayerMemoryWithoutUpdate().set("$encounteredWeird", true);

        FleetInteractionDialogPluginImpl plugin =
                new FleetInteractionDialogPluginImpl(config);
        dialog.setPlugin(plugin);
        plugin.init(dialog);

        return true;
    }

    public static CampaignFleetAPI createDwellerFleet(DwellerStrength str, Random random) {
        CampaignFleetAPI f = Global.getFactory().createEmptyFleet("dweller", "Manifestation", true);
        FactionAPI faction = Global.getSector().getFaction("dweller");

        String typeKey = "patrolSmall";
        if (str == DwellerStrength.MEDIUM) {
            typeKey = "patrolMedium";
        }
        if (str == DwellerStrength.HIGH) {
            typeKey = "patrolLarge1";
        }
        if (str == DwellerStrength.EXTREME) {
            typeKey = "patrolLarge2";
        }
        if (str == DwellerStrength.INSANE) {
            typeKey = "patrolLarge2";
        }
        if (str == DwellerStrength.PRIMORDIAL) {
            typeKey = "patrolLarge2";
        }

        f.setName(faction.getFleetTypeName(typeKey));
        if (str == DwellerStrength.HIGH) {
            String greaterName = faction.getFleetTypeName("patrolLarge");
            if (greaterName != null && !greaterName.isEmpty()) {
                f.setName(greaterName);
            } else {
                f.setName("Greater Manifestation");
            }
        } else if (str == DwellerStrength.INSANE) {
            f.setName("Abyssal Incursion");
        } else if (str == DwellerStrength.PRIMORDIAL) {
            f.setName("The Primordial Hunger");
        }
        f.setInflater((FleetInflater) null);

        if (str == DwellerStrength.LOW) {
            addVariantOrRoleShips(f, 5, 7, random, 0.5f,
                    VARIANT_TENDRIL_ACCELERATOR, "dwellerTendril");
            addShips(f, 1, 2, random, "dwellerEye");
            addShips(f, 1, 2, random, "dwellerMaelstrom");
            addVariantOrRoleShips(f, 0, 1, random, 0.5f,
                    VARIANT_MAW_MASSLAUNCHER, "dwellerMaw");
        } else if (str == DwellerStrength.MEDIUM) {
            addVariantOrRoleShipsNoDrops(f, 10, 13, random, 0.5f,
                    VARIANT_TENDRIL_ACCELERATOR, "dwellerTendril");
            addDirectShipsNoDrops(f,2,3,random, "shrouded_banshee_tenebrous");
            int eyes = addShipsNoDrops(f, 1, 1, random, "dwellerEye");
            addShipsNoDrops(f, 2 - eyes, 3 - eyes, random, "dwellerMaelstrom");
            addVariantOrRoleShipsNoDrops(f, 1, 1, random, 0.5f,
                    VARIANT_MAW_MASSLAUNCHER, "dwellerMaw");
        } else if (str == DwellerStrength.HIGH) {
            addVariantOrRoleShips(f, 8, 11, random, 0.5f,
                    VARIANT_TENDRIL_ACCELERATOR, "dwellerTendril");
            addDirectShips(f,4,6,random, "shrouded_banshee_tenebrous");
            int eyes = addShips(f, 2, 4, random, "dwellerEye");
            addDirectShips(f, 1, 1, random, VARIANT_BRAIN_SEER);
            addShips(f, 3 - eyes, 5 - eyes, random, "dwellerMaelstrom");
            addVariantOrRoleShips(f, 1, 2, random, 0.5f,
                    VARIANT_MAW_MASSLAUNCHER, "dwellerMaw");
        } else if (str == DwellerStrength.EXTREME) {
            addVariantOrRoleShips(f, 10, 13, random, 0.5f,
                    VARIANT_TENDRIL_ACCELERATOR, "dwellerTendril");
            addDirectShips(f,6,8,random, "shrouded_banshee_tenebrous");
            int eyes = addShips(f, 2, 4, random, "dwellerEye");
            addDirectShips(f, 1, 1, random, VARIANT_BRAIN_SEER);
            addShips(f, 3 - eyes, 5 - eyes, random, "dwellerMaelstrom");
            addVariantOrRoleShips(f, 3, 3, random, 0.5f,
                    VARIANT_MAW_MASSLAUNCHER, "dwellerMaw");
        } else if (str == DwellerStrength.INSANE) {
            addVariantOrRoleShips(f, 8, 10, random, PLAYER_VARIANT_CHANCE,
                    VARIANT_TENDRIL_ACCELERATOR, "dwellerTendril");
            addDirectShips(f, 12, 14, random, VARIANT_BANSHEE_TENEBROUS);
            int eyes = addShips(f, 4, 5, random, "dwellerEye");
            addShips(f, eyes, eyes, random, "dwellerMaelstrom");
            addDirectShips(f, 1, 1, random, VARIANT_BRAIN_SEER);
            addVariantOrRoleShips(f, 4, 4, random, PLAYER_VARIANT_CHANCE,
                    VARIANT_MAW_MASSLAUNCHER, "dwellerMaw");
        } else if (str == DwellerStrength.PRIMORDIAL) {
            addDirectShips(f, 1, 1, random, VARIANT_INSATIABLE_HUNGER_APEX);
            addDirectShips(f, 1, 1, random, VARIANT_BRAIN_SEER);
            addDirectShips(f, 12, 14, random, VARIANT_BANSHEE_TENEBROUS);
            addShips(f, 4, 4, random, "dwellerEye");
            addShips(f, 4, 4, random, "dwellerMaelstrom");
            addVariantOrRoleShips(f, 10, 10, random, PLAYER_VARIANT_CHANCE,
                    VARIANT_TENDRIL_ACCELERATOR, "dwellerTendril");
            addVariantOrRoleShips(f, 4, 4, random, PLAYER_VARIANT_CHANCE,
                    VARIANT_MAW_MASSLAUNCHER, "dwellerMaw");
        }

        f.getFleetData().setSyncNeeded();
        f.getFleetData().syncIfNeeded();
        f.getFleetData().sort();

        for (FleetMemberAPI curr : f.getFleetData().getMembersListCopy()) {
            curr.getRepairTracker().setCR(curr.getRepairTracker().getMaxCR());
        }

        f.getMemoryWithoutUpdate().set("$cfai_makeHostile", true);
        f.getMemoryWithoutUpdate().set("$mayGoIntoAbyss", true);
        return f;
    }

    public static int addShips(CampaignFleetAPI fleet, int min, int max, Random random,
                               Object... roles) {
        return addShipsInternal(fleet, min, max, random, true, roles);
    }

    public static int addShipsNoDrops(CampaignFleetAPI fleet, int min, int max, Random random,
                                      Object... roles) {
        return addShipsInternal(fleet, min, max, random, false, roles);
    }

    private static int addShipsInternal(CampaignFleetAPI fleet, int min, int max, Random random,
                                        boolean includeDrops, Object... roles) {
        if (min < 0) min = 0;
        if (max < 0) max = 0;

        WeightedRandomPicker<String> picker = new WeightedRandomPicker<>();
        if (roles.length == 1) {
            picker.add((String) roles[0], 1f);
        } else {
            for (int i = 0; i < roles.length; i += 2) {
                picker.add((String) roles[i], (Float) roles[i + 1]);
            }
        }

        int num = min + random.nextInt(max - min + 1);
        FactionAPI faction = Global.getSector().getFaction("dweller");
        FactionAPI.ShipPickParams p = new FactionAPI.ShipPickParams(ShipPickMode.ALL);
        p.blockFallback = true;
        p.maxFP = 1000000;

        for (int i = 0; i < num; ++i) {
            String role = picker.pick();

            for (ShipRolePick pick : faction.pickShip(role, p, (ShipFilter) null, random)) {
                fleet.getFleetData().addFleetMember(pick.variantId);
                ShipVariantAPI variant = Global.getSettings().getVariant(pick.variantId);

                if (includeDrops && variant != null) {
                    String hullId = variant.getHullSpec().getRestoredToHullId();
                    for (String group : DROP_GROUPS.get(hullId)) {
                        fleet.addDropRandom(group, 1);
                    }
                }
            }
        }

        return num;
    }

    public static int addDirectShips(CampaignFleetAPI fleet, int min, int max, Random random,
                                     String... variantIds) {
        return addDirectShipsInternal(fleet, min, max, random, true, variantIds);
    }

    public static int addDirectShipsNoDrops(CampaignFleetAPI fleet, int min, int max, Random random,
                                            String... variantIds) {
        return addDirectShipsInternal(fleet, min, max, random, false, variantIds);
    }

    private static int addDirectShipsInternal(CampaignFleetAPI fleet, int min, int max, Random random,
                                              boolean includeDrops, String... variantIds) {
        if (min < 0) min = 0;
        if (max < 0) max = 0;
        if (variantIds == null || variantIds.length == 0) return 0;

        WeightedRandomPicker<String> picker = new WeightedRandomPicker<>();
        for (String variantId : variantIds) {
            picker.add(variantId, 1f);
        }

        int num = min + random.nextInt(max - min + 1);

        for (int i = 0; i < num; i++) {
            String variantId = picker.pick();
            if (variantId == null) continue;

            fleet.getFleetData().addFleetMember(variantId);

            ShipVariantAPI variant = Global.getSettings().getVariant(variantId);
            if (includeDrops && variant != null) {
                String hullId = variant.getHullSpec().getRestoredToHullId();

                for (String group : DROP_GROUPS.get(hullId)) {
                    fleet.addDropRandom(group, 1);
                }
            }
        }

        return num;
    }

    public static int addVariantOrRoleShips(CampaignFleetAPI fleet, int min, int max, Random random,
                                            float variantChance, String variantId, String role) {
        return addVariantOrRoleShipsInternal(
                fleet, min, max, random, true, variantChance, variantId, role);
    }

    public static int addVariantOrRoleShipsNoDrops(CampaignFleetAPI fleet, int min, int max, Random random,
                                                   float variantChance, String variantId, String role) {
        return addVariantOrRoleShipsInternal(
                fleet, min, max, random, false, variantChance, variantId, role);
    }

    private static int addVariantOrRoleShipsInternal(CampaignFleetAPI fleet, int min, int max, Random random,
                                                     boolean includeDrops, float variantChance,
                                                     String variantId, String role) {
        if (min < 0) min = 0;
        if (max < 0) max = 0;
        if (max < min) max = min;

        int num = min + random.nextInt(max - min + 1);
        for (int i = 0; i < num; i++) {
            if (random.nextFloat() < variantChance) {
                try {
                    addDirectShipsInternal(fleet, 1, 1, random, includeDrops, variantId);
                    continue;
                } catch (RuntimeException ex) {
                    // If a variant can't be instantiated at runtime, fall back to the stock role.
                }
            }
            addShipsInternal(fleet, 1, 1, random, includeDrops, role);
        }

        return num;
    }

    private static boolean isAotdVokEnabled() {
        if (Global.getSector() == null) {
            return false;
        }

        if (Global.getSettings() == null) {
            return false;
        }

        if (Global.getSettings().getModManager() != null
                && Global.getSettings().getModManager().isModEnabled(AOTD_VOK_MOD_ID)) {
            return true;
        }

        // Fallback: if the AoTD substrate item is loaded, treat VoK as present.
        try {
            return Global.getSettings().getSpecialItemSpec(AOTD_SUBSTRATE_ITEM_ID) != null;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static int getAotdVokVanillaSubstrateDrop(DwellerStrength strength,
                                                      boolean hasContainmentResearch) {
        if (strength == DwellerStrength.INSANE) {
            return 18;
        }
        if (strength == DwellerStrength.EXTREME) {
            return 12;
        }
        if (strength == DwellerStrength.HIGH) {
            return hasContainmentResearch ? 8 : 0;
        }
        return 0;
    }

    private static boolean hasAotdBetterContainmentMethods() {
        if (Global.getSector() == null) {
            return false;
        }

        MemoryAPI playerMemory = Global.getSector().getPlayerMemoryWithoutUpdate();
        if (playerMemory != null && playerMemory.getBoolean(AOTD_BETTER_CONTAINMENT_MEMORY_KEY)) {
            return true;
        }

        MemoryAPI sectorMemory = Global.getSector().getMemoryWithoutUpdate();
        return sectorMemory != null && sectorMemory.getBoolean(AOTD_BETTER_CONTAINMENT_MEMORY_KEY);
    }

    private static boolean hasAotdGreatFleetAbility() {
        if (Global.getSector() == null) {
            return false;
        }

        MemoryAPI playerMemory = Global.getSector().getPlayerMemoryWithoutUpdate();
        if (playerMemory != null && playerMemory.getBoolean(AOTD_GREAT_FLEET_MEMORY_KEY)) {
            return true;
        }

        MemoryAPI sectorMemory = Global.getSector().getMemoryWithoutUpdate();
        return sectorMemory != null && sectorMemory.getBoolean(AOTD_GREAT_FLEET_MEMORY_KEY);
    }

    private static int computeAotdContainmentBonus(List<FleetMemberAPI> losses, Random random) {
        if (losses == null || losses.isEmpty() || random == null) {
            return 0;
        }

        int bonus = 0;
        float chanceToFail = 0.4f;
        for (FleetMemberAPI member : losses) {
            if (member == null || member.getHullSpec() == null || !member.getHullSpec().hasTag("dweller")) {
                continue;
            }

            int amount = member.getHullSpec().getHullSize().ordinal() - 2;
            amount += 1;
            float ch = random.nextFloat();
            if (ch >= chanceToFail) {
                bonus += amount;
            }
        }
        return bonus;
    }

    private static void setSubstrateDropsForAotd(CargoAPI salvage, int amount) {
        if (salvage == null) {
            return;
        }

        salvage.removeItems(CargoItemType.SPECIAL, new SpecialItemData(VANILLA_SUBSTRATE_ITEM_ID, null), DROP_PURGE_AMOUNT);
        salvage.removeItems(CargoItemType.SPECIAL, new SpecialItemData(AOTD_SUBSTRATE_ITEM_ID, null), DROP_PURGE_AMOUNT);

        if (amount <= 0) {
            return;
        }

        salvage.addItems(CargoItemType.SPECIAL, new SpecialItemData(VANILLA_SUBSTRATE_ITEM_ID, null), (float) amount);
        salvage.addItems(CargoItemType.SPECIAL, new SpecialItemData(AOTD_SUBSTRATE_ITEM_ID, null), (float) amount);
    }

    private static void removeNonSubstrateSpecialDropsForAotd(CargoAPI salvage) {
        if (salvage == null) {
            return;
        }

        salvage.removeItems(CargoItemType.SPECIAL, new SpecialItemData("shrouded_lens", null), DROP_PURGE_AMOUNT);
        salvage.removeItems(CargoItemType.SPECIAL, new SpecialItemData("shrouded_mantle", null), DROP_PURGE_AMOUNT);
        salvage.removeItems(CargoItemType.SPECIAL, new SpecialItemData("shrouded_thunderhead", null), DROP_PURGE_AMOUNT);
    }

    private static void applyPrimordialLoot(CargoAPI salvage, int vanillaRollSubstrate,
                                            boolean aotdVokEnabled) {
        if (salvage == null) {
            return;
        }

        if (!aotdVokEnabled) {
            if (vanillaRollSubstrate > 0) {
                salvage.addItems(
                        CargoItemType.SPECIAL,
                        new SpecialItemData(VANILLA_SUBSTRATE_ITEM_ID, null),
                        (float) vanillaRollSubstrate
                );
            }
            salvage.addItems(
                    CargoItemType.SPECIAL,
                    new SpecialItemData(VANILLA_SUBSTRATE_ITEM_ID, null),
                    (float) PRIMORDIAL_BONUS_SUBSTRATE
            );
            return;
        }

        salvage.addItems(
                CargoItemType.SPECIAL,
                new SpecialItemData(VANILLA_SUBSTRATE_ITEM_ID, null),
                (float) PRIMORDIAL_BONUS_SUBSTRATE
        );
        salvage.addItems(
                CargoItemType.SPECIAL,
                new SpecialItemData(AOTD_SUBSTRATE_ITEM_ID, null),
                (float) PRIMORDIAL_BONUS_SUBSTRATE
        );
    }

    private static void markPrimordialFleetDefeated() {
        if (Global.getSector() == null) {
            return;
        }
        MemoryAPI global = Global.getSector().getMemoryWithoutUpdate();
        if (global != null) {
            global.set(PRIMORDIAL_FLEET_DEFEATED_KEY, true);
        }
        EntityContactManager.markFirstTrialCompleted();
    }

    public enum DwellerStrength {
        LOW,
        MEDIUM,
        HIGH,
        EXTREME,
        INSANE,
        PRIMORDIAL
    }
}
