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

package net.TheElm.project.mixins.Server;

import net.TheElm.project.CoreMod;
import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.utilities.FormattingUtils;
import net.TheElm.project.utilities.SleepUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerMetadata;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@Mixin(ServerMetadata.class)
public abstract class MOTD {
    
    @Final
    private Map<String, Callable<String>> motdVariables = new HashMap<>();
    
    @Shadow private ServerMetadata.Players players;
    @Shadow private ServerMetadata.Version version;
    
    @Inject(at = @At("RETURN"), method = "<init> *")
    public void onConstruct(CallbackInfo callback) {
        // Version
        this.motdVariables.put( "version", () -> {
            if (this.version == null)
                return "??.??.??";
            return this.version.getGameVersion();
        });
        // Time
        this.motdVariables.put( "time", () -> {
            MinecraftServer server = CoreMod.getServer();
            ServerWorld world = server.getWorld(DimensionType.OVERWORLD);
            if (world == null) return "Unknown";
            return SleepUtils.timeFromMillis(world.getTimeOfDay());
        });
        // Difficulty
        this.motdVariables.put( "difficulty", () -> {
            MinecraftServer server = CoreMod.getServer();
            return server.getDefaultDifficulty().getName();
        });
    }
    
    @Inject(at = @At("TAIL"), method = "getDescription", cancellable = true)
    public void onGetDescription(CallbackInfoReturnable<Text> callback) {
        // Get MOTDs and if empty, cancel
        List<String> configMOTD = SewingMachineConfig.INSTANCE.SERVER_MOTD_LIST.get();
        if (configMOTD.size() <= 0) return;
        
        // Get the formatted MOTD
        String raw = descriptionReplaceVariables(SewingMachineConfig.INSTANCE.SERVER_MOTD_LIST.getRandom());
        if (raw != null) {
            Text motd = FormattingUtils.stringToText(raw);
            if (motd != null)
                callback.setReturnValue(motd);
        }
    }
    
    private String descriptionReplaceVariables(String description) {
        if (description != null) {
            // For all keys
            for (Map.Entry<String, Callable<String>> row : this.motdVariables.entrySet()) {
                // If description contains
                String key = "${" + row.getKey() + "}";
                if (!description.contains(key))
                    continue;
                
                String val;
                try {
                    val = row.getValue().call();
                    if (val == null) continue;
                } catch (Exception e) { CoreMod.logError(new Exception("Error in MOTD variable \"" + row.getKey() + "\"", e)); return null; }
                // Replace
                description = description.replace(key, val);
            }
        }
        return description;
    }
    
}
