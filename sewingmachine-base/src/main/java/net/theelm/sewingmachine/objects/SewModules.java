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

package net.theelm.sewingmachine.objects;

import org.jetbrains.annotations.NotNull;

public final class SewModules {
    private SewModules() {}

    /**
     * The name of the module
     */
    public static final @NotNull String MODULE = "sewing-machine";
    
    /**
     * The prefix used for all submodules
     */
    public static final @NotNull String MOD_PREFIX = MODULE + "-";
    
    /**
     * Base module with all necessary libraries
     */
    public static final @NotNull String BASE = MOD_PREFIX + "base";
    
    /**
     * Chat module for formatting chat
     */
    public static final @NotNull String CHAT = MOD_PREFIX + "chat";
    
    /**
     * Custom module for some custom implementations
     */
    public static final @NotNull String CUSTOM = MOD_PREFIX + "custom";
    
    /**
     * Deathchests module for handling death chests
     */
    public static final @NotNull String DEATHCHESTS = MOD_PREFIX + "deathchests";
    
    /**
     * Protection module for world claims
     */
    public static final @NotNull String PROTECTION = MOD_PREFIX + "protection";
}
