/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jncregridder.util;

/**
 *
 * @author raffaelemontella
 */
public abstract class Interpolator3DBase extends InterpolatorBase {
    
    protected int srcDepthDim = -1;
    
    protected int dstDepthDim = -1;
    
    
    protected double[][][] srcZ=null;
    
    protected double[][][] dstZ=null;
    
    
    
    
    
    
    protected double dstMinZ=0;
    
    public abstract double[][][] interp(double[][][] src,double srcMissingValue, double dstMissingValue, double[] params) throws InterpolatorException;
    
    
    protected void init(double[][] srcLAT, double[][] srcLON, double[][][] srcZ, double[][] dstLAT, double[][] dstLON, double[][][] dstZ, int[][] srcMASK, int[][] dstMASK) throws InterpolatorException {
        init(srcLAT, srcLON,dstLAT, dstLON, srcMASK, dstMASK);
        
        this.srcZ = srcZ;
        this.dstZ = dstZ;
        
        
        srcDepthDim = srcZ.length;
        
        dstDepthDim = dstZ.length;
        
        
        // Search the minimum value of Z in the destination grid
        for (int k=0;k<dstZ.length;k++) {
            for (int j=0;j<dstSNDim;j++) {
                for (int i=0;i<dstWEDim;i++) {
                    if (dstMASK[j][i]==1) {
                        if (dstZ[k][j][i]<dstMinZ) dstMinZ=dstZ[k][j][i];
                    }
                }
            }
        }
    }
    
}
