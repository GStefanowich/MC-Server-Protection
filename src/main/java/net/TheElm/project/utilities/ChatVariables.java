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

package net.TheElm.project.utilities;

import net.TheElm.project.interfaces.MoneyHolder;
import net.TheElm.project.interfaces.MotdFunction;
import net.TheElm.project.interfaces.PlayerData;
import net.TheElm.project.interfaces.chat.ChatFunction;
import net.TheElm.project.interfaces.chat.EntityChatFunction;
import net.TheElm.project.interfaces.chat.ChatMessageFunction;
import net.TheElm.project.interfaces.chat.ServerChatFunction;
import net.TheElm.project.protections.claiming.ClaimantPlayer;
import net.TheElm.project.protections.claiming.ClaimantTown;
import net.TheElm.project.utilities.text.MessageUtils;
import net.TheElm.project.utilities.text.StyleApplicator;
import net.TheElm.project.utilities.text.TextUtils;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created on Dec 20 2021 at 7:34 PM.
 * By greg in SewingMachineMod
 */
public class ChatVariables {
    private static final @NotNull Map<String, ChatFunction> VARIABLES;
    static {
        /*
         * Save our Variables to parse
         */
        Map<String, ChatFunction> variables = new HashMap<>();
        
        // Get the regular name
        variables.put("name", (EntityChatFunction)(source, entity, casing) -> TextUtils.literal(entity.getEntityName(), casing));
        
        // Get the uuid
        variables.put("uuid", (EntityChatFunction)(source, entity, casing) -> TextUtils.literal(entity.getUuidAsString(), casing));
        
        // Get the nickname (Or regular name)
        variables.put("nick", (source, chatMessage, room, casing) -> {
            if (source.getEntity() instanceof ServerPlayerEntity player)
                return PlayerNameUtils.getPlayerDisplayName(player);
            return source.getDisplayName();
        });
        
        // Get town name
        variables.put("town", (EntityChatFunction)(source, entity, casing) -> {
            ClaimantTown town = null;
            if (entity instanceof ServerPlayerEntity player) {
                ClaimantPlayer claim = ((PlayerData)player).getClaim();
                if (claim != null)
                    town = claim.getTown();
            }
            return town == null ? TextUtils.literal() : town.getName();
        });
        
        // Get the player balance
        variables.put("balance", (EntityChatFunction)(source, entity, casing) -> TextUtils.literal(entity instanceof ServerPlayerEntity player ? ((MoneyHolder) player).getPlayerWallet() : 0));
        
        // Chat room
        variables.put("chat", (ChatMessageFunction)(room, message, casing) -> TextUtils.literal(room.name(), casing));
        
        // Entity X, Y, Z position
        variables.put("x", (EntityChatFunction)(source, entity, casing) -> TextUtils.literal(entity.getBlockX()));
        variables.put("y", (EntityChatFunction)(source, entity, casing) -> TextUtils.literal(entity.getBlockY()));
        variables.put("z", (EntityChatFunction)(source, entity, casing) -> TextUtils.literal(entity.getBlockZ()));
        
        // Get the chat message
        variables.put("message", (ChatMessageFunction)(room, message, casing) -> message);
        
        // Get the entity biome
        variables.put("biome", (EntityChatFunction)(source, entity, casing) -> {
            World world = entity.getWorld();
            RegistryKey<Biome> registryKey = world.getBiomeKey(entity.getBlockPos())
                .orElse(BiomeKeys.PLAINS);
            Identifier identifier = registryKey.getValue();
            return new TranslatableText("biome." + identifier.getNamespace() + "." + identifier.getPath());
        });
        
        // Get the entity world
        variables.put("world", (EntityChatFunction)(source, entity, casing)
            -> ChatVariables.worldText(entity.getWorld().getRegistryKey(), casing, true));
        variables.put("w", (EntityChatFunction)(source, entity, casing)
            -> ChatVariables.worldText(entity.getWorld().getRegistryKey(), casing, false));
        
        // Copy all MOTD (Non-user) Variables
        for (Map.Entry<String, MotdFunction> entry : ServerVariables.entrySet()) {
            MotdFunction function = entry.getValue();
            variables.put(entry.getKey(), (ServerChatFunction)(server, casing) -> TextUtils.literal(function.parseVar(server), casing));
        }
        
        VARIABLES = variables;
    }
    
    private static Text worldText(@NotNull RegistryKey<World> dimension, @NotNull CasingUtils.Casing casing, boolean showAsLong) {
        // Create the text
        MutableText longer = DimensionUtils.longDimensionName(dimension, casing);
        
        // Create the hover event
        StyleApplicator formatting = DimensionUtils.dimensionColor(dimension);
        return ( showAsLong ? longer : DimensionUtils.shortDimensionName(dimension, CasingUtils.Casing.UPPER)
            .styled(MessageUtils.simpleHoverText(longer.styled(formatting))))
            .styled(formatting);
    }
    
    public static @NotNull Set<Map.Entry<String, ChatFunction>> entrySet() {
        return ChatVariables.VARIABLES.entrySet();
    }
    public static ChatFunction get(String key) {
        return ChatVariables.VARIABLES.get(key);
    }
}
