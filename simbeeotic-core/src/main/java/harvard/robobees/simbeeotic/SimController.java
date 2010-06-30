package harvard.robobees.simbeeotic;


import com.bulletphysics.collision.broadphase.AxisSweep3;
import com.bulletphysics.collision.dispatch.CollisionConfiguration;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.dispatch.CollisionWorld;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.collision.narrowphase.ManifoldPoint;
import com.bulletphysics.collision.narrowphase.PersistentManifold;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import harvard.robobees.simbeeotic.configuration.ConfigurationAnnotations.GlobalScope;
import harvard.robobees.simbeeotic.configuration.Variation;
import harvard.robobees.simbeeotic.configuration.VariationIterator;
import harvard.robobees.simbeeotic.configuration.InvalidScenarioException;
import harvard.robobees.simbeeotic.configuration.scenario.ConfigProps;
import harvard.robobees.simbeeotic.configuration.scenario.Scenario;
import harvard.robobees.simbeeotic.configuration.scenario.ModelConfig;
import harvard.robobees.simbeeotic.configuration.scenario.SensorConfig;
import harvard.robobees.simbeeotic.configuration.scenario.RadioConfig;
import harvard.robobees.simbeeotic.configuration.scenario.Vector;
import harvard.robobees.simbeeotic.configuration.scenario.CustomClass;
import harvard.robobees.simbeeotic.configuration.world.World;
import static harvard.robobees.simbeeotic.environment.PhysicalConstants.EARTH_GRAVITY;
import harvard.robobees.simbeeotic.environment.WorldMap;
import harvard.robobees.simbeeotic.model.Contact;
import harvard.robobees.simbeeotic.model.EntityInfo;
import harvard.robobees.simbeeotic.model.Model;
import harvard.robobees.simbeeotic.model.Event;
import harvard.robobees.simbeeotic.model.PhysicalEntity;
import harvard.robobees.simbeeotic.model.CollisionEvent;
import harvard.robobees.simbeeotic.model.MotionRecorder;
import harvard.robobees.simbeeotic.model.sensor.AbstractSensor;
import harvard.robobees.simbeeotic.util.DocUtil;
import harvard.robobees.simbeeotic.model.comms.AntennaPattern;
import harvard.robobees.simbeeotic.model.comms.IsotropicAntenna;
import harvard.robobees.simbeeotic.model.comms.AbstractRadio;
import harvard.robobees.simbeeotic.component.VariationComponent;
import org.apache.log4j.Logger;

import javax.vecmath.Vector3f;
import java.util.HashSet;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.List;
import java.util.LinkedList;
import java.util.Queue;
import java.util.PriorityQueue;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;


/**
 * The entry point to the simulation. The controller takes descriptions of the scenario and world
 * and executes the variations.
 *
 * @author bkate
 */
public class SimController {

    private static final double DEFAULT_STEP = 0.1;               // s
    private static final double DEFAULT_SUBSTEP = 1.0 / 240.0;    // s

    private static Logger logger = Logger.getLogger(SimController.class);

    
    /**
     * Runs all simulation variations of a given scenario. Each variation
     * will be executed as fast as possible, meaning that virtual time may
     * progress faster than real time.
     *
     * @param scenario The scenario, describing the models to execute.
     * @param world The world in which the models operate.
     */
    public void runSim(final Scenario scenario, final World world) {
        runSim(scenario, world, 0);
    }


    /**
     * Behaves identically to {@link #runSim(Scenario, World)} with the exception that
     * an attempt is made to constrain virtual time to real time, meaning that the
     * simulation may not run as fast as possible.
     *
     * @param scenario The scenario, describing the models to execute.
     * @param world The world in which the models operate.
     * @param realTimeScale The scale factor for constraining real time. A scale less
     *                      than or equal to zero indicates that no constraint should be made,
     *                      and values greater than zero will scale the virtual time accordingly.
     *                      For example, a value of 2 will allow the simulation to progress,
     *                      at most, twice the rate of real time.
     *
     */
    public void runSim(final Scenario scenario, final World world, double realTimeScale) {

        int currVariation = 0;
        VariationIterator variations = new VariationIterator(scenario);

        // todo: parallelize the running of variations
        for (final Variation variation : variations) {

            final int varId = ++currVariation;

            logger.info("");
            logger.info("--------------------------------------------");
            logger.info("Executing scenario variation " + currVariation);
            logger.info("--------------------------------------------");
            logger.info("");

            final AtomicInteger nextModelId = new AtomicInteger(0);
            final AtomicInteger nextMotionId = new AtomicInteger(0);
            final Random variationSeedGenertor = new Random(variation.getSeed());

            // make a new clock
            final SimEngineImpl simEngine = new SimEngineImpl(realTimeScale);
            final ClockControl clockControl = new ClockControl();

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

            dynamicsWorld.setGravity(new Vector3f(0, 0, (float)EARTH_GRAVITY));

            final MotionRecorder motionRecorder = new MotionRecorder();


            // top level guice injector - all others are derived from this
            Module baseModule = new AbstractModule() {

                protected void configure() {

                    // the variation number of this scenario variation
                    bindConstant().annotatedWith(Names.named("variation-number")).to(varId);

                    // scenario variation variable map
                    bind(Variation.class).annotatedWith(Names.named("variation")).toInstance(variation);

                    // the global access to sim engine executive
                    bind(SimEngine.class).annotatedWith(GlobalScope.class).toInstance(simEngine);

                    // clock controller for the sim engine
                    bind(ClockControl.class).annotatedWith(GlobalScope.class).toInstance(clockControl);

                    // dynamics world
                    bind(DiscreteDynamicsWorld.class).annotatedWith(GlobalScope.class).toInstance(dynamicsWorld);

                    // motion recorder
                    bind(MotionRecorder.class).annotatedWith(GlobalScope.class).toInstance(motionRecorder);
                }

                // todo: figure out how to get these providers to not be called for each child injector?

                @Provides @Named("random-seed")
                public long generateRandomSeed() {
                    return variationSeedGenertor.nextLong();
                }
            };

            Injector baseInjector = Guice.createInjector(baseModule);


            // establish components
            final List<VariationComponent> varComponents = new LinkedList<VariationComponent>();

            if (scenario.getComponents() != null) {

                for (CustomClass config : scenario.getComponents().getVariation()) {

                    final Class compClass;
                    final Properties compProps = loadConfigProps(config.getProperties(), variation);

                    try {

                        // locate the model implementation
                        compClass = Class.forName(config.getJavaClass());

                        // make sure it implements Model
                        if (!VariationComponent.class.isAssignableFrom(compClass)) {
                            throw new RuntimeException("The component implementation must extend from VariationComponent.");
                        }
                    }
                    catch(ClassNotFoundException cnf) {
                        throw new RuntimeException("Could not locate the component class: " +
                                                   config.getJavaClass(), cnf);
                    }

                    Injector compInjector = baseInjector.createChildInjector(new AbstractModule() {

                        @Override
                        protected void configure() {

                            Names.bindProperties(binder(), compProps);

                            // component class
                            bind(VariationComponent.class).to(compClass);
                        }
                    });


                    VariationComponent component = compInjector.getInstance(VariationComponent.class);

                    component.initialize();
                    varComponents.add(component);
                }
            }


            // setup the simulated world (obstacle, flowers, etc)
            final WorldMap map = new WorldMap(world, dynamicsWorld, motionRecorder, nextMotionId, variationSeedGenertor.nextLong());

            baseInjector = baseInjector.createChildInjector(new AbstractModule() {

                @Override
                protected void configure() {

                    // established simulated world instance
                    bind(WorldMap.class).annotatedWith(GlobalScope.class).toInstance(map);
                }
            });


            // parse model definitions
            final List<Model> models = new LinkedList<Model>();

            if (scenario.getModels() != null) {
                
                for (ModelConfig config : scenario.getModels().getModel()) {
                    parseModelConfig(config, null, null, models, variation, baseInjector, nextModelId, nextMotionId);
                }
            }

            for (Model model : models) {
                simEngine.addModel(model);
            }

            // initialize all models
            for (Model model : models) {
                model.initialize();
            }


            // setup a handler for dealing with contacts and informing objects
            // of when they collide
            ContactHandler contactHandler = new ContactHandler(dynamicsWorld, simEngine);

            // run it
            long variationStartTime = System.currentTimeMillis();

            SimTime lastSimTime = new SimTime(0);
            SimTime nextSimTime = simEngine.getNextEventTime();
            double endTime = scenario.getSimulation().getEndTime();

            while((nextSimTime != null) && (nextSimTime.getImpreciseTime() < endTime)) {

                clockControl.waitUntilStarted();

                if (logger.isDebugEnabled()) {
                    logger.debug("Executing event at time: " + nextSimTime);
                }

                // update positions in physical world so that all
                // objects are up to date with the event time
                if (nextSimTime.getTime() > lastSimTime.getTime()) {

                    double diff = nextSimTime.getImpreciseTime() - lastSimTime.getImpreciseTime();
                    long updatedTime = 0;

                    while(diff > 0) {

                        double step = Math.min(DEFAULT_STEP, diff);

                        dynamicsWorld.stepSimulation((float)step,
                                                     (int)Math.ceil(step / DEFAULT_SUBSTEP),
                                                     (float)DEFAULT_SUBSTEP);

                        // keep track of how far ahead the physics engine is getting from the last processed event time
                        updatedTime += (long)(step * TimeUnit.SECONDS.toNanos(1));

                        // update collisions
                        if (contactHandler.update(lastSimTime, updatedTime)) {
                            break;
                        }

                        diff -= DEFAULT_STEP;
                    }

                    lastSimTime = simEngine.getNextEventTime();
                }

                clockControl.notifyListeners(nextSimTime);

                nextSimTime = simEngine.processNextEvent();
            }

            // cleanup
            map.destroy();

            for (Model m : models) {

                m.finish();

                if (m instanceof PhysicalEntity) {
                    ((PhysicalEntity)m).destroy();
                }
            }

            for (VariationComponent comp : varComponents) {
                comp.shutdown();
            }

            motionRecorder.shutdown();

            double runTime = (double)(System.currentTimeMillis() - variationStartTime) / TimeUnit.SECONDS.toMillis(1);

            logger.info("");
            logger.info("--------------------------------------------");
            logger.info("Scenario variation " + currVariation + " executed in " + runTime + " seconds.");
            logger.info("--------------------------------------------");
        }
    }


    /**
     * Parses model definitions and returns the corresponding models. This is a
     * recursive call, allowing nested models to be instantiated. The entire model
     * tree is returned as a flattened list of models.
     *
     * @param config The model config to parse.
     * @param parent The parent model, or {@code null} if the model config is a root.
     * @param startPos The starting position in the world. May be {@code null} if it is not set by an ancestor.
     * @param models The list of configured models.
     * @param variation The current scenario variation.
     * @param injector The Guice injector to use as a parent injector.
     * @param nextModelId The next ID for a model.
     * @param nextMotionId The next ID for a motion-recorded object.
     */
    private void parseModelConfig(final ModelConfig config, Model parent, Vector3f startPos, List<Model> models,
                                  Variation variation, Injector injector,
                                  final AtomicInteger nextModelId, final AtomicInteger nextMotionId) {

        if (config == null) {
            return;
        }

        final Class modelClass;

        try {

            // locate the model implementation
            modelClass = Class.forName(config.getJavaClass());

            // make sure it implements Model
            if (!Model.class.isAssignableFrom(modelClass)) {
                throw new RuntimeException("The model implementation must extend from Model.");
            }
        }
        catch(ClassNotFoundException cnf) {
            throw new RuntimeException("Could not locate the model class: " +
                                       config.getJavaClass(), cnf);
        }

        // custom properties
        final Properties modelProps = loadConfigProps(config.getProperties(), variation);

        final Vector3f starting;

        if (startPos != null) {

            starting = startPos;

            Vector pos = config.getStartPosition();

            if ((pos != null) && 
                ((starting.x != pos.getX()) || (starting.y != pos.getY()) || (starting.z != pos.getZ()))) {

                logger.warn("A child model has specified a starting position different from its parent, using parent position.");
            }
        }
        else {

            starting = new Vector3f();

            if (config.getStartPosition() != null) {

                starting.x = config.getStartPosition().getX();
                starting.y = config.getStartPosition().getY();
                starting.z = config.getStartPosition().getZ();
            }
            else {

                starting.x = 0;
                starting.y = 0;
                starting.z = 0;
            }
        }


        // model injection config
        for (int i = 0; i < config.getCount(); i++) {

            Injector modelInjector = injector.createChildInjector(new AbstractModule() {

                protected void configure() {

                    bindConstant().annotatedWith(Names.named("model-id")).to(nextModelId.getAndIncrement());
                    bindConstant().annotatedWith(Names.named("object-id")).to(nextMotionId.getAndIncrement());
                    bindConstant().annotatedWith(Names.named("model-name")).to(config.getName());

                    bind(Vector3f.class).annotatedWith(Names.named("start-position")).toInstance(starting);

                    Names.bindProperties(binder(), modelProps);

                    // model class
                    bind(Model.class).to(modelClass);

                    // a workaround for guice issue 282
                    bind(modelClass);
                }
            });

            // instantiate the model
            final Model m = modelInjector.getInstance(Model.class);

            if (parent != null) {

                m.setParentModel(parent);
                parent.addChildModel(m);
            }

            models.add(m);

            // go through child models
            for (ModelConfig childConfig : config.getModel()) {
                parseModelConfig(childConfig, m, starting, models, variation, injector, nextModelId, nextMotionId);
            }

            // sensors
            for (final SensorConfig sensorConfig : config.getSensor()) {

                // sensors must be attached to physical entities because they rely on
                // their position and orientation information
                if (!(m instanceof PhysicalEntity)) {
                    throw new InvalidScenarioException("Sensor being attached to a model that is not a PhysicalEntity.");
                }

                final Class sensorClass;

                try {

                    sensorClass = Class.forName(sensorConfig.getJavaClass());

                    if (!Model.class.isAssignableFrom(sensorClass)) {
                        throw new RuntimeException("The sensor implementation must implement Model.");
                    }
                    else if (!AbstractSensor.class.isAssignableFrom(sensorClass)) {
                        throw new RuntimeException("The sensor implementation must extend from AbstractSensor.");
                    }
                }
                catch(ClassNotFoundException cnf) {
                    throw new RuntimeException("Could not locate the sensor class: " +
                                               config.getJavaClass(), cnf);
                }

                final Properties sensorProps = loadConfigProps(sensorConfig.getProperties(), variation);
                final Vector3f offset = new Vector3f();
                final Vector3f pointing = new Vector3f();

                if (sensorConfig.getOffset() != null) {

                    offset.set(sensorConfig.getOffset().getX(),
                               sensorConfig.getOffset().getY(),
                               sensorConfig.getOffset().getZ());
                }

                if (sensorConfig.getPointing() != null) {

                    pointing.set(sensorConfig.getPointing().getX(),
                                 sensorConfig.getPointing().getY(),
                                 sensorConfig.getPointing().getZ());
                }

                Injector sensorInjector = injector.createChildInjector(new AbstractModule() {

                    protected void configure() {

                        Names.bindProperties(binder(), sensorProps);

                        bindConstant().annotatedWith(Names.named("model-id")).to(nextModelId.getAndIncrement());
                        bindConstant().annotatedWith(Names.named("model-name")).to(sensorConfig.getName());

                        bind(Vector3f.class).annotatedWith(Names.named("offset")).toInstance(offset);
                        bind(Vector3f.class).annotatedWith(Names.named("pointing")).toInstance(pointing);

                        bind(Model.class).to(sensorClass);

                        // a workaround for guice issue 282
                        bind(sensorClass);
                    }
                });

                Model sensor = sensorInjector.getInstance(Model.class);

                models.add(sensor);

                sensor.setParentModel(m);
                m.addChildModel(sensor);
            }

            // radio
            if (config.getRadio() != null) {

                // radios must be attached to physical entities because they rely on
                // their position and orientation information
                if (!(m instanceof PhysicalEntity)) {
                    throw new InvalidScenarioException("Radio being attached to a model that is not a PhysicalEntity.");
                }

                final RadioConfig radioConfig = config.getRadio();
                final Class radioClass;
                final AntennaPattern pattern;

                final Properties radioProps = loadConfigProps(radioConfig.getProperties(), variation);

                try {

                    radioClass = Class.forName(radioConfig.getJavaClass());

                    if (!Model.class.isAssignableFrom(radioClass)) {
                        throw new RuntimeException("The radio implementation must implement Model.");                        
                    }
                    else if (!AbstractRadio.class.isAssignableFrom(radioClass)) {
                        throw new RuntimeException("The radio implementation must implement AbstractRadio.");
                    }
                }
                catch(ClassNotFoundException cnf) {
                    throw new RuntimeException("Could not locate the radio class: " +
                                               radioConfig.getJavaClass(), cnf);
                }

                if (config.getRadio().getAntennaPattern() != null) {

                    final Class patternClass;
                    final Properties patternProps = loadConfigProps(radioConfig.getAntennaPattern().getProperties(),
                                                                    variation);

                    try {

                        patternClass = Class.forName(radioConfig.getAntennaPattern().getJavaClass());

                        if (!AntennaPattern.class.isAssignableFrom(patternClass)) {
                            throw new RuntimeException("The antenna pattern implementation must implement AntennaPattern.");
                        }
                    }
                    catch(ClassNotFoundException cnf) {
                        throw new RuntimeException("Could not locate the antenna pattern class: " +
                                                   radioConfig.getAntennaPattern().getJavaClass(), cnf);
                    }

                    Injector patternInjector = injector.createChildInjector(new AbstractModule() {

                        protected void configure() {

                            Names.bindProperties(binder(), patternProps);

                            bind(AntennaPattern.class).to(patternClass);

                            // a workaround for guice issue 282
                            bind(patternClass);
                        }
                    });

                    pattern = patternInjector.getInstance(AntennaPattern.class);
                }
                else {
                    pattern = new IsotropicAntenna();
                }

                Injector radioInjector = injector.createChildInjector(new AbstractModule() {

                    protected void configure() {

                        Names.bindProperties(binder(), radioProps);

                        bindConstant().annotatedWith(Names.named("model-id")).to(nextModelId.getAndIncrement());
                        bindConstant().annotatedWith(Names.named("model-name")).to(radioConfig.getName());

                        bind(AntennaPattern.class).toInstance(pattern);
                        bind(Model.class).to(radioClass);

                        // a workaround for guice issue 282
                        bind(radioClass);
                    }
                });

                Model radio = radioInjector.getInstance(Model.class);

                models.add(radio);

                radio.setParentModel(m);
                m.addChildModel(radio);
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

            if (DocUtil.isPlaceholder(val)) {

                String var = DocUtil.extractPlaceholderName(val);

                if (variation.getVariables().containsKey(var)) {
                    val = variation.getVariables().get(var);
                }
                else {

                    logger.warn("The variable '" + prop.getName() + "' has not been set.");

                    String def = DocUtil.extractPlaceholderDefault(val);

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
        private SimEngine simEngine;

        private Map<CollisionObject, Set<CollisionObject>> contactMap = new HashMap<CollisionObject, Set<CollisionObject>>();


        public ContactHandler(CollisionWorld world, SimEngine engine) {

            this.world = world;
            this.simEngine = engine;
        }


        /**
         * Looks for contacts between objects at the given time.
         *
         * @param lastSimTime The last officially processed evnet time.
         * @param updatedTime The amount of time that the physics engine has moved forward since the last event was executed (nanoseconds).
         *
         * @return True if a collision event was scheduled, false otherwise.
         */
        public boolean update(SimTime lastSimTime, long updatedTime) {

            // remove old contacts
            for (CollisionObject obj : contactMap.keySet()) {
                ((EntityInfo)obj.getUserPointer()).getContactPoints().clear();
            }

            Map<CollisionObject, Set<CollisionObject>> newContacts = new HashMap<CollisionObject, Set<CollisionObject>>();

            boolean scheduledEvent = false;
            int numManifolds = world.getDispatcher().getNumManifolds();

            for (int i = 0; i < numManifolds; i++) {

                PersistentManifold manifold = world.getDispatcher().getManifoldByIndexInternal(i);
                CollisionObject objectA = (CollisionObject)manifold.getBody0();
                CollisionObject objectB = (CollisionObject)manifold.getBody1();
                EntityInfo infoA = (EntityInfo)objectA.getUserPointer();
                EntityInfo infoB = (EntityInfo)objectB.getUserPointer();

                int numPoints = manifold.getNumContacts();

                for (int j = 0; j < numPoints; j++) {

                    ManifoldPoint point = manifold.getContactPoint(j);

                    Contact contactA = new Contact(point.localPointA,
                                                   point.getPositionWorldOnA(new Vector3f()),
                                                   ((EntityInfo)objectB.getUserPointer()).getMetadata());

                    Contact contactB = new Contact(point.localPointB,
                                                   point.getPositionWorldOnB(new Vector3f()),
                                                   ((EntityInfo)objectA.getUserPointer()).getMetadata());

                    // add the contact points to the objects
                    infoA.getContactPoints().add(contactA);
                    infoB.getContactPoints().add(contactB);
                }

                // record that the two objects are touching
                if (numPoints > 0) {

                    if (!newContacts.containsKey(objectA)) {
                        newContacts.put(objectA, new HashSet<CollisionObject>());
                    }

                    if (!newContacts.containsKey(objectB)) {
                        newContacts.put(objectB, new HashSet<CollisionObject>());
                    }

                    newContacts.get(objectA).add(objectB);
                    newContacts.get(objectB).add(objectA);

                    // if it wasn't in the old map, it is a new contact (i.e. collision)
                    if (!contactMap.containsKey(objectA) ||
                        (contactMap.containsKey(objectA) && !contactMap.get(objectA).contains(objectB))) {

                        // schedule an event to signify the collision on the object(s) that
                        // are expecting such an event. it is better to do it this way (despite the
                        // hackiness) than broadcasting an event to all models and having them check
                        // if they are involved in the collision.

                        SimTime time = new SimTime(lastSimTime, updatedTime, TimeUnit.NANOSECONDS);
                        CollisionEvent event = new CollisionEvent();

                        for (int id : infoA.getCollisionListeners()) {

                            simEngine.scheduleEvent(id, time, event);
                            scheduledEvent = true;
                        }

                        for (int id : infoB.getCollisionListeners()) {

                            simEngine.scheduleEvent(id, time, event);
                            scheduledEvent = true;
                        }
                    }
                }
            }

            contactMap = newContacts;

            return scheduledEvent;
        }
    }


    /**
     * An implementation of {@link SimEngine} that is used as a container
     * and coordinator for events in each scenario variation.
     *
     * <br/>
     * This class is not thread safe, so it would need to be updated if
     * multiple models are allowed to execute in parallel in the future.
     */
    static final class SimEngineImpl implements SimEngine {

        private Queue<ScheduledEvent> eventQ = new PriorityQueue<ScheduledEvent>();
        private Map<Integer, Model> modelMap = new HashMap<Integer, Model>();
        private Map<String, List<Model>> modelNameMap = new HashMap<String, List<Model>>();
        private Map<Class, List> modelTypeMap = new HashMap<Class, List>();

        private SimTime processing = null;
        private SimTime lastProcessed = null;
        private long nextEventId = 1;

        private double realTimeScale = 1;
        private long firstEventRealTime = -1;


        public SimEngineImpl(double realTimeScale) {
            this.realTimeScale = realTimeScale;
        }


        /** {@inheritDoc} */
        public SimTime getCurrentTime() {
            return processing;
        }


        /** {@inheritDoc} */
        public long scheduleEvent(final int modelId, final SimTime time, final Event event) {

            SimTime minTime = processing;

            if ((minTime == null) && (lastProcessed != null)) {
                minTime = lastProcessed;
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

            long eventId = nextEventId++;

            // add it to the queue
            eventQ.add(new ScheduledEvent(eventId, time, event, model));

            return eventId;
        }


        /** {@inheritDoc} */
        public void cancelEvent(long eventId) {

            // todo: do this better
            ScheduledEvent toRemove = null;

            for (ScheduledEvent e : eventQ) {

                if (e.getId() == eventId) {

                    toRemove = e;
                    break;
                }
            }

            if (toRemove != null) {
                eventQ.remove(toRemove);
            }
        }


        /** {@inheritDoc} */
        public Model findModelById(int ID) {
            return modelMap.get(ID);
        }


        /** {@inheritDoc} */
        public Model findModelByName(String name) {

            List<Model> models = findModelsByName(name);

            if (models.isEmpty()) {
                return null;
            }
            else if (models.size() > 1) {
                throw new RuntimeException("There is more than one model with the name: '" + name + "'.");
            }

            return models.get(0);
        }


        /** {@inheritDoc} */
        public <T> T findModelByType(Class<T> type) {

            List<T> models = findModelsByType(type);

            if (models.isEmpty()) {
                return null;
            }
            else if (models.size() > 1) {
                throw new RuntimeException("There is more than one model with the type: '" + type.toString() + "'.");
            }

            return type.cast(models.get(0));
        }


        /** {@inheritDoc} */
        public List<Model> findModelsByName(String name) {
            return modelNameMap.get(name);
        }


        /** {@inheritDoc} */
        public <T> List<T> findModelsByType(Class<T> type) {

            if (!modelTypeMap.containsKey(type)) {

                List<T> results = new LinkedList<T>();

                // search all models and cache the results
                for (Model m : modelMap.values()) {

                    if (type.isAssignableFrom(m.getClass())) {
                        results.add(type.cast(m));
                    }
                }

                modelTypeMap.put(type, results);

                return results;
            }

            return modelTypeMap.get(type);
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

                if (firstEventRealTime < 0) {
                    firstEventRealTime = System.currentTimeMillis();
                }

                // if we are scaling to real time, hold off until we are ready to run the event.
                // this implementation provide millisecond precision
                long diff = TimeUnit.NANOSECONDS.toMillis((long)(next.time.getTime() * realTimeScale)) - 
                            (System.currentTimeMillis() - firstEventRealTime);

                if (diff > 0) {

                    try {
                        Thread.sleep(diff);
                    }
                    catch(InterruptedException ie) {
                        throw new RuntimeException("SimEngine was interrupted while sleeping.");
                    }
                }

                processing = next.time;

                next.model.processEvent(next.time, next.event);
            }

            lastProcessed = processing;
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

            if (modelMap.containsKey(model.getModelId())) {
                throw new RuntimeException("A model with the ID " + model.getModelId() + " is already registered.");
            }

            modelMap.put(model.getModelId(), model);

            if (!modelNameMap.containsKey(model.getName())) {
                modelNameMap.put(model.getName(), new LinkedList<Model>());
            }

            modelNameMap.get(model.getName()).add(model);
        }


        /**
         * A container that holds the details of an event to be processed in the future.
         */
        private static class ScheduledEvent implements Comparable<ScheduledEvent> {

            public long id;
            public SimTime time;
            public Event event;
            public Model model;


            public ScheduledEvent(long id, SimTime time, Event event, Model model) {

                this.id = id;
                this.time = time;
                this.event = event;
                this.model = model;
            }


            /**
             * We need an absolute (deterministic) sorting of scheduled events, so we use the
             * time, model ID, and event ID as tiebreakers.
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

                    int modelComp = Integer.valueOf(model.getModelId()).compareTo(o.model.getModelId());

                    if (modelComp == 0) {
                        return Long.valueOf(id).compareTo(o.id);
                    }

                    return modelComp;
                }

                return timeComp;
            }


            public long getId() {
                return id;
            }
        }
    }
}
