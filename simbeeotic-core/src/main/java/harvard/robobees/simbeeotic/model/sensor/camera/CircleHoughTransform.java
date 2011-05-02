package harvard.robobees.simbeeotic.model.sensor.camera;
import java.util.ArrayList;
/** This class provides static methods that implement the Hough Transform. 
 *  These methods are used by CircleDetector to find a circle of radius r in the resuling
 *  image
 *
 */
public class CircleHoughTransform{  
    /** This method generates a series of points that lie on the circle cented at 
     *  point p with radius r
     *  @param p center of circle
     *  @param r radius of circle
     *  @return an arraylist of points lying on the circle
     */
    public static ArrayList<Point> evaluate(Point p, int r) {
        ArrayList<Point> list = new ArrayList<Point>();
        for (int d = 0; d <= r; d+=(r/10)) {
            int x1 = p.x + d;
            int x2 = p.x - d;
            int ydisp = (int)Math.round(Math.sqrt(r*r-d*d));
            int y1 = p.y + ydisp;
            int y2 = p.y - ydisp;
            list.add(new Point(x1,y1));
            list.add(new Point(x1,y2));   
            list.add(new Point(x2,y1));
            list.add(new Point(x2,y2));

        }
        return list;
    }
    /** This method keeps track of the number of circles that pass through a given pixel coordinate
     *  @param list Takes a list of points, which represent the points on a circle
     *  @param accumulator an array of ints such that accumulator[i][j] is the number of circles 
     *   that have been counted as passing through (i,j)
     *  @return returns an updated accumulator array, in which coordinates in list have been incremented
     *   in the accumulator array.
     *
     */
    private static int[][] increment (ArrayList<Point> list, int[][] accumulator) {
        for(Point p: list) 
            if (p.x >=0 && p.x < accumulator.length && p.y >= 0 && p.y < accumulator[p.x].length) accumulator[p.x][p.y]++;
        return accumulator;
    }                     
	/** This method finds the coordinate and Point through which the most circle point passed.
	 *  @param accumulator an array of ints such that accumulator[i][j] is the number of circles that have passed through (i,j)
	 *  @return the Point of the maximum value of accumulator, which represents the center of the circle.
	 */
    private static Point getGlobalMax(int[][] acc) {
    	int n = -1;
    	int x = -1;
    	int y = -1;
        for (int i = 0; i < acc.length; i++) {
            for (int j = 0; j < acc[i].length; j++) {
            	if (acc[i][j] > n) {
            		x = i;
            		y = j;
            		n = acc[i][j];
            	}
    		}
            	
    	}
    	System.out.println("Global max: " +x + ", " + y);
    	return new Point(x,y);
    }
    /** getCircle is the publically callable method, and integrates the helper methods together
     *  @param list list of all edge points
     *  @param radius radius of circle to be detected
     *  @param dim_x height of image
     *  @param dim_y width of image
     *  @return returns the center of the circle of a given image
     */
    public static Point getCircle(ArrayList<Point> list, int radius, int dim_x, int dim_y) {
        int[][] acc = new int[dim_x][dim_y];
        for (Point p: list) {
            acc = increment (evaluate(p, radius), acc);
        }
        return getGlobalMax(acc);
    }
    	   
}