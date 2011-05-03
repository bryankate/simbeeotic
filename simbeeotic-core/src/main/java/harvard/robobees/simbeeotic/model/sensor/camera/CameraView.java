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
    CircleDetector d = new CircleDetector(0.1, 3, 21, 6);

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
    public void writeEdges(String pathout){
		d.writeEdges(getImg(), pathout);
    }
    //Given a BufferedImage, return another with circles of radius r detected
    public void writeCircle(String pathout, int r){
        d.writeCircle(getImg(), pathout, r);
    }
    //Given a BufferedImage, return an array of BufferedImages with cirlces ranging
    //from radius r1 to r2 detected
    public void writeCircles(String pathout, int r1, int r2) {
        d.writeCircles(getImg(), pathout, r1, r2);
     }
}
