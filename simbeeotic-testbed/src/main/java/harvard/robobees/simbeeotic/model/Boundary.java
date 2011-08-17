package harvard.robobees.simbeeotic.model;


/**
 * A class that represents the physical bounds in which the helicopters can fly.
 *
 * @author bkate
 */
public class Boundary {

    private float xMin = DEFAULT_X_MIN;
    private float xMax = DEFAULT_X_MAX;
    private float yMin = DEFAULT_Y_MIN;
    private float yMax = DEFAULT_Y_MAX;
    private float zMax = DEFAULT_Z_MAX;

    public static final float DEFAULT_X_MIN = -2.2f;
    public static final float DEFAULT_X_MAX = 2.2f;
    public static final float DEFAULT_Y_MIN = -3.6f;
    public static final float DEFAULT_Y_MAX = 3.6f;
    public static final float DEFAULT_Z_MAX = 2f;


    public Boundary() {
        // use default settings
    }


    public Boundary(float xMin, float xMax, float yMin, float yMax, float zMax) {

        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;
    }


    public float getXMax() {
        return xMax;
    }


    public float getXMin() {
        return xMin;
    }


    public float getYMax() {
        return yMax;
    }


    public float getYMin() {
        return yMin;
    }


    public float getZMax() {
        return yMax;
    }


    public float getRadius() {
        return Math.max(xMax, yMax);
    }
}
