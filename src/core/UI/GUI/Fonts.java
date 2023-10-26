package core.UI.GUI;
import core.EventHandling.Logging.Logger;
import core.World.Textures.TextureLoader;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class Fonts {
    private static final int fontSize = 18;
    private static final HashMap<Character, ByteBuffer> chars = new HashMap<>();
    public static ConcurrentHashMap<Character, Dimension> letterSize = new ConcurrentHashMap<>();

    public static void generateFont(String pathTTF) {
        Font font = null;
        try {
            font = Font.createFont(Font.PLAIN, new File(pathTTF));
            //default 12
            font = font.deriveFont(Font.PLAIN, (float) (fontSize * Toolkit.getDefaultToolkit().getScreenResolution() / 72.0));
        } catch (IOException | FontFormatException e) {
            Logger.printException("Error when generate font", e);
            Logger.logExit(1);
        }

        if (font != null) {
            FontRenderContext fontRenderContext = new FontRenderContext(null, true, true);

            StringBuilder allChars = new StringBuilder();
            for (char c = 0; c < Character.MAX_VALUE; c++) {
                if (font.canDisplay(c) && font.createGlyphVector(fontRenderContext, Character.toString(c)).getGlyphCode(0) != 0) {
                    allChars.append(c);
                }
            }

            for (int a = 0; a < allChars.length(); a++) {
                char c = allChars.charAt(a);

                if (font.canDisplay(c)) {
                    String str = Character.toString(c);
                    LineMetrics lm = font.getLineMetrics(str, fontRenderContext);

                    if (font.getStringBounds(str, fontRenderContext).getWidth() == 0 || lm.getHeight() == 0) {
                        continue;
                    }

                    int charWidth = (int) font.getStringBounds(str, fontRenderContext).getWidth();
                    int charHeight = (int) lm.getHeight();

                    BufferedImage charImage = new BufferedImage(charWidth, charHeight, BufferedImage.TYPE_INT_ARGB);
                    Graphics graphics = charImage.getGraphics();

                    graphics.setColor(Color.WHITE);
                    graphics.setFont(font);
                    graphics.drawString(str, 0, charHeight - (int) lm.getDescent());

                    graphics.dispose();

                    letterSize.put(c, new Dimension(charWidth, charHeight));
                    chars.put(c, TextureLoader.ByteBufferEncoder(charImage));

                } else {
                    Logger.log("Charter '" + c + "' cannot displayed, file: '" + pathTTF + "'");
                }
            }
        }
    }

    public static Dimension getCharDimension(char c) {
        return letterSize.get(c) != null && letterSize.get(c).width > 0 && letterSize.get(c).height > 0 ? letterSize.get(c) : letterSize.get('?');
    }

    public static ByteBuffer getCharBuffer(char c) {
        return chars.get(c) != null ? chars.get(c) : chars.get('?');
    }
}
