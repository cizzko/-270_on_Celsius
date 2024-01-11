package core.g2d;

import core.Utils.Sized;

public sealed interface Drawable
        extends Sized
        permits Atlas.Region, Font.Glyph, Texture {

    int width();
    int height();

    float u();
    float v();
    float u2();
    float v2();
}
