package harvard.robobees.simbeeotic.model;


import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.linearmath.Transform;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * A class that acts as a go-between for an external source of
 * state information (e.g. motion tracking cameras) and
 * physics engine collision objects. The external source updates the
 * data and the collision object queries for the most recent state
 * periodically.
 *
 * @author bkate
 */
public class ExternalStateSync {

    private Map<Integer, CollisionObject> objects = new HashMap<Integer, CollisionObject>();
    private Map<Integer, Transform> states = new ConcurrentHashMap<Integer, Transform>();


    /**
     * Registers a collision object with the synchronizer. The object's state
     * is then updated by the external source.
     *
     * @param name The name of the object.
     * @param colObj The object to be synchronized.
     */
    public void registerObject(String name, CollisionObject colObj) {

        int id = name.hashCode();

        if (!objects.containsKey(id)) {
            objects.put(id, colObj);
        }
    }


    /**
     * Sets the state of an object using external data.
     *
     * @param name The ID of the object being updated.
     * @param position The new position of the object.
     * @param orientation The new orientation of the object.
     */
    public void setState(String name, Vector3f position, Quat4f orientation) {

        Transform trans = new Transform();

        trans.origin.set(position);
        trans.setRotation(orientation);
        
        states.put(name.hashCode(), trans);
    }


    /**
     * Updates the collision objects to reflect the latest external
     * kinematic state update.
     */
    public void updateStates() {

        for (int id : objects.keySet()) {

            Transform trans = states.get(id);

            if (trans != null) {

                objects.get(id).activate();
                objects.get(id).setWorldTransform(states.get(id));
            }
        }
    }
}
