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

package net.theelm.sewingmachine.protection.interfaces;

import net.theelm.sewingmachine.protection.claims.ClaimantPlayer;
import net.theelm.sewingmachine.protection.objects.PlayerVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Created on Jun 09 2023 at 4:34 AM.
 * By greg in sewingmachine
 */
public interface PlayerTravel {
    /**
     * @return Get the Players claim information
     */
    ClaimantPlayer getClaim();

    /**
     * @return Get the Players current position (as a Claim)
     */
    @Nullable PlayerVisitor getLocation();
    
    /**
     * Set the Player as positioned in another Players claimed terrority
     * @param uuid The playerspace that the Player is in
     * @return The Players current position (as a Claim)
     */
    @NotNull PlayerVisitor updateLocation(@NotNull UUID uuid);

    /**
     * Set the Player as positioned in unclaimed territory
     * @return The Players current position (as a Claim)
     */
    @NotNull PlayerVisitor updateLocation();
}