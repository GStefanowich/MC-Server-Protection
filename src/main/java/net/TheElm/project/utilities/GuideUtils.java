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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.TheElm.project.CoreMod;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class GuideUtils {
    
    private final JsonObject guideInformation;
    
    private GuideUtils(JsonObject json) {
        this.guideInformation = json;
    }
    
    public String getTitle() {
        return guideInformation.get("title").getAsString();
    }
    
    private @NotNull Collection<StringTag> getAllPageContents() {
        ArrayList<StringTag> list = new ArrayList<>();
        JsonArray pages = this.guideInformation.getAsJsonArray("pages");
        
        // For each page
        for (JsonElement page : pages) {
            list.add(StringTag.of(page.toString()));
        }
        
        return list;
    }
    public @NotNull Tag getPages() {
        ListTag pages = new ListTag();
        pages.addAll(this.getAllPageContents());
        return pages;
    }
    
    private String getBookDescription() {
        return guideInformation.get("description").getAsString();
    }
    
    @Nullable
    private String getBookDescriptionColor() {
        if (this.guideInformation.has("lore_color"))
            return this.guideInformation.get("lore_color").getAsString();
        return null;
    }
    private String getBookDescriptionColorOrDefault(String def) {
        String bookDescColor;
        if (( bookDescColor = this.getBookDescriptionColor() ) == null)
            return def;
        return bookDescColor;
    }
    
    @Nullable
    private String getAuthor() {
        if (this.guideInformation.has("author"))
            return this.guideInformation.get("author").getAsString();
        return null;
    }
    private String getAuthorOrDefault(String def) {
        String bookAuthor;
        if ((bookAuthor = this.getAuthor()) == null)
            return def;
        return bookAuthor;
    }
    
    public @NotNull Tag getBookLore() {
        // Json
        JsonObject json = new JsonObject();
        json.addProperty("text", this.getBookDescription());
        json.addProperty("color", this.getBookDescriptionColorOrDefault("dark_purple"));
        
        // Lore
        ListTag lore = new ListTag();
        lore.add(StringTag.of( json.toString() ));
        
        // Return
        return lore;
    }
    
    public @NotNull ItemStack newStack() {
        // Create the object
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag nbt = book.getOrCreateTag();
        
        // Write the guide data to NBT
        this.writeCustomDataToTag(nbt);
        
        return book;
    }
    
    public void writeCustomDataToTag(@NotNull CompoundTag nbt) {
        // Put Basic Information
        nbt.putString("author", this.getAuthorOrDefault("Server"));
        nbt.putString("title", this.getTitle());
        nbt.putByte("resolved", (byte)1);
        nbt.putInt("generation", 1);
        nbt.put("pages", this.getPages());
        
        // Put Lore
        CompoundTag display = new CompoundTag();
        display.put("Lore", this.getBookLore());
        nbt.put("display", display);
    }
    
    public static @Nullable GuideUtils getBook(@NotNull String name) throws JsonSyntaxException {
        JsonObject fileContents = GuideUtils.readBooksFile();
        if (!fileContents.has(name.toLowerCase() ))
            return null;
        return new GuideUtils(fileContents.getAsJsonObject(name.toLowerCase()));
    }
    public static @NotNull Collection<String> getBooks() {
        JsonObject fileContents = GuideUtils.readBooksFile();
        List<String> list = new ArrayList<>();
        
        for (Map.Entry<String, JsonElement> entry : fileContents.entrySet())
            list.add(entry.getKey());
        
        return list;
    }
    private static JsonObject readBooksFile() throws JsonSyntaxException {
        // Get file locations
        File confDir = CoreMod.getConfDir();
        File bookDat = new File( confDir, "books.json" );
        
        // If the directory doesn't exist, return empty
        if (bookDat.exists()) {
            try {
                return new JsonParser().parse(new FileReader(bookDat)).getAsJsonObject();
            } catch (FileNotFoundException e) {
                CoreMod.logError( e );
            }
        }
        return new JsonObject();
    }
    
}
