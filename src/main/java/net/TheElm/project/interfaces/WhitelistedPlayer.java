package net.TheElm.project.interfaces;

import java.util.UUID;

public interface WhitelistedPlayer {
    void setInvitedBy(UUID uuid);
    UUID getInvitedBy();
    
    UUID getUUID();
    String getName();
}
