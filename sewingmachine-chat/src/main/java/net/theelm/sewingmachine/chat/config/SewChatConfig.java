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

package net.theelm.sewingmachine.chat.config;

import net.theelm.sewingmachine.chat.objects.ChatFormat;
import net.theelm.sewingmachine.config.ConfigOption;

/**
 * Created on Jun 08 2023 at 8:31 PM.
 * By greg in sewingmachine
 */
public final class SewChatConfig {
    private SewChatConfig() {}

    /*
     * Naming
     */

    public static final ConfigOption<Boolean> DO_PLAYER_NICKS = ConfigOption.json("player.nicks", true);
    public static final ConfigOption<Integer> NICKNAME_COST = ConfigOption.json("player.nick_cost", 0);
    
    /*
     * Chat Booleans
     */
    
    public static final ConfigOption<Boolean> CHAT_MODIFY = ConfigOption.json("chat.modify", true);
    public static final ConfigOption<Boolean> CHAT_SHOW_TOWNS = ConfigOption.json("chat.show_towns", true);
    
    public static final ConfigOption<ChatFormat> CHAT_WHISPER_FORMAT = ConfigChatOption.chat("chat.formatting.whisper", "&7&o[Whisper] ${nick}&r&o: ${message}");
    public static final ConfigOption<ChatFormat> CHAT_GLOBAL_FORMAT = ConfigChatOption.chat("chat.formatting.global", "[${world}] &b${nick}&r: ${message}");
    public static final ConfigOption<ChatFormat> CHAT_LOCAL_FORMAT = ConfigChatOption.chat("chat.formatting.local", "&9[Local] &b${nick}&r: ${message}");
    public static final ConfigOption<ChatFormat> CHAT_TOWN_FORMAT = ConfigChatOption.chat("chat.formatting.town", "&a[${town}] &2${nick}&r: ${message}");
    
    //public static final ConfigOption<ChatFormat> CHAT_SERVER_FORMAT = SewConfig.addConfig(ConfigOption.chat("chat.formatting.server", "&7Server&r: ${message}"));
    
    public static final ConfigOption<Boolean> CHAT_MUTE_SELF = ConfigOption.json("chat.mute.personal_mute", true);
    public static final ConfigOption<Boolean> CHAT_MUTE_OP = ConfigOption.json("chat.mute.moderator_mute", true);
    
}
