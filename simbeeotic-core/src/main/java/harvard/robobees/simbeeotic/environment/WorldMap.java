package harvard.robobees.simbeeotic.environment;


import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.ConeShapeZ;
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

import com.google.inject.Inject;
import com.google.inject.name.Named;
import harvard.robobees.simbeeotic.configuration.ConfigurationAnnotations.GlobalScope;
import harvard.robobees.simbeeotic.configuration.world.Box;
import harvard.robobees.simbeeotic.configuration.world.Cone;
import harvard.robobees.simbeeotic.configuration.world.Cylinder;
import harvard.robobees.simbeeotic.configuration.world.Man;
import harvard.robobees.simbeeotic.configuration.world.Meta;
import harvard.robobees.simbeeotic.configuration.world.Obstacle;
import harvard.robobees.simbeeotic.configuration.world.Patch;
import harvard.robobees.simbeeotic.configuration.world.Person;
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
import javax.vecmath.Vector2f;
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
    private Set<WorldObject> people = new HashSet<WorldObject>();
    private Map<Integer, Double> approxPatchDensity = new HashMap<Integer, Double>();

    // params
    private boolean approximatePatches = false;
    private float stemHeight = 0.2f;   // m
    private float stemRadius = 0.01f;  // m
    private float floraHeight = 0.02f; // m
    private float floraRadius = 0.1f;  // m


    public void initialize() {

        // setup the inifinite ground plane
        CollisionShape groundShape = new StaticPlaneShape(new Vector3f(0, 0, 1), 0);
        Transform groundTransform = new Transform();

        groundTransform.setIdentity();
        groundTransform.origin.set(new Vector3f(0, 0, 0));

        int groundId = nextId.getAndIncrement();

        recorder.updateShape(groundId, groundShape);
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
        
        // setup people
        if (world.getPeople() != null) {
        	
        	for (Person person: world.getPeople().getPerson()) {
        		
        		if (person.getMan() != null) {
        			
        			Transform startTransform = null;

                    Map<String, Object> meta = loadProperties(person.getMeta());

                    int id = nextId.getAndIncrement();
                    EntityInfo info = new EntityInfo(id, meta);

                    String label = null;
                    Color color = new Color(205, 175, 149, 128);

                    if (person.getColor() != null) {
                    	
                        color = new Color(person.getColor().getRed(), 
                        				  person.getColor().getGreen(), 
                        				  person.getColor().getBlue(),
                        				  person.getColor().getAlpha());
                    }

        			Man man = person.getMan();

	            	// extract properties of the man from the xml
	            	Vector3f position = new Vector3f(man.getPosition().getX(),
	            			     					 man.getPosition().getY(),
	            									 man.getPosition().getZ());

	            	float width = man.getWidth();
	            	float height = man.getHeight();
	            	float xscale = width / 13.0f;
	            	float thickness = 0.075f * height;
	            	float zscale = height / 13.0f;

	                startTransform = new Transform();
	                startTransform.setIdentity();

	                // set up center
	                Vector3f center = new Vector3f(position.x,
	                		    				   position.y,
	                							   position.z + height/2.0f);

	                CompoundShape compoundShape = new CompoundShape();

	                CollisionShape colShape = null;
	                
                	// left leg
                	colShape = new BoxShape(new Vector3f(0.5f * xscale, thickness, 2.5f * zscale));

                	startTransform.origin.set(new Vector3f(-1 * xscale, 0, -4.0f * zscale));
                	compoundShape.addChildShape(startTransform, colShape);

                	// right leg
                	colShape = new BoxShape(new Vector3f(0.5f * xscale, thickness, 2.5f * zscale));

                	startTransform.origin.set(new Vector3f(1 * xscale, 0, -4 * zscale));
                	compoundShape.addChildShape(startTransform, colShape);

                	// torso
                	colShape = new BoxShape(new Vector3f(1.5f * xscale, thickness, 2.5f * zscale));

                	startTransform.origin.set(new Vector3f(0 * xscale, 0, 1 * zscale));
                	compoundShape.addChildShape(startTransform, colShape);
                	
                	// left arm
                	colShape = new BoxShape(new Vector3f(2.5f * xscale, thickness, 0.5f * zscale));

                	startTransform.origin.set(new Vector3f(-4 * xscale, 0, 3 * zscale));
                	compoundShape.addChildShape(startTransform, colShape);
                	
                	// right arm
                	colShape = new BoxShape(new Vector3f(2.5f * xscale, thickness, 0.5f * zscale));

                	startTransform.origin.set(new Vector3f(4 * xscale, 0, 3 * zscale));
                	compoundShape.addChildShape(startTransform, colShape);
                	
                	// neck
                	colShape = new BoxShape(new Vector3f(0.5f * xscale, thickness, 0.5f * zscale));

                	startTransform.origin.set(new Vector3f(0 * xscale, 0, 4 * zscale));
                	compoundShape.addChildShape(startTransform, colShape);
                	
                	// head
                	colShape = new BoxShape(new Vector3f(1 * xscale, thickness, 1 * zscale));

                	startTransform.origin.set(new Vector3f(0 * xscale, 0, 5.5f * zscale));
                	compoundShape.addChildShape(startTransform, colShape);
                	
                	
                	// necessary I think?
            		compoundShape.recalculateLocalAabb();

            		// setup the transform for the full compound shape
            		// correct for rotation to place the surface such that the lower left corner is in position
            		startTransform.origin.set(center);

            		// add the compound shape into the world!
            		addBody(WorldObject.Type.PERSON, startTransform, compoundShape, color, null, label, info, people);
        		}
        	}
        }

        // setup flower patches
        if (world.getFlowers() != null) {

            for (Patch patch : world.getFlowers().getPatch()) {

                float radius = patch.getRadius();
                float diam = radius * 2;

                Color color = new Color(255, 102, 151);

                if (patch.getColor() != null) {

                    color = new Color(patch.getColor().getRed(),
                                      patch.getColor().getGreen(),
                                      patch.getColor().getBlue(),
                                      patch.getColor().getAlpha());
                }

                if (!approximatePatches) {

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

                        addBody(WorldObject.Type.FLOWER, trans, shape, color, null, null, flowerInfo, flowers);
                    }
                }
                else {

                    EntityInfo patchInfo = new EntityInfo(nextId.getAndIncrement(), loadProperties(patch.getMeta()));
                    Transform trans = new Transform();
                    float halfHeight = 0.01f;

                    trans.setIdentity();
                    trans.origin.set(new Vector3f(patch.getCenter().getX(),
                                                  patch.getCenter().getY(),
                                                  halfHeight));

                    CollisionShape shape = new CylinderShapeZ((new Vector3f(patch.getRadius(), patch.getRadius(), halfHeight)));

                    color = new Color(color.getRed(), color.getGreen(), color.getBlue(), 128);

                    approxPatchDensity.put(patchInfo.getObjectId(), (double)patch.getDensity());
                    addBody(WorldObject.Type.FLOWER, trans, shape, color, null, null, patchInfo, flowers);
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
     * For approximate flower patches, the patch and intersecting area are modeled
     * as circles.
     *
     * @param center The center of the query sphere.
     * @param radius The radius of the query sphere.
     *
     * @return The set of all flowers in the given area.
     */
    public Set<WorldObject> getFlowers(Vector3f center, double radius) {

        Set<WorldObject> in = new HashSet<WorldObject>();

        for (WorldObject obj : flowers) {

            if (!approximatePatches) {

                Vector3f diff = new Vector3f();

                diff.sub(center, obj.getTruthPosition());

                if (diff.length() <= radius) {
                    in.add(obj);
                }
            }
            else {

                // we need to approximate detecting individual flowers using the
                // size of the patch, the amount of overlap, and the density

                Vector2f patchCenter = new Vector2f(obj.getTruthPosition().x, obj.getTruthPosition().y);
                Vector2f queryCenter = new Vector2f(center.x, center.y);

                Vector2f temp = new Vector2f();
                temp.sub(queryCenter, patchCenter);

                double dist = temp.length();
                double distSq = temp.lengthSquared();
                double patchRadius = obj.getTruthBoundingSphere().getRadius();
                double patchRadiusSq = patchRadius * patchRadius;
                double radiusSq = (float)(radius * radius);
                double intersectedArea = 0;

                if (dist >= (patchRadius + radius)) {

                    // the query circle is outside the patch
                    continue;
                }
                else if (dist + radius <= patchRadius) {

                    // the query is inscribed
                    intersectedArea = Math.PI * radiusSq;
                }
                else {

                    // the circles partially intersect
                    double theta1 = 2 * Math.acos((radiusSq + distSq - patchRadiusSq) / (2 * radius * dist));
                    double theta2 = 2 * Math.acos((patchRadiusSq + distSq - radiusSq) / (2 * patchRadius * dist));

                    intersectedArea = (0.5 * theta1 * radiusSq) - (0.5 * radiusSq * Math.sin(theta1)) +
                                      (0.5 * theta2 * patchRadiusSq) - (0.5 * patchRadiusSq * Math.sin(theta2));
                }

                // use the density and the intersected area to determine how likely
                // we are to see a flower. then do a random draw to see if a flower is seen
                if (rand.nextDouble() <= (approxPatchDensity.get(obj.getObjectId()) * intersectedArea)) {
                    in.add(obj);
                }
            }
        }

        return in;
    }
    
    /**
     * Gets the people that are contained within the right cone defining the field of view. The
     * person is determined to be in the cone if its center is in the cone.
     *
     * @param origin The center of the sensor.
     * @param pointing Direction in which sensor looks. Assume normalized.
     * @param range The length of the field-of-view cone.
     * @param halfAngle Angle in radians of half of the field of view.
     *
     * @return The set of all people in the given area.
     */
    public Set<WorldObject> getPeople(Vector3f origin, Vector3f pointing, double range, float halfAngle) {

        Set<WorldObject> in = new HashSet<WorldObject>();

        for (WorldObject obj : people) {
        	
        	// vector from sensor to object
            Vector3f relPos = new Vector3f();
            relPos.sub(obj.getTruthPosition(), origin);
            
            pointing.normalize();
            
            // distance between sensor and object along direction in which sensor is pointed
            float component = pointing.dot(relPos);
            
            // check that component falls within range of cone
            if(component >= 0 && component <= range) {
            	
            	// get vector projection
            	Vector3f proj = new Vector3f();
            	proj.get(pointing);
            	proj.scale(component);
            	
            	// get orthogonal component
            	Vector3f orthogonal = new Vector3f();
            	orthogonal.sub(relPos, proj);
            	float orthogonalComponent = proj.length();
            	
            	// 'right angle cone' portion
            	if(component <= range * Math.cos(halfAngle)) {
	            	// determine whether orthogonal component places the object in the cone
	            	if(orthogonalComponent <= Math.tan(halfAngle) * component)
	            		in.add(obj);
            	}
            	// 'oblique' portion
            	else {
            		float arcHeight = (float) range - component;
            		// get chord length from radius and arc height
            		float coneWidth = ((float) Math.sqrt((8 * arcHeight) * (range - arcHeight/2)))/2;
            		if(orthogonalComponent <= coneWidth)
            			in.add(obj);
            	}
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

        recorder.updateShape(id, colShape);
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
            case PERSON:

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

    
    @Inject
    public final void setRandomSeed(@Named("random-seed") final long seed) {
        this.rand = new Random(seed);
    }


    @Inject
    public final void setNextId(@Named("next-id") final AtomicInteger next) {
        this.nextId = next;
    }


    @Inject
    public final void setWorld(final World world) {
        this.world = world;
    }


    @Inject
    public final void setDynamicsWorld(@GlobalScope final DiscreteDynamicsWorld dynamicsWorld) {
        this.dynamicsWorld = dynamicsWorld;
    }


    @Inject
    public final void setMotionRecorder(@GlobalScope final MotionRecorder recorder) {
        this.recorder = recorder;
    }


    @Inject(optional = true)
    public final void setApproximatePatches(@Named("approximate-patches") final boolean use) {
        approximatePatches = use;
    }


    @Inject(optional = true)
    public final void setStemHeight(@Named("flower-stem-height") final float height) {
        stemHeight = height;
    }


    @Inject(optional = true)
    public final void setStemRadius(@Named("flower-stem-radius") final float rad) {
        stemRadius = rad;
    }


    @Inject(optional = true)
    public final void setFloraHeight(@Named("flower-flora-height") final float height) {
        floraHeight = height;
    }


    @Inject(optional = true)
    public final void setFloraRadius(@Named("flower-flora-radius") final float rad) {
        floraRadius = rad;
    }
}
