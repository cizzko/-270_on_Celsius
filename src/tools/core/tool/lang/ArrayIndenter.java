package core.tool.lang;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

import java.io.IOException;
import java.util.Collection;

final class ArrayIndenter extends DefaultIndenter {
    public void writeIndentation(JsonGenerator jg, int level) throws IOException {
        int size;
        if (jg.currentValue() instanceof Collection<?> c) {
            size = c.size();
        } else {
            size = 0;
            assert false : jg.currentValue();
        }
        if (size > 1) {
            super.writeIndentation(jg, level);
        } else {
            DefaultPrettyPrinter.FixedSpaceIndenter.instance.writeIndentation(jg, level);
        }
    }
}
