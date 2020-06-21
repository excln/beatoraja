package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.property.AnimationProperty;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;

public class ScriptableSkinAnimator implements SkinAnimator {
    private final AnimationProperty animation;
    private final float dx;
    private final float dy;

    public ScriptableSkinAnimator(Skin skin, AnimationProperty animationProperty) {
        animation = animationProperty;
        dx = skin.getScaleX();
        dy = skin.getScaleY();
    }

    public boolean validate() {
        return animation != null;
    }

    public boolean prepareTime(long time, MainState state) {
        return animation.update(time);
    }

    public void getRegion(Rectangle region) {
        animation.getRegion(region);
        region.x *= dx;
        region.y *= dy;
        region.width *= dx;
        region.height *= dy;
    }

    public void getColor(Color color) {
        animation.getColor(color);
    }

    public float getAngle() {
        return animation.getAngle();
    }
}
