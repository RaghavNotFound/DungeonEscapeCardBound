package io.github.pkgde.quiz;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;

/**
 * Centralized UI styles for the quiz system.
 * Provides consistent styling for both boss and door quiz contexts.
 */
public class QuizUIStyles implements Disposable {

    // Textures (owned — must be disposed)
    private final Texture dialogBgTex;
    private final Texture optionUpTex;
    private final Texture optionDownTex;
    private final Texture optionCheckedTex;
    private final Texture submitUpTex;
    private final Texture submitDownTex;
    private final Texture submitDisabledTex;

    // Public styles
    public final TextureRegionDrawable dialogBackground;
    public final TextButton.TextButtonStyle optionStyle;
    public final TextButton.TextButtonStyle submitStyle;
    public final Label.LabelStyle questionStyle;
    public final Label.LabelStyle instructionStyle;

    public QuizUIStyles(BitmapFont font) {
        // Dialog background — dark semi-transparent panel
        dialogBgTex = createColorTexture(new Color(0.06f, 0.06f, 0.10f, 0.92f));
        dialogBackground = new TextureRegionDrawable(dialogBgTex);

        // Option button textures
        optionUpTex = createRoundedTexture(64, 10, new Color(0.15f, 0.16f, 0.25f, 1f),
            new Color(0.35f, 0.38f, 0.55f, 1f));
        optionDownTex = createRoundedTexture(64, 10, new Color(0.10f, 0.10f, 0.18f, 1f),
            new Color(0.25f, 0.28f, 0.45f, 1f));
        optionCheckedTex = createRoundedTexture(64, 10, new Color(0.12f, 0.35f, 0.18f, 1f),
            new Color(0.20f, 0.70f, 0.30f, 1f));

        // Submit button textures
        submitUpTex = createRoundedTexture(48, 8, new Color(0.14f, 0.45f, 0.22f, 1f),
            new Color(0.22f, 0.72f, 0.35f, 1f));
        submitDownTex = createRoundedTexture(48, 8, new Color(0.10f, 0.35f, 0.16f, 1f),
            new Color(0.18f, 0.60f, 0.28f, 1f));
        submitDisabledTex = createRoundedTexture(48, 8, new Color(0.20f, 0.20f, 0.22f, 0.6f),
            new Color(0.30f, 0.30f, 0.32f, 0.6f));

        // Option style
        optionStyle = new TextButton.TextButtonStyle();
        optionStyle.font = font;
        optionStyle.up = new TextureRegionDrawable(optionUpTex);
        optionStyle.down = new TextureRegionDrawable(optionDownTex);
        optionStyle.checked = new TextureRegionDrawable(optionCheckedTex);
        optionStyle.fontColor = new Color(0.85f, 0.88f, 0.95f, 1f);
        optionStyle.checkedFontColor = new Color(0.90f, 1f, 0.92f, 1f);

        // Submit style
        submitStyle = new TextButton.TextButtonStyle();
        submitStyle.font = font;
        submitStyle.up = new TextureRegionDrawable(submitUpTex);
        submitStyle.down = new TextureRegionDrawable(submitDownTex);
        submitStyle.disabled = new TextureRegionDrawable(submitDisabledTex);
        submitStyle.fontColor = Color.WHITE;
        submitStyle.disabledFontColor = new Color(0.50f, 0.50f, 0.50f, 0.7f);

        // Question label style — prominent, gold
        questionStyle = new Label.LabelStyle();
        questionStyle.font = font;
        questionStyle.fontColor = new Color(1f, 0.85f, 0.30f, 1f);

        // Instruction label style — subtle
        instructionStyle = new Label.LabelStyle();
        instructionStyle.font = font;
        instructionStyle.fontColor = new Color(0.65f, 0.68f, 0.75f, 1f);
    }

    /**
     * Creates a simple 1x1 solid color texture.
     */
    private Texture createColorTexture(Color color) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    /**
     * Creates a small rounded-feel texture with fill and border colors.
     * Uses a pixmap with a subtle border for a polished look.
     */
    private Texture createRoundedTexture(int size, int borderThickness, Color fill, Color border) {
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);

        // Fill entire with border color
        pixmap.setColor(border);
        pixmap.fill();

        // Inner fill
        pixmap.setColor(fill);
        pixmap.fillRectangle(borderThickness, borderThickness,
            size - borderThickness * 2, size - borderThickness * 2);

        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    @Override
    public void dispose() {
        dialogBgTex.dispose();
        optionUpTex.dispose();
        optionDownTex.dispose();
        optionCheckedTex.dispose();
        submitUpTex.dispose();
        submitDownTex.dispose();
        submitDisabledTex.dispose();
    }
}
