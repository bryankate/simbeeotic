package harvard.robobees.simbeeotic.model.weather;


import harvard.robobees.simbeeotic.model.AbstractModel;
import harvard.robobees.simbeeotic.model.Model;
import harvard.robobees.simbeeotic.model.PhysicalEntity;
import harvard.robobees.simbeeotic.model.ExternalForceEvent;
import harvard.robobees.simbeeotic.SimTime;

import javax.vecmath.Vector3f;

import com.google.inject.Inject;
import com.google.inject.name.Named;


/**
 * A model that exerts a constant external wind force on all
 * {@link PhysicalEntity} models in the simulation.
 *
 * @author bkate
 */
public class ConstantWindModel extends AbstractModel {

    private Vector3f force = new Vector3f();


    @Override
    public void initialize() {

        super.initialize();

        // schedule an event on each model indicating the constant wind
        SimTime time = new SimTime(0);
        ExternalForceEvent windEvent = new ExternalForceEvent("wind", force);

        for (Model m : getSimEngine().findModelsByType(PhysicalEntity.class)) {
            getSimEngine().scheduleEvent(m.getModelId(), time, windEvent);
        }
    }


    public void finish() {
    }


    @Inject(optional = true)
    public final void setForceX(@Named("force-x") final float x) {
        force.x = x;
    }


    @Inject(optional = true)
    public final void setForceY(@Named("force-y") final float y) {
        force.y = y;
    }


    @Inject(optional = true)
    public final void setForceZ(@Named("force-z") final float z) {
        force.z = z;
    }
}
