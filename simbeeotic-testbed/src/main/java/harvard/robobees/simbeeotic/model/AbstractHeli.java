package harvard.robobees.simbeeotic.model;


import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.CompoundShape;
import com.bulletphysics.collision.shapes.CylinderShapeZ;
import com.bulletphysics.linearmath.Transform;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import harvard.robobees.simbeeotic.SimTime;
import harvard.robobees.simbeeotic.configuration.heli.BehaviorConfig;
import harvard.robobees.simbeeotic.configuration.heli.Behaviors;
import harvard.robobees.simbeeotic.configuration.heli.ConfigProps;
import harvard.robobees.simbeeotic.util.DocUtil;
import harvard.robobees.simbeeotic.util.JaxbHelper;
import org.w3c.dom.Document;

import javax.vecmath.Vector3f;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;


/**
 * A base class for helicopter models that parses behaviors and provides some
 * convenience mechanisms for heli implementations.
 *
 * @author bkate
 */
public abstract class AbstractHeli extends GenericModel implements HeliControl {

    private Map<String, HeliBehavior> behaviors = new HashMap<String, HeliBehavior>();

    // params
    private boolean startBehaviors = true;


    protected static final CollisionShape HELI_SHAPE;

    // make a helicopter shaped obejct for use in the simulation. by sharing the
    // smae collision shape the sim may run a tiny bit faster (plus it is
    // convenient to expose to other models so we do not cut and paste).
    static {

        CompoundShape cs = new CompoundShape();
        Transform trans = new Transform();

        // main body
        trans.origin.set(new Vector3f(0.0175f, 0, 0));

        cs.addChildShape(trans, new BoxShape(new Vector3f(0.045f, 0.02f, 0.025f)));

        // tail
        trans = new Transform();
        trans.origin.set(new Vector3f(-0.055f, 0, 0));

        cs.addChildShape(trans, new BoxShape(new Vector3f(0.055f, 0.0015f, 0.0015f)));

        trans = new Transform();
        trans.origin.set(new Vector3f(-0.11f, 0, 0.01f));

        cs.addChildShape(trans, new BoxShape(new Vector3f(0.01f, 0.0015f, 0.03f)));

        // rotors
        trans = new Transform();
        trans.origin.set(0, 0, 0.025f);

        cs.addChildShape(trans, new CylinderShapeZ(new Vector3f(0.002f, 0.002f, 0.05f)));

        trans = new Transform();
        trans.origin.set(0, 0, 0.03f);

        cs.addChildShape(trans, new CylinderShapeZ(new Vector3f(0.0925f, 0.0925f, 0.0001f)));

        trans = new Transform();
        trans.origin.set(0, 0, 0.045f);

        cs.addChildShape(trans, new CylinderShapeZ(new Vector3f(0.0925f, 0.0925f, 0.0001f)));

        HELI_SHAPE = cs;
    }


    @Override
    public void initialize() {

        super.initialize();

        // parse behaviors
        if (getCustomConfig() != null) {

            try {

                Document customConfig = DocUtil.setDocumentNamespace(getCustomConfig(),
                                                                     "http://harvard/robobees/simbeeotic/configuration/heli");

                Behaviors allBehaviors = JaxbHelper.objectFromNode(customConfig, Behaviors.class);

                for (BehaviorConfig config : allBehaviors.getBehavior()) {

                    final Class bClass = Class.forName(config.getJavaClass());
                    final Properties props = new Properties();

                    if (config.getProperties() != null) {

                        for (ConfigProps.Prop prop : config.getProperties().getProp()) {
                            props.setProperty(prop.getName(), prop.getValue());
                        }
                    }

                    Injector injector = Guice.createInjector(new AbstractModule() {

                        @Override
                        protected void configure() {

                            bind(HeliBehavior.class).to(bClass);
                            Names.bindProperties(binder(), props);
                        }
                    });

                    behaviors.put(config.getName(), injector.getInstance(HeliBehavior.class));
                }
            }
            catch(Exception e) {
                throw new RuntimeModelingException("Could not parse heli behaviors.", e);
            }
        }

        // start behaviors in an event
        if (startBehaviors) {
            
            createTimer(new TimerCallback() {

                @Override
                public void fire(SimTime time) {

                    for (HeliBehavior b : behaviors.values()) {
                        b.start(AbstractHeli.this, AbstractHeli.this);
                    }
                }
            }, 0, TimeUnit.SECONDS);
        }
    }


    @Override
    public void finish() {

        // stop all behaviors
        for (HeliBehavior b : behaviors.values()) {
            b.stop();
        }
    }


    protected final Map<String, HeliBehavior> getBehaviors() {
        return Collections.unmodifiableMap(behaviors);
    }


    @Inject(optional = true)
    public final void setStartBehaviors(@Named("start-behaviors") final boolean start) {
        startBehaviors = start;
    }
}
