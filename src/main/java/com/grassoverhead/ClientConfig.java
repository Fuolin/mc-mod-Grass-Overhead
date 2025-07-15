package com.grassoverhead;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ClientConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    // 配置项定义
    public static final ModConfigSpec.ConfigValue<String> BLOCK_TYPE;
    public static final ModConfigSpec.DoubleValue HEIGHT_OFFSET;

    static {
        // 方块类型配置 (默认值为草方块)
        BLOCK_TYPE = BUILDER
                .comment("The block type to render above players (e.g. minecraft:grass_block, minecraft:diamond_block)")
                .define("blockType", "minecraft:grass_block");

        // 高度偏移配置 (默认值1.5)
        HEIGHT_OFFSET = BUILDER
                .comment("Vertical offset above players' heads")
                .defineInRange("heightOffset", 1.5, 0.5, 5.0);

        SPEC = BUILDER.build();
    }
}