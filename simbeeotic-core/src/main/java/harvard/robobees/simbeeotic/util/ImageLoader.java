package harvard.robobees.simbeeotic.util;


import java.awt.*;


/**
 * A utility for loading an image from the multiple soruces.
 *
 * @author bkate
 */
public class ImageLoader {

    /**
     * Loads an image from a resource that is available on the classpath. Classpath
     * resources can be accessed by an absolute path that is rooted in the jar file
     * containing the image. For example, if a classpath entry (jar file) contained
     * a relative path {@code images/foo.jpg}, the path passed to this method would
     * be {@code /images/foo.jpg}.
     *
     * @param path The path to the image to be loaded.
     *
     * @return The loaded image.
     */
    public static Image loadImageFromClasspath(String path) {
        return Toolkit.getDefaultToolkit().getImage(ImageLoader.class.getResource(path));
    }


    /**
     * Loads an image that is stored on the local filesystem. The path given should
     * either be relative to the working directory or an absolute path, delimited
     * by the '/' character.
     *
     * @param path The path to the image file on the filesystem.
     *
     * @return The loaded binary image.
     */
    public static Image loadImageFromFilesystem(String path) {
        return Toolkit.getDefaultToolkit().getImage(path);
    }
}
