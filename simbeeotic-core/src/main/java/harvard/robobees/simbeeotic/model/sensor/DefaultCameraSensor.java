package harvard.robobees.simbeeotic.model.sensor;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import harvard.robobees.simbeeotic.configuration.ConfigurationAnnotations;
import harvard.robobees.simbeeotic.model.MotionRecorder;
import harvard.robobees.simbeeotic.model.sensor.camera.CameraView;
import harvard.robobees.simbeeotic.model.sensor.camera.CannyEdgeDetector;

import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix3d;
import javax.vecmath.Matrix3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

/**
 * Created by IntelliJ IDEA.
 * User: Joseph
 * Date: 4/14/11
 * Time: 9:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultCameraSensor extends AbstractSensor implements CameraSensor  {
    private CameraView view;
    private int height;
    private int width;
    private float focalLength;
    Matrix3d rotation;
    Vector3d offset;
    Transform3D trans;

    //Return the CameraView's View object

    public CameraView getView(){
        motionRecorder.updateView(getCameraID());
        return view;
    }

    //Return the Camera's ID (which is its host's ID)
    public int getCameraID() {
        return getHost().getObjectId();

    }
    public void initialize() {
        //Create Transform for Camera
        trans = new Transform3D();
        trans.setTranslation(offset);
        trans.setRotation(rotation);

        //Set properties
        view = new CameraView(width,height);

        //Have motionRecorder add camera to 3DWorld
        motionRecorder.addView(getCameraID(), view.getBuf(), trans, height, width, focalLength);

        super.initialize();

    };

    @Inject
    @ConfigurationAnnotations.GlobalScope
    private MotionRecorder motionRecorder;

    @Inject(optional = true)
    public final void setWidth(@Named("width") final int w) {

        if (!isInitialized()) {
            this.width = w;
        }
    }

    @Inject(optional = true)
    public final void setHeight(@Named("height") final int h) {

        if (!isInitialized()) {
            this.height = h;
        }
    }
    @Inject(optional = true)
    public final void setlength(@Named("length") final int l) {

        if (!isInitialized()) {
            this.focalLength = l;
        }
    }
    //Uses ZXY transform
    @Inject(optional = true)
    public final void setRotation(@Named("rotx") final double x, @Named("roty") final double y, @Named("rotz") final double z) {
        if (!isInitialized()) {
            Matrix3d matx = new Matrix3d(1,0,0,0,Math.cos(x),-Math.sin(x),0,Math.sin(x),Math.cos(x));
            Matrix3d maty = new Matrix3d(Math.cos(y), 0, Math.sin(y),0,1,0,-Math.sin(y),0,Math.cos(y));
            Matrix3d matz = new Matrix3d(Math.cos(z),-Math.sin(z),0,Math.sin(z),Math.cos(z),0,0,0,1);
            maty.mul(matx,maty);
            matz.mul(maty,matz);
            this.rotation = matz;
        }
    }
    @Inject(optional = true)
    public final void setOffset(@Named("offx") final double x, @Named("offy") final double y, @Named("offz") final double z) {
        if (!isInitialized()) {
            this.offset = new Vector3d(x,y,z);
        }
    }
}