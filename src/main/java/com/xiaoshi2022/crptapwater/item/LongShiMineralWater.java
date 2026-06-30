package com.xiaoshi2022.crptapwater.item;

import com.xiaoshi2022.crptapwater.register.ItemRegistry;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class LongShiMineralWater extends Item {

    public LongShiMineralWater(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
            serverPlayer.awardStat(Stats.ITEM_USED.get(this));
        }

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            try {
                com.phagens.corpseorigin.api.watercompany.WaterCompanyAPI.getInfectionTrigger()
                        .triggerInfection(entity, serverLevel);
                // com.xiaoshi2022.crptapwater.CRPTapWater.LOGGER.info(
                //         "{} 喝下了矿泉水(龙氏)，感染了尸兄病毒！", entity.getName().getString());
            } catch (Exception e) {
                com.xiaoshi2022.crptapwater.CRPTapWater.LOGGER.warn("触发尸兄感染失败: {}", e.getMessage());
            }

            entity.addEffect(new MobEffectInstance(
                    MobEffects.CONFUSION,
                    200,
                    0,
                    false,
                    true,
                    true
            ));

            entity.addEffect(new MobEffectInstance(
                    MobEffects.POISON,
                    100,
                    0,
                    false,
                    true,
                    true
            ));

            entity.addEffect(new MobEffectInstance(
                    MobEffects.WEAKNESS,
                    300,
                    0,
                    false,
                    true,
                    true
            ));

            entity.addEffect(new MobEffectInstance(
                    MobEffects.HUNGER,
                    400,
                    1,
                    false,
                    true,
                    true
            ));
        }

        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS,
                1.0F, 1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F));

        if (stack.isEmpty()) {
            return new ItemStack(ItemRegistry.LONGSHI_MINERAL_WATER_EMPTY.get());
        } else {
            if (entity instanceof Player player && !player.getAbilities().instabuild) {
                stack.shrink(1);
                ItemStack emptyBottle = new ItemStack(ItemRegistry.LONGSHI_MINERAL_WATER_EMPTY.get());
                if (!player.getInventory().add(emptyBottle)) {
                    player.drop(emptyBottle, false);
                }
            }
            return stack;
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 40;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }
}

