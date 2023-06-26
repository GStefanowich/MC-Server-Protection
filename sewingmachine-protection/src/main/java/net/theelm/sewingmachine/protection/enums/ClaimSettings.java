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

import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.base.config.SewCoreConfig;
import net.theelm.sewingmachine.config.ConfigOption;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.interfaces.BoolEnums;
import net.theelm.sewingmachine.protection.interfaces.Claim;
import net.theelm.sewingmachine.interfaces.IClaimedChunk;
import net.theelm.sewingmachine.protection.interfaces.IClaimedChunk;
import net.theelm.sewingmachine.protection.claims.ClaimantPlayer;
import net.theelm.sewingmachine.protection.claims.ClaimantTown;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public enum ClaimSettings implements BoolEnums {
    // "False positives"
    ENDERMAN_GRIEFING(SewCoreConfig.CLAIM_ALLOW_GRIEFING_ENDERMAN, "Allow Endermen to pickup blocks", false, true, false),
    WEATHER_GRIEFING(SewCoreConfig.CLAIM_ALLOW_GRIEFING_WEATHER, "Allow weather to damage blocks", false, true, false),
    CREEPER_GRIEFING(SewCoreConfig.CLAIM_ALLOW_GRIEFING_CREEPER, "Allow Creepers to destroy blocks", false, true, false),
    GHAST_GRIEFING(SewCoreConfig.CLAIM_ALLOW_GRIEFING_GHAST, "Allow Ghasts to destroy blocks", false, true, false),
    PLAYER_COMBAT(SewCoreConfig.CLAIM_ALLOW_PLAYER_COMBAT, "Allow Player vs Player", false, true, false) {
        @Override
        public boolean hasSettingSet(@NotNull Claim claimant) {
            return this.hasSettingSet(claimant, true);
        }
    },
    FIRE_SPREAD(SewCoreConfig.CLAIM_ALLOW_FIRE_SPREAD, "Allow fire to spread and destroy blocks", false, true, false),
    HURT_TAMED("Allow Players to harm Wolves, Cats and other tamed pets", false, false, false),
    
    // "True positives"
    CROP_AUTOREPLANT(SewCoreConfig.CLAIM_ALLOW_CROP_AUTOREPLANT, "Automatically replant harvested crops", true, false, true);
    
    protected final @NotNull Text description;
    protected final @Nullable ConfigOption<Boolean> configOption;
    protected final boolean valueShouldBe;
    private final boolean playrDef;
    private final boolean spawnDef;
    
    ClaimSettings(@NotNull String description, boolean valueShouldBe, boolean playerDefault, boolean spawnDefault) {
        this(null, description, valueShouldBe, playerDefault, spawnDefault);
    }
    ClaimSettings(@NotNull ConfigOption<Boolean> config, @NotNull String description, boolean valueShouldBe, boolean playerDefault, boolean spawnDefault) {
        this.valueShouldBe = valueShouldBe;
        this.configOption = config;
        this.playrDef = playerDefault;
        this.spawnDef = spawnDefault;
        this.description = Text.literal(description)
            .append(" (Default: ")
            .append(Text.literal(Boolean.toString(this.playrDef))
                .formatted(this.getAttributeColor(this.playrDef)))
            .append(")");
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
        if (CoreMod.SPAWN_ID.equals( owner ))
            return this.getSpawnDefault();
        return this.getPlayerDefault();
    }
    public @NotNull Text getDescription() {
        return this.description;
    }
    
    @Override
    public boolean isEnabled() {
        return this.configOption == null || SewConfig.get(this.configOption);
    }
    
    public boolean hasSettingSet(@NotNull Claim claimant) {
        return this.hasSettingSet(claimant, false);
    }
    public final boolean hasSettingSet(@NotNull Claim claimant, boolean prioritizeTown) {
        ClaimantPlayer player = claimant.getOwner();
        
        // This Setting should be overriden in Towns by the town (If the owner isn't SPAWN (Takes priority)).
        if (player != null) {
            boolean playerSetting = player.getProtectedChunkSetting(this);
            
            // If the setting is allowed to be overridden by the Town
            if (prioritizeTown) {
                if (playerSetting == this.valueShouldBe)
                    return playerSetting;
                
                if (!player.isSpawn() && claimant instanceof IClaimedChunk chunk) {
                    // Get the town
                    ClaimantTown town = chunk.getTown();
                    
                    if (town != null) {
                        // Get the settings of the towns owner
                        ClaimantPlayer townOwner = town.getOwner();
                        
                        // Return the town owners settings
                        if (townOwner != null)
                            return townOwner.getProtectedChunkSetting(this);
                    }
                }
            }
            
            return playerSetting;
        }
        
        // Fallback to the default
        return this.getDefault(null);
    }
}
