package net.TheElm.project.utilities;

import java.util.function.Function;

public final class Assert {
    private Assert() {}
    
    @SafeVarargs
    public static <T> boolean ifAny(Function<T, Boolean> function, T... objects) {
        for (T obj : objects) {
            if (function.apply(obj))
                return true;
        }
        return false;
    }
    
    @SafeVarargs
    public static <T> boolean ifAll(Function<T, Boolean> function, T... objects) {
        for (T obj : objects) {
            if (!function.apply(obj))
                return false;
        }
        return true;
    }
}
