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


import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.io.File;
import java.io.FileWriter;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;


/**
 * A very quick and dirty gnuplot wrapper. For a better approach to standard 2D
 * plots, you should really use JFreeChart or the gnuplot Java wrapper jgnuplot.
 *
 * This class primarily exists so we can output a basic 3D surface from sim data
 * at runtime.
 * 
 * @author bkate
 */
public class Gnuplotter {

    private Process process;
    private Writer out;

    private Map<String, String> plotParams = new HashMap<String, String>();
    private Map<String, StringBuffer> plotData = new TreeMap<String, StringBuffer>();
    private StringBuffer commandLog = new StringBuffer();
    private String lastPlotCommand = "";

    private static Gnuplotter instance;

    static {
        instance = new Gnuplotter();
    }

    private static final String DEFAULT_PLOT = "";


    private static Logger logger = Logger.getLogger(Gnuplotter.class);


    public Gnuplotter() {

        ProcessBuilder pb = new ProcessBuilder("gnuplot");

        try {

            process = pb.start();
            out = new OutputStreamWriter(process.getOutputStream());
        }
        catch(IOException ioe) {

            logger.error("Could not start gnuplot process.", ioe);
            throw new RuntimeException("Could not start gnuplot process.", ioe);
        }
    }

    
    public static Gnuplotter getGlobalInstance() {
        return instance;
    }


    public void setProperty(String property, String arguments) {
        inject("set " + property + " " + arguments);
    }


    public void setTextProperty(String property, String arguments) {
        inject("set " + property + " \"" + arguments + "\"");
    }


    public void unsetProperty(String property) {
        inject("unset " + property);
    }


    public void setPlotParams(String params) {
        plotParams.put(DEFAULT_PLOT, params);
    }


    public void setPlotParams(String plotKey, String params) {
        plotParams.put(plotKey, params);
    }


    public void plot() {
        doPlot("plot");
    }


    public void splot() {
        doPlot("splot");
    }


    private void doPlot(String plotType) {

        StringBuffer plotCmd = new StringBuffer(plotType).append(" ");
        StringBuffer allData = new StringBuffer();
        boolean firstPlot = true;

        for (Map.Entry<String, StringBuffer> p : plotData.entrySet()) {

            String plotKey = p.getKey();
            StringBuffer data = p.getValue();
            String params = plotParams.get(plotKey);

            if ((params == null) || params.isEmpty()) {

                logger.error("Cannot plot without plot parameters!");
                throw new RuntimeException("Plotting parameters not set.");
            }


            if (!firstPlot) {
                plotCmd.append(", ");
            }

            plotCmd.append("'-' ").append(params);
            allData.append(data).append("\ne\n");

            firstPlot = false;
        }

        lastPlotCommand = plotCmd.append("\n").append(allData).toString();

        inject(lastPlotCommand, true);
    }


    public void addDataPoint(String line) {
        addDataPoint(DEFAULT_PLOT, line);
    }


    public void addDataPoint(String plotKey, String line) {

        if (!plotData.containsKey(plotKey)) {
            plotData.put(plotKey, new StringBuffer());
        }

        plotData.get(plotKey).append(line).append("\n");
    }


    public void setData(String lines) {
        setData(DEFAULT_PLOT, lines);
    }


    public void setData(String plotKey, String lines) {

        clearData(plotKey);
        plotData.put(plotKey, new StringBuffer(lines).append("\n"));
    }


    public void clearData() {
        clearData(DEFAULT_PLOT);
    }


    public void clearData(String plotKey) {
        plotData.remove(plotKey);
    }


    public void shutdown() {

        inject("exit");
        process.destroy();
    }


    public void writeLog(String outFile) {

        try {

            File file = new File(outFile);
            file.getParentFile().mkdirs();

            FileWriter writer = new FileWriter(file);

            writer.write(commandLog.toString());
            writer.write(lastPlotCommand);
            writer.flush();
            writer.close();
        }
        catch(IOException ioe) {
            logger.error("Error writing Gnuplotter log.", ioe);
        }
    }


    private void inject(String cmd) {
        inject(cmd, false);
    }


    private void inject(String cmd, boolean isData) {

        try {

            if (!isData) {
                commandLog.append(cmd.trim()).append("\n");
            }

            out.write(cmd.trim() + "\n");
            out.flush();
        }
        catch(IOException ioe) {
            logger.error("IO Error", ioe);
        }
    }
}
