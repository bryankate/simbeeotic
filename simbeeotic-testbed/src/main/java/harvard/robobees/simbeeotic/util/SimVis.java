package harvard.robobees.simbeeotic.util;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;

import javax.swing.*;
import java.awt.*;

/**
 * Created with IntelliJ IDEA.
 * User: kar
 * Email: kar@eecs.harvard.edu
 * Date: 5/28/13
 * Time: 4:55 PM
 *
 * Generic visualization for heli and other testbed vehicles.
 *
 */


public class SimVis extends ApplicationFrame {
    private static final SimVis INSTANCE = new SimVis("Heli Visualization");
    private Box grids = Box.createHorizontalBox();
    private XYSeriesCollection controls, flow, aggregate;
    public static enum PlotType {CONTROLS, FLOW, AGGREGATE};


    public static final SimVis getInstance() {
        return INSTANCE;
    }

    private SimVis(final String title) {
        super(title);
        // Add controls
        // Add flow and aggregates using addFlow and addAggregate
        // This is because the controls are fixed, but flow and aggregates
        // may vary.
        final JFreeChart mainChart = createCombinedChart();
        final ChartPanel mainPanel = new ChartPanel(mainChart, true, true, true, false, true);
        mainPanel.setPreferredSize(new Dimension(700, 1000));
        setContentPane(mainPanel);
        pack();
        setLocationRelativeTo(null);
        //setLocation();
        setVisible(true);
    }

    JFreeChart createCombinedChart() {
        CombinedDomainXYPlot mainPlot;

        controls  = new XYSeriesCollection();
        controls.addSeries(new XYSeries("thrust"));
        controls.addSeries(new XYSeries("roll"));
        controls.addSeries(new XYSeries("pitch"));
        controls.addSeries(new XYSeries("yaw"));
        final XYItemRenderer r1 = new StandardXYItemRenderer();
        final NumberAxis cAxis = new NumberAxis("Control");
        final XYPlot cPlot = new XYPlot(controls, null, cAxis, r1);
        cPlot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);

        flow      = new XYSeriesCollection();
        final XYItemRenderer r2 = new StandardXYItemRenderer();
        final NumberAxis fAxis = new NumberAxis("Flow");
        final XYPlot fPlot = new XYPlot(flow, null, fAxis, r2);
        fPlot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);

        aggregate = new XYSeriesCollection();
        final XYItemRenderer r3 = new StandardXYItemRenderer();
        final NumberAxis aAxis = new NumberAxis("Aggregate Info");
        final XYPlot aPlot = new XYPlot(aggregate, null, aAxis, r3);
        aPlot.setRangeAxisLocation(AxisLocation.TOP_OR_LEFT);

        NumberAxis xaxis = new NumberAxis("Frame count");
        xaxis.setAutoRange(true);
        mainPlot = new CombinedDomainXYPlot(xaxis);
        mainPlot.setGap(10.0);
        mainPlot.add(cPlot, 1);
        mainPlot.add(fPlot, 1);
        mainPlot.add(aPlot, 1);
        mainPlot.setOrientation(PlotOrientation.VERTICAL);

        return new JFreeChart("Flow Visualization", JFreeChart.DEFAULT_TITLE_FONT, mainPlot, true);
    }

    JFreeChart createChart(final XYDataset d) {
        final JFreeChart c = ChartFactory.createXYLineChart("Control Commands", "frame count", "control", d, PlotOrientation.VERTICAL, true, true, false);

        final XYPlot p = c.getXYPlot();
        ValueAxis axis = p.getDomainAxis();
        axis.setAutoRange(true);
        axis = p.getRangeAxis();
        axis.setRange(-1.0, 1.0);

        return c;

    }

    public void addFlow(String name) {
        flow.addSeries(new XYSeries(name));
    }

    public void addAggregate(String name) {
        aggregate.addSeries(new XYSeries(name));
    }

    public void addDataPoint(PlotType p, String plotSeries, double x, double y) {
        XYSeries s;
        switch(p) {
            case CONTROLS:
                s = controls.getSeries(plotSeries);
                if(s != null)
                    s.add(x, y);
                break;

            case FLOW:
                s = flow.getSeries(plotSeries);
                if(s != null)
                    s.add(x, y);
                break;

            case AGGREGATE:
                s = aggregate.getSeries(plotSeries);
                if(s != null)
                    s.add(x, y);
        }
    }
    
    public void testVis() {

        controls.getSeries("thrust").add(1, 0.5);
        controls.getSeries("thrust").add(2, 0.55);
        controls.getSeries("thrust").add(3, 0.6);
        controls.getSeries("thrust").add(4, 0.65);
        controls.getSeries("thrust").add(5, 0.7);

        controls.getSeries("roll").add(1, 0.3);
        controls.getSeries("roll").add(2, 0.35);
        controls.getSeries("roll").add(3, 0.4);
        controls.getSeries("roll").add(4, 0.45);
        controls.getSeries("roll").add(5, 0.5);

        controls.getSeries("pitch").add(1, 0.1);
        controls.getSeries("pitch").add(2, 0.15);
        controls.getSeries("pitch").add(3, 0.2);
        controls.getSeries("pitch").add(4, 0.25);
        controls.getSeries("pitch").add(5, 0.3);

        controls.getSeries("yaw").add(1, 0.7);
        controls.getSeries("yaw").add(2, 0.75);
        controls.getSeries("yaw").add(3, 0.8);
        controls.getSeries("yaw").add(4, 0.85);
        controls.getSeries("yaw").add(5, 0.9);

//        for(int i=0; i < 16; i++)
//            addFlow("process" + i);
//
//        addAggregate("testag");
//

//        for(int j=0; j < 5; j++) {
//            for(int i=0; i < 16; i++) {
//                flow.getSeries("process"+i).add(j, 0.1 + j * 0.1 + i*0.02);
//            }
//
//            aggregate.getSeries("testag").add(j, 0.4 + 0.1*j);
//        }

        setVisible(true);


    }
}