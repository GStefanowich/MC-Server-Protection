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

import net.TheElm.project.CoreMod;
import net.TheElm.project.ServerCore;
import net.TheElm.project.objects.PlayerBookPropertyDelegate;
import net.TheElm.project.utilities.GuideUtils;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.block.BlockState;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.WrittenBookItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.screen.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Lazy;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created on Aug 11 2021 at 3:04 PM.
 * By greg in SewingMachineMod
 */
public class LecternGuideBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {
    private Inventory inventory = new Inventory() {
        @Override
        public int size() {
            return 1;
        }
        
        @Override
        public boolean isEmpty() {
            return false;
        }
        
        @Override
        public ItemStack getStack(int slot) {
            return slot == 0 ? LecternGuideBlockEntity.this.book.get() : ItemStack.EMPTY;
        }
        
        @Override
        public ItemStack removeStack(int slot, int amount) {
            return slot != 0 || amount < 1 ? ItemStack.EMPTY : this.getStack(slot);
        }
        
        @Override
        public ItemStack removeStack(int slot) {
            CoreMod.logError(new Exception("Remove B: " + slot));
            return this.getStack(slot);
        }
        
        @Override
        public void setStack(int slot, ItemStack stack) {}
        
        @Override
        public void markDirty() {
            LecternGuideBlockEntity.this.markDirty();
        }
        
        @Override
        public boolean canPlayerUse(PlayerEntity player) {
            return !this.isEmpty()
                && LecternGuideBlockEntity.this.world.getBlockEntity(LecternGuideBlockEntity.this.pos) == LecternGuideBlockEntity.this
                && !(player.squaredDistanceTo((double) LecternGuideBlockEntity.this.pos.getX() + 0.5D, (double) LecternGuideBlockEntity.this.pos.getY() + 0.5D, (double) LecternGuideBlockEntity.this.pos.getZ() + 0.5D) > 64.0D);
        }
        
        @Override
        public boolean isValid(int slot, ItemStack stack) {
            return false;
        }
        
        @Override
        public void clear() {}
    };
    
    private String bookName = null;
    private Lazy<ItemStack> book = this.newLazy();
    
    public LecternGuideBlockEntity() {
        super(ServerCore.GUIDE_BLOCK_ENTITY);
    }
    
    public void setGuide(@Nullable String bookName) {
        LecternBlock.setHasBook(this.world, this.pos, this.getCachedState(), bookName != null);
        this.world.playSound(null, this.pos, SoundEvents.ITEM_BOOK_PUT, SoundCategory.BLOCKS, 1.0f, 1.0f);
        
        this.bookName = bookName;
        this.book = this.newLazy();
    }
    
    @Override
    public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        ItemStack book;
        if (this.bookName == null || (book = this.book.get()) == null || book.isEmpty())
            return null;
        return new LecternScreenHandler(syncId, this.inventory, new PlayerBookPropertyDelegate(player, book)) {
            @Override
            public boolean onButtonClick(@NotNull PlayerEntity player, int id) {
                if (id != 3)
                    return super.onButtonClick(player, id);
                ItemStack book = LecternGuideBlockEntity.this.getBook();
                if (player instanceof ServerPlayerEntity)
                    ((ServerPlayerEntity)player).closeHandledScreen();
                return player.inventory.insertStack(book);
            }
        };
    }
    
    public @NotNull ItemStack getBook() {
        GuideUtils guide;
        return this.bookName == null || (guide = GuideUtils.getBook(this.bookName)) == null ? ItemStack.EMPTY : guide.newStack();
    }
    public @NotNull Lazy<ItemStack> newLazy() {
        return new Lazy<>(this::getBook);
    }
    
    @Override
    public @NotNull Text getDisplayName() {
        return new LiteralText(this.bookName);
    }
    
    /*
     * Save and load the lectern block
     */
    
    @Override
    public void fromTag(BlockState state, CompoundTag tag) {
        super.fromTag(state, tag);
        
        // Load the book name
        if (tag.contains("guide_book", NbtType.STRING))
            this.bookName = tag.getString("guide_book");
    }
    
    @Override
    public CompoundTag toTag(CompoundTag tag) {
        // Save the book name
        if (this.bookName != null)
            tag.putString("guide_book", this.bookName);
        
        return super.toTag(tag);
    }
    
    @Override
    public void markDirty() {
        super.markDirty();
        this.book = this.newLazy();
    }
}
