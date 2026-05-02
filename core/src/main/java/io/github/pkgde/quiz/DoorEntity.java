package io.github.pkgde.quiz;

import com.badlogic.gdx.math.Rectangle;

/**
 * Represents a door/gate in the map that can be locked.
 * Contains ONLY spatial data + lock state.
 * The door does NOT store a Question — the QuizController
 * fetches questions dynamically from the QuestionProvider.
 */
public class DoorEntity {

    private final Rectangle bounds;
    private boolean locked;

    public DoorEntity(float x, float y, float width, float height) {
        this.bounds = new Rectangle(x, y, width, height);
        this.locked = true;
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public boolean isLocked() {
        return locked;
    }

    public void unlock() {
        this.locked = false;
    }

    public void lock() {
        this.locked = true;
    }
}
