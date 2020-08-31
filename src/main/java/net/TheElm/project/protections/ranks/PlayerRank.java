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

import net.TheElm.project.enums.Permissions;
import net.TheElm.project.utilities.FormattingUtils;
import net.TheElm.project.utilities.RankUtils;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.SortedSet;
import java.util.TreeSet;

public final class PlayerRank implements Comparable<PlayerRank> {
    
    private final SortedSet<String> nodes = new TreeSet<>();
    private String parent;
    
    private final String iden;
    private final Text display;
    
    public PlayerRank(@NotNull String iden, @Nullable String display) {
        this.iden = iden;
        this.display = (display == null ? null : FormattingUtils.stringToText( display ));
        this.parent = ( "*".equals(iden) ? null : "*" );
    }
    
    /*
     * Display
     */
    public @NotNull String getIdentifier() {
        return this.iden;
    }
    public @Nullable Text getDisplay() {
        return this.display == null ? null : this.display.copy();
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
    public boolean addNode(String node) {
        return this.nodes.add(node);
    }
    private boolean hasNode(String node) {
        boolean contains = this.nodes.contains(node);
        PlayerRank parent;
        if ((!contains) && ((parent = RankUtils.getRank( this.parent )) != null))
            return parent.hasNode(node);
        return contains;
    }
    public boolean isAdditive(String node) {
        return this.hasNode("+" + Permissions.ALL_PERMISSIONS) || this.hasNode("+" + node);
    }
    public boolean isSubtractive(String node) {
        return this.hasNode("-" + Permissions.ALL_PERMISSIONS) || this.hasNode("-" + node);
    }
    
    /*
     * Overrides
     */
    @Override
    public String toString() {
        return this.getIdentifier();
    }
    @Override
    public int compareTo(@NotNull PlayerRank other) {
        if ("*".equals(this.getIdentifier())) return 1;
        if (("*".equals(other.getIdentifier())) || (this.parent.equals(other.getIdentifier()))) return -1;
        return 0;
    }
}
