package harvard.robobees.simbeeotic.model;

/**
 * Created with IntelliJ IDEA.
 * User: kar
 * Date: 5/12/13
 * Time: 8:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class HeliDataStruct {
    long frameCount;
    short[] process;
    short[] gyros;
    float[] debug;

    HeliDataStruct() {
        frameCount = 0;
        process = new short[16];
        gyros   = new short[3];
        debug   = new float[18];
    }
}

