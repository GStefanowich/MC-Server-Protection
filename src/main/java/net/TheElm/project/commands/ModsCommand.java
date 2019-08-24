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

package net.TheElm.project.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.Person;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Collection;

public final class ModsCommand {
    
    private ModsCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("mods")
            .executes(ModsCommand::getModList)
        );
    }

    private static int getModList(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        
        Collection<ModContainer> mods = FabricLoader.getInstance().getAllMods();
        
        Text output = new LiteralText("Server Mods:").formatted(Formatting.YELLOW);
        for ( ModContainer mod : mods ) {
            ModMetadata meta = mod.getMetadata();
            String name = meta.getName();
            String desc = meta.getDescription();
            String type = meta.getType();
            
            // Skip if it doesn't have a name
            if ( name == null ) continue;
            
            Text modText = new LiteralText("\n  ")
                .append(new LiteralText("(" + type.toUpperCase() + ") ").formatted(Formatting.RED))
                .append(name);
            
            System.out.println( type );
            
            Collection<Person> authors = meta.getAuthors();
            Text authorText = new LiteralText(", by: ");
            int authorC = 0;
            for ( Person author : authors )
                authorText.append(( ++authorC > 1 ? ", " : "" )).append(new LiteralText( author.getName() ).formatted(Formatting.AQUA));
            
            // Append information
            if ( authorC > 0 ) modText.append( authorText );
            if ( !"".equals( desc ) ) modText.append(new LiteralText("\n    " + desc).formatted(Formatting.WHITE));
            
            // Append to lines
            output.append( modText );
        }
        
        player.sendMessage(output);
        return Command.SINGLE_SUCCESS;
    }

}
