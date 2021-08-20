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
import net.TheElm.project.config.ConfigOption;
import net.TheElm.project.config.SewConfig;
import net.TheElm.project.interfaces.BoolEnums;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public enum ClaimSettings implements BoolEnums {
    // "False positives"
    ENDERMAN_GRIEFING(SewConfig.CLAIM_ALLOW_GRIEFING_ENDERMAN, "Allow Endermen to pickup blocks", false, true, false),
    WEATHER_GRIEFING(SewConfig.CLAIM_ALLOW_GRIEFING_WEATHER, "Allow weather to damage blocks", false, true, false),
    CREEPER_GRIEFING(SewConfig.CLAIM_ALLOW_GRIEFING_CREEPER, "Allow Creepers to destroy blocks", false, true, false),
    GHAST_GRIEFING(SewConfig.CLAIM_ALLOW_GRIEFING_GHAST, "Allow Ghasts to destroy blocks", false, true, false),
    PLAYER_COMBAT(SewConfig.CLAIM_ALLOW_PLAYER_COMBAT, "Allow Player vs Player", false, true, false),
    FIRE_SPREAD(SewConfig.CLAIM_ALLOW_FIRE_SPREAD, "Allow fire to spread and destroy blocks", false, true, false),
    HURT_TAMED("Allow Players to harm Wolves, Cats and other tamed pets", false, false, false),
    
    // "True positives"
    CROP_AUTOREPLANT(SewConfig.CLAIM_ALLOW_CROP_AUTOREPLANT, "Automatically replant harvested crops", true, false, true),
    TREE_CAPACITATE(SewConfig.CLAIM_ALLOW_TREE_CAPACITATOR, "Automatically knock down entire trees", true, false, false),
    VEIN_MINER(SewConfig.CLAIM_ALLOW_VEIN_MINER, "Automatically mine entire ore veins", true, false, false);
    
    private final @NotNull Text description;
    private final @Nullable ConfigOption<Boolean> configOption;
    private final boolean valueShouldBe;
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
        this.description = new LiteralText(description)
            .append(" (Default: ")
            .append(new LiteralText(Boolean.toString(this.playrDef))
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
}
