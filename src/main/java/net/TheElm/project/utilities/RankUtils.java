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

package net.TheElm.project.utilities;

import com.mojang.authlib.GameProfile;
import net.TheElm.project.protections.ranks.PlayerRank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public final class RankUtils {
    
    public static HashMap<String, PlayerRank> RANKS = new HashMap<>();
    
    private RankUtils() {}
    
    /*
     * Stored ranks
     */
    @Nullable
    public static PlayerRank getRank(@Nullable String identifier) {
        if (identifier == null) return null;
        return RankUtils.RANKS.get( identifier );
    }
    
    /*
     * Get player ranks
     */
    @Nullable
    public static PlayerRank getHighestRank(@NotNull GameProfile profile) {
        PlayerRank[] ranks = RankUtils.getRanks(profile);
        if (ranks.length > 0)
            return ranks[0];
        return null;
    }
    public static PlayerRank[] getRanks(@NotNull GameProfile profile) {
        return new PlayerRank[]{
            new PlayerRank( "*", "&cMod" )
        };
    }
    public static boolean hasPermission(@NotNull GameProfile profile, String permission) {
        boolean allowed = false;
        
        PlayerRank[] ranks = RankUtils.getRanks( profile );
        for (int i = (ranks.length - 1); i >= 0; --i) {
            PlayerRank rank = ranks[i];
            
            if (rank.isAdditive( permission ))
                allowed = true;
            else if (rank.isSubtractive( permission ))
                allowed = false;
        }
        
        return allowed;
    }
    
}
