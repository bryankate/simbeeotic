package harvard.robobees.simbeeotic.environment;


import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.ConeShape;
import com.bulletphysics.collision.shapes.ConeShapeZ;
import com.bulletphysics.collision.shapes.CylinderShape;
import com.bulletphysics.collision.shapes.CylinderShapeZ;
import com.bulletphysics.collision.shapes.SphereShape;
import com.bulletphysics.collision.shapes.StaticPlaneShape;
import com.bulletphysics.collision.shapes.CompoundShape;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.MatrixUtil;
import com.bulletphysics.linearmath.Transform;
import com.bulletphysics.linearmath.MotionState;
import com.bulletphysics.linearmath.VectorUtil;

import harvard.robobees.simbeeotic.configuration.world.Box;
import harvard.robobees.simbeeotic.configuration.world.Cone;
import harvard.robobees.simbeeotic.configuration.world.Cylinder;
import harvard.robobees.simbeeotic.configuration.world.Meta;
import harvard.robobees.simbeeotic.configuration.world.Obstacle;
import harvard.robobees.simbeeotic.configuration.world.Patch;
import harvard.robobees.simbeeotic.configuration.world.Sphere;
import harvard.robobees.simbeeotic.configuration.world.Structure;
import harvard.robobees.simbeeotic.configuration.world.Surface;
import harvard.robobees.simbeeotic.configuration.world.Wall;
import harvard.robobees.simbeeotic.configuration.world.World;
import harvard.robobees.simbeeotic.model.EntityInfo;
import harvard.robobees.simbeeotic.model.MotionRecorder;
import harvard.robobees.simbeeotic.model.RecordedMotionState;
import harvard.robobees.simbeeotic.util.ImageLoader;

import static harvard.robobees.simbeeotic.model.PhysicalEntity.COLLISION_BEE;
import static harvard.robobees.simbeeotic.model.PhysicalEntity.COLLISION_FLOWER;
import static harvard.robobees.simbeeotic.model.PhysicalEntity.COLLISION_TERRAIN;

import javax.vecmath.Matrix3f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.awt.*;


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
    private Set<WorldObject> structures = new HashSet<WorldObject>();
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
        recorder.updateMetadata(groundId, new Color(20, 70, 30), ImageLoader.loadImageFromClasspath("/textures/grass_1.jpg"), null);

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

                CollisionShape colShape = null;
                Transform startTransform = null;
                Map<String, Object> meta = loadProperties(obstacle.getMeta());

                int id = nextId.getAndIncrement();
                EntityInfo info = new EntityInfo(id, meta);

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
                    colShape = new CylinderShapeZ(new Vector3f(cylinder.getWidth() / 2,
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
                    colShape = new ConeShapeZ(cone.getRadius(), cone.getHeight());

                    startTransform = new Transform();
                    startTransform.setIdentity();

                    // todo: get z loc once terrain is added
                    startTransform.origin.set(new Vector3f(cone.getPosition().getX(),
                                                           cone.getPosition().getY(),
                                                           cone.getHeight() / 2));
                }

                Color color = new Color(140, 140, 140);
                Image texture = null;

                if (obstacle.getColor() != null) {
                    
                    color = new Color(obstacle.getColor().getRed(),
                                      obstacle.getColor().getGreen(),
                                      obstacle.getColor().getBlue(),
                                      obstacle.getColor().getAlpha());
                }

                if (obstacle.getTexture() != null) {

                    if (obstacle.getTexture().getClasspath() != null) {
                        texture = ImageLoader.loadImageFromClasspath(obstacle.getTexture().getClasspath().getPath());
                    }
                    else {
                        texture = ImageLoader.loadImageFromFilesystem(obstacle.getTexture().getFilesystem().getPath());
                    }
                }

                addBody(WorldObject.Type.OBSTACLE, startTransform, colShape, color, texture, obstacle.getLabel(), info, obstacles);
            }
        }

        // setup structures
        if (world.getStructures() != null) {

            for (Structure structure : world.getStructures().getStructure()) {

                Transform startTransform = null;

                Map<String, Object> meta = loadProperties(structure.getMeta());

                int id = nextId.getAndIncrement();
                EntityInfo info = new EntityInfo(id, meta);

                String label = null;
                Color color = new Color(77, 77, 77);
                Image texture = null;

                if (structure.getColor() != null) {
                    
                    color = new Color(structure.getColor().getRed(),
                                      structure.getColor().getGreen(),
                                      structure.getColor().getBlue(),
                                      structure.getColor().getAlpha());
                }

                if (structure.getTexture() != null) {

                    if (structure.getTexture().getClasspath() != null) {
                        texture = ImageLoader.loadImageFromClasspath(structure.getTexture().getClasspath().getPath());
                    }
                    else {
                        texture = ImageLoader.loadImageFromFilesystem(structure.getTexture().getFilesystem().getPath());
                    }
                }

                // walls
                if (structure.getWall() != null)
                {
	                Wall wall = structure.getWall();

	            	startTransform = new Transform();
	            	startTransform.setIdentity();

	            	// extract properties of the wall from the xml
	            	// these are the coordinates of the 'lower-left corner' of the compound shape
	            	Vector3f position = new Vector3f(wall.getPosition().getX(),
	            									 wall.getPosition().getY(),
	            									 wall.getPosition().getZ());

	            	Vector3f dimensions = new Vector3f(wall.getLength(),
	            									   wall.getWidth(),
	            									   wall.getHeight());

	            	float rotation = wall.getRotation();

	            	float doorWidth = wall.getDoorwidth();
	            	float doorHeight = wall.getDoorheight();
	            	float doorX = wall.getDoorposition();

	            	// divide by 2 to preserve dimensions specified in xml
	            	Vector3f extents = new Vector3f(dimensions.x/2,
	            									dimensions.y/2,
	            									dimensions.z/2);

	            	// rotate about z axis by angle 'rotation' (radians)
	            	Matrix3f rot = new Matrix3f();
	            	Quat4f quat = new Quat4f();
	                MatrixUtil.setEulerZYX(rot, 0, 0, rotation);
	                MatrixUtil.getRotation(rot, quat);

	                Vector3f center = new Vector3f(position.x + extents.x,
	                							   position.y + extents.y,
	                							   position.z + extents.z);

	                Vector3f relPos = new Vector3f(-extents.x, -extents.y, 0);
	                adjustCenterPostRot(center, relPos, rotation);

	                startTransform = new Transform();
	                startTransform.setIdentity();

	            	// wall without door
	            	if(!wall.isDoor()) {

	                	CollisionShape colShape = new BoxShape(extents);

	                	// add half of given height to ensure that wall actually rests upon the specified z
	                	startTransform.origin.set(center);
		                startTransform.setRotation(quat);

                        addBody(WorldObject.Type.STRUCTURE, startTransform, colShape, color, texture, label, info, structures);
	            	}

	            	// wall with door
	            	else {
	            		// relative distances from center of compound shape to centers of each child shape
	            		float relDistLeft = -extents.x + doorX/2;
	            		float relDistRight = extents.x - (dimensions.x  - (doorX + doorWidth + dimensions.x)/2);
	            		float relDistTop = -extents.x + doorX + doorWidth/2;

	            		CompoundShape compoundShape = new CompoundShape();

	            		// collision shape for the boxes on the left side
	            		// |xxxx|DOOR|----| form the x part!
	            		CollisionShape colShape = new BoxShape(new Vector3f(doorX/2, extents.y, extents.z));

	            		// relative transform for left piece
	            		startTransform.origin.set(new Vector3f(relDistLeft, 0, 0));
	            		compoundShape.addChildShape(startTransform, colShape);

	            		// collision shape for the box on the right side
	            		colShape = new BoxShape(new Vector3f((dimensions.x - doorX - doorWidth)/2, extents.y, extents.z));

	            		// relative transform for right piece
	            		startTransform.origin.set(new Vector3f(relDistRight, 0, 0));
	            		compoundShape.addChildShape(startTransform, colShape);

	            		// collision shape for box above door
	            		colShape = new BoxShape(new Vector3f(doorWidth/2, extents.y, extents.z - doorHeight/2));
	            		startTransform.origin.set(new Vector3f(relDistTop, 0, doorHeight/2));

	            		compoundShape.addChildShape(startTransform, colShape);

	            		// I think I have to do this since I added child shapes...
	            		compoundShape.recalculateLocalAabb();

	            		// reset the transform for the full compound shape
	            		startTransform.setRotation(quat);
	            		startTransform.origin.set(center);

	            		// add the compound shape into the world!
                        addBody(WorldObject.Type.STRUCTURE, startTransform, compoundShape, color, texture, label, info, structures);
	            	}
                }

                // surfaces
                else if (structure.getSurface() != null)
                {
	                Surface surface = structure.getSurface();

	            	startTransform = new Transform();
	            	startTransform.setIdentity();

	            	// extract properties of the wall from the xml
	            	Vector3f position = new Vector3f(surface.getPosition().getX(),
	            			     					 surface.getPosition().getY(),
	            									 surface.getPosition().getZ());

	            	Vector3f dimensions = new Vector3f(surface.getLength(),
	            									   surface.getWidth(),
	            									   surface.getHeight());

	            	float rotation = surface.getRotation();

	            	// divide by 2 to preserve dimensions specified in xml
	            	Vector3f extents = new Vector3f(dimensions.x/2, dimensions.y/2, dimensions.z/2);

	            	// rotate about z axis by angle 'rotation' (radians)
	            	Matrix3f rot = new Matrix3f();
	            	Quat4f quat = new Quat4f();
	                MatrixUtil.setEulerZYX(rot, 0, 0, rotation);
	                MatrixUtil.getRotation(rot, quat);

	                startTransform = new Transform();
	                startTransform.setIdentity();
	                startTransform.setRotation(quat);

	                // set up center to correct for rotation (in order to position corner at specified position)
	                Vector3f center = new Vector3f(position.x + extents.x,
	                		    				   position.y + extents.y,
	                							   position.z + extents.z);
	                Vector3f relPos = new Vector3f(-extents.x, -extents.y, 0);

	                if(rotation != 0)
	                {
	                	adjustCenterPostRot(center, relPos, rotation);
	                }

	                // surface without 'trap'
	                if(!surface.isTrap())
	                {
	                	CollisionShape colShape = new BoxShape(extents);
		                startTransform.origin.set(center);

                        addBody(WorldObject.Type.STRUCTURE, startTransform, colShape, color, texture, label, info, structures);
	                }

	                // surface with 'trap'
	                else // IT'S A TRAP!
	                {
	                	float trapX = surface.getTrapX();
	                	float trapY = surface.getTrapY();
	                	float trapLength = surface.getTrapLength();
	                	float trapWidth = surface.getTrapWidth();

	                	float relDistLeft = -extents.x + trapX/2;
	                	float relDistRight = extents.x - (dimensions.x - (trapX + trapLength + dimensions.x)/2);
	                	float relDistTopY = extents.y - (dimensions.y - (trapY + trapWidth + dimensions.y)/2);
	                	float relDistBottomY = -extents.y + trapY/2;
	                	float relDistTopX = -extents.x + trapX + trapWidth/2;
	                	float relDistBottomX = relDistTopX;

	                	CompoundShape compoundShape = new CompoundShape();

	                	// left shape
	                	CollisionShape colShape = new BoxShape(new Vector3f(trapX/2, extents.y, extents.z));

	                	startTransform.origin.set(new Vector3f(relDistLeft, 0, 0));
	                	compoundShape.addChildShape(startTransform, colShape);

	                	// right shape
	                	colShape = new BoxShape(new Vector3f((dimensions.x - trapX - trapLength)/2,
	                										 extents.y,
	                										 extents.z));

	                	startTransform.origin.set(new Vector3f(relDistRight, 0, 0));
	                	compoundShape.addChildShape(startTransform, colShape);

	                	// top shape
	                	colShape = new BoxShape(new Vector3f(trapLength/2,
	                										(dimensions.y - trapY - trapWidth)/2, 0));

	                	startTransform.origin.set(new Vector3f(relDistTopX, relDistTopY, extents.z));
	                	compoundShape.addChildShape(startTransform, colShape);

	                	// bottom shape
	                	colShape = new BoxShape(new Vector3f(trapLength/2, trapY/2, 0));

	                	startTransform.origin.set(new Vector3f(relDistBottomX, relDistBottomY, extents.z));
	                	compoundShape.addChildShape(startTransform, colShape);

	            		compoundShape.recalculateLocalAabb();

	            		// setup the transform for the full compound shape
	            		// correct for rotation to place the surface such that the lower left corner is in position
	            		startTransform.origin.set(center);

	            		// add the compound shape into the world!
	            		addBody(WorldObject.Type.STRUCTURE, startTransform, compoundShape, color, texture, label, info, structures);
	                }
                }
            }
        }

        // setup flower patches
        if (world.getFlowers() != null) {

            for (Patch patch : world.getFlowers().getPatch()) {

                float radius = patch.getRadius();
                float diam = radius * 2;
                int numFlowers = (int)(Math.PI * (radius * radius) * patch.getDensity());

                CollisionShape stemShape = new CylinderShapeZ(new Vector3f(stemRadius, stemRadius, stemHeight / 2));
                CollisionShape platShape = new CylinderShapeZ(new Vector3f(floraRadius, floraRadius, floraHeight / 2));

                // make individual flowers and place them in the patch randomly
                for (int i = 0; i < numFlowers; i++) {

                    // todo: get the z loc once terrain is added
                    float x = patch.getCenter().getX() + (rand.nextFloat() * diam) - radius;
                    float y = patch.getCenter().getY() + (rand.nextFloat() * diam) - radius;
                    float z = 0;

                    EntityInfo flowerInfo = new EntityInfo(nextId.getAndIncrement(), loadProperties(patch.getMeta()));

                    // make stem
                    Transform stemTransform = new Transform();
                    stemTransform.setIdentity();

                    stemTransform.origin.set(new Vector3f(0, 0, stemHeight / 2));

                    // make platform
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

                    Color color = new Color(250, 0, 50);

                    if (patch.getColor() != null) {

                        color = new Color(patch.getColor().getRed(),
                                          patch.getColor().getGreen(),
                                          patch.getColor().getBlue(),
                                          patch.getColor().getAlpha());
                    }

                    addBody(WorldObject.Type.FLOWER, trans, shape, color, null, null, flowerInfo, flowers);
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
     * @return The set of all obstacles.
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
     * @return The set of all obstacles within the given area.
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
     * Gets all structures present in the world.
     *
     * @return The set of all structures.
     */
    public Set<WorldObject> getStructures() {
        return Collections.unmodifiableSet(structures);
    }


    /**
     * Gets the structures that are contained within the given sphere. The
     * obstacle is determined to be in the sphere if its center is in the sphere.
     *
     * @param center The center of the query sphere.
     * @param radius The radius of the query sphere.
     *
     * @return The set of all objects.
     */
    public Set<WorldObject> getStructures(Vector3f center, double radius) {

        Set<WorldObject> in = new HashSet<WorldObject>();

        for (WorldObject obj : structures) {

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
     * @return The set of all flowers in the given area.
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
    
    
    private void addBody(WorldObject.Type type, Transform startTransform, CollisionShape colShape,
                         Color color, Image texture, String label, EntityInfo info, Set<WorldObject> objSet) {

        int id = info.getObjectId();

        recorder.initializeObject(id, colShape);
        recorder.updateMetadata(id, color, texture, label);

    	MotionState myMotionState = new RecordedMotionState(id, recorder, startTransform);
    	RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(0, myMotionState,
                                                                         colShape, new Vector3f(0, 0, 0));

		RigidBody body = new RigidBody(rbInfo);
		body.setUserPointer(info);

        switch(type) {

            case OBSTACLE:
            case TERRAIN:
            case STRUCTURE:

                dynamicsWorld.addRigidBody(body, COLLISION_TERRAIN, COLLISION_BEE);
                break;

            case FLOWER:
                dynamicsWorld.addRigidBody(body, COLLISION_FLOWER, COLLISION_BEE);
                break;
        }

        objSet.add(new WorldObject(id, type, body, info.getMetadata()));
    }

    
    /**
     * Given the coordinates of a rotational center, the relative coordinates of the
     * point about which we actually want to rotate, and a rotation in radians, changes 
     * the center coordinates to those which should be used post-rotation to simulate a rotation 
     * about the relative point. Assumes axis of rotation is parallel to the z axis.
     * 
     * @param center
     * @param relPos
     * @param rot
     */
	private void adjustCenterPostRot(Vector3f center, Vector3f relPos, float rot)
    {  	
		Vector3f newRelPos = new Vector3f();
		newRelPos.x = (float) (Math.cos(rot) * relPos.x - Math.sin(rot) * relPos.y);
		newRelPos.y = (float) (Math.sin(rot) * relPos.x + Math.cos(rot) * relPos.y);
		
		newRelPos.x *= -1;
		newRelPos.y *= -1;
		
		Vector3f shift = new Vector3f();
		VectorUtil.add(shift, relPos, newRelPos);
		
    	center.x = center.x + shift.x;
    	center.y = center.y + shift.y;
    	
    }
}
