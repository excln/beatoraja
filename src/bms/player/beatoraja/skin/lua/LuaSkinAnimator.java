package bms.player.beatoraja.skin.lua;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.SkinAnimator;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

public class LuaSkinAnimator implements SkinAnimator {
    private LuaTable table;
    private LuaFunction updater;

    public LuaSkinAnimator(LuaTable table) {
        this.table = table;
        this.updater = table.get("update").checkfunction();
    }

    public boolean validate() {
        return updater != null && table != null;
    }

    public boolean prepareTime(long time, MainState state) {
        LuaValue result = updater.call(table, LuaValue.valueOf(time));
        return result.toboolean();
    }

    public void getRegion(Rectangle region) {
        region.x = table.get("x").tofloat();
        region.y = table.get("y").tofloat();
        region.width = table.get("w").tofloat();
        region.height = table.get("h").tofloat();
    }

    public void getColor(Color color) {
        color.r = table.get("r").tofloat();
        color.g = table.get("g").tofloat();
        color.b = table.get("b").tofloat();
        color.a = table.get("a").tofloat();
    }

    public float getAngle() {
        return table.get("angle").tofloat();
    }
}
