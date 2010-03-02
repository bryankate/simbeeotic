package harvard.robobees.simbeeotic.util;


import junit.framework.TestCase;

import javax.vecmath.Vector3f;
import javax.vecmath.Quat4f;

import com.bulletphysics.linearmath.Transform;


/**
 * @author bkate
 */
public class LinearMathUtilTest extends TestCase {

    public void testGetRotation() {

        Vector3f v1 = new Vector3f(0, 0, 1);
        Vector3f v2 = new Vector3f(0, 1, 0);

        Quat4f rot = LinearMathUtil.getRotation(v1, v2);

        Vector3f unitZ = new Vector3f(0, 0, 1);

        Transform trans = new Transform();

        trans.setIdentity();
        trans.setRotation(rot);

        trans.transform(unitZ);

        assertEquals("The computed rotation between two vectors is incorrect.", unitZ.x, 0.0f);
        assertEquals("The computed rotation between two vectors is incorrect.", unitZ.y, 1.0f);
        assertEquals("The computed rotation between two vectors is incorrect.", unitZ.z, 0.0f);


        // no rotation between parallel vectors
        rot = LinearMathUtil.getRotation(v1, v1);

        assertEquals("Parallel vectors have nonzero rotation!", rot.x, 0.0f);
        assertEquals("Parallel vectors have nonzero rotation!", rot.y, 0.0f);
        assertEquals("Parallel vectors have nonzero rotation!", rot.z, 0.0f);
        assertEquals("Parallel vectors have nonzero rotation!", rot.w, 0.0f);
    }
}
