package net.TheElm.project.permissions;

import org.jetbrains.annotations.NotNull;

public class OtherInflictableNode extends InflictableNode {
    
    private final InflictableNode onOther;
    
    public OtherInflictableNode(@NotNull String node, @NotNull String description) {
        super(node, description);
        
        this.onOther = new InflictableNode(node + ".other", description);
    }
    
    public InflictableNode onOther() {
        return this.onOther;
    }
    
}
