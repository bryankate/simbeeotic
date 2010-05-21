package harvard.robobees.simbeeotic.util;


import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;


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

    private String plotParams = "u 1:2 w l";
    private StringBuffer data = new StringBuffer();

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


    public void setProperty(String property, String arguments) {
        inject("set " + property + " " + arguments);
    }


    public void unsetProperty(String property) {
        inject("unset " + property);
    }


    public void setPlotParams(String params) {
        this.plotParams = params;
    }


    public void plot() {

        if (plotParams.isEmpty() || (data.length() == 0)) {

            logger.error("Cannot plot without data and plot parameters!");
            throw new RuntimeException("Data or plotting parameters not set.");
        }

        inject("plot '-' " + plotParams + "\n" + data.toString() + "e");
    }


    public void splot() {

        if (plotParams.isEmpty() || (data.length() == 0)) {

            logger.error("Cannot splot without data and plot parameters!");
            throw new RuntimeException("Data or plotting parameters not set.");
        }

        inject("splot '-' " + plotParams + "\n" + data.toString() + "e");
    }


    public void addDataPoint(String line) {
        data.append(line).append("\n");
    }


    public void setData(String lines) {

        clearData();
        data.append(lines).append("\n");
    }


    public void clearData() {
        data = new StringBuffer();
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
