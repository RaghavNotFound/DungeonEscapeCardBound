package io.github.pkgde;

import com.badlogic.gdx.utils.Array;

/**
 * A Data Transfer Object (DTO) that represents a snapshot of the game state.
 * This class is designed for easy serialization to JSON.
 */
public class SaveState {
    public PlayerState player = new PlayerState();
    public Array<EnemyState> enemies = new Array<>();
    public Array<InteractableState> interactables = new Array<>();
    public boolean isCenterFireLit = false;
    public String currentMapPath = "Maps/tutorial.ldtk";
    public int currentLevelIndex = 0;
    public boolean isLevelTransition = false;

    public static class PlayerState {
        public float x, y, health, stamina, timeSurvived;
        public int torches, cards, enemiesKilled;
    }

    public static class EnemyState {
        public float x, y, health;
    }

    public static class InteractableState {
        public float x, y;
        public String type;
        public boolean interacted;
    }
}
