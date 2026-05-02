package io.github.pkgde.quiz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * A hardcoded bank of questions.
 * Questions are shuffled once at startup and flagged as "used" when drawn.
 */
public class HardcodedQuestionProvider implements QuestionProvider {

    private final List<Question> cardQuestions = new ArrayList<>();
    private final List<Question> doorQuestions = new ArrayList<>();

    public HardcodedQuestionProvider() {
        buildCardQuestions();
        buildDoorQuestions();
        
        // Shuffle both decks once at the start of the game
        java.util.Collections.shuffle(cardQuestions);
        java.util.Collections.shuffle(doorQuestions);
    }

    @Override
    public Question getQuestion(QuizContext.Source source) {
        List<Question> pool = (source == QuizContext.Source.DOOR) ? doorQuestions : cardQuestions;

        // Loop through the shuffled pool and find the first UNUSED question
        for (Question q : pool) {
            if (!q.isUsed()) {
                q.setUsed(true); // Flag it so it is never reused in this run
                return q;
            }
        }

        // FALLBACK: If the player exhausts the entire pool of questions in a single run
        return new Question(
            "SYSTEM DEPLETED: You have answered all available questions! Free pass!",
            Arrays.asList("Accept", "Acknowledge", "Proceed", "Continue"),
            new HashSet<>(Arrays.asList(0, 1, 2, 3)) // All answers correct
        );
    }

    // ==========================================
    // QUESTION BANKS
    // ==========================================

    private void buildCardQuestions() {
        // SYNTAX: addCard("Question", "Option 0", "Option 1", "Option 2", "Option 3", CorrectIndex1, CorrectIndex2...);
        
        addCard("Which of the following are valid Java access modifiers?", "public", "friend", "protected", "visible", 0, 2);
        addCard("Which keyword is used to inherit a class in Java?", "implements", "extends", "inherits", "super", 1);
        addCard("Which of these are primitive types in Java?", "int", "String", "boolean", "Integer", 0, 2);
        addCard("What does the 'final' keyword prevent?", "Overriding", "Overloading", "Instantiation", "Compilation", 0);
        addCard("Which collections allow duplicate elements?", "List", "Set", "ArrayList", "HashSet", 0, 2);
        addCard("What is the default value of an int in Java?", "0", "null", "1", "undefined", 0);
        addCard("Which operator is used to allocate memory for an object?", "malloc", "alloc", "new", "create", 2);
        addCard("Which of these are valid loop constructs in Java?", "for", "foreach", "while", "loop", 0, 2);
        addCard("What is the size of a boolean variable in Java?", "1 bit", "8 bits", "16 bits", "Depends on JVM", 3);
        addCard("Which method is the entry point of a Java program?", "start()", "init()", "main()", "run()", 2);
        addCard("Which concept allows multiple methods with the same name but different parameters?", "Overriding", "Overloading", "Polymorphism", "Encapsulation", 1);
        addCard("Which of these keywords are used for exception handling?", "try", "catch", "throw", "All of the above", 3);
        addCard("What is the parent class of all Java classes?", "Main", "Object", "System", "Class", 1);
        addCard("Which keyword is used to refer to the current object?", "this", "super", "self", "me", 0);
        addCard("Which statement is used to stop a loop prematurely?", "stop", "exit", "break", "return", 2);
        addCard("Which package is automatically imported in every Java program?", "java.util", "java.io", "java.lang", "java.net", 2);
        addCard("What is used to restrict a variable from being modified?", "static", "const", "final", "immutable", 2);
        addCard("Which of the following are interfaces in Java?", "Runnable", "Thread", "Serializable", "String", 0, 2);
        addCard("How do you compare two Strings for equality in Java?", "==", "equals()", "compare()", "match()", 1);
        addCard("Which symbol is used for a single-line comment?", "/*", "//", "<!--", "#", 1);
        
        // Add your remaining 130 Card questions here using the addCard() helper!
    }

    private void buildDoorQuestions() {
        // SYNTAX: addDoor("Question", "Option 0", "Option 1", "Option 2", "Option 3", CorrectIndex1, CorrectIndex2...);
        
        addDoor("What is the average time complexity of QuickSort?", "O(n log n)", "O(n^2)", "O(n)", "O(log n)", 0);
        addDoor("Which data structure is best for implementing a FIFO queue?", "Stack", "LinkedList", "Binary Tree", "Heap", 1);
        addDoor("What is the time complexity of searching a sorted array with Binary Search?", "O(n)", "O(n log n)", "O(log n)", "O(1)", 2);
        addDoor("Which of these are object-oriented principles?", "Encapsulation", "Recursion", "Polymorphism", "Compilation", 0, 2);
        addDoor("What does OOP stand for?", "Object-Oriented Programming", "Open Online Platform", "Ordered Object Processing", "Output Protocol", 0);
        addDoor("Which sorting algorithm has a worst-case of O(n log n)?", "Bubble Sort", "Merge Sort", "Selection Sort", "Insertion Sort", 1);
        addDoor("What does 'static' mean in Java?", "Belongs to the class", "Cannot be changed", "Runs at startup", "Is thread-safe", 0);
        addDoor("Which data structure uses LIFO?", "Queue", "Array", "Stack", "Tree", 2);
        addDoor("What is the time complexity of accessing an element in an Array by index?", "O(n)", "O(log n)", "O(1)", "O(n^2)", 2);
        addDoor("Which of the following are tree traversal methods?", "In-order", "Pre-order", "Cross-order", "Post-order", 0, 1, 3);
        addDoor("Which design pattern ensures only one instance of a class exists?", "Factory", "Singleton", "Observer", "Decorator", 1);
        addDoor("In a Hash Map, what handles indexing?", "The keys", "The values", "A Hash Function", "A Linked List", 2);
        addDoor("What characterizes a Directed Acyclic Graph (DAG)?", "It has cycles", "It has no cycles", "Edges have direction", "Nodes are unlinked", 1, 2);
        addDoor("Which graph algorithm finds the shortest path?", "Dijkstra's", "Depth First Search", "Kruskal's", "Merge Sort", 0);
        addDoor("What is a memory leak?", "RAM overheating", "Unreachable objects not garbage collected", "Hard drive failure", "Stack Overflow", 1);
        
        // Add your remaining 35 Door questions here using the addDoor() helper!
    }

    // ==========================================
    // SHORTHAND HELPER METHODS
    // ==========================================

    /**
     * Helper method to drastically reduce the lines of code needed to add a CARD question.
     */
    private void addCard(String text, String opt0, String opt1, String opt2, String opt3, Integer... correctIndices) {
        cardQuestions.add(new Question(text, Arrays.asList(opt0, opt1, opt2, opt3), new HashSet<>(Arrays.asList(correctIndices))));
    }

    /**
     * Helper method to drastically reduce the lines of code needed to add a DOOR question.
     */
    private void addDoor(String text, String opt0, String opt1, String opt2, String opt3, Integer... correctIndices) {
        doorQuestions.add(new Question(text, Arrays.asList(opt0, opt1, opt2, opt3), new HashSet<>(Arrays.asList(correctIndices))));
    }
}
