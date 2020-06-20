package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;
import bms.player.beatoraja.skin.property.*;

import com.badlogic.gdx.graphics.Color;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.*;

/**
 * スキンオブジェクト
 * 
 * @author exch
 */
public abstract class SkinObject implements Disposable {

	/**
	 * オフセットの参照ID
	 */
	private int[] offset = new int[0];

	private boolean relative;

	/**
	 * ブレンド(2:加算, 9:反転)
	 */
	private int dstblend = 0;
    /**
     * 0 : Nearest neighbor
     * 1 : Linear filtering
     */
	private int dstfilter;
	
	private int imageType;
	
	/**
	 * 画像回転の中心
	 */
	private int dstcenter;

	/**
	 * オブジェクトクリック時に実行するイベント
	 */
	private Event clickevent = null;
	/**
	 * オブジェクトクリック判定・イベント引数の種類
	 * 0: 通常(plus only)
	 * 1: 通常(minus only)
	 * 2: 左右分割(左=minus,右=plus)
	 * 3: 上下分割(下=minus,上=plus)
	 */
	private int clickeventType = 0;
	/**
	 * 描画条件となるオプション定義
	 */
	private int[] dstop = new int[0];
	private BooleanProperty[] dstdraw = new BooleanProperty[0];
	/**
	 * 描画条件のマウス範囲
	 */
	private Rectangle mouseRect = null;
	/**
	 * 画像の伸縮方法の指定
	 */
	private StretchType stretch = StretchType.STRETCH;

	private static final float[] CENTERX = { 0.5f, 0, 0.5f, 1, 0, 0.5f, 1, 0, 0.5f, 1, };
	private static final float[] CENTERY = { 0.5f, 0, 0, 0, 0.5f, 0.5f, 0.5f, 1, 1, 1 };

	/**
	 * 回転中心のX座標(左端:0.0 - 右端:1.0)
	 */
	private float centerx;
	/**
	 * 回転中心のY座標(下端:0.0 - 上端:1.0)
	 */
	private float centery;

	private SkinAnimator skinAnimator;
	
	// 以下、高速化用
	public boolean draw;
	public Rectangle region = new Rectangle();
	public Color color = new Color();
	public float angle;
	private SkinOffset[] off = new SkinOffset[0];

	private Rectangle tmpRect = new Rectangle();
	private TextureRegion tmpImage = new TextureRegion();

	public void setAnimator(SkinAnimator animator) {
		skinAnimator = animator;
	}

	public void setStaticDestination(float x, float y, float w, float h, int a, int r, int g, int b, int angle) {
		StandardSkinAnimator animator = new StandardSkinAnimator();
		animator.setDestination(0, x, y, w, h, 0, a, r, g, b, angle, 0, null);
		this.skinAnimator = animator;
	}

	public BooleanProperty[] getDrawCondition() {
		return dstdraw;
	}

	public int[] getOption() {
		return dstop;
	}

	public void setOption(int[] dstop) {
		this.dstop = dstop;
	}

	public void setDrawCondition(int[] dstop) {
		IntSet l = new IntSet(dstop.length);
		IntArray op = new IntArray(dstop.length);
		Array<BooleanProperty> draw = new Array(dstop.length);
		for(int i : dstop) {
			if(i != 0 && !l.contains(i)) {
				BooleanProperty dc = BooleanPropertyFactory.getBooleanProperty(i);
				if(dc != null) {
					draw.add(dc);
				} else {
					op.add(i);
				}
				l.add(i);
			}
		}
		this.dstop = op.toArray();
		this.dstdraw = draw.toArray(BooleanProperty.class);
	}
	
	public void setDrawCondition(BooleanProperty[] dstdraw) {
		this.dstdraw = dstdraw;
	}

	public void setStretch(int stretch) {
		if (stretch < 0)
			return;
		for (StretchType type : StretchType.values()) {
			if (type.id == stretch) {
				this.stretch = type;
				return;
			}
		}
	}

	public void setStretch(StretchType stretch) {
		this.stretch = stretch;
	}

	public StretchType getStretch() {
		return stretch;
	}

	public int getBlend() {
		return this.dstblend;
	}

	public void setBlend(int blend) {
		dstblend = blend;
	}

	private void prepareRegion() {
		skinAnimator.getRegion(region);
		for (SkinOffset off : this.off) {
			if (off != null) {
				if (!relative) {
					region.x += off.x - off.w / 2;
					region.y += off.y - off.h / 2;
				}
				region.width += off.w;
				region.height += off.h;
			}
		}
	}

	public Rectangle getDestination(long time, MainState state) {
		return draw ? region : null;
	}

	private void prepareColor() {
		skinAnimator.getColor(color);
		for (SkinOffset off :this.off) {
			if (off != null) {
				float a = color.a + (off.a / 255.0f);
				a = a > 1 ? 1 : (a < 0 ? 0 : a);
				color.a = a;
			}
		}
	}

	public Color getColor() {
		return color;
	}

	private void prepareAngle() {
		angle = skinAnimator.getAngle();
		for (SkinOffset off :this.off) {
			if (off != null) {
				angle += off.r;
			}
		}
	}

	public boolean validate() {
		return skinAnimator != null && skinAnimator.validate();
	}

	/**
	 * リソースをあらかじめロードしておく
	 */
	public void load() {
	}
	
	public void prepare(long time, MainState state) {
		prepare(time, state, 0, 0);
	}

	public void prepare(long time, MainState state, float offsetX, float offsetY) {
		for (BooleanProperty drawCondition : dstdraw) {
			if (!drawCondition.get(state)) {
				draw = false;
				return;
			}
		}
		draw = skinAnimator.prepareTime(time, state);
		if (!draw) {
			return;
		}
		for (int i = 0; i < off.length; i++) {
			off[i] = state != null ? state.getOffsetValue(offset[i]) : null;
		}
		prepareRegion();
		region.x += offsetX;
		region.y += offsetY;
		if (mouseRect != null && !mouseRect.contains(state.main.getInputProcessor().getMouseX() -region.x,
				state.main.getInputProcessor().getMouseY() - region.y)) {
			draw = false;
			return;
		}
		prepareColor();
		prepareAngle();
	}

	public abstract void draw(SkinObjectRenderer sprite);

	protected void draw(SkinObjectRenderer sprite, TextureRegion image) {
		if (color.a == 0f || image == null) {
			return;
		}
		
		tmpRect.set(region);
		if(stretch != null) {
			stretch.stretchRect(tmpRect, tmpImage, image);
		}
		sprite.setColor(color);
		sprite.setBlend(dstblend);
		sprite.setType(dstfilter != 0 && imageType == SkinObjectRenderer.TYPE_NORMAL ? 
				(tmpRect.width == tmpImage.getRegionWidth() && tmpRect.height == tmpImage.getRegionHeight() ?
				SkinObjectRenderer.TYPE_NORMAL : SkinObjectRenderer.TYPE_BILINEAR) : imageType);
		
		if (angle != 0) {
			sprite.draw(tmpImage, tmpRect.x, tmpRect.y, tmpRect.width, tmpRect.height, centerx , centery, angle);
		} else {
			sprite.draw(tmpImage, tmpRect.x, tmpRect.y, tmpRect.width, tmpRect.height);
		}
	}

	protected void draw(SkinObjectRenderer sprite, TextureRegion image, float x, float y, float width, float height) {
		draw(sprite, image, x, y, width, height, color, angle);
	}

	protected void draw(SkinObjectRenderer sprite, TextureRegion image, float x, float y, float width, float height,
			Color color, float angle) {
		if (color == null || color.a == 0f || image == null) {
			return;
		}
		tmpRect.set(x, y, width, height);
		if(stretch != null) {
			stretch.stretchRect(tmpRect, tmpImage, image);
		}
		sprite.setColor(color);
		sprite.setBlend(dstblend);
		sprite.setType(dstfilter != 0 && imageType == SkinObjectRenderer.TYPE_NORMAL ? 
				(tmpRect.width == tmpImage.getRegionWidth() && tmpRect.height == tmpImage.getRegionHeight() ?
				SkinObjectRenderer.TYPE_NORMAL : SkinObjectRenderer.TYPE_BILINEAR) : imageType);
		
		if (angle != 0) {
			sprite.draw(tmpImage, tmpRect.x, tmpRect.y, tmpRect.width, tmpRect.height, centerx , centery, angle);
		} else {
			sprite.draw(tmpImage, tmpRect.x, tmpRect.y, tmpRect.width, tmpRect.height);
		}
	}

	protected boolean mousePressed(MainState state, int button, int x, int y) {
		if (clickevent != null) {
			final Rectangle r = region;
			// System.out.println(obj.getClickeventId() + " : " + r.x +
			// "," + r.y + "," + r.width + "," + r.height + " - " + x +
			// "," + y);
			switch (clickeventType) {
			case 0:
				if (r != null && r.x <= x && r.x + r.width >= x && r.y <= y && r.y + r.height >= y) {
					clickevent.exec(state, 1);
					return true;
				}
				break;
			case 1:
				if (r != null && r.x <= x && r.x + r.width >= x && r.y <= y && r.y + r.height >= y) {
					clickevent.exec(state, -1);
					return true;
				}
				break;
			case 2:
				if (r != null && r.x <= x && r.x + r.width >= x && r.y <= y && r.y + r.height >= y) {
					clickevent.exec(state, x >= r.x + r.width/2 ? 1 : -1);
					return true;
				}
				break;
			case 3:
				if (r != null && r.x <= x && r.x + r.width >= x && r.y <= y && r.y + r.height >= y) {
					clickevent.exec(state, y >= r.y + r.height/2 ? 1 : -1);
					return true;
				}
				break;
			}
		}
		return false;
	}

	public int getClickeventId() {
		return clickevent.getEventId();
	}

	public Event getClickevent() {
		return clickevent;
	}

	public void setClickevent(int clickevent) {
		this.clickevent = EventFactory.getEvent(clickevent);
	}

	public void setClickevent(Event clickevent) {
		this.clickevent = clickevent;
	}

	public int getClickeventType() {
		return clickeventType;
	}

	public void setClickeventType(int clickeventType) {
		this.clickeventType = clickeventType;
	}

	public boolean isRelative() {
		return relative;
	}

	public void setRelative(boolean relative) {
		this.relative = relative;
	}
	
	/**
	 * オフセット
	 * 
	 * @author exch
	 */
	public static class SkinOffset {
		public float x;
		public float y;
		public float w;
		public float h;
		public float r;
		public float a;
	}

	/**
	 * IntegerPropertyからmin - max間の比率を表現するためのProperty
	 *
	 * @author exch
	 */
	public static class RateProperty implements FloatProperty {
		
		private final IntegerProperty ref;
		private final int min;
		private final int max;
		
		public RateProperty(int type, int min, int max) {
			this.ref = IntegerPropertyFactory.getIntegerProperty(type);
			this.min = min;
			this.max = max;
		}
		
		public float get(MainState state) {
			final int value = ref != null ? ref.get(state) : 0;
			if(min < max) {
				if(value > max) {
					return 1;
				} else if(value < min) {
					return 0;
				} else {
					return Math.abs( ((float) value - min) / (max - min) );
				}
			} else {
				if(value < max) {
					return 1;
				} else if(value > min) {
					return 0;
				} else {
					return Math.abs( ((float) value - min) / (max - min) );
				}
			}
		}
	}

	public abstract void dispose();

	public int[] getOffsetID() {
		return offset;
	}

	public void setOffsetID(int offset) {
		setOffsetID(new int[]{offset});
	}

	public void setOffsetID(int[] offset) {
		if(this.offset.length > 0) {
			return;
		}
		IntSet a = new IntSet(offset.length);
		for(int o : offset) {
			if(o > 0 && o < SkinProperty.OFFSET_MAX + 1) {
				a.add(o);
			}
		}
		if(a.size > 0) {
			this.offset = a.iterator().toArray().toArray();
			this.off = new SkinOffset[this.offset.length];
		}
	}
	
	public SkinOffset[] getOffsets() {
		return off;
	}
	
	public static void disposeAll(Disposable[] obj) {
		for(int i = 0;i < obj.length;i++) {
			if(obj[i] != null) {
				obj[i].dispose();
				obj[i] = null;
			}
		}
	}

	public int getImageType() {
		return imageType;
	}

	public void setImageType(int imageType) {
		this.imageType = imageType;
	}
	
	public int getFilter() {
		return dstfilter;
	}

	public void setFilter(int filter) {
		dstfilter = filter;
	}

	public void setCenter(int center) {
		dstcenter = center;
		centerx = CENTERX[center];
		centery = CENTERY[center];
	}

	public void setMouseRect(float x2, float y2, float w2, float h2) {
		this.mouseRect = new Rectangle(x2, y2, w2, h2);
	}

	// FIXME: LR2スキン用の固有実装
	public float getLastPositionY() {
		if (skinAnimator instanceof StandardSkinAnimator) {
			return ((StandardSkinAnimator) skinAnimator).getLastPositionY();
		} else {
			return 0;
		}
	}
}
