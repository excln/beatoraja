package bms.player.beatoraja.skin.property;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;

public interface AnimationProperty {
    public boolean update(long time);
    public void getRegion(Rectangle region);
    public void getColor(Color color);
    public float getAngle();
}
