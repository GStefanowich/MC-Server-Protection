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

package net.TheElm.project.objects;

import com.mojang.serialization.Lifecycle;
import net.TheElm.project.config.ConfigOption;
import net.TheElm.project.config.SewConfig;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.LevelProperties;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.level.UnmodifiableLevelProperties;
import net.minecraft.world.timer.Timer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class DynamicLevelProperties implements ServerWorldProperties {
    private final @NotNull ServerWorldProperties accessor;
    private final @NotNull ServerWorldProperties dynamic;
    
    public DynamicLevelProperties(@NotNull SaveProperties save, @NotNull ServerWorldProperties server) {
        this.dynamic = new LevelProperties(new LevelInfo(
                save.getLevelName(),
                save.getGameMode(),
                save.isHardcore(),
                save.getDifficulty(),
                save.areCommandsAllowed(),
                save.getGameRules().copy(),
                save.getDataPackSettings()
            ),
            save.getGeneratorOptions(),
            Lifecycle.stable()
        );
        this.accessor = new UnmodifiableLevelProperties(save, server);
    }
    
    @Override
    public void setSpawnX(int spawnX) {
        this.getSewPropertiesFor(SewConfig.WORLD_SPECIFIC_SPAWN)
            .setSpawnX(spawnX);
    }
    
    @Override
    public void setSpawnY(int spawnY) {
        this.getSewPropertiesFor(SewConfig.WORLD_SPECIFIC_SPAWN)
            .setSpawnY(spawnY);
    }
    
    @Override
    public void setSpawnZ(int spawnZ) {
        this.getSewPropertiesFor(SewConfig.WORLD_SPECIFIC_SPAWN)
            .setSpawnZ(spawnZ);
    }
    
    @Override
    public void setSpawnAngle(float angle) {
        this.getSewPropertiesFor(SewConfig.WORLD_SPECIFIC_SPAWN)
            .setSpawnAngle(angle);
    }
    
    @Override
    public void setSpawnPos(BlockPos pos, float angle) {
        this.getSewPropertiesFor(SewConfig.WORLD_SPECIFIC_SPAWN)
            .setSpawnPos(pos, angle);
    }
    
    @Override
    public int getSpawnX() {
        return this.getSewPropertiesFor(SewConfig.WORLD_SPECIFIC_SPAWN)
            .getSpawnX();
    }
    
    @Override
    public int getSpawnY() {
        return this.getSewPropertiesFor(SewConfig.WORLD_SPECIFIC_SPAWN)
            .getSpawnY();
    }
    
    @Override
    public int getSpawnZ() {
        return this.getSewPropertiesFor(SewConfig.WORLD_SPECIFIC_SPAWN)
            .getSpawnZ();
    }
    
    @Override
    public float getSpawnAngle() {
        return this.getSewPropertiesFor(SewConfig.WORLD_SPECIFIC_SPAWN)
            .getSpawnAngle();
    }
    
    @Override
    public void setTime(long time) {
        this.accessor.setTime(time);
    }
    
    @Override
    public long getTime() {
        return this.accessor.getTime();
    }
    
    @Override
    public long getTimeOfDay() {
        return this.getSewPropertiesFor(SewConfig.WORLD_SPECIFIC_TIME)
            .getTimeOfDay();
    }
    
    @Override
    public void setTimeOfDay(long timeOfDay) {
        this.getSewPropertiesFor(SewConfig.WORLD_SPECIFIC_TIME).setTimeOfDay(timeOfDay);
    }
    
    @Override
    public boolean isRaining() {
        return this.getSewPropertiesFor(SewConfig.WORLD_SPECIFIC_WEATHER)
            .isRaining();
    }
    
    @Override
    public void setRaining(boolean raining) {
        this.getSewPropertiesFor(SewConfig.WORLD_SPECIFIC_WEATHER)
            .setRaining(raining);
    }
    
    @Override
    public boolean isThundering() {
        return this.getSewPropertiesFor(SewConfig.WORLD_SPECIFIC_WEATHER)
            .isThundering();
    }
    
    @Override
    public void setThundering(boolean thundering) {
        this.getSewPropertiesFor(SewConfig.WORLD_SPECIFIC_WEATHER)
            .setThundering(thundering);
    }
    
    @Override
    public int getRainTime() {
        return this.getSewPropertiesFor(SewConfig.WORLD_SPECIFIC_WEATHER)
            .getRainTime();
    }
    
    @Override
    public void setRainTime(int rainTime) {
        this.getSewPropertiesFor(SewConfig.WORLD_SPECIFIC_WEATHER)
            .setRainTime(rainTime);
    }
    
    @Override
    public int getThunderTime() {
        return this.getSewPropertiesFor(SewConfig.WORLD_SPECIFIC_WEATHER)
            .getThunderTime();
    }
    
    @Override
    public void setThunderTime(int thunderTime) {
        this.getSewPropertiesFor(SewConfig.WORLD_SPECIFIC_WEATHER)
            .setThunderTime(thunderTime);
    }
    
    @Override
    public int getClearWeatherTime() {
        return this.getSewPropertiesFor(SewConfig.WORLD_SPECIFIC_WEATHER)
            .getClearWeatherTime();
    }
    
    @Override
    public void setClearWeatherTime(int clearWeatherTime) {
        this.getSewPropertiesFor(SewConfig.WORLD_SPECIFIC_WEATHER)
            .setClearWeatherTime(clearWeatherTime);
    }
    
    @Override
    public boolean isHardcore() {
        return this.accessor
            .isHardcore();
    }
    
    @Override
    public Difficulty getDifficulty() {
        return this.accessor
            .getDifficulty();
    }
    
    @Override
    public boolean isDifficultyLocked() {
        return this.accessor
            .isDifficultyLocked();
    }
    
    @Override
    public GameRules getGameRules() {
        return this.getSewPropertiesFor(SewConfig.WORLD_SPECIFIC_GAME_RULES)
            .getGameRules();
    }
    
    @Override
    public String getLevelName() {
        return this.accessor.getLevelName();
    }
    
    @Override
    public int getWanderingTraderSpawnDelay() {
        return this.accessor.getWanderingTraderSpawnDelay();
    }
    
    @Override
    public void setWanderingTraderSpawnDelay(int wanderingTraderSpawnDelay) {
        this.accessor.setWanderingTraderSpawnDelay(wanderingTraderSpawnDelay);
    }
    
    @Override
    public int getWanderingTraderSpawnChance() {
        return this.accessor.getWanderingTraderSpawnChance();
    }
    
    @Override
    public void setWanderingTraderSpawnChance(int wanderingTraderSpawnChance) {
        this.accessor.setWanderingTraderSpawnChance(wanderingTraderSpawnChance);
    }
    
    @Override
    public @Nullable UUID getWanderingTraderId() {
        return this.accessor.getWanderingTraderId();
    }
    
    @Override
    public void setWanderingTraderId(UUID uuid) {
        this.accessor.setWanderingTraderId(uuid);
    }
    
    @Override
    public GameMode getGameMode() {
        return (SewConfig.get(SewConfig.WORLD_SPECIFIC_GAMEMODE) ? this.dynamic : this.accessor)
            .getGameMode();
    }
    
    @Override
    public void setGameMode(GameMode gameMode) {
        (SewConfig.get(SewConfig.WORLD_SPECIFIC_GAMEMODE) ? this.dynamic : this.accessor)
            .setGameMode(gameMode);
    }
    
    @Override
    public void setWorldBorder(WorldBorder.Properties properties) {
        this.getSewPropertiesFor(SewConfig.WORLD_SPECIFIC_WORLD_BORDER)
            .setWorldBorder(properties);
    }
    
    @Override
    public WorldBorder.Properties getWorldBorder() {
        return this.getSewPropertiesFor(SewConfig.WORLD_SPECIFIC_WORLD_BORDER)
            .getWorldBorder();
    }
    
    @Override
    public boolean isInitialized() {
        return true;
    }
    
    @Override
    public void setInitialized(boolean initialized) {}
    
    @Override
    public boolean areCommandsAllowed() {
        return this.accessor.areCommandsAllowed();
    }
    
    @Override
    public Timer<MinecraftServer> getScheduledEvents() {
        return this.accessor.getScheduledEvents();
    }
    
    public NbtCompound writeToTag(DynamicRegistryManager registryManager) {
        if (SewConfig.get(SewConfig.WORLD_SEPARATE_PROPERTIES) && this.dynamic instanceof LevelProperties)
            return ((LevelProperties)this.dynamic).cloneWorldNbt(registryManager, null);
        return new NbtCompound();
    }
    
    public @NotNull ServerWorldProperties getDynamicPropertyHandler() {
        return this.dynamic;
    }
    
    private @NotNull ServerWorldProperties getSewPropertiesFor(@NotNull ConfigOption<Boolean> config) {
        return (SewConfig.get(config) ? this.getDynamicPropertyHandler() : this.accessor);
    }
}
