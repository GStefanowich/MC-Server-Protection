package net.TheElm.project.utilities;

import net.minecraft.world.World;

import java.util.Random;

public final class IntUtils {
    private IntUtils() {}
    
    public static int random(Random random, int min, int max) {
        if (min == max)
            return min;
        return random.nextInt(max - min) + min;
    }
    
    public static long timeToDays(World world) {
        return world.getTimeOfDay() / 24000L % 2147483647L;
    }
    
    public static boolean between(int lower, int middle, int upper) {
        return IntUtils.between(lower, middle, upper, true);
    }
    public static boolean between(int lower, int middle, int upper, boolean inclusive) {
        return (inclusive && (lower == middle || upper == middle)) || (lower < middle && middle < upper);
    }
}
