package net.theelm.sewingmachine.utilities;

import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldProperties;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public final class IntUtils {
    private IntUtils() {}
    
    public static int random(Random random, int min, int max) {
        if (min == max)
            return min;
        return random.nextInt(max - min) + min;
    }
    
    public static long timeToDays(@NotNull World world) {
        return IntUtils.timeToDays(world.getLevelProperties());
    }
    public static long timeToDays(@NotNull WorldProperties properties) {
        return properties.getTimeOfDay() / 24000L % 2147483647L;
    }
    
    public static boolean between(int lower, int middle, int upper) {
        return IntUtils.between(lower, middle, upper, true);
    }
    public static boolean between(int lower, int middle, int upper, boolean inclusive) {
        return (inclusive && (lower == middle || upper == middle)) || (lower < middle && middle < upper);
    }
    
    public static @NotNull String text(int count) {
        return switch (count) {
            case 1 -> "one";
            case 2 -> "two";
            case 3 -> "three";
            case 4 -> "four";
            case 5 -> "five";
            case 6 -> "six";
            case 7 -> "seven";
            case 8 -> "eight";
            case 9 -> "nine";
            case 10 -> "ten";
            default -> String.valueOf(count);
        };
    }
    
    public static TimeUnit getTimeUnit(@NotNull String symbol) {
        return switch (symbol) {
            case "s" -> TimeUnit.SECONDS;
            case "m" -> TimeUnit.MINUTES;
            case "h" -> TimeUnit.HOURS;
            case "d" -> TimeUnit.DAYS;
            default -> null;
        };
    }
    
    public static int convertToTicks(int length, TimeUnit unit) {
        return Math.toIntExact(TimeUnit.SECONDS.convert(length, unit) * 20);
    }
}
