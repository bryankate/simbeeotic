package harvard.robobees.simbeeotic.util;


import com.bulletphysics.linearmath.Transform;
import junit.framework.TestCase;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;


/**
 * @author bkate
 */
public class MathUtilTest extends TestCase {

    public void testGetRotation() {

        Vector3f v1 = new Vector3f(0, 0, 1);
        Vector3f v2 = new Vector3f(0, 1, 0);

        Quat4f rot = MathUtil.getRotation(v1, v2);

        Vector3f unitZ = new Vector3f(0, 0, 1);

        Transform trans = new Transform();

        trans.setIdentity();
        trans.setRotation(rot);

        trans.transform(unitZ);

        assertEquals("The computed rotation between two vectors is incorrect.", unitZ.x, 0.0f);
        assertEquals("The computed rotation between two vectors is incorrect.", unitZ.y, 1.0f);
        assertEquals("The computed rotation between two vectors is incorrect.", unitZ.z, 0.0f);


        // no rotation between parallel vectors
        rot = MathUtil.getRotation(v1, v1);

        assertEquals("Parallel vectors have nonzero rotation!", rot.x, 0.0f);
        assertEquals("Parallel vectors have nonzero rotation!", rot.y, 0.0f);
        assertEquals("Parallel vectors have nonzero rotation!", rot.z, 0.0f);
        assertEquals("Parallel vectors have nonzero rotation!", rot.w, 0.0f);
    }


    public void testDbmConversion() {

        assertEquals(MathUtil.mwToDbm(0), Double.NEGATIVE_INFINITY);
        assertEquals(MathUtil.mwToDbm(1), 0.0, 1e-10);
        assertEquals(MathUtil.mwToDbm(100), 20.0, 1e-10);
        assertEquals(MathUtil.mwToDbm(0.1), -10.0, 1e-10);

        assertEquals(MathUtil.dbmToMw(0), 1.0, 1e-10);
        assertEquals(MathUtil.dbmToMw(-10), 0.1, 1e-10);
        assertEquals(MathUtil.dbmToMw(20), 100.0, 1e-10);
    }
}
