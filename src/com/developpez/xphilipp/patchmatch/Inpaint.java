package com.developpez.xphilipp.patchmatch;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Inpaint using the PatchMatch Algorithm
 * 
 * | PatchMatch : A Randomized Correspondence Algorithm for Structural Image Editing
 * | by Connelly Barnes and Eli Shechtman and Adam Finkelstein and Dan B Goldman
 * | ACM Transactions on Graphics (Proc. SIGGRAPH), vol.28, aug-2009
 *
 * @author Xavier Philippeau
 * 
 */
public class Inpaint {
	
	//initial image
	MaskedImage initial;
	
	// Nearest-Neighbor Field
	NNF nnf_TargetToSource;
	
	// patch radius
	int radius;
	
	// Pyramid of downsampled initial images
	List<MaskedImage> pyramid;
	
	public BufferedImage inpaint(BufferedImage input, boolean[][] mask, int radius) {
		// initial image
		this.initial = new MaskedImage(input, mask);
		
		// patch radius
		this.radius = radius;

		// working copies
		MaskedImage source = initial;

		System.out.println("build pyramid of images...");
		
		// build pyramid of downscaled images
		this.pyramid = new ArrayList<MaskedImage>();
		this.pyramid.add(source);
		while(source.W>radius && source.H>radius) {
			if (source.countMasked()==0) break;
			source = source.downsample();
			this.pyramid.add(source);
			
		}
		int maxlevel=this.pyramid.size();
		
		// The initial target is the same as the smallest source.
		// We consider that this target contains no masked pixels
		MaskedImage target = source.copy();
		for(int y=0;y<target.H;y++)
			for(int x=0;x<target.W;x++)
				target.setMask(x,y,false);
		
		// for each level of the pyramid 
		for(int level=maxlevel-1;level>=1;level--) {
			System.out.println("\n*** Processing -  Zoom 1:"+(1<<level)+" ***");

			// create Nearest-Neighbor Fields (direct and reverse)
			source = this.pyramid.get(level);
			
			System.out.println("initialize NNF...");
			if (level==maxlevel-1) {
				// at first,  use random data as initial guess
				nnf_TargetToSource = new NNF(target, source, radius);
				nnf_TargetToSource.randomize();
			} else {
				// then, we use the rebuilt (upscaled) target 
				// and reuse the previous NNF as initial guess
				NNF new_nnf = new NNF(target, source, radius);
				new_nnf.initialize(nnf_TargetToSource);
				nnf_TargetToSource = new_nnf;
			}
			
			// Build an upscaled target by EM-like algorithm (see "PatchMatch" - page 6)
			target = ExpectationMaximization(level);
		}

		return target.getBufferedImage();
	}
	
	// EM-Like algorithm (see "PatchMatch" - page 6)
	// Returns a double sized target image
	private MaskedImage ExpectationMaximization(int level) {
		
		int iterEM = Math.min(2*level,4);
		int iterNNF = Math.min(5,level);
		
		MaskedImage source = nnf_TargetToSource.output;
		MaskedImage target = nnf_TargetToSource.input;
		MaskedImage newtarget = null;
		
		System.out.print("EM loop (em="+iterEM+",nnf="+iterNNF+") :");

		// EM Loop
		for(int emloop=1;emloop<=iterEM;emloop++) {

			System.out.print(" "+(1+iterEM-emloop));
			
			// set the new target as current target
			if (newtarget!=null) {
				nnf_TargetToSource.input = newtarget;
				target = newtarget;
				newtarget = null;
			}

			// -- minimize the NNF
			nnf_TargetToSource.minimize(iterNNF);
			
			// -- Now we rebuild the target using best patches from source
			
			MaskedImage newsource;
			boolean upscaled = false;
				
			// Instead of upsizing the final target, we build the last target from the next level source image 
			// So the final target is less blurry (see "Space-Time Video Completion" - page 5)
			if (level>=1 && (emloop==iterEM) ) {
				newsource = pyramid.get(level-1);
				newtarget = target.upscale(newsource.W,newsource.H);
				upscaled = true;
			} else {
				newsource = pyramid.get(level);
				newtarget = target.copy();
				upscaled = false;
			}

			// --- EXPECTATION/MAXIMIZATION step ---
			EM_Step(newsource, newtarget, nnf_TargetToSource, upscaled);
			
			// debug : display intermediary result
			//BufferedImage result = MaskedImage.resize(newtarget.getBufferedImage(), initial.W, initial.H);
			//Demo.display(result);
		}
		System.out.println();
		
		return newtarget;
	}

	// store the RGB histograms (used in EM_TensorVoting)
	double[][] histo = new double[3][256];
	
	// Expectation-Maximization step : vote for best estimations of each pixel and compute maximum likelihood
	private void EM_Step(MaskedImage source, MaskedImage target, NNF nnf, boolean upscaled) {
		int[][][] field = nnf.getField();
		int R = nnf.S;
		if (upscaled) R*=2;
		
		// for each pixel in the target image
		for(int y=0;y<target.H;y++) {
			for(int x=0;x<target.W;x++) {

				// clear histograms
				Arrays.fill(histo[0], 0);
				Arrays.fill(histo[1], 0);
				Arrays.fill(histo[2], 0);
				double wsum=0;

				// **** ESTIMATION STEP ****
				
				// for all target patches containing the pixel
				for(int dy=-R;dy<=R;dy++) {
					for(int dx=-R;dx<=R;dx++) {
						
						// (xpt,ypt) = center pixel of the target patch
						int xpt=x+dx, ypt=y+dy;
						
						// get best corresponding source patch from the NNF
						int xst,yst; double w;
						if (!upscaled) {
							if (xpt<0 || xpt>=nnf.input.W) continue;
							if (ypt<0 || ypt>=nnf.input.H) continue;
							xst=field[xpt][ypt][0];
							yst=field[xpt][ypt][1];
							w = MaskedImage.similarity[field[xpt][ypt][2]];
						} else {
							if (xpt<0 || xpt>=2*nnf.input.W) continue;
							if (ypt<0 || ypt>=2*nnf.input.H) continue;
							xst=2*field[xpt/2][ypt/2][0]+(xpt%2);
							yst=2*field[xpt/2][ypt/2][1]+(ypt%2);
							w = MaskedImage.similarity[field[xpt/2][ypt/2][2]];
						}
						
						// get pixel corresponding to (x,y) in the source patch
						int xs=xst-dx, ys=yst-dy;
						if (xs<0 || xs>=source.W) continue;
						if (ys<0 || ys>=source.H) continue;
						
						// add contribution of the source pixel
						if (source.isMasked(xs, ys)) continue;
						int red   = source.getSample(xs, ys, 0);
						int green = source.getSample(xs, ys, 1);
						int blue  = source.getSample(xs, ys, 2);
						histo[0][red]+=w;
						histo[1][green]+=w;
						histo[2][blue]+=w;
						wsum+=w;
					}
				}

				// no significant contribution : conserve the values from previous target
				if (wsum<1) continue;
				
				// **** MAXIMIZATION STEP ****
				
				// average the contributions of significant pixels (near the median) 
				double lowth=0.40*wsum;  // low threshold in the CDF
				double highth=0.60*wsum; // high threshold in the CDF
				for(int band=0;band<3;band++) {
					double cdf=0, contrib=0, wcontrib=0;
					for(int i=0;i<256;i++) {
						cdf+=histo[band][i];
						if (cdf<lowth) continue;
						contrib+=i*histo[band][i]; 
						wcontrib+=histo[band][i];
						if (cdf>highth) break;
					}
					int value = (int)(contrib/wcontrib);
					target.setSample(x, y, band, value);
				}
			}
		}
	}
	
}
