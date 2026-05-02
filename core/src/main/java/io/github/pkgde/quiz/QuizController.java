package io.github.pkgde.quiz;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;

import java.util.function.Consumer;

/**
 * Shared, context-agnostic quiz orchestrator.
 */
public class QuizController {

    private boolean quizActive = false;
    private InputProcessor previousInputProcessor;
    private MCQComponent currentMcq;
    private Table wrapperTable;
    private final QuizUIStyles styles;
    private final QuestionProvider questionProvider;

    // Frame-safe pending result
    private Consumer<Boolean> pendingCallback;
    private Boolean pendingResult;
    private boolean resultReady = false;

    public QuizController(QuizUIStyles styles, QuestionProvider questionProvider) {
        this.styles = styles;
        this.questionProvider = questionProvider;
    }

    public void startQuiz(QuizContext ctx, QuestionPresenter presenter,
                          Stage stage, Consumer<Boolean> resultCallback) {
        if (quizActive) return; 

        quizActive = true;
        resultReady = false;
        pendingResult = null;
        pendingCallback = resultCallback;

        // Save current input processor
        previousInputProcessor = Gdx.input.getInputProcessor();
        Gdx.input.setInputProcessor(stage);

        // Fetch question via provider 
        Question question = questionProvider.getQuestion(ctx.getSource());

        // Presenter hook (dialogue narration)
        if (presenter != null) {
            presenter.showQuestion(question.getText());
        }

        // Build MCQ UI
        currentMcq = new MCQComponent(question, styles, selectedIndices -> {
            if (!quizActive || resultReady) return;
            pendingResult = AnswerValidator.validate(question, selectedIndices);
            resultReady = true;
        });

        // --- NEW WRAPPER TABLE LOGIC ---
        wrapperTable = new Table();
        wrapperTable.setFillParent(true);
        
        // FIX: Center the table in the viewport, but push it down 30 pixels 
        // to safely clear the bottom of the Dialogue Box
        wrapperTable.center().padTop(30f); 
        
        wrapperTable.add(currentMcq); 
        stage.addActor(wrapperTable);
    }

    public void update(Stage stage) {
        if (resultReady && quizActive) {
            boolean result = pendingResult;
            Consumer<Boolean> cb = pendingCallback;

            finishQuiz(stage);

            if (cb != null) {
                cb.accept(result);
            }
        }
    }

    public void cancelQuiz(Stage stage) {
        if (!quizActive)
            return;
        finishQuiz(stage);
    }

    public boolean isActive() {
        return quizActive;
    }

    private void finishQuiz(Stage stage) {
        // Safe removal of the layout wrapper
        if (wrapperTable != null) {
            wrapperTable.remove();
            wrapperTable = null;
        }
        currentMcq = null;

        // Restore input processor
        if (quizActive) {
            Gdx.input.setInputProcessor(previousInputProcessor);
            previousInputProcessor = null;
        }

        quizActive = false;
        resultReady = false;
        pendingResult = null;
        pendingCallback = null;
    }
}