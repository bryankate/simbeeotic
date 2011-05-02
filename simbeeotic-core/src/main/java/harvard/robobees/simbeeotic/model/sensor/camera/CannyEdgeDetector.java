/**
 * @(#)CannyEdgeDetector.java
 *
 *
 * @author
 * @version 1.00 2011/4/17
 */
package harvard.robobees.simbeeotic.model.sensor.camera;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.*;

public class CannyEdgeDetector {
    /**
    *
    */
    double sigma;
    double threshold;
    int offset;
    BufferedImage image;
    int height;
    int width;
    double[] kernel;
    double[][] kernel2;
    double hi;
    double lo;
    static final double[][] SOBEL_X = {{-0.25, 0, 0.25}, {-0.5,0,0.5}, {-0.25, 0, 0.25}};
    static final double[][] SOBEL_Y = {{-0.25, -0.5, -0.25}, {0, 0, 0}, {0.25, 0.5, 0.25}};

    public CannyEdgeDetector(double s, double t, BufferedImage i, double h, double l) {
        setGaussianKernel2(s, t);
        image = i;
        hi = h;
        lo = l;
        height = image.getHeight();
        width = image.getWidth();

    }

    public void setGaussianKernel(double s, double t){
        sigma = s;
        threshold = t;
        offset = (int)Math.round(Math.sqrt(-(Math.log10(t))*2*s*s));
        int size = 2 * offset + 1;
        kernel = new double[size];
        kernel[offset] = 1.0;
        double sum = 1.0;
        for (int i = 0; i < offset; i++) {
            double g = Math.exp(-(offset - i) * (offset - i)/(2*sigma*sigma));
            kernel[i] = g;
            kernel[offset + i + 1] = g;
            sum+=(2*g);
        }
        for (int i = 0; i < size; i++) {
            kernel[i] /= sum;
        }
    }

    public void setGaussianKernel2(double s, double t) {
        sigma = s;
        threshold = t;
        offset = (int)Math.round(Math.sqrt(-(Math.log10(t))*2*s*s));
        int size = 2 * offset + 1;
        kernel2 = new double[size][size];
        kernel2[offset][offset] = 1.0;
        double sum = 1.0;

        for(int i = 0; i < offset; i++) {
            for(int j = 0; j < offset; j++) {
                double g = Math.exp(-((offset-i)*(offset-i)+(offset-j)*(offset-j))/(2*sigma*sigma));
                kernel2[i][j] = g;
                kernel2[offset+i+1][j] = g;
                kernel2[i][offset+j+1] = g;
                kernel2[offset+i+1][offset+j+1] = g;
                sum += 4*g;
            }
        }

        for(int i = 0; i < size; i++) {
            for(int j = 0; j < size; j++) {
                kernel2[i][j] /= sum;
            }
        }
    }

    public int[][] convolve(int[][] data, double[][] operator) {
        int offset = (operator.length - 1)/2;
        int dimy = data.length;
        int dimx = data[0].length;
        int[][] convolved = new int[dimy-offset*2][dimx - offset*2]; // does this initialize to 0? It should
        for(int i = 0; i < dimy; i++) {
            for(int j = 0; j < dimx; j++) {
            	//System.out.println ("convolve: ij"+ i + j);
                int rgb = data[i][j];
                int red = getRed(rgb);
                int green = getGreen(rgb);
                int blue = getBlue(rgb);
                int alpha = getAlpha(rgb);
                for(int k = -offset; k < offset; k++) {
                    for(int l = -offset; l < offset; l++) {
                    	//System.out.println("convolve: kl " + k + l);
                        // convert should take red, green, blue and return the entire int
                        double toMult = operator[offset+k][offset+l];
                        //System.out.println("convolve: toMult"+toMult);
                        int newRGB = convert((int)(red*toMult),(int)(green*toMult),(int)(blue*toMult),(int)(alpha*toMult));
                        //System.out.println("convolve: newRGB" + newRGB);
                        if(i+k >= offset && i+k < dimy-offset && j+l >= offset && j+l < dimx-offset) {
                            convolved[i+k-offset][j+l-offset] += newRGB;
                            //System.out.println ("convolve: new convolved"+convolved[i+k-offset][j+l-offset]);
                        }
                    }
                }
            }
        }
        return convolved;

    }

    public int[][] applyGaussianKernel() {
        int[][] convolved = new int[height-offset*2][width - offset*2];
        for(int i = 0; i < height; i++) {
            for(int j = 0; j < width; j++) {
            	//System.out.println ("ij"+ i + j);
                int rgb = image.getRGB(j,i);
                int red = getRed(rgb);
                int green = getGreen(rgb);
                int blue = getBlue(rgb);
                int alpha = getAlpha(rgb);
                for(int k = -offset; k <= offset; k++) {
                    for(int l = -offset; l <= offset; l++) {
                    	//System.out.println("kl" + k + l);
                        double toMult = kernel2[offset+k][offset+l];
                        //System.out.println("toMult"+toMult);
                        int newRGB = convert((int)(red*toMult),(int)(green*toMult),(int)(blue*toMult), (int)(alpha*toMult));
                        //System.out.println("newRGB" + newRGB);
                        if(i+k >= offset && i+k < height-offset && j+l >= offset && j+l < width-offset) {
                            convolved[i+k-offset][j+l-offset] += newRGB;
                           //System.out.println ("new convolved"+convolved[i+k-offset][j+l-offset]);
                        }
                    }
                }
            }
        }
        return convolved;
    }

    public Gradient[][] findGradient(int[][] colors) {
        int[][] gx = convolve (colors, SOBEL_X);
        int[][] gy = convolve (colors, SOBEL_Y);
        Gradient[][] g = new Gradient[gx.length][gx[0].length];
        for (int i = 0; i < gx.length; i++) {
            for (int j = 0; j < gx[i].length; j++) {
                g[i][j] = new Gradient(gx[i][j], gy[i][j]);
            }
        }

		return g;
    }

    public boolean[][] findEdgePoints(Gradient[][] g, int[][] colors) {
        int dimy = colors.length;
        int dimx = colors[0].length;
        boolean[][] edges = new boolean[dimy][dimx];
        for (int i = 1; i < dimy-1; i++) {
            for (int j = 1; j < dimx-1; j++) {
                Gradient grad = g[i-1][j-1];
                int dir = grad.getDirection();
                int intensity = colors[i][j];
                switch(dir) {
                    case 0:
                        edges[i][j] = ((j <= 0 || intensity > colors[i][j-1]) && (j >= dimx-1 || intensity > colors[i][j+1]));
                        break;
                    case 45:
                        edges[i][j] = ((j <= 0 || i <= 0 || intensity > colors[i-1][j-1]) && (j >= dimx-1 || i >= dimy - 1 || intensity > colors[i+1][j+1]));
                        break;
                    case 90:
                        edges[i][j] = ((i <= 0 || intensity > colors[i-1][j]) && (i >= dimy-1 || intensity > colors[i+1][j]));
                        break;
                    case 135:
                        edges[i][j] = ((j >= dimx - 1 || i <= 0 || intensity > colors[i-1][j+1]) && (j <= 0 || i >= dimy - 1 || intensity > colors[i+1][j-1]));
                        break;
                }
            }
        }
	return edges;

    }

    public int[][] removeNonEdgePoints(int[][] data, boolean[][] e)
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

    public void updateImage(int[][] rgb, int crop) {
        for (int i = 0; i < height; i ++ ) {
            for (int j = 0; j < width; j ++) {
                if (!(i < crop || j < crop || i >= height - crop || j >= width - crop)) image.setRGB(j, i, rgb[i - crop][j - crop]);
            }
        }
    }

    public int getRed(int rgb) {
        return (rgb >> 16) & 0xFF;
    }

    public int getGreen(int rgb) {
        return (rgb >> 8) & 0xFF;
    }

    public int getBlue(int rgb) {
        return rgb & 0xFF;
    }

    public int getAlpha(int rgb) {
        return (rgb >> 24) & 0xFF;
    }

    public int convert(int red, int green, int blue, int alpha) {
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

	public BufferedImage findEdges() {
		int[][] a = applyGaussianKernel();
		Gradient[][] g = findGradient(a);
		boolean[][] b = findEdgePoints(g, a);
		a = removeNonEdgePoints(a, b);
		updateImage(a, 3);
        return this.image;
	}

}