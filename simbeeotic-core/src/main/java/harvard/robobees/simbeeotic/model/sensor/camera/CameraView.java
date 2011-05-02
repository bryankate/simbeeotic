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
    //make sure BufferedImage size etc match in all references
    private ImageComponent2D buf;
    public CameraView(int w, int h){
        buf = new ImageComponent2D(ImageComponent2D.FORMAT_RGB, w, h);
    }
    public BufferedImage getImg() {
        return buf.getImage();
    }
    public ImageComponent2D getBuf() {
        return buf;
    }
    public BufferedImage findEdges(){
        CannyEdgeDetector d = new CannyEdgeDetector (1.0, 0.05, buf.getImage(), 5.0, 10.0);
		return d.findEdges();
    }



}
