package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.property.TimerProperty;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

public class SkinAnimator {

    /**
     * 参照するタイマー定義
     */
    private TimerProperty dsttimer;

    /**
     * ループ開始タイマー
     */
    private int dstloop = 0;

    private int acc;

    /**
     * 描画先
     */
    private SkinObjectDestination[] dst = new SkinObjectDestination[0];

    // 以下、高速化用
    private long starttime;
    private long endtime;

    private Rectangle fixr = null;
    private Color fixc = null;
    private int fixa = Integer.MIN_VALUE;

    private long nowtime = 0;
    private float rate = 0;
    private int index = 0;

    public void setDestination(long time, float x, float y, float w, float h, int acc, int a, int r, int g, int b,
                               int angle, int loop, TimerProperty timer) {
        SkinObjectDestination obj = new SkinObjectDestination(time, new Rectangle(x, y, w, h),
                new Color(r / 255.0f, g / 255.0f, b / 255.0f, a / 255.0f), angle);
        if (dst.length == 0) {
            fixr = obj.region;
            fixc = obj.color;
            fixa = obj.angle;
        } else {
            if (!obj.region.equals(fixr)) {
                fixr = null;
            }
            if (!obj.color.equals(fixc)) {
                fixc = null;
            }
            if (!(fixa == obj.angle)) {
                fixa = Integer.MIN_VALUE;
            }
        }
        if (this.acc == 0) {
            this.acc = acc;
        }

        if (dsttimer == null) {
            dsttimer = timer;
        }
        if (dstloop == 0) {
            dstloop = loop;
        }
        for (int i = 0; i < dst.length; i++) {
            if (dst[i].time > time) {
                Array<SkinObjectDestination> l = new Array<>(dst);
                l.insert(i, obj);
                dst = l.toArray(SkinObjectDestination.class);
                starttime = dst[0].time;
                endtime = dst[dst.length - 1].time;
                return;
            }
        }
        Array<SkinObjectDestination> l = new Array<>(dst);
        l.add(obj);
        dst = l.toArray(SkinObjectDestination.class);
        starttime = dst[0].time;
        endtime = dst[dst.length - 1].time;
    }

    /**
     * 時刻を更新してアニメーションの準備を行う
     * @param time
     * @param state
     * @return 描画範囲内の時刻かどうか
     */
    public boolean prepareTime(long time, MainState state) {
        final TimerProperty timer = dsttimer;

        if (timer != null) {
            if (timer.isOff(state)) {
                return false;
            }
            time -= timer.get(state);
        }

        final long lasttime = endtime;
        if (dstloop == -1) {
            if(time > endtime) {
                time = -1;
            }
        } else if (lasttime > 0 && time > dstloop) {
            if (lasttime == dstloop) {
                time = dstloop;
            } else {
                time = (time - dstloop) % (lasttime - dstloop) + dstloop;
            }
        }
        if (starttime > time) {
            return false;
        }
        nowtime = time;
        if (fixr == null || fixc == null || fixa == Integer.MIN_VALUE) {
            updateRate();
        }
        return true;
    }

    private void updateRate() {
        long time2 = dst[dst.length - 1].time;
        if (nowtime == time2) {
            this.rate = 0;
            this.index = dst.length - 1;
            return;
        }
        for (int i = dst.length - 2; i >= 0; i--) {
            final long time1 = dst[i].time;
            if (time1 <= nowtime && time2 > nowtime) {
                float rate = (float) (nowtime - time1) / (time2 - time1);
                switch(acc) {
                case 1:
                    rate = rate * rate;
                    break;
                case 2:
                    rate = 1 - (rate - 1) * (rate - 1);
                    break;
                }
                this.rate = rate;
                this.index = i;
                return;
            }
            time2 = time1;
        }
        this.rate = 0;
        this.index = 0;
    }

    /**
     * 指定して時間に応じた描画領域を返す
     */
    public void getRegion(Rectangle region) {
        if (fixr == null) {
            if (rate == 0) {
                region.set(dst[index].region);
            } else {
                if (acc == 3) {
                    final Rectangle r1 = dst[index].region;
                    region.x = r1.x;
                    region.y = r1.y;
                    region.width = r1.width;
                    region.height = r1.height;
                } else {
                    final Rectangle r1 = dst[index].region;
                    final Rectangle r2 = dst[index + 1].region;
                    region.x = r1.x + (r2.x - r1.x) * rate;
                    region.y = r1.y + (r2.y - r1.y) * rate;
                    region.width = r1.width + (r2.width - r1.width) * rate;
                    region.height = r1.height + (r2.height - r1.height) * rate;
                }
            }
        } else {
            region.set(fixr);
        }
    }

    public void getColor(Color color) {
        if (fixc != null) {
            color.set(fixc);
            return;
        }
        if (rate == 0) {
            color.set(dst[index].color);
        } else {
            if (acc == 3) {
                final Color r1 = dst[index].color;
                color.r = r1.r;
                color.g = r1.g;
                color.b = r1.b;
                color.a = r1.a;
            } else {
                final Color r1 = dst[index].color;
                final Color r2 = dst[index + 1].color;
                color.r = r1.r + (r2.r - r1.r) * rate;
                color.g = r1.g + (r2.g - r1.g) * rate;
                color.b = r1.b + (r2.b - r1.b) * rate;
                color.a = r1.a + (r2.a - r1.a) * rate;
            }
        }
    }

    public int getAngle() {
        if (fixa != Integer.MIN_VALUE) {
            return fixa;
        }
        return (rate == 0 || acc == 3 ? dst[index].angle :  (int) (dst[index].angle + (dst[index + 1].angle - dst[index].angle) * rate));
    }

    public boolean validate() {
        return dst.length > 0;
    }

    // FIXME: LR2スキン用の固有実装
    public float getLastPositionY() {
        return dst[dst.length - 1].region.y;
    }

    /**
     * スキンオブジェクトの描画先を表現するクラス
     *
     * @author exch
     */
    public static class SkinObjectDestination {

        public final long time;
        /**
         * 描画領域
         */
        public final Rectangle region;
        public final Color color;
        public final int angle;

        public SkinObjectDestination(long time, Rectangle region, Color color, int angle) {
            this.time = time;
            this.region = region;
            this.color = color;
            this.angle = angle;
        }
    }
}
