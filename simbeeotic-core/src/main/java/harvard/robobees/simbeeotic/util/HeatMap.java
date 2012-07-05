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


import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYDataImageAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.GrayPaintScale;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.DefaultXYZDataset;

import javax.swing.*;
import javax.vecmath.Vector3f;
import java.awt.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * A simple utility that allows a user to plot two-dimensional data. The class is meant to be used
 * as a debugging tool, not a finished display tool.
 *
 * @author Mburkardt
 */
public class HeatMap extends JFrame {

    private XYPlot mapPlot;


    public void initialize(){

        XYBlockRenderer mapRenderer = new XYBlockRenderer();
        NumberAxis mapXAxis = new NumberAxis("X");
        NumberAxis mapYAxis = new NumberAxis("Y");
        mapPlot = new XYPlot(null, mapXAxis, mapYAxis, mapRenderer);

        JFreeChart mapChart = new JFreeChart(mapPlot);
        mapChart.removeLegend();

        PaintScale mapScale = new InvertedGrayPaintScale(0,1);

        mapRenderer.setPaintScale(mapScale);
        setContentPane(new ChartPanel(mapChart));
        setVisible(true);
        mapPlot.setDomainCrosshairVisible(true);
        mapPlot.setRangeCrosshairVisible(true);

        Dimension size = new Dimension(400,400);
        setSize(size);
    }

    public void setDataBlock(float[][] data, Vector3f currPos){

        int rows = data.length;
        int cols = data[0].length;
        double [][] converted = new double[3][rows*cols];

        for (int i=0; i<rows; i++){
            for (int j=0; j<cols; j++){
                converted[0][i*cols+j] = j;
                converted[1][i*cols+j] = i;
                converted[2][i*cols+j] = data[i][j];
            }
        }
        DefaultXYZDataset dataset = new DefaultXYZDataset();
        dataset.addSeries(0,converted);

        mapPlot.setDataset(dataset);


        mapPlot.setDomainCrosshairValue(currPos.y+99);
        mapPlot.setRangeCrosshairValue(currPos.x+99);


    }

    public void setDataBlock(double[][] data, Vector3f currPos){

        int rows = data.length;
        int cols = data[0].length;
        double [][] converted = new double[3][rows*cols];

        for (int i=0; i<rows; i++){
            for (int j=0; j<cols; j++){
                converted[0][i*cols+j] = j;
                converted[1][i*cols+j] = i;
                converted[2][i*cols+j] = data[i][j];
            }
        }
        DefaultXYZDataset dataset = new DefaultXYZDataset();
        dataset.addSeries(0,converted);

        mapPlot.setDataset(dataset);
        mapPlot.setDomainCrosshairValue(currPos.y);
        mapPlot.setRangeCrosshairValue(currPos.x);

        //mapPlot.setDomainCrosshairValue(currPos.y+99);
        //mapPlot.setRangeCrosshairValue(currPos.x+99);


    }



    private static class InvertedGrayPaintScale implements PaintScale {

        private double lower = 0;
        private double upper = 1;


        public InvertedGrayPaintScale() {
        }


        public InvertedGrayPaintScale(double lower, double upper) {

            this.lower = lower;
            this.upper = upper;
        }


        @Override
        public double getLowerBound() {
            return lower;
        }


        @Override
        public double getUpperBound() {
            return upper;
        }


        @Override
        public Paint getPaint(double v) {

            if (v < lower) {
                v = lower;
            }

            if (v > upper) {
                v = upper;
            }

            double ratio = v / (upper - lower);
            int grayVal = (int)(255 - (ratio * 255));

            return new Color(grayVal, grayVal, grayVal);
        }
    }

}


