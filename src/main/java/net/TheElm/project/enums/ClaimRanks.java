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

import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

public enum ClaimRanks {
    OWNER(2, "Restrict to Owners only", "Let this player do everything", Formatting.DARK_GREEN),
    ALLY(1, "Restrict to Allies and Owners", "Make this player your Ally", Formatting.DARK_GREEN),
    PASSIVE(0, "Restrict to anyone but Enemies", "This player is Nothing to you", Formatting.YELLOW),
    ENEMY(-1, "Anyone can do this", "Make this player your Enemy", Formatting.DARK_RED);
    
    private final Formatting[] color;
    public final int power;
    
    private final String baseDescription;
    private final String setterDescription;
    
    ClaimRanks(int power, String desc, String setter, Formatting... formatting ) {
        this.power = power;
        this.color = formatting;
        
        this.baseDescription = desc;
        this.setterDescription = setter;
    }
    
    public @NotNull Text rankNormalDescription() {
        return new LiteralText(this.baseDescription);
    }
    public @NotNull Text rankSetterDescription() {
        return new LiteralText(this.setterDescription);
    }
    
    public boolean canPerform( @NotNull ClaimRanks userRank ) {
        return userRank.power >= this.power;
    }
    public Formatting[] getColor() {
        return this.color;
    }
}
