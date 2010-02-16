package harvard.robobees.simbeeotic.model;


import com.google.inject.Inject;
import com.google.inject.name.Named;


/**
 * A base convenience class for models. Currently it only acts as a container for
 * commonly used fields, but could be extended in the future with added functionality.
 *
 * @author bkate
 */
public abstract class AbstractModel extends AbstractPhysicalEntity implements Model {

    private int modelId;


    /** {@inheritDoc} */
    @Override
    public final int getModelId() {
        return modelId;
    }


    @Inject
    public final void setModelId(@Named("model-id") final int id) {

        if (!isInitialized()) {
            this.modelId = id;
        }
    }
}
