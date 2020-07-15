package net.TheElm.project.permissions;

import org.jetbrains.annotations.NotNull;

public class PermissionNode {
    
    private final String node;
    private final String description;
    
    public PermissionNode(@NotNull String node, @NotNull String description) {
        this.node = node;
        this.description = description;
    }
    
    public String getNode() {
        return this.node;
    }
    
    public String getDescription() {
        return this.description;
    }
    
}
