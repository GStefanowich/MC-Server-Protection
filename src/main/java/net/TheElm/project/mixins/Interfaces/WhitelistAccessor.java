package net.TheElm.project.mixins.Interfaces;

import net.minecraft.server.ServerConfigEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerConfigEntry.class)
public interface WhitelistAccessor<T> {
    @Accessor("key")
    T getObject();
}
