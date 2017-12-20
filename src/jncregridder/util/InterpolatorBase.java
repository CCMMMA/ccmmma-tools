/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jncregridder.util;

/**
 *
 * @author raffaelemontella
 */
public abstract class InterpolatorBase {
    
    protected int srcSNDim = -1;
    protected int srcWEDim = -1;
    protected int dstSNDim = -1;
    protected int dstWEDim = -1;
    
    protected double[][] srcLAT=null;
    protected double[][] srcLON=null;
    protected double[][] dstLAT=null;
    protected double[][] dstLON=null;
    
    protected int[][] srcMASK=null;
    protected int[][] dstMASK=null;
    
    public int getDstSNDim() { return dstSNDim; }
    public int getDstWEDim() { return dstWEDim; }
    public double[][] getDstLAT() { return dstLAT; }
    public double[][] getDstLON() { return dstLON; }
    public int[][] getDstMASK() { return dstMASK; }
    
    public int getSrcSNDim() { return srcSNDim; }
    public int getSrcWEDim() { return srcWEDim; }
    public double[][] getSrcLAT() { return srcLAT; }
    public double[][] getSrcLON() { return srcLON; }
    public int[][] getSrcMASK() { return srcMASK; }
    
    protected double minLon=Double.NaN,maxLon=Double.NaN,minLat=Double.NaN,maxLat=Double.NaN;
    protected double srcMinLon=Double.NaN,srcMaxLon=Double.NaN,srcMinLat=Double.NaN,srcMaxLat=Double.NaN;
    
    protected void init(double[][] srcLAT, double[][] srcLON, double[][] dstLAT, double[][] dstLON, int[][] srcMASK,int[][] dstMASK) throws InterpolatorException {
        if (srcLAT==null) throw new InterpolatorException("Source grid latitudes are null!");
        if (srcLON==null) throw new InterpolatorException("Source grid lonfitudes are null!");
        if (dstLAT==null) throw new InterpolatorException("Destination grid latitudes are null!");
        if (dstLON==null) throw new InterpolatorException("Destination grid lonfitudes are null!");
        
        if (srcMASK==null) {
            srcMASK = new int[srcLAT.length][srcLON[0].length];
            for (int j=0;j<srcLAT.length;j++) {
                for (int i=0;i<srcLON[0].length;i++) {
                    srcMASK[j][i]=1;
                }
            }
        }
        
        if (dstMASK==null) {
            dstMASK = new int[dstLAT.length][dstLON[0].length];
            for (int j=0;j<dstLAT.length;j++) {
                for (int i=0;i<dstLON[0].length;i++) {
                    dstMASK[j][i]=1;
                }
            }
        }
        
        this.srcLAT = srcLAT;
        this.srcLON = srcLON;
        this.dstLAT = dstLAT;
        this.dstLON = dstLON;
        
        this.srcMASK = srcMASK;
        this.dstMASK = dstMASK;
        
        srcSNDim = srcLAT.length;
        srcWEDim = srcLAT[0].length;
        
        dstSNDim = dstLAT.length;
        dstWEDim = dstLAT[0].length;
        
        for (int j=0;j<dstSNDim;j++) {
            for (int i=0;i<dstWEDim;i++) {
                if (dstMASK[j][i]==1) {
                    if (Double.isNaN(minLon)==true ) {
                        minLon=dstLON[j][i];
                    } else if (dstLON[j][i]<minLon) {
                        minLon=dstLON[j][i];
                    }
             
                    if(Double.isNaN(minLat)==true ) {
                        minLat=dstLAT[j][i];
                    } else if (dstLAT[j][i]<minLat) {
                        minLat=dstLAT[j][i];
                    }
             
                    /************/
             
                    if (Double.isNaN(maxLon)==true ) {
                        maxLon=dstLON[j][i];
                    } else if (dstLON[j][i]>maxLon) {
                        maxLon=dstLON[j][i];
                    }
             
                    if (Double.isNaN(maxLat)==true ) {
                        maxLat=dstLAT[j][i];
                    } else if (dstLAT[j][i]>maxLat) {
                        maxLat=dstLAT[j][i];
                    }
                }
            }
        }
        
        for (int j=0;j<srcSNDim;j++) {
            for (int i=0;i<srcWEDim;i++) {
                if (srcMASK[j][i]==1) {
                    if (Double.isNaN(srcMinLon)==true ) {
                        srcMinLon=srcLON[j][i];
                    } else if (srcLON[j][i]<srcMinLon) {
                        srcMinLon=srcLON[j][i];
                    }
             
                    if(Double.isNaN(srcMinLat)==true ) {
                        srcMinLat=srcLAT[j][i];
                    } else if (srcLAT[j][i]<srcMinLat) {
                        srcMinLat=srcLAT[j][i];
                    }
             
                    /************/
             
                    if (Double.isNaN(srcMaxLon)==true ) {
                        srcMaxLon=srcLON[j][i];
                    } else if (srcLON[j][i]>srcMaxLon) {
                        srcMaxLon=srcLON[j][i];
                    }
             
                    if (Double.isNaN(srcMaxLat)==true ) {
                        srcMaxLat=srcLAT[j][i];
                    } else if (srcLAT[j][i]>srcMaxLat) {
                        srcMaxLat=srcLAT[j][i];
                    }
                }
            }
        }
        
    }
    
    public abstract double[][] interp(double[][] src, double srcMissingValue,double dstMissingValue, double[] params) throws InterpolatorException;
    
}
