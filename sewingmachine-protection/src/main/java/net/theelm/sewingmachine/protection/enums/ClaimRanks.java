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

package net.theelm.sewingmachine.protection.enums;

import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.theelm.sewingmachine.interfaces.TranslationDescriber;
import net.theelm.sewingmachine.utilities.ColorUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public enum ClaimRanks implements TranslationDescriber {
    OWNER(2, "Restrict to Owners only", "Let this player do everything", ColorUtils.getNearestTextColor(34, 139, 34)),
    ALLY(1, "Restrict to Allies and Owners", "Make this player your Ally", ColorUtils.getNearestTextColor(80, 200, 120)),
    PASSIVE(0, "Restrict to anyone but Enemies", "This player is Nothing to you", ColorUtils.getNearestTextColor(125, 249, 255)),
    ENEMY(-1, "Anyone can do this", "Make this player your Enemy", ColorUtils.getNearestTextColor(228, 208, 10));
    
    private final @NotNull TextColor color;
    public final int power;
    
    private final String baseDescription;
    private final String setterDescription;
    
    ClaimRanks(int power, String desc, String setter, Color color) {
        this(power, desc, setter, ColorUtils.getNearestTextColor(color));
    }
    ClaimRanks(int power, String desc, String setter, TextColor color) {
        this.power = power;
        this.color = color;
        
        this.baseDescription = desc;
        this.setterDescription = setter;
    }
    
    public @NotNull Text rankNormalDescription() {
        return Text.literal(this.baseDescription);
    }
    public @NotNull Text rankSetterDescription() {
        return Text.literal(this.setterDescription);
    }
    
    public boolean canPerform( @NotNull ClaimRanks userRank ) {
        return userRank.power >= this.power;
    }
    
    @Override
    public @NotNull String getTranslationKey() {
        return "claim.rank." + this.name().toLowerCase();
    }
    
    @Override
    public @NotNull TextColor getColor() {
        return this.color;
    }
}
