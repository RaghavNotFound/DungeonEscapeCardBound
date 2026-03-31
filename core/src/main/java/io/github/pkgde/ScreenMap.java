package io.github.pkgde;

import com.badlogic.gdx.math.Rectangle;
import java.util.ArrayList;

public class ScreenMap
{

    public float width;
    public float height;

    public ArrayList<Rectangle> obstacles=new ArrayList<>();

    public ScreenMap(float width,float height)
    {
        this.width=width;
        this.height=height;
    }
}
