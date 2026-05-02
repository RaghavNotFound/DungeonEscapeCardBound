package io.github.pkgde.quiz;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import io.github.pkgde.BossFightScreen.Card;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class CardQuizHandler {

    private boolean quizActive = false;
    private InputProcessor previousInputProcessor;
    private MCQComponent currentMcqComponent;
    private final QuizUIStyles styles;

    // Mock database
    private final Question mockQuestion = new Question(
        "Which of the following are valid Java access modifiers?",
        Arrays.asList("public", "friend", "protected", "visible"),
        new HashSet<>(Arrays.asList(0, 2))
    );

    public CardQuizHandler(QuizUIStyles styles) {
        this.styles = styles;
    }

    public void startQuizForCard(Card c, QuestionPresenter presenter, Stage stage, Consumer<Boolean> resultCallback) {
        if (quizActive) return; // Prevent re-entrancy

        quizActive = true;
        previousInputProcessor = Gdx.input.getInputProcessor();
        Gdx.input.setInputProcessor(stage);

        presenter.showQuestion(mockQuestion.getText());

        currentMcqComponent = new MCQComponent(mockQuestion, styles, selectedIndices -> {
            if (!quizActive) return;
            
            boolean isCorrect = AnswerValidator.validate(mockQuestion, selectedIndices);
            
            finishQuiz(stage);
            
            // Fire callback EXACTLY ONCE
            resultCallback.accept(isCorrect);
        });

        stage.addActor(currentMcqComponent);
    }

    public void cancelQuiz(Stage stage) {
        if (!quizActive) return;
        finishQuiz(stage);
    }

    private void finishQuiz(Stage stage) {
        quizActive = false;
        if (currentMcqComponent != null) {
            currentMcqComponent.remove(); // Remove ONLY the MCQ Component, do not clear the whole stage
            currentMcqComponent = null;
        }
        // Restore input
        Gdx.input.setInputProcessor(previousInputProcessor);
        previousInputProcessor = null;
    }
}
