/**
 * @(#)ColorInt.java
 *
 * A Gradient class used to define the norm and direction of gradients.
 *
 * @author Tianen Li and Andrew Zhou
 * @version 1.00 2011/4/30
 */
package harvard.robobees.simbeeotic.model.sensor.camera;

public class Gradient {

    double gx;
    double gy;
    double g;
    int direction;
    // Given the x and y values of the Gradient, calculates its norm and direction
    public Gradient (int x, int y) {
        gx = x;
        gy = y;
        g = Math.sqrt(gx*gx+gy*gy);
        double angle = Math.atan2(gy,gx)*180/Math.PI;

		// if the angle is negative, adds 180. Based on how atan2 is defined, our
		// casework below works: near the x-axis in quadrant III approaches -180 degrees,
		// while near the x-axis in quadrant I approaches 0 degrees from below
        if(angle < 0)
        	angle += 180;

		// does casework to find (approximately) the direction of the gradient: 0 is near the
		// x-axis, 45 is near the x=y line, 90 is near the y-axis, and 135 is near the y=-x line.
		// note that the edge direction is orthogonal to the gradient direction
        if((angle >= 0 && angle <= 22.5) || (angle > 157.5 && angle <= 180))
        	direction = 0;
        if(angle > 22.5 && angle <= 67.5)
        	direction = 45;
        if(angle > 67.5  && angle <= 112.5)
        	direction = 90;
        if(angle > 112.5 && angle <= 157.5)
        	direction = 135;
    }

    int getDirection() {return direction;}
    double getGradientX() {return gx;}
    double getGradientY() {return gy;}
    double getNorm() {return g;}

}