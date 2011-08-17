package harvard.robobees.simbeeotic.model;


import com.google.inject.Inject;
import com.google.inject.name.Named;


/**
 * A model that holds the physical bounds of the helicopters in the simulation. Each
 * helicopter can lookup this model to get the bounds.
 *
 * @author bkate
 */
public class HeliBounds extends AbstractModel {

    // state
    private Boundary bounds;

    // params
    private float xMin = Boundary.DEFAULT_X_MIN;
    private float xMax = Boundary.DEFAULT_X_MAX;
    private float yMin = Boundary.DEFAULT_Y_MIN;
    private float yMax = Boundary.DEFAULT_Y_MAX;
    private float zMax = Boundary.DEFAULT_Z_MAX;


    @Override
    public void finish() {
    }


    public Boundary getBounds() {

        if (bounds == null) {
            bounds = new Boundary(xMin, xMax, yMin, yMax, zMax);
        }

        return bounds;
    }


    @Inject(optional = true)
    public final void setXMin(@Named("min-x-bound") final float bound) {
        this.xMin = bound;
    }


    @Inject(optional = true)
    public final void setXMax(@Named("max-x-bound") final float bound) {
        this.xMax = bound;
    }


    @Inject(optional = true)
    public final void setYMin(@Named("min-y-bound") final float bound) {
        this.yMin = bound;
    }


    @Inject(optional = true)
    public final void setYMax(@Named("max-y-bound") final float bound) {
        this.yMax = bound;
    }


    @Inject(optional = true)
    public final void setZMax(@Named("max-z-bound") final float bound) {
        this.zMax = bound;
    }
}
