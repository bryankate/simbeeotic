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
    Gnuplotter2 p = new Gnuplotter2(true);
    private static Logger logger = Logger.getLogger(DefaultParticleFilter.class);
    public Random rand = new Random();

    public double sensorNoise = 5;
    public double forwardNoise = .05;
    public double turnNoise = .05;





    public Matrix landmarks = new Matrix(new double[][] {{20, 20},
            {80,80},
            {20,80},
            {80,20}});



    public void initialize(){
        p.setProperty("term", "x11");
        p.unsetProperty("key");
        p.setProperty("title", "'Distance to nearest object (0 = infinity or zero)'");
        p.setPlotParams("with points");
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
        return Math.exp(-(Math.pow(mu-x,2))/Math.pow(sigma,2))/Math.sqrt(2*Math.PI*Math.pow(sigma,2));
    }

    public Matrix resample(Matrix particles, double[] measurementProbability, Vector3f pos){
        int m = particles.getRowDimension();
        int n = particles.getColumnDimension();
        Matrix newParticles = new Matrix(m,n);
        double[][] particleMap = new double[100][100];

        int index = (int) (Math.random() * m);
        double beta = 0;
        double mw = 0;
        for (int i = 0; i<m; i++){
            if (measurementProbability[i]>mw){
                mw = measurementProbability[i];
            }
        }
        for (int i=0; i<m; i++){
            beta += Math.random()*2*mw;
            while (beta>measurementProbability[index]){
                beta -= measurementProbability[index];
                index = (index+1)%m;
            }
            for (int j=0; j<n; j++){
                newParticles.set(i,j,particles.get(index,j ));
            }

        }
        for (int i = 0; i<m; i++){
            int xParticle = (int)Math.round(newParticles.get(i,0))%100;
            int yParticle = (int)Math.round(newParticles.get(i,1))%100;
            particleMap[xParticle][yParticle] = 1;
        }
        heatMap.setDataBlock(particleMap, pos);
        return newParticles;
    }
}
