package harvard.robobees.simbeeotic.util;


import javax.vecmath.Vector3f;


/**
 * @author bkate
 */
public class BoundingSphere {

    private Vector3f center;
    private float radius;


    public BoundingSphere(final Vector3f center, final float radius) {

        this.center = new Vector3f(center);
        this.radius = radius;
    }


    public final Vector3f getCenter() {
        return center;
    }


    public final float getRadius() {
        return radius;
    }


    public final boolean intersects(final BoundingSphere other) {
        return intersects(this, other);
    }


    public static boolean intersects(final BoundingSphere sphere1, final BoundingSphere sphere2) {

        Vector3f diff = new Vector3f();

        diff.sub(sphere1.getCenter(), sphere2.getCenter());

        return (diff.length() <= (sphere1.getRadius() + sphere2.getRadius()));
    }
}
