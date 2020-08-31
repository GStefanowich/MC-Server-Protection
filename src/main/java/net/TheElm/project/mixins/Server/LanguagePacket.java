package net.TheElm.project.mixins.Server;

import net.TheElm.project.interfaces.LanguageEntity;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.c2s.play.ClientSettingsC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Locale;

@Mixin(ClientSettingsC2SPacket.class)
public abstract class LanguagePacket implements Packet<ServerPlayPacketListener>, LanguageEntity {
    
    @Shadow private String language;
    
    @Override
    public Locale getClientLanguage() {
        return Locale.forLanguageTag(this.language);
    }
    
}
