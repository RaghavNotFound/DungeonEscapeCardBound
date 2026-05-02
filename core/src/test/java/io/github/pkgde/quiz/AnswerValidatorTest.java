package io.github.pkgde.quiz;

import org.junit.Test;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

public class AnswerValidatorTest {

    @Test
    public void testExactMatch_SingleAnswer_ReturnsTrue() {
        Question q = new Question("Q1", Arrays.asList("A", "B", "C", "D"), new HashSet<>(Arrays.asList(1)));
        Set<Integer> selected = new HashSet<>(Arrays.asList(1));
        assertTrue(AnswerValidator.validate(q, selected));
    }

    @Test
    public void testExactMatch_MultiAnswer_ReturnsTrue() {
        Question q = new Question("Q1", Arrays.asList("A", "B", "C", "D"), new HashSet<>(Arrays.asList(0, 2)));
        Set<Integer> selected = new HashSet<>(Arrays.asList(0, 2));
        assertTrue(AnswerValidator.validate(q, selected));
    }

    @Test
    public void testMissingAnswer_ReturnsFalse() {
        Question q = new Question("Q1", Arrays.asList("A", "B", "C", "D"), new HashSet<>(Arrays.asList(0, 2)));
        Set<Integer> selected = new HashSet<>(Arrays.asList(0));
        assertFalse(AnswerValidator.validate(q, selected));
    }

    @Test
    public void testExtraAnswer_ReturnsFalse() {
        Question q = new Question("Q1", Arrays.asList("A", "B", "C", "D"), new HashSet<>(Arrays.asList(1)));
        Set<Integer> selected = new HashSet<>(Arrays.asList(1, 2));
        assertFalse(AnswerValidator.validate(q, selected));
    }

    @Test
    public void testEmptySelection_WhenAnswerExists_ReturnsFalse() {
        Question q = new Question("Q1", Arrays.asList("A", "B", "C", "D"), new HashSet<>(Arrays.asList(1)));
        Set<Integer> selected = new HashSet<>();
        assertFalse(AnswerValidator.validate(q, selected));
    }
}
