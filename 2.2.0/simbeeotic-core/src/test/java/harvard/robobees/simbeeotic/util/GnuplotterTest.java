package harvard.robobees.simbeeotic.util;


import junit.framework.TestCase;


/**
 * @author bkate
 */
public class GnuplotterTest extends TestCase {

    private String lineData = "1 1 \n" +
                              "2 2 \n" +
                              "3 3 \n" +
                              "4 4 \n" +
                              "5 5";

    
    public void testPlotting() {

        Gnuplotter plot = new Gnuplotter();

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
