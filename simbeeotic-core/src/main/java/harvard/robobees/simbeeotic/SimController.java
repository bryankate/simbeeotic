package harvard.robobees.simbeeotic;


import com.bulletphysics.collision.broadphase.AxisSweep3;
import com.bulletphysics.collision.broadphase.BroadphaseProxy;
import com.bulletphysics.collision.broadphase.OverlapFilterCallback;
import com.bulletphysics.collision.dispatch.CollisionConfiguration;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.dispatch.CollisionWorld;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.collision.narrowphase.ManifoldPoint;
import com.bulletphysics.collision.narrowphase.PersistentManifold;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import com.bulletphysics.linearmath.Transform;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import harvard.robobees.simbeeotic.comms.AbstractRadio;
import harvard.robobees.simbeeotic.comms.AntennaPattern;
import harvard.robobees.simbeeotic.comms.DefaultPropagationModel;
import harvard.robobees.simbeeotic.comms.IsotropicAntenna;
import harvard.robobees.simbeeotic.comms.PropagationModel;
import harvard.robobees.simbeeotic.configuration.ConfigurationAnnotations.GlobalScope;
import harvard.robobees.simbeeotic.configuration.Variation;
import harvard.robobees.simbeeotic.configuration.VariationIterator;
import harvard.robobees.simbeeotic.configuration.scenario.Colony;
import harvard.robobees.simbeeotic.configuration.scenario.ConfigProps;
import harvard.robobees.simbeeotic.configuration.scenario.GenericModelConfig;
import harvard.robobees.simbeeotic.configuration.scenario.Scenario;
import harvard.robobees.simbeeotic.configuration.scenario.Sensor;
import harvard.robobees.simbeeotic.configuration.scenario.MiscModels;
import harvard.robobees.simbeeotic.configuration.world.World;
import static harvard.robobees.simbeeotic.environment.PhysicalConstants.EARTH_GRAVITY;
import harvard.robobees.simbeeotic.environment.WorldMap;
import harvard.robobees.simbeeotic.model.Contact;
import harvard.robobees.simbeeotic.model.EntityInfo;
import harvard.robobees.simbeeotic.model.GenericBee;
import harvard.robobees.simbeeotic.model.GenericBeeLogic;
import harvard.robobees.simbeeotic.model.GenericHive;
import harvard.robobees.simbeeotic.model.GenericHiveLogic;
import harvard.robobees.simbeeotic.model.GenericModel;
import harvard.robobees.simbeeotic.model.PhysicalEntity;
import harvard.robobees.simbeeotic.model.PhysicalModel;
import harvard.robobees.simbeeotic.model.Model;
import harvard.robobees.simbeeotic.model.sensor.AbstractSensor;
import harvard.robobees.simbeeotic.util.BoundingSphere;
import harvard.robobees.simbeeotic.util.DocUtil;
import org.apache.log4j.Logger;

import javax.vecmath.Vector3f;
import java.util.HashSet;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * The entry point to the simulation. The controller takes descriptions of the scenario and world
 * and executes the variations.
 *
 * @author bkate
 */
public class SimController {

    private static final double DEFAULT_SUBSTEP = 1.0f / 240.0f;
    private static final int DEFAULT_MAX_SUBSTEPS = 100;

    private static Logger logger = Logger.getLogger(SimController.class);

    
    /**
     * Runs all simulation variations of a given scenario.
     *
     * @param scenario The scenario, describing the models to execute.
     * @param world The world in which the models operate.
     */
    public void runSim(final Scenario scenario, final World world) {

        final double step = scenario.getSimulation().getStep();

        int currVariation = 0;
        VariationIterator variations = new VariationIterator(scenario);

        // todo: parallelize the running of variations
        for (Variation variation : variations) {

            final int varId = ++currVariation;

            logger.info("");
            logger.info("--------------------------------------------");
            logger.info("Executing scenario variation " + currVariation);
            logger.info("--------------------------------------------");
            logger.info("");

            final AtomicInteger nextId = new AtomicInteger(0);
            final Random variationSeedGenertor = new Random(variation.getSeed());

            // make a new clock
            final SimClockImpl clock = new SimClockImpl(step);

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
            Module baseModule = new AbstractModule() {

                protected void configure() {

                    // the variation number of this scenario variation
                    bindConstant().annotatedWith(Names.named("variation-number")).to(varId);

                    // the global access to timing information
                    bind(SimClock.class).annotatedWith(GlobalScope.class).toInstance(clock);

                    // dynamics world
                    bind(DiscreteDynamicsWorld.class).annotatedWith(GlobalScope.class).toInstance(dynamicsWorld);

                    // established simulated world instance
                    bind(WorldMap.class).annotatedWith(GlobalScope.class).toInstance(map);
                }

                // todo: figure out how to get these providers to not be called for each child injector?

                @Provides @Named("random-seed")
                public long generateRandomSeed() {
                    return variationSeedGenertor.nextLong();
                }
            };

            Injector baseInjector = Guice.createInjector(baseModule);


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
                    throw new RuntimeException("Could not locate the propagation model class: " +
                                               scenario.getComms().getPropagationModel().getJavaClass(), cnf);
                }
            }
            else {

                commModelProps = new Properties();
                commModelClass = DefaultPropagationModel.class;
            }

            Injector propModelInjector = baseInjector.createChildInjector(new AbstractModule() {

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
            baseInjector = baseInjector.createChildInjector(new AbstractModule() {

                protected void configure() {

                    // established simulated world instance
                    bind(PropagationModel.class).annotatedWith(GlobalScope.class).toInstance(commModel);
                }
            });

            
            // todo: setup weather


            // create hive
            final PhysicalModel hive;

            // todo: get z loc from world map (terrain)?
            final float hiveStartX = scenario.getColony().getHive().getPosition().getX();
            final float hiveStartY = scenario.getColony().getHive().getPosition().getY();

            final Class hiveClass;
            final Properties hiveProps = loadConfigProps(scenario.getColony().getHive().getProperties(), variation);

            if (scenario.getColony().getHive().getCustomHive() != null) {

                try {

                    // locate the hive implementation
                    hiveClass = Class.forName(scenario.getColony().getHive().getCustomHive().getJavaClass());

                    // make sure it implements PhysicalModel
                    if (!PhysicalModel.class.isAssignableFrom(hiveClass)) {
                        throw new RuntimeException("The custom hive implementation must extend from PhysicalModel.");
                    }
                }
                catch(ClassNotFoundException cnf) {
                    throw new RuntimeException("Could not locate the hive class: " +
                                               scenario.getColony().getHive().getCustomHive().getJavaClass(), cnf);
                }
            }
            else {
                hiveClass = GenericHive.class;
            }

            Injector hiveInjector = baseInjector.createChildInjector(new AbstractModule() {

                protected void configure() {

                    Names.bindProperties(binder(), hiveProps);

                    bindConstant().annotatedWith(Names.named("start-x")).to(hiveStartX + "");
                    bindConstant().annotatedWith(Names.named("start-y")).to(hiveStartY + "");

                    // hive class
                    bind(PhysicalModel.class).to(hiveClass);

                    // a workaround for guice issue 282
                    bind(hiveClass);
                }

                @Provides @Named("model-id")
                public int generateModelId() {
                    return nextId.incrementAndGet();
                }
            });

            hive = hiveInjector.getInstance(PhysicalModel.class);

            // if a generic hive, instantiate components
            if (scenario.getColony().getHive().getGenericHive() != null) {
                loadGenericModelComponents(baseInjector, scenario.getColony().getHive().getGenericHive(), variation, hive, false);
            }

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
                final Properties beeProps = loadConfigProps(group.getProperties(), variation);

                final float beeStartX;
                final float beeStartY;

                // default to starting in the hive unless otherwise given
                if (group.getStartPosition() != null) {

                    beeStartX = group.getStartPosition().getX();
                    beeStartY = group.getStartPosition().getY();
                }
                else {

                    beeStartX = hiveStartX;
                    beeStartY = hiveStartY;
                }

                if (group.getCustomBee() != null) {

                    try {

                        // locate the bee implementation
                        beeClass = Class.forName(group.getCustomBee().getJavaClass());

                        // make sure it implements PhysicalModel
                        if (!PhysicalModel.class.isAssignableFrom(beeClass)) {
                            throw new RuntimeException("The bee implementation must extend from PhysicalModel.");
                        }
                    }
                    catch(ClassNotFoundException cnf) {
                        throw new RuntimeException("Could not locate the bee class: " +
                                                   group.getCustomBee().getJavaClass(), cnf);
                    }
                }
                else {
                    beeClass = GenericBee.class;
                }

                Injector beeInjector = baseInjector.createChildInjector(new AbstractModule() {

                    protected void configure() {

                        Names.bindProperties(binder(), beeProps);

                        bindConstant().annotatedWith(Names.named("start-x")).to(beeStartX + "");
                        bindConstant().annotatedWith(Names.named("start-y")).to(beeStartY + "");

                        // bee class
                        bind(PhysicalModel.class).to(beeClass);

                        // a workaround for guice issue 282
                        bind(beeClass);
                    }

                    @Provides @Named("model-id")
                    public int generateModelId() {
                        return nextId.incrementAndGet();
                    }
                });

                for (int i = 0; i < group.getCount(); i++) {

                    PhysicalModel b = beeInjector.getInstance(PhysicalModel.class);

                    if (group.getGenericBee() != null) {
                        loadGenericModelComponents(baseInjector, group.getGenericBee(), variation, b, true);
                    }

                    b.initialize();
                    bees.add(b);
                }
            }


            // create misc models, if any exist
            final Set<Model> miscModels = new HashSet<Model>();

            if (scenario.getMiscModels() != null) {
                
                for (MiscModels.Model model : scenario.getMiscModels().getModel()) {

                    final Class modelClass;
                    final Properties modelProps = loadConfigProps(model.getProperties(), variation);

                    try {

                        // locate the bee implementation
                        modelClass = Class.forName(model.getJavaClass());

                        // make sure it implements PhysicalModel
                        if (!Model.class.isAssignableFrom(modelClass)) {
                            throw new RuntimeException("The model implementation must extend from Model.");
                        }
                    }
                    catch(ClassNotFoundException cnf) {
                        throw new RuntimeException("Could not locate the model class: " +
                                                   model.getJavaClass(), cnf);
                    }

                    Injector modelInjector = baseInjector.createChildInjector(new AbstractModule() {

                        protected void configure() {

                            Names.bindProperties(binder(), modelProps);

                            // model class
                            bind(PhysicalModel.class).to(modelClass);

                            // a workaround for guice issue 282
                            bind(modelClass);
                        }

                        @Provides @Named("model-id")
                        public int generateModelId() {
                            return nextId.incrementAndGet();
                        }
                    });

                    Model m = modelInjector.getInstance(Model.class);

                    miscModels.add(m);
                    m.initialize();
                }
            }


            // setup a handler for dealing with contacts and informing objects
            // of when they collide
            ContactHandler contactHandler = new ContactHandler(dynamicsWorld);


            long variationStartTime = System.currentTimeMillis();

            // run it
            double endTime = scenario.getSimulation().getEndTime();

            // todo: turn this into a DES
            while(clock.getCurrentTime() < endTime) {

                // update positions in physical world
                dynamicsWorld.stepSimulation((float)step, DEFAULT_MAX_SUBSTEPS, (float)DEFAULT_SUBSTEP);

                clock.incrementTime();

                // todo: put this inside an internal tick callback
                // update collisions
                contactHandler.update();

                hive.sampleKinematics(step);
                hive.update(clock.getCurrentTime());

                for (PhysicalModel b : bees) {

                    // todo: put this inside an internal tick callback
                    // sample kinematic state to produce a truth
                    // measurement of accelerations
                    b.sampleKinematics(step);

                    // update model behaviors
                    b.update(clock.getCurrentTime());
                }

                for (Model m : miscModels) {
                    m.update(clock.getCurrentTime());
                }
            }

            double runTime = (double)(System.currentTimeMillis() - variationStartTime) / 1000.0;

            // cleanup
            map.destroy();

            hive.finish();
            hive.destroy();

            for (PhysicalModel b : bees) {

                b.finish();
                b.destroy();
            }

            for (Model m : miscModels) {
                m.finish();
            }

            logger.info("");
            logger.info("--------------------------------------------");
            logger.info("Scenario variation " + currVariation + " executed in " + runTime + " seconds.");
            logger.info("--------------------------------------------");
        }
    }


    private void loadGenericModelComponents(Injector injector, GenericModelConfig model, Variation variation,
                                            final PhysicalModel host, final boolean isBee) {

        GenericModel genModel = (GenericModel)host;

        // radio
        if (model.getRadio() != null) {

            final AntennaPattern pattern;

            if (model.getRadio().getAntennaPattern() != null) {

                final Class patternClass;
                final Properties patternProps = loadConfigProps(model.getRadio().getAntennaPattern().getProperties(),
                                                                variation);

                try {

                    patternClass = Class.forName(model.getRadio().getAntennaPattern().getJavaClass());

                    if (!AntennaPattern.class.isAssignableFrom(patternClass)) {
                        throw new RuntimeException("The antenna pattern implementation must implement AntennaPattern.");
                    }
                }
                catch(ClassNotFoundException cnf) {
                    throw new RuntimeException("Could not locate the antenna pattern class: " +
                                               model.getRadio().getAntennaPattern().getJavaClass(), cnf);
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

            final Class radioClass;
            final Properties radioProps = loadConfigProps(model.getRadio().getProperties(),
                                                          variation);

            try {

                radioClass = Class.forName(model.getRadio().getJavaClass());

                if (!AbstractRadio.class.isAssignableFrom(radioClass)) {
                    throw new RuntimeException("The radio implementation must implement AbstractRadio.");
                }
            }
            catch(ClassNotFoundException cnf) {
                throw new RuntimeException("Could not locate the radio class: " +
                                           model.getRadio().getJavaClass(), cnf);
            }

            Injector radioInjector = injector.createChildInjector(new AbstractModule() {

                protected void configure() {

                    Names.bindProperties(binder(), radioProps);

                    bind(AntennaPattern.class).toInstance(pattern);

                    bind(PhysicalModel.class).toInstance(host);
                    bind(AbstractRadio.class).to(radioClass);

                    // a workaround for guice issue 282
                    bind(radioClass);
                }
            });

            genModel.setRadio(radioInjector.getInstance(AbstractRadio.class));
        }


        // sensors
        if (model.getSensors() != null) {

            for (Sensor sensor : model.getSensors().getSensor()) {

                final Class sensorClass;
                final Properties sensorProps = loadConfigProps(sensor.getProperties(), variation);

                try {

                    sensorClass = Class.forName(sensor.getJavaClass());

                    if (!AbstractSensor.class.isAssignableFrom(sensorClass)) {
                        throw new RuntimeException("The sensor implementation must implement AbstractSensor.");
                    }
                }
                catch(ClassNotFoundException cnf) {
                    throw new RuntimeException("Could not locate the sensor class: " +
                                               sensor.getJavaClass(), cnf);
                }

                final Vector3f offset = new Vector3f();
                final Vector3f pointing = new Vector3f();

                if (sensor.getOffset() != null) {

                    offset.set(sensor.getOffset().getX(),
                               sensor.getOffset().getY(),
                               sensor.getOffset().getZ());
                }

                if (sensor.getPointing() != null) {

                    pointing.set(sensor.getPointing().getX(),
                                 sensor.getPointing().getY(),
                                 sensor.getPointing().getZ());
                }

                Injector sensorInjector = injector.createChildInjector(new AbstractModule() {

                    protected void configure() {

                        Names.bindProperties(binder(), sensorProps);

                        bind(PhysicalModel.class).toInstance(host);
                        bind(Vector3f.class).annotatedWith(Names.named("offset")).toInstance(offset);
                        bind(Vector3f.class).annotatedWith(Names.named("pointing")).toInstance(offset);

                        bind(AbstractSensor.class).to(sensorClass);

                        // a workaround for guice issue 282
                        bind(sensorClass);
                    }
                });

                genModel.addSensor(sensor.getName(), sensorInjector.getInstance(AbstractSensor.class));
            }
        }

        // logic
        final Class logicClass;
        final Properties logicProps = loadConfigProps(model.getLogic().getProperties(), variation);

        try {

            logicClass = Class.forName(model.getLogic().getJavaClass());

            if (isBee) {

                if (!GenericBeeLogic.class.isAssignableFrom(logicClass)) {
                    throw new RuntimeException("The logic implementation must implement GenericBeeLogic.");
                }
            }
            else {

                if (!GenericHiveLogic.class.isAssignableFrom(logicClass)) {
                    throw new RuntimeException("The logic implementation must implement GenericHiveLogic.");
                }
            }
        }
        catch(ClassNotFoundException cnf) {
            throw new RuntimeException("Could not locate the logic class: " +
                                       model.getLogic().getJavaClass(), cnf);
        }

        Injector logicInjector = injector.createChildInjector(new AbstractModule() {

            protected void configure() {

                Names.bindProperties(binder(), logicProps);

                bind(PhysicalModel.class).toInstance(host);

                if (isBee) {
                    bind(GenericBeeLogic.class).to(logicClass);
                }
                else {
                    bind(GenericHiveLogic.class).to(logicClass);
                }

                // a workaround for guice issue 282
                bind(logicClass);
            }
        });

        if (isBee) {
            ((GenericBee)genModel).setLogic(logicInjector.getInstance(GenericBeeLogic.class));
        }
        else {
            ((GenericHive)genModel).setLogic(logicInjector.getInstance(GenericHiveLogic.class));
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
     * An implementation of {@link SimClock} that is used as the main time
     * reference for each scenario variation.
     */
    static final class SimClockImpl implements SimClock {

        private long time = 0;
        private long step;

        private static final long PRECISION = 1000000;


        public SimClockImpl(double step) {
            this.step = (long)(step * PRECISION);
        }

        @Override
        public double getCurrentTime() {
            return (double)time / PRECISION;
        }

        @Override
        public double getTimeStep() {
            return (double)step / PRECISION;
        }

        public void incrementTime() {
            time += step;
        }
    }
}
