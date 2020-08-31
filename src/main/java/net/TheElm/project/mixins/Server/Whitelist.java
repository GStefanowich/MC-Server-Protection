package net.TheElm.project.mixins.Server;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.TheElm.project.interfaces.WhitelistedPlayer;
import net.TheElm.project.mixins.Interfaces.WhitelistAccessor;
import net.minecraft.server.ServerConfigEntry;
import net.minecraft.server.WhitelistEntry;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(WhitelistEntry.class)
public abstract class Whitelist extends ServerConfigEntry<GameProfile> implements WhitelistedPlayer, WhitelistAccessor<GameProfile> {
    //private UUID id;
    private UUID invitedBy = null;
    
    public Whitelist(GameProfile object) {
        super(object);
    }
    
    @Inject(at = @At("RETURN"), method = "<init>(Lcom/google/gson/JsonObject;)V")
    public void onInitialize(JsonObject json, CallbackInfo callback) {
        if (json.has("invitedBy"))
            this.invitedBy = UUID.fromString(json.get("invitedBy").getAsString());
    }
    
    @Inject(at = @At("TAIL"), method = "fromJson")
    public void onSerialize(JsonObject json, CallbackInfo callback) {
        if (invitedBy != null)
            json.addProperty("invitedBy",this.invitedBy.toString());
    }
    
    @Override
    public void setInvitedBy(UUID uuid) {
        this.invitedBy = uuid;
    }
    
    @Override
    public @Nullable String getName() {
        GameProfile profile = this.getObject();
        if (profile == null) return null;
        return profile.getName();
    }
    
    @Override
    public @Nullable UUID getUUID() {
        GameProfile profile = this.getObject();
        if (profile == null) return null;
        return profile.getId();
    }
    
    @Override
    public @Nullable UUID getInvitedBy() {
        return this.invitedBy;
    }
}
