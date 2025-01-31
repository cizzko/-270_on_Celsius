package core.util;

import java.util.*;

public class ArrayUtils {

    public static int findFreeCell(Object[] array) {
        if (array != null) {
            for (int i = 0; i < array.length; i++) {
                if (array[i] == null) {
                    return i;
                }
            }
        }
        return -1; // not found or null
    }

    public static int findEqualsObjects(Object[] array, Object equalsObject) {
        return (int) Arrays.stream(array).filter(obj -> obj != null && obj.equals(equalsObject)).count();
    }

    public static int findDistinctObjects(Object[] array) {
        return (int) Arrays.stream(array).filter(Objects::nonNull).distinct().count();
    }
}
