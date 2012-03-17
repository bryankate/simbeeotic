/*
 * Copyright (c) 2012, The President and Fellows of Harvard College.
 * All Rights Reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *  3. Neither the name of the University nor the names of its contributors
 *     may be used to endorse or promote products derived from this software
 *     without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE UNIVERSITY AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE UNIVERSITY OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package harvard.robobees.simbeeotic.model;


import java.util.concurrent.TimeUnit;


/**
 * A factory for generating timers.
 *
 * @author bkate
 */
public interface TimerFactory {

    /**
     * Creates a new timer that will fire at a future point in time.
     *
     * @param callback The callback that will be made when the timer fires.
     * @param offset The offset from the current time at which the timer should fire. This
     *               value must be greater than or equal to zero.
     * @param offsetUnit The time unit in which the offset is measured.
     *
     * @return The timer object, which can be used to reset or cancel the firing.
     */
    public Timer createTimer(TimerCallback callback, long offset, TimeUnit offsetUnit);


    /**
     * Creates a new timer that will fire at a periodic rate starting at a future point in time.
     *
     * @param callback The callback that will be made when the timer fires.
     * @param offset The offset from the current time at which the timer should fire. This
     *               value must be greater than or equal to zero.
     * @param offsetUnit The time unit in which the offset is measured.
     * @param period The rate at which this timer fires. If the value is less than or equal to zero
     *               then the behavior is identical to {@link #createTimer(TimerCallback, long, java.util.concurrent.TimeUnit)}
     * @param periodUnit The time unit in which the period is measured.
     *
     * @return The timer object, which can be used to reset or cancel the firing.
     */
    public Timer createTimer(TimerCallback callback, long offset, TimeUnit offsetUnit, long period, TimeUnit periodUnit);
}
