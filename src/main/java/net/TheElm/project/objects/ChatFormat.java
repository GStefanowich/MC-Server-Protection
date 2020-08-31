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

package net.TheElm.project.objects;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import net.TheElm.project.CoreMod;
import net.TheElm.project.enums.ChatRooms;
import net.TheElm.project.utilities.MessageUtils;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

public final class ChatFormat {
    
    private final String raw;
    
    private ChatFormat( String raw ) {
        this.raw = raw;
        
        // Debug
        CoreMod.logDebug( raw );
    }
    
    @Nullable
    public Text format(ChatRooms room, ServerPlayerEntity player) {
        return null;
    }
    
    private Text worldText(RegistryKey<World> dimension, boolean showAsLong) {
        String longKey = null, shortKey = null;
        
        // Set keys based on the world
        if (World.OVERWORLD.equals(dimension)) {
            longKey = "generator.minecraft.surface";
            shortKey = "S";
        } else if (World.NETHER.equals(dimension)) {
            longKey = "biome.minecraft.nether";
            shortKey = "N";
        } else if (World.END.equals(dimension)) {
            longKey = "biome.minecraft.the_end";
            shortKey = "E";
        }
        
        // Create the text
        MutableText longer = ( longKey == null ? new LiteralText("Server") : new TranslatableText( longKey ));
        MutableText shorter = new LiteralText( shortKey == null ? "~" : shortKey );
        
        // Create the hover event
        Formatting formatting = this.worldColor(dimension);
        
        return ( showAsLong ? longer : shorter )
            .formatted( formatting )
            .styled(MessageUtils.simpleHoverText(longer.formatted( formatting )));
    }
    private Formatting worldColor(RegistryKey<World> world) {
        if (World.OVERWORLD.equals(world))
            return Formatting.GREEN;
        if (World.NETHER.equals(world))
            return Formatting.RED;
        if (World.END.equals(world))
            return Formatting.DARK_GRAY;
        return Formatting.WHITE;
    }
    
    @Override
    public String toString() {
        return this.raw;
    }
    
    public static ChatFormat parse(String string) {
        return new ChatFormat( string );
    }
    public static ChatFormat parse(JsonElement config) {
        return ChatFormat.parse(config.getAsString());
    }
    public static JsonElement serializer(ChatFormat src, Type type, JsonSerializationContext context) {
        return context.serialize( src.toString() );
    }
}
