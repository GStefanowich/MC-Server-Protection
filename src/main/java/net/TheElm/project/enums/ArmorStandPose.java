/*
 * This software is licensed under the MIT License
 * https://github.com/GStefanowich/MC-Server-Protection
 *
 * Copyright (c) 2019 Gregory Stefanowich
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.TheElm.project.enums;

import net.minecraft.entity.Entity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.EulerAngle;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Created on Jun 27 2021 at 8:48 PM.
 * By greg in SewingMachineMod
 */
public enum ArmorStandPose {
    DEFAULT(0) {
        @Override
        public @NotNull EulerAngle getLeftArmRotation() {
            return new EulerAngle(-10, 0, -10);
        }
        @Override
        public @NotNull EulerAngle getRightArmRotation() {
            return new EulerAngle(-15, 0, 10);
        }
        @Override
        public @NotNull EulerAngle getLeftLegRotation() {
            return new EulerAngle(-1, 0, -1);
        }
        @Override
        public @NotNull EulerAngle getRightLegRotation() {
            return new EulerAngle(1, 0, 1);
        }
    },
    NONE(1),
    SOLEMN(2) {
        @Override
        public @NotNull EulerAngle getHeadRotation() {
            return new EulerAngle(15, 0, 0);
        }
        @Override
        public @NotNull EulerAngle getBodyRotation() {
            return new EulerAngle(0, 0, 2);
        }
        @Override
        public @NotNull EulerAngle getLeftArmRotation() {
            return new EulerAngle(-30, 15, 15);
        }
        @Override
        public @NotNull EulerAngle getRightArmRotation() {
            return new EulerAngle(-60, -20, -10);
        }
        @Override
        public @NotNull EulerAngle getLeftLegRotation() {
            return new EulerAngle(-1, 0, -1);
        }
        @Override
        public @NotNull EulerAngle getRightLegRotation() {
            return new EulerAngle(1, 0, 1);
        }
    },
    ATHENA(3) {
        @Override
        public @NotNull EulerAngle getHeadRotation() {
            return new EulerAngle(-5, 0, 0);
        }
        @Override
        public @NotNull EulerAngle getBodyRotation() {
            return new EulerAngle(0, 0, 2);
        }
        @Override
        public @NotNull EulerAngle getLeftArmRotation() {
            return new EulerAngle(10, 0, -5);
        }
        @Override
        public @NotNull EulerAngle getRightArmRotation() {
            return new EulerAngle(-60, 20, -10);
        }
        @Override
        public @NotNull EulerAngle getLeftLegRotation() {
            return new EulerAngle(-3, -3, -3);
        }
        @Override
        public @NotNull EulerAngle getRightLegRotation() {
            return new EulerAngle(3, 3, 3);
        }
    },
    BRANDISH(4) {
        @Override
        public @NotNull EulerAngle getHeadRotation() {
            return new EulerAngle(-15, 0, 0);
        }
        @Override
        public @NotNull EulerAngle getBodyRotation() {
            return new EulerAngle(0, 0, -2);
        }
        @Override
        public @NotNull EulerAngle getLeftArmRotation() {
            return new EulerAngle(20, 0, -10);
        }
        @Override
        public @NotNull EulerAngle getRightArmRotation() {
            return new EulerAngle(-110, 50, 0);
        }
        @Override
        public @NotNull EulerAngle getLeftLegRotation() {
            return new EulerAngle(5, -3, -3);
        }
        @Override
        public @NotNull EulerAngle getRightLegRotation() {
            return new EulerAngle(-5, 3, 3);
        }
    },
    HONOR(5) {
        @Override
        public @NotNull EulerAngle getHeadRotation() {
            return new EulerAngle(-15, 0, 0);
        }
        @Override
        public @NotNull EulerAngle getLeftArmRotation() {
            return new EulerAngle(-110, 35, 0);
        }
        @Override
        public @NotNull EulerAngle getRightArmRotation() {
            return new EulerAngle(-110, -35, 0);
        }
        @Override
        public @NotNull EulerAngle getLeftLegRotation() {
            return new EulerAngle(5, -3, -3);
        }
        @Override
        public @NotNull EulerAngle getRightLegRotation() {
            return new EulerAngle(-5, 3, 3);
        }
    },
    ENTERTAINMENT(6) {
        @Override
        public @NotNull EulerAngle getHeadRotation() {
            return new EulerAngle(-15, 0, 0);
        }
        @Override
        public @NotNull EulerAngle getLeftArmRotation() {
            return new EulerAngle(-110, -35, 0);
        }
        @Override
        public @NotNull EulerAngle getRightArmRotation() {
            return new EulerAngle(-110, 35, 0);
        }
        @Override
        public @NotNull EulerAngle getLeftLegRotation() {
            return new EulerAngle(5, -3, -3);
        }
        @Override
        public @NotNull EulerAngle getRightLegRotation() {
            return new EulerAngle(-5, 3, 3);
        }
    },
    SALUTE(7) {
        @Override
        public @NotNull EulerAngle getLeftArmRotation() {
            return new EulerAngle(10, 0, -5);
        }
        @Override
        public @NotNull EulerAngle getRightArmRotation() {
            return new EulerAngle(-70, -40, 0);
        }
        @Override
        public @NotNull EulerAngle getLeftLegRotation() {
            return new EulerAngle(-1, 0, -1);
        }
        @Override
        public @NotNull EulerAngle getRightLegRotation() {
            return new EulerAngle(1, 0, 1);
        }
    },
    RIPOSTE(8) {
        @Override
        public @NotNull EulerAngle getHeadRotation() {
            return new EulerAngle(16, 20, 0);
        }
        @Override
        public @NotNull EulerAngle getLeftArmRotation() {
            return new EulerAngle(4, 8, 237);
        }
        @Override
        public @NotNull EulerAngle getRightArmRotation() {
            return new EulerAngle(246, 0, 89);
        }
        @Override
        public @NotNull EulerAngle getLeftLegRotation() {
            return new EulerAngle(-14, -18, -16);
        }
        @Override
        public @NotNull EulerAngle getRightLegRotation() {
            return new EulerAngle(8, 20, 4);
        }
    },
    ZOMBIE(9) {
        @Override
        public @NotNull EulerAngle getHeadRotation() {
            return new EulerAngle(-10, 0, -5);
        }
        @Override
        public @NotNull EulerAngle getLeftArmRotation() {
            return new EulerAngle(-105, 0, 0);
        }
        @Override
        public @NotNull EulerAngle getRightArmRotation() {
            return new EulerAngle(-100, 0, 0);
        }
        @Override
        public @NotNull EulerAngle getLeftLegRotation() {
            return new EulerAngle(7, 0, 0);
        }
        @Override
        public @NotNull EulerAngle getRightLegRotation() {
            return new EulerAngle(-46, 0, 0);
        }
    },
    CANCAN_A(10) {
        @Override
        public @NotNull EulerAngle getHeadRotation() {
            return new EulerAngle(-5, 18, 0);
        }
        @Override
        public @NotNull EulerAngle getBodyRotation() {
            return new EulerAngle(0, 22, 0);
        }
        @Override
        public @NotNull EulerAngle getLeftArmRotation() {
            return new EulerAngle(8, 0, -114);
        }
        @Override
        public @NotNull EulerAngle getRightArmRotation() {
            return new EulerAngle(0, 84, 111);
        }
        @Override
        public @NotNull EulerAngle getLeftLegRotation() {
            return new EulerAngle(-111, 55, 0);
        }
        @Override
        public @NotNull EulerAngle getRightLegRotation() {
            return new EulerAngle(0, 23, -13);
        }
    },
    CANCAN_B(11) {
        @Override
        public @NotNull EulerAngle getHeadRotation() {
            return new EulerAngle(-10, -20, 0);
        }
        @Override
        public @NotNull EulerAngle getBodyRotation() {
            return new EulerAngle(0, -18, 0);
        }
        @Override
        public @NotNull EulerAngle getLeftArmRotation() {
            return new EulerAngle(0, 0, -112);
        }
        @Override
        public @NotNull EulerAngle getRightArmRotation() {
            return new EulerAngle(8, 90, 111);
        }
        @Override
        public @NotNull EulerAngle getLeftLegRotation() {
            return new EulerAngle(0, 0, 13);
        }
        @Override
        public @NotNull EulerAngle getRightLegRotation() {
            return new EulerAngle(-119, -42, 0);
        }
    },
    HERO(12) {
        @Override
        public @NotNull EulerAngle getHeadRotation() {
            return new EulerAngle(-4, 67, 0);
        }
        @Override
        public @NotNull EulerAngle getBodyRotation() {
            return new EulerAngle(0, 8, 0);
        }
        @Override
        public @NotNull EulerAngle getLeftArmRotation() {
            return new EulerAngle(16, 32, -8);
        }
        @Override
        public @NotNull EulerAngle getRightArmRotation() {
            return new EulerAngle(-99, 63, 0);
        }
        @Override
        public @NotNull EulerAngle getLeftLegRotation() {
            return new EulerAngle(0, -75, -8);
        }
        @Override
        public @NotNull EulerAngle getRightLegRotation() {
            return new EulerAngle(4, 63, 8);
        }
    };
    
    private final int redstonePower;
    
    ArmorStandPose(int redstone) {
        this.redstonePower = redstone;
    }
    
    public @NotNull ArmorStandPose next() {
        switch (this) {
            case DEFAULT: return NONE;
            case NONE: return SOLEMN;
            case SOLEMN: return ATHENA;
            case ATHENA: return BRANDISH;
            case BRANDISH: return HONOR;
            case HONOR: return ENTERTAINMENT;
            case ENTERTAINMENT: return SALUTE;
            case SALUTE: return RIPOSTE;
            case RIPOSTE: return ZOMBIE;
            case ZOMBIE: return CANCAN_A;
            case CANCAN_A: return CANCAN_B;
            case CANCAN_B: return HERO;
            case HERO: break;
        }
        return DEFAULT;
    }
    public @NotNull ArmorStandPose fromRedstone(int strength) {
        if (strength == 0)
            return DEFAULT;
        if (strength == 1)
            return NONE;
        if (strength == 2)
            return SOLEMN;
        if (strength == 3)
            return ATHENA;
        if (strength == 4)
            return BRANDISH;
        if (strength == 5)
            return HONOR;
        if (strength == 6)
            return ENTERTAINMENT;
        if (strength == 7)
            return SALUTE;
        if (strength == 8)
            return RIPOSTE;
        if (strength == 9)
            return ZOMBIE;
        if (strength == 10)
            return CANCAN_A;
        if (strength == 11)
            return CANCAN_B;
        if (strength >= 12)
            return HERO;
        return DEFAULT;
    }
    
    public @NotNull EulerAngle getHeadRotation() {
        return new EulerAngle(0, 0, 0);
    }
    public @NotNull EulerAngle getBodyRotation() {
        return new EulerAngle(0, 0, 0);
    }
    public @NotNull EulerAngle getLeftArmRotation() {
        return new EulerAngle(0, 0, 0);
    }
    public @NotNull EulerAngle getRightArmRotation() {
        return new EulerAngle(0, 0, 0);
    }
    public @NotNull EulerAngle getLeftLegRotation() {
        return new EulerAngle(0, 0, 0);
    }
    public @NotNull EulerAngle getRightLegRotation() {
        return new EulerAngle(0, 0, 0);
    }
    
    public static void apply(@NotNull ArmorStandPose pose, @NotNull Entity entity) {
        if (entity instanceof ArmorStandEntity)
            ArmorStandPose.apply(pose, (ArmorStandEntity) entity);
    }
    public static void apply(@NotNull ArmorStandPose pose, @NotNull ArmorStandEntity entity) {
        entity.setHeadRotation(pose.getHeadRotation());
        entity.setBodyRotation(pose.getBodyRotation());
        entity.setLeftArmRotation(pose.getLeftArmRotation());
        entity.setRightArmRotation(pose.getRightArmRotation());
        entity.setLeftLegRotation(pose.getLeftLegRotation());
        entity.setRightLegRotation(pose.getRightLegRotation());
    }
    public static @NotNull ArmorStandPose getCurrent(@NotNull Entity entity) {
        return entity instanceof ArmorStandEntity ? ArmorStandPose.getCurrent((ArmorStandEntity) entity) : ArmorStandPose.DEFAULT;
    }
    public static @NotNull ArmorStandPose getCurrent(@NotNull ArmorStandEntity entity) {
        DataTracker tracker = entity.getDataTracker();
        for (ArmorStandPose pose : ArmorStandPose.values()) {
            if (Objects.equals(pose.getHeadRotation(), tracker.get(ArmorStandEntity.TRACKER_HEAD_ROTATION))
                && Objects.equals(pose.getBodyRotation(), tracker.get(ArmorStandEntity.TRACKER_BODY_ROTATION))
                && Objects.equals(pose.getLeftArmRotation(), tracker.get(ArmorStandEntity.TRACKER_LEFT_ARM_ROTATION))
                && Objects.equals(pose.getRightArmRotation(), tracker.get(ArmorStandEntity.TRACKER_RIGHT_ARM_ROTATION))
                && Objects.equals(pose.getLeftLegRotation(), tracker.get(ArmorStandEntity.TRACKER_LEFT_LEG_ROTATION))
                && Objects.equals(pose.getRightLegRotation(), tracker.get(ArmorStandEntity.TRACKER_RIGHT_LEG_ROTATION))) return pose;
        }
        return DEFAULT;
    }
}
