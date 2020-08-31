package net.TheElm.project.mixins.World;

import net.TheElm.project.utilities.ChunkUtils;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.raid.Raid;
import net.minecraft.village.raid.RaidManager;
import net.minecraft.world.PersistentState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RaidManager.class)
public abstract class RaidFight extends PersistentState {
    
    public RaidFight(String string) {
        super(string);
    }
    
    @Inject(at = @At("HEAD"), method = "startRaid", cancellable = true)
    public void onStartRaid(ServerPlayerEntity player, CallbackInfoReturnable<Raid> callback) {
        // Get the players position
        BlockPos pos = player.getBlockPos();
        // Test if the player has permission to interact with the village
        if (!(ChunkUtils.canPlayerInteractFriendlies(player, pos)
            && ChunkUtils.canPlayerBreakInChunk(player, pos)
            && ChunkUtils.canPlayerLootChestsInChunk(player, pos))
        ) callback.setReturnValue(null);
    }
}
