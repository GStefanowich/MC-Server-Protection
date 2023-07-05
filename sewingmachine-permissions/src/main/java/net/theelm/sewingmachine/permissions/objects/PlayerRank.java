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

package net.theelm.sewingmachine.permissions.objects;

import net.theelm.sewingmachine.enums.PermissionNodes;
import net.theelm.sewingmachine.permissions.utilities.RankUtils;
import net.theelm.sewingmachine.utilities.FormattingUtils;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

@Deprecated(forRemoval = true)
public final class PlayerRank implements Comparable<PlayerRank> {
    
    private final @NotNull SortedSet<String> nodes = new TreeSet<>();
    private @Nullable String parent;
    
    private final @NotNull String iden;
    private final @Nullable Text display;
    
    public PlayerRank(@NotNull String iden, @Nullable String display) {
        this.iden = iden;
        this.display = (display == null ? null : FormattingUtils.stringToText(display));
        this.parent = ( "*".equals(iden) ? null : "*" );
    }
    
    /*
     * Display
     */
    public @NotNull String getIdentifier() {
        return this.iden;
    }
    public @Nullable Text getDisplay() {
        return this.display == null ? null : FormattingUtils.deepCopy(this.display);
    }
    
    /*
     * Relativity
     */
    public void setParent(@NotNull String parent) {
        if (parent.equals(this.iden))
            throw new IllegalArgumentException("Rank \"" + this.iden + "\" cannot inherit from itself");
        this.parent = parent;
    }
    public @Nullable String getParent() {
        return this.parent;
    }
    public @Nullable PlayerRank getParentReference() {
        if (!Objects.equals(this.parent, this.iden)) {
            PlayerRank rank = RankUtils.getRank(this.parent);
            if (rank != this)
                return rank;
        }
        return null;
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
        if ((!contains) && ((parent = this.getParentReference()) != null))
            return parent.hasNode(node);
        return contains;
    }
    public boolean isAdditive(String node) {
        return this.hasNode("+" + PermissionNodes.ALL_PERMISSIONS) || this.hasNode("+" + node);
    }
    public boolean isSubtractive(String node) {
        return this.hasNode("-" + PermissionNodes.ALL_PERMISSIONS) || this.hasNode("-" + node);
    }
    
    /*
     * Overrides
     */
    @Override
    public @NotNull String toString() {
        return this.getIdentifier();
    }
    @Override
    public int compareTo(@NotNull PlayerRank other) {
        if ("*".equals(this.getIdentifier())) return 1;
        if (("*".equals(other.getIdentifier())) || (this.parent.equals(other.getIdentifier()))) return -1;
        return 0;
    }
}
