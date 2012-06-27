package harvard.robobees.simbeeotic.model;


import Jama.Matrix;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import harvard.robobees.simbeeotic.SimTime;
import harvard.robobees.simbeeotic.algorithms.DefaultParticleFilter;
import harvard.robobees.simbeeotic.algorithms.ParticleFilter;
import harvard.robobees.simbeeotic.model.sensor.Compass;
import harvard.robobees.simbeeotic.model.sensor.LaserRangeSensor;
import org.apache.log4j.Logger;

import javax.vecmath.Vector3f;

public class ParticleBee extends SimpleBee{

    private Compass compass;
    private LaserRangeSensor laserRangeSensor;
    public ParticleFilter particleFilter = new DefaultParticleFilter();

    private float maxVel = 1f; //set max velocity to 3 m/s, so that entire map can be mapped.
    private float[] range = new float[181];
    public float beeTheta;

    private static Logger logger = Logger.getLogger(OccupancyBee.class);
    public boolean move = true;
    public Matrix particles;
    public double[] w;
    public double[] z;
    double xSigma = .1;
    double ySigma = .1;
    double headingSigma = .1;


    @Override
    public void initialize() {
        super.initialize();
        setHovering(true);  //make the bee hover at constant height
        setUseRandomStart(true);

        compass = getSensor("compass", Compass.class); //compass to find heading
        laserRangeSensor = getSensor("range-sensor", LaserRangeSensor.class); //laser range finder for occupancy mapping
        //particleFilter = new DefaultParticleFilter();


        particleFilter.initialize();

        Vector3f pos = getTruthPosition();
        double xNoisy = pos.x + getRandom().nextGaussian()*xSigma;
        double yNoisy = pos.y + getRandom().nextGaussian()*ySigma;
        logger.info("X for input: " + xNoisy + " Y for input: " + yNoisy);
        double headingNoisy = beeTheta + getRandom().nextGaussian()*headingSigma;

        //gives distance from the bee to the landmarks, with a little bit of noise.
        z = particleFilter.sense(xNoisy,yNoisy,headingNoisy);
        particles = particleFilter.generateParticles(1000);
        w = particleFilter.measureProb(particles, z);


    }


    @Override
    protected void updateKinematics(SimTime time) {
        Vector3f pos = getTruthPosition();
        Vector3f vel = getTruthLinearVelocity();




        beeTheta = compass.getHeading();

        logger.info("ID: " + getModelId() + "  " +
                "time: " + time.getImpreciseTime() + "  " +
                "pos: " + pos + "  " +
                "vel: " + vel + " ");


        if (move == false){
            setDesiredLinearVelocity(new Vector3f(0,0,0));
            double xNoisy = pos.x + getRandom().nextGaussian()*xSigma;
            double yNoisy = pos.y + getRandom().nextGaussian()*ySigma;
            logger.info("X for input: " + xNoisy + " Y for input: " + yNoisy);
            double headingNoisy = beeTheta + getRandom().nextGaussian()*headingSigma;
            z = particleFilter.sense(xNoisy,yNoisy,headingNoisy);
            particles = particleFilter.resample(particles,w);
            w = particleFilter.measureProb(particles, z);
            move = true;
        }
        if (move == true){
            turn(.1f);
            setDesiredLinearVelocity(new Vector3f(5,0,0));
            particles = particleFilter.moveParticles(particles,.1,.5);
            move = false;
        }






    }


    @Override
    public void finish() {
    }


    @Inject(optional = true)
    public final void setMaxVelocity(@Named(value = "max-vel") final float vel) {
        this.maxVel = vel;
    }


}

