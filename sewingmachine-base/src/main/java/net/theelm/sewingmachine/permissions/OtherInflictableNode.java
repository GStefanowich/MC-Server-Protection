package net.theelm.sewingmachine.permissions;

import org.jetbrains.annotations.NotNull;

public class OtherInflictableNode extends InflictableNode {
    
    private final @NotNull InflictableNode onOther;
    
    public OtherInflictableNode(@NotNull String node, @NotNull String thisDescription, @NotNull String otherDescription) {
        super(node, thisDescription);
        
        this.onOther = new InflictableNode(node + ".other", otherDescription);
    }
    
    public @NotNull InflictableNode onOther() {
        return this.onOther;
    }
    
}
