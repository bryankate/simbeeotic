package harvard.robobees.simbeeotic.environment;


import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.ConeShape;
import com.bulletphysics.collision.shapes.CylinderShape;
import com.bulletphysics.collision.shapes.SphereShape;
import com.bulletphysics.collision.shapes.StaticPlaneShape;
import com.bulletphysics.collision.shapes.CompoundShape;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.MatrixUtil;
import com.bulletphysics.linearmath.Transform;
import com.bulletphysics.linearmath.MotionState;
import harvard.robobees.simbeeotic.configuration.world.Box;
import harvard.robobees.simbeeotic.configuration.world.Cone;
import harvard.robobees.simbeeotic.configuration.world.Cylinder;
import harvard.robobees.simbeeotic.configuration.world.Meta;
import harvard.robobees.simbeeotic.configuration.world.Obstacle;
import harvard.robobees.simbeeotic.configuration.world.Patch;
import harvard.robobees.simbeeotic.configuration.world.Sphere;
import harvard.robobees.simbeeotic.configuration.world.World;
import harvard.robobees.simbeeotic.model.EntityInfo;
import harvard.robobees.simbeeotic.model.MotionRecorder;
import harvard.robobees.simbeeotic.model.RecordedMotionState;
import static harvard.robobees.simbeeotic.model.PhysicalEntity.COLLISION_BEE;
import static harvard.robobees.simbeeotic.model.PhysicalEntity.COLLISION_FLOWER;
import static harvard.robobees.simbeeotic.model.PhysicalEntity.COLLISION_TERRAIN;

import javax.vecmath.Matrix3f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import java.util.HashSet;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * A class that establishes the physical layout of the world. The world contains a ground plane,
 * obstacles, and flowers. All objects created in the world are static.
 *
 * @author bkate
 */
public class WorldMap {

    private World world;
    private Random rand;

    private DiscreteDynamicsWorld dynamicsWorld;
    private MotionRecorder recorder;
    private AtomicInteger nextId;
    private WorldObject ground;
    private Set<WorldObject> obstacles = new HashSet<WorldObject>();
    private Set<WorldObject> flowers = new HashSet<WorldObject>();

    // todo: parameterize this?
    private float stemHeight = 0.3f;   // m
    private float stemRadius = 0.01f;  // m
    private float floraHeight = 0.02f; // m
    private float floraRadius = 0.1f;  // m


    public WorldMap(World world, DiscreteDynamicsWorld dynWorld,
                    MotionRecorder recorder, AtomicInteger nextId, long seed) {

        this.world = world;
        this.rand = new Random(seed);

        this.dynamicsWorld = dynWorld;
        this.recorder = recorder;
        this.nextId = nextId;

        initialize();
    }


    private void initialize() {

        // setup the inifinite ground plane
        CollisionShape groundShape = new StaticPlaneShape(new Vector3f(0, 0, 1), 0);
        Transform groundTransform = new Transform();

        groundTransform.setIdentity();
        groundTransform.origin.set(new Vector3f(0, 0, 0));

        int groundId = nextId.getAndIncrement();

        recorder.initializeObject(groundId, groundShape);

        // the plane is a static object, so it does not need mass properties
        MotionState myMotionState = new RecordedMotionState(groundId, recorder, groundTransform);
        RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(0, myMotionState,
                                                                         groundShape, new Vector3f(0, 0, 0));

        Map<String, Object> groundMeta = new HashMap<String, Object>();

        RigidBody groundBody = new RigidBody(rbInfo);
        groundBody.setUserPointer(new EntityInfo(groundId, groundMeta));

        ground = new WorldObject(groundId, WorldObject.Type.TERRAIN, groundBody, groundMeta);
        dynamicsWorld.addRigidBody(groundBody, COLLISION_TERRAIN, COLLISION_BEE);

        // todo: setup uneven terrain

        // setup obstacles
        if (world.getObstacles() != null) {
            
            for (Obstacle obstacle : world.getObstacles().getObstacle()) {

                RigidBody body = null;
                CollisionShape colShape = null;
                Transform startTransform = null;
                Map<String, Object> meta = loadProperties(obstacle.getMeta());

                int id = nextId.getAndIncrement();
                EntityInfo info = new EntityInfo(id, meta);

                // todo: use color info

                if (obstacle.getBox() != null) {

                    Box box = obstacle.getBox();
                    colShape = new BoxShape(new Vector3f(box.getLength() / 2,
                                                         box.getWidth() / 2,
                                                         box.getHeight() / 2));

                    Matrix3f rot = new Matrix3f();
                    Quat4f quat = new Quat4f();

                    MatrixUtil.setEulerZYX(rot, 0, 0, box.getRotation());
                    MatrixUtil.getRotation(rot, quat);

                    startTransform = new Transform();
                    startTransform.setIdentity();
                    startTransform.setRotation(quat);

                    // todo: get z loc once terrain is added
                    startTransform.origin.set(new Vector3f(box.getPosition().getX(),
                                                           box.getPosition().getY(),
                                                           box.getHeight() / 2));
                }
                else if (obstacle.getCylinder() != null) {

                    Cylinder cylinder = obstacle.getCylinder();
                    colShape = new CylinderShape(new Vector3f(cylinder.getWidth() / 2,
                                                              cylinder.getWidth() / 2,
                                                              cylinder.getHeight() / 2));

                    startTransform = new Transform();
                    startTransform.setIdentity();

                    // todo: get z loc once terrain is added
                    startTransform.origin.set(new Vector3f(cylinder.getPosition().getX(),
                                                           cylinder.getPosition().getY(),
                                                           cylinder.getHeight() / 2));
                }
                else if (obstacle.getSphere() != null) {

                    Sphere sphere = obstacle.getSphere();
                    colShape = new SphereShape(sphere.getRadius());

                    startTransform = new Transform();
                    startTransform.setIdentity();

                    // todo: get z loc once terrain is added
                    startTransform.origin.set(new Vector3f(sphere.getPosition().getX(),
                                                           sphere.getPosition().getY(),
                                                           sphere.getRadius()));
                }
                else {

                    Cone cone = obstacle.getCone();
                    colShape = new ConeShape(cone.getRadius(), cone.getHeight());

                    startTransform = new Transform();
                    startTransform.setIdentity();

                    // todo: get z loc once terrain is added
                    startTransform.origin.set(new Vector3f(cone.getPosition().getX(),
                                                           cone.getPosition().getY(),
                                                           cone.getHeight() / 2));
                }

                recorder.initializeObject(id, colShape);

                myMotionState = new RecordedMotionState(id, recorder, startTransform);
                rbInfo = new RigidBodyConstructionInfo(0, myMotionState,
                                                       colShape, new Vector3f(0, 0, 0));

                body = new RigidBody(rbInfo);

                body.setUserPointer(info);

                obstacles.add(new WorldObject(id, WorldObject.Type.OBSTACLE, body, meta));
                dynamicsWorld.addRigidBody(body, COLLISION_TERRAIN, COLLISION_BEE);
            }
        }

        // setup flower patches
        if (world.getFlowers() != null) {

            for (Patch patch : world.getFlowers().getPatch()) {

                float radius = patch.getRadius();
                float diam = radius * 2;
                int numFlowers = (int)(Math.PI * (radius * radius) * patch.getDensity());

                // make individual flowers and place them in the patch randomly
                for (int i = 0; i < numFlowers; i++) {

                    // todo: get the z loc once terrain is added
                    float x = patch.getCenter().getX() + (rand.nextFloat() * diam) - radius;
                    float y = patch.getCenter().getY() + (rand.nextFloat() * diam) - radius;
                    float z = 0;

                    Map<String, Object> meta = loadProperties(patch.getMeta());

                    int id = nextId.getAndIncrement();
                    EntityInfo platformInfo = new EntityInfo(id, meta);

                    // todo: make this less brittle
                    platformInfo.getMetadata().put("isFlower", true);

                    // todo: use color info

                    // make stem
                    CollisionShape stemShape = new CylinderShape(new Vector3f(stemRadius, stemRadius, stemHeight / 2));

                    Transform stemTransform = new Transform();
                    stemTransform.setIdentity();

                    stemTransform.origin.set(new Vector3f(0, 0, stemHeight / 2));

                    // make platform
                    CollisionShape platShape = new CylinderShape(new Vector3f(floraRadius, floraRadius, floraHeight / 2));

                    Transform platTransform = new Transform();
                    platTransform.setIdentity();

                    platTransform.origin.set(new Vector3f(0, 0, stemHeight));

                    // put the flower together
                    Transform trans = new Transform();

                    trans.setIdentity();
                    trans.origin.set(new Vector3f(x, y, z));

                    CompoundShape shape = new CompoundShape();

                    shape.addChildShape(stemTransform, stemShape);
                    shape.addChildShape(platTransform, platShape);

                    recorder.initializeObject(id, platShape);

                    DefaultMotionState motion = new RecordedMotionState(id, recorder, trans);
                    RigidBodyConstructionInfo flowerRbInfo = new RigidBodyConstructionInfo(0, motion,
                                                                                           shape, new Vector3f(0, 0, 0));

                    RigidBody flowerBody = new RigidBody(flowerRbInfo);

                    flowerBody.setUserPointer(platformInfo);

                    flowers.add(new WorldObject(id, WorldObject.Type.FLOWER, flowerBody, meta));
                    dynamicsWorld.addRigidBody(flowerBody, COLLISION_FLOWER, COLLISION_BEE);
                }
            }
        }
    }


    public void destroy() {
    }


    /**
     * Gets the radius of a bounding half-sphere that defines the "edge" of the physical world. Physical
     * entities are permitted to exceed these bounds but they may not be subject to collision detection.
     *
     * @return The world bounds, as a radius (in meters).
     */
    public float getBounds() {
        return world.getRadius();
    }


    /**
     * Get the object that defines the terrain (ground) in the world.
     *
     * @return The details of the terrain.
     */
    public WorldObject getTerrain() {
        return ground;
    }


    /**
     * Gets all obstacles present in the world.
     *
     * @return The set of all objects.
     */
    public Set<WorldObject> getObstacles() {
        return Collections.unmodifiableSet(obstacles);
    }


    /**
     * Gets the obstacles that are contained within the given sphere. The
     * obstacle is determined to be in the sphere if its center is in the sphere.
     *
     * @param center The center of the query sphere.
     * @param radius The radius of the query sphere.
     *
     * @return The set of all objects.
     */
    public Set<WorldObject> getObstacles(Vector3f center, double radius) {

        Set<WorldObject> in = new HashSet<WorldObject>();

        for (WorldObject obj : obstacles) {

            Vector3f diff = new Vector3f();

            diff.sub(center, obj.getTruthPosition());

            if (diff.length() <= radius) {
                in.add(obj);
            }
        }

        return in;
    }


    /**
     * Gets all flowers present in the world.
     *
     * @return The set of all flowers.
     */
    public Set<WorldObject> getFlowers() {
        return Collections.unmodifiableSet(flowers);
    }


    /**
     * Gets the flowers that are contained within the given sphere. The
     * flower is determined to be in the sphere if its center is in the sphere.
     *
     * @param center The center of the query sphere.
     * @param radius The radius of the query sphere.
     *
     * @return The set of all objects.
     */
    public Set<WorldObject> getFlowers(Vector3f center, double radius) {

        Set<WorldObject> in = new HashSet<WorldObject>();

        for (WorldObject obj : flowers) {

            Vector3f diff = new Vector3f();

            diff.sub(center, obj.getTruthPosition());

            if (diff.length() <= radius) {
                in.add(obj);
            }
        }

        return in;
    }


    private Map<String, Object> loadProperties(Meta meta) {

        Map<String, Object> props = new HashMap<String, Object>();

        if (meta != null) {

            // todo: resolve scenario variable placeholders?
            
            for (Meta.Prop p : meta.getProp()) {
                props.put(p.getName(), p.getValue());
            }
        }

        return props;
    }
}
