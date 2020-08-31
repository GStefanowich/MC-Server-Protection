package net.TheElm.project.config.defaults;

import com.google.gson.JsonElement;
import net.TheElm.project.config.ConfigOption;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ConfigInteger extends ConfigOption<Integer> {
    public ConfigInteger(@NotNull String location) {
        super(location, JsonElement::getAsInt);
    }
    public ConfigInteger(@NotNull String location, @Nullable Integer defaultValue) {
        super(location, defaultValue, JsonElement::getAsInt);
    }
}
