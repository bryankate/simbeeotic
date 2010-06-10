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


/**
 * A class that coordinates the dissemination of motion information for the
 * objects in the simulation. Interested classes can register as a listener
 * of motion state updates via the {@link MotionListener} interface.
 *
 * @author bkate
 */
public class MotionRecorder {

    private Set<MotionListener> listeners = new CopyOnWriteArraySet<MotionListener>();
    private BlockingQueue<Update> queue = new LinkedBlockingQueue<Update>();
    private Thread processThread;
    private Lock lock = new ReentrantLock();


    public MotionRecorder() {

        // run update callbacks in a separate thread to allow the sim to continue
        processThread = new Thread(new Runnable() {

            public void run() {

                try {

                    // forever wait for an update
                    while(true) {

                        Update update = queue.take();

                        lock.lock();

                        try {

                            // inform all listeners of the update
                            for (MotionListener listener : listeners) {
                                listener.stateUpdate(update.id, update.pos, update.orient);
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
                listener.initializeObject(objectId, shape);
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
        queue.add(new Update(objectId, position, orientation));
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
     * A container class for update details.
     */
    private static class Update {

        public int id;
        public Vector3f pos;
        public Quat4f orient;

        public Update(int id, Vector3f pos, Quat4f orient) {

            this.id = id;
            this.pos = pos;
            this.orient = orient;
        }
    }
}
