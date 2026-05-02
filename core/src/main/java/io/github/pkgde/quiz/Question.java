package io.github.pkgde.quiz;

import java.util.List;
import java.util.Set;
import java.util.Collections;

public class Question {
    private final String text;
    private final List<String> options;
    private final Set<Integer> correctIndices;

    public Question(String text, List<String> options, Set<Integer> correctIndices) {
        if (options == null || options.size() != 4) {
            throw new IllegalArgumentException("Question must have exactly 4 options.");
        }
        this.text = text;
        this.options = Collections.unmodifiableList(options);
        this.correctIndices = Collections.unmodifiableSet(correctIndices);
    }

    public String getText() {
        return text;
    }

    public List<String> getOptions() {
        return options;
    }

    public Set<Integer> getCorrectIndices() {
        return correctIndices;
    }
}
