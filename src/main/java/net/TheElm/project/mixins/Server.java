package net.TheElm.project.mixins;

import net.TheElm.project.CoreMod;
import net.TheElm.project.protections.claiming.Claimant;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTask;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.util.NonBlockingThreadExecutor;
import net.minecraft.util.snooper.SnooperListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public abstract class Server extends NonBlockingThreadExecutor<ServerTask> implements SnooperListener, CommandOutput, AutoCloseable, Runnable {
    
    public Server(String string_1) {
        super(string_1);
    }
    
    @Inject(at = @At("RETURN"), method = "save")
    public void save(boolean silent, boolean boolean_2, boolean boolean_3, CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValue()) {
            if (!silent) CoreMod.logInfo("Saving claimed player data");
            CoreMod.getCacheStream( Claimant.ClaimantType.PLAYER ).forEach(Claimant::save);
            
            if (!silent) CoreMod.logInfo("Saving claimed town data");
            CoreMod.getCacheStream( Claimant.ClaimantType.TOWN ).forEach(Claimant::save);
        }
    }
    
}
