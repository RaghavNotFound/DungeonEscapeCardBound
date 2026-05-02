package io.github.pkgde.quiz;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Self-contained MCQ dialog component.
 */
public class MCQComponent extends Table {

    private final Set<Integer> selectedIndices = new HashSet<>();
    private final TextButton submitBtn;
    private boolean submitted = false;

    public MCQComponent(Question question, QuizUIStyles styles, Consumer<Set<Integer>> onSubmit) {
        setBackground(styles.dialogBackground);
        pad(15f, 30f, 15f, 30f); 
        setTransform(true);

        // === Instruction ===
        Label instructionLabel = new Label("Select your answer(s):", styles.instructionStyle);
        add(instructionLabel).padBottom(10).center().row();

        // === Option buttons ===
        for (int i = 0; i < question.getOptions().size(); i++) {
            final int index = i;
            TextButton optionBtn = new TextButton(question.getOptions().get(i), styles.optionStyle);

            // Allow long answers to wrap nicely if needed
            optionBtn.getLabel().setWrap(true);

            optionBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (submitted)
                        return;
                    if (optionBtn.isChecked()) {
                        selectedIndices.add(index);
                    } else {
                        selectedIndices.remove(index);
                    }
                    updateSubmitButton();
                }
            });

            // FIX: Use expandX() and fillX() to let the button stretch to fit the text
            // minWidth ensures it's wide enough to look like a proper menu
            add(optionBtn).expandX().fillX().minWidth(350).minHeight(40).padBottom(5).row();
        }

        // === Submit button ===
        submitBtn = new TextButton("Submit", styles.submitStyle);
        submitBtn.setDisabled(true);
        submitBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (!submitBtn.isDisabled() && !submitted) {
                    submitted = true;
                    onSubmit.accept(new HashSet<>(selectedIndices));
                }
            }
        });

        // Slightly wider submit button for a cleaner look
        add(submitBtn).width(200).height(45).padTop(10).center();

        pack();

        // === Fade-in animation ===
        getColor().a = 0f;
        addAction(Actions.fadeIn(0.25f));
    }

    private void updateSubmitButton() {
        submitBtn.setDisabled(selectedIndices.isEmpty());
    }
}