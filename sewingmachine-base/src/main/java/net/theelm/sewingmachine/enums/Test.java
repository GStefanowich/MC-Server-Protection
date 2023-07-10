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

package net.theelm.sewingmachine.enums;

import net.minecraft.util.ActionResult;
import org.jetbrains.annotations.NotNull;

/**
 * Created on Jul 10 2023 at 1:28 AM.
 * By greg in sewingmachine
 */
public enum Test {
    /**
     * The Test is a SUCCESS, no more Tests need to be run
     */
    SUCCESS(ActionResult.SUCCESS, true),
    
    /**
     * The current Test is inconclusive and has been CONTINUED to the next Test
     */
    CONTINUE(ActionResult.PASS, false),
    
    /**
     * The Test has FAILED, no more Tests need to be run
     */
    FAIL(ActionResult.FAIL, true);
    
    private final @NotNull ActionResult actionResult;
    
    private final boolean conclusive;
    
    Test(@NotNull ActionResult equivalent, boolean conclusive) {
        this.actionResult = equivalent;
        this.conclusive = conclusive;
    }
    
    public @NotNull ActionResult toResult() {
        return this.actionResult;
    }
    
    public boolean isConclusive() {
        return this.conclusive;
    }
}
