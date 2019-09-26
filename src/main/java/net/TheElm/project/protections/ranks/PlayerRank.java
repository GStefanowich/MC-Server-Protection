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

package net.TheElm.project.protections.ranks;

import net.TheElm.project.utilities.FormattingUtils;
import net.TheElm.project.utilities.RankUtils;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;

public final class PlayerRank {
    
    private final HashSet<String> nodes = new HashSet<>();
    private String parent;
    
    private final String iden;
    private final Text name;
    
    public PlayerRank(@NotNull String iden, @NotNull String name) {
        this.iden = iden;
        this.name = FormattingUtils.stringToText( name );
        this.parent = ( "*".equals(iden) ? null : "*" );
    }
    
    /*
     * Display
     */
    @NotNull
    public String getName() {
        return this.getDisplay().asString();
    }
    public Text getDisplay() {
        return this.name.deepCopy();
    }
    
    /*
     * Relativity
     */
    public void setParent(@NotNull String parent) {
        if (parent.equals(this.iden))
            throw new IllegalArgumentException("Rank \"" + iden + "\" cannot inherit from itself");
        this.parent = parent;
    }
    
    /*
     * Permissions
     */
    private boolean hasNode(String node) {
        boolean contains = this.nodes.contains(node);
        PlayerRank parent;
        if ((!contains) && ((parent = RankUtils.getRank( this.parent )) != null))
            return parent.hasNode(node);
        return contains;
    }
    public boolean isAdditive(String node) {
        return this.hasNode("+*") || this.hasNode("+" + node);
    }
    public boolean isSubtractive(String node) {
        return this.hasNode("-*") || this.hasNode("-" + node);
    }
}
