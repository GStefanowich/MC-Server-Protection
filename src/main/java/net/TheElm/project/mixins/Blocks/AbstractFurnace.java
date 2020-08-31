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

package net.TheElm.project.mixins.Blocks;

import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeInputProvider;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.RecipeUnlocker;
import net.minecraft.util.Tickable;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnace extends LockableContainerBlockEntity implements SidedInventory, RecipeUnlocker, RecipeInputProvider, Tickable {
    
    protected AbstractFurnace(BlockEntityType<?> blockEntityType) {
        super(blockEntityType);
    }
    
    private Item cachedRecipeIngredient = null;
    private Recipe<?> cachedRecipe = null;
    private boolean isRecipeDirty = true;
    
    @Shadow private native boolean isBurning();
    @Shadow protected native int getFuelTime(ItemStack itemStack);
    @Shadow protected native int getCookTime();
    @Shadow private native void craftRecipe(@Nullable Recipe<?> recipe);
    
    @Shadow protected DefaultedList<ItemStack> inventory;
    @Shadow private int burnTime;
    @Shadow private int fuelTime;
    @Shadow private int cookTime;
    @Shadow private int cookTimeTotal;
    
    @Shadow @Final
    protected RecipeType<? extends AbstractCookingRecipe> recipeType;
    
    /**
     * Cached the furnace recipe
     * 
     * @author TheElm
     * @reason Speeds up TPS
     */
    @Overwrite
    public void tick() {
        boolean wasBurning = this.isBurning(), // If WAS burning (before tick iteration)
            isBurning; // If is NOW burning (during tick iteration)
        boolean updateClient = false; // If we should update the client
        if (wasBurning) {
            --this.burnTime;
        }
        
        // Ignore on the client
        if (this.world == null || this.world.isClient) return;
        
        ItemStack fuelSourceStack = this.inventory.get(1);
        if ((!(isBurning = this.isBurning())) && (fuelSourceStack.isEmpty() || this.inventory.get(0).isEmpty())) {
            if (!this.isBurning() && this.cookTime > 0) {
                this.cookTime = MathHelper.clamp(this.cookTime - 2, 0, this.cookTimeTotal);
            }
        } else {
            if ((!(isBurning = this.isBurning())) && this.canAcceptRecipeOutput()) {
                
                this.burnTime = this.getFuelTime(fuelSourceStack); // Get the burn time of the fuel source
                this.fuelTime = this.burnTime;
                
                // If currently burning (updated from fuel time)
                if (isBurning = this.isBurning()) {
                    updateClient = true;
                    
                    // If fuel source is not empty
                    if (!fuelSourceStack.isEmpty()) {
                        Item fuelSourceItem = fuelSourceStack.getItem();
                        
                        // Take from the fuel supply
                        fuelSourceStack.decrement(1);
                        
                        // If the fuel supply is empty
                        if (fuelSourceStack.isEmpty()) {
                            // Check if an item gets returned from the recipe (Lava bucket -> bucket)
                            Item fuelReturnItem = fuelSourceItem.getRecipeRemainder();
                            
                            // If an item was returned, put it into the fuel slot
                            this.inventory.set(1, fuelReturnItem == null ? ItemStack.EMPTY : new ItemStack(fuelReturnItem));
                        }
                    }
                }
            }
            
            // If the furnace is burning AND the output slot can accept the given recipe
            if (isBurning && this.canAcceptRecipeOutput()) {
                // Increase the cooking time
                ++this.cookTime;
                
                // If cook time meets the required total cook time
                if (this.cookTime == this.cookTimeTotal) {
                    this.cookTime = 0; // Reset cook time
                    this.cookTimeTotal = this.getCookTime();
                    this.craftRecipe(this.cachedRecipe); // Complete the recipe
                    updateClient = true; // Update the client for recipe changes
                }
            } else {
                this.cookTime = 0; // Reset cook time
            }
        }
        
        if (wasBurning != isBurning) {
            updateClient = true; // Update the client for state change
            this.world.setBlockState(this.pos, this.world.getBlockState(this.pos).with(AbstractFurnaceBlock.LIT, isBurning), 3);
        }
        
        // Mark as send to client
        if (updateClient) this.markDirty();
    }
    
    @Inject(at = @At("TAIL"), method = "setStack")
    public void onInvUpdate(int pos, ItemStack itemStack, CallbackInfo callback) {
        if (pos == 0) this.isRecipeDirty = true;
    }
    
    private boolean canAcceptRecipeOutput() {
        Recipe<?> recipe = this.cachedRecipe;
        
        ItemStack smeltStack = this.inventory.get(0);
        Item smeltItem = smeltStack.getItem();
        
        // Find a match for the cache
        if ((recipe == null) || (smeltItem != this.cachedRecipeIngredient))
            recipe = this.updateCachedRecipe( smeltItem );
        
        // Run a check on the recipe
        if (smeltStack.isEmpty() || recipe == null)
            return false;
        
        // Get the recipes output
        ItemStack recipeOutput = recipe.getOutput();
        if (recipeOutput.isEmpty()) return false;
        
        ItemStack outputStack = this.inventory.get(2);
        if (outputStack.isEmpty()) {
            return true;
        } else if (!outputStack.isItemEqualIgnoreDamage(recipeOutput)) {
            return false;
        } else if (outputStack.getCount() < this.getMaxCountPerStack() && outputStack.getCount() < outputStack.getMaxCount()) {
            return true;
        } else {
            return outputStack.getCount() < recipeOutput.getMaxCount();
        }
    }
    
    @Nullable
    private Recipe<?> updateCachedRecipe(Item ingredient) {
        if (this.isRecipeDirty && (this.world != null)) {
            // Cache the ingredient to the recipe
            this.cachedRecipeIngredient = ingredient;
            
            // Ignore if ingredient is nothing
            if (ingredient != Items.AIR) {
                // Find the cached recipe
                this.cachedRecipe = this.world.getRecipeManager()
                    .getFirstMatch(this.recipeType, this, this.world).orElse(null);
            }
            // No longer dirty
            this.isRecipeDirty = false;
        }
        return this.cachedRecipe;
    }
    
}
