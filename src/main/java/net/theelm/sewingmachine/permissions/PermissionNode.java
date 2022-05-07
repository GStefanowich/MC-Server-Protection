package net.theelm.sewingmachine.permissions;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class PermissionNode {
    
    private final @NotNull String node;
    private final @NotNull String description;
    
    public PermissionNode(@NotNull String node, @NotNull String description) {
        this.node = node;
        this.description = description;
    }
    
    public @NotNull String getNode() {
        return this.node;
    }
    public @NotNull String getDescription() {
        return this.description;
    }
    
    public boolean isWildcard() {
        return this.node.equals("*");
    }
    
    @Override
    public int hashCode() {
        return this.node.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PermissionNode other)
            return Objects.equals(this.node, other.getNode());
        return false;
    }
    
    @Override
    public String toString() {
        return this.node;
    }
}
