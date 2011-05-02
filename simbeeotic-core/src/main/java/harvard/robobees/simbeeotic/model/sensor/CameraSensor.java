package harvard.robobees.simbeeotic.model.sensor;

import harvard.robobees.simbeeotic.model.sensor.camera.CameraView;

import javax.media.j3d.Transform3D;
import javax.vecmath.Vector3f;

/**
 * Created by IntelliJ IDEA.
 * User: Joseph
 * Date: 4/14/11
 * Time: 8:06 PM
 * To change this template use File | Settings | File Templates.
 */
public interface CameraSensor {

    public CameraView getView();
    public int getCameraID();
    public void initialize();

}
