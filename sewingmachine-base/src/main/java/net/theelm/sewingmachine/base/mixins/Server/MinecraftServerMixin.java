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

import com.mojang.authlib.GameProfile;
import net.minecraft.util.math.random.Random;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.base.ServerCore;
import net.theelm.sewingmachine.base.config.SewCoreConfig;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.events.PlayerNameCallback;
import net.theelm.sewingmachine.interfaces.MotdFunction;
import net.theelm.sewingmachine.utilities.CasingUtils;
import net.theelm.sewingmachine.utilities.FormattingUtils;
import net.theelm.sewingmachine.utilities.ServerVariables;
import net.theelm.sewingmachine.utilities.text.TextUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerMetadata;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Shadow public abstract boolean shouldEnforceSecureProfile();
    @Shadow protected abstract ServerMetadata.Players createMetadataPlayers();
    @Shadow @Final private Random random;

    @Shadow @Nullable private String motd;
    private final List<byte[]> base64 = new ArrayList<>();
    private final List<String> motds = new ArrayList<>();
    
    @Inject(at = @At("RETURN"), method = "<init> *")
    public void onConstruct(@NotNull CallbackInfo callback) {
        /*
         * Process the randomized server icons
         */
        for( String iconName : SewConfig.get(SewCoreConfig.SERVER_ICON_LIST) ) {
            File iconFile = new File(".", iconName + ".png");
            if (iconFile.isFile()) {
                try {
                    BufferedImage icoBufferedImage = ImageIO.read(iconFile);
                    
                    // Validate the icon size
                    Validate.validState(icoBufferedImage.getWidth() == 64, "Must be 64 pixels wide");
                    Validate.validState(icoBufferedImage.getHeight() == 64, "Must be 64 pixels high");
                    
                    // Write the read buffer to the stream
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    ImageIO.write(icoBufferedImage, "PNG", stream);
                    
                    this.base64.add(stream.toByteArray());
                } catch (Exception e) {
                    CoreMod.logError("Couldn't load server icon", e);
                }
            }
        }
        
        // Get all of the MOTDs and shuffle
        SewConfig.afterReload(this::refreshMotd);
        this.refreshMotd();
    }
    
    @Inject(at = @At("HEAD"), method = "createMetadata", cancellable = true)
    public void onCreateMetadata(CallbackInfoReturnable<ServerMetadata> callback) {
        ServerMetadata metadata = new ServerMetadata(
            this.getCustomMotd(),
            Optional.of(this.createMetadataPlayers()), // Players
            Optional.of(ServerMetadata.Version.create()), // Version
            Optional.ofNullable(this.getCustomFavicon()), // Favicon
            this.shouldEnforceSecureProfile()
        );
        
        callback.setReturnValue(metadata);
    }
    
    private @Nullable Text getCustomMotd() {
        // Get MOTDs and if empty, cancel
        if (!this.motds.isEmpty()) {
            // Cycle the MOTD every 10 seconds
            int i = this.motds.size() == 1 ? 0 : (int) ((System.currentTimeMillis() / 10000) % this.motds.size());
            int clamp = Integer.min(this.motds.size() - 1, i);
            
            // Get the formatted MOTD
            //String raw = this.descriptionReplaceVariables(this.motds.get(clamp));
            String raw = this.motds.get(clamp);
            if (raw != null)
                return FormattingUtils.visitVariables(raw, this::descriptionReplaceVariables);
        }
        
        // Fallback to the default assigned from the server properties
        return Text.of(this.motd);
    }
    
    @Inject(at = @At("RETURN"), method = "createMetadataPlayers")
    public void getCustomPlayers(@NotNull CallbackInfoReturnable<ServerMetadata.Players> callback) {
        ServerMetadata.Players players = callback.getReturnValue();
        List<GameProfile> profiles = players == null ? null : players.sample();
        if (profiles == null)
            return;
        
        for (int i = 0; i < profiles.size(); i++) {
            GameProfile profile = profiles.get(i);
            String name = PlayerNameCallback.getName((MinecraftServer)(Object) this, profile.getId())
                .getString();
            
            // Update the value with a new profile
            profiles.set(i, new GameProfile(
                profile.getId(),
                name
            ));
        }
    }
    
    public ServerMetadata.Favicon getCustomFavicon() {
        if (this.base64.isEmpty())
            return null;
        byte[] random = this.base64.get(this.random.nextInt(this.base64.size()));
        return new ServerMetadata.Favicon(random);
    }
    
    private void refreshMotd() {
        this.motds.clear();
        this.motds.addAll(SewConfig.get(SewCoreConfig.SERVER_MOTD_LIST));
        Collections.shuffle(this.motds);
    }
    
    private String descriptionReplaceVariables(@NotNull MutableText text, @Nullable String description) {
        if (description != null) {
            MinecraftServer server = ServerCore.get();
            
            // For all keys
            for (Map.Entry<String, MotdFunction> row : ServerVariables.entrySet()) {
                // If description contains
                Pattern pattern = Pattern.compile("\\$\\{(" + row.getKey() + "[\\^_]{0,2})}");
                Matcher matcher = pattern.matcher(description);
                
                while (matcher.find()) {
                    String key = matcher.group(1);
                    String val;
                    try {
                        val = row.getValue().parseVar(server);
                        if (val == null) continue;
                        
                        // Change val casing
                        if (key.endsWith("__"))
                            val = CasingUtils.lower(val);
                        else if (key.endsWith("^^"))
                            val = CasingUtils.upper(val);
                        else if (key.endsWith("^"))
                            val = CasingUtils.words(val);
                        
                    } catch (Exception e) { CoreMod.logError(new Exception("Error in MOTD variable \"" + row.getKey() + "\"", e)); return null; }
                    
                    // Replace
                    description = description.replace("${" + key + "}", val);
                }
            }
        }
        return description;
    }
}
