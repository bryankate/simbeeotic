package harvard.robobees.simbeeotic.model;


import com.google.inject.Inject;
import com.google.inject.name.Named;


/**
 * A base convenience class for models. Currently it only acts as a container for
 * commonly used fields, but could be extended in the future with added functionality.
 *
 * @author bkate
 */
public abstract class AbstractModel implements Model {

    private int modelId;


    /** {@inheritDoc} */
    @Override
    public final int getModelId() {
        return modelId;
    }


    // this is not really optional. we just made it optional here to avoid having it double-injected
    // when an instance of this class is used in a Guice child injector module.
    @Inject(optional = true)
    public final void setModelId(@Named("model-id") final int id) {
        this.modelId = id;
    }
}
