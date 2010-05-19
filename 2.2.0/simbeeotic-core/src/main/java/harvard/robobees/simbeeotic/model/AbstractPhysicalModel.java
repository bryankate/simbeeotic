package harvard.robobees.simbeeotic.model;


import com.google.inject.Inject;
import com.google.inject.name.Named;


/**
 * A convenience class that bridges the functionality of {@link AbstractPhysicalEntity} with
 * base functionality needed by all {@link Model} implementations.
 *
 * @author bkate
 */
public abstract class AbstractPhysicalModel extends AbstractPhysicalEntity implements PhysicalModel {

    private int modelId;


    /**
     * Initializes any model behavior prior to starting the simulation time steps.
     */
    protected abstract void initializeBehavior();


    @Override
    public void initialize() {

        // physical initialization
        super.initialize();

        // model initialization
        initializeBehavior();
    }


    /** {@inheritDoc} */
    @Override
    public final int getModelId() {
        return modelId;
    }


    // this is not really optional. we just made it optional here to avoid having it double-injected
    // when an instance of this class is used in a Guice child injector module.
    @Inject(optional = true)
    public final void setModelId(@Named("model-id") final int id) {

        if (!isInitialized()) {
            this.modelId = id;
        }
    }
}
