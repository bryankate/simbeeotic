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
