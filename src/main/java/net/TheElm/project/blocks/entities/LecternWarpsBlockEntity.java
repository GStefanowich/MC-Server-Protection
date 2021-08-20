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

package net.TheElm.project.blocks.entities;

import net.TheElm.project.ServerCore;
import net.TheElm.project.commands.TeleportsCommand;
import net.TheElm.project.objects.PlayerBookPropertyDelegate;
import net.TheElm.project.utilities.CasingUtils;
import net.TheElm.project.utilities.DimensionUtils;
import net.TheElm.project.utilities.FormattingUtils;
import net.TheElm.project.utilities.WarpUtils;
import net.TheElm.project.utilities.text.MessageUtils;
import net.TheElm.project.utilities.text.TextUtils;
import net.TheElm.project.utilities.text.WrittenBookBuilder;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.screen.LecternScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Tickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Created on Aug 18 2021 at 3:41 PM.
 * By greg in SewingMachineMod
 */
public class LecternWarpsBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {
    public LecternWarpsBlockEntity() {
        super(ServerCore.WARPS_BLOCK_ENTITY);
    }
    
    public void onCreated() {
        LecternBlock.setHasBook(this.world, this.pos, this.getCachedState(), true);
    }
    
    public boolean canPlayerUse(@Nullable PlayerEntity player) {
        return player != null
            && this.world.getBlockEntity(this.pos) == this
            && !(player.squaredDistanceTo((double) this.pos.getX() + 0.5D, (double) this.pos.getY() + 0.5D, (double) this.pos.getZ() + 0.5D) > 64.0D);
    }
    
    @Override
    public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        final PlayerWarpBookInventory inventory = new PlayerWarpBookInventory(player);
        return new LecternScreenHandler(syncId, inventory, inventory.delegate) {
            @Override
            public boolean onButtonClick(@NotNull PlayerEntity player, int id) {
                if (id != 3)
                    return super.onButtonClick(player, id);
                boolean failed;
                if (failed = (!LecternWarpsBlockEntity.this.canPlayerUse(player) || !inventory.teleport())) {
                    player.sendMessage(new LiteralText("Can't take this book.")
                        .formatted(Formatting.RED), true);
                }
                if (player instanceof ServerPlayerEntity)
                    ((ServerPlayerEntity) player).closeHandledScreen();
                return !failed;
            }
        };
    }
    
    @Override
    public Text getDisplayName() {
        return new LiteralText("Warps");
    }
    
    private class PlayerWarpBookInventory implements Inventory {
        private final @NotNull PlayerEntity player;
        public final @NotNull ItemStack book;
        private final @NotNull List<WarpUtils.Warp> warps;
        public final @NotNull PropertyDelegate delegate;
        
        public PlayerWarpBookInventory(@NotNull PlayerEntity player) {
            this.player = player;
            this.warps = new ArrayList<>(WarpUtils.getWarps(this.player)
                .values());
            
            // Sort the list of collections
            Collections.sort(this.warps, WarpUtils.Warp::compare);
            
            this.book = this.getBook();
            this.delegate = new PlayerBookPropertyDelegate(this.player, this.book);
        }
        
        public @NotNull ItemStack getBook() {
            ItemStack stack = new ItemStack(Items.WRITTEN_BOOK, 1);
            
            ListTag pages;
            CompoundTag nbt = stack.getOrCreateTag();
            nbt.put("pages", pages = new ListTag());
            
            this.writeBookNbt(nbt);
            this.writePagesNbt(pages);
            
            return stack;
        }
        
        public void writeBookNbt(@NotNull final CompoundTag nbt) {
            // Put Basic Information
            nbt.putString("author", "Server");
            nbt.putString("title", "Warps");
            nbt.putByte("resolved", (byte)1);
            nbt.putInt("generation", 1);
        }
        
        private void writePagesNbt(@NotNull final ListTag pages) {
            WrittenBookBuilder page;
            if (this.warps.isEmpty()) {
                page = new WrittenBookBuilder();
                page.addString("It seems you don't have any warps :(");
                
                pages.add(StringTag.of(page.toString()));
                
            } else for (WarpUtils.Warp warp : this.warps) {
                page = new WrittenBookBuilder();
                this.toPageNbt(
                    page,
                    warp.getWorld(LecternWarpsBlockEntity.this.world),
                    warp
                );
                pages.add(StringTag.of(page.toString()));
            }
        }
        
        private void toPageNbt(@NotNull WrittenBookBuilder page, @Nullable World world, @NotNull WarpUtils.Warp warp) {
            BlockPos blockPos = warp.warpPos;
            if (world != null) {
                // Warp Name
                page.addString("Warp", Formatting.GOLD)
                    .addString(": ", Formatting.BLACK)
                    .addString(warp.name, Formatting.DARK_BLUE, Formatting.BOLD);
                
                // World
                page.addLine()
                    .addString("World", Formatting.GOLD)
                    .addString(": ", Formatting.BLACK)
                    .addString(DimensionUtils.longDimensionName(warp.world), Formatting.RED);
                
                // Biome
                Biome biome = world.getBiome(blockPos);
                Identifier biomeId = world.getRegistryManager().get(Registry.BIOME_KEY).getId(biome);
                if (biomeId != null) {
                    page.addLine()
                        .addString("Biome", Formatting.GOLD)
                        .addString(": ", Formatting.BLACK)
                        .addTranslation("biome." + biomeId.getNamespace() + "." + biomeId.getPath(), Formatting.RED);
                }
                
                // Weather
                Biome.Precipitation precipitation = world.isRaining() ? biome.getPrecipitation() : Biome.Precipitation.NONE;
                page.addLine()
                    .addString("Weather", Formatting.GOLD)
                    .addString(": ", Formatting.BLACK)
                    .addString(world.isThundering() ? "Thunder" : precipitation == Biome.Precipitation.NONE ? "Clear" : CasingUtils.Sentence(precipitation.name()), Formatting.RED);
                
                // Block Position
                page.addLine()
                    .addString("Location", Formatting.GOLD)
                    .addString(": ", Formatting.BLACK)
                    .addLine()
                    .addString(" X ", Formatting.BLACK)
                        .addString(FormattingUtils.number(warp.warpPos.getX()), Formatting.RED)
                    .addLine()
                    .addString(" Z ", Formatting.BLACK)
                        .addString(FormattingUtils.number(warp.warpPos.getZ()), Formatting.RED);
            }
        }
        
        public boolean teleport() {
            int page = this.delegate.get(0);
            WarpUtils.Warp warp = (page + 1) > this.warps.size() ? null : this.warps.get(page);
            if (warp != null && this.player instanceof ServerPlayerEntity) {
                TeleportsCommand.feedback(this.player, warp);
                
                WarpUtils.teleportPlayerAndAttached(warp, (ServerPlayerEntity) this.player);
                
                return true;
            }
            return false;
        }
        
        @Override
        public int size() { return 1; }
        @Override
        public boolean isEmpty() { return false; }
        @Override
        public ItemStack getStack(int slot) { return this.book; }
        @Override
        public ItemStack removeStack(int slot, int amount) { return this.getStack(slot); }
        @Override
        public ItemStack removeStack(int slot) { return this.getStack(slot); }
        @Override
        public void setStack(int slot, ItemStack stack) {}
        @Override
        public void markDirty() {}
        @Override
        public boolean canPlayerUse(@Nullable PlayerEntity player) { return !this.isEmpty() && LecternWarpsBlockEntity.this.canPlayerUse(player); }
        @Override
        public void clear() {}
    }
}
