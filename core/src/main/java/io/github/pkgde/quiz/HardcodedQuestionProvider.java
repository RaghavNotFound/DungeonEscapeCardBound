package io.github.pkgde.quiz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * A hardcoded bank of ~12 questions for initial implementation.
 * Questions are selected randomly; the same question won't repeat
 * until all questions have been used (per source).
 */
public class HardcodedQuestionProvider implements QuestionProvider {

    private final List<Question> cardQuestions = new ArrayList<>();
    private final List<Question> doorQuestions = new ArrayList<>();
    private int cardIndex = 0;
    private int doorIndex = 0;

    public HardcodedQuestionProvider() {
        buildCardQuestions();
        buildDoorQuestions();
        shuffle(cardQuestions);
        shuffle(doorQuestions);
    }

    @Override
    public Question getQuestion(QuizContext.Source source) {
        switch (source) {
            case CARD:
                if (cardIndex >= cardQuestions.size()) {
                    Question last = cardQuestions.get(cardQuestions.size() - 1);
                    shuffle(cardQuestions);
                    // Avoid immediate repeat
                    if (cardQuestions.get(0) == last && cardQuestions.size() > 1) {
                        java.util.Collections.swap(cardQuestions, 0, 1);
                    }
                    cardIndex = 0;
                }
                return cardQuestions.get(cardIndex++);

            case DOOR:
                if (doorIndex >= doorQuestions.size()) {
                    Question last = doorQuestions.get(doorQuestions.size() - 1);
                    shuffle(doorQuestions);
                    // Avoid immediate repeat
                    if (doorQuestions.get(0) == last && doorQuestions.size() > 1) {
                        java.util.Collections.swap(doorQuestions, 0, 1);
                    }
                    doorIndex = 0;
                }
                return doorQuestions.get(doorIndex++);

            default:
                return cardQuestions.get(0);
        }
    }

    private void shuffle(List<Question> list) {
        java.util.Collections.shuffle(list);
    }

    private void buildCardQuestions() {
        cardQuestions.add(new Question(
            "Which of the following are valid Java access modifiers?",
            Arrays.asList("public", "friend", "protected", "visible"),
            new HashSet<>(Arrays.asList(0, 2))
        ));
        cardQuestions.add(new Question(
            "Which keyword is used to inherit a class in Java?",
            Arrays.asList("implements", "extends", "inherits", "super"),
            new HashSet<>(Arrays.asList(1))
        ));
        cardQuestions.add(new Question(
            "Which of these are primitive types in Java?",
            Arrays.asList("int", "String", "boolean", "Integer"),
            new HashSet<>(Arrays.asList(0, 2))
        ));
        cardQuestions.add(new Question(
            "What does the 'final' keyword prevent?",
            Arrays.asList("Overriding", "Overloading", "Instantiation", "Compilation"),
            new HashSet<>(Arrays.asList(0))
        ));
        cardQuestions.add(new Question(
            "Which collections allow duplicate elements?",
            Arrays.asList("List", "Set", "ArrayList", "HashSet"),
            new HashSet<>(Arrays.asList(0, 2))
        ));
        cardQuestions.add(new Question(
            "What is the default value of an int in Java?",
            Arrays.asList("0", "null", "1", "undefined"),
            new HashSet<>(Arrays.asList(0))
        ));
    }

    private void buildDoorQuestions() {
        doorQuestions.add(new Question(
            "What is the average time complexity of QuickSort?",
            Arrays.asList("O(n log n)", "O(n^2)", "O(n)", "O(log n)"),
            new HashSet<>(Arrays.asList(0))
        ));

        doorQuestions.add(new Question(
            "Which data structure is best for implementing a FIFO queue?",
            Arrays.asList("Stack", "LinkedList", "Binary Tree", "Heap"),
            new HashSet<>(Arrays.asList(1))
        ));

        doorQuestions.add(new Question(
            "What is the time complexity of searching a sorted array with Binary Search?",
            Arrays.asList("O(n)", "O(n log n)", "O(log n)", "O(1)"),
            new HashSet<>(Arrays.asList(2))
        ));
        doorQuestions.add(new Question(
            "Which of these are object-oriented principles?",
            Arrays.asList("Encapsulation", "Recursion", "Polymorphism", "Compilation"),
            new HashSet<>(Arrays.asList(0, 2))
        ));
        doorQuestions.add(new Question(
            "What does OOP stand for?",
            Arrays.asList("Object-Oriented Programming", "Open Online Platform",
                "Ordered Object Processing", "Output-Oriented Protocol"),
            new HashSet<>(Arrays.asList(0))
        ));
        doorQuestions.add(new Question(
            "Which sorting algorithm has worst-case O(n log n)?",
            Arrays.asList("Bubble Sort", "Merge Sort", "Selection Sort", "Insertion Sort"),
            new HashSet<>(Arrays.asList(1))
        ));
        doorQuestions.add(new Question(
            "What does 'static' mean in Java?",
            Arrays.asList("Belongs to the class", "Cannot be changed",
                "Runs at startup", "Is thread-safe"),
            new HashSet<>(Arrays.asList(0))
        ));
    }
}
