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

package harvard.robobees.simbeeotic.algorithms;


import Jama.Matrix;
import harvard.robobees.simbeeotic.util.Gnuplotter2;
import harvard.robobees.simbeeotic.util.HeatMap;
import org.apache.log4j.Logger;

import javax.vecmath.Vector3f;
import java.util.Random;

public class DefaultParticleFilter implements ParticleFilter {


    //robot is initialized in random xyheading.
    //robot turns .1rad and moves 5m
    //robot senses
    //robot.sense gives distance to the four landmarks

    private HeatMap heatMap = new HeatMap();
    //Gnuplotter2 p = new Gnuplotter2(true);
    private static Logger logger = Logger.getLogger(DefaultParticleFilter.class);
    public Random rand = new Random();

    public double sensorNoise = 5;
    public double forwardNoise = .05;
    public double turnNoise = .05;

    int leftPoint = 0;
    int rightPoint=0;



    public Matrix landmarks = new Matrix(new double[][] {{20, 20},
    //        {80,80},
    //        {20,80},
            {80,20}});

    //public Matrix landmarks = new Matrix(new double[][] {{10, 10}, {90,90}});


    public void initialize(){
        //p.setProperty("term", "x11");
        //p.unsetProperty("key");
        //p.setProperty("title", "'Distance to nearest object (0 = infinity or zero)'");
        //p.setPlotParams("with points");
        heatMap.initialize();
    }

    public double[] sense(double xNoisy, double yNoisy, double headingNoisy) {
        double[] z = new double[landmarks.getRowDimension()];
        for (int i = 0; i<landmarks.getRowDimension(); i++){
            double dist = Math.sqrt(Math.pow(xNoisy-landmarks.get(i,0),2) + Math.pow(yNoisy-landmarks.get(i,1),2));
            dist += rand.nextGaussian() * sensorNoise;
            z[i] = dist;
        }
        return z;
    }


    public Matrix generateParticles(int numberOfParticles){
        double[][] particleMap = new double[100][100];
        double[][] particles = new double[numberOfParticles][3];
        for (int i = 0; i<numberOfParticles; i++){
            particles[i][0] = Math.random()*100;
            particles[i][1] = Math.random()*100;
            particles[i][2] = Math.random()*Math.PI*2;

            int xParticle = (int)Math.round(particles[i][0])%100;
            int yParticle = (int)Math.round(particles[i][1])%100;
            particleMap[xParticle][yParticle] = 1;

        }

        return new Matrix(particles);
    }

    public Matrix moveParticles(Matrix particles, double rotation, double distance){
        for (int i=0; i<particles.getRowDimension(); i++){
            double theta = particles.get(i,2) + rotation + rand.nextGaussian()*turnNoise;
            theta %= 2*Math.PI;
            double dist = distance + rand.nextGaussian()*forwardNoise;
            double x = particles.get(i,0) + Math.cos(theta)*dist;
            double y = particles.get(i,1) + Math.sin(theta)*dist;
            x %= 100;
            y %= 100;
            particles.set(i,0,x);
            particles.set(i,1,y);
            particles.set(i,2,theta);
        }
        return particles;
    }

    public double[] measureProb(Matrix particles, double[] z){
        double[] w = new double[particles.getRowDimension()];
        for (int h = 0; h<particles.getRowDimension(); h++){
            double prob = 1;
            for (int i = 0; i<landmarks.getRowDimension(); i++){
                double dist = Math.sqrt(Math.pow(particles.get(h,0)-landmarks.get(i,0),2) + Math.pow(particles.get(h,1)-landmarks.get(i,1),2));
                prob *= gaussian(dist, sensorNoise, z[i]);
            }
            w[h] = prob;
        }
        return w;
    }

    public double gaussian(double mu, double sigma, double x){
        return Math.exp(-.5*(Math.pow(mu-x,2))/Math.pow(sigma,2))/Math.sqrt(2*Math.PI*Math.pow(sigma,2));
    }

    public Matrix resample(Matrix particles, double[] w, Vector3f pos){
        leftPoint=0;
        rightPoint=0;

        int m = particles.getRowDimension();
        int n = particles.getColumnDimension();
        Matrix newParticles = new Matrix(m,n);
        double[][] particleMap = new double[100][100];

        int index = (int) (Math.random() * m);
        double beta = 0;
        double mw = 0;
        for (int i = 0; i<m; i++){
            if (w[i]>mw){
                mw = w[i];
            }
        }
        for (int i=0; i<m; i++){
            beta += Math.random()*2*mw;
            while (beta>w[index]){
                beta -= w[index];
                index = (index+1)%m;
            }
            for (int j=0; j<n; j++){
                newParticles.set(i,j,particles.get(index,j));
            }

        }
        for (int i = 0; i<m; i++){

            int xParticle = (int)Math.round(newParticles.get(i,0))%100;
            int yParticle = (int)Math.round(newParticles.get(i,1))%100;
            if (xParticle <0){
                xParticle = 0;
            }
            if (yParticle <0){
                yParticle = 0;
            }
            if (yParticle<50){
                leftPoint++;
            }
            if (yParticle>50){
                rightPoint++;
            }

            particleMap[xParticle][yParticle] = 1;
        }
        heatMap.setDataBlock(particleMap, pos);
        logger.info("left points:" + leftPoint + "right points:" + rightPoint);
        return newParticles;
    }
}
