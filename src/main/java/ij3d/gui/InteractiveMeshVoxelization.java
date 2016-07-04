package ij3d.gui;

import customnode.CustomTriangleMesh;
import customnode.FullInfoMesh;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import java.awt.Button;
import java.awt.Label;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Vector;
import org.scijava.vecmath.Point3f;

public class InteractiveMeshVoxelization
{
  private static final int X = 0;
  private static final int Y = 1;
  private static final int Z = 2;
  
  public void run(final CustomTriangleMesh ctm)
  {
    FullInfoMesh fim = new FullInfoMesh(ctm.getMesh());
    
    final GenericDialog gd = new GenericDialog("Mesh voxelization")
    {
      public void keyPressed(KeyEvent e)
      {
        if (e.getKeyCode() != 10) {
          super.keyPressed(e);
        }
      }
    };
    gd.addNumericField("Width", 256.0D, 0);
    gd.addNumericField("Height", 256.0D, 0);
    gd.addNumericField("Depth", 256.0D, 0);
    
    final TextField tfWidth = (TextField)gd.getNumericFields().get(0);
    final TextField tfHeight = (TextField)gd.getNumericFields().get(1);
    final TextField tfDepth = (TextField)gd.getNumericFields().get(2);
    
    Label label = (Label)gd.getMessage();
    gd.enableYesNoCancel("Voxelize", "Save");
    
    gd.setModal(false);
    gd.showDialog();
    Button[] buttons = gd.getButtons();
    
    buttons[0].setLabel("Voxelize");
    buttons[0].removeActionListener(gd);
    
    buttons[0].addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        final int width = Integer.parseInt(tfWidth.getText());
        final int height = Integer.parseInt(tfHeight.getText());
        final int depth = Integer.parseInt(tfDepth.getText());
        gd.setEnabled(false);
        
        new Thread()
        {
          public void run()
          {
            InteractiveMeshVoxelization.this.voxelize(ctm, width, height, depth);
            gd.setEnabled(true);
          }
        }.start();
      }
    });
    buttons[1].setLabel("Ok");
    buttons[1].removeActionListener(gd);
    buttons[1].addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        gd.dispose();
      }
    });
  }
  
  public void voxelize(CustomTriangleMesh ctm, int width, int height, int depth)
  {
    ImagePlus imp = IJ.createImage(ctm.getName() + "_voxelization", width, height, depth, 8);
    ImageStack stack = imp.getStack();
    
    Point3f minVec = new Point3f();Point3f maxVec = new Point3f();Point3f centerVec = new Point3f();
    ctm.calculateMinMaxCenterPoint(minVec, maxVec, centerVec);
    Point3f dimVec = new Point3f(maxVec);
    dimVec.sub(minVec);
    
    float xStep = dimVec.x / width;
    float yStep = dimVec.y / height;
    float zStep = dimVec.z / depth;
    
    List mesh = ctm.getMesh();
    
    float[] voxelHalfsize = new float[3];
    voxelHalfsize[0] = (xStep / 2.0F);
    voxelHalfsize[1] = (yStep / 2.0F);
    voxelHalfsize[2] = (zStep / 2.0F);
    for (int triIdx = 0; triIdx < mesh.size(); triIdx += 3)
    {
      Point3f v1 = (Point3f)mesh.get(triIdx);Point3f v2 = (Point3f)mesh.get(triIdx + 1);Point3f v3 = (Point3f)mesh.get(triIdx + 2);
      v1.sub(minVec);
      v2.sub(minVec);
      v3.sub(minVec);
      
      float minX = Math.min(Math.min(v1.x, v2.x), v3.x);
      float maxX = Math.max(Math.max(v1.x, v2.x), v3.x);
      float minY = Math.min(Math.min(v1.y, v2.y), v3.y);
      float maxY = Math.max(Math.max(v1.y, v2.y), v3.y);
      float minZ = Math.min(Math.min(v1.z, v2.z), v3.z);
      float maxZ = Math.max(Math.max(v1.z, v2.z), v3.z);
      for (int bbX = (int)Math.floor(minX / xStep); bbX < Math.ceil(maxX / xStep); bbX++) {
        for (int bbY = (int)Math.floor(minY / yStep); bbY < Math.ceil(maxY / yStep); bbY++) {
          for (int bbZ = (int)Math.floor(minZ / zStep); bbZ < Math.ceil(maxZ / zStep); bbZ++) {
            if (stack.getVoxel(bbX, bbY, bbZ) == 0.0D)
            {
              float[] voxelCenter = new float[3];
              
              voxelCenter[0] = (bbX * xStep + voxelHalfsize[0]);
              voxelCenter[1] = (bbY * yStep + voxelHalfsize[1]);
              voxelCenter[2] = (bbZ * zStep + voxelHalfsize[2]);
              if (triBoxOverlap(voxelCenter, voxelHalfsize, v1, v2, v3) == 1) {
                stack.setVoxel(bbX, bbY, bbZ, 255.0D);
              }
            }
          }
        }
      }
    }
    imp.show();
  }
  
  private float findMin(float x0, float x1, float x2)
  {
    return Math.min(Math.min(x0, x1), x2);
  }
  
  private float findMax(float x0, float x1, float x2)
  {
    return Math.max(Math.max(x0, x1), x2);
  }
  
  private float dotArray(float[] v1, float[] v2)
  {
    return v1[0] * v2[0] + v1[1] * v2[1] + v1[2] * v2[2];
  }
  
  private int planeBoxOverlap(float[] normalArray, float[] vertArray, float[] maxboxArray)
  {
    float[] vminArray = new float[3];float[] vmaxArray = new float[3];
    for (int q = 0; q <= 2; q++)
    {
      float v = vertArray[q];
      if (normalArray[q] > 0.0F)
      {
        vminArray[q] = (-maxboxArray[q] - v);
        maxboxArray[q] -= v;
      }
      else
      {
        maxboxArray[q] -= v;
        vmaxArray[q] = (-maxboxArray[q] - v);
      }
    }
    if (dotArray(normalArray, vminArray) > 0.0F) {
      return 0;
    }
    if (dotArray(normalArray, vmaxArray) >= 0.0F) {
      return 1;
    }
    return 0;
  }
  
  private int axisTest_x01(float a, float b, float fa, float fb, float[] v0, float[] v1, float[] v2, float[] boxhalfsize)
  {
    float p0 = a * v0[1] - b * v0[2];
    float p2 = a * v2[1] - b * v2[2];
    float max;
    float min;
    
    if (p0 < p2)
    {
    	min = p0;max = p2;
    }
    else
    {
      min = p2;max = p0;
    }
    float rad = fa * boxhalfsize[1] + fb * boxhalfsize[2];
    if ((min > rad) || (max < -rad)) {
      return 0;
    }
    return 1;
  }
  
  private int axisTest_x2(float a, float b, float fa, float fb, float[] v0, float[] v1, float[] v2, float[] boxhalfsize)
  {
    float p0 = a * v0[1] - b * v0[2];
    float p1 = a * v1[1] - b * v1[2];
    float max;
    float min;
    
    if (p0 < p1)
    {
       min = p0;max = p1;
    }
    else
    {
      min = p1;max = p0;
    }
    float rad = fa * boxhalfsize[1] + fb * boxhalfsize[2];
    if ((min > rad) || (max < -rad)) {
      return 0;
    }
    return 1;
  }
  
  private int axisTest_y02(float a, float b, float fa, float fb, float[] v0, float[] v1, float[] v2, float[] boxhalfsize)
  {
    float p0 = -a * v0[0] + b * v0[2];
    float p2 = -a * v2[0] + b * v2[2];
    float max;
    float min;
    
    if (p0 < p2)
    {
       min = p0;max = p2;
    }
    else
    {
      min = p2;max = p0;
    }
    float rad = fa * boxhalfsize[0] + fb * boxhalfsize[2];
    if ((min > rad) || (max < -rad)) {
      return 0;
    }
    return 1;
  }
  
  private int axisTest_y1(float a, float b, float fa, float fb, float[] v0, float[] v1, float[] v2, float[] boxhalfsize)
  {
    float p0 = -a * v0[0] + b * v0[2];
    float p1 = -a * v1[0] + b * v1[2];
    float max;
    float min;
    
    if (p0 < p1)
    {
       min = p0;max = p1;
    }
    else
    {
      min = p1;max = p0;
    }
    float rad = fa * boxhalfsize[0] + fb * boxhalfsize[2];
    if ((min > rad) || (max < -rad)) {
      return 0;
    }
    return 1;
  }
  
  private int axisTest_z12(float a, float b, float fa, float fb, float[] v0, float[] v1, float[] v2, float[] boxhalfsize)
  {
    float p1 = a * v1[0] - b * v1[1];
    float p2 = a * v2[0] - b * v2[1];
    float max;
    float min;
    
    if (p2 < p1)
    {
       min = p2;max = p1;
    }
    else
    {
      min = p1;max = p2;
    }
    float rad = fa * boxhalfsize[0] + fb * boxhalfsize[1];
    if ((min > rad) || (max < -rad)) {
      return 0;
    }
    return 1;
  }
  
  private int axisTest_z0(float a, float b, float fa, float fb, float[] v0, float[] v1, float[] v2, float[] boxhalfsize)
  {
    float p0 = a * v0[0] - b * v0[1];
    float p1 = a * v1[0] - b * v1[1];
    float max;
    float min;
    
    if (p0 < p1)
    {
       min = p0;max = p1;
    }
    else
    {
      min = p1;max = p0;
    }
    float rad = fa * boxhalfsize[0] + fb * boxhalfsize[1];
    if ((min > rad) || (max < -rad)) {
      return 0;
    }
    return 1;
  }
  
  private void sub(float[] result, float[] v1, float[] v2)
  {
    v1[0] -= v2[0];
    v1[1] -= v2[1];
    v1[2] -= v2[2];
  }
  
  private void cross(float[] dest, float[] v1, float[] v2)
  {
    dest[0] = (v1[1] * v2[2] - v1[2] * v2[1]);
    dest[1] = (v1[2] * v2[0] - v1[0] * v2[2]);
    dest[2] = (v1[0] * v2[1] - v1[1] * v2[0]);
  }
  
  private int triBoxOverlap(float[] boxcenter, float[] boxhalfsize, Point3f pf1, Point3f pf2, Point3f pf3)
  {
    float[] vert1 = new float[3];float[] vert2 = new float[3];float[] vert3 = new float[3];
    
    pf1.get(vert1);
    pf2.get(vert2);
    pf3.get(vert3);
    
    float[] v0 = new float[3];float[] v1 = new float[3];float[] v2 = new float[3];
    
    float[] normal = new float[3];float[] e0 = new float[3];float[] e1 = new float[3];float[] e2 = new float[3];
    
    sub(v0, vert1, boxcenter);
    sub(v1, vert2, boxcenter);
    sub(v2, vert3, boxcenter);
    
    sub(e0, v1, v0);
    sub(e1, v2, v1);
    sub(e2, v0, v2);
    
    float fex = Math.abs(e0[0]);
    float fey = Math.abs(e0[1]);
    float fez = Math.abs(e0[2]);
    
    axisTest_x01(e0[2], e0[1], fez, fey, v0, v1, v2, boxhalfsize);
    axisTest_y02(e0[2], e0[0], fez, fex, v0, v1, v2, boxhalfsize);
    axisTest_z12(e0[1], e0[0], fey, fex, v0, v1, v2, boxhalfsize);
    
    fex = Math.abs(e1[0]);
    fey = Math.abs(e1[1]);
    fez = Math.abs(e1[2]);
    
    axisTest_x01(e1[2], e1[1], fez, fey, v0, v1, v2, boxhalfsize);
    axisTest_y02(e1[2], e1[0], fez, fex, v0, v1, v2, boxhalfsize);
    axisTest_z0(e1[1], e1[0], fey, fex, v0, v1, v2, boxhalfsize);
    
    fex = Math.abs(e2[0]);
    fey = Math.abs(e2[1]);
    fez = Math.abs(e2[2]);
    
    axisTest_x2(e2[2], e2[1], fez, fey, v0, v1, v2, boxhalfsize);
    axisTest_y1(e2[2], e2[0], fez, fex, v0, v1, v2, boxhalfsize);
    axisTest_z12(e2[1], e2[0], fey, fex, v0, v1, v2, boxhalfsize);
    
    float min = findMin(v0[0], v1[0], v2[0]);
    float max = findMax(v0[0], v1[0], v2[0]);
    if ((min > boxhalfsize[0]) || (max < -boxhalfsize[0])) {
      return 0;
    }
    min = findMin(v0[1], v1[1], v2[1]);
    max = findMax(v0[1], v1[1], v2[1]);
    if ((min > boxhalfsize[1]) || (max < -boxhalfsize[1])) {
      return 0;
    }
    min = findMin(v0[2], v1[2], v2[2]);
    max = findMax(v0[2], v1[2], v2[2]);
    if ((min > boxhalfsize[2]) || (max < -boxhalfsize[2])) {
      return 0;
    }
    cross(normal, e0, e1);
    if (planeBoxOverlap(normal, v0, boxhalfsize) != 1) {
      return 0;
    }
    return 1;
  }
}
