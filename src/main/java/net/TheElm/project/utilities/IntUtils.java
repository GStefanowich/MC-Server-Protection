package net.TheElm.project.utilities;

import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public final class IntUtils {
    private IntUtils() {}
    
    public static int random(Random random, int min, int max) {
        if (min == max)
            return min;
        return random.nextInt(max - min) + min;
    }
    
    public static long timeToDays(@NotNull World world) {
        return world.getTimeOfDay() / 24000L % 2147483647L;
    }
    
    public static boolean between(int lower, int middle, int upper) {
        return IntUtils.between(lower, middle, upper, true);
    }
    public static boolean between(int lower, int middle, int upper, boolean inclusive) {
        return (inclusive && (lower == middle || upper == middle)) || (lower < middle && middle < upper);
    }
    
    public static @NotNull String text(int count) {
        switch (count) {
            case 1: return "one";
            case 2: return "two";
            case 3: return "three";
            case 4: return "four";
            case 5: return "five";
            case 6: return "six";
            case 7: return "seven";
            case 8: return "eight";
            case 9: return "nine";
            case 10: return "ten";
            default: return String.valueOf(count);
        }
    }
}
