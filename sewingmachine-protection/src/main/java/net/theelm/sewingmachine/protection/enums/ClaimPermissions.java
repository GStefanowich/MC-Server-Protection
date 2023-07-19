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

import net.theelm.sewingmachine.utilities.CasingUtils;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

public enum ClaimPermissions {
    BEDS(ClaimRanks.PASSIVE, "Sleep and set spawn at beds"), // Bed usage
    CREATURES(ClaimRanks.PASSIVE, "Harm or kill passive mobs"), // Harm passive mods
    HARVEST(ClaimRanks.ALLY, "Harvest crops that are fully grown"), // Harvest plants
    BLOCKS(ClaimRanks.ALLY, "Place or break any block"), // Break or Place blocks
    STORAGE(ClaimRanks.ALLY, "Access shared inventories: chests, hoppers, furnaces, etc"), // Access storage containers
    DOORS(ClaimRanks.PASSIVE, "Open or close doors"), // Open doors and gates
    PICKUP(ClaimRanks.ALLY, "Pickup dropped items"), // Pickup entities
    REDSTONE(ClaimRanks.PASSIVE, "Operate buttons and levers"), // Trigger redstone
    RIDING(ClaimRanks.ALLY, "Ride on horses, llamas, boats, and minecarts"), // Ride carts and animals
    WARP(ClaimRanks.OWNER, "Teleport to waystones"), // Warp to the players warp location
    TRADING(ClaimRanks.PASSIVE, "Make trades with villagers"), // Trade with villagers
    CRAFTING(ClaimRanks.PASSIVE, "Access non-shared inventories: crafting tables, looms, etc") // Open crafting benches
    ;
    
    private final @NotNull ClaimRanks defaultRank;
    private final @NotNull Text description;
    
    ClaimPermissions(@NotNull ClaimRanks defaultRank, @NotNull String description) {
        this.defaultRank = defaultRank;
        this.description = Text.literal(description)
            .append(" (Default: ")
            .append(Text.literal(CasingUtils.sentence(this.defaultRank.name()))
                .styled(style -> style.withColor(this.defaultRank.getColor())))
            .append(")");
    }
    
    public @NotNull String getTranslationKey() {
        return "claim.permissions." + this.name().toLowerCase();
    }
    
    public @NotNull Text getDescription() {
        return this.description;
    }
    public @NotNull ClaimRanks getDefault() {
        return this.defaultRank;
    }
}
