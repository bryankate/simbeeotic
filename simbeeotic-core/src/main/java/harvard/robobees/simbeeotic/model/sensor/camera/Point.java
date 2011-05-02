/**
 * @(#)Point.java
 *
 *
 * Objects of this class carry two cartesian coordinates.
 */

package harvard.robobees.simbeeotic.model.sensor.camera;
public class Point {
	/** x - coordinate
	 */
	int x;
	/** y-coordinate
	 */
	int y;
	/** constructs a point with
	 *  @param x x-coordinate
	 *  @param y y-coordinate
	 */
    public Point(int x, int y) {
    	this.x = x;
    	this.y = y;
    }
}