package core.ui.widget;

import core.g2d.Fill;
import core.graphic.GuiDrawing;
import core.input.InputListener;
import core.math.MathUtil;
import core.math.Rectangle;
import core.ui.ClickListener;
import core.ui.ClickType;
import core.ui.Style;
import core.ui.Styles;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;

public final class Console extends Panel {
    private static final int FLAG_ZERO = GROUP_LAST_FLAG << 1;

    public static final float sliderWidth  = 8;
    public static final float sliderHeight = 30;

    private final ArrayList<String> history = new ArrayList<>();
    private final int historySize = 16;

    private int index;
    private float sliderY;

    public Console(@Nullable String id, Style.Panel style) {
        super(id, style);
        setFlag(FLAG_ZERO, true);

        addListener(new InputListener() {
            @Override
            public void onScroll(float xOffset, float yOffset) {
                int direction = -(int)Math.signum(yOffset);
                if (index + direction >= history.size() || index + direction < 0) {
                    return;
                }
                index += direction;
                computeSliderY();
            }
        });

        addListener(new ClickListener(GLFW.GLFW_MOUSE_BUTTON_1, ClickType.PRESS) {
            boolean scrolling;

            @Override
            protected void onPress(float mx, float my) {
                if (Rectangle.contains(x, y, 20, height, mx, my)) {
                    scrolling = true;
                }
            }

            @Override
            protected void onRelease(float x, float y) {
                scrolling = false;
            }

            @Override
            protected void onDrag(float mx, float my) {
                if (scrolling) {
                    sliderY = Math.clamp(my,
                            y+sliderHeight/2f,
                            y+height-sliderHeight/2f);
                    setFlag(FLAG_ZERO, false);
                    updateIndex(sliderValue());
                }
            }
        });
    }

    private void computeSliderY() {
        sliderY = MathUtil.lerp(y+height-sliderHeight/2f, y+sliderHeight/2f, (float)index/(history.size() - 1));
    }

    private void updateIndex(int i) {
        index = i;
    }

    private float sliderY() {
        if (isFlag(FLAG_ZERO)) {
            setFlag(FLAG_ZERO, false);
            sliderY = y+height-sliderHeight/2f;
            // FIXME Не установило координату/разме при открытии F5
            // java.lang.Exception
            //         at core.main/core.ui.Console.sliderY(Console.java:86)
            //         at core.main/core.ui.Console.drawThis(Console.java:131)
            //         at core.main/core.ui.LayoutGroup.draw(LayoutGroup.java:50)
            //         at core.main/core.ui.LayoutGroup.draw(LayoutGroup.java:55)
            //         at core.main/core.ui.LayoutGroup.draw(LayoutGroup.java:55)
            //         at core.main/core.UIScene.draw(UIScene.java:94)
            //         at core.main/core.MenuScene.draw(MenuScene.java:51)
            //         at core.main/core.GameScene.readyLoop(GameScene.java:93)
            //         at core.main/core.GameScene.loop(GameScene.java:70)
        }
        return sliderY;
    }

    public int sliderValue() {
        float relativePos = (sliderY() - y) / (height-sliderHeight);
        relativePos = Math.clamp(relativePos, 0, 1);
        relativePos = 1f - relativePos;
        int cnt = history.size() - 1;
        if (cnt < 0)
            cnt = 0;
        return Math.round(relativePos * cnt + 0);
    }

    public Console add(String str) {
        if (history.size() >= historySize) {
            history.removeFirst();
        } else {
            index++;
        }
        history.add(str);
        return this;
    }

    @Override
    protected void drawThis() {
        super.drawThis();

        int begin = Math.max(0, index - historySize);
        int end = Math.min(history.size(), index + 1);
        int lineHeight = 30;
        int ox = 25;
        int oy = 20;
        float lineY = y + oy;
        for (String str : history.subList(begin, end).reversed()) {
            if (lineY + lineHeight >= y+height) {
                break;
            }

            GuiDrawing.drawText(x + ox, lineY, str, Styles.TEXT_COLOR);
            lineY += lineHeight;
        }

        float sliderX = x + sliderWidth / 2f;
        float sliderY = sliderY();
        Fill.rect(sliderX, y, sliderWidth, height, Styles.DEFAULT_PANEL_COLOR);
        Fill.rect(sliderX, sliderY - sliderHeight / 2f, sliderWidth, sliderHeight, Styles.GRAY_BRIGHT);
    }

    public void clearHistory() {
        index = 0;
        history.clear();
        computeSliderY();
    }
}
