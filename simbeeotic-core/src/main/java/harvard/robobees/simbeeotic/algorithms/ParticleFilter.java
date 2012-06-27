package harvard.robobees.simbeeotic.algorithms;

import Jama.Matrix;

public interface ParticleFilter{

    public double[] sense(double xNoisy, double yNoisy, double headingNoisy);

    public void initialize();
    public Matrix generateParticles(int numberOfParticles);
    public Matrix moveParticles(Matrix particles, double rotation, double distance);
    public double[] measureProb(Matrix particles, double[] z);
    public double gaussian(double mu, double sigma, double x);
    public Matrix resample(Matrix particles, double[] measurementProbability);

}
