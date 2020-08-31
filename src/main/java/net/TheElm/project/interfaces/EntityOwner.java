package net.TheElm.project.interfaces;

import net.minecraft.entity.Entity;
import org.jetbrains.annotations.Nullable;

public interface EntityOwner {
    @Nullable
    Entity getOwner();
}
