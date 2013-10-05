package com.davidchatting.patchmatch;

/**
 * ##library.name##
 * ##library.sentence##
 * ##library.url##
 *
 * Copyright ##copyright## ##author##
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General
 * Public License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307  USA
 * 
 * @author      ##author##
 * @modified    ##date##
 * @version     ##library.prettyVersion## (##library.version##)
 */

import java.awt.image.BufferedImage;

import processing.core.*;

import com.developpez.xphilipp.patchmatch.*;

public class PatchMatch extends Thread {
	public final static String VERSION = "##library.prettyVersion##";
	
	private PApplet parent=null;
	
	private BufferedImage result=null;
	private boolean running=false;
	
	private BufferedImage input=null;
	private boolean[][] mask=null;
	private int radius=2;
	
	public PatchMatch(PApplet p){
		super();
		parent=p;
		start();
	}
	
	public void start(){
        super.start();
        running=true;
    }
	
	public void run(){
		while(running){
			if(result==null && input!=null && mask!=null){
				result=new Inpaint().inpaint(input,mask,radius);
			}
			else{
				try { sleep(100); }
	            catch (Exception e) {}
			}
		}
    }
	
	public void quit(){
		running=false;
        interrupt();
    }
	
	public void patch(PImage i,PImage m,int r){
		patch(getBufferedImage(i),getBufferedImage(m),r);
	}
	
	public void patch(PImage i,PShape m,int r){
		patch(getBufferedImage(i),getBufferedImage(m,i.width,i.height),r);
	}

	public void patch(BufferedImage i,BufferedImage m,int r){
		patch(i,get2dArray(m),r);
	}
	
	public void patch(PImage i,boolean[][] m,int r){
		patch(getBufferedImage(i),m,r);
	}
	
	public void patch(BufferedImage i,boolean[][] m,int r){
		input=i;
		mask=m;
		this.radius=r;
		
		result=null;
	}
	
	public synchronized PImage getResultPImage() {
	    return(getPImage(result));
	}
	
	public synchronized BufferedImage getResultBufferedImage() {
	    return(result);
	}
	
	public synchronized boolean available(){
		return(result!=null);
	}
	
	public boolean[][] get2dArray(BufferedImage i){
		boolean[][] result=null;
		
		int w=i.getWidth(), h=i.getHeight();
		result = new boolean[w][h];
		for(int y=0;y<h;y++) {
			for(int x=0;x<w;x++) {
				result[x][y]=(i.getRGB(x, y)!=0xFF000000);
			}
		}
		
		return(result);
	}
	
	public BufferedImage getBufferedImage(PShape m, int w, int h) {
		return(getBufferedImage(getPImage(m,w,h)));
	}
	
	public PImage getPImage(PShape m,int w,int h) {
		PGraphics result=null;
		
		if(m!=null){
			result=parent.createGraphics(w,h,PGraphics.P2D);
			result.beginDraw();
			result.background(0);
			result.shape(m);
			result.endDraw();
		}
		
		return(result);
	}
	
	public BufferedImage getBufferedImage(PImage i){
		BufferedImage result=null;
		
		if(i!=null){
			result=new BufferedImage(i.width,i.height,BufferedImage.TYPE_INT_ARGB);
			i.loadPixels();
			result.setRGB(0,0,i.width,i.height,i.pixels,0,i.width);
		}
		
		return(result);
	}
	
	public PImage getPImage(BufferedImage i){
		PImage result=null;
		
		if(i!=null){
			result=new PImage(i.getWidth(),i.getHeight(),PConstants.ARGB);
			i.getRGB(0,0,result.width,result.height,result.pixels,0,result.width);
			result.updatePixels();
		}
		
		return(result);
	}
	
	/**
	 * return the version of the library.
	 * 
	 * @return String
	 */
	public static String version() {
		return VERSION;
	}
}