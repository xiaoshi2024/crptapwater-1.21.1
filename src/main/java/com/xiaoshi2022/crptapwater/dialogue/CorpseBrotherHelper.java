package com.xiaoshi2022.crptapwater.dialogue;

import com.phagens.corpseorigin.entity.ICorpseBrother;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;

public final class CorpseBrotherHelper {

    public static final int IQ_BEAST = 0;
    public static final int IQ_LOW = 1;
    public static final int IQ_MEDIUM = 2;
    public static final int IQ_HIGH = 3;

    private CorpseBrotherHelper() {}

    public static boolean isCorpseBrother(Mob entity) {
        return entity instanceof ICorpseBrother;
    }

    public static boolean canDialogue(Mob entity) {
        if (!(entity instanceof ICorpseBrother brother)) return false;

        if (isLongyou(entity)) return true;
        if (isCoco(entity)) return true;

        int level = brother.getEvolutionLevel();
        return level >= 1;
    }

    public static int getDialogueIQ(Mob entity) {
        if (entity instanceof ICorpseBrother brother) {
            if (isLongyou(entity)) return IQ_HIGH;
            if (isCoco(entity)) return IQ_MEDIUM;

            int level = brother.getEvolutionLevel();
            if (level >= 3) return IQ_HIGH;
            if (level >= 1) return IQ_LOW;
        }
        return IQ_BEAST;
    }

    public static Component transformSpeechByIQ(Mob entity, Component text) {
        int iq = getDialogueIQ(entity);
        String content = text.getString();

        return switch (iq) {
            case IQ_HIGH -> {
                String trimmed = content.substring(0, Math.min(content.length(), 48));
                yield Component.literal(trimmed + (content.length() > 48 ? "..." : ""));
            }
            case IQ_MEDIUM -> {
                StringBuilder sb = new StringBuilder("咕. ");
                int chop = Math.min(content.length(), 24);
                sb.append(content, 0, chop);
                if (content.length() > chop) sb.append("...");
                sb.append(" 咕.");
                yield Component.literal(sb.toString());
            }
            case IQ_LOW -> {
                StringBuilder sb = new StringBuilder();
                int n = entity.getRandom().nextInt(3) + 2;
                for (int i = 0; i < n; i++) sb.append("咕. ");
                int chop = Math.min(content.length(), 12);
                sb.append(content, 0, chop);
                if (content.length() > chop) sb.append("...");
                for (int i = 0; i < n; i++) sb.append(" 咕.");
                yield Component.literal(sb.toString());
            }
            default -> {
                StringBuilder sb = new StringBuilder();
                int n = entity.getRandom().nextInt(4) + 3;
                for (int i = 0; i < n; i++) sb.append("咕. ");
                yield Component.literal(sb.toString());
            }
        };
    }

    private static boolean isLongyou(Mob entity) {
        return entity.getClass().getName().contains("LongyouEntity");
    }

    private static boolean isCoco(Mob entity) {
        String name = entity.getClass().getName();
        return name.contains("Coco") || name.contains("ZbWorm") || name.contains("ZbrFish");
    }
}
