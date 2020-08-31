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

import net.TheElm.project.CoreMod;
import net.TheElm.project.ServerCore;
import net.TheElm.project.enums.ShopSigns;
import net.TheElm.project.exceptions.ShopBuilderException;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.MessageType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ShopSignBuilder {
    
    /*
     * Builder information
     */
    private final SignBlockEntity sign;
    private final World world;
    private final BlockPos blockPos;
    private Text[] lines = new Text[4];
    
    /* 
     * Shop information
     */
    private UUID ownerUUID = null;
    private ShopSigns signType = null;
    
    private Identifier tradeItemIdentifier = null;
    private Item tradeItem = null;
    private int tradePrice = 0;
    private int stackSize = 0;
    
    private BlockPos regionPosA = null;
    private BlockPos regionPosB = null;
    
    private ShopSignBuilder(@NotNull World world, @NotNull BlockPos blockPos, @NotNull SignBlockEntity sign) {
        this.world = world;
        this.blockPos = blockPos;
        this.sign = sign;
    }
    
    /*
     * Get information to save to our sign
     */
    public Text[] getLines() {
        return this.lines;
    }
    public SignBlockEntity getSign() {
        return this.sign;
    }
    
    public Identifier getItem() {
        return this.tradeItemIdentifier;
    }
    public UUID shopOwner() {
        return this.ownerUUID;
    }
    public int shopPrice() {
        return this.tradePrice;
    }
    public int itemSize() {
        return this.stackSize;
    }
    public BlockPos regionPosA() {
        return this.regionPosA;
    }
    public BlockPos regionPosB() {
        return this.regionPosB;
    }
    
    public World getWorld() {
        return this.world;
    }
    
    public void shopOwner(UUID newOwner) {
        this.ownerUUID = newOwner;
    }
    public void regionPositioning(BlockPos first, BlockPos second) {
        this.regionPosA = first;
        this.regionPosB = second;
    }
    
    @Nullable
    public ShopSigns getType() {
        return this.signType;
    }
    
    /*
     * Builder
     */
    public boolean setLineText( int line, Text text ) {
        // If the line was already previously updated, skip
        if ( this.lines[line] != null )
            return false;
        
        this.lines[line] = text;
        
        return true;
    }
    public boolean build(@NotNull final ServerPlayerEntity player) {
        /* Signs:
         * [BUY]
         * [SELL]
         * [FREE]
         * [HEAL]
         */
        try {
            this.signType = ShopSigns.valueOf(this.lines[0]);
            if ((this.signType == null) || (!this.signType.isEnabled()))
                return false;
            this.formatOrBreak( player );
            return true;
        } finally {
            // Remove from the map
            buildingSigns.remove( createIdentifier( this.world, this.blockPos ) );
        }
    }
    private void formatOrBreak(@NotNull final ServerPlayerEntity creator) {
        try {
            if (this.signType.formatSign(this, creator))
                return;
        } catch (ShopBuilderException e) {
            creator.sendMessage(
                e.getErrorMessage(),
                MessageType.SYSTEM,
                ServerCore.spawnID
            );
        }
        this.breakSign();
    }
    
    /*
     * Build individual sign types
     */
    public boolean textMatchItem(ServerPlayerEntity player, Text text) {
        String str = text.getString();
        Pattern p = Pattern.compile( "^(\\d+) (.*)$" );
        
        // Check matches
        Matcher m = p.matcher( str );
        if ( !m.find() )
            return false;
        
        this.stackSize = Integer.parseUnsignedInt( m.group( 1 ) );
        String itemName = m.group( 2 ).replace( " ", "_" ).toLowerCase();
        
        if ( "hand".equals(itemName)) {
            ItemStack handStack = player.getOffHandStack();
            this.tradeItemIdentifier = Registry.ITEM.getId(handStack.getItem());
            return (this.tradeItem = handStack.getItem()) != Items.AIR;
            
        } else {
            try {
                if (!itemName.contains(":"))
                    this.tradeItemIdentifier = new Identifier("minecraft:" + itemName);
                else this.tradeItemIdentifier = new Identifier(itemName);
            } catch (InvalidIdentifierException e) {
                return false;
            }
            
            return (this.tradeItem = Registry.ITEM.get(this.tradeItemIdentifier)) != Items.AIR;
        }
    }
    public boolean textMatchCount(Text text) {
        try {
            this.stackSize = Integer.parseUnsignedInt(text.asString());
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }
    public boolean textMatchPrice(Text text) {
        String str = text.getString();
        if ( str.startsWith( "$" ) )
            str = str.substring( 1 );
        
        try {
            this.tradePrice = Integer.parseUnsignedInt( str );
            return true;
        } catch ( NumberFormatException e ) {
            return false;
        }
    }
    public MutableText textParseOwner(Text text, ServerPlayerEntity player) {
        String str = text.getString();
        if ( "server".equalsIgnoreCase( str ) && player.isCreative() ) {
            // Set the owner
            this.ownerUUID = CoreMod.spawnID;
            return new LiteralText( "" );
        } else {
            // Set the owner
            this.ownerUUID = player.getUuid();
            return ((MutableText)player.getName()).formatted(Formatting.DARK_GRAY);
        }
    }
    
    private void breakSign() {
        this.world.breakBlock( this.blockPos, true );
    }
    
    /*
     * Static methods
     */
    private static final Map<String, ShopSignBuilder> buildingSigns = Collections.synchronizedMap(new HashMap<>());
    public static ShopSignBuilder create(@NotNull final World world, @NotNull final BlockPos blockPos, @NotNull final SignBlockEntity sign) {
        String worldLocation = createIdentifier( world, blockPos);
        if (buildingSigns.containsKey( worldLocation ))
            return buildingSigns.get( worldLocation );
        
        ShopSignBuilder builder = new ShopSignBuilder( world, blockPos, sign );
        buildingSigns.put( worldLocation, builder );
        
        return builder;
    }
    private static String createIdentifier(@NotNull final World world, @NotNull final BlockPos blockPos) {
        return IdUtils.get(world) + ":" + MessageUtils.blockPosToString(blockPos);
    }
    
}
