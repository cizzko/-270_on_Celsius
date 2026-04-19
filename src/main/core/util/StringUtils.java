package core.util;

public class StringUtils {
    public static String normalizePath(String v) {
        // Потому что \\ выглядит ужасно
        String unixLike = v.replace("\\", "/");

        if (unixLike.startsWith("/")) {
            unixLike = unixLike.substring(1);
        }
        return unixLike;
    }
}
