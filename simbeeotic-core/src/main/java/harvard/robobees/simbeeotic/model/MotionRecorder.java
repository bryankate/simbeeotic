package harvard.robobees.simbeeotic.model;


import com.bulletphysics.collision.shapes.CollisionShape;

import javax.vecmath.Vector3f;
import javax.vecmath.Quat4f;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.awt.*;

import org.apache.log4j.Logger;


/**
 * A class that coordinates the dissemination of motion information for the
 * objects in the simulation. Interested classes can register as a listener
 * of motion state updates via the {@link MotionListener} interface.
 *
 * @author bkate
 */
public class MotionRecorder {

    private Set<MotionListener> listeners = new CopyOnWriteArraySet<MotionListener>();
    private BlockingQueue queue = new LinkedBlockingQueue();
    private Thread processThread;
    private Lock lock = new ReentrantLock();

    private static Logger logger = Logger.getLogger(MotionRecorder.class);


    public MotionRecorder() {

        // run update callbacks in a separate thread to allow the sim to continue
        processThread = new Thread(new Runnable() {

            public void run() {

                try {

                    // forever wait for an update
                    while(true) {

                        Object update = queue.take();

                        lock.lock();

                        try {

                            // inform all listeners of the motionUpdate
                            for (MotionListener listener : listeners) {

                                try {

                                    if (update instanceof MotionUpdate) {

                                        MotionUpdate m = (MotionUpdate)update;

                                        listener.stateUpdate(m.id, m.pos, m.orient);
                                    }
                                    else {

                                        MetaUpdate m = (MetaUpdate)update;

                                        listener.metaUpdate(m.id, m.color, m.label);
                                    }
                                }
                                catch(Exception e) {
                                    logger.warn("Caught an exception when updating MotionListener.");
                                }
                            }
                        }
                        finally {
                            lock.unlock();
                        }
                    }
                }
                catch(InterruptedException ie) {
                    // do nothing
                }
            }
        });

        processThread.start();
    }


    /**
     * Set the details of the object. For the sake of any {@link MotionListener}s, this must be
     * called at least prior to any call to {@link #updateKinematicState}.
     *
     * @param objectId The unique identifier of the object.
     * @param shape The shape of the object.
     */
    public void initializeObject(int objectId, CollisionShape shape) {

        // use this lock so that we do not call the listener form more than one thread. we could
        // require that listeners be thread safe, but this is not bad considering the
        // initialization happens once.
        lock.lock();

        try {

            // inform all listeners of the update
            for (MotionListener listener : listeners) {

                try {
                    listener.initializeObject(objectId, shape);
                }
                catch(Exception e) {
                    logger.warn("Caught an exception when updating MotionListener.");
                }
            }
        }
        finally {
            lock.unlock();
        }
    }


    /**
     * Update thie kinematic state of an object (its position and orientation).
     *
     * @param objectId The unique identifier of the object.
     * @param position The position of the object (in the world frame).
     * @param orientation The orientation of the object (in the world frame).
     */
    public void updateKinematicState(int objectId, Vector3f position, Quat4f orientation) {
        queue.add(new MotionUpdate(objectId, position, orientation));
    }


    /**
     * Updates the color of an object.
     *
     * @param objectId The unique identifier of the object.
     * @param color The new color information (may be null if no change is being made).
     */
    public void updateMetadata(int objectId, Color color) {
        updateMetadata(objectId, color, null);
    }


    /**
     * Updates the label associated with an object.
     *
     * @param objectId The unique identifier of the object.
     * @param label The new label information (may be null if no change is being made).
     */
    public void updateMetadata(int objectId, String label) {
        updateMetadata(objectId, null, label);
    }


    /**
     * Updates the metadata associated with an object.
     *
     * @param objectId The unique identifier of the object.
     * @param color The new color information (may be null if no change is being made).
     * @param label The new label information (may be null if no change is being made).
     */
    public void updateMetadata(int objectId, Color color, String label) {
        queue.add(new MetaUpdate(objectId, color, label));
    }


    /**
     * Adds a listener to the recorder.
     *
     * @param listener The motion listener to be added.
     */
    public void addListener(MotionListener listener) {
        listeners.add(listener);
    }


    /**
     * Removes a listener from the recorder.
     *
     * @param listener The motion listener to be removed.
     */
    public void removeListener(MotionListener listener) {
        listeners.remove(listener);
    }


    /**
     * Shuts down the recorder. This includes stopping any processing threads.
     */
    public void shutdown() {

        processThread.interrupt();
        listeners.clear();
    }


    /**
     * A container class for motion update details.
     */
    private static class MotionUpdate {

        public int id;
        public Vector3f pos;
        public Quat4f orient;

        public MotionUpdate(int id, Vector3f pos, Quat4f orient) {

            this.id = id;
            this.pos = pos;
            this.orient = orient;
        }
    }


    /**
     * A container class for motion update details.
     */
    private static class MetaUpdate {

        public int id;
        public Color color;
        public String label;

        public MetaUpdate(int id, Color color, String label) {

            this.id = id;
            this.color = color;
            this.label = label;
        }
    }
}
