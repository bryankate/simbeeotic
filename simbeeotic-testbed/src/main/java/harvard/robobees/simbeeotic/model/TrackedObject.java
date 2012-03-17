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


import com.google.inject.Inject;
import com.google.inject.name.Named;
import harvard.robobees.simbeeotic.SimTime;
import harvard.robobees.simbeeotic.configuration.ConfigurationAnnotations;
import org.apache.log4j.Logger;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.TimeUnit;


/**
 * A debugging model that outputs its position and orientation (as tracked in Vicon)
 * to a file for debugging purposes.
 *
 * @author bkate
 */
public class TrackedObject extends ViconObject {

    private Writer out;

    // params
    private long updateRate = 33;  // ms


    private static Logger logger = Logger.getLogger(TrackedObject.class);


    @Override
    public void initialize() {

        super.initialize();

        try {
            out = new FileWriter(getName() + ".log");
        }
        catch(IOException ioe) {
            throw new RuntimeException(ioe);
        }

        // data recording timer
        createTimer(new TimerCallback() {

            @Override
            public void fire(SimTime time) {

                Vector3f pos = getTruthPosition();
                Quat4f orient = getTruthOrientation();

                logger.debug("time: " + time.getTime() + "  pos: " + pos + "  orient: " + orient);

                try {

                    out.write(time.getTime() + " " + pos.x + " " + pos.y + " " + pos.z + " " +
                              orient.x + " " + orient.y + " " + orient.z + " " + orient.w + "\n");
                }
                catch(IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            }

        }, 0, TimeUnit.MILLISECONDS, updateRate, TimeUnit.MILLISECONDS);
    }


    @Override
    public void finish() {

        try {

            out.flush();
            out.close();
        }
        catch(IOException ioe) {
            throw new RuntimeException(ioe);
        }

        super.finish();
    }


    @Inject(optional=true)
    public final void setUpdateRate(@Named("update-rate") long rate) {
        this.updateRate = rate;
    }
}
