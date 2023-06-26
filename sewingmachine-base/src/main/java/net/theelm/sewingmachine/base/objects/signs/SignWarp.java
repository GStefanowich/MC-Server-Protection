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

package net.theelm.sewingmachine.base.objects.signs;

import com.mojang.datafixers.util.Either;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.base.config.SewCoreConfig;
import net.theelm.sewingmachine.base.objects.ShopSign;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.interfaces.LogicalWorld;
import net.theelm.sewingmachine.interfaces.ShopSignData;
import net.theelm.sewingmachine.objects.ticking.WaystoneSearch;
import net.theelm.sewingmachine.utilities.ShopSignBuilder;
import net.theelm.sewingmachine.utilities.WarpUtils;
import org.jetbrains.annotations.NotNull;

/*
 * Teleport the player to a random location around the map
 */
public final class SignWarp extends ShopSign {
    public SignWarp() {
        super("WARP", Formatting.DARK_PURPLE);
    }

    @Override
    public boolean formatSign(@NotNull final ShopSignBuilder sign, @NotNull final ServerPlayerEntity creator) {
        // Break if not in creative mode
        if (!creator.isCreative())
            return false;
        
        // Set the sign owner to SPAWN
        sign.setShopOwner(CoreMod.SPAWN_ID);
        
        return this.renderSign(sign);
    }
    
    @Override
    public boolean renderSign(@NotNull ShopSignData shop) {
        Text[] text = new Text[] {
            Text.literal("Teleport to ").formatted(Formatting.DARK_GRAY)
                .append(Text.literal("Biome").formatted(Formatting.OBFUSCATED, Formatting.RED))
        };
        
        return shop.setSign(text);
    }
    
    @Override
    public Either<Text, Boolean> onInteract(@NotNull MinecraftServer server, @NotNull final ServerPlayerEntity player, @NotNull final BlockPos signPos, final ShopSignData sign) {
        WarpUtils.Warp warp = WarpUtils.getWarp(player.getUuid(), null);
        if ( warp == null ) {
            // Create new warp
            if ( WarpUtils.isPlayerCreating( player ) )
                return Either.right(Boolean.FALSE);
            // Make a new warp
            return Either.right(this.generateNewWarp(player));
        } else {
            // Warp the player to their home
            WarpUtils.teleportEntityAndAttached(player, warp);
        }
        return Either.right(Boolean.TRUE);
    }
    
    @Override
    public boolean isEnabled() {
        return SewConfig.get(SewCoreConfig.WARP_MAX_DISTANCE) > 0;
    }
    
    private boolean generateNewWarp(@NotNull final ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        RegistryKey<World> registryKey = SewConfig.get(SewCoreConfig.WARP_DIMENSION);
        
        ServerWorld world = server.getWorld(registryKey);
        if (world == null)
            world = player.getServerWorld();
        
        ((LogicalWorld)world).addTickableEvent(new WaystoneSearch(
            world,
            player
        ));
        return true;
    }
}
