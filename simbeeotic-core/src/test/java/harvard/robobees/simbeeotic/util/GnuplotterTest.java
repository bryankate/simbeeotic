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
package harvard.robobees.simbeeotic.util;


import junit.framework.TestCase;
import org.apache.log4j.Logger;


/**
 * @author bkate
 */
public class GnuplotterTest extends TestCase {

    private String lineData = "1 1 \n" +
                              "2 2 \n" +
                              "3 3 \n" +
                              "4 4 \n" +
                              "5 5";

    private static Logger logger = Logger.getLogger(GnuplotterTest.class);

    
    public void testPlotting() {


        Gnuplotter plot;

        try {
            plot = new Gnuplotter();
        }
        catch(Exception e) {

            // an exception is thrown if gnuplot cannot be found on the path
            logger.warn("Exception thrown when trying to setup plotter.", e);
            return;
        }

        plot.setProperty("term", "x11");
        plot.unsetProperty("key");
        plot.setProperty("title", "'Test Plot'");

        plot.setPlotParams("u 1:($2*10) w l");

        plot.setData(lineData);
        plot.plot();

        plot.clearData();
        plot.addDataPoint("1 1");
        plot.addDataPoint("2 2");
        plot.addDataPoint("3 3");
        plot.plot();
    }
}
