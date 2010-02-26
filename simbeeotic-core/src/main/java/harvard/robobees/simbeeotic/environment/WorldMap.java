package harvard.robobees.simbeeotic.environment;

import static harvard.robobees.simbeeotic.model.PhysicalEntity.COLLISION_TERRAIN;
import static harvard.robobees.simbeeotic.model.PhysicalEntity.COLLISION_BEE;
import static harvard.robobees.simbeeotic.model.PhysicalEntity.COLLISION_FLOWER;
import harvard.robobees.simbeeotic.model.EntityInfo;
import harvard.robobees.simbeeotic.configuration.world.World;
import harvard.robobees.simbeeotic.configuration.world.Obstacle;
import harvard.robobees.simbeeotic.configuration.world.Meta;
import harvard.robobees.simbeeotic.configuration.world.Box;
import harvard.robobees.simbeeotic.configuration.world.Cylinder;
import harvard.robobees.simbeeotic.configuration.world.Sphere;
import harvard.robobees.simbeeotic.configuration.world.Cone;
import harvard.robobees.simbeeotic.configuration.world.Patch;

import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.StaticPlaneShape;
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CylinderShape;
import com.bulletphysics.collision.shapes.SphereShape;
import com.bulletphysics.collision.shapes.ConeShape;
import com.bulletphysics.linearmath.Transform;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.MatrixUtil;

import javax.vecmath.Vector3f;
import javax.vecmath.Quat4f;
import javax.vecmath.Matrix3f;
import java.util.Set;
import java.util.HashSet;
import java.util.Properties;
import java.util.Random;


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
    private RigidBody groundBody;
    private Set<RigidBody> obstacleBodies = new HashSet<RigidBody>();
    private Set<RigidBody> flowerBodies = new HashSet<RigidBody>();

    // todo: parameterize this?
    private float stemHeight = 0.3f;   // m
    private float stemRadius = 0.01f;  // m
    private float floraHeight = 0.02f; // m
    private float floraRadius = 0.1f;  // m


    public WorldMap(World world, DiscreteDynamicsWorld dynWorld, long seed) {

        this.world = world;
        this.rand = new Random(seed);

        this.dynamicsWorld = dynWorld;

        initialize();
    }


    private void initialize() {

        // setup the inifinite ground plane
        CollisionShape groundShape = new StaticPlaneShape(new Vector3f(0, 0, 1), 0);
        Transform groundTransform = new Transform();

        groundTransform.setIdentity();
        groundTransform.origin.set(new Vector3f(0, 0, 0));

        // the plane is a static obejct, so it does not need mass properties
        DefaultMotionState myMotionState = new DefaultMotionState(groundTransform);
        RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(0, myMotionState,
                                                                         groundShape, new Vector3f(0, 0, 0));

        groundBody = new RigidBody(rbInfo);
        groundBody.setUserPointer(new EntityInfo());

        dynamicsWorld.addRigidBody(groundBody, COLLISION_TERRAIN, COLLISION_BEE);

        // todo: setup uneven terrain

        // setup obstacles
        if (world.getObstacles() != null) {
            
            for (Obstacle obstacle : world.getObstacles().getObstacle()) {

                RigidBody body = null;
                CollisionShape colShape = null;
                Transform startTransform = null;
                EntityInfo info = new EntityInfo(loadProperties(obstacle.getMeta()));

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

                myMotionState = new DefaultMotionState(startTransform);
                rbInfo = new RigidBodyConstructionInfo(0, myMotionState,
                                                       colShape, new Vector3f(0, 0, 0));

                body = new RigidBody(rbInfo);

                body.setUserPointer(info);

                obstacleBodies.add(body);
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

                    EntityInfo platformInfo = new EntityInfo(loadProperties(patch.getMeta()));

                    // todo: make this less brittle
                    platformInfo.getProperties().setProperty("isFlower", Boolean.TRUE.toString());

                    // todo: use color info

                    // make stem
                    CollisionShape stemShape = new CylinderShape(new Vector3f(stemRadius, stemRadius, stemHeight / 2));

                    Transform stemTransform = new Transform();
                    stemTransform.setIdentity();

                    stemTransform.origin.set(new Vector3f(x, y, z + stemHeight / 2));

                    DefaultMotionState stemMotion = new DefaultMotionState(stemTransform);
                    RigidBodyConstructionInfo stemRbInfo = new RigidBodyConstructionInfo(0, stemMotion,
                                                           stemShape, new Vector3f(0, 0, 0));

                    RigidBody stemBody = new RigidBody(stemRbInfo);

                    stemBody.setUserPointer(new EntityInfo());

                    dynamicsWorld.addRigidBody(stemBody, COLLISION_FLOWER, COLLISION_BEE);

                    // make platform
                    CollisionShape platShape = new CylinderShape(new Vector3f(floraRadius, floraRadius, floraHeight / 2));

                    Transform platTransform = new Transform();
                    platTransform.setIdentity();

                    platTransform.origin.set(new Vector3f(x, y, z + stemHeight));

                    DefaultMotionState platMotion = new DefaultMotionState(platTransform);
                    RigidBodyConstructionInfo platRbInfo = new RigidBodyConstructionInfo(0, platMotion,
                                                                                         platShape, new Vector3f(0, 0, 0));

                    RigidBody platBody = new RigidBody(platRbInfo);


                    platBody.setUserPointer(platformInfo);

                    flowerBodies.add(platBody);
                    dynamicsWorld.addRigidBody(platBody, COLLISION_FLOWER, COLLISION_BEE);
                }
            }
        }
    }


    public void destroy() {
    }


    private Properties loadProperties(Meta meta) {

        Properties props = new Properties();

        if (meta != null) {

            // todo: resolve scenario variable placeholders?
            
            for (Meta.Prop p : meta.getProp()) {
                props.setProperty(p.getName(), p.getValue());
            }
        }

        return props;
    }
}
