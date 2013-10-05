import com.davidchatting.patchmatch.*;

PatchMatch patchMatch=null;

PImage image=null;
PShape mask=null;

void setup() {
  patchMatch=new PatchMatch(this);
  
  image = loadImage("231718267_f4b6e87545_z.jpg");  //(cc) from: http://www.flickr.com/photos/fantasticalmonkey/231718267/sizes/z/
  size(image.width,image.height,P2D);
}

void draw() {
  background(0);
  if(patchMatch.available()){
    image(patchMatch.getResultPImage(),0,0,width,height);
  }
  else{
    image(image,0,0,width,height);
  }
}

void mousePressed() {
  mask=createShape();
  mask.beginShape();
  mask.vertex(mouseX,mouseY);
}

void mouseDragged() {
  mask.vertex(mouseX,mouseY);
}

void mouseReleased() {
  mask.vertex(mouseX,mouseY);
  mask.endShape(CLOSE);
  
  patchMatch.patch(image,mask,2);
}
