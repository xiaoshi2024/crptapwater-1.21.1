package com.xiaoshi2022.crptapwater.register;

import com.xiaoshi2022.crptapwater.CRPTapWater;
import com.xiaoshi2022.crptapwater.item.LongShiMineralWater;
import net.minecraft.world.food.FoodProperties;  // 添加这个导入
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ItemRegistry {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(CRPTapWater.MODID);

    public static final DeferredItem<BlockItem> CORPSE_WATER_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem("corpse_water", BlockRegistry.CORPSE_WATER_BLOCK,
                    new Item.Properties().stacksTo(1));

    public static final DeferredItem<BlockItem> PIPE_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem("pipe", BlockRegistry.PIPE_BLOCK,
                    new Item.Properties());

    public static final DeferredItem<BlockItem> WATER_TROUGH_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem("water_trough", BlockRegistry.WATER_TROUGH_BLOCK,
                    new Item.Properties());

    public static final DeferredItem<BucketItem> CORPSE_WATER_BUCKET =
            ITEMS.register("corpse_water_bucket", () -> new BucketItem(
                    com.xiaoshi2022.crptapwater.fluid.FluidRegistry.CORPSE_WATER.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)
            ));

    public static final DeferredItem<Item> CORPSE_WATER_BOTTLE =
            ITEMS.register("corpse_water_bottle", () -> new Item(
                    new Item.Properties().craftRemainder(Items.GLASS_BOTTLE).stacksTo(16)
                            .food(new FoodProperties.Builder().alwaysEdible().nutrition(0).saturationModifier(0F).build())
            ));

    public static final DeferredItem<Item> LONGSHI_MINERAL_WATER_EMPTY =
            ITEMS.register("longshi_mineral_water_empty", () -> new Item(
                    new Item.Properties().stacksTo(16)
            ));

    public static final DeferredItem<LongShiMineralWater> LONGSHI_MINERAL_WATER =
            ITEMS.register("longshi_mineral_water", () -> new LongShiMineralWater(
                    new Item.Properties().stacksTo(16)
                            .food(new FoodProperties.Builder().alwaysEdible().nutrition(2).saturationModifier(0.5F).build())
            ));
}