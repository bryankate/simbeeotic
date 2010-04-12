package harvard.robobees.simbeeotic.util;


import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;
import java.util.HashMap;


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
    private Map<String, StringBuffer> plotData = new HashMap<String, StringBuffer>();

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

        plotCmd.append("\n").append(allData);
        inject(plotCmd.toString());
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
        setData(DEFAULT_PLOT);
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


    private void inject(String cmd) {

        try {
            out.write(cmd.trim() + "\n");
            out.flush();
        }
        catch(IOException ioe) {
            logger.error("IO Error", ioe);
        }
    }
}
