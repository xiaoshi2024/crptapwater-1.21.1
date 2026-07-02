package com.xiaoshi2022.crptapwater.dialogue;

import com.xiaoshi2022.crptapwater.network.CorpseNetwork;
import com.xiaoshi2022.crptapwater.network.s2c.SCorpseDialogueQuestionPacket;
import com.xiaoshi2022.crptapwater.network.s2c.SCorpseDialogueResponsePacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Collections;

public class DialogFlow {

    public static void continueDialogue(Mob corpse, ServerPlayer player, String nextQuestionId) {
        CorpseDialogues.Question question = CorpseDialogues.getQuestion(nextQuestionId);
        Component questionText = Component.translatable(question.textKey);
        Component zombieText = CorpseBrotherHelper.transformSpeechByIQ(corpse, questionText);

        CorpseNetwork.sendToPlayer(new SCorpseDialogueQuestionPacket(corpse.getId(), zombieText, false), player);
        CorpseNetwork.sendToPlayer(new SCorpseDialogueResponsePacket(corpse.getId(), question.id, question.getAnswerIds()), player);
    }

    public static void sayThenClose(Mob corpse, ServerPlayer player, String responseTextKey) {
        Component sayText = Component.translatable(responseTextKey);
        Component zombieText = CorpseBrotherHelper.transformSpeechByIQ(corpse, sayText);

        CorpseNetwork.sendToPlayer(new SCorpseDialogueQuestionPacket(corpse.getId(), zombieText, false), player);
        CorpseNetwork.sendToPlayer(new SCorpseDialogueResponsePacket(corpse.getId(), "", Collections.emptyList()), player);
    }

    public static void closeDialogue(Mob corpse, ServerPlayer player) {
    }

    public static void tryGiveGoldenApple(Mob corpse, ServerPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.is(Items.GOLDEN_APPLE)) {
            if (!player.getAbilities().instabuild) {
                mainHand.shrink(1);
            }
            sayThenClose(corpse, player, "dialog.crptapwater.result.accept_golden_apple");
        } else {
            sayThenClose(corpse, player, "dialog.crptapwater.result.no_golden_apple");
        }
    }

    public static void tryGiveFlesh(Mob corpse, ServerPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.is(Items.ROTTEN_FLESH)) {
            if (!player.getAbilities().instabuild) {
                mainHand.shrink(1);
            }
            corpse.heal(4.0F);
            sayThenClose(corpse, player, "dialog.crptapwater.result.accept_flesh");
        } else {
            sayThenClose(corpse, player, "dialog.crptapwater.result.no_flesh");
        }
    }
}
