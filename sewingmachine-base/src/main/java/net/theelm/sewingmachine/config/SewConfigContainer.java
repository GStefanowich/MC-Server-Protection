package net.theelm.sewingmachine.config;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public abstract class SewConfigContainer {
    // All config options
    protected final List<ConfigBase<?>> configOptions = new ArrayList<>();
    
    // Config options that can be synced with the client
    protected final List<ConfigBase<?>> syncedOptions = new ArrayList<>();
    
    protected JsonObject saveToJSON() {
        final JsonObject baseObject = new JsonObject();
        
        for ( ConfigBase<?> config : this.configOptions ) {
            JsonObject inner = baseObject;
            
            String[] path = config.getPath().split("\\.");
            
            int p;
            for ( p = 0; p < ( path.length - 1 ); p++ ) {
                String seg = path[p];
                
                if (inner.has(seg) && inner.get(seg).isJsonObject()) {
                    inner = inner.getAsJsonObject(seg);
                } else {
                    JsonObject segObj = new JsonObject();
                    inner.add( seg, segObj );
                    inner = segObj;
                }
            }
            
            inner.add(path[p], config.getElement());
        }
        
        return baseObject;
    }
    
}
