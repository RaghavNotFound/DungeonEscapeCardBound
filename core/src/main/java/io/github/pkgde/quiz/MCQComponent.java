package io.github.pkgde.quiz;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class MCQComponent extends Table {

    private final Set<Integer> selectedIndices = new HashSet<>();
    private final TextButton submitBtn;

    public MCQComponent(Question question, QuizUIStyles styles, Consumer<Set<Integer>> onSubmit) {
        setFillParent(true);
        // Add a slight dark background to the whole table or just center it.
        // For simplicity, we just center elements.
        center();

        Label instructionLabel = new Label("Select one or more answers", styles.labelStyle);
        add(instructionLabel).padBottom(20).row();

        for (int i = 0; i < question.getOptions().size(); i++) {
            final int index = i;
            TextButton optionBtn = new TextButton(question.getOptions().get(i), styles.optionStyle);
            optionBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (optionBtn.isChecked()) {
                        selectedIndices.add(index);
                    } else {
                        selectedIndices.remove(index);
                    }
                    updateSubmitButton();
                }
            });
            add(optionBtn).width(400).height(50).padBottom(10).row();
        }

        submitBtn = new TextButton("Submit", styles.submitStyle);
        submitBtn.setDisabled(true); // Disable initially
        submitBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (!submitBtn.isDisabled()) {
                    // Create a copy of the set to prevent external modification
                    onSubmit.accept(new HashSet<>(selectedIndices));
                }
            }
        });
        add(submitBtn).width(200).height(50).padTop(20);
    }

    private void updateSubmitButton() {
        submitBtn.setDisabled(selectedIndices.isEmpty());
    }
}
