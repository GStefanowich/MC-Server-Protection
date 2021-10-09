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

package net.TheElm.project.utilities.text;

import net.TheElm.project.ServerCore;
import net.TheElm.project.enums.ChatRooms;
import net.TheElm.project.interfaces.PlayerData;
import net.TheElm.project.protections.claiming.ClaimantPlayer;
import net.TheElm.project.protections.claiming.ClaimantTown;
import net.TheElm.project.utilities.ColorUtils;
import net.TheElm.project.utilities.FormattingUtils;
import net.TheElm.project.utilities.PlayerNameUtils;
import net.TheElm.project.utilities.TranslatableServerSide;
import net.minecraft.block.Block;
import net.minecraft.client.option.ChatVisibility;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.MessageType;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
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
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public final class MessageUtils {
    private MessageUtils() {}
    
    // General send
    public static void sendTo(@NotNull ChatRooms chatRoom, @NotNull ServerPlayerEntity player, @NotNull Text chatText) {
        MessageUtils.sendTo(chatRoom, player, Collections.emptyList(), chatText);
    }
    public static boolean sendTo(@NotNull ChatRooms chatRoom, @NotNull ServerPlayerEntity player, @NotNull Collection<ServerPlayerEntity> tags, @NotNull Text chatText) {
        switch (chatRoom) {
            // Local message
            case LOCAL: {
                return MessageUtils.sendToLocal(player.world, player.getBlockPos(), tags, chatText);
            }
            // Global message
            case GLOBAL: {
                MessageUtils.sendToAll(chatText, tags);
                return true;
            }
            // Message to the players town
            case TOWN: {
                ClaimantPlayer claimantPlayer = ((PlayerData) player).getClaim();
                return MessageUtils.sendToTown(claimantPlayer.getTown(), tags, chatText);
            }
        }
        return false;
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
        ServerCore.get()
            .sendSystemMessage(text, ServerCore.SPAWN_ID);
        
        // Send the message to the player (SENDER)
        if ((sender != null) && (!sender.getUuid().equals( target.getUuid() ))) {
            MessageUtils.sendChat(
                Stream.of( sender ),
                text
            );
        }
        
        ChatVisibility visibility = target.getClientChatVisibility();
        if (visibility != null && visibility != ChatVisibility.FULL)
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
    public static void sendToLocal(@NotNull final World world, @NotNull final Vec3i pos, @NotNull Text text) {
        MessageUtils.sendToLocal(world, pos, Collections.emptyList(), text);
    }
    public static boolean sendToLocal(@NotNull final World world, @NotNull final Vec3i pos, @NotNull Collection<ServerPlayerEntity> tags, @NotNull Text text) {
        // Log to the server
        ((ServerWorld) world).getServer()
            .sendSystemMessage(text, ServerCore.SPAWN_ID);
        
        // Get the players in the area
        BlockPos outerA = new BlockPos(pos.getX() + 800, 0, pos.getZ() + 800);
        BlockPos outerB = new BlockPos(pos.getX() - 800, 800, pos.getZ() - 800);
        List<ServerPlayerEntity> players = world.getEntitiesByClass(ServerPlayerEntity.class, new Box(outerA, outerB), EntityPredicates.VALID_ENTITY);
        
        // Send the message to the players
        MessageUtils.sendChat(
            players.stream(),
            text
        );
        return false;
    }
    
    // Send a translation blob to all Players
    public static void sendToAll(@NotNull final String translationKey, final Object... objects) {
        final MinecraftServer server = ServerCore.get();
        MessageUtils.sendSystem(
            server.getPlayerManager().getPlayerList().stream(),
            translationKey,
            objects
        );
    }
    public static void sendToAll(@NotNull final Text text) {
        MessageUtils.sendToAll(text, Collections.emptyList());
    }
    public static boolean sendToAll(@NotNull final Text text, @NotNull Collection<ServerPlayerEntity> tags) {
        final MinecraftServer server = ServerCore.get();
        
        // Log to the server
        server.sendSystemMessage(text, ServerCore.SPAWN_ID);
        
        // Send to the players
        MessageUtils.sendChat(
            server.getPlayerManager().getPlayerList().stream(),
            text
        );
        return false;
    }
    public static void sendToAll(@NotNull final Text text, @NotNull Predicate<ServerPlayerEntity> predicate) {
        final MinecraftServer server = ServerCore.get();
        
        // Log to the server
        server.sendSystemMessage(text, ServerCore.SPAWN_ID);
        
        // Send to the players
        MessageUtils.sendChat(
            server.getPlayerManager().getPlayerList().stream().filter(predicate),
            text
        );
    }
    
    // Send a translation blob to a Town
    public static void sendToTown(@NotNull final ClaimantTown town, @NotNull Collection<ServerPlayerEntity> tags, @NotNull final String translationKey, final Object... objects) {
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
    public static boolean sendToTown(@NotNull final ClaimantTown town, @NotNull Collection<ServerPlayerEntity> tags, @NotNull final Text text) {
        final MinecraftServer server = ServerCore.get();
        
        // Log to the server
        server.sendSystemMessage(text, ServerCore.SPAWN_ID);
        
        // Send to the players
        MessageUtils.sendChat(
            server.getPlayerManager().getPlayerList().stream().filter((player) -> {
                ClaimantPlayer claimant = ((PlayerData) player).getClaim();
                return (claimant != null) && (claimant.getTown() != null) && town.getId().equals(claimant.getTown().getId());
            }),
            text
        );
        return false;
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
    public static void consoleToOps(@NotNull Text event) {
        MessageUtils.consoleToOps(new LiteralText("@"), event);
    }
    public static void consoleToOps(@NotNull Text player, @NotNull Text event) {
        MinecraftServer server = ServerCore.get();
        GameRules gameRules = server.getGameRules();
        
        Text send = (new TranslatableText("chat.type.admin", player, event)).formatted(Formatting.GRAY, Formatting.ITALIC);
        if (gameRules.getBoolean(GameRules.SEND_COMMAND_FEEDBACK)) {
            PlayerManager playerManager = server.getPlayerManager();
            for (ServerPlayerEntity op : playerManager.getPlayerList())
                if (playerManager.isOperator(op.getGameProfile()))
                    op.sendSystemMessage(send, ServerCore.SPAWN_ID);
        }
        
        if (gameRules.getBoolean(GameRules.LOG_ADMIN_COMMANDS))
            server.sendSystemMessage(send, Util.NIL_UUID);
    }
    
    // Send a translation blob to a stream of players
    private static void sendSystem(@NotNull final Stream<ServerPlayerEntity> players, final String translationKey, final Object... objects) {
        players.forEach((player) -> player.sendMessage(
            TranslatableServerSide.text(player, translationKey, objects).formatted(Formatting.YELLOW),
            MessageType.SYSTEM,
            Util.NIL_UUID
        ));
    }
    private static void sendChat(@NotNull final Stream<ServerPlayerEntity> players, final Text text) {
        players.forEach((player) -> player.sendMessage( text, MessageType.CHAT, Util.NIL_UUID ));
    }
    
    // Convert a Block Position to a Text component
    public static MutableText xyzToText(@NotNull final BlockPos pos) {
        return MessageUtils.xyzToText(pos, ", ");
    }
    public static MutableText xyzToText(@NotNull final BlockPos pos, @NotNull final Identifier id) {
        MutableText out = xyzToText(pos);
        
        // Get the dimension
        RegistryKey<World> dimension = RegistryKey.of(Registry.WORLD_KEY, id);
        if (dimension != null) {
            // TODO: Verify the translation key of the dimension
            out.append(" ")
                .append(new TranslatableText(dimension.toString()));
        }
        
        return out;
    }
    public static MutableText xyzToText(@NotNull final BlockPos pos, @NotNull final String separator) {
        return MessageUtils.dimensionToTextComponent(separator, pos.getX(), pos.getY(), pos.getZ());
    }
    public static MutableText dimensionToTextComponent(@NotNull final String separator, final int x, final int y, final int z) {
        return MessageUtils.dimensionToTextComponent(separator, x, y, z, Formatting.AQUA);
    }
    public static MutableText dimensionToTextComponent(@NotNull final String separator, final int x, final int y, final int z, Formatting... formatting) {
        String[] pos = MessageUtils.posToString(x, y, z);
        return new LiteralText("")
            .append(new LiteralText(pos[0]).formatted(formatting))
            .append(separator)
            .append(new LiteralText(pos[1]).formatted(formatting))
            .append(separator)
            .append(new LiteralText(pos[2]).formatted(formatting));
    }
    
    public static @NotNull <O> MutableText listToTextComponent(@NotNull final Collection<O> list, @NotNull Function<O, Text> function) {
        return MessageUtils.listToTextComponent(list, ", ", function);
    }
    public static @NotNull <O> MutableText listToTextComponent(@NotNull final Collection<O> list, @NotNull String separator, @NotNull Function<O, Text> function) {
        MutableText base = new LiteralText("");
        
        Iterator<O> iterator = list.iterator();
        while (iterator.hasNext()) {
            base.append(function.apply(iterator.next()));
            if (iterator.hasNext())
                base.append(separator);
        }
        
        return base;
    }
    
    public static @NotNull String xyzToString(@NotNull final Vec3i pos) {
        return MessageUtils.xyzToString(pos, ", ");
    }
    public static @NotNull String xyzToString(@NotNull final Vec3i pos, final String separator) {
        return MessageUtils.xyzToString(separator, pos.getX(), pos.getY(), pos.getZ());
    }
    public static @NotNull String xyzToString(@NotNull final String separator, final int x, final int y, final int z) {
        return String.join(separator, MessageUtils.posToString( x, y, z ));
    }
    
    public static @NotNull String xzToString(@NotNull final Vec3i pos) {
        return MessageUtils.xzToString(", ", pos.getX(), pos.getZ());
    }
    public static @NotNull String xzToString(@NotNull final ChunkPos pos) {
        return MessageUtils.xzToString(pos, ", ");
    }
    public static @NotNull String xzToString(@NotNull final ChunkPos pos, final String separator) {
        return MessageUtils.xzToString(separator, pos.x, pos.z);
    }
    public static @NotNull String xzToString(@NotNull final String separator, final int x, final int z) {
        return String.join(separator,MessageUtils.posToString(x, z));
    }
    
    private static @NotNull String[] posToString(final int x, final int z) {
        return new String[]{
            String.valueOf(x),
            String.valueOf(z)
        };
    }
    private static @NotNull String[] posToString(final int x, final int y, final int z) {
        return new String[]{
            String.valueOf(x),
            String.valueOf(y),
            String.valueOf(z)
        };
    }
    
    public static @NotNull String colorToString(@NotNull Color color) {
        return MessageUtils.colorToString(color.getRed(), color.getBlue(), color.getGreen());
    }
    public static @NotNull String colorToString(final int r, final int g, final int b) {
        return "R: " + r +" G: " + g + " B: " + b;
    }
    
    // Format a message to chat from a player
    public static @NotNull MutableText formatPlayerMessage(ServerPlayerEntity player, ChatRooms chatRoom, String raw) {
        return MessageUtils.formatPlayerMessage(player, chatRoom, new LiteralText(raw));
    }
    public static @NotNull MutableText formatPlayerMessage(ServerPlayerEntity player, ChatRooms chatRoom, Text text) {
        return PlayerNameUtils.getPlayerChatDisplay(player, chatRoom)
            .append(new LiteralText(": ").formatted(Formatting.GRAY))
            .append(ColorUtils.format(text, chatRoom.getFormatting()));
    }
    public static @NotNull MutableText formatPlayerMessage(@NotNull ServerCommandSource source, ChatRooms chatRoom, Text text) {
        if (source.getEntity() instanceof ServerPlayerEntity)
            return MessageUtils.formatPlayerMessage((ServerPlayerEntity) source.getEntity(), chatRoom, text);
        return PlayerNameUtils.getServerChatDisplay( chatRoom )
            .append(new LiteralText( ": " ).formatted(Formatting.GRAY))
            .append(ColorUtils.format(text, chatRoom.getFormatting()));
    }
    
    public static @NotNull MutableText formatObject(@NotNull Item item) {
        return new TranslatableText(item.getTranslationKey()).formatted(Formatting.AQUA);
    }
    public static @NotNull MutableText formatObject(@NotNull Block block) {
        return new TranslatableText(block.getTranslationKey()).formatted(Formatting.AQUA);
    }
    public static @NotNull MutableText formatNumber(@NotNull Number number, @NotNull Formatting... formatting) {
        return MessageUtils.formatNumber("", number, "", formatting);
    }
    public static @NotNull MutableText formatNumber(@NotNull String prefix, @NotNull Number number, @NotNull Formatting... formatting) {
        return MessageUtils.formatNumber(prefix, number, "", formatting);
    }
    public static @NotNull MutableText formatNumber(@NotNull Number number, @NotNull String suffix, @NotNull Formatting... formatting) {
        return MessageUtils.formatNumber("", number, suffix, formatting);
    }
    public static @NotNull MutableText formatNumber(@NotNull String prefix, @NotNull Number number, @NotNull String suffix, @NotNull Formatting... formatting) {
        MutableText out = new LiteralText(prefix + FormattingUtils.number(number) + suffix);
        if (formatting.length > 0)
            return out.formatted(formatting);
        return out.formatted(Formatting.AQUA);
    }
    
    // Text Events
    public static @NotNull UnaryOperator<Style> simpleHoverText(@NotNull String text, @NotNull Formatting... styled) {
        return MessageUtils.simpleHoverText(new LiteralText(text).formatted(styled));
    }
    public static @NotNull UnaryOperator<Style> simpleHoverText(@NotNull Text text) {
        return style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, text));
    }
    
    // Item text details
    public static @NotNull MutableText detailedItem(@NotNull ItemStack stack) {
        List<Text> infos = new LinkedList<>();
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(stack);
        MutableText output = new LiteralText("")
            .append(MessageUtils.detailedItem(stack.getItem()));
        
        if (stack.getItem().isDamageable() && !stack.isDamageable())
            infos.add(new TranslatableText("item.unbreakable"));
        
        if (!enchantments.isEmpty()) {
            for (Map.Entry<Enchantment, Integer> value : enchantments.entrySet()) {
                // Get enchantment details
                Enchantment enchantment = value.getKey();
                int level = value.getValue();
                
                // Append onto output
                infos.add(enchantment.getName(level));
            }
        }
        
        if (!infos.isEmpty()) {
            // Add the opening bracket
            output.append(" (");
            
            Iterator<Text> it = infos.iterator();
            while (it.hasNext()) {
                output.append(it.next());
                if (it.hasNext())
                    output.append(", ");
            }
            
            // Add the close bracket
            output.append(")");
        }
        
        output.append(" x" + FormattingUtils.number(stack.getCount()));
        
        return output;
    }
    public static @NotNull MutableText detailedItem(@NotNull Item item) {
        return new TranslatableText(item.getTranslationKey());
    }
}