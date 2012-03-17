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


import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.DefaultXYDataset;

import javax.swing.*;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;


/**
 * A simple utility that allows a user to plot two-dimensional data. The class is meant to be used
 * as a debugging tool, not a finished display tool.
 *
 * @author bkate
 */
public class TracePlotter2D {

    private Map<String, Trace> traces = new HashMap<String, Trace>();

    private DefaultXYDataset data = new DefaultXYDataset();
    private XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
    private JFreeChart chart;
    private JFrame frame;

    private static TracePlotter2D instance;


    public TracePlotter2D() {
        this("", "", "");
    }


    /**
     * Makes a new tracer with the given labels.
     *
     * @param title The plot title.
     * @param xLabel The plot X label.
     * @param yLabel The plot Y label.
     */
    public TracePlotter2D(String title, String xLabel, String yLabel) {

        chart = ChartFactory.createXYLineChart(title, xLabel, yLabel,
                                               data, PlotOrientation.VERTICAL,
                                               false, false, false);

        chart.getXYPlot().setRenderer(renderer);

        frame = new JFrame();

        frame.setContentPane(new ChartPanel(chart));
        frame.setSize(500, 500);
        frame.setVisible(true);
    }


    /**
     * Gets the global instance of the trace plotter.
     *
     * @return The static tracer instance.
     */
    public static TracePlotter2D getGlobalInstance() {

        if (instance == null) {
            instance = new TracePlotter2D();
        }

        return instance;
    }


    /**
     * Adds a data point to the series with the given key.
     *
     * @param key The key that identifies the series to which the data is being added.
     * @param x The X value.
     * @param y The Y value.
     */
    public void addData(final String key, double x, double y) {

        makeSeries(key);

        final Trace t = traces.get(key);

        t.addData(x, y);

        try {

            SwingUtilities.invokeAndWait(new Runnable() {

                public void run() {

                    // this is really inefficient with high churn - look into making a custom dataset
                    data.addSeries(key, t.toSeries());
                }
            });
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Sets a limit on how many data points to retain for a series. The oldest points will be
     * discarded as new data arrives and the size exceeds the limit.
     *
     * @param key The key that identifies the series to which the data is being added.
     * @param cap The number of item to allow in the series.
     */
    public void setCap(String key, int cap) {

        makeSeries(key);

        traces.get(key).setCap(cap);
    }


    public void setPlotLines(String key, boolean plotLines) {

        makeSeries(key);

        int index = data.indexOf(key);

        if (index >= 0) {
            renderer.setSeriesLinesVisible(index, plotLines);
        }
    }


    public void setPlotLines(boolean plotLines) {

        renderer.setBaseLinesVisible(plotLines);

        for (int s = 0; s < data.getSeriesCount(); s++) {
            renderer.setSeriesLinesVisible(s, plotLines);
        }
    }


    public void setPlotPoints(String key, boolean plotPoints) {

        makeSeries(key);

        int index = data.indexOf(key);

        if (index >= 0) {
            renderer.setSeriesShapesVisible(index, plotPoints);
        }
    }


    public void setPlotPoints(boolean plotPoints) {

        renderer.setBaseShapesVisible(plotPoints);
        
        for (int s = 0; s < data.getSeriesCount(); s++) {
            renderer.setSeriesShapesVisible(s, plotPoints);
        }
    }


    public void setTitle(String title) {
        chart.setTitle(title);
    }


    public void setXLabel(String label) {
        chart.getXYPlot().getRangeAxis().setLabel(label);
    }


    public void setYLabel(String label) {
        chart.getXYPlot().getDomainAxis().setLabel(label);
    }


    /**
     * Breaks down the plot, including the window. This is required in order to exit
     * cleanly from the program (the AWT threads are still running).
     */
    public void dispose() {
        frame.dispose();
    }


    private void makeSeries(String key) {

        if (!traces.containsKey(key)) {
            
            traces.put(key, new Trace());
            data.addSeries(key, new double[][] {{},{}});
        }
    }


    /**
     * A simple container for trace data.
     */
    private static class Trace {

        private int cap = Integer.MAX_VALUE;
        private List<Double> xs = new LinkedList<Double>();
        private List<Double> ys = new LinkedList<Double>();


        public void addData(double x, double y) {

            xs.add(x);
            ys.add(y);

            trim();
        }


        public double[][] toSeries() {

            double[][] vals = new double[2][xs.size()];

            for (int i = 0; i < xs.size(); i++) {

                vals[0][i] = xs.get(i);
                vals[1][i] = ys.get(i);
            }

            return vals;
        }

        
        public void setCap(int items) {

            this.cap = items;

            trim();
        }


        private void trim() {


            while (xs.size() > cap) {
                xs.remove(0);
            }

            while (ys.size() > cap) {
                ys.remove(0);
            }
        }
    }

}
