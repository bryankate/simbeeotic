package harvard.robobees.simbeeotic.model.sensor.camera;
import java.util.ArrayList;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.*;
/** CircleDetector is an extension of CannyEdgeDetector. 
 *  It uses utilizes the edge detector to find edge points, 
 *  which it then uses to find a circle of a given radius r.
 *  The circle is marked in the resulting image with dots of a given color.
 *  We use the Hough Transform to find the center of the circle. 
 *  At each edge point, we take a circle of radius r, and notes the pixels it passes through.
 *  The pixel where the most circles pass through must be the center of the circle.
 */

public class CircleDetector extends CannyEdgeDetector {

    /** RGB value of color to mark circle.
     */
    int color;
    /** Constructor which sets the following (the first four are for Canny edge detection)
     *  Color is initiated to red by default.
     *  @param s = sigma
     *  @param o = offset
     *  @param h = hi hysteresis threshold
     *  @param l = lo hysteresis threshold
     */
    public CircleDetector(double s, int o, double h, double l) {
    	super(s,o,h,l);
        color = ColorInt.convert(255,0,0,255);

    }
    /** Constructor which sets the following (the first four are for Canny edge detection)
     *  Color is initiated to red by default.
     *  @param s = sigma
     *  @param o = offset
     *  @param h = hi hysteresis threshold
     *  @param l = lo hysteresis threshold
     *  @param c = color to mark circle
     */
    public CircleDetector(double s, int o, double h, double l, int c) {
    	super(s,o,h,l);
        color = c;

    }
    /** Draws a pixel of radius r around the point pt in red color.
     *  @param data Array which carries the RGB values of each pixel
     *  @param p Point which carries the x and y values of the center of the circle
     *  @param r Radius of the circle to draw
     *  @return a modified data array which carries the RGB values of the original input 
     *   with the newly drawn circle
     */
    private int[][] drawCircle (int[][] data, Point p, int r) {
       for (int d = 0; d <= r; d++) {
           int x1 = p.x + d;
           int x2 = p.x - d;
           int ydisp = (int)Math.round(Math.sqrt(r*r-d*d));
           int y1 = p.y + ydisp;
           int y2 = p.y - ydisp;
           if (x1>= 0 && x1<data.length){
                   if (y1>=0 && y1<data[x1].length)
                           data[x1][y1]=color;
                   if (y2>=0 && y2<data[x1].length)
                           data[x1][y2]=color;
           }
           if (x2>= 0 && x2<data.length){
                   if (y1>=0 && y1<data[x1].length)
                           data[x2][y1]=color;
                   if (y2>=0 && y2<data[x1].length)
                           data[x2][y2]=color;
           }

       }
       return data;
   }
    /**
    *    Generates a BufferedImage of the edges of a given input image.
    *    @param input original image
    *    @param r Radius of the circle to draw
    *    @return A buffered image of the edges of input.
    */
    public BufferedImage getCircleImage(BufferedImage input, int r) {
        int[][] a = applyGaussianKernel(toArray(input));
        Gradient[][] g = findNormGradient(a);
        boolean[][] e = findEdgePoints(g);
        boolean[][] e2 = hysteresis(g,e);
        ArrayList<Point> list = new ArrayList<Point>();
        for (int i = 0; i < e2.length; i++) {
            for (int j = 0; j < e2[i].length; j++) {
                if (e2[i][j]) list.add(new Point(i,j));
            }
        }
        int[][] edges = removeNonEdgePoints(a, e2);
        Point pt = CircleHoughTransform.getCircle(list, r, edges.length, edges[0].length);
        edges = drawCircle(edges, pt, r);
        return genImage(edges,input.getType());
    }

     /**
    *    Generates a BufferedImage array with circle detection run on the input image for
    *    circles with radii ranging from r1 to r2
    *    @param input original image
    *    @param r1 minimum radius to detect; should be <= r2
    *    @param r2 maximum radius to detect; should be >= r1
    *    @return A buffered image of the input after circle detection with radius r is run on it.
    */
    public BufferedImage[] getCircleImages(BufferedImage input, int r1, int r2) {
        BufferedImage c[] = new BufferedImage [r2-r1+1];
        for(int i = r1; i <= r2; ++i){
            c[i-r1] = getCircleImage(input, i);
        }
        return c;
    }
    /**
    *    Given an image, writes an image with circles of radius r detected to a given file.
    *    @param input original image
    *    @param pathout The path to which to write edge image
    *    @param r The radius of circles to detect
    *
    */
    public void writeCircle(BufferedImage input, String pathout, int r) {
        try {
            File outputfile = new File(pathout+".png");
            ImageIO.write(getCircleImage(input, r), "png", outputfile);
        } catch (IOException ex) {
            System.out.println ("write error");
        }
    }

    /**
    *    Given an image, writes images with circles of radii ranging from r1 to r2
    *    detected to files, with the file prefix given by pathout and the radius
    *    appended to this prefix
    *    @param input original image
    *    @param pathout The path to which to write edge image
    *    @param r1 The minimum radius of circles to detect
    *    @param r2 The maximum radius of circles to detect
    *
    */
    public void writeCircles(BufferedImage input, String pathout, int r1, int r2) {
        for(int i = r1; i < r2; ++i) {
            writeCircle(input, pathout+Integer.toString(i), i);
        }
    }
}