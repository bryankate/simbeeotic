package harvard.robobees.simbeeotic.model.sensor.camera;
import javax.media.j3d.ImageComponent2D;
import java.awt.image.BufferedImage;


/**
 * Created by IntelliJ IDEA.
 * User: Joseph
 * Date: 4/24/11
 * Time: 8:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class CameraView {

    private ImageComponent2D buf;

    public CameraView(int w, int h){
        buf = new ImageComponent2D(ImageComponent2D.FORMAT_RGB, w, h);
    }

    //Return Buffered image from camera's view
    public BufferedImage getImg() {
        return buf.getImage();
    }

    //Return buffer to which images are written
    public ImageComponent2D getBuf() {
        return buf;
    }

    //Given a BufferedImage, return another with only edges visible (Vision algorithm)
    public BufferedImage findEdges(){
        CannyEdgeDetector d = new CannyEdgeDetector (2.0, 3, 160.0, 50.0);
		return d.getEdgesImage(buf.getImage());
    }
}
