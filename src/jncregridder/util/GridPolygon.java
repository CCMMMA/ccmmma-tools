/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jncregridder.util;

import java.awt.geom.Point2D;

/**
 *
 * @author raffaelemontella
 */
class GridPolygon extends Polygon2D  {
    public int i;
    public int j;
    
    private double xc;
    private double yc;
    private double r;
    private double r2;
    
    
    
    private double getArea() {
        
        int idx=0,jdx=0;
        double area=0;
        
        
        for (idx=0;idx<npoints;idx++) {
		jdx = (idx + 1) % npoints;
		area += xpoints[idx] * ypoints[jdx];
		area -= ypoints[idx] * xpoints[jdx];
	}
	area /= 2.0;
        return area;
    }
    
    double[] getCentroids() {
        int idx=0,jdx=0;
        
        double cx=0;
        double cy=0;
        double factor=0;
	for (idx=0;idx<npoints;idx++) {
		jdx = (idx + 1) % npoints;
		factor=(xpoints[idx]*ypoints[jdx]-xpoints[jdx]*ypoints[idx]);
		cx+=(xpoints[idx]+xpoints[jdx])*factor;
		cy+=(ypoints[idx]+ypoints[jdx])*factor;
	}
	factor=1/(getArea()*6.0);
	cx*=factor;
	cy*=factor;
        return new double[] { cx, cy };
    }
    
    
    private void computeRadius() {
    
        double[] cxy = getCentroids();
        xc = cxy[0];
        yc = cxy[1];
        
        double rC,r2C;
        for (int idx=0;idx<npoints;idx++) {
            r2C = (xc-xpoints[idx])*(xc-xpoints[idx])+ (yc-ypoints[idx])*(yc-ypoints[idx]);
            rC = Math.pow(r2C,.5);
            
            if (idx==0) {
                r = rC;
                r2 = r2C;
            } else {
                r = Math.max(r, rC);
                r2 = Math.max(r2, r2C);
                
            }
        }
        
    }
    
    public GridPolygon(double[] xpoints,double[] ypoints, int n, int j, int i) {
        super(xpoints,ypoints,n);
        
        this.i = i;
        this.j = j;
        
        
        computeRadius();
        
    }

    public double getRadius2() { return r2; }
    public double getRadius() { return r; }
    
    public double getXC() { return xc; }
    public double getYC() { return yc; }
}
