package net.unfamily.another_quarries.config;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.unfamily.another_quarries.AnotherQuarries;
import net.unfamily.another_quarries.item.QuarryModules;
import net.unfamily.another_quarries.mining.QuarryMiningFilters;

import net.unfamily.another_quarries.mining.QuarryDrillType;

import java.util.List;

@EventBusSubscriber(modid = AnotherQuarries.MOD_ID)
public final class ModConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /** Seconds of peak draw used to pick the energy buffer tier from {@link #energyBufferTiers()}. */
    private static final int DEFAULT_BUFFER_SECONDS_AT_PEAK = 30;

    private static int[] cachedEnergyBufferTiers = new int[0];

    static {
        BUILDER.comment("Quarry base settings").push("000_quarry");
    }

    private static final ModConfigSpec.IntValue RF_PER_BLOCK = BUILDER
            .comment("Base RF consumed per block mined (before module and drill extras)")
            .defineInRange("000_rfPerBlock", 50, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.ConfigValue<List<? extends String>> ENERGY_BUFFER_TIERS = BUILDER
            .comment(
                    "Ascending RF buffer capacity tiers (round values)",
                    "Quarry starts at the first tier and moves up while peak consumption reaches the upgrade ratio")
            .defineList(
                    "001_energyBufferTiers",
                    List.of("100000", "500000", "1000000", "5000000", "10000000", "50000000"),
                    entry -> entry instanceof String);

    private static final ModConfigSpec.IntValue ENERGY_BUFFER_SECONDS_AT_PEAK = BUILDER
            .comment("Seconds of peak RF draw used to estimate consumption against buffer tiers")
            .defineInRange("002_energyBufferSecondsAtPeak", DEFAULT_BUFFER_SECONDS_AT_PEAK, 1, 600);

    private static final ModConfigSpec.DoubleValue ENERGY_BUFFER_TIER_UPGRADE_RATIO = BUILDER
            .comment("Advance to the next buffer tier when peak consumption reaches this fraction of the current tier")
            .defineInRange("energyBufferTierUpgradeRatio", 0.80, 0.1, 1.0);

    private static final ModConfigSpec.IntValue QUARRY_BASE_RANGE = BUILDER
            .comment("Default max blocks per axis when no range modules are installed")
            .defineInRange("003_baseRange", 4, 1, 64);

    private static final ModConfigSpec.IntValue QUARRY_MAX_RANGE = BUILDER
            .comment("Max horizontal width and depth of the mining area in blocks (e.g. 128 = up to 128×128 footprint)")
            .defineInRange("004_maxRange", 128, 1, 256);

    private static final ModConfigSpec.IntValue QUARRY_MAX_HEIGHT = BUILDER
            .comment("Max vertical height of the mining area in blocks")
            .defineInRange("maxHeight", 64, 1, 256);

    private static final ModConfigSpec.IntValue BASE_BREAK_TICKS = BUILDER
            .comment("Base ticks to break one block with one worker and no speed modules (20 ticks = 1 second)")
            .defineInRange("005_baseBreakTicks", 60, 1, 20 * 60 * 10);

    private static final ModConfigSpec.IntValue MAX_DRONES = BUILDER
            .comment("Maximum drones installed in the drone equipment slot")
            .defineInRange("006_maxDrones", 64, 1, 1024);

    private static final ModConfigSpec.IntValue REGEN_SCAN_INTERVAL_TICKS = BUILDER
            .comment("Ticks between regen scans near the mining front (20 ticks = 1 second)")
            .defineInRange("007_regenScanIntervalTicks", 2400, 20, 20 * 60 * 10);

    private static final ModConfigSpec.IntValue REGEN_SCAN_BLOCKS_PER_TICK = BUILDER
            .comment("Block positions checked per tick during a regen scan (mining continues)")
            .defineInRange("regenScanBlocksPerTick", 12, 1, 4096);

    private static final ModConfigSpec.IntValue REGEN_SCAN_LAYER_DEPTH = BUILDER
            .comment("How many recently cleared layers behind the mining front are checked per regen scan")
            .defineInRange("regenScanLayerDepth", 4, 1, 64);

    private static final ModConfigSpec.IntValue REGEN_QUEUE_MAX_SIZE = BUILDER
            .comment("Maximum pending regenerated block targets queued per quarry")
            .defineInRange("regenQueueMaxSize", 256, 16, 4096);

    private static final ModConfigSpec.IntValue FRAME_VALIDATION_BLOCKS_PER_TICK = BUILDER
            .comment(
                    "Minimum block positions checked per tick during frame validation on reboot, power-on, or resize",
                    "The quarry auto-scales upward so validation usually finishes within ~2 seconds")
            .defineInRange("frameValidationBlocksPerTick", 256, 1, 4096);

    /** Target ticks to complete a frame validation scan (~2 seconds at 20 TPS). */
    private static final int FRAME_VALIDATION_TARGET_TICKS = 40;

    private static final ModConfigSpec.IntValue EQUIPMENT_GUI_COLUMNS = BUILDER
            .comment("Equipment row width in the quarry GUI background (drone + drill + 3 upgrade slots must fit)")
            .defineInRange("008_equipmentGuiColumns", 9, 5, 9);

    private static final ModConfigSpec.IntValue EQUIPMENT_DRONE_SLOTS = BUILDER
            .comment("Number of drone equipment slots; total installed drones are capped by maxDrones")
            .defineInRange("009_equipmentDroneSlots", 1, 1, 8);

    private static final ModConfigSpec.IntValue EQUIPMENT_DRILL_SLOTS = BUILDER
            .comment("Number of drill equipment slots; the best drill among them is used for mining")
            .defineInRange("010_equipmentDrillSlots", 1, 1, 4);

    private static final ModConfigSpec.IntValue MAX_ACTIVE_MINING_WORKERS = BUILDER
            .comment(
                    "Maximum worker simulations per quarry per tick (reduces TPS cost of large drone stacks)",
                    "Extra drones still increase throughput via batched block breaks per worker completion")
            .defineInRange("011_maxActiveMiningWorkers", 64, 1, 128);

    private static final ModConfigSpec.IntValue MAX_BLOCK_BREAKS_PER_TICK = BUILDER
            .comment("Hard cap on breakBlock calls per quarry per tick (safety limit for server TPS)")
            .defineInRange("012_maxBlockBreaksPerTick", 64, 1, 512);

    private static final ModConfigSpec.IntValue VOLUME_MODE_MAX_FOOTPRINT = BUILDER
            .comment(
                    "Max horizontal width and depth (in blocks, height excluded) before auto-switching to chunk-by-chunk mining",
                    "Footprints up to this size on both axes use volume mode (e.g. 64x64 = 4096 blocks)")
            .defineInRange("013_volumeModeMaxFootprint", 64, 4, 128);

    static {
        BUILDER.pop();
        BUILDER.comment("Mining block filters").push("010_mining_filters");
    }

    private static final ModConfigSpec.ConfigValue<List<? extends String>> MINING_DENY_LIST = BUILDER
            .comment(
                    "Block IDs or #block tags the quarry will never break",
                    "Examples: minecraft:spawner, #c:ores/allthemodium")
            .defineList(
                    "miningDenyList",
                    List.of(
                            "minecraft:spawner",
                            "minecraft:budding_amethyst",
                            "#c:ores/allthemodium",
                            "#c:ores/vibranium",
                            "#c:ores/unobtainium"),
                    entry -> entry instanceof String);

    private static final ModConfigSpec.ConfigValue<List<? extends String>> MINING_ALLOW_LIST = BUILDER
            .comment(
                    "When non-empty, only listed block IDs or #block tags can be mined (deny list still applies)",
                    "Examples: minecraft:stone, #minecraft:mineable/pickaxe")
            .defineList("miningAllowList", List.of(), entry -> entry instanceof String);

    private static final ModConfigSpec.BooleanValue SKIP_INVENTORIES = BUILDER
            .comment("Skip chests, barrels, shulker boxes, and other blocks with item inventories (vanilla or modded)")
            .define("skipInventories", true);

    static {
        BUILDER.pop();
        BUILDER.comment("Module limits and extra RF per block").push("100_modules");
    }

    private static final ModConfigSpec.IntValue MODULE_BASE_EXTRA_RF = moduleExtraRf("module_base");
    private static final ModConfigSpec.IntValue MODULE_SPEED_EXTRA_RF = moduleExtraRf("module_speed", 1);
    private static final ModConfigSpec.IntValue MODULE_DIGGER_EXTRA_RF = moduleExtraRf("module_digger", 1);
    private static final ModConfigSpec.IntValue MODULE_SILK_TOUCH_EXTRA_RF = moduleExtraRf("module_silktouch", 8);
    private static final ModConfigSpec.IntValue MODULE_FORTUNE_EXTRA_RF = moduleExtraRf("module_fortune", 3);

    private static final ModConfigSpec.IntValue MODULE_BASE_MAX_COUNT = moduleMaxCount("module_base");
    private static final ModConfigSpec.IntValue MODULE_SPEED_MAX_COUNT = moduleMaxCount("module_speed", 16);
    private static final ModConfigSpec.IntValue MODULE_DIGGER_MAX_COUNT = moduleMaxCount("module_digger", 16);
    private static final ModConfigSpec.IntValue MODULE_SILK_TOUCH_MAX_COUNT = moduleMaxCount("module_silktouch", 1);
    private static final ModConfigSpec.IntValue MODULE_FORTUNE_MAX_COUNT = moduleMaxCount("module_fortune", 3);

    private static final ModConfigSpec.DoubleValue SPEED_TICK_MULTIPLIER = BUILDER
            .comment("Speed multiplier per speed module (1.0 = no change; 1.1 = 10% faster per module)")
            .defineInRange("2speed_tickMultiplier", 1.10, 1.0, 4.0);

    private static final ModConfigSpec.DoubleValue DIGGER_TICK_MULTIPLIER = BUILDER
            .comment("Speed multiplier per digger module on non-ore blocks",
                    "Tuned so 16 digger + 16 speed modules reach the 1-tick floor at default baseBreakTicks")
            .defineInRange("2digger_tickMultiplier", 1.152, 1.0, 4.0);

    static {
        BUILDER.pop();
        BUILDER.comment("Extra RF per block by installed drill tier (no drill / iron tier adds none)").push("101_drills");
    }

    private static final ModConfigSpec.IntValue DRILL_DIAMOND_EXTRA_RF = BUILDER
            .comment("Extra RF per block with a diamond drill installed")
            .defineInRange("0diamond_extraRf", 5, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue DRILL_NETHERITE_EXTRA_RF = BUILDER
            .comment("Extra RF per block with a netherite drill installed")
            .defineInRange("1netherite_extraRf", 8, 0, Integer.MAX_VALUE);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private static ModConfigSpec.IntValue moduleExtraRf(String key) {
        return moduleExtraRf(key, 0);
    }

    private static ModConfigSpec.IntValue moduleExtraRf(String key, int defaultValue) {
        return BUILDER
                .comment("Extra RF per block when " + key + " is installed")
                .defineInRange("1" + key.substring("module_".length()) + "_extraRf", defaultValue, 0, Integer.MAX_VALUE);
    }

    private static ModConfigSpec.IntValue moduleMaxCount(String key) {
        return moduleMaxCount(key, 7);
    }

    private static ModConfigSpec.IntValue moduleMaxCount(String key, int defaultValue) {
        return BUILDER
                .comment("Maximum installed count for " + key)
                .defineInRange("1" + key.substring("module_".length()) + "_maxCount", defaultValue, 0, 64);
    }

    private ModConfig() {}

    /** Peak RF draw per game tick at configured max drones with max modules and netherite drill. */
    public static int peakRfPerTickAtMaxLoad() {
        int logicalWorkers = 1 + maxDrones();
        int activeWorkers = Math.min(logicalWorkers, maxActiveMiningWorkers());
        int blocksPerCompletion = blocksPerWorkerCompletion(logicalWorkers, activeWorkers);
        int blocksPerTick = Math.min(maxBlockBreaksPerTick(), activeWorkers * blocksPerCompletion);
        int maxRfPerBlock = totalRfPerBlock(
                maxDiggerModules(),
                maxSpeedModules(),
                maxFortuneModules(),
                false,
                QuarryDrillType.NETHERITE);
        return blocksPerTick * maxRfPerBlock;
    }

    public static int maxActiveMiningWorkers() {
        return MAX_ACTIVE_MINING_WORKERS.get();
    }

    public static int maxBlockBreaksPerTick() {
        return MAX_BLOCK_BREAKS_PER_TICK.get();
    }

    public static int volumeModeMaxFootprint() {
        return VOLUME_MODE_MAX_FOOTPRINT.get();
    }

    /** Blocks one simulated worker may break when its progress completes (preserves drone throughput). */
    public static int blocksPerWorkerCompletion(int logicalWorkers, int activeWorkers) {
        if (activeWorkers <= 0 || logicalWorkers <= 0) {
            return 1;
        }
        return (logicalWorkers + activeWorkers - 1) / activeWorkers;
    }

    public static int energyBufferSecondsAtPeak() {
        return ENERGY_BUFFER_SECONDS_AT_PEAK.get();
    }

    public static double energyBufferTierUpgradeRatio() {
        return ENERGY_BUFFER_TIER_UPGRADE_RATIO.get();
    }

    public static int[] energyBufferTiers() {
        if (cachedEnergyBufferTiers.length == 0) {
            reloadEnergyBufferTiers();
        }
        return cachedEnergyBufferTiers;
    }

    public static void reloadEnergyBufferTiers() {
        int[] parsed = ENERGY_BUFFER_TIERS.get().stream()
                .mapToInt(value -> {
                    try {
                        return Integer.parseInt(value.trim());
                    } catch (NumberFormatException ignored) {
                        return -1;
                    }
                })
                .filter(value -> value > 0)
                .distinct()
                .sorted()
                .toArray();
        cachedEnergyBufferTiers = parsed.length > 0 ? parsed : new int[] {100_000};
    }

    /**
     * Walks buffer tiers upward while peak consumption reaches {@link #energyBufferTierUpgradeRatio()} of the current tier.
     */
    public static int resolveEnergyBufferCapacity(int peakRfPerTick) {
        int[] tiers = energyBufferTiers();
        long consumption = (long) peakRfPerTick * 20L * energyBufferSecondsAtPeak();
        int index = 0;
        while (index < tiers.length - 1) {
            long upgradeThreshold = (long) Math.floor(tiers[index] * energyBufferTierUpgradeRatio());
            if (consumption < upgradeThreshold) {
                break;
            }
            index++;
        }
        return tiers[index];
    }

    @SubscribeEvent
    static void onLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) {
            reloadEnergyBufferTiers();
            QuarryMiningFilters.reload();
            AnotherQuarries.LOGGER.debug("Loaded {}", AnotherQuarries.MOD_ID + " config");
        }
    }

    @SubscribeEvent
    static void onReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) {
            reloadEnergyBufferTiers();
            QuarryMiningFilters.reload();
        }
    }

    public static int baseRfPerBlock() {
        return RF_PER_BLOCK.get();
    }

    public static int quarryBaseRange() {
        return QUARRY_BASE_RANGE.get();
    }

    public static int quarryMaxRange() {
        return QUARRY_MAX_RANGE.get();
    }

    public static int quarryMaxHeight() {
        return QUARRY_MAX_HEIGHT.get();
    }

    public static int baseBreakTicks() {
        return BASE_BREAK_TICKS.get();
    }

    public static int maxDrones() {
        return MAX_DRONES.get();
    }

    public static int equipmentGuiColumns() {
        return EQUIPMENT_GUI_COLUMNS.get();
    }

    public static int equipmentDroneSlots() {
        int drones = EQUIPMENT_DRONE_SLOTS.get();
        int maxDrones = equipmentGuiColumns() - equipmentDrillSlots() - 3;
        return Math.min(drones, Math.max(1, maxDrones));
    }

    public static int equipmentDrillSlots() {
        return EQUIPMENT_DRILL_SLOTS.get();
    }

    public static int regenScanIntervalTicks() {
        return REGEN_SCAN_INTERVAL_TICKS.get();
    }

    public static int regenScanBlocksPerTick() {
        return REGEN_SCAN_BLOCKS_PER_TICK.get();
    }

    public static int regenScanLayerDepth() {
        return REGEN_SCAN_LAYER_DEPTH.get();
    }

    public static int regenQueueMaxSize() {
        return REGEN_QUEUE_MAX_SIZE.get();
    }

    public static int frameValidationBlocksPerTick() {
        return FRAME_VALIDATION_BLOCKS_PER_TICK.get();
    }

    /** Scales validation throughput so large quarries finish in roughly {@link #FRAME_VALIDATION_TARGET_TICKS} ticks. */
    public static int frameValidationBlocksPerTick(int remainingBlocks) {
        int configured = FRAME_VALIDATION_BLOCKS_PER_TICK.get();
        if (remainingBlocks <= 0) {
            return configured;
        }
        int scaled = (remainingBlocks + FRAME_VALIDATION_TARGET_TICKS - 1) / FRAME_VALIDATION_TARGET_TICKS;
        return Math.min(4096, Math.max(configured, scaled));
    }

    public static List<? extends String> miningDenyList() {
        return MINING_DENY_LIST.get();
    }

    public static List<? extends String> miningAllowList() {
        return MINING_ALLOW_LIST.get();
    }

    public static boolean skipInventories() {
        return SKIP_INVENTORIES.get();
    }

    public static int maxDiggerModules() {
        return MODULE_DIGGER_MAX_COUNT.get();
    }

    public static int maxSpeedModules() {
        return MODULE_SPEED_MAX_COUNT.get();
    }

    public static int maxFortuneModules() {
        return MODULE_FORTUNE_MAX_COUNT.get();
    }

    public static int maxSilkTouchModules() {
        return MODULE_SILK_TOUCH_MAX_COUNT.get();
    }

    public static float speedFactor(int speedModules) {
        return (float) Math.pow(SPEED_TICK_MULTIPLIER.get(), speedModules);
    }

    public static float diggerFactor(int diggerModules) {
        return (float) Math.pow(DIGGER_TICK_MULTIPLIER.get(), diggerModules);
    }

    public static int extraRfFor(QuarryModules module) {
        return switch (module) {
            case BASE -> MODULE_BASE_EXTRA_RF.get();
            case SPEED -> MODULE_SPEED_EXTRA_RF.get();
            case DIGGER -> MODULE_DIGGER_EXTRA_RF.get();
            case SILK_TOUCH -> MODULE_SILK_TOUCH_EXTRA_RF.get();
            case FORTUNE -> MODULE_FORTUNE_EXTRA_RF.get();
        };
    }

    public static int maxCountFor(QuarryModules module) {
        return switch (module) {
            case BASE -> MODULE_BASE_MAX_COUNT.get();
            case SPEED -> MODULE_SPEED_MAX_COUNT.get();
            case DIGGER -> MODULE_DIGGER_MAX_COUNT.get();
            case SILK_TOUCH -> MODULE_SILK_TOUCH_MAX_COUNT.get();
            case FORTUNE -> MODULE_FORTUNE_MAX_COUNT.get();
        };
    }

    public static int extraRfForDrill(net.unfamily.another_quarries.mining.QuarryDrillType drill) {
        return switch (drill) {
            case BASE -> 0;
            case DIAMOND -> DRILL_DIAMOND_EXTRA_RF.get();
            case NETHERITE -> DRILL_NETHERITE_EXTRA_RF.get();
        };
    }

    public static int moduleRfComponent(int diggerCount, int speedCount, int fortuneCount, boolean silkTouch) {
        int total = 0;
        total += diggerCount * extraRfFor(QuarryModules.DIGGER);
        total += speedCount * extraRfFor(QuarryModules.SPEED);
        total += fortuneCount * extraRfFor(QuarryModules.FORTUNE);
        if (silkTouch) {
            total += extraRfFor(QuarryModules.SILK_TOUCH);
        }
        return total;
    }

    public static int totalRfPerBlock(
            int diggerCount,
            int speedCount,
            int fortuneCount,
            boolean silkTouch,
            net.unfamily.another_quarries.mining.QuarryDrillType drill) {
        return baseRfPerBlock() + moduleRfComponent(diggerCount, speedCount, fortuneCount, silkTouch) + extraRfForDrill(drill);
    }
}
