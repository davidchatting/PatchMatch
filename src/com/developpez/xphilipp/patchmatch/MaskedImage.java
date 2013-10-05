package com.developpez.xphilipp.patchmatch;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Wrapper/Helper for Masked RGB BufferedImage 
 * 
 * @author Xavier Philippeau
 *
 */
public class MaskedImage {

	// image data
	private boolean[][] mask;
	private BufferedImage image;
	public final int W,H;
	
	// the maximum value returned by MaskedImage.distance() 
	public static final int DSCALE = 65535;
	
	// array for converting distance to similarity
	public static final double[] similarity;

	static {
		// build similarity curve such that similarity[0%]=0.999 and similarity[4%]=0.5
		double s_zero=0.999;
		double t_halfmax=0.10;

		double x = (s_zero-0.5)*2;
		double invtanh = 0.5*Math.log((1+x)/(1-x));
		double coef = invtanh/t_halfmax;
		
		similarity = new double[DSCALE+1];
		for(int i=0;i<similarity.length;i++) {
			double t = (double)i/similarity.length;
			similarity[i] = 0.5-0.5*Math.tanh(coef*(t-t_halfmax));
		}
	}
	
	// construct from existing BufferedImage and mask
	public MaskedImage(BufferedImage image, boolean[][] mask) {
		this.image = image;
		this.W=image.getWidth();
		this.H=image.getHeight();
		this.mask = mask;
		if (this.mask==null)
			this.mask = new boolean[W][H];
	}

	// construct empty image
	public MaskedImage(int width, int height) {
		this.W=width;
		this.H=height;
		this.image = new BufferedImage(W,H,BufferedImage.TYPE_INT_RGB);
		this.mask = new boolean[W][H];
	}
		
	public BufferedImage getBufferedImage() {
		return image;
	}
	
	public int getSample(int x, int y, int band) {
		return image.getRaster().getSample(x, y, band);
	}
	
	public void setSample(int x, int y, int band, int value) {
		image.getRaster().setSample(x, y, band, value);
	}

	public boolean isMasked(int x, int y) {
		return mask[x][y];
	}
	
	public void setMask(int x, int y, boolean value) {
		mask[x][y]=value;
	}

	
	public int countMasked() {
		int count=0;
		for(int y=0;y<H;y++)
			for(int x=0;x<W;x++) 
				if (mask[x][y]) count++;
		return count;
	}	

	// return true if the patch contains one (or more) masked pixel
	public boolean constainsMasked(int x, int y, int S) {
		for(int dy=-S;dy<=S;dy++) {
			for(int dx=-S;dx<=S;dx++) {
				int xs=x+dx, ys=y+dy;
				if (xs<0 || xs>=W) continue;
				if (ys<0 || ys>=H) continue;
				if (mask[xs][ys]) return true;
			}
		}
		return false;
	}	
	
	// distance between two patches in two images
	public static int distance(MaskedImage source,int xs,int ys, MaskedImage target,int xt,int yt, int S) {
		long distance=0, wsum=0, ssdmax = 10*255*255;
		
		// for each pixel in the source patch
		for(int dy=-S;dy<=S;dy++) {
			for(int dx=-S;dx<=S;dx++) {
				wsum+=ssdmax;
				
				int xks=xs+dx, yks=ys+dy;
				if (xks<0 || xks>=source.W) {distance+=ssdmax; continue;}
				if (yks<0 || yks>=source.H) {distance+=ssdmax; continue;}
				
				// cannot use masked pixels as a valid source of information
				if (source.isMasked(xks, yks)) {distance+=ssdmax; continue;}
				
				// corresponding pixel in the target patch
				int xkt=xt+dx, ykt=yt+dy;
				if (xkt<0 || xkt>=target.W) {distance+=ssdmax; continue;}
				if (ykt<0 || ykt>=target.H) {distance+=ssdmax; continue;}

				// cannot use masked pixels as a valid source of information
				if (target.isMasked(xkt, ykt)) {distance+=ssdmax; continue;}
				
				// SSD distance between pixels (each value is in [0,255^2])
				long ssd=0;
				
				// value distance (weight for R/G/B components = 3/6/1)
				for(int band=0;band<3;band++) {
					int weight = (band==0)?3:(band==1)?6:1;
					double diff2 = Math.pow( source.getSample(xks, yks, band) - target.getSample(xkt, ykt, band) , 2); // Value 
					ssd += weight*diff2;
				}
		
				// add pixel distance to global patch distance
				distance += ssd;
			}
		}
		
		return (int)(DSCALE*distance/wsum);
	}
	
	// Helper for BufferedImage resize
	public static BufferedImage resize(BufferedImage input, int newwidth, int newheight) {
		BufferedImage out = new BufferedImage(newwidth, newheight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = out.createGraphics();
		java.awt.Image scaled = input.getScaledInstance(newwidth, newheight, java.awt.Image.SCALE_SMOOTH);
		g.drawImage(scaled, 0, 0, out.getWidth(), out.getHeight(), null);
		g.dispose();
		return out;
	}

	// return a copy of the image
	public MaskedImage copy() {
		boolean[][] newmask= new boolean[W][H];
		BufferedImage newimage = new BufferedImage(W,H, BufferedImage.TYPE_INT_RGB);
		newimage.createGraphics().drawImage(image, 0, 0, null);
		for(int y=0;y<H;y++)
			for(int x=0;x<W;x++)
				newmask[x][y] = mask[x][y];
		return new MaskedImage(newimage,newmask);
	}
	
	// return a downsampled image (factor 1/2)
	public MaskedImage downsample() {
		int newW=W/2, newH=H/2;
		
		// Binomial coefficient kernels
		int[] kernelEven = new int[] {1,5,10,10,5,1}; 
		int[] kernelOdd = new int[] {1,4,6,4,1};

		int[] kernelx = (W%2==0)?kernelEven:kernelOdd;
		int[] kernely = (H%2==0)?kernelEven:kernelOdd;

		MaskedImage newimage = new MaskedImage(newW, newH);
		
		for(int y=0,ny=0;y<H-1;y+=2,ny++) {
			for(int x=0,nx=0;x<W-1;x+=2,nx++) {
				
				long r=0,g=0,b=0,ksum=0,masked=0,total=0;
				
				for(int dy=0;dy<kernely.length;dy++) {
					int yk=y+dy-2;
					if (yk<0 || yk>=H) continue;
					for(int dx=0;dx<kernelx.length;dx++) {
						int xk = x+dx-2;
						if (xk<0 || xk>=W) continue;
						
						total++;
						if (mask[xk][yk]) {masked++;continue;}
						
						int k = kernelx[dx]*kernely[dy];
						r+= k*this.getSample(xk, yk, 0);
						g+= k*this.getSample(xk, yk, 1);
						b+= k*this.getSample(xk, yk, 2);
						ksum+=k;
					}
				}
				
				if (ksum>0) {
					newimage.setSample(nx, ny, 0, (int)((double)r/ksum+0.5));
					newimage.setSample(nx, ny, 1, (int)((double)g/ksum+0.5));
					newimage.setSample(nx, ny, 2, (int)((double)b/ksum+0.5));
					newimage.setMask(nx, ny, false);
				} else {
					newimage.setMask(nx, ny, true);
				}
				
				
				if (masked>0.75*total)
					newimage.setMask(nx, ny, true);
				else
					newimage.setMask(nx, ny, false);
			}
		}
		
		return newimage;
	}
	
	
	// return an upscaled image
	public MaskedImage upscale(int newW,int newH) {
		MaskedImage newimage = new MaskedImage(newW, newH);
		newimage.image = resize(this.image, newW, newH);
		return newimage;
	}

}
