package net.TheElm.project.utilities;

import net.TheElm.project.objects.LootInventory;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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
    
    public boolean addLoot(UUID uuid, ItemStack... items) {
        LootInventory inventory = this.getPlayerLoot(uuid);
        
        // Add new items to the collection
        for (ItemStack item : items) {
            if (!inventory.add(item).isEmpty())
                return false;
            
            MessageUtils.consoleToOps(new LiteralText("Gave new reward ")
                .append(MessageUtils.detailedItem(item))
                .append(" to ")
                .append(PlayerNameUtils.fetchPlayerName(uuid))
                .append("."));
        }
        
        // Save the players loot
        return this.savePlayerLoot(uuid);
    }
    
    public @NotNull LootInventory getPlayerLoot(UUID uuid) {
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
    
    public boolean savePlayerLoot(UUID uuid) {
        LootInventory inventory = this.getPlayerLoot(uuid);
        if (inventory.isInvEmpty() && this.playerRewards.containsKey(uuid))
            return this.playerRewards.remove(uuid) != null;
        else if ((!inventory.isInvEmpty()) && (!this.playerRewards.containsKey(uuid)))
            this.playerRewards.put(uuid, inventory);
        
        // Mark to save loot rewards
        this.isDirty();
        
        return true;
    }
    
    /*
     * Display
     */
    public Text getEntityName() {
        return new TranslatableText(this.entityType.getTranslationKey());
    }
    public Text getContainerName() {
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
    public CompoundTag toTag(CompoundTag tag) {
        for (Map.Entry<UUID, LootInventory> players : playerRewards.entrySet())
            tag.put(players.getKey().toString(), players.getValue().toTag(new ListTag()));
        
        return tag;
    }
    
    public static CompoundTag save(CompoundTag tag) {
        for (Map.Entry<Identifier, BossLootRewards> reward : LOOT_REWARDS.entrySet())
            tag.put(reward.getKey().toString(), reward.getValue().toTag(new CompoundTag()));
        
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
    private static BossLootRewards register(@NotNull BossLootRewards rewards) {
        LOOT_REWARDS.put(rewards.getIdentifier(), rewards);
        return rewards;
    }
}
