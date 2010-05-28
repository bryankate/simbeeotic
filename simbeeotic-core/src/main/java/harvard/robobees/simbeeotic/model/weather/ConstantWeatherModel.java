package harvard.robobees.simbeeotic.model.weather;


import harvard.robobees.simbeeotic.model.AbstractModel;
import harvard.robobees.simbeeotic.SimTime;

import javax.vecmath.Vector3f;

import com.google.inject.Inject;
import com.google.inject.name.Named;


/**
 * A model that represents weather conditions with constant values.
 *
 * @author bkate
 */
public class ConstantWeatherModel extends AbstractModel implements WeatherModel {

    private Vector3f windVel = new Vector3f();   // m/s


    /** {@inheritDoc} */
    public Vector3f getWindVelocity(SimTime time, Vector3f position) {
        return windVel;
    }


    public void finish() {
    }


    @Inject(optional = true)
    public final void setWindX(@Named("wind-x") final float x) {
        windVel.x = x;
    }


    @Inject(optional = true)
    public final void setWindY(@Named("wind-y") final float y) {
        windVel.y = y;
    }


    @Inject(optional = true)
    public final void setWindZ(@Named("wind-z") final float z) {
        windVel.z = z;
    }
}
