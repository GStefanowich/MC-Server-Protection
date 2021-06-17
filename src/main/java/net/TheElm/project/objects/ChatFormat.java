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
import net.TheElm.project.utilities.DimensionUtils;
import net.TheElm.project.utilities.text.MessageUtils;
import net.TheElm.project.utilities.text.StyleApplicator;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

public final class ChatFormat {
    
    private final String raw;
    
    private ChatFormat( String raw ) {
        this.raw = raw;
        
        // Debug
        CoreMod.logDebug(raw);
    }
    
    @Nullable
    public Text format(ChatRooms room, ServerPlayerEntity player) {
        return null;
    }
    
    private Text worldText(@NotNull RegistryKey<World> dimension, boolean showAsLong) {
        // Create the text
        MutableText longer = DimensionUtils.longDimensionName(dimension);
        MutableText shorter = DimensionUtils.shortDimensionName(dimension);
        
        // Create the hover event
        StyleApplicator formatting = DimensionUtils.dimensionColor(dimension);
        
        return ( showAsLong ? longer : shorter )
            .styled(formatting)
            .styled(MessageUtils.simpleHoverText(longer.styled(formatting)));
    }
    
    @Override
    public String toString() {
        return this.raw;
    }
    
    public static @NotNull ChatFormat parse(@NotNull String string) {
        return new ChatFormat(string);
    }
    public static @NotNull ChatFormat parse(@NotNull JsonElement config) {
        return ChatFormat.parse(config.getAsString());
    }
    public static JsonElement serializer(@NotNull ChatFormat src, Type type, @NotNull JsonSerializationContext context) {
        return context.serialize(src.toString());
    }
}
