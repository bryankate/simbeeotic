package harvard.robobees.simbeeotic.model.sensor.camera;
public class Gradient {

    double gx;
    double gy;
    double g;
    int direction;
    public Gradient (double x, double y) {
        gx = x;
        gy = y;
        g = Math.sqrt(gx*gx+gy*gy);
        double angle = Math.atan2(gy,gx)*180/Math.PI;
       // if (angle < 0) angle = 2*Math.PI - angle;
        //direction = (int)Math.round(angle / Math.PI * 4) % 4;
        if(angle < 0)
        	angle += 180;

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