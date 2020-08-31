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

import net.TheElm.project.CoreMod;
import net.TheElm.project.config.SewConfig;
import net.TheElm.project.interfaces.BoolEnums;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public enum ClaimSettings implements BoolEnums {
    // "False positives"
    ENDERMAN_GRIEFING( false, true, false ) {
        @Override
        public boolean isEnabled() {
            return SewConfig.get(SewConfig.CLAIM_ALLOW_GRIEFING_ENDERMAN);
        }
    },
    CREEPER_GRIEFING( false, true, false ) {
        @Override
        public boolean isEnabled() {
            return SewConfig.get(SewConfig.CLAIM_ALLOW_GRIEFING_CREEPER);
        }
    },
    GHAST_GRIEFING( false, true, false ) {
        @Override
        public boolean isEnabled() {
            return SewConfig.get(SewConfig.CLAIM_ALLOW_GRIEFING_GHAST);
        }
    },
    PLAYER_COMBAT( false, true, false ) {
        @Override
        public boolean isEnabled() {
            return SewConfig.get(SewConfig.CLAIM_ALLOW_PLAYER_COMBAT);
        }
    },
    HURT_TAMED( false, false, false ) {
        @Override
        public boolean isEnabled() {
            return true;
        }
    },
    
    // "True positives"
    CROP_AUTOREPLANT( true, false, true ) {
        @Override
        public boolean isEnabled() {
            return SewConfig.get(SewConfig.CLAIM_ALLOW_CROP_AUTOREPLANT);
        }
    },
    TREE_CAPACITATE(true, false, false ) {
        @Override
        public boolean isEnabled() {
            return SewConfig.get(SewConfig.CLAIM_ALLOW_TREE_CAPACITATOR);
        }
    },
    VEIN_MINER( true, false, false ) {
        @Override
        public boolean isEnabled() {
            return SewConfig.get(SewConfig.CLAIM_ALLOW_VEIN_MINER);
        }
    };
    
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
    private boolean getPlayerDefault() {
        return this.playrDef;
    }
    private boolean getSpawnDefault() {
        return this.spawnDef;
    }
    public boolean getDefault(@Nullable UUID owner) {
        if (CoreMod.spawnID.equals( owner ))
            return this.getSpawnDefault();
        return this.getPlayerDefault();
    }
    
}
