/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jncregridder.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 *
 * @author raffaelemontella
 */
public class GridPolygons {
    double c=10000;
    private GridPolygon[][] grid = null;
    private int snDim;
    private int weDim;
    private double[][] LAT = null;
    private double[][] LONG= null;
    private double rmax,r2max;
    
    public double getRadiusMax() { return rmax; }
    public double getRadiusMax2() { return r2max; }
    public GridPolygon get(int b, int a) { return grid[b][a]; } 
    
    public GridPolygons(int snDim, int weDim, double[][] LAT, double[][] LONG) {
        
        this.snDim = snDim;
        this.weDim = weDim;
        this.LAT = LAT;
        this.LONG = LONG;
        
        grid = new GridPolygon[snDim-1][weDim-1];
        
        for (int j=0;j<snDim-1;j++) {
            for (int i=0;i<weDim-1;i++) {
                
                double[] xpoints = new double[] { LONG[j][i],LONG[j+1][i],LONG[j+1][i+1],LONG[j][i+1] };
                double[] ypoints = new double[] { LAT[j][i], LAT[j+1][i], LAT[j+1][i+1],LAT[j][i+1] };
                
                
                GridPolygon p = new GridPolygon(xpoints,ypoints,4,j,i);
                
                grid[j][i]=p;
            }
        }
        
        double rc=0, r2c=0;
        for (int j=0;j<snDim-1;j++) {
            for (int i=0;i<weDim-1;i++) {
                rc=(grid[j][i].getRadius());
                r2c=(grid[j][i].getRadius2());
                if (j==0 && i==0) {
                    rmax=rc;
                    r2max=r2c;
                } else {
                    rmax=Math.max(rc, rmax);
                    r2max=Math.max(r2c, r2max);
                } 
            }
        }
    }
    
    public void saveAsCSV(String fileName) throws IOException {
        FileWriter fw = new FileWriter(fileName);
        PrintWriter pw = new PrintWriter(fw);
        
        for (int j=0;j<snDim-1;j++) {
            for (int i=0;i<weDim-1;i++) {
                GridPolygon p = grid[j][i];
                String out=p.getXC()+","+p.getYC()+","+j+","+i+","+p.getRadius2();
                pw.println(out);
            }
        }
    }
    
    private int[] search(int j1, int i1, int j2, int i2, double xLon, double xLat, double filter) {
        int[] result = null;
        
        
        double xLonM = xLon*c;
        double xLatM = xLat*c;
        
        int jj=j1+(j2-j1)/2;
        int ii=i1+(i2-i1)/2;
        int[][] QI = new int[4][];
        Polygon2D[] Q = new Polygon2D[4]; 
        Q[0] = new Polygon2D(
            new double[] { LONG[j1][i1]*c, LONG[jj][i1]*c, LONG[jj][ii]*c, LONG[j1][ii]*c },
            new double[] { LAT[j1][i1]*c, LAT[jj][i1]*c, LAT[jj][ii]*c, LAT[j1][ii]*c },
            4);
        QI[0] = new int[] { j1, i1, jj, ii };
        
        Q[1] = new Polygon2D(
            new double[] { LONG[jj][i1]*c, LONG[j2][i1]*c, LONG[j2][ii]*c, LONG[jj][ii]*c },
            new double[] { LAT[jj][i1]*c, LAT[j2][i1]*c, LAT[j2][ii]*c, LAT[jj][ii]*c },
            4);
        QI[1] = new int[] { jj, i1, j2, ii };
        
        Q[2] = new Polygon2D(
            new double[] { LONG[jj][ii]*c, LONG[j2][ii]*c, LONG[j2][i2]*c, LONG[jj][i2]*c },
            new double[] { LAT[jj][ii]*c, LAT[j2][ii]*c, LAT[j2][i2]*c, LAT[jj][i2]*c },
            4);
        QI[2] = new int[] { jj, ii, j2, i2 };
        
        Q[3] = new Polygon2D(
            new double[] { LONG[j1][ii]*c, LONG[jj][ii]*c, LONG[jj][i2]*c, LONG[j1][i2]*c },
            new double[] { LAT[j1][ii]*c, LAT[jj][ii]*c, LAT[jj][i2]*c, LAT[j1][i2]*c },
            4);
        QI[3] = new int[] { j1, ii, jj, i2 };
        
        for (int idx=0;idx<4;idx++) {
            if (Q[idx].contains(xLonM, xLatM)) {
                int jjj = QI[idx][0]+(QI[idx][2]-QI[idx][0])/2;
                int iii = QI[idx][1]+(QI[idx][3]-QI[idx][1])/2;
                
                
                double xc = grid[jjj][iii].getXC();
                double yc = grid[jjj][iii].getYC();

                double dd2=(xLon-xc)*(xLon-xc)+(xLat-yc)*(xLat-yc);

                if (dd2<filter) {
                    result = new int[] {jjj, iii};
                } else {
                    result = search(QI[idx][0],QI[idx][1],QI[idx][2],QI[idx][3],xLon,xLat, filter);
                }
                break;
            }
        }
        return result;
    }    
    
    public int[] calculateSearchRange(double xLon, double xLat, double filter, int span, int method) throws NCRegridderException {
        
        int bMax = snDim-1;
        int aMax = weDim-1;
        int aMin = 0;
        int bMin = 0;
        
        if (method>0) {
            
            int jjC=-1,iiC=-1;

            switch (method) {

                case 1:
                    double dd=0;
                    for (int jj=0;jj<snDim-1;jj++) {
                        for (int ii=0;ii<weDim-1;ii++) {
                            double xc = grid[jj][ii].getXC();
                            double yc = grid[jj][ii].getYC();

                            dd=Math.pow((xLon-xc)*(xLon-xc)+(xLat-yc)*(xLat-yc),.5);

                            if (dd<filter) {
                                iiC = ii;
                                jjC = jj;
                                ii=weDim-1;
                                jj=snDim-1;
                            }
                        }
                    }

                    bMin = jjC-span;
                    bMax = jjC+span;
                    aMin = iiC-span;
                    aMax = iiC+span;

                break;

                case 2:
                    double dd2=0;
                    for (int jj=0;jj<snDim-1;jj++) {
                        for (int ii=0;ii<weDim-1;ii++) {
                            double xc = grid[jj][ii].getXC();
                            double yc = grid[jj][ii].getYC();

                            dd2=(xLon-xc)*(xLon-xc)+(xLat-yc)*(xLat-yc);

                            if (dd2<filter) {
                                iiC = ii;
                                jjC = jj;
                                ii=weDim-1;
                                jj=snDim-1;
                            }
                        }
                    }

                    bMin = jjC-span;
                    bMax = jjC+span;
                    aMin = iiC-span;
                    aMax = iiC+span;

                break;

                case 3:
                    int[] jjiiC = search(0, 0, snDim-1, weDim-1, xLon, xLat, filter);

                    if (jjiiC!=null) {
                        jjC = jjiiC[0];
                        iiC = jjiiC[1];
                        bMin = jjC-span;
                        bMax = jjC+span;
                        aMin = iiC-span;
                        aMax = iiC+span;
                    }
                break;


            }
            
            if (jjC==-1 || iiC==-1) {
                throw new NCRegridderException("("+xLat+","+xLon+" filter="+filter+" span="+span+"). Minimizing centroid not found!");
            }

            // System.out.println("Centroid:"+jjC+","+iiC);


            if (bMin<0) bMin=0;
            if (aMin<0) aMin=0;
            if (bMax>snDim-1) bMax=snDim-1;
            if (aMax>weDim-1) aMax=weDim-1;
        }

        return new int[] {aMin,aMax,bMin,bMax};
    }
}
