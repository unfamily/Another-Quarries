package net.unfamily.another_quarries.config;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.unfamily.another_quarries.AnotherQuarries;
import net.unfamily.another_quarries.item.QuarryModules;
import net.unfamily.another_quarries.mining.QuarryMiningFilters;

import java.util.List;

@EventBusSubscriber(modid = AnotherQuarries.MOD_ID)
public final class ModConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.comment("Quarry base settings").push("000_quarry");
    }

    private static final ModConfigSpec.IntValue RF_PER_BLOCK = BUILDER
            .comment("Default RF consumed per block mined")
            .defineInRange("000_rfPerBlock", 10, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue QUARRY_ENERGY_BUFFER = BUILDER
            .comment("Internal RF buffer capacity for the quarry")
            .defineInRange("001_energyBuffer", 100_000, 1, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue QUARRY_BASE_RANGE = BUILDER
            .comment("Default max blocks per axis when no range modules are installed")
            .defineInRange("002_baseRange", 4, 1, 64);

    private static final ModConfigSpec.IntValue QUARRY_MAX_RANGE = BUILDER
            .comment("Absolute max blocks per axis for the quarry area")
            .defineInRange("003_maxRange", 256, 1, 256);

    private static final ModConfigSpec.IntValue BASE_BREAK_TICKS = BUILDER
            .comment("Base ticks to break one block with one worker and no speed modules (20 ticks = 1 second)")
            .defineInRange("004_baseBreakTicks", 60, 1, 20 * 60 * 10);

    private static final ModConfigSpec.IntValue MAX_DRONES = BUILDER
            .comment("Maximum drones in the drone equipment slot")
            .defineInRange("005_maxDrones", 64, 1, 64);

    private static final ModConfigSpec.IntValue REGEN_SCAN_INTERVAL_TICKS = BUILDER
            .comment("Ticks between backward scans for regenerated blocks in the cleared area (20 ticks = 1 second)")
            .defineInRange("006_regenScanIntervalTicks", 200, 20, 20 * 60 * 10);

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
        BUILDER.comment("Automatic quarry frame (structure_quarry border)").push("011_frame");
    }

    private static final ModConfigSpec.IntValue FRAME_VALIDATION_INTERVAL_TICKS = BUILDER
            .comment("Ticks between border frame integrity scans while mining (20 ticks = 1 second)")
            .defineInRange("frameValidationIntervalTicks", 200, 20, 20 * 60 * 10);

    static {
        BUILDER.pop();
        BUILDER.comment("Module limits and extra RF per block").push("100_modules");
    }

    private static final ModConfigSpec.IntValue MODULE_BASE_EXTRA_RF = moduleExtraRf("module_base");
    private static final ModConfigSpec.IntValue MODULE_SPEED_EXTRA_RF = moduleExtraRf("module_speed");
    private static final ModConfigSpec.IntValue MODULE_DIGGER_EXTRA_RF = moduleExtraRf("module_digger");
    private static final ModConfigSpec.IntValue MODULE_SILK_TOUCH_EXTRA_RF = moduleExtraRf("module_silktouch");
    private static final ModConfigSpec.IntValue MODULE_FORTUNE_EXTRA_RF = moduleExtraRf("module_fortune");

    private static final ModConfigSpec.IntValue MODULE_BASE_MAX_COUNT = moduleMaxCount("module_base");
    private static final ModConfigSpec.IntValue MODULE_SPEED_MAX_COUNT = moduleMaxCount("module_speed", 16);
    private static final ModConfigSpec.IntValue MODULE_DIGGER_MAX_COUNT = moduleMaxCount("module_digger", 16);
    private static final ModConfigSpec.IntValue MODULE_SILK_TOUCH_MAX_COUNT = moduleMaxCount("module_silktouch", 1);
    private static final ModConfigSpec.IntValue MODULE_FORTUNE_MAX_COUNT = moduleMaxCount("module_fortune", 3);

    private static final ModConfigSpec.DoubleValue SPEED_TICK_MULTIPLIER = BUILDER
            .comment("Speed multiplier per speed module (1.0 = no change; 1.1 = 10% faster per module)")
            .defineInRange("2speed_tickMultiplier", 1.10, 1.0, 4.0);

    private static final ModConfigSpec.DoubleValue DIGGER_TICK_MULTIPLIER = BUILDER
            .comment("Speed multiplier per digger module on non-ore blocks")
            .defineInRange("2digger_tickMultiplier", 1.15, 1.0, 4.0);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private static ModConfigSpec.IntValue moduleExtraRf(String key) {
        return BUILDER
                .comment("Extra RF per block when " + key + " is installed")
                .defineInRange("1" + key.substring("module_".length()) + "_extraRf", 0, 0, Integer.MAX_VALUE);
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

    @SubscribeEvent
    static void onLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) {
            QuarryMiningFilters.reload();
            AnotherQuarries.LOGGER.debug("Loaded {}", AnotherQuarries.MOD_ID + " config");
        }
    }

    @SubscribeEvent
    static void onReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) {
            QuarryMiningFilters.reload();
        }
    }

    public static int baseRfPerBlock() {
        return RF_PER_BLOCK.get();
    }

    public static int quarryEnergyBuffer() {
        return QUARRY_ENERGY_BUFFER.get();
    }

    public static int quarryBaseRange() {
        return QUARRY_BASE_RANGE.get();
    }

    public static int quarryMaxRange() {
        return QUARRY_MAX_RANGE.get();
    }

    public static int baseBreakTicks() {
        return BASE_BREAK_TICKS.get();
    }

    public static int maxDrones() {
        return MAX_DRONES.get();
    }

    public static int regenScanIntervalTicks() {
        return REGEN_SCAN_INTERVAL_TICKS.get();
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

    public static int frameValidationIntervalTicks() {
        return FRAME_VALIDATION_INTERVAL_TICKS.get();
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

    public static int totalRfPerBlock(int diggerCount, int speedCount, int fortuneCount, boolean silkTouch) {
        int total = baseRfPerBlock();
        total += diggerCount * extraRfFor(QuarryModules.DIGGER);
        total += speedCount * extraRfFor(QuarryModules.SPEED);
        total += fortuneCount * extraRfFor(QuarryModules.FORTUNE);
        if (silkTouch) {
            total += extraRfFor(QuarryModules.SILK_TOUCH);
        }
        return total;
    }
}
