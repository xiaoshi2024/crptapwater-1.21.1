package com.xiaoshi2022.crptapwater.client.gui;

import com.xiaoshi2022.crptapwater.network.CorpseNetwork;
import com.xiaoshi2022.crptapwater.network.c2s.CCorpseDialogueAnswerPacket;
import com.xiaoshi2022.crptapwater.network.c2s.CCorpseDialogueClosePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class CorpseDialogScreen extends Screen {

    private final Mob corpse;
    private List<FormattedCharSequence> dialogQuestionText;
    private List<String> dialogAnswers;
    private String dialogQuestionId;
    private String dialogAnswerHover;
    private int timeSinceLastClick = 10;

    public CorpseDialogScreen(Mob corpse) {
        super(Component.literal("Corpse Dialog"));
        this.corpse = corpse;
    }

    public boolean matchesEntity(int entityId) {
        return corpse != null && corpse.getId() == entityId;
    }

    public Mob getCorpse() {
        return corpse;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (corpse == null || !corpse.isAlive()) {
            onClose();
            return;
        }
        if (this.minecraft != null && this.minecraft.player != null && corpse.distanceTo(this.minecraft.player) > 8.0) {
            onClose();
            return;
        }
        timeSinceLastClick++;
    }

    @Override
    public void onClose() {
        if (corpse != null) {
            CorpseNetwork.sendToServer(new CCorpseDialogueClosePacket(corpse.getUUID()));
        }
        super.onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float partialTick) {
        if (corpse == null) return;
        drawDialogue(context, mouseX, mouseY);
    }

    private void drawDialogue(GuiGraphics context, int mouseX, int mouseY) {
        if (dialogQuestionText == null || dialogAnswers == null) return;
        dialogAnswerHover = null;

        Minecraft mc = Minecraft.getInstance();
        var font = mc.font;
        int centerX = this.width / 2;
        int boxWidth = 340;
        int boxHeight = 200;
        int boxLeft = centerX - boxWidth / 2;
        int boxRight = centerX + boxWidth / 2;
        int boxTop = this.height / 2 - boxHeight / 2;
        int boxBottom = this.height / 2 + boxHeight / 2;

        context.fill(boxLeft, boxTop, boxRight, boxBottom, 0xE8220011);
        context.fill(boxLeft + 3, boxTop + 3, boxRight - 3, boxBottom - 3, 0xF01A0520);
        context.renderOutline(boxLeft, boxTop, boxRight - boxLeft, boxBottom - boxTop, 0xAAFF5566);

        Component name = Component.translatable("gui.crptapwater.corpse_dialog.name_prefix").append(corpse.getDisplayName());
        context.drawString(font, name, boxLeft + 12, boxTop + 10, 0xFFFF8888, false);

        if (corpse instanceof LivingEntity le) {
            float hp = le.getHealth();
            float maxHp = le.getMaxHealth();
            int hpColor = 0xFF00FF00;
            if (hp / maxHp < 0.3F) hpColor = 0xFFFF4444;
            else if (hp / maxHp < 0.6F) hpColor = 0xFFFFAA00;
            String hpText = String.format("%.1f / %.1f ❤", hp, maxHp);
            context.drawString(font, hpText, boxRight - 12 - font.width(hpText), boxTop + 10, hpColor, false);
        }

        int y = boxTop + 34;
        int lineGap = font.lineHeight + 2;
        int maxTextWidth = boxWidth - 44;
        for (FormattedCharSequence line : dialogQuestionText) {
            context.drawString(font, line, boxLeft + 18, y, 0xFFEEEEEE, false);
            y += lineGap;
        }

        y += 8;
        int sepY = y;
        context.fill(boxLeft + 16, sepY, boxRight - 16, sepY + 1, 0x77FF4466);
        y += 10;

        for (String a : dialogAnswers) {
            int textW = Math.min(boxWidth - 48, 260);
            int ansX = centerX - textW / 2;
            boolean hover = hoveringOver(ansX, y - 2, textW, 12, mouseX, mouseY);
            Component text = Component.translatable(getAnswerTranslationKey(dialogQuestionId, a));
            int color = hover ? 0xFFFFFF88 : 0xFFDDDDDD;
            context.drawCenteredString(font, text, centerX, y, color);
            if (hover) {
                dialogAnswerHover = a;
            }
            y += 14;
        }

        Component hint = Component.translatable("gui.crptapwater.corpse_dialog.hint");
        context.drawCenteredString(font, hint, centerX, boxBottom - 14, 0x99AAAAAA);
    }

    @Override
    public boolean mouseClicked(double posX, double posY, int button) {
        if (button == 0 && dialogAnswerHover != null
                && dialogQuestionId != null && !dialogQuestionId.isEmpty()
                && timeSinceLastClick > 3) {
            timeSinceLastClick = 0;
            CorpseNetwork.sendToServer(new CCorpseDialogueAnswerPacket(corpse.getUUID(), dialogQuestionId, dialogAnswerHover));
            return true;
        }
        return super.mouseClicked(posX, posY, button);
    }

    public void setDialogue(String question, List<String> answers) {
        dialogQuestionId = question;
        dialogAnswers = answers;
    }

    public void setLastPhrase(Component questionText) {
        dialogQuestionText = font.split(questionText, 296);
    }

    private static String getAnswerTranslationKey(String questionId, String answerId) {
        if ("back".equals(answerId)) {
            return "dialog.crptapwater.answer.back";
        }
        return "dialog.crptapwater.answer." + questionId + "." + answerId;
    }

    private static boolean hoveringOver(int x, int y, int w, int h, int mx, int my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
