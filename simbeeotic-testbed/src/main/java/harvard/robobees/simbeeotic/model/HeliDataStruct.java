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
    byte[] process;
    short[] gyros;
    byte[] cntl;

    public HeliDataStruct() {
        frameCount = 0;
        process = new byte[16];
        gyros   = new short[3];
        cntl   = new byte[6];
    }
}

