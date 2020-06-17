package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;

public interface SkinAnimator {
    public boolean validate();
    public boolean prepareTime(long time, MainState state);
    public void getRegion(Rectangle region);
    public void getColor(Color color);
    public float getAngle();
}
