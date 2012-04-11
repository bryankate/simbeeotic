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

package harvard.robobees.simbeeotic.component;


import com.bulletphysics.linearmath.QuaternionUtil;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import harvard.robobees.simbeeotic.configuration.ConfigurationAnnotations.GlobalScope;
import harvard.robobees.simbeeotic.model.ExternalStateSync;
import kinectd.protocol.Control;
import kinectd.protocol.KinematicState;
import org.apache.log4j.Logger;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * A component that interfaces with the Kinect data server
 * and updates the state of an object in the simulation.
 *
 * @author bkate
 */
public class KinectInputComponent implements VariationComponent {

    @Inject
    @GlobalScope
    private ExternalStateSync externalSync;

    @Inject
    @Named("object-name")
    private String objectName;

    @Inject(optional = true)
    @Named("server-host")
    private String serverHost = "localhost";

    @Inject(optional = true)
    @Named("server-port")
    private int serverPort = 6789;

    @Inject(optional = true)
    @Named("pos-x")
    private float xPos = 0.0f;

    @Inject(optional = true)
    @Named("pos-y")
    private float yPos = 0.0f;

    @Inject(optional = true)
    @Named("pos-z")
    private float zPos = 1.0f;

    private AtomicBoolean running = new AtomicBoolean(true);

    private static Logger logger = Logger.getLogger(KinectInputComponent.class);

    
    @Override
    public void initialize() {

        new Thread(new KinectThread(objectName.trim(), running)).start();
    }


    @Override
    public void shutdown() {
        running.set(false);
    }


    /**
     * An instance of this class runs in a thread and updates an individual object.
     */
    private class KinectThread implements Runnable {

        private String objectName;
        private DatagramSocket sock;
        private AtomicBoolean running;


        public KinectThread(String name, AtomicBoolean run) {

            objectName = name;
            running = run;
        }


        public boolean connect() {

            // open an incoming socket to receive data
            try {

                sock = new DatagramSocket(null);
                sock.bind(null);

                byte[] buff = Control.Connect.newBuilder().setPort(sock.getLocalPort()).build().toByteArray();

                // send request
                DatagramPacket packet = new DatagramPacket(buff, buff.length, InetAddress.getByName(serverHost), serverPort);

                sock.send(packet);

                // wait for response
                buff = new byte[1024];
                packet = new DatagramPacket(buff, buff.length);

                sock.receive(packet);

                ByteArrayInputStream in = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                Control.Result result = Control.Result.parseFrom(in);
                in.close();

                if (result.getType() == Control.Result.Type.SUCCESS) {
                    return true;
                }
                else {
                    logger.error("Could not connect to kinectd for object '" + objectName + "': " + result.getMessage());
                }
            }
            catch(Exception e) {

                e.printStackTrace();
                // fall through
            }

            return false;
        }


        @Override
        public void run() {

            if (!connect()) {
                return;
            }

            byte[] buff = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buff, buff.length);

            while(running.get()) {

                try {

                    // get another data packet and update the object in the world
                    sock.receive(packet);

                    ByteArrayInputStream in = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                    KinematicState.State state = KinematicState.State.parseFrom(in);
                    in.close();

                    Vector3f pos = new Vector3f((float)state.getLocX() + xPos,
                                                (float)state.getLocY() + yPos,
                                                (float)state.getLocZ() + zPos);

                    Quat4f orient = new Quat4f();

                    switch(state.getOrient()) {

                        default:
                        case UNKNOWN:
                        case CENTER:

                            QuaternionUtil.setEuler(orient, 0, 0, 0);
                            break;

                        case RIGHT:
                            QuaternionUtil.setEuler(orient, (float)Math.PI / 2, 0, 0);
                            break;

                        case LEFT:
                            QuaternionUtil.setEuler(orient, (float)-Math.PI / 2, 0, 0);
                            break;
                    }

                    externalSync.setState(objectName, pos, orient);

//                    logger.debug(objectName + " " + pos + " " + orient);
                }
                catch(IOException ioe) {
                    return;
                }
            }
        }
    }
}
