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

package net.theelm.sewingmachine.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.theelm.sewingmachine.CoreMod;
import net.theelm.sewingmachine.ServerCore;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.interfaces.CommandPredicate;
import net.theelm.sewingmachine.utilities.text.StyleApplicator;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ContactInformation;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.Person;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ModsCommand {
    
    private ModsCommand() {}
    
    public static void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher) {
        ServerCore.register(dispatcher, "Mods", (builder) -> builder
            .requires(CommandPredicate.isEnabled(SewConfig.COMMAND_MODS_LIST))
            .executes(ModsCommand::getModList)
        );
    }
    
    private static int getModList(@NotNull CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        Collection<ModContainer> mods = CoreMod.getFabric()
            .getAllMods();
        
        MutableText output = new LiteralText("Server Mods:").formatted(Formatting.YELLOW);
        for ( ModContainer mod : mods ) {
            ModMetadata metadata = mod.getMetadata();
            ContactInformation contact = metadata.getContact();
            String name = metadata.getName();
            String desc = metadata.getDescription();
            String type = metadata.getType(); // Type should always be FABRIC
            String version = metadata.getVersion()
                .getFriendlyString();
            
            StyleApplicator applicator = new StyleApplicator();
            
            // Skip if it doesn't have a name (Or is one of many fabric API mods)
            if ( name == null || ModsCommand.isFabricAPI(metadata) )
                continue;
            
            MutableText modText = new LiteralText("\n ").formatted(Formatting.WHITE)
                .append(new LiteralText(name).formatted(Formatting.RED))
                .append(" v")
                .append(new LiteralText(version).formatted(Formatting.YELLOW));
            
            MutableText authorText = new LiteralText(", by: ").formatted(Formatting.WHITE);
            int authorC = 0;
            for (Person author : metadata.getAuthors())
                authorText.append(( ++authorC > 1 ? ", " : "" ))
                    .append(new LiteralText(author.getName()).formatted(Formatting.AQUA));
            
            // Get the URL source
            Optional<String> url = contact.get("homepage");
            if (!url.isPresent())
                url = contact.get("sources");
            if (url.isPresent())
                applicator.withClick(ClickEvent.Action.OPEN_URL, url.get());
            
            // Append information
            if ( authorC > 0 )
                modText.append(authorText);
            if ( !"".equals( desc ) )
                applicator.withHover(HoverEvent.Action.SHOW_TEXT, new LiteralText(desc)
                    .formatted(Formatting.WHITE));
            
            // Append to lines
            output.append(modText.styled(applicator));
        }
        
        Collection<ResourcePackProfile> packs = source.getServer().getDataPackManager()
            .getEnabledProfiles();
        
        output.append("\nServer Resource Packs:");
        for ( ResourcePackProfile pack : packs ) {
            // Skip the built-in resource packs
            if (pack.getSource() == ResourcePackSource.PACK_SOURCE_BUILTIN || pack.isAlwaysEnabled())
                continue;
            
            // Add the "Text" json/nbt to the list
            MutableText packText = new LiteralText("\n ")
                .append(pack.getDescription());
            
            output.append(packText);
        }
        
        source.sendFeedback(output, false);
        return Command.SINGLE_SUCCESS;
    }
    
    private static boolean isFabricAPI(@NotNull ModMetadata metadata) {
        if (!metadata.getId().startsWith("fabric-"))
            return false;
        String authors = "";
        for (Person author : metadata.getAuthors())
            authors += author.getName();
        
        ContactInformation contact = metadata.getContact();
        Map<String, String> contactInfo = contact.asMap();
        return Objects.equals("FabricMC", authors)
            && Objects.equals("irc://irc.esper.net:6667/fabric", contactInfo.get("irc"))
            && Objects.equals("https://github.com/FabricMC/fabric/issues", contactInfo.get("issues"))
            && Objects.equals("https://fabricmc.net", contactInfo.get("homepage"));
    }
}
