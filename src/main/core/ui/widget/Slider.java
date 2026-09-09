package core.ui.widget;

import core.g2d.Atlas;
import core.g2d.Fill;
import core.g2d.Font;
import core.g2d.StackfulRender;
import core.math.TmpShapes;
import core.ui.*;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import static core.Global.atlas;
import static core.graphic.Color.rgba8888;
import static core.graphic.GuiDrawing.calculateTextSize;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_1;

public class Slider extends LayoutElement<Slider> {
    private static final int FLAG_ZERO      = ELEMENT_LAST_FLAG << 1;
    private static final int FLAG_CLICKABLE = FLAG_ZERO << 1;

    private static final int TRACK_COLOR  = rgba8888(18, 18, 18, 255);
    private static final int TRACK_SHADOW = rgba8888(0, 0, 0, 150);
    private static final int THUMB_COLOR  = rgba8888(150, 150, 150, 255);
    private static final int TOOLTIP_BG   = rgba8888(10, 10, 10, 166);
    private static final int TOOLTIP_TEXT = rgba8888(255, 255, 255, 255);
    private static final int LABEL_COLOR  = rgba8888(150, 150, 150, 255);
    private static final int TITLE_COLOR  = rgba8888(210, 210, 210, 255);

    private static final float TRACK_H = 14f;
    private static final float THUMB_W = 16f;
    private static final float THUMB_H = 38f;
    private static final float TIP_W   = 96f;
    private static final float TIP_H   = 35f;
    private static final float BEAK_H  = 9f;
    private static final float GAP     = 14f;

    private static final float VALUE_SCALE = 17f / Font.fontSize;
    private static final float TITLE_SCALE = 13f / Font.fontSize;
    private static final float LABEL_SCALE = 11f / Font.fontSize;

    private static final float[] BEAK_T = new float[(int) BEAK_H];
    static {
        for (int r = 0; r < BEAK_T.length; r++) {
            float lo = 0f, hi = 1f;
            for (int i = 0; i < 7; i++) {
                float mid = (lo + hi) * 0.5f;
                if (bezier(mid, 0f, 6f, 8.5f, 9f) < r + 0.5f) {
                    lo = mid;
                } else {
                    hi = mid;
                }
            }
            BEAK_T[r] = (lo + hi) * 0.5f;
        }
    }

    public final Style.Slider style;

    private float sliderPos;
    private int sliderValue, lastSliderValue;

    private final ArrayList<Ember> embers = new ArrayList<>();

    public int min, max;
    public MoveListener updater;

    public interface MoveListener {
        void update(int pos, int max);
    }

    public Slider(@Nullable String id, Style.Slider style) {
        super(id);
        this.style = style;

        setFlag(FLAG_CLICKABLE, true);

        addListener(new ClickListener(GLFW_MOUSE_BUTTON_1, ClickType.PRESS) {
            private void updateFromMouse(float mx) {
                if (!isFlag(FLAG_CLICKABLE)) {
                    return;
                }

                lastSliderValue = sliderValue;

                if (style instanceof Style.MoltenSlider) {
                    float trackX = trackX();
                    float trackW = trackWidth();
                    sliderPos = Math.clamp(mx, trackX, trackX + trackW);
                    float norm = (sliderPos - trackX) / trackW;
                    int raw = Math.round(norm * (max - min) + min);
                    sliderValue = Math.clamp(Math.round(raw / 100f) * 100, min, max);
                } else {
                    sliderPos = Math.clamp(mx, x, x + width);
                    float norm = (sliderPos - x) / width;
                    sliderValue = Math.round(norm * (max - min) + min);
                }

                if (updater != null) {
                    updater.update(sliderValue, max);
                }
            }

            @Override
            protected void onPress(float mx, float my) {
                updateFromMouse(mx);
            }

            @Override
            protected void onDrag(float mx, float my) {
                updateFromMouse(mx);
            }
        });
    }

    @Override
    public @Nullable LayoutElement<?> hit(float hx, float hy) {
        if (!isFlag(FLAG_CLICKABLE)) {
            return null;
        }
        if (style instanceof Style.MoltenSlider) {
            float minY = trackY() - 55f;
            float maxY = trackY() + 55f;
            if (hx >= x && hx <= x + width && hy >= Math.min(minY, maxY) && hy <= Math.max(minY, maxY)) {
                return this;
            }
            return super.hit(hx, hy);
        }
        return super.hit(hx, hy);
    }

    public void setClickable(boolean state) {
        setFlag(FLAG_CLICKABLE, state);
    }

    public Slider onMove(MoveListener updater) {
        this.updater = updater;
        return this;
    }

    public Slider bounds(int min, int max) {
        this.sliderValue = min;
        this.lastSliderValue = min;
        this.min = min;
        this.max = max;
        return this;
    }

    public int sliderValue() {
        return sliderValue;
    }

    public Slider value(int val) {
        this.sliderValue = val;
        this.lastSliderValue = val;
        return this;
    }

    @Override
    public void onLayoutComplete() {
        if (min == max) {
            return;
        }

        float norm = (float) (sliderValue - min) / (max - min);
        norm = Math.clamp(norm, 0, 1);

        if (style instanceof Style.MoltenSlider) {
            sliderPos = trackX() + (norm * trackWidth());
        } else {
            sliderPos = x + (norm * width);
        }
    }

    @Override
    protected void updateThis(float dt) {
        if (style instanceof Style.MoltenSlider) {
            updateEmbers(dt);
        }
    }

    @Override
    public void draw() {
        if (min == max) {
            return;
        }

        if (style instanceof Style.MoltenSlider moltenStyle) {
            drawMolten(moltenStyle);
        } else {
            drawDefault();
        }
    }

    private void drawDefault() {
        float sliderX = sliderPos;

        final float radius = height * 1.75f;
        Fill.rect(x, y, sliderX - x, height, style.sliderColor);

        var tmp = TmpShapes.c1;
        tmp.set(style.sliderColor);
        tmp.a(tmp.a() - 100);
        Fill.rect(x, y, width, height, tmp);

        int rectHeight = 22;
        int rectBrightness = 170;
        int rectY = 45;
        float rectWidth = 1.75f;
        if (lastSliderValue != sliderValue) {
            rectHeight = 26;
            rectWidth = 2.5f;
            rectY = 40;
        }

        Atlas.Region triangle = atlas.get("UI/GUI/numberBoardTriangle");
        if (triangle != null) {
            StackfulRender.draw(triangle, sliderX - (triangle.width() / 2f), y + rectY - triangle.height());
        }

        String sliderValueStr = Integer.toString(sliderValue);
        int numbersWidth = calculateTextSize(sliderValueStr).x;

        Fill.rect(sliderX - (triangle != null ? triangle.width() / 2f : 0f) - (numbersWidth / (rectWidth * 2f)),
                y + rectY, 30 + numbersWidth / rectWidth, rectHeight,
                rgba8888(0, 0, 0, rectBrightness));

        float tx = sliderX - (numbersWidth / 2f) + 5;
        for (int i = 0; i < sliderValueStr.length(); i++) {
            char ch = sliderValueStr.charAt(i);
            Font.Glyph glyph = style.font.getGlyph(ch);
            if (glyph != null) {
                StackfulRender.draw(glyph, Styles.DIRTY_WHITE.rgba8888(), tx, y + rectY);
                tx += glyph.width();
            }
        }

        Fill.circle(sliderX - 0.875f * height, y - 5, radius, style.dotColor);
    }

    private void drawMolten(Style.MoltenSlider moltenStyle) {
        if (trackWidth() <= 0) {
            return;
        }

        float trackX = trackX();
        float trackW = trackWidth();
        float pct = Math.clamp((sliderPos - trackX) / trackW, 0f, 1f);

        if (moltenStyle.title != null && !moltenStyle.title.isEmpty()) {
            drawTitle(moltenStyle.title);
        }
        drawLabels();
        drawTrack(trackX, trackW, pct);
        drawEmbers();
        drawTooltip(trackX, trackW, pct);
        drawThumb(trackX + pct * trackW);
    }

    private float getSidePad() {
        if (style instanceof Style.MoltenSlider moltenStyle) {
            return moltenStyle.sidePad;
        }
        return 0f;
    }

    private float trackX() {
        return x + getSidePad() + scaledTextWidth(minLabel(), LABEL_SCALE) + GAP;
    }

    private float trackWidth() {
        float pad = getSidePad();
        return (width - pad * 2f) - scaledTextWidth(minLabel(), LABEL_SCALE)
                - scaledTextWidth(maxLabel(), LABEL_SCALE) - GAP * 2f;
    }

    private float trackY() {
        return y + (height - TRACK_H) * 0.5f;
    }

    private String minLabel() {
        return Integer.toString(min);
    }

    private String maxLabel() {
        return max >= 1000 ? (max / 1000) + "k" : Integer.toString(max);
    }

    private void drawTitle(String title) {
        drawScaledText(title, x + getSidePad(), trackY() + 36f, TITLE_SCALE, 1.5f, TITLE_COLOR);
    }

    private void drawLabels() {
        float labelTop = trackY() + 2f;
        float pad = getSidePad();
        drawScaledText(minLabel(), x + pad, labelTop, LABEL_SCALE, 0.5f, LABEL_COLOR);
        drawScaledText(maxLabel(), x + width - pad - scaledTextWidth(maxLabel(), LABEL_SCALE),
                labelTop, LABEL_SCALE, 0.5f, LABEL_COLOR);
    }

    private void drawTrack(float trackX, float trackW, float pct) {
        Fill.rect(trackX, trackY(), trackW, TRACK_H, TRACK_COLOR);

        float fillW = pct * trackW;
        for (float ox = 0f; ox < fillW; ox += 2f) {
            float stripW = Math.min(2f, fillW - ox);
            float norm = (ox + stripW * 0.5f) / trackW;
            int g = norm <= 0.5f
                    ? Math.round(106f + (60f - 106f) * (norm / 0.5f))
                    : Math.round(60f + (0f - 60f) * ((norm - 0.5f) / 0.5f));
            Fill.rect(trackX + ox, trackY(), stripW, TRACK_H, rgba8888(255, g, 0, 255));
        }

        Fill.rect(trackX, trackY() + 13f, trackW, 1f, TRACK_SHADOW);
        Fill.rect(trackX, trackY() + 12f, trackW, 1f, rgba8888(0, 0, 0, 75));
    }

    private void drawThumb(float centerX) {
        Fill.rect(centerX - THUMB_W * 0.5f, trackY() - (THUMB_H - TRACK_H) * 0.5f, THUMB_W, THUMB_H, THUMB_COLOR);
    }

    private void drawTooltip(float trackX, float trackW, float pct) {
        float thumbX = pct * trackW;
        float minCenter = TIP_W * 0.5f;
        float maxCenter = Math.max(minCenter, trackW - TIP_W * 0.5f);
        float clampedCenter = Math.clamp(thumbX, minCenter, maxCenter);

        float boxX = trackX + clampedCenter - TIP_W * 0.5f;
        float beakTipY = trackY() - 12f;

        float relThumbX = thumbX - (clampedCenter - TIP_W * 0.5f);
        float bx = Math.clamp(relThumbX, 12f, TIP_W - 12f);
        float dxR = TIP_W - bx;
        float dxL = bx;
        float cpR1 = bx + dxR * 0.58f;
        float cpR2 = bx + dxR * 0.17f;
        float cpL1 = bx - dxL * 0.17f;
        float cpL2 = bx - dxL * 0.58f;

        for (int r = 0; r < BEAK_T.length; r++) {
            float t = BEAK_T[r];
            float xL = bezier(t, bx, cpL1, cpL2, 0f);
            float xR = bezier(t, bx, cpR2, cpR1, TIP_W);
            Fill.rect(boxX + xL, beakTipY - r - 1f, Math.max(1f, xR - xL), 1f, TOOLTIP_BG);
        }

        Fill.rect(boxX, beakTipY - TIP_H, TIP_W, TIP_H - BEAK_H, TOOLTIP_BG);

        String value = formatValue(sliderValue);
        float textW = scaledTextWidth(value, VALUE_SCALE);
        drawScaledText(value, boxX + (TIP_W - textW) * 0.5f, beakTipY - 34f, VALUE_SCALE, 0f, TOOLTIP_TEXT);
    }

    private void updateEmbers(float dt) {
        if (min == max || width <= 0) {
            return;
        }

        float trackX = trackX();
        float trackW = trackWidth();
        if (trackW <= 0) {
            return;
        }

        float speed = Math.clamp(dt * 60f, 0.1f, 3f);
        float pct = Math.clamp((sliderPos - trackX) / trackW, 0f, 1f);

        float heatNorm = (pct - 0.4f) / 0.6f;
        int maxAllowed = Math.min(4, (int) (heatNorm * 4f) + 1);
        if (pct >= 0.4f && embers.size() < maxAllowed) {
            float spawnChance = (float) Math.pow(Math.clamp(heatNorm, 0f, 1f), 2.8) * 0.04f * speed;
            if (ThreadLocalRandom.current().nextFloat() < spawnChance) {
                spawnEmber(trackX, trackY(), trackW, pct);
            }
        }

        float maxX = trackX + pct * trackW + 4f;
        for (int i = embers.size() - 1; i >= 0; i--) {
            Ember p = embers.get(i);
            p.life += speed;

            if (p.life >= p.maxLife || p.x > maxX) {
                embers.remove(i);
                continue;
            }

            p.x += p.vx * speed;
            p.y += p.vy * speed;
            p.vx += p.drift * speed;
        }
    }

    private void drawEmbers() {
        for (Ember p : embers) {
            float progress = p.life / p.maxLife;
            float alpha = 1f;
            if (progress < 0.25f) {
                alpha = progress / 0.25f;
            } else if (progress > 0.65f) {
                alpha = 1f - (progress - 0.65f) / 0.35f;
            }
            alpha = Math.clamp(alpha * 0.85f, 0f, 1f);
            Fill.rect(p.x, p.y, p.size, p.size, rgba8888(p.r, p.g, p.b, (int) (alpha * 255f)));
        }
    }

    private void spawnEmber(float trackX, float trackY, float trackW, float pct) {
        var rnd = ThreadLocalRandom.current();
        float minP = Math.max(0.2f, pct - 0.4f);
        float bias = 1f - (float) Math.pow(rnd.nextDouble(), 2.2);
        float p = minP + (pct - minP) * bias;
        float proximity = (p - minP) / Math.max(0.01f, pct - minP);

        int r = 255, g = 51, b = 0;
        if (pct >= 0.85f && proximity > 0.85f && rnd.nextFloat() > 0.4f) {
            r = 255;
            g = 255;
            b = 255;
        } else if (pct >= 0.75f && proximity > 0.7f) {
            if (rnd.nextFloat() > 0.5f) {
                g = 244;
                b = 208;
            } else {
                g = 224;
                b = 130;
            }
        } else if (proximity > 0.4f) {
            if (rnd.nextFloat() > 0.5f) {
                g = 160;
                b = 64;
            } else {
                g = 102;
                b = 0;
            }
        }

        Ember e = new Ember();
        e.x = trackX + p * trackW;
        e.y = trackY + 13f - rnd.nextFloat() * 2.5f;
        e.vx = (rnd.nextFloat() - 0.5f) * 0.2f;
        e.vy = 0.35f + rnd.nextFloat() * 0.4f;
        e.drift = (rnd.nextFloat() - 0.5f) * 0.02f;
        e.maxLife = 50f + rnd.nextFloat() * 40f;
        e.size = Math.max(1f, 1f + rnd.nextFloat() * 0.3f);
        e.r = r;
        e.g = g;
        e.b = b;
        embers.add(e);
    }

    private float drawScaledText(String text, float x, float y, float scale, float spacing, int color) {
        for (int i = 0; i < text.length(); i++) {
            Font.Glyph glyph = style.font.getGlyph(text.charAt(i));
            if (glyph == null) {
                continue;
            }
            StackfulRender.draw(glyph, color, x, y, glyph.width() * scale, glyph.height() * scale);
            x += glyph.width() * scale + spacing;
        }
        return x;
    }

    private float scaledTextWidth(String text, float scale) {
        float w = 0f;
        for (int i = 0; i < text.length(); i++) {
            Font.Glyph glyph = style.font.getGlyph(text.charAt(i));
            w += (glyph == null ? 0 : glyph.width()) * scale;
        }
        return w;
    }

    private String formatValue(int value) {
        String digits = Integer.toString(value);
        StringBuilder sb = new StringBuilder(digits.length() + 4);
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && (digits.length() - i) % 3 == 0) {
                sb.append(' ');
            }
            sb.append(digits.charAt(i));
        }
        return sb.toString();
    }

    private static float bezier(float t, float p0, float p1, float p2, float p3) {
        float u = 1f - t;
        return u * u * u * p0 + 3f * u * u * t * p1 + 3f * u * t * t * p2 + t * t * t * p3;
    }

    private static final class Ember {
        float x, y, vx, vy, drift;
        float life, maxLife;
        float size;
        int r, g, b;
    }
}