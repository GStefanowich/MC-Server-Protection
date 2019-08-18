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

import net.minecraft.util.Formatting;

public enum ClaimSettings {
    // "False positives"
    ENDERMAN_GRIEFING( false, true, false ),
    CREEPER_GRIEFING( false, true, false ),
    PLAYER_COMBAT( false, true, false ),
    HURT_TAMED( false, false, false ),
    
    // "True positives"
    CROP_AUTOREPLANT( true, false, true );
    
    private final boolean valueShouldBe;
    private final boolean playrDef;
    private final boolean spawnDef;
    
    ClaimSettings( boolean valueShouldBe, boolean playerDefault, boolean spawnDefault ) {
        this.valueShouldBe = valueShouldBe;
        this.playrDef = playerDefault;
        this.spawnDef = spawnDefault;
    }
    
    public boolean isTruePositive() {
        return this.valueShouldBe;
    }
    public Formatting getAttributeColor( boolean status ) {
        if (this.isTruePositive()) {
            if ( status ) return Formatting.GREEN;
            return Formatting.RED;
        } else {
            if ( status ) return Formatting.RED;
            return Formatting.GREEN;
        }
    }
    public boolean getPlayerDefault() {
        return this.playrDef;
    }
    public boolean getSpawnDefault() {
        return this.spawnDef;
    }
    
}
