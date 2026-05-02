package io.github.pkgde.quiz;

/**
 * Metadata-only context describing WHAT triggered a quiz.
 * Contains NO question data — the QuizController fetches
 * the question from the QuestionProvider using the source.
 */
public class QuizContext {

    public enum Source { CARD, DOOR }

    private final Source source;
    private final boolean cancellable;

    /**
     * @param source      what triggered the quiz (CARD or DOOR)
     * @param cancellable whether the player can cancel (ESC) during this quiz
     */
    public QuizContext(Source source, boolean cancellable) {
        this.source = source;
        this.cancellable = cancellable;
    }

    public Source getSource() {
        return source;
    }

    public boolean isCancellable() {
        return cancellable;
    }
}
