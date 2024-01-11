package core.g2d;

import static org.lwjgl.opengl.GL46.*;

public enum Blending {
    NORMAL {
        @Override
        public void apply() {
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        }
    },
    DISABLE {
        @Override
        public void apply() {
            glDisable(GL_BLEND);
        }
    };

    public abstract void apply();
}
