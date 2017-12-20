/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jncregridder.util;

/**
 *
 * @author raffaelemontella
 */
public class Linear2DInterpolator extends InterpolatorBase {
    
    private double srcMinLon=0;
    private double srcMinLat=0;
    private double srcLatDelta, srcLonDelta;
    private double srcLonStep, srcLatStep;
    
    public Linear2DInterpolator(double[][] srcLAT, double[][] srcLON, double[][] dstLAT, double[][] dstLON, int[][] srcMASK,int[][] dstMASK) throws InterpolatorException {
        init(srcLAT,srcLON,dstLAT,dstLON,srcMASK,dstMASK);   
        
        srcMinLon = srcLON[0][0];
        srcMinLat = srcLAT[0][0];
        srcMaxLon = srcLON[srcLAT.length-1][srcLON[0].length-1];
        srcMaxLat = srcLAT[srcLAT.length-1][srcLON[0].length-1];
        srcLatDelta = srcMaxLat - srcMinLat;
        srcLonDelta = srcMinLon - srcMinLon;
        srcLatStep = srcLAT[1][0]-srcMinLat;
        srcLonStep = srcLON[0][1]-srcMinLon;
    }
    
    public Linear2DInterpolator(double[][] srcLAT, double[][] srcLON, double[][] dstLAT, double[][] dstLON, int[][] srcMASK,int[][] dstMASK, InterpolatorParams param) throws InterpolatorException {
        init(srcLAT,srcLON,dstLAT,dstLON,srcMASK,dstMASK); 
        
        srcMinLon = srcLON[0][0];
        srcMinLat = srcLAT[0][0];
        srcMaxLon = srcLON[srcLAT.length-1][srcLON[0].length-1];
        srcMaxLat = srcLAT[srcLAT.length-1][srcLON[0].length-1];
        srcLatDelta = srcMaxLat - srcMinLat;
        srcLonDelta = srcMaxLon - srcMinLon;
        srcLatStep = srcLAT[1][0]-srcMinLat;
        srcLonStep = srcLON[0][1]-srcMinLon;
    }

    @Override
    public double[][] interp(double[][] src, double srcMissingValue, double dstMissingValue, double[] params) throws InterpolatorException {
        
        double[][] dst = new double[dstLAT.length][dstLON[0].length];
        for (int dstJ=0;dstJ<dstLAT.length;dstJ++) {
            for (int dstI=0;dstI<dstLON[0].length;dstI++) {
                double dstLon = dstLON[dstJ][dstI];
                double dstLat = dstLAT[dstJ][dstI];
                
                double srcII =(dstLon-srcMinLon)/srcLonStep;
                double srcJJ =(dstLat-srcMinLat)/srcLatStep;
                
                int[] II = { (int)Math.floor(srcII),(int)Math.ceil(srcII) };
                int[] JJ = { (int)Math.floor(srcJJ),(int)Math.ceil(srcJJ) };
                
                double[] q = new double[4];
                q[0]=src[JJ[0]][II[0]];
                q[1]=src[JJ[0]][II[1]];
                q[2]=src[JJ[1]][II[0]];
                q[3]=src[JJ[1]][II[1]];
                
                double[] w = new double[4];
                w[0]=((srcLON[JJ[1]][II[1]]-dstLon)*(srcLAT[JJ[1]][II[1]]-dstLat))/(srcLonStep*srcLatStep);
                w[1]=((dstLon-srcLON[JJ[0]][II[0]])*(srcLAT[JJ[1]][II[1]]-dstLat))/(srcLonStep*srcLatStep);
                w[2]=((srcLON[JJ[1]][II[1]]-dstLon)*(dstLat-srcLAT[JJ[0]][II[0]]))/(srcLonStep*srcLatStep);
                w[3]=((dstLon-srcLON[JJ[0]][II[0]])*(dstLat-srcLAT[JJ[0]][II[0]]))/(srcLonStep*srcLatStep);
                double wS=0;
                for (int l=0;l<4;l++) wS+=w[l];
                if (wS<1-1e-8 || wS>1+1e-1) throw new InterpolatorException("Bad weights! "+wS);
                
                dst[dstJ][dstI]=0;
                for (int l=0;l<4;l++){
                    dst[dstJ][dstI]=dst[dstJ][dstI]+q[l]*w[l];
                }
                
            }
        }
        return dst;
    }
    
}
