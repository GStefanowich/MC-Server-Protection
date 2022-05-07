package net.theelm.sewingmachine.interfaces;

import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public interface BossLootableContainer {
    
    @Nullable Identifier getBossLootIdentifier();
    
}
