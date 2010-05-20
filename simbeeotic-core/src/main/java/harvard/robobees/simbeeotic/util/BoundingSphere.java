package harvard.robobees.simbeeotic.util;


import javax.vecmath.Vector3f;


/**
 * A class that represents a bounding sphere - a sphere that
 * completely encompasses an object's physical extent.
 *
 * @author bkate
 */
public class BoundingSphere {

    private Vector3f center;
    private float radius;


    public BoundingSphere(final Vector3f center, final float radius) {

        this.center = new Vector3f(center);
        this.radius = radius;
    }


    /**
     * Gets the center of the boundign sphere.
     *
     * @return The center of the sphere, in the world frame.
     */
    public final Vector3f getCenter() {
        return center;
    }


    /**
     * Gets the radius of the bounding sphere.
     *
     * @return The radius of the sphere (in meters).
     */
    public final float getRadius() {
        return radius;
    }


    /**
     * Determines if a bounding sphere intersects this sphere.
     *
     * @param other The sphere to check against.
     *
     * @return True if the other sphere intersects this sphere, false otherwise.
     */
    public final boolean intersects(final BoundingSphere other) {
        return intersects(this, other);
    }


    /**
     * Determines if two bounding spheres intersect.
     *
     * @param sphere1 The first sphere to check.
     * @param sphere2 The second shpere to check.
     *
     * @return True if the two spheres intersect, false otherwise.
     */
    public static boolean intersects(final BoundingSphere sphere1, final BoundingSphere sphere2) {

        Vector3f diff = new Vector3f();

        diff.sub(sphere1.getCenter(), sphere2.getCenter());

        return (diff.length() <= (sphere1.getRadius() + sphere2.getRadius()));
    }
}
