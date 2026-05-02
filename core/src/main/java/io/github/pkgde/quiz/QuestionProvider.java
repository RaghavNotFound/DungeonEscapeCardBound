package io.github.pkgde.quiz;

/**
 * Abstraction for sourcing questions.
 * Supports future extensibility: difficulty scaling,
 * category filtering, boss-specific vs door-specific pools.
 */
public interface QuestionProvider {

    /**
     * Returns a question appropriate for the given source context.
     *
     * @param source the quiz trigger context (CARD or DOOR)
     * @return a Question to present to the player
     */
    Question getQuestion(QuizContext.Source source);
}
