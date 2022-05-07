package net.theelm.sewingmachine.utilities;

import net.theelm.sewingmachine.objects.LootInventory;
import net.theelm.sewingmachine.objects.rewards.RewardContext;
import net.theelm.sewingmachine.objects.rewards.WeightedReward;
import net.theelm.sewingmachine.utilities.text.MessageUtils;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BossLootRewards {
    
    private static final Map<Identifier, BossLootRewards> LOOT_REWARDS = new HashMap<>();
    public static final BossLootRewards DRAGON_LOOT = register(new BossLootRewards("loot:ender_dragon", EntityType.ENDER_DRAGON));
    
    private boolean dirty = false;
    
    private final Identifier identifier;
    private final Map<UUID, LootInventory> playerRewards = new HashMap<>();
    private final EntityType<?> entityType;
    
    protected BossLootRewards(String identifier, EntityType<?> entityType) {
        this(new Identifier(identifier), entityType);
    }
    protected BossLootRewards(Identifier identifier, EntityType<?> entityType) {
        this.identifier = identifier;
        this.entityType = entityType;
    }
    
    public Identifier getIdentifier() {
        return this.identifier;
    }
    
    @Override
    public String toString() {
        return this.getIdentifier()
            .toString();
    }
    
    /*
     * Loot
     */
    
    public boolean addLoot(@NotNull PlayerEntity player, @NotNull WeightedReward reward) {
        boolean success;
        
        // Add new items to the collection
        RewardContext context = reward.create(player);
        if (success = this.addLoot(player.getUuid(), context)) {
            MessageUtils.consoleToOps(new LiteralText("Gave new reward ")
                .append(context.asText())
                .append(" to ")
                .append(player.getDisplayName())
                .append("."));
        }
        
        return success;
    }
    public boolean addLoot(@NotNull UUID uuid, @NotNull RewardContext context) {
        // If no stack to insert, success
        if (!context.hasStack())
            return context.wasSuccess();
        ItemStack stack = context.getStack();
        
        // Get the loot inventory to insert into
        LootInventory inventory = this.getPlayerLoot(uuid);
        
        if (!inventory.insertLoot(stack))
            return false;
        
        // Save the players loot
        return this.savePlayerLoot(uuid);
    }
    
    public @NotNull LootInventory getPlayerLoot(@NotNull UUID uuid) {
        LootInventory inventory = this.playerRewards.get(uuid);
        
        // Get the inventory if it is loaded
        if (inventory == null) {
            // Create a new inventory
            inventory = new LootInventory();
            
            // Store the inventory
            this.playerRewards.put(uuid, inventory);
        }
        
        // Return the grabbed inventory
        return inventory;
    }
    
    public boolean savePlayerLoot(@NotNull UUID uuid) {
        LootInventory inventory = this.getPlayerLoot(uuid);
        if (inventory.isEmpty() && this.playerRewards.containsKey(uuid))
            return this.playerRewards.remove(uuid) != null;
        else if ((!inventory.isEmpty()) && (!this.playerRewards.containsKey(uuid)))
            this.playerRewards.put(uuid, inventory);
        
        // Mark to save loot rewards
        this.isDirty();
        
        return true;
    }
    
    /*
     * Display
     */
    public @NotNull Text getEntityName() {
        return new TranslatableText(this.entityType.getTranslationKey());
    }
    public @NotNull Text getContainerName() {
        return new LiteralText("")
            .append(this.getEntityName())
            .append(" loot.");
    }
    
    /*
     * Saving
     */
    
    public void isDirty() {
        this.dirty = true;
    }
    public @NotNull NbtCompound toTag(@NotNull NbtCompound tag) {
        for (Map.Entry<UUID, LootInventory> players : playerRewards.entrySet())
            tag.put(players.getKey().toString(), players.getValue().toTag(new NbtList()));
        
        return tag;
    }
    
    public static @NotNull NbtCompound save(@NotNull NbtCompound tag) {
        for (Map.Entry<Identifier, BossLootRewards> reward : LOOT_REWARDS.entrySet())
            tag.put(reward.getKey().toString(), reward.getValue().toTag(new NbtCompound()));
        
        return tag;
    }
    
    /*
     * Static Methods
     */
    public static @Nullable BossLootRewards get(@NotNull Identifier identifier) {
        for (Map.Entry<Identifier, BossLootRewards> entry : LOOT_REWARDS.entrySet()) {
            if (entry.getKey().equals(identifier))
                return entry.getValue();
        }
        return null;
    }
    private static @NotNull BossLootRewards register(@NotNull BossLootRewards rewards) {
        BossLootRewards.LOOT_REWARDS.put(rewards.getIdentifier(), rewards);
        return rewards;
    }
}
