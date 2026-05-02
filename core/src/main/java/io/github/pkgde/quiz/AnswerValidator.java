package io.github.pkgde.quiz;

import java.util.Set;

public class AnswerValidator {
    
    /**
     * Validates the selected indices against the question's correct indices.
     * Enforces EXACT MATCH (no missing, no extra).
     * This method is pure, deterministic, and side-effect free.
     *
     * @param question The question containing the correct indices.
     * @param selected The set of indices selected by the player.
     * @return true if exactly matched, false otherwise.
     */
    public static boolean validate(Question question, Set<Integer> selected) {
        if (question == null || selected == null) {
            return false;
        }
        return question.getCorrectIndices().equals(selected);
    }
}
