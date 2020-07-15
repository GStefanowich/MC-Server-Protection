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

package net.TheElm.project.enums;

/**
 * The different Op-Level numerators for testing permissions, and which commands they can use.
 * CommandBlocks are OP-LEVEL 2
 */
public final class OpLevels {
    private OpLevels() {}
    
    /**
     * Allow bypassing of spawn protections
     */
    public static final int SPAWN_PROTECTION = 1;
    
    /**
     * Allows using the following commands:
     *   /give
     *   /clear
     *   /effect
     *   /gamemode
     *   /gamerule
     *   /give
     *   /summon
     *   /setblock
     */
    public static final int CHEATING = 2;
    
    /**
     * Allows using the following commands:
     *   /kick
     *   /ban
     *   /op
     *   /deop
     */
    public static final int KICK_BAN_OP = 3;
    
    /**
     * Allows using the following commands:
     *   /stop
     */
    public static final int STOP = 4;
}
