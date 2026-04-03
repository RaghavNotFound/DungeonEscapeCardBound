package io.github.pkgde;

import com.badlogic.gdx.math.MathUtils;

public class GameTimeManager {

    private float timeScale = 1f;
    private float targetTimeScale = 1f;
    private final float transitionSpeed = 2.5f;

    public void setSlowMotion(boolean enabled, float scale) {
        this.targetTimeScale = enabled ? scale : 1f;
    }

    public float getDelta(float rawDelta) {
        timeScale = MathUtils.lerp(timeScale, targetTimeScale, rawDelta * transitionSpeed);
        return rawDelta * timeScale;
    }
}