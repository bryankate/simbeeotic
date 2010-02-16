package harvard.robobees.simbeeotic;


import org.w3c.dom.Document;
import org.apache.log4j.Logger;
import harvard.robobees.simbeeotic.configuration.scenario.Scenario;
import harvard.robobees.simbeeotic.configuration.scenario.Colony;
import harvard.robobees.simbeeotic.configuration.scenario.ConfigProps;
import harvard.robobees.simbeeotic.configuration.world.World;
import harvard.robobees.simbeeotic.configuration.VariationIterator;
import harvard.robobees.simbeeotic.configuration.Variation;
import harvard.robobees.simbeeotic.configuration.ConfigurationAnnotations;
import harvard.robobees.simbeeotic.util.JaxbHelper;
import harvard.robobees.simbeeotic.util.BoundingSphere;
import harvard.robobees.simbeeotic.util.DocUtils;
import harvard.robobees.simbeeotic.environment.WorldMap;
import static harvard.robobees.simbeeotic.environment.PhysicalConstants.EARTH_GRAVITY;
import harvard.robobees.simbeeotic.model.Model;
import harvard.robobees.simbeeotic.model.PhysicalEntity;
import harvard.robobees.simbeeotic.model.EntityInfo;
import harvard.robobees.simbeeotic.model.Contact;

import javax.xml.bind.JAXBException;
import javax.vecmath.Vector3f;
import java.util.Random;
import java.util.Properties;
import java.util.Set;
import java.util.HashSet;

import com.bulletphysics.collision.dispatch.CollisionConfiguration;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.dispatch.CollisionWorld;
import com.bulletphysics.collision.broadphase.AxisSweep3;
import com.bulletphysics.collision.broadphase.OverlapFilterCallback;
import com.bulletphysics.collision.broadphase.BroadphaseProxy;
import com.bulletphysics.collision.narrowphase.PersistentManifold;
import com.bulletphysics.collision.narrowphase.ManifoldPoint;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.linearmath.Transform;
import com.google.inject.Injector;
import com.google.inject.Guice;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.name.Names;


/**
 * @author bkate
 */
public class SimController {

    private static Logger logger = Logger.getLogger(SimController.class);

    
    /**
     * Runs all simulation variations of a given scenario.
     *
     * @param scenarioDoc The raw scenario document, which will be parsed and executed.
     * @param worldDoc The raw world description document, which will be parsed and loaded.
     */
    public void runSim(final Document scenarioDoc, final Document worldDoc) {

        Scenario scenario;
        World world;

        try {

            scenario = JaxbHelper.objectFromNode(scenarioDoc, Scenario.class);
            world = JaxbHelper.objectFromNode(worldDoc, World.class);
        }
        catch(JAXBException je) {
            throw new RuntimeException("Could not parse the given scenario or world file.", je);
        }

        final float step = scenario.getSimulation().getStep();

        int currVariation = 0;
        VariationIterator variations = new VariationIterator(scenario);

        // todo: parallelize the running of variations
        for (Variation variation : variations) {

            final Random variationSeedGenertor = new Random(variation.getSeed());

            currVariation++;

            // setup a new world in the physics engine
            CollisionConfiguration collisionConfiguration = new DefaultCollisionConfiguration();
            CollisionDispatcher dispatcher = new CollisionDispatcher(collisionConfiguration);

            float aabbDim = world.getRadius() / (float)Math.sqrt(3);

            Vector3f worldAabbMin = new Vector3f(-aabbDim, -aabbDim, 0);
            Vector3f worldAabbMax = new Vector3f(aabbDim, aabbDim, aabbDim * 2);

            AxisSweep3 overlappingPairCache = new AxisSweep3(worldAabbMin, worldAabbMax);
            SequentialImpulseConstraintSolver solver = new SequentialImpulseConstraintSolver();

            final DiscreteDynamicsWorld dynamicsWorld = new DiscreteDynamicsWorld(dispatcher, overlappingPairCache,
                                                                                  solver, collisionConfiguration);

            dynamicsWorld.setGravity(new Vector3f(0, 0, EARTH_GRAVITY));

            // setup the simulated world (obstacle, flowers, etc)
            final WorldMap map = new WorldMap(world, dynamicsWorld, variationSeedGenertor.nextLong());

            // todo: setup environment


            // top level guice injector - all others are derived from this
            Injector injector = Guice.createInjector(new AbstractModule() {

                protected void configure() {

                    // random seed generator - this may not work well if the sim becomes multi-threaded
                    bind(Long.class).annotatedWith(Names.named("random-seed")).toProvider(new Provider<Long>() {

                        public Long get() {
                            return variationSeedGenertor.nextLong();
                        }
                    });

                    bindConstant().annotatedWith(Names.named("time-step")).to(step);

                    // dynamics world
                    bind(DiscreteDynamicsWorld.class).annotatedWith(ConfigurationAnnotations.GlobalScope.class).toInstance(dynamicsWorld);

                    // established simulated world instance
                    bind(WorldMap.class).annotatedWith(ConfigurationAnnotations.GlobalScope.class).toInstance(map);
                }
            });


            // create hive
            final Model hive;

            // todo: get z loc from world map?
            final float startX = scenario.getColony().getHive().getPosition().getX();
            final float startY = scenario.getColony().getHive().getPosition().getY();

            final Properties hiveProps = loadConfigProps(scenario.getColony().getHive().getProperties(), variation);
            final Class hiveClass;

            try {

                // locate the hive implementation
                hiveClass = Class.forName(scenario.getColony().getHive().getJavaClass());

                // make sure it extends AbstractHive
                if (!Model.class.isAssignableFrom(hiveClass)) {
                    throw new RuntimeException("The hive implementation must extend from Model.");
                }
            }
            catch(ClassNotFoundException cnf) {
                throw new RuntimeException("Could not locate the hive class: " + scenario.getColony().getHive().getJavaClass(), cnf);
            }

            Injector hiveInjector = injector.createChildInjector(new AbstractModule() {

                protected void configure() {

                    Names.bindProperties(binder(), hiveProps);

                    bindConstant().annotatedWith(Names.named("start-x")).to(startX + "");
                    bindConstant().annotatedWith(Names.named("start-y")).to(startY + "");

                    // hive class
                    bind(Model.class).to(hiveClass);

                    // a workaround for guice issue 282
                    bind(hiveClass);
                }
            });

            hive = hiveInjector.getInstance(Model.class);
            hive.initialize();


            // this callback is used to filter out collisions between bees when they are inside the hive
            dynamicsWorld.getBroadphase().getOverlappingPairCache().setOverlapFilterCallback(new OverlapFilterCallback() {

                public boolean needBroadphaseCollision(BroadphaseProxy objA, BroadphaseProxy objB) {

                    // check if both are bees by their collision group
                    if ((objA.collisionFilterGroup == PhysicalEntity.COLLISION_BEE) &&
                        (objB.collisionFilterGroup == PhysicalEntity.COLLISION_BEE)) {

                        // if either bee's center is inside the hive, do not allow a collision
                        BoundingSphere hiveBounds = hive.getTruthBoundingSphere();
                        Vector3f diff = new Vector3f();

                        diff.sub(hiveBounds.getCenter(),
                                 ((CollisionObject)objA.clientObject).getWorldTransform(new Transform()).origin);

                        if (diff.length() <= hiveBounds.getRadius()) {
                            return false;
                        }

                        diff.sub(hiveBounds.getCenter(),
                                 ((CollisionObject)objB.clientObject).getWorldTransform(new Transform()).origin);

                        if (diff.length() <= hiveBounds.getRadius()) {
                            return false;
                        }
                    }

                    // return the default collision filtering, which uses the collision group and mask
                    return ((objA.collisionFilterGroup & objB.collisionFilterMask) != 0) &&
                           ((objB.collisionFilterGroup & objA.collisionFilterMask) != 0);
                }
            });


            // create bees
            final Set<Model> bees = new HashSet<Model>();

            for (Colony.BeeGroup group : scenario.getColony().getBeeGroup()) {

                if (group.getCount() <= 0) {
                    throw new RuntimeException("Cannot create a bee swarm of non-positive size.");
                }

                final Class beeClass;

                try {

                    // locate the bee implementation
                    beeClass = Class.forName(group.getJavaClass());

                    // make sure it extends AbstractBee
                    if (!Model.class.isAssignableFrom(beeClass)) {
                        throw new RuntimeException("The bee implementation must extend from Model.");
                    }
                }
                catch(ClassNotFoundException cnf) {
                    throw new RuntimeException("Could not locate the bee class: " + group.getJavaClass(), cnf);
                }

                final Properties beeProps = loadConfigProps(group.getProperties(), variation);

                Injector beeInjector = injector.createChildInjector(new AbstractModule() {

                    protected void configure() {

                        Names.bindProperties(binder(), beeProps);

                        bindConstant().annotatedWith(Names.named("start-x")).to(startX + "");
                        bindConstant().annotatedWith(Names.named("start-y")).to(startY + "");

                        // bee class
                        bind(Model.class).to(beeClass);

                        // a workaround for guice issue 282
                        bind(beeClass);
                    }
                });

                for (int i = 0; i < group.getCount(); i++) {

                    Model b = beeInjector.getInstance(Model.class);

                    b.initialize();
                    bees.add(b);
                }
            }


            // setup a handler for dealing with contacts and informing objects
            // of when they collide
            ContactHandler contactHandler = new ContactHandler(dynamicsWorld);


            logger.info("");
            logger.info("--------------------------------------------");
            logger.info("Executing scenario variation " + currVariation);
            logger.info("--------------------------------------------");
            logger.info("");

            long variationStartTime = System.currentTimeMillis();

            // run it
            float endTime = scenario.getSimulation().getEndTime();
            float currTime = 0.0f;

            // todo: turn this into a DES
            while(currTime < endTime) {

                // update positions in physical world
//                dynamicsWorld.stepSimulation(step, 10);
                dynamicsWorld.stepSimulation(step, 100, 1.0f / 240.0f);

                currTime += step;

                // todo: put this inside an internal tick callback
                // update collisions
                contactHandler.update();

                hive.sampleKinematics(step);
                hive.update(currTime);

                for (Model b : bees) {

                    // todo: put this inside an internal tick callback
                    // sample kinematic state to produce a truth
                    // measurement of accelerations
                    b.sampleKinematics(step);

                    // update model behaviors
                    b.update(currTime);
                }
            }

            logger.info("");
            logger.info("--------------------------------------------");
            logger.info("Scenario variation " + currVariation + " executed in " +
                        (double)(System.currentTimeMillis() - variationStartTime) / 1000.0 + " seconds.");
            logger.info("--------------------------------------------");

            // cleanup
            map.destroy();
            hive.destroy();

            for (Model b : bees) {
                b.destroy();
            }
        }
    }


    /**
     * Loads configuration properties into a Properties object and substitutes values
     * according to the variable map for the given variation, if necessary.
     *
     * @param config The configuration properties defined in the scenario file.
     * @param variation The map of current variable values.
     *
     * @return The loaded properties.
     */
    private Properties loadConfigProps(ConfigProps config, Variation variation) {

        Properties resolved = new Properties();

        if (config == null) {
            return resolved;
        }

        for (ConfigProps.Prop prop : config.getProp()) {

            String val = prop.getValue();

            if (DocUtils.isPlaceholder(val)) {

                String var = DocUtils.extractPlaceholderName(val);

                if (variation.getVariables().containsKey(var)) {
                    val = variation.getVariables().get(val);
                }
                else {

                    logger.warn("The variable '" + prop.getName() + "' has not been set.");

                    String def = DocUtils.extractPlaceholderDefault(val);

                    if (def != null) {
                        val = def;
                    }
                    else {
                        logger.warn("The variable '" + prop.getName() + "' has no default.");
                    }
                }
            }

            resolved.setProperty(prop.getName(), val);
        }

        return resolved;
    }


    /**
     * A class that iterates through contacts and informs each object of its contact.
     */
    private static final class ContactHandler {

        private CollisionWorld world;
        private Set<CollisionObject> touching = new HashSet<CollisionObject>();


        public ContactHandler(CollisionWorld world) {
            this.world = world;
        }


        public void update() {

            // remove old contacts
            for (CollisionObject obj : touching) {
                ((EntityInfo)obj.getUserPointer()).getContactPoints().clear();
            }

            touching.clear();

            int numManifolds = world.getDispatcher().getNumManifolds();

            for (int i = 0; i < numManifolds; i++) {

                PersistentManifold manifold = world.getDispatcher().getManifoldByIndexInternal(i);
                CollisionObject objectA = (CollisionObject)manifold.getBody0();
                CollisionObject objectB = (CollisionObject)manifold.getBody1();

                int numPoints = manifold.getNumContacts();

                for (int j = 0; j < numPoints; j++) {

                    ManifoldPoint point = manifold.getContactPoint(j);

                    Contact contactA = new Contact(point.localPointA,
                                                   point.getPositionWorldOnA(new Vector3f()),
                                                   ((EntityInfo)objectB.getUserPointer()).getProperties());

                    Contact contactB = new Contact(point.localPointB,
                                                   point.getPositionWorldOnB(new Vector3f()),
                                                   ((EntityInfo)objectA.getUserPointer()).getProperties());

                    // add the contact points to the objects
                    ((EntityInfo)objectA.getUserPointer()).getContactPoints().add(contactA);
                    ((EntityInfo)objectB.getUserPointer()).getContactPoints().add(contactB);
                }

                if (numPoints > 0) {

                    touching.add(objectA);
                    touching.add(objectB);
                }
            }
        }
    }
    
}
