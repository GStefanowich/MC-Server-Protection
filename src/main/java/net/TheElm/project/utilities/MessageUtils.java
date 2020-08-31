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

import net.TheElm.project.ServerCore;
import net.TheElm.project.enums.ChatRooms;
import net.TheElm.project.interfaces.PlayerData;
import net.TheElm.project.protections.claiming.ClaimantPlayer;
import net.TheElm.project.protections.claiming.ClaimantTown;
import net.minecraft.client.options.ChatVisibility;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.MessageType;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public final class MessageUtils {
    
    private MessageUtils() {}
    
    // General send
    public static void sendTo(ChatRooms chatRoom, ServerPlayerEntity player, Text chatText) {
        switch (chatRoom) {
            // Local message
            case LOCAL: {
                MessageUtils.sendToLocal( player.world, player.getBlockPos(), chatText );
                break;
            }
            // Global message
            case GLOBAL: {
                MessageUtils.sendToAll( chatText );
                break;
            }
            // Message to the players town
            case TOWN: {
                ClaimantPlayer claimantPlayer = ((PlayerData) player).getClaim();
                MessageUtils.sendToTown( claimantPlayer.getTown(), chatText );
                break;
            }
        }
    }
    
    // Send a text blob from a target to a player
    public static void sendAsWhisper(@NotNull ServerCommandSource sender, @NotNull ServerPlayerEntity target, @NotNull Text text) {
        if (!MessageUtils.sendAsWhisper( ( sender.getEntity() instanceof ServerPlayerEntity ? (ServerPlayerEntity) sender.getEntity() : null ), target, text ))
            sender.sendFeedback(new LiteralText("")
                .append(target.getDisplayName())
                .append(" could not receive your message.")
                .formatted(Formatting.RED, Formatting.ITALIC), false);
    }
    public static boolean sendAsWhisper(@Nullable ServerPlayerEntity sender, @NotNull ServerPlayerEntity target, @NotNull Text text) {
        // Log the the server
        ServerCore.get().sendSystemMessage(text, ServerCore.spawnID);
        
        // Send the message to the player (SENDER)
        if ((sender != null) && (!sender.getUuid().equals( target.getUuid() ))) {
            MessageUtils.sendChat(
                Stream.of( sender ),
                text
            );
        }
        
        ChatVisibility visibility = target.getClientChatVisibility();
        if (visibility != ChatVisibility.FULL)
            return false;
        else {
            // Send the message to the player (TARGET)
            MessageUtils.sendChat(
                Stream.of( target ),
                text
            );
            
            return true;
        }
    }
    
    // Send a text blob to a local area
    public static void sendToLocal(final World world, final BlockPos blockPos, Text text) {
        // Log to the server
        ((ServerWorld) world).getServer().sendSystemMessage(text, ServerCore.spawnID);
        
        // Get the players in the area
        BlockPos outerA = new BlockPos(blockPos.getX() + 800, 0, blockPos.getZ() + 800);
        BlockPos outerB = new BlockPos(blockPos.getX() - 800, 800, blockPos.getZ() - 800);
        List<ServerPlayerEntity> players = world.getEntities(ServerPlayerEntity.class, new Box(outerA, outerB), EntityPredicates.VALID_ENTITY);
        
        // Send the message to the players
        MessageUtils.sendChat(
            players.stream(),
            text
        );
    }
    
    // Send a translation blob to all Players
    public static void sendToAll(final String translationKey, final Object... objects) {
        final MinecraftServer server = ServerCore.get();
        MessageUtils.sendSystem(
            server.getPlayerManager().getPlayerList().stream(),
            translationKey,
            objects
        );
    }
    public static void sendToAll(final Text text) {
        final MinecraftServer server = ServerCore.get();
        
        // Log to the server
        server.sendSystemMessage(text, ServerCore.spawnID);
        
        // Send to the players
        MessageUtils.sendChat(
            server.getPlayerManager().getPlayerList().stream(),
            text
        );
    }
    public static void sendToAll(final Text text, Predicate<ServerPlayerEntity> predicate) {
        final MinecraftServer server = ServerCore.get();
        
        // Log to the server
        server.sendSystemMessage(text, ServerCore.spawnID);
        
        // Send to the players
        MessageUtils.sendChat(
            server.getPlayerManager().getPlayerList().stream().filter(predicate),
            text
        );
    }
    
    // Send a translation blob to a Town
    public static void sendToTown(final ClaimantTown town, final String translationKey, final Object... objects) {
        final MinecraftServer server = ServerCore.get();
        MessageUtils.sendSystem(
            server.getPlayerManager().getPlayerList().stream().filter((player) -> {
                ClaimantPlayer claimant = ((PlayerData) player).getClaim();
                return (claimant != null) && (claimant.getTown() != null) && town.getId().equals(claimant.getTown().getId());
            }),
            translationKey,
            objects
        );
    }
    public static void sendToTown(final ClaimantTown town, final Text text) {
        final MinecraftServer server = ServerCore.get();
        
        // Log to the server
        server.sendSystemMessage(text, ServerCore.spawnID);
        
        // Send to the players
        MessageUtils.sendChat(
            server.getPlayerManager().getPlayerList().stream().filter((player) -> {
                ClaimantPlayer claimant = ((PlayerData) player).getClaim();
                return (claimant != null) && (claimant.getTown() != null) && town.getId().equals(claimant.getTown().getId());
            }),
            text
        );
    }
    
    // Send a translation blob to OPs
    public static void sendToOps(final String translationKey, final Object... objects) {
        MessageUtils.sendToOps( 1, translationKey, objects );
    }
    public static void sendToOps(final int opLevel, final String translationKey, final Object... objects) {
        final MinecraftServer server = ServerCore.get();
        MessageUtils.sendSystem(
            server.getPlayerManager().getPlayerList().stream().filter((player) -> player.hasPermissionLevel( opLevel )),
            translationKey,
            objects
        );
    }
    
    // Send command block text to OPs
    public static void consoleToOps(Text event) {
        MessageUtils.consoleToOps(new LiteralText("@"), event);
    }
    public static void consoleToOps(Text player, Text event) {
        MinecraftServer server = ServerCore.get();
        
        Text send = (new TranslatableText("chat.type.admin", player, event)).formatted(Formatting.GRAY, Formatting.ITALIC);
        if (server.getGameRules().getBoolean(GameRules.SEND_COMMAND_FEEDBACK)) {
            Iterator iterator = server.getPlayerManager().getPlayerList().iterator();
            
            while ( iterator.hasNext() ) {
                ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)iterator.next();
                if (server.getPlayerManager().isOperator(serverPlayerEntity.getGameProfile()))
                    serverPlayerEntity.sendMessage(send, MessageType.GAME_INFO, ServerCore.spawnID);
            }
        }
        
        if (server.getGameRules().getBoolean(GameRules.LOG_ADMIN_COMMANDS))
            server.sendSystemMessage(send, Util.NIL_UUID);
    }
    
    // Send a translation blob to a stream of players
    private static void sendSystem(final Stream<ServerPlayerEntity> players, final String translationKey, final Object... objects) {
        players.forEach((player) -> player.sendMessage(
            TranslatableServerSide.text(player, translationKey, objects).formatted(Formatting.YELLOW),
            MessageType.SYSTEM,
            Util.NIL_UUID
        ));
    }
    private static void sendChat(final Stream<ServerPlayerEntity> players, final Text text) {
        players.forEach((player) -> player.sendMessage( text, MessageType.CHAT, Util.NIL_UUID ));
    }
    
    // Convert a Block Position to a Text component
    public static MutableText blockPosToTextComponent(final BlockPos pos) {
        return MessageUtils.blockPosToTextComponent(pos, ", ");
    }
    public static MutableText blockPosToTextComponent(final BlockPos pos, final Identifier id) {
        MutableText out = blockPosToTextComponent(pos);
        
        // Get the dimension
        RegistryKey<World> dimension = RegistryKey.of(Registry.DIMENSION, id);
        if (dimension != null) {
            // TODO: Verify the translation key of the dimension
            out.append(" ")
                .append(new TranslatableText(dimension.toString()));
        }
        
        return out;
    }
    public static MutableText blockPosToTextComponent(final BlockPos pos, final String separator) {
        return MessageUtils.dimensionToTextComponent( separator, pos.getX(), pos.getY(), pos.getZ() );
    }
    public static MutableText dimensionToTextComponent(final String separator, final int x, final int y, final int z) {
        String[] pos = MessageUtils.posToString(x, y, z);
        return new LiteralText("")
            .append(new LiteralText(pos[0]).formatted(Formatting.AQUA))
            .append(separator)
            .append(new LiteralText(pos[1]).formatted(Formatting.AQUA))
            .append(separator)
            .append(new LiteralText(pos[2]).formatted(Formatting.AQUA));
    }
    
    public static <O> MutableText listToTextComponent(final Collection<O> list, Function<O, Text> function) {
        return MessageUtils.listToTextComponent(list, ", ", function);
    }
    public static <O> MutableText listToTextComponent(final Collection<O> list, String separator, Function<O, Text> function) {
        MutableText base = new LiteralText("");
        
        Iterator<O> iterator = list.iterator();
        while (iterator.hasNext()) {
            base.append(function.apply(iterator.next()));
            if (iterator.hasNext())
                base.append(separator);
        }
        
        return base;
    }
    
    public static String blockPosToString(final BlockPos pos) {
        return MessageUtils.blockPosToString(pos, ", ");
    }
    public static String blockPosToString(final BlockPos pos, final String separator) {
        return MessageUtils.blockPosToString(separator, pos.getX(), pos.getY(), pos.getZ());
    }
    public static String blockPosToString(final String separator, final int x, final int y, final int z) {
        return String.join( separator, MessageUtils.posToString( x, y, z ));
    }
    
    private static String[] posToString(final int x, final int y, final int z) {
        return new String[]{
            String.valueOf( x ),
            String.valueOf( y ),
            String.valueOf( z )
        };
    }
    
    // Format a message to chat from a player
    public static MutableText formatPlayerMessage(ServerPlayerEntity player, ChatRooms chatRoom, String raw) {
        return MessageUtils.formatPlayerMessage(player, chatRoom, new LiteralText(raw));
    }
    public static MutableText formatPlayerMessage(ServerPlayerEntity player, ChatRooms chatRoom, Text text) {
        return PlayerNameUtils.getPlayerChatDisplay( player, chatRoom )
            .append(new LiteralText( ": " ).formatted(Formatting.GRAY))
            .append(ColorUtils.format(text, chatRoom.getFormatting()));
    }
    public static MutableText formatPlayerMessage(ServerCommandSource source, ChatRooms chatRoom, Text text) {
        if (source.getEntity() instanceof ServerPlayerEntity)
            return MessageUtils.formatPlayerMessage((ServerPlayerEntity) source.getEntity(), chatRoom, text);
        return PlayerNameUtils.getServerChatDisplay( chatRoom )
            .append(new LiteralText( ": " ).formatted(Formatting.GRAY))
            .append(ColorUtils.format(text, chatRoom.getFormatting()));
    }
    
    // Text Events
    public static UnaryOperator<Style> simpleHoverText(String text, Formatting... styled) {
        return MessageUtils.simpleHoverText(new LiteralText(text).formatted(styled));
    }
    public static UnaryOperator<Style> simpleHoverText(Text text) {
        return style -> style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, text));
    }
    
    // Item text details
    public static @NotNull MutableText detailedItem(ItemStack stack) {
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(stack);
        MutableText output = new LiteralText("")
            .append(detailedItem(stack.getItem()));
        
        if (enchantments.size() > 0) {
            // Add the opening bracket
            output.append(" (");
            
            Iterator<Map.Entry<Enchantment, Integer>> iterator = enchantments.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Enchantment, Integer> value = iterator.next();
                
                // Get enchantment details
                Enchantment enchantment = value.getKey();
                int level = value.getValue();
                
                // Append onto output
                output.append(enchantment.getName(level));
                if (iterator.hasNext())
                    output.append(", ");
            }
            
            // Add the close bracket
            output.append(")");
        }
        
        return output;
    }
    public static @NotNull MutableText detailedItem(@NotNull Item item) {
        return new TranslatableText(item.getTranslationKey());
    }
}