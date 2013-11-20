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
    int[] process;
    int[] gyros;
    int[] cntl;

    public HeliDataStruct() {
        frameCount = 0;
        process = new int[16];
        gyros   = new int[3];
        cntl   = new int[6];
    }
}

