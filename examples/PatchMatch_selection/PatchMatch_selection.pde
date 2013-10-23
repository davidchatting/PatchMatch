//David Chatting
//===
//david@davidchatting.com
//23rd October 2013
//===

import com.davidchatting.patchmatch.*;

PatchMatch patchMatch=null;

PImage image=null;
PShape mask=null;
int x=0,y=0,w=0,h=0;

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
  
  if(mousePressed || !patchMatch.available()){
    showSelectionBox();
  }
}

void showSelectionBox(){
  noFill();
  stroke(0,128);
  strokeWeight(0.5f);
  rect(x,y,w,h);
  stroke(255,128);
  rect(x+1,y+1,w,h);
}

void mousePressed() {
  x=mouseX;
  y=mouseY;
  w=0;
  h=0;
}

void mouseDragged() {
  w=mouseX-x;
  h=mouseY-y;
}

void mouseReleased() {
  if((w*h)!=0){
    mask=createShape(RECT,x,y,w,h);
    patchMatch.patch(image,mask,2);
  }
}
