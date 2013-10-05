package com.developpez.xphilipp.patchmatch;

import java.awt.Graphics2D;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

public class Demo {
	
	// display widget
	static JLabel jlabel;

	public static void display(BufferedImage bimg) {
		if (jlabel==null) {
			int H = bimg.getHeight();
			int W = bimg.getWidth();

			JFrame frame = new JFrame();
			frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			JPanel panneau = new JPanel();
			JLabel label0 = new JLabel();
			panneau.add(label0);
			JScrollPane scrollPane = new JScrollPane(panneau);
			frame.getContentPane().add(scrollPane);
			frame.setSize(W + 32, H + 64);
			frame.setVisible(true);
			label0.setIcon(new ImageIcon());
			
			jlabel = label0;
		}
		
		((ImageIcon)jlabel.getIcon()).setImage(bimg);
		jlabel.repaint();
	}
	
	public static BufferedImage loadImage(String filename) {
		BufferedImage image;
		try {
			BufferedImage input = ImageIO.read( new File(filename) );
			// convert to RGB format
			image = new BufferedImage(input.getWidth(),input.getHeight(),ColorSpace.TYPE_RGB);
	        ((Graphics2D) image.getGraphics()).drawImage(input,0,0,null);
		} catch (Exception e) {
			throw new RuntimeException("Error loading Image file '"+filename+"' : "+e.getMessage());
		}
		return image;
	}
	
	public static void main(String[] args) {

		BufferedImage input = loadImage("8744728400_2221f2daa3_z.jpg");
		BufferedImage maskimage = loadImage("8744728400_2221f2daa3_z-mask.png");

		// generate mask array from mask image
		int W=maskimage.getWidth(), H=maskimage.getHeight();
		boolean[][] mask = new boolean[W][H];
		for(int y=0;y<H;y++) 
			for(int x=0;x<W;x++) 
				mask[x][y]=(maskimage.getRGB(x, y)!=0xFF000000);
		
		// overwrite image, to see the mask in RED
		W=input.getWidth(); H=input.getHeight();
		for(int y=0;y<H;y++) 
			for(int x=0;x<W;x++)
				if (mask[x][y]) input.setRGB(x, y, 0xFFFF0000);	;
	
		display(input);
		BufferedImage output = new Inpaint().inpaint(input, mask, 2);
		display(output);
		
		System.out.println("\nDONE.");
	}
}
