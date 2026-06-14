package core.util;

import java.util.Objects;

/// Рефлексия, классы и метадата
public final class TypeUtil {
    private TypeUtil() {}

    public static String canonicalNameOrParent(Class<?> type) {
        String name = type.getCanonicalName();
        if (name == null)
            name = type.getSuperclass().getCanonicalName();
        // Намерено не разрешаю случай:
        // class LocalParent {}                    // getCanonicalName() -> null
        // class LocalChild extends LocalParent {} // getCanonicalName() -> null
        Objects.requireNonNull(name);
        return name;
    }
}
