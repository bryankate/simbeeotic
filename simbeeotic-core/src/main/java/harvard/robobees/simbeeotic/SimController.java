package harvard.robobees.simbeeotic;


import org.w3c.dom.Document;
import org.apache.log4j.Logger;
import harvard.robobees.simbeeotic.configuration.scenario.Scenario;
import harvard.robobees.simbeeotic.configuration.scenario.Colony;
import harvard.robobees.simbeeotic.configuration.scenario.ConfigProps;
import harvard.robobees.simbeeotic.configuration.world.World;
import harvard.robobees.simbeeotic.configuration.VariationIterator;
import harvard.robobees.simbeeotic.configuration.Variation;
import harvard.robobees.simbeeotic.configuration.ConfigurationAnnotations.GlobalScope;
import harvard.robobees.simbeeotic.util.JaxbHelper;
import harvard.robobees.simbeeotic.util.BoundingSphere;
import harvard.robobees.simbeeotic.util.DocUtils;
import harvard.robobees.simbeeotic.environment.WorldMap;
import static harvard.robobees.simbeeotic.environment.PhysicalConstants.EARTH_GRAVITY;
import harvard.robobees.simbeeotic.model.PhysicalEntity;
import harvard.robobees.simbeeotic.model.EntityInfo;
import harvard.robobees.simbeeotic.model.Contact;
import harvard.robobees.simbeeotic.model.PhysicalModel;
import harvard.robobees.simbeeotic.model.SimTime;
import harvard.robobees.simbeeotic.model.Event;
import harvard.robobees.simbeeotic.model.Model;
import harvard.robobees.simbeeotic.comms.PropagationModel;
import harvard.robobees.simbeeotic.comms.DefaultPropagationModel;

import javax.xml.bind.JAXBException;
import javax.vecmath.Vector3f;
import java.util.Random;
import java.util.Properties;
import java.util.Set;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Map;
import java.util.HashMap;

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

    // JBullet time stepping params
    private static final int DEFAULT_MAX_SUBSTEPS = 100;
    private static final double DEFAULT_STEP = 0.1;
    private static final double DEFAULT_SUBSTEP = 1.0 / 240.0;

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

            // make a new clock
            final SimEngineImpl engine = new SimEngineImpl();

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

            // top level guice injector - all others are derived from this
            Injector injector = Guice.createInjector(new AbstractModule() {

                protected void configure() {

                    // random seed generator - this may not work well if the sim becomes multi-threaded
                    bind(Long.class).annotatedWith(Names.named("random-seed")).toProvider(new Provider<Long>() {

                        public Long get() {
                            return variationSeedGenertor.nextLong();
                        }
                    });

                    // model ID generator - this may not work well if the sim becomes multi-threaded
                    bind(Integer.class).annotatedWith(Names.named("model-id")).toProvider(new Provider<Integer>() {

                        int nextId = 1;

                        public Integer get() {
                            return nextId++;
                        }
                    });

                    // the global access to timing information
                    bind(SimEngine.class).annotatedWith(GlobalScope.class).toInstance(engine);

                    // dynamics world
                    bind(DiscreteDynamicsWorld.class).annotatedWith(GlobalScope.class).toInstance(dynamicsWorld);

                    // established simulated world instance
                    bind(WorldMap.class).annotatedWith(GlobalScope.class).toInstance(map);
                }
            });


            // setup the RF environment
            final PropagationModel commModel;
            final Properties commModelProps;
            final Class commModelClass;

            if (scenario.getComms() != null) {

                commModelProps = loadConfigProps(scenario.getComms().getPropagationModel().getProperties(), variation);

                try {

                    // locate the propagation model implementation
                    commModelClass = Class.forName(scenario.getComms().getPropagationModel().getJavaClass());

                    // make sure it implements PropagationModel
                    if (!PropagationModel.class.isAssignableFrom(commModelClass)) {
                        throw new RuntimeException("The propagation model implementation must extend from PropagationModel.");
                    }
                }
                catch(ClassNotFoundException cnf) {
                    throw new RuntimeException("Could not locate the propagation model class: " + scenario.getComms().getPropagationModel().getJavaClass(), cnf);
                }
            }
            else {

                commModelProps = new Properties();
                commModelClass = DefaultPropagationModel.class;
            }

            Injector propModelInjector = injector.createChildInjector(new AbstractModule() {

                protected void configure() {

                    Names.bindProperties(binder(), commModelProps);

                    // comm model class
                    bind(PropagationModel.class).to(commModelClass);

                    // a workaround for guice issue 282
                    bind(commModelClass);
                }
            });

            commModel = propModelInjector.getInstance(PropagationModel.class);

            // update the main injector to contain the global comm model
            injector = injector.createChildInjector(new AbstractModule() {

                protected void configure() {

                    // established simulated world instance
                    bind(PropagationModel.class).annotatedWith(GlobalScope.class).toInstance(commModel);
                }
            });

            
            // todo: setup weather


            // create hive
            final PhysicalModel hive;

            // todo: get z loc from world map?
            final float startX = scenario.getColony().getHive().getPosition().getX();
            final float startY = scenario.getColony().getHive().getPosition().getY();

            final Properties hiveProps = loadConfigProps(scenario.getColony().getHive().getProperties(), variation);
            final Class hiveClass;

            try {

                // locate the hive implementation
                hiveClass = Class.forName(scenario.getColony().getHive().getJavaClass());

                // make sure it implements PhysicalModel
                if (!PhysicalModel.class.isAssignableFrom(hiveClass)) {
                    throw new RuntimeException("The hive implementation must extend from PhysicalModel.");
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
                    bind(PhysicalModel.class).to(hiveClass);

                    // a workaround for guice issue 282
                    bind(hiveClass);
                }
            });

            hive = hiveInjector.getInstance(PhysicalModel.class);

            engine.addModel(hive);
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
            final Set<PhysicalModel> bees = new HashSet<PhysicalModel>();

            for (Colony.BeeGroup group : scenario.getColony().getBeeGroup()) {

                if (group.getCount() <= 0) {
                    throw new RuntimeException("Cannot create a bee swarm of non-positive size.");
                }

                final Class beeClass;

                try {

                    // locate the bee implementation
                    beeClass = Class.forName(group.getJavaClass());

                    // make sure it implements PhysicalModel
                    if (!PhysicalModel.class.isAssignableFrom(beeClass)) {
                        throw new RuntimeException("The bee implementation must extend from PhysicalModel.");
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
                        bind(PhysicalModel.class).to(beeClass);

                        // a workaround for guice issue 282
                        bind(beeClass);
                    }
                });

                for (int i = 0; i < group.getCount(); i++) {

                    PhysicalModel b = beeInjector.getInstance(PhysicalModel.class);

                    engine.addModel(b);
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
            SimTime nextSimTime = engine.getNextEventTime();
            double endTime = scenario.getSimulation().getEndTime();
            double lastPhysicsUpdate = 0;

            while((nextSimTime != null) && (nextSimTime.getTime() < endTime)) {

                double currTime = nextSimTime.getTime();

                if (logger.isDebugEnabled()) {
                    logger.debug("Executing event at time: " + currTime);
                }

                // update positions in physical world so that all
                // objects are up to date with the event time
                if (currTime > lastPhysicsUpdate) {

                    double diff = currTime - lastPhysicsUpdate;

                    while(diff > 0) {

                        dynamicsWorld.stepSimulation((float)Math.min(DEFAULT_STEP, diff),
                                                     DEFAULT_MAX_SUBSTEPS, 
                                                     (float)DEFAULT_SUBSTEP);

                        diff -= DEFAULT_STEP;
                    }

                    // todo: put this inside an internal tick callback or make asynch?
                    // update collisions
                    contactHandler.update();

                    lastPhysicsUpdate = currTime;
                }

                nextSimTime = engine.processNextEvent();

                // todo: put this inside an internal tick callback
//                hive.sampleKinematics(step);
//
//                for (PhysicalModel b : bees) {
//
//                    // sample kinematic state to produce a truth
//                    // measurement of accelerations
//                    b.sampleKinematics(step);
//                }
            }

            logger.info("");
            logger.info("--------------------------------------------");
            logger.info("Scenario variation " + currVariation + " executed in " +
                        (double)(System.currentTimeMillis() - variationStartTime) / 1000.0 + " seconds.");
            logger.info("--------------------------------------------");

            // cleanup
            map.destroy();
            hive.destroy();

            for (PhysicalModel b : bees) {
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


    /**
     * An implementation of {@link SimEngine} that is used as a container
     * and coordinator for events in each scenario variation.
     *
     * @note This class is not thread safe, so it would need to be updated if
     *       multiple models are allowed to execute in parallel in the future.
     */
    static final class SimEngineImpl implements SimEngine {

        private Queue<ScheduledEvent> eventQ = new PriorityQueue<ScheduledEvent>();
        private Map<Integer, Model> modelMap = new HashMap<Integer, Model>();

        private SimTime processing = null;


        /** {@inheritDoc} */
        @Override
        public SimTime getCurrentTime() {
            return processing;
        }


        /** {@inheritDoc} */
        @Override
        public void scheduleEvent(final int modelId, final SimTime time, final Event event) {

            SimTime minTime = processing;

            if ((minTime == null) && (eventQ.peek() != null)) {
                minTime = eventQ.peek().time;
            }

            // the user is trying to schedule an event for a time in the past
            if ((minTime != null) && (time.compareTo(minTime) < 0)) {

                throw new CausalityViolationException("The time of the event (" + time +
                                                      ") is prior to GVT (" + minTime + ").");
            }

            Model model = modelMap.get(modelId);

            if (model == null) {
                throw new ModelNotFoundException();
            }

            // add it to the queue
            eventQ.add(new ScheduledEvent(time, event, model));
        }


        /**
         * Gets the time of the event that is at the head of the queue.
         *
         * @return The {@link SimTime} of the next {@link Event} to be processed,
         *         or {@code null} if there are no events scheduled.
         */
        public SimTime getNextEventTime() {

            ScheduledEvent next = eventQ.peek();

            if (next != null) {
                return next.time;
            }

            return null;
        }


        /**
         * Processes the next event in the queue. This method will block while the
         * event is being processed.
         *
         * @return The time of the event <i>following</i> the event that was just
         *         processed, which is the head of the event queue. This is equivalent
         *         to calling {@link #getNextEventTime()} immediately after this call.
         */
        public SimTime processNextEvent() {

            ScheduledEvent next = eventQ.poll();

            if (next != null) {

                processing = next.time;

                next.model.processEvent(next.time, next.event);
            }

            processing = null;
            
            return getNextEventTime();
        }


        /**
         * Adds a model to the simulation. Only models that have been added can have events
         * scheduled on them.
         *
         * @param model The model that is capable of event execution.
         */
        public void addModel(final Model model) {
            modelMap.put(model.getModelId(), model);
        }


        /**
         * A container that holds the details of an event to be processed in the future.
         */
        private static class ScheduledEvent implements Comparable<ScheduledEvent> {

            public SimTime time;
            public Event event;
            public Model model;


            public ScheduledEvent(SimTime time, Event event, Model model) {

                this.time = time;
                this.event = event;
                this.model = model;
            }

            /**
             * We need an absolute (deterministic) sorting of scheduled events, so we use the
             * time and model ID as a tiebreaker.
             *
             * @param o The other event.
             *
             * @return An integer less than 0 if this event should come before the other, greater
             *         than zero if it should come after the other, and 0 if there is no difference
             *         in the order of processing.
             */
            @Override
            public int compareTo(ScheduledEvent o) {

                int timeComp = time.compareTo(o.time);

                if (timeComp == 0) {
                    return Integer.valueOf(model.getModelId()).compareTo(o.model.getModelId());
                }

                return timeComp;
            }
        }
    }
}
