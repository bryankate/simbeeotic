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
	/** radius of circle to be detected
	 */
    int radius;
    /** RGB value of color to mark circle.
     */
    int color;
    /** Constructor which sets the following (the first four are for Canny edge detection)
     *  Color is initiated to red by default.
     *  @param s = sigma
     *  @param o = offset
     *  @param h = hi hysteresis threshold
     *  @param l = lo hysteresis threshold
     *  @param r = radius to be detected
     */
    public CircleDetector(double s, int o, double h, double l, int r) {
    	super(s,o,h,l);
        radius = r;
        color = ColorInt.convert(255,0,0,255);

    }
    /** Constructor which sets the following (the first four are for Canny edge detection)
     *  Color is initiated to red by default.
     *  @param s = sigma
     *  @param o = offset
     *  @param h = hi hysteresis threshold
     *  @param l = lo hysteresis threshold
     *  @param r = radius to be detected
     *  @param c = color to mark circle
     */
    public CircleDetector(double s, int o, double h, double l, int r, int c) {
    	super(s,o,h,l);
        radius = r;
        color = c;

    }
    /** Draws a pixel of radius r around the point pt in red color.
     *  @param data Array which carries the RGB values of each pixel
     *  @param pt Point which carries the x and y values of the center of the circle
     *  @return a modified data array which carries the RGB values of the original input 
     *   with the newly drawn circle
     */
    private int[][] drawCircle (int[][] data, Point p) {
        for (int d = 0; d <= radius; d++) {
            int x1 = p.x + d;
            int x2 = p.x - d;
            int ydisp = (int)Math.round(Math.sqrt(radius*radius-d*d));
            int y1 = p.y + ydisp;
            int y2 = p.y - ydisp;
            data[x1][y1] = color;
            data[x2][y2] = color;
            data[x1][y2] = color;
            data[x2][y1] = color;

        }
        return data;
    	
    }
    /**
    *    Generates a BufferedImage of the edges of a given input image.
    *    @param input original image
    *    @return A buffered image of the edges of input.
    */
    public BufferedImage getEdgesImage(BufferedImage input) {
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
        Point pt = CircleHoughTransform.getCircle(list, radius, edges.length, edges[0].length);
        edges = drawCircle(edges, pt);
        return genImage(edges,input.getType());
    }

  /**
    *    Given an image's path, writes an image of its edges to a file after instantiating a
    *    CircleDetector
    *    @param pathin The path at which the image is located
    *    @param pathout The path to which to write edge image
	*
    */

    public static void testImg(String pathin, String pathout) {
        CircleDetector d = new CircleDetector (2.0, 3, 160.0, 50.0, 60);
        d.writeEdges(pathin, pathout);
    }

    public static void main (String[] args) {
        testImg("circle.jpg", "circle.png");
    }
    
    
}