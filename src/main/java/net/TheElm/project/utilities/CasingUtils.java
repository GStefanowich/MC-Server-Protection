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

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class CasingUtils {
    public static @NotNull String words(@NotNull String modify ) {
        String[] split = CasingUtils.lower( modify ).split(" ");
        List<String> out = new ArrayList<>();
        for ( String seg : split ) {
            out.add( CasingUtils.sentence( seg ) );
        }
        return String.join( " ", out );
    }
    
    public static @NotNull String sentence(@NotNull String modify ) {
        if ( "".equals(modify) )
            return modify;
        String out = CasingUtils.lower(modify);
        return out.substring( 0, 1 ).toUpperCase() + out.substring( 1 );
    }
    
    public static @NotNull String upper(@NotNull String modify ) {
        return modify.toLowerCase();
    }
    
    public static @NotNull String lower(@NotNull String modify ) {
        return modify.toLowerCase();
    }
    
    public static @NotNull String acronym(@NotNull String modify ) {
        return CasingUtils.acronym(modify, false);
    }
    public static @NotNull String acronym(@NotNull String modify, boolean dots ) {
        StringBuilder acronym = new StringBuilder();
        
        for (String split : modify.split(" ")) {
            if (split.length() <= 0)
                continue;
            char first = split.charAt(0);
            acronym.append(first).append(dots && Character.isLetter(first) ? "." : "");
        }
        
        return acronym.toString();
    }
    
}
