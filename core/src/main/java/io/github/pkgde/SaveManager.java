package io.github.pkgde;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.badlogic.gdx.math.Vector2;
import java.util.Comparator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.io.IOException;

public class SaveManager {

    private static final String SAVE_DIR = "saves/";
    private static final String AUTO_SAVE_SLOT_NAME = "auto_save";

    // FIXED: Reads the file safely without loading the whole world so we know what map to load!
    public static SaveState peekSave(String slotName) {
        FileHandle file = Gdx.files.local(SAVE_DIR + slotName + ".json");
        if (!file.exists()) return null;
        return new Json().fromJson(SaveState.class, file);
    }

    public static void saveGame(GameWorld world, String slotName) {
        saveGameWithLocation(world, slotName, world.getMapManager().getCurrentMapPath(), world.getMapManager().getCurrentLevelIndex());
    }

    public static void saveGameWithLocation(GameWorld world, String slotName, String mapPath, int levelIndex) {
        SaveState state = new SaveState();

        state.currentMapPath = mapPath;
        state.currentLevelIndex = levelIndex;

        Player p = world.getPlayer();
        state.player.x = p.getPosition().x;
        state.player.y = p.getPosition().y;
        state.player.health = p.getHealth();
        state.player.stamina = p.getStamina();
        state.player.torches = p.getTorchCount();
        state.player.cards = p.getCardsCount();
        state.player.enemiesKilled = p.getEnemiesKilled();
        state.player.timeSurvived = p.getTimeSurvived();

        for (Enemy e : world.getEnemies()) {
            if (!e.isAlive()) continue;
            SaveState.EnemyState es = new SaveState.EnemyState();
            es.x = e.getPosition().x;
            es.y = e.getPosition().y;
            es.health = e.getHealth();
            state.enemies.add(es);
        }

        for (Interactable i : world.getInteractables()) {
            SaveState.InteractableState is = new SaveState.InteractableState();
            is.x = i.getBounds().x;
            is.y = i.getBounds().y;
            is.type = i.getType().name();
            is.interacted = i.isInteracted();
            state.interactables.add(is);
        }

        if (world.getLightingManager() != null) {
            state.isCenterFireLit = world.getLightingManager().isLit();
        }

        FileHandle file = Gdx.files.local(SAVE_DIR + slotName + ".json");
        Json json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        file.writeString(json.prettyPrint(state), false);
    }

    public static void saveLevelTransition(GameWorld world, String slotName, String mapPath, int levelIndex) {
        SaveState state = new SaveState();

        state.currentMapPath = mapPath;
        state.currentLevelIndex = levelIndex;
        state.isLevelTransition = true;

        Player p = world.getPlayer();
        state.player.x = p.getPosition().x;
        state.player.y = p.getPosition().y;
        state.player.health = p.getHealth();
        state.player.stamina = p.getStamina();
        state.player.torches = p.getTorchCount();
        state.player.cards = p.getCardsCount();
        state.player.enemiesKilled = p.getEnemiesKilled();
        state.player.timeSurvived = p.getTimeSurvived();

        for (Enemy e : world.getEnemies()) {
            if (!e.isAlive()) continue;
            SaveState.EnemyState es = new SaveState.EnemyState();
            es.x = e.getPosition().x;
            es.y = e.getPosition().y;
            es.health = e.getHealth();
            state.enemies.add(es);
        }

        for (Interactable i : world.getInteractables()) {
            SaveState.InteractableState is = new SaveState.InteractableState();
            is.x = i.getBounds().x;
            is.y = i.getBounds().y;
            is.type = i.getType().name();
            is.interacted = i.isInteracted();
            state.interactables.add(is);
        }

        if (world.getLightingManager() != null) {
            state.isCenterFireLit = world.getLightingManager().isLit();
        }

        FileHandle file = Gdx.files.local(SAVE_DIR + slotName + ".json");
        Json json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        file.writeString(json.prettyPrint(state), false);
    }

    public static void autoSaveGame(GameWorld world) {
        saveGame(world, AUTO_SAVE_SLOT_NAME);
    }

    public static void loadGame(GameWorld world, String slotName) {
        FileHandle file = Gdx.files.local(SAVE_DIR + slotName + ".json");
        if (!file.exists()) return;

        Json json = new Json();
        SaveState state = json.fromJson(SaveState.class, file);

        Player p = world.getPlayer();

        if (!state.isLevelTransition) {
            p.setPosition(state.player.x, state.player.y);
        } else {
            // FIX: Force the player to the actual map spawn point for the new room!
            Vector2 spawn = world.getMapManager().getPlayerSpawn();
            p.setPosition(spawn.x, spawn.y);
        }

        p.setHealth(state.player.health);
        p.setStamina(state.player.stamina);
        p.setTorchCount(state.player.torches);
        p.setCardsCount(state.player.cards);
        p.setEnemiesKilled(state.player.enemiesKilled);
        p.setTimeSurvived(state.player.timeSurvived);

        world.getEnemies().clear();
        for (SaveState.EnemyState es : state.enemies) {
            Enemy e = new Enemy();
            e.setPosition(es.x, es.y);
            e.setHealth(es.health);
            e.setBoundaries(world.getMapManager().getCollisionRects());
            e.setWorldBounds(0, 0, world.getMapManager().getMapWidth(), world.getMapManager().getMapHeight());
            world.getEnemies().add(e);
        }

        for (SaveState.InteractableState is : state.interactables) {
            for (Interactable i : world.getInteractables()) {
                if (Math.abs(i.getBounds().x - is.x) < 1f && Math.abs(i.getBounds().y - is.y) < 1f) {
                    i.setInteracted(is.interacted);
                    break;
                }
            }
        }

        if (state.isCenterFireLit && world.getLightingManager() != null) {
            for (Interactable i : world.getInteractables()) {
                if (i.getType() == Interactable.Type.CENTER_FIRE) {
                    world.getLightingManager().triggerLighting(i.getBounds().x + i.getBounds().width / 2f, i.getBounds().y + i.getBounds().height / 2f);
                    break;
                }
            }
        }
    }

    public static Array<String> getAllSaves() {
        Array<String> saves = new Array<>();
        FileHandle autoSaveFile = null;
        Array<FileHandle> otherSaves = new Array<>();

        FileHandle dir = Gdx.files.local(SAVE_DIR);
        if (dir.exists()) {
            for (FileHandle file : dir.list(".json")) {
                if (file.nameWithoutExtension().equals(AUTO_SAVE_SLOT_NAME)) {
                    autoSaveFile = file;
                } else {
                    otherSaves.add(file);
                }
            }
        }

        otherSaves.sort(Comparator.comparingLong(FileHandle::lastModified).reversed());

        if (autoSaveFile != null) {
            saves.add(autoSaveFile.nameWithoutExtension());
        }

        for (FileHandle file : otherSaves) {
            saves.add(file.nameWithoutExtension());
        }

        return saves;
    }

    public static String getLatestSave() {
        FileHandle dir = Gdx.files.local(SAVE_DIR);
        if (!dir.exists() || dir.list(".json").length == 0) return null;

        FileHandle latest = null;
        for (FileHandle file : dir.list(".json")) {
            if (latest == null || file.lastModified() > latest.lastModified()) {
                latest = file;
            }
        }
        return latest != null ? latest.nameWithoutExtension() : null;
    }

    public static void deleteSave(String slotName) {
        FileHandle file = Gdx.files.local(SAVE_DIR + slotName + ".json");
        if (file.exists()) {
            file.delete();
        }
    }

    public static void renameSave(String oldName, String newName) {
        if (oldName.equals(AUTO_SAVE_SLOT_NAME)) {
            Gdx.app.log("SaveManager", "Attempted to rename auto-save slot, operation blocked.");
            return;
        }

        FileHandle oldFile = Gdx.files.local(SAVE_DIR + oldName + ".json");
        FileHandle newFile = Gdx.files.local(SAVE_DIR + newName + ".json");

        if (oldFile.exists()) {
            try {
                Path source = Paths.get(oldFile.file().getAbsolutePath());
                Path target = Paths.get(newFile.file().getAbsolutePath());
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                Gdx.app.log("SaveManager", "Successfully renamed " + oldName + " to " + newName);
            } catch (IOException e) {
                Gdx.app.error("SaveManager", "Critical failure renaming file: " + e.getMessage());
            }
        }
    }

    public static String generateNewSaveName() {
        int index = 1;
        while (true) {
            String name = "Save " + index;
            if (!Gdx.files.local(SAVE_DIR + name + ".json").exists()) return name;
            index++;
        }
    }
}
