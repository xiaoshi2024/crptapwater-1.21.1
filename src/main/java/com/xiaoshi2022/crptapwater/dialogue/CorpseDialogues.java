package com.xiaoshi2022.crptapwater.dialogue;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CorpseDialogues {

    public interface DialogueAction {
        void execute(Mob corpse, ServerPlayer player);
    }

    public static class Answer {
        public final String id;
        public final String textKey;
        public final String resultText;
        public final String nextQuestionId;
        public final DialogueAction action;

        public Answer(String id, String textKey) {
            this(id, textKey, null, null, null);
        }

        public Answer(String id, String textKey, String nextQuestionId) {
            this(id, textKey, null, nextQuestionId, null);
        }

        public Answer(String id, String textKey, DialogueAction action) {
            this(id, textKey, null, null, action);
        }

        public Answer(String id, String textKey, String resultText, String nextQuestionId, DialogueAction action) {
            this.id = id;
            this.textKey = textKey;
            this.resultText = resultText;
            this.nextQuestionId = nextQuestionId;
            this.action = action;
        }
    }

    public static class Question {
        public final String id;
        public final String textKey;
        public final List<Answer> answers = new ArrayList<>();

        public Question(String id, String textKey) {
            this.id = id;
            this.textKey = textKey;
        }

        public Question add(Answer answer) {
            answers.add(answer);
            return this;
        }

        public Answer getAnswer(String answerId) {
            for (Answer a : answers) {
                if (a.id.equals(answerId)) return a;
            }
            return null;
        }

        public List<String> getAnswerIds() {
            List<String> ids = new ArrayList<>();
            for (Answer a : answers) ids.add(a.id);
            return ids;
        }
    }

    private static final Map<String, Question> QUESTIONS = new LinkedHashMap<>();

    static {
        Question root = new Question("root", "dialog.crptapwater.question.root")
                .add(new Answer("drink", "dialog.crptapwater.answer.root.drink", "about_drink"))
                .add(new Answer("body", "dialog.crptapwater.answer.root.body", "about_body"))
                .add(new Answer("work", "dialog.crptapwater.answer.root.work", "about_work"))
                .add(new Answer("gift", "dialog.crptapwater.answer.root.gift", "about_gift"))
                .add(new Answer("bye", "dialog.crptapwater.answer.root.bye", (c, p) -> DialogFlow.closeDialogue(c, p)));
        QUESTIONS.put(root.id, root);

        Question aboutDrink = new Question("about_drink", "dialog.crptapwater.question.about_drink")
                .add(new Answer("taste", "dialog.crptapwater.answer.about_drink.taste",
                        "dialog.crptapwater.result.drink_taste", null, null))
                .add(new Answer("recipe", "dialog.crptapwater.answer.about_drink.recipe",
                        "dialog.crptapwater.result.drink_recipe", null, null))
                .add(new Answer("suggest", "dialog.crptapwater.answer.about_drink.suggest",
                        "dialog.crptapwater.result.drink_suggest", null, null))
                .add(new Answer("back", "dialog.crptapwater.answer.back", "root"));
        QUESTIONS.put(aboutDrink.id, aboutDrink);

        Question aboutBody = new Question("about_body", "dialog.crptapwater.question.about_body")
                .add(new Answer("comfort", "dialog.crptapwater.answer.about_body.comfort",
                        "dialog.crptapwater.result.body_comfort", null, null))
                .add(new Answer("heal", "dialog.crptapwater.answer.about_body.heal",
                        "dialog.crptapwater.result.body_heal", null, null))
                .add(new Answer("back", "dialog.crptapwater.answer.back", "root"));
        QUESTIONS.put(aboutBody.id, aboutBody);

        Question aboutWork = new Question("about_work", "dialog.crptapwater.question.about_work")
                .add(new Answer("what", "dialog.crptapwater.answer.about_work.what",
                        "dialog.crptapwater.result.work_what", null, null))
                .add(new Answer("price", "dialog.crptapwater.answer.about_work.price",
                        "dialog.crptapwater.result.work_price", null, null))
                .add(new Answer("back", "dialog.crptapwater.answer.back", "root"));
        QUESTIONS.put(aboutWork.id, aboutWork);

        Question aboutGift = new Question("about_gift", "dialog.crptapwater.question.about_gift")
                .add(new Answer("golden_apple", "dialog.crptapwater.answer.about_gift.golden_apple",
                        (c, p) -> DialogFlow.tryGiveGoldenApple(c, p)))
                .add(new Answer("flesh", "dialog.crptapwater.answer.about_gift.flesh",
                        (c, p) -> DialogFlow.tryGiveFlesh(c, p)))
                .add(new Answer("later", "dialog.crptapwater.answer.about_gift.later",
                        "dialog.crptapwater.result.gift_later", null, null))
                .add(new Answer("back", "dialog.crptapwater.answer.back", "root"));
        QUESTIONS.put(aboutGift.id, aboutGift);
    }

    public static Question getQuestion(String id) {
        return QUESTIONS.getOrDefault(id, QUESTIONS.get("root"));
    }

    public static void handleAnswer(Mob corpse, ServerPlayer player, String questionId, String answerId) {
        Question question = getQuestion(questionId);
        Answer answer = question.getAnswer(answerId);
        if (answer == null) return;

        if (answer.action != null) {
            answer.action.execute(corpse, player);
            return;
        }

        if (answer.resultText != null) {
            DialogFlow.sayThenClose(corpse, player, answer.resultText);
        } else if (answer.nextQuestionId != null) {
            DialogFlow.continueDialogue(corpse, player, answer.nextQuestionId);
        }
    }
}
