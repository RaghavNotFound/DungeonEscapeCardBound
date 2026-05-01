package io.github.pkgde;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.ArrayList;
import java.util.List;

/**
 * A reusable classic RPG-style dialogue box overlay.
 * <p>
 * Supports:
 * <ul>
 *   <li>Typewriter text reveal effect</li>
 *   <li>Speaker name display</li>
 *   <li>Multiple-choice branching</li>
 *   <li>Keyboard (SPACE / ENTER / W / S) and mouse input</li>
 * </ul>
 * <p>
 * Usage:
 * <pre>
 *   DialogueOverlay dlg = new DialogueOverlay();
 *   dlg.start(nodes, () -> { /* dialogue finished *{@literal /} });
 *   // in render loop:
 *   dlg.update(delta);
 *   dlg.render(batch, font, viewport);
 * </pre>
 */
public class DialogueOverlay {

    // ── Inner data classes ───────────────────────────────────────────────

    /** A single choice the player can pick. */
    public static class Choice {
        public final String text;
        public final int nextNodeIndex; // -1 = advance to next node normally

        public Choice(String text, int nextNodeIndex) {
            this.text = text;
            this.nextNodeIndex = nextNodeIndex;
        }
    }

    /** One "page" of dialogue. */
    public static class DialogueNode {
        public final String speaker;
        public final String text;
        public final List<Choice> choices; // null or empty = no choices (simple advance)
        public final Color speakerColor;

        public DialogueNode(String speaker, String text, Color speakerColor) {
            this(speaker, text, speakerColor, null);
        }

        public DialogueNode(String speaker, String text, Color speakerColor, List<Choice> choices) {
            this.speaker = speaker;
            this.text = text;
            this.speakerColor = speakerColor != null ? speakerColor : Color.WHITE;
            this.choices = choices;
        }

        public boolean hasChoices() {
            return choices != null && !choices.isEmpty();
        }
    }

    // ── Constants ────────────────────────────────────────────────────────

    /** Characters revealed per second during typewriter effect. */
    private static final float CHARS_PER_SECOND = 35f;

    /** Speed multiplier when SPACE is held. */
    private static final float FAST_FORWARD_MULT = 4f;

    // ── State ────────────────────────────────────────────────────────────

    private Texture boxTexture;
    private List<DialogueNode> nodes;
    private int currentNodeIndex;
    private float charProgress;       // fractional number of characters revealed
    private boolean textFullyRevealed;
    private boolean active;
    private Runnable onFinished;

    private int selectedChoice = 0;
    private float animTime = 0f;

    // Input helpers
    private final Vector3 touch = new Vector3();
    private final GlyphLayout glyphLayout = new GlyphLayout();

    // Prevent the first frame from eating a stale SPACE press
    private boolean inputLockedFirstFrame = true;

    // ── Public API ───────────────────────────────────────────────────────

    public DialogueOverlay() {
        try {
            boxTexture = new Texture(Gdx.files.internal("DialogueBox/DialogueBox.png"));
            boxTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        } catch (Exception e) {
            Gdx.app.error("DialogueOverlay", "Could not load DialogueBox texture", e);
        }
    }

    /**
     * Start a new dialogue sequence.
     *
     * @param nodes      ordered list of dialogue nodes
     * @param onFinished callback invoked when the last node is dismissed
     */
    public void start(List<DialogueNode> nodes, Runnable onFinished) {
        this.nodes = nodes;
        this.onFinished = onFinished;
        this.currentNodeIndex = 0;
        this.charProgress = 0f;
        this.textFullyRevealed = false;
        this.selectedChoice = 0;
        this.active = true;
        this.inputLockedFirstFrame = true;
    }

    /** @return true while a dialogue sequence is being shown. */
    public boolean isActive() {
        return active;
    }

    /** Advance simulation (call every frame). */
    public void update(float delta) {
        if (!active) return;

        animTime += delta;

        DialogueNode node = currentNode();
        if (node == null) { finish(); return; }

        // Typewriter progression
        if (!textFullyRevealed) {
            float speed = CHARS_PER_SECOND;
            if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
                speed *= FAST_FORWARD_MULT;
            }
            charProgress += speed * delta;
            if ((int) charProgress >= node.text.length()) {
                charProgress = node.text.length();
                textFullyRevealed = true;
            }
        }

        // Consume the "locked" flag after first frame so a held key from previous screen doesn't fire
        if (inputLockedFirstFrame) {
            inputLockedFirstFrame = false;
            return;
        }

        // ── Input handling ───────────────────────────────────────────────
        if (!textFullyRevealed) {
            // Click or ENTER to instantly reveal
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.justTouched()) {
                charProgress = node.text.length();
                textFullyRevealed = true;
            }
        } else {
            // Text fully shown
            if (node.hasChoices()) {
                // Navigate choices
                if (Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
                    selectedChoice = (selectedChoice + node.choices.size() - 1) % node.choices.size();
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.S) || Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
                    selectedChoice = (selectedChoice + 1) % node.choices.size();
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                    advanceWithChoice(node.choices.get(selectedChoice));
                }
            } else {
                // Simple advance on press
                if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                    || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
                    || Gdx.input.justTouched()) {
                    advanceToNode(currentNodeIndex + 1);
                }
            }
        }
    }

    /**
     * Render the dialogue box.
     * Call between {@code batch.begin()} / {@code batch.end()} is NOT required —
     * this method manages its own begin/end calls.
     */
    public void render(SpriteBatch batch, BitmapFont font, Viewport viewport) {
        if (!active) return;
        DialogueNode node = currentNode();
        if (node == null) return;

        float worldW = viewport.getWorldWidth();
        float worldH = viewport.getWorldHeight();

        // ── Box dimensions & position ────────────────────────────────────
        float boxW = worldW * 0.85f;
        float boxH = worldH * 0.25f;
        float boxX = (worldW - boxW) * 0.5f;
        float boxY = worldH * 0.03f;

        // Padding inside the box for text
        // The texture has a built-in name plate at the top (~30% of box height)
        // and decorative borders on the sides (~7%) and bottom (~15%)
        float padX = boxW * 0.07f;
        float padBottom = boxH * 0.18f;

        batch.setProjectionMatrix(viewport.getCamera().combined);

        // ── Draw dialogue box texture ────────────────────────────────────
        batch.begin();
        if (boxTexture != null) {
            batch.draw(boxTexture, boxX, boxY, boxW, boxH);
        }

        float oldSX = font.getData().scaleX;
        float oldSY = font.getData().scaleY;

        // ── Body text (typewriter) ───────────────────────────────────────
        String visibleText = node.text.substring(0, Math.min((int) charProgress, node.text.length()));
        float bodyScale = worldW / 1300f;
        font.getData().setScale(bodyScale * 1.1f);

        float textX = boxX + padX;
        // Place text in the dark body area: starts at ~75% up from the box bottom
        float textY = boxY + boxH * 0.75f;
        float wrapWidth = boxW - padX * 2f;

        // Shadow
        font.setColor(0f, 0f, 0f, 0.7f);
        glyphLayout.setText(font, visibleText, Color.BLACK, wrapWidth, -1, true);
        font.draw(batch, glyphLayout, textX + 1.5f, textY - 1.5f);

        // Main text
        font.setColor(Color.WHITE);
        glyphLayout.setText(font, visibleText, Color.WHITE, wrapWidth, -1, true);
        font.draw(batch, glyphLayout, textX, textY);

        // ── Advance indicator (blinking triangle) ────────────────────────
        if (textFullyRevealed && !node.hasChoices()) {
            float blinkAlpha = 0.4f + 0.6f * (0.5f + 0.5f * MathUtils.sin(animTime * 5f));
            font.setColor(1f, 1f, 1f, blinkAlpha);
            font.getData().setScale(bodyScale * 1.3f);
            glyphLayout.setText(font, "▶");
            float indX = boxX + boxW - padX - glyphLayout.width;
            float indY = boxY + padBottom + glyphLayout.height;
            font.draw(batch, glyphLayout, indX, indY);
        }

        // ── Choices ──────────────────────────────────────────────────────
        if (textFullyRevealed && node.hasChoices()) {
            float choiceScale = worldW / 1300f;
            font.getData().setScale(choiceScale * 1.05f);
            float choiceX = boxX + padX + boxW * 0.02f;
            float choiceY = textY - glyphLayout.height - boxH * 0.12f;
            float lineH = worldH * 0.035f;

            for (int i = 0; i < node.choices.size(); i++) {
                Choice c = node.choices.get(i);
                String prefix;
                Color color;
                if (i == selectedChoice) {
                    float pulse = 0.5f + 0.5f * MathUtils.sin(animTime * 6f);
                    prefix = "▸ ";
                    color = new Color(0.25f + 0.1f * pulse, 0.85f, 1f, 1f);
                } else {
                    prefix = "  ";
                    color = new Color(0.75f, 0.75f, 0.75f, 1f);
                }

                font.setColor(color);
                glyphLayout.setText(font, prefix + c.text);
                font.draw(batch, glyphLayout, choiceX, choiceY - i * lineH);
            }
        }

        // ── Restore font state ───────────────────────────────────────────
        font.setColor(Color.WHITE);
        font.getData().setScale(oldSX, oldSY);
        batch.end();
    }

    /** Dispose the dialogue box texture. Call on screen dispose. */
    public void dispose() {
        if (boxTexture != null) {
            boxTexture.dispose();
            boxTexture = null;
        }
    }

    // ── Internals ────────────────────────────────────────────────────────

    private DialogueNode currentNode() {
        if (nodes == null || currentNodeIndex < 0 || currentNodeIndex >= nodes.size()) return null;
        return nodes.get(currentNodeIndex);
    }

    private void advanceWithChoice(Choice choice) {
        int next = choice.nextNodeIndex;
        if (next < 0) {
            next = currentNodeIndex + 1;
        }
        advanceToNode(next);
    }

    private void advanceToNode(int index) {
        if (index < 0 || index >= nodes.size()) {
            finish();
            return;
        }
        currentNodeIndex = index;
        charProgress = 0f;
        textFullyRevealed = false;
        selectedChoice = 0;
    }

    private void finish() {
        active = false;
        if (onFinished != null) {
            onFinished.run();
        }
    }
}
