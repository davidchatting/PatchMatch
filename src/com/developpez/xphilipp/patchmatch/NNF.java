package com.developpez.xphilipp.patchmatch;

import java.util.Random;

/**
 * Nearest-Neighbor Field (see PatchMatch algorithm) 
 * 
 * @author Xavier Philippeau
 *
 */
public class NNF {
	
	// image 
	MaskedImage input, output;
	
	//  patch size
	int S;

	// Nearest-Neighbor Field 1 pixel = { x_target, y_target, distance_scaled } 
	int[][][] field;
	
	// random generator
	Random random = new Random(0);

	// constructor
	public NNF(MaskedImage input, MaskedImage output, int patchsize) {
		this.input = input;
		this.output= output;
		this.S = patchsize;	
	}
	
	// initialize field with random values
	public void randomize() {
		// field
		this.field = new int[input.W][input.H][3];
		
		for(int y=0;y<input.H;y++) {
			for(int x=0;x<input.W;x++) {
				field[x][y][0] = random.nextInt(output.W);  
				field[x][y][1] = random.nextInt(output.H);
				field[x][y][2] = MaskedImage.DSCALE;
			}
		}
		initialize();
	}
	
	// initialize field from an existing (possibily smaller) NNF
	public void initialize(NNF nnf) {
		// field
		this.field = new int[input.W][input.H][3];
		
		int fx = input.W/nnf.input.W;
		int fy = input.H/nnf.input.H;
		//System.out.println("nnf upscale by "+fx+"x"+fy+" : "+nnf.input.W+","+nnf.input.H+" -> "+input.W+","+input.H);
		for(int y=0;y<input.H;y++) {
			for(int x=0;x<input.W;x++) {
				int xlow = Math.min(x/fx, nnf.input.W-1);
				int ylow = Math.min(y/fy, nnf.input.H-1);
				field[x][y][0] = nnf.field[xlow][ylow][0]*fx;  
				field[x][y][1] = nnf.field[xlow][ylow][1]*fy;
				field[x][y][2] = MaskedImage.DSCALE;
			}
		}
		initialize();
	}
	
	// compute initial value of the distance term
	private void initialize() {
		for(int y=0;y<input.H;y++) {
			for(int x=0;x<input.W;x++) {
				field[x][y][2] = distance(x,y,  field[x][y][0],field[x][y][1]);

				// if the distance is INFINITY (all pixels masked ?), try to find a better link
				int iter=0, maxretry=20;
				while( field[x][y][2] == MaskedImage.DSCALE && iter<maxretry) {
					field[x][y][0] = random.nextInt(output.W);
					field[x][y][1] = random.nextInt(output.H);
					field[x][y][2] = distance(x,y,  field[x][y][0],field[x][y][1]);
					iter++;
				}
			}
		}
	}
	
	// multi-pass NN-field minimization (see "PatchMatch" - page 4)
	public void minimize(int pass) {
		
		int min_x=0, min_y=0, max_x=input.W-1, max_y=input.H-1;
		
		// multi-pass minimization
		for(int i=0;i<pass;i++) {
			System.out.print(".");
			
			// scanline order
			for(int y=min_y;y<max_y;y++)
				for(int x=min_x;x<=max_x;x++)
					if (field[x][y][2]>0) minimizeLink(x,y,+1);

			// reverse scanline order
			for(int y=max_y;y>=min_y;y--)
				for(int x=max_x;x>=min_x;x--)
					if (field[x][y][2]>0) minimizeLink(x,y,-1);
		}
	}

	// minimize a single link (see "PatchMatch" - page 4)
	public void minimizeLink(int x, int y, int dir) {
		int xp,yp,dp;
		
		//Propagation Left/Right
		if (x-dir>0 && x-dir<input.W) {
			xp = field[x-dir][y][0]+dir;
			yp = field[x-dir][y][1];
			dp = distance(x,y, xp,yp);
			if (dp<field[x][y][2]) {
				field[x][y][0] = xp;
				field[x][y][1] = yp;
				field[x][y][2] = dp;
			}
		}
		
		//Propagation Up/Down
		if (y-dir>0 && y-dir<input.H) {
			xp = field[x][y-dir][0];
			yp = field[x][y-dir][1]+dir;
			dp = distance(x,y, xp,yp);
			if (dp<field[x][y][2]) {
				field[x][y][0] = xp;
				field[x][y][1] = yp;
				field[x][y][2] = dp;
			}
		}
		
		//Random search
		int wi=output.W, xpi=field[x][y][0], ypi=field[x][y][1];
		while(wi>0) {
			xp = xpi + random.nextInt(2*wi)-wi;
			yp = ypi + random.nextInt(2*wi)-wi;
			xp = Math.max(0, Math.min(output.W-1, xp ));
			yp = Math.max(0, Math.min(output.H-1, yp ));
			
			dp = distance(x,y, xp,yp);
			if (dp<field[x][y][2]) {
				field[x][y][0] = xp;
				field[x][y][1] = yp;
				field[x][y][2] = dp;
			}
			wi/=2;
		}
	}

	// compute distance between two patch 
	public int distance(int x,int y, int xp,int yp) {
		return MaskedImage.distance(input,x,y, output,xp,yp, S);
	}
	
	public int[][][] getField() {
		return field;
	}
	
}
