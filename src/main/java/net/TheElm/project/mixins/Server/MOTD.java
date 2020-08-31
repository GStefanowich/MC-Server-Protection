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

import com.mojang.bridge.game.GameVersion;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import net.TheElm.project.CoreMod;
import net.TheElm.project.ServerCore;
import net.TheElm.project.config.SewConfig;
import net.TheElm.project.utilities.CasingUtils;
import net.TheElm.project.utilities.FormattingUtils;
import net.TheElm.project.utilities.SleepUtils;
import net.minecraft.SharedConstants;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerMetadata;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.Difficulty;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.World;
import org.apache.commons.lang3.Validate;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

@Mixin(ServerMetadata.class)
public abstract class MOTD {
    
    private final Map<String, Callable<String>> motdVariables = new HashMap<>();
    private final List<CharBuffer> base64 = new ArrayList<>();
    
    @Shadow private ServerMetadata.Players players;
    @Shadow private ServerMetadata.Version version;
    
    @Inject(at = @At("RETURN"), method = "<init> *")
    public void onConstruct(CallbackInfo callback) {
        /*
         * Save our Variables to parse
         */
        
        // Version
        this.motdVariables.put( "version", () -> {
            GameVersion version = SharedConstants.getGameVersion();
            if (version == null)
                return "1.??.??";
            return version.getId();
        });
        // Time
        this.motdVariables.put( "time", () -> {
            MinecraftServer server = ServerCore.get();
            ServerWorld world = server.getWorld(World.OVERWORLD);
            if (world == null) return "time";
            return SleepUtils.timeFromMillis(world.getTimeOfDay());
        });
        // Difficulty
        this.motdVariables.put( "difficulty", () -> {
            MinecraftServer server = ServerCore.get();
            SaveProperties properties = server.getSaveProperties();
            if (properties.isHardcore())
                return "hardcore";
            Difficulty difficulty = properties.getDifficulty();
            ServerWorld world = StreamSupport.stream(server.getWorlds().spliterator(), false).findFirst().orElse(null);
            if (world != null)
                difficulty = world.getLevelProperties().getDifficulty();
            return difficulty.getName();
        });
        
        /*
         * Process the randomized server icons
         */
        for( String iconName : SewConfig.get(SewConfig.SERVER_ICON_LIST) ) {
            File iconFile = new File(".", iconName + ".png");
            if (iconFile.isFile()) {
                ByteBuf byteBuf_1 = Unpooled.buffer();
        
                try {
                    BufferedImage bufferedImage_1 = ImageIO.read(iconFile);
                    Validate.validState(bufferedImage_1.getWidth() == 64, "Must be 64 pixels wide");
                    Validate.validState(bufferedImage_1.getHeight() == 64, "Must be 64 pixels high");
                    ImageIO.write(bufferedImage_1, "PNG", new ByteBufOutputStream(byteBuf_1));
                    ByteBuffer byteBuffer_1 = Base64.getEncoder().encode(byteBuf_1.nioBuffer());
                    this.base64.add(StandardCharsets.UTF_8.decode(byteBuffer_1));
                } catch (Exception e) {
                    CoreMod.logError("Couldn't load server icon", e);
                } finally {
                    byteBuf_1.release();
                }
            }
        }
    }
    
    @Inject(at = @At("TAIL"), method = "getDescription", cancellable = true)
    public void onGetDescription(CallbackInfoReturnable<Text> callback) {
        // Get MOTDs and if empty, cancel
        List<String> configMOTD = SewConfig.get(SewConfig.SERVER_MOTD_LIST);
        if (configMOTD.size() <= 0) return;
        
        // Get the formatted MOTD
        String raw = descriptionReplaceVariables(SewConfig.getRandom(SewConfig.SERVER_MOTD_LIST));
        if (raw != null) {
            Text motd = FormattingUtils.stringToText(raw);
            if (motd != null)
                callback.setReturnValue(motd);
        }
    }
    
    @Inject(at = @At("HEAD"), method = "getFavicon", cancellable = true)
    public void getFavicon(CallbackInfoReturnable<String> callback) {
        if (this.base64.isEmpty())
            return;
        CharBuffer random = this.base64.get(ThreadLocalRandom.current().nextInt(this.base64.size()));
        callback.setReturnValue("data:image/png;base64," + random);
    }
    
    private String descriptionReplaceVariables(String description) {
        if (description != null) {
            // For all keys
            for (Map.Entry<String, Callable<String>> row : this.motdVariables.entrySet()) {
                // If description contains
                Pattern pattern = Pattern.compile("\\$\\{(" + row.getKey() + "[\\^_]{0,2})}");
                Matcher matcher = pattern.matcher(description);
                
                while (matcher.find()) {
                    String key = matcher.group(1);
                    String val;
                    try {
                        val = row.getValue().call();
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
