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

import com.mojang.authlib.GameProfile;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import net.TheElm.project.CoreMod;
import net.TheElm.project.ServerCore;
import net.TheElm.project.config.SewConfig;
import net.TheElm.project.protections.ranks.PlayerRank;
import net.TheElm.project.utilities.*;
import net.TheElm.project.utilities.text.TextUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerMetadata;
import net.minecraft.text.Text;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(ServerMetadata.class)
public abstract class MOTD {
    
    private final List<CharBuffer> base64 = new ArrayList<>();
    
    @Shadow private ServerMetadata.Players players;
    @Shadow private ServerMetadata.Version version;
    
    @Inject(at = @At("RETURN"), method = "<init> *")
    public void onConstruct(@NotNull CallbackInfo callback) {
        /*
         * Process the randomized server icons
         */
        for( String iconName : SewConfig.get(SewConfig.SERVER_ICON_LIST) ) {
            File iconFile = new File(".", iconName + ".png");
            if (iconFile.isFile()) {
                ByteBuf icoByteBuffer = Unpooled.buffer();
                
                try {
                    BufferedImage icoBufferedImage = ImageIO.read(iconFile);
                    
                    // Validate the icon size
                    Validate.validState(icoBufferedImage.getWidth() == 64, "Must be 64 pixels wide");
                    Validate.validState(icoBufferedImage.getHeight() == 64, "Must be 64 pixels high");
                    
                    // Write the read buffer to the stream
                    ImageIO.write(icoBufferedImage, "PNG", new ByteBufOutputStream(icoByteBuffer));
                    
                    ByteBuffer byteBuffer_1 = Base64.getEncoder().encode(icoByteBuffer.nioBuffer());
                    this.base64.add(StandardCharsets.UTF_8.decode(byteBuffer_1));
                } catch (Exception e) {
                    CoreMod.logError("Couldn't load server icon", e);
                } finally {
                    icoByteBuffer.release();
                }
            }
        }
    }
    
    @Inject(at = @At("HEAD"), method = "getDescription", cancellable = true)
    public void onGetDescription(@NotNull CallbackInfoReturnable<Text> callback) {
        // Get MOTDs and if empty, cancel
        List<String> configMOTD = SewConfig.get(SewConfig.SERVER_MOTD_LIST);
        if (configMOTD.size() <= 0) return;
        
        // Get the formatted MOTD
        String raw = this.descriptionReplaceVariables(SewConfig.getRandom(SewConfig.SERVER_MOTD_LIST));
        if (raw != null) {
            Text motd = FormattingUtils.stringToText(raw);
            if (motd != null)
                callback.setReturnValue(motd);
        }
    }
    
    @Inject(at = @At("RETURN"), method = "getPlayers", cancellable = true)
    public void onGetPlayers(@NotNull CallbackInfoReturnable<ServerMetadata.Players> callback) {
        ServerMetadata.Players players = callback.getReturnValue();
        GameProfile[] profiles = players.getSample();
        if (profiles == null)
            return;
        
        for (int i = 0; i < profiles.length; i++) {
            GameProfile profile = profiles[i];
            String name = PlayerNameUtils.fetchPlayerNick(profile.getId())
                .getString();
            
            // If the player has any rank
            for (PlayerRank rank : RankUtils.getPlayerRanks(profile)) {
                Text display;
                if ((display = rank.getDisplay()) != null) {
                    name += " [" + TextUtils.legacyConvert(display) + "]";
                    break;
                }
            }
            
            profiles[i] = new GameProfile(
            profile.getId(),
            name
            );
        }
        
        players.setSample(profiles);
    }
    
    @Inject(at = @At("HEAD"), method = "getFavicon", cancellable = true)
    public void getFavicon(@NotNull CallbackInfoReturnable<String> callback) {
        if (this.base64.isEmpty())
            return;
        CharBuffer random = this.base64.get(ThreadLocalRandom.current().nextInt(this.base64.size()));
        callback.setReturnValue("data:image/png;base64," + random);
    }
    
    private @Contract("null -> null") String descriptionReplaceVariables(@Nullable String description) {
        if (description != null) {
            MinecraftServer server = ServerCore.get();
            
            // For all keys
            for (Map.Entry<String, Function<MinecraftServer, String>> row : ServerVariables.entrySet()) {
                // If description contains
                Pattern pattern = Pattern.compile("\\$\\{(" + row.getKey() + "[\\^_]{0,2})}");
                Matcher matcher = pattern.matcher(description);
                
                while (matcher.find()) {
                    String key = matcher.group(1);
                    String val;
                    try {
                        val = row.getValue().apply(server);
                        if (val == null) continue;
                        
                        // Change val casing
                        if (key.endsWith("__"))
                            val = CasingUtils.Lower(val);
                        else if (key.endsWith("^^"))
                            val = CasingUtils.Upper(val);
                        else if (key.endsWith("^"))
                            val = CasingUtils.Words(val);
                        
                    } catch (Exception e) { CoreMod.logError(new Exception("Error in MOTD variable \"" + row.getKey() + "\"", e)); return null; }
                    
                    // Replace
                    description = description.replace("${" + key + "}", val);
                }
            }
        }
        return description;
    }
}
