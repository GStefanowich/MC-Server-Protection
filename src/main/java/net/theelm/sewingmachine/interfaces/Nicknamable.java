package net.theelm.sewingmachine.interfaces;

import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public interface Nicknamable {
    
    void setPlayerNickname(@Nullable Text nickname);
    Text getPlayerNickname();
    
}
