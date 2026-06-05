package net.unfamily.another_quarries.mining;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.unfamily.another_quarries.registry.ModItems;

public enum QuarryDrillType {
    BASE(Items.IRON_PICKAXE, 1.0F, QuarryMiningLevels.TIER_IRON),
    DIAMOND(Items.DIAMOND_PICKAXE, 1.15F, QuarryMiningLevels.TIER_DIAMOND),
    NETHERITE(Items.NETHERITE_PICKAXE, 1.25F, QuarryMiningLevels.TIER_NETHERITE),
    DRILL_LASER(ModItems.DRILL_LASER.get(), 1.35F, QuarryMiningLevels.TIER_MODDED);

    private final ItemStack toolTemplate;
    private final float speedMultiplier;
    private final int maxMiningLevel;

    QuarryDrillType(net.minecraft.world.item.Item templateItem, float speedMultiplier, int maxMiningLevel) {
        this.toolTemplate = new ItemStack(templateItem);
        this.speedMultiplier = speedMultiplier;
        this.maxMiningLevel = maxMiningLevel;
    }

    public ItemStack toolTemplate() {
        return toolTemplate.copy();
    }

    public float speedMultiplier() {
        return speedMultiplier;
    }

    public int maxMiningLevel() {
        return maxMiningLevel;
    }
}
