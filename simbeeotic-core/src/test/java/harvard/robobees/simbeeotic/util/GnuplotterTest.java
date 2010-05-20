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
