package io.github.pkgde.quiz;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;

public class QuizUIStyles implements Disposable {

    private final Texture btnUp;
    private final Texture btnDown;
    private final Texture btnChecked;
    private final Texture btnDisabled;
    
    public final TextButton.TextButtonStyle optionStyle;
    public final TextButton.TextButtonStyle submitStyle;
    public final Label.LabelStyle labelStyle;

    public QuizUIStyles(BitmapFont font) {
        btnUp = createColorTexture(new Color(0.2f, 0.2f, 0.3f, 1f));
        btnDown = createColorTexture(new Color(0.1f, 0.1f, 0.2f, 1f));
        btnChecked = createColorTexture(new Color(0.2f, 0.6f, 0.2f, 1f));
        btnDisabled = createColorTexture(new Color(0.3f, 0.3f, 0.3f, 0.5f));

        optionStyle = new TextButton.TextButtonStyle();
        optionStyle.font = font;
        optionStyle.up = new TextureRegionDrawable(btnUp);
        optionStyle.down = new TextureRegionDrawable(btnDown);
        optionStyle.checked = new TextureRegionDrawable(btnChecked);
        optionStyle.fontColor = Color.WHITE;

        submitStyle = new TextButton.TextButtonStyle();
        submitStyle.font = font;
        submitStyle.up = new TextureRegionDrawable(btnUp);
        submitStyle.down = new TextureRegionDrawable(btnDown);
        submitStyle.disabled = new TextureRegionDrawable(btnDisabled);
        submitStyle.fontColor = Color.WHITE;

        labelStyle = new Label.LabelStyle();
        labelStyle.font = font;
        labelStyle.fontColor = Color.YELLOW;
    }

    private Texture createColorTexture(Color color) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    @Override
    public void dispose() {
        btnUp.dispose();
        btnDown.dispose();
        btnChecked.dispose();
        btnDisabled.dispose();
    }
}
