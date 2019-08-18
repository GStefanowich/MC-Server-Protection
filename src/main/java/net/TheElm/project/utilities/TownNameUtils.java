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

import net.TheElm.project.CoreMod;
import net.TheElm.project.MySQL.MySQLStatement;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public final class TownNameUtils {
    
    private TownNameUtils() {}
    
    public static Pair<Text, UUID> fetchTownInfo(@NotNull UUID uuid) {
        Text name = null;
        UUID creator = null;
        try ( MySQLStatement statement = CoreMod.getSQL().prepare("SELECT `townName`, `townOwner` FROM `chunk_Towns` WHERE `townId` = ?;", false).addPrepared( uuid ) ) {
            ResultSet rs = statement.executeStatement();
            
            while ( rs.next() ) {
                name = new LiteralText( rs.getString( "townName" ) );
                creator = UUID.fromString( rs.getString( "townOwner" ) );
            }
            
        } catch (SQLException e) {
            CoreMod.logError( e );
        }
        
        return new Pair<>( name, creator );
    }
    public static String getTownName(final int chunkSize, final int residents) {
        // Small claims
        if ( chunkSize <= 250 ) {
            if ( residents >= 28 )
                return "metropolis";
            if (residents >= 24)
                return "large city";
            if (residents >= 20)
                return "city";
            if (residents >= 14)
                return "large town";
            if (residents >= 10)
                return "town";
            if ( residents >= 5 )
                return "village";
            if ( residents >= 2 )
                return "hamlet";
            return "settlement";
        }
        // Larger claims
        if ( residents >= 60 )
            return "realm";
        if ( residents >= 40 )
            return "empire";
        if ( residents >= 30 )
            return "kingdom";
        if ( residents >= 5 )
            return "nation";
        return "hinterland";
    }
    public static String getOwnerTitle(final int chunkSize, final int residents, final boolean male) {
        // Small claims
        if ( chunkSize <= 250 ) {
            if ( residents >= 28 )
                return male ? "lord" : "lady";
            if (residents >= 24)
                return male ? "duke" : "duchess";
            if (residents >= 20)
                return male ? "earl" : "countess";
            if (residents >= 14)
                return male ? "count" : "countess";
            if (residents >= 10)
                return male ? "viscount" : "viscountess";
            if ( residents >= 5 )
                return male ? "baron" : "baroness";
            if ( residents >= 2 )
                return "constable";
            return "burgess";
        }
        // Larger claims
        if ( residents >= 80 )
            return "the";
        if ( residents >= 60 )
            return male ? "god emperor" : "god empress";
        if ( residents >= 40 )
            return male ? "emperor" : "empress";
        if ( residents >= 30 )
            return male ? "king" : "queen";
        if ( residents >= 5 )
            return "leader";
        return "recluse";
    }
    
}
