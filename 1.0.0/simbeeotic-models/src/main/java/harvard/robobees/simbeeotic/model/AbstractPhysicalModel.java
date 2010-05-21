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
