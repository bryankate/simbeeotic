/**
 * @(#)CannyEdgeDetector.java
 *
 *
 * @author Tianen Li and Andrew Zhou
 * @version 1.00 2011/4/17
 */
package harvard.robobees.simbeeotic.model.sensor.camera;
import  javax.media.j3d.ColorInterpolator;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.*;

/** This class is an implementation of the Canny Edge Detection Algorithm.
 *  It contains the utilities to read a file, generate a BufferedImage from that file,
 *  and then process the image to create a BufferedImage showing the edges of the original image.
 *
 *  Briefly summarized, the algorithm consists of the following steps:
 *  1) We convolve the image with a Gaussian kernel to blur it, smoothing out the image and removing noise.
 *     This gets rid of pixels with high gradients due to noise, ensuring that only those pixels on
 *     edges are detected.
 *
 *  2) The gradient (direction of maximum change) of color is estimated by convolving the image with
 *     the Sobel operators SOBEL_X and SOBEL_Y. The SOBEL_X operator detects points with a high rate
 *     of change in the x-direction by giving negative weight to pixels to the left of the current
 *     pixel and positive weight to pixels to the right. Thus, those pixels with a large difference between
 *     their left neighbors and right neighbors will have large gradients. The SOBEL_Y operator does likewise
 *     for the y-direction.
 *
 *  3) We employ non-maximum suppression to find a preliminary set of edge points whose gradients are local maxima.
 *     We first find the direction of the gradient for each point : either 0, 45, 90, or 135 degrees, corresponding to the x-axis,
 *     the x=y line, the y axis, and the x=-y line respectively.
 *     These directions allow us to isolate two neighbors for each point: the two neighbors orthogonal to the direction
 *     of the edge at that point (if one exists). If the given point's gradient magnitude is greater than the gradient
 *     magnitude of its two neighbors, its gradient is a local maximum, so the point is a candidate edge point.
 *
 *  4) We employ hysteresis and thresholding to process our candidate points and "trace" edges through the image. We first establish
 *     two thresholds: a high threshold and a low threshold. Candidate points whose gradients are above the high threshold are
 *     automatically classified as edge points: we assume that the high threshold is sufficiently high so that chance/noise alone
 *     are unlikely to push a point's gradient above this value. On the other hand, points above the low threshold but below the high
 *     threshold are deemed possible (but not guaranteed) edge points. If a possible edge point is next to a marked edge point, though,
 *     the possible edge point is marked as a edge point itself. Note that if a possible edge point is next to a edge point that used to
 *     be a possible edge point, the first point is marked as an edge point itself. Points whose gradients are lower than the low threshold
 *     are not edge points.
 *     Using two thresholds allows us to frther combat the influence of noise. Those candidate points whose gradients are above the high threshold
 *     are guaranteed to be edge points, so we mark those first. Of the potential edge points whose gradients are larger than the lower threshold
 *     but lower than the high threshold, those potential points adjacent to another edge point are more likely to be actual edge points than
 *     noise, so we mark them as such. We assume that the candidate edge points not adjacent to a verified edge point are the result of noise.
 *
 */
public class CannyEdgeDetector {
    /**
    *    the standard deviation used to compute the Gaussian Kernel. Greater values correspond to greater blurring effect.
    */
    double sigma;
    /**
	 *    This is the number of standard deviations in both dimensions (x and y) and both directions (positive and negative) encompassed by the
	 *    Gaussian blur. It is also the number of pixels the image is clipped by due to the Gaussian blur (we exclude pixels where the Gaussian
	 *    kernel, when overlaid on the image, would extend outside the image's boundaries)
	 *
	 */
    int offset;
    /**
    *    The dimension of the Gaussian kernel. The greater the size, the closer approximation it is to a true Gaussian blur. Should always be odd,
    *    and is equal to 2*offset+1.
    */
    int size;

    /**
    *    The normalized Gaussian kernel (sum of all values is 1). It is normalized to prevent darkening or brightening the picture as a result of blurring
    */
    double[][] kernel;
    /**
    *    Upper threshold for hysteresis
    */
    double t1;
    /**
    *    Lower threshold for hysteresis
    */
    double t2;
    /**
    *    Sobel operator that estimates the magnitude of the Gradient in the x direction
    */
    static final double[][] SOBEL_X = {{-0.25, 0, 0.25}, {-0.5,0,0.5}, {-0.25, 0, 0.25}};
    /**
    *    Sobel operator that estimates the magnitude of the Gradient in the y direction
    */
    static final double[][] SOBEL_Y = {{-0.25, -0.5, -0.25}, {0, 0, 0}, {0.25, 0.5, 0.25}};
    /**
    *    Constructs a CannyEdgeDetector Object with the following settings.
    *    @param s Sigma of Gaussian Kernel
    *    @param o Offset of Gaussian Kernel (determines size)
    *    @param h Upper threshold for hysteresis
    *    @param l Lower threshold for hysteresis
    */
    public CannyEdgeDetector(double s, int o, double h, double l) {
        setGaussianKernel(s, o);
        t1 = h;
        t2 = l;
    }
    /**
    *    Sets the High Threshold for hysteresis to t
    *    @param t threshold
    */
    public void setHiThreshold(double t) {t1 = t;}
    /**
    *    Sets the low threshold for hysteresis to t
    *    @param t threshold
    */
    public void setLoThreshold(double t) {t2 = t;}
    /**
    *    Sets the sigma to s, and offset to o, and set kernel to a Gaussian operator with those settings. The Gaussian kernel is
    *    a (2o+1)x(2o+1) array with value G(i,j)=c*e^(-(|o-i|^2+|o-j|^2)/2s^2) for an element with indices (i,j), where |o-i| and
    *    |o-j| are the element's x and y offsets from the center, respectively. c, the reciprocal of the sum of all the elements
    *    prior to normalization, is a normalization constant and used to ensure that the entire Gaussian kernel sums to 1. Note that
    *    while the Gaussian does not encompass the entire extent of the Gaussian curve (which extends infinitely out in both the x and
    *    y directions), setting o = 3 or above will capture the vast majority of the area underneath the curve.
    *    @param s sigma
    *    @param o offset
    */
    public void setGaussianKernel(double s, int o) {
        sigma = s;
        offset = o;
        size = 2 * offset + 1;
        kernel = new double[size][size];
        kernel[offset][offset] = 1.0;
        double sum = 1.0;

        for(int i = 0; i < offset; i++) {
            for(int j = 0; j < offset; j++) {
                double g = Math.exp(-((offset-i)*(offset-i)+(offset-j)*(offset-j))/(2*sigma*sigma));

                kernel[i][j] = g;
                kernel[offset+i+1][j] = g;
                kernel[i][offset+j+1] = g;
                kernel[offset+i+1][offset+j+1] = g;
                sum += 4*g;
            }
        }

        for(int i = 0; i < size; i++) {
            for(int j = 0; j < size; j++) {
                kernel[i][j] /= sum;
            }
        }
    }
    /**
    *    Returns an int[][] representation of the given BufferedImage
    *    @param img BufferedImage Object containing image to be converted
    *    @return Returns an int[][] output where output[i][j] is the RGB value of the pixel in the jth row, ith column of the image (where upper left corner is (0,0))
    */
    protected int[][] toArray(BufferedImage img) {
        int height = img.getHeight();
        int width = img.getWidth();
        int[][] output = new int[height][width];
        for(int i = 0; i < height; i++) {
            for(int j = 0; j < width; j++) {
                //System.out.println ("ij"+ i + j);
                output[i][j] = img.getRGB(j,i);
            }
        }
        return output;
    }
    /**
    *    Convolves image data with a given operator. Convolution consists of redistributing a pixel's value to itself and its neighbors in a way dictated
    *    by the particular operator passed in. For a 3x3 operator, the center of the operator is centered on the pixel, the pixel's value is multiplied by
    *    each of the elements of the operator, and the resulting product is added to the value of the image data at that point (which is stored in a temporary
    *    data structure that starts out at 0, so we don't overwrite values during the process). To deal with dfferent color values, we perform the convolution
    *    on each color separately and then combine the separate results into the whole answer.
    *    @param data 2-dimensional array of RGB color values
    *    @param operator the given operator that will be convolved with the data
    *    @return An array where the data at each point is distributed to its surroundings by the given operator. Results are truncated to the nearest int
    */
    protected int[][] convolve(int[][] data, double[][] operator) {
        int offset = (operator.length - 1)/2;
        int dimy = data.length;
        int dimx = data[0].length;
        int convolve_y = dimy - offset*2;
        int convolve_x = dimx - offset*2;
        double[][] convolved_red = new double[convolve_y][convolve_x];
        double[][] convolved_green = new double[convolve_y][convolve_x];
        double[][] convolved_blue = new double[convolve_y][convolve_x];
        double[][] convolved_alpha = new double[convolve_y][convolve_x];
        for(int i = 0; i < dimy; i++) {
            for(int j = 0; j < dimx; j++) {
                //System.out.println ("convolve: ij"+ i + j);
                int rgb = data[i][j];
                int red = ColorInt.getRed(rgb);
                int green = ColorInt.getGreen(rgb);
                int blue = ColorInt.getBlue(rgb);
                int alpha = ColorInt.getAlpha(rgb);
                for(int k = -offset; k <= offset; k++) {
                    for(int l = -offset; l <= offset; l++) {
                        //System.out.println("convolve: kl " + k + l);
                        // convert should take red, green, blue and return the entire int
                        double toMult = operator[offset+k][offset+l];
                        //System.out.println("convolve: toMult"+toMult);
                        //System.out.println("convolve: newRGB" + newRGB);
                        if(i+k >= offset && i+k < dimy-offset && j+l >= offset && j+l < dimx-offset) {
                            convolved_red[i+k-offset][j+l-offset] += (toMult*red);
                            convolved_green[i+k-offset][j+l-offset] += (toMult*green);
                            convolved_blue[i+k-offset][j+l-offset] += (toMult*blue);
                            convolved_alpha[i+k-offset][j+l-offset] += (toMult*alpha);
                            //System.out.println ("convolve: new convolved"+convolved[i+k-offset][j+l-offset]);
                        }
                    }
                }
            }
        }
        int[][] convolved = new int[dimy-offset*2][dimx - offset*2];
        for (int i = 0; i < convolved_red.length; i++) {
            for (int j = 0; j < convolved_red[i].length; j++) {
				// combine the separately convolved values into a 32-bit color
                convolved[i][j] = ColorInt.convert((int)convolved_red[i][j], (int)convolved_green[i][j], (int)convolved_blue[i][j], (int)convolved_alpha[i][j]);
            }
        }
        return convolved;

    }
    /**
    *    Convolves image data with the Gaussian kernel
    *    @param data 2-dimensional array of RGB color values
    *    @return An array of color values convolved the Gaussian kernel
    */
    protected int[][] applyGaussianKernel(int[][] data) {
        return convolve(data, kernel);
    }
    /**
    *    Given an array of ints, approximates the gradient of those values by convolving the image data with the Sobel operator
    *    @param data 2-dimensional array of values. The method will approximate the gradient of a function f(x,y) = data[x][y].
    *    @return An array g of Gradient objects which contain the magnitude and direction of the gradient vector where g[x][y] = Grad(f)(x,y)
    */
    protected Gradient[][] findGradient(int[][] data) {
        int[][] gx = convolve (data, SOBEL_X);
        int[][] gy = convolve (data, SOBEL_Y);
        Gradient[][] g = new Gradient[gx.length][gx[0].length];
        for (int i = 0; i < gx.length; i++) {
            for (int j = 0; j < gx[i].length; j++) {
                g[i][j] = new Gradient(gx[i][j], gy[i][j]);
            }
        }

        return g;
    }
    /**
    *    Given an array of int color values, approximates the gradient of those values by first finding the norm of the color and
    *    then convolving that with the Sobel operator
    *    @param colors 2-dimensional array of RGB color values
    *    @return An array of Gradient objects which contain the magnitude and direction of the gradient vector.
    */

    protected Gradient[][] findNormGradient(int[][] colors) {
        return findGradient(ColorInt.normalizeArr(colors));

    }
    /**
    *    Given an array of Gradient values, determines the edge points using non-maximum supression. For each pixel, we first find the direction of the gradient,
    *    and then check the appropriate neighbors of the pixel based on that direction (these pixels are orthogonal to the edge direction). If the pixel's
    *    gradient intensity is greater than the intensity of both its neighbors, we mark it as a possible edge. Hence non-maximum suppression: those pixels
    *    whose gradients are not local maxima are suppressed (not marked as potential edges).
    *    @param g Array of Gradient objects
    *    @return An array edges of booleans where edges[i][j] is true if g[i][j] was a local maxima and false if it isn't.
    */
    protected boolean[][] findEdgePoints(Gradient[][] g) {
        int dimy = g.length;
        int dimx = g[0].length;
        boolean[][] edges = new boolean[dimy][dimx];
        for (int i = 0; i < dimy; i++) {
            for (int j = 0; j < dimx; j++) {
                Gradient grad = g[i][j];
                int dir = grad.getDirection();
                double norm = grad.getNorm();
                switch(dir) {
                    case 0:
                    	// if the edge runs along the y-axis (direction = 0), check the points to the west and east
                        edges[i][j] = ((j <= 0 || norm > g[i][j-1].getNorm()) && (j >= dimx-1 || norm > g[i][j+1].getNorm()));
                        break;
                    case 45:
                    	// if the edge runs along the x=y line (direction = 45), check the points to the northeast and southwest
                        edges[i][j] = ((j <= 0 || i <= 0 || norm > g[i-1][j-1].getNorm()) && (j >= dimx-1 || i >= dimy - 1 || norm > g[i+1][j+1].getNorm()));
                        break;
                    case 90:
                    	// if the edge runs along the x-axis (direction = 90), check the points to the north and south
                        edges[i][j] = ((i <= 0 || norm > g[i-1][j].getNorm()) && (i >= dimy-1 || norm > g[i+1][j].getNorm()));
                        break;
                    case 135:
                    	// if the edge runs along the x=-y line (direction = 135), check the points to the northwest and southeast
                        edges[i][j] = ((j >= dimx - 1 || i <= 0 || norm > g[i-1][j+1].getNorm()) && (j <= 0 || i >= dimy - 1 || norm > g[i+1][j-1].getNorm()));
                        break;
                }
            }
        }
        return edges;

    }
   /**
	*    Given a verified edge point's coordinates (x,y) at which to start, traces the edge from that point using a floodfill. Essentially, we trace out
	*    the edge recursively starting at the given point by checking whether its neighbors meet the threshold requirement.
	*    @param y The y coordinate at which to start the floodfill
	*    @param x The x coordinate at which to start the floodfill
	*    @param g Array of Gradient objects
	*    @param edges2 The list of edge candidates which trace will update
	*/
    protected void trace(int y, int x, Gradient[][] g, boolean[][] edges2) {
        int dimy = g.length;
        int dimx = g[0].length;
        Gradient grad = g[y][x];
        int dir = grad.getDirection();
        int y1 = -1;
        int x1 = -1;
        int y2 = -1;
        int x2 = -1;
        // calculates the neighbors for each direction
        switch(dir) {
        case 0:
            y1 = y;
            x1 = x-1;
            y2 = y;
            x2 = x+1;
            break;
        case 45:
            y1 = y-1;
            x1 = x-1;
            y2 = y+1;
            x2 = x+1;
            break;
        case 90:
            y1 = y-1;
            x1 = x;
            y2 = y+1;
            x2 = x;
            break;
        case 135:
            y1 = y-1;
            x1 = x+1;
            y2 = y+1;
            x2 = x-1;
            break;
        }

        if(y1 >= 0 && y1 < dimy && x1 >= 0 && x1 < dimx) {
			// edges2[y1][x1] must not be a verified edge - if we don't include this condition
			// there will be an infinite loop. We can do this because if edges2[y1][x1] is set to
			// true, we know trace has been called on it before.
            if(!edges2[y1][x1] && g[y1][x1].getNorm() > t2) {
                edges2[y1][x1] = true;
                trace(y1, x1, g, edges2);
            }
        }

        if(y2 >= 0 && y2 < dimy && x2 >= 0 && x2 < dimx) {
            if(!edges2[y2][x2] && g[y2][x2].getNorm() > t2) {
                edges2[y2][x2] = true;
                trace(y2, x2, g, edges2);
            }
        }
    }
  /**
	*    Given the image's Gradient array and a list of edge candidates from non-maximum suppression, performs hysteresis on the array using a floodfill
	*    starting from all of the edge candidates meeting the upper threshold requirement. We first set all candidates meeting the upper threshold to
	*    guaranteed edges. Then we run a floodfill (trace) from each guaranteed candidate that checks each of its neighbors, sets those neighbors meeting the lower
	*    threshold to guaranteed edge points, and floodfills starting from those neighbors.
	*    @param g Array of Gradient objects
	*    @param edges A list of edge candidates from non-maximum suppression
	*/
    protected boolean[][] hysteresis(Gradient[][] g, boolean[][] edges) {
        int dimy = g.length;
        int dimx = g[0].length;
        boolean[][] edges2 = new boolean[dimy][dimx];
        for(int i = 0; i < dimy; i++) {
            for(int j = 0; j < dimx; j++) {
                Gradient grad = g[i][j];
                double norm = grad.getNorm();
                // if edges[i][j] and (norm > t1) (i,j) is a guaranteed edge point because it is an edge candidate from non-maximum suppression
                // and its gradient magnitude is greater than the upper threshold
                edges2[i][j] = edges[i][j] && (norm > t1);
            }
        }
        for(int i = 0; i < dimy; i++) {
            for(int j = 0; j < dimx; j++) {
                if(edges[i][j] && (g[i][j].getNorm() > t1))  {
					// floodfills starting from (i,j), which is a guaranteed edge point meeting the upper threshold
                    trace(i,j,g,edges2);
                }
            }
        }

        return edges2;
    }
    /**
    *    Given an array of image color values, sets all non-edge points to black.
    *    @param data Array of color RGB values
    *    @param e Array of booleans such that e[i][j] is whether or not the point represented by data[i][j] is an edge or not.
    *    @return An array of RGB values of which if e[i][j] is false, than the corresponding value in the array is 0.
    */
    protected int[][] removeNonEdgePoints(int[][] data, boolean[][] e)
    {
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                if (i == 0 || i == data.length - 1 || j == 0 || j == data[i].length - 1 || !(e[i-1][j-1])) {
                    data[i][j] = 0;
                }
            }
        }
        return data;
    }
    /**
    *    Given an array of image color values, converts it to a BufferedImage of a given type.
    *    @param rgb Array of color RGB values
    *    @param type Type of BufferedImage to be generated (see BufferedImage).
    *    @return A buffered image where the RGB value of the jth row ith column is rgb[i][j].
    */
    public BufferedImage genImage(int[][] rgb, int type) {
        if (type == BufferedImage.TYPE_CUSTOM) type = BufferedImage.TYPE_4BYTE_ABGR;
        int h = rgb.length;
        int w = rgb[0].length;
        BufferedImage output = new BufferedImage(w, h, type);
        for (int i = 0; i < h; i ++ ) {
            for (int j = 0; j < w; j ++) {
                output.setRGB(j, i, rgb[i][j]);
            }
        }
        return output;
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
        int[][] edges = removeNonEdgePoints(a, e2);
        return genImage(edges,input.getType());
    }
    /**
    *    Generates a BufferedImage of the edges of a given input image.
    *    @param f File of original image
    *    @return A buffered image of the edges of input. Returns null if file is invalid.
    */
    public BufferedImage getEdgesImage(File f) {
        try {
            BufferedImage img = ImageIO.read(f);
            return getEdgesImage(img);
        } catch (IOException ex) {
            System.out.println("File error");
            return null;
        }
    }
    /**
    *    Generates a BufferedImage of the edges of a given input image.
    *    @param pathin Path of original image.
    *    @return A buffered image of the edges of input image. Returns null if path is invalid.
    */
    public BufferedImage getEdgesImage(String pathin) {
        return getEdgesImage((new File(pathin)));
    }
   /**
    *    Given an image, writes an image of its edges to a file.
    *    @param input original image
    *    @param pathout The path to which to write edge image
    *
    */
    public void writeEdges(BufferedImage input, String pathout) {
        try {
            File outputfile = new File(pathout);
            ImageIO.write(getEdgesImage(input), "png", outputfile);
        } catch (IOException ex) {
            System.out.println ("write error");
        }
    }
  /**
    *    Given a File containing an image, writes an image of its edges to a file
    *    @param f File containing original image
    *    @param pathout The path to which to write edge image
    *
    */
    public void writeEdges(File f, String pathout) {
        try {
            File outputfile = new File(pathout);
            ImageIO.write(getEdgesImage(f), "png", outputfile);
        } catch (IOException ex) {
            System.out.println ("write error");
        }
    }
  /**
    *    Given an image's path, writes an image of its edges to a file
    *    @param pathin The path at which the image is located
    *    @param pathout The path to which to write edge image
	*
    */
    public void writeEdges(String pathin, String pathout) {
        try {
            File outputfile = new File(pathout);
            ImageIO.write(getEdgesImage(pathin), "png", outputfile);
        } catch (IOException ex) {
            System.out.println ("write error");
        }
    }

  /**
    *    Given an image's path, writes an image of its edges to a file after instantiating a
    *    CannyEdgeDetector
    *    @param pathin The path at which the image is located
    *    @param pathout The path to which to write edge image
	*
    */

    public static void testImg(String pathin, String pathout) {

        CannyEdgeDetector d = new CannyEdgeDetector (2.0, 3, 160.0, 50.0);
        d.writeEdges(pathin, pathout);

    }

    public static void main (String[] args) {
        testImg("flower.jpg", "flower.png");
    }

}