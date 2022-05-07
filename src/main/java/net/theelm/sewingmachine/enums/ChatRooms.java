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

package net.theelm.sewingmachine.enums;

import net.theelm.sewingmachine.config.ConfigOption;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.interfaces.ServerTranslatable;
import net.theelm.sewingmachine.objects.ChatFormat;
import net.theelm.sewingmachine.utilities.TranslatableServerSide;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum ChatRooms implements ServerTranslatable {
    WHISPER(SewConfig.CHAT_WHISPER_FORMAT, Formatting.GRAY, Formatting.ITALIC),
    LOCAL(SewConfig.CHAT_LOCAL_FORMAT, Formatting.BLUE),
    GLOBAL(SewConfig.CHAT_GLOBAL_FORMAT, Formatting.WHITE),
    TOWN(SewConfig.CHAT_TOWN_FORMAT, Formatting.GREEN);
    
    private final ConfigOption<ChatFormat> format;
    private final Formatting[] formatting;
    
    ChatRooms(ConfigOption<ChatFormat> format, Formatting... formattings) {
        this.format = format;
        this.formatting = formattings;
    }
    
    @Override
    public @NotNull MutableText translate(@NotNull Locale locale) {
        return TranslatableServerSide.text(
            locale,
            "chat.room." + this.name().toLowerCase()
        );
    }
    
    public ChatFormat getFormat() {
        return SewConfig.get(this.format);
    }
    
    public Formatting[] getFormatting() {
        return this.formatting;
    }
}
