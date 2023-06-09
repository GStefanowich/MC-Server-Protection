/*
 * This software is licensed under the MIT License
 * https://github.com/GStefanowich/MC-Server-Protection
 *
 * Copyright (c) 2019 Gregory Stefanowich
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.theelm.sewingmachine.base.mixins.Server;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.theelm.sewingmachine.interfaces.WhitelistedPlayer;
import net.theelm.sewingmachine.base.mixins.Interfaces.WhitelistAccessor;
import net.minecraft.server.ServerConfigEntry;
import net.minecraft.server.WhitelistEntry;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(WhitelistEntry.class)
public abstract class WhitelistEntryMixin extends ServerConfigEntry<GameProfile> implements WhitelistedPlayer, WhitelistAccessor<GameProfile> {
    //private UUID id;
    private UUID invitedBy = null;
    
    public WhitelistEntryMixin(GameProfile object) {
        super(object);
    }
    
    @Inject(at = @At("RETURN"), method = "<init>(Lcom/google/gson/JsonObject;)V")
    public void onInitialize(JsonObject json, CallbackInfo callback) {
        if (json.has("invitedBy"))
            this.invitedBy = UUID.fromString(json.get("invitedBy").getAsString());
    }
    
    @Inject(at = @At("TAIL"), method = "write")
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
