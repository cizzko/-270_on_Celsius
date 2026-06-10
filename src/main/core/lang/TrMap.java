package core.lang;

import java.util.Comparator;
import java.util.TreeMap;

public final class TrMap {
    private TrMap() {}

    public static final Comparator<String> TR_ORDER = String.CASE_INSENSITIVE_ORDER;

    public static TreeMap<String, TrLine> makeTrMap() {
        return new TreeMap<String, TrLine>(TR_ORDER);
    }
}
