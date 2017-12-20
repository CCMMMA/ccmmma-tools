/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jncregridder.util;

import java.io.FileNotFoundException;
import java.io.Serializable;



class Weight3D implements Serializable {
    public double[] w = new double[2];
    public int[] KK = new int[2];
    public boolean masked=false;
}


/**
 *
 * @author raffaelemontella
 */
public class CustomInterpolator3D {
    
    private CustomInterpolator interpolator=null;
    private Weight3D[][][] weight3Ds = null;
    double[][][] dstDEPTH = null;
    double[][][] srcDEPTH = null;
    
    public CustomInterpolator3D(CustomInterpolator interpolator, double[][][]srcDEPTH, double[][][] dstDEPTH) {
        this.interpolator = interpolator;
        this.dstDEPTH = dstDEPTH;
        this.srcDEPTH = srcDEPTH;
        
        prepare();
    }
    
    public void prepare() {
        int dstLevs = dstDEPTH.length;
        int srcLevs = srcDEPTH.length;
        
        weight3Ds = new Weight3D[dstLevs][interpolator.dstSNDim][interpolator.dstWEDim];
        for (int dstK=0;dstK<dstLevs;dstK++) {
            System.out.println("k="+dstK);
            for (int dstJ=0;dstJ<interpolator.dstSNDim;dstJ++) {
                for (int dstI=0;dstI<interpolator.dstWEDim;dstI++) {
                    double dstZ = Math.abs(dstDEPTH[dstK][dstJ][dstI]);
                    

                    int srcK=0;
                    double srcZ=0;

                    while ((srcZ=Math.abs(srcDEPTH[srcK][0][0]))<dstZ && srcK<srcLevs) {
                        srcK++;
                    }
                    
                    int srcKmin=srcK-1;
                    int srcKmax=srcK;
                    
                    if (srcK==srcLevs) {
                        srcKmax=srcKmin;
                    }

                    if (srcKmin < 0) {
                        srcKmin = 0;
                    }
                    
                    double srcZmin=Math.abs(srcDEPTH[srcKmin][0][0]);
                    double srcZmax=Math.abs(srcDEPTH[srcKmax][0][0]);

                    // System.out.println(dstZ+" =>" + srcKmin+":"+srcZmin+" - "+srcKmax+":"+srcZmax);
                    double delta=srcZmax-srcZmin;
                    
                    // System.out.println(w1+" "+w2);
                    Weight3D weight3D = new Weight3D();
                    if (delta!=0) {
                        weight3D.w[0] = (srcZmax-dstZ)/delta;
                        weight3D.w[1] = (dstZ-srcZmin)/delta;
                    } else {
                        weight3D.w[0] = 0;
                        weight3D.w[1] = 1;
                    
                    }
                    weight3D.KK[0] = srcKmin;
                    weight3D.KK[1] = srcKmax;
                    weight3Ds[dstK][dstJ][dstI] = weight3D;


                }
            }
        }
    } 
    
    public double[][][] interp(double[][][] src,double srcMissingValue, double dstMissingValue, double[][] mask) throws NCRegridderException {
        int dstLevs = dstDEPTH.length;   
        int srcLevs = srcDEPTH.length;   
        
        double[][][] tSrc = new double[srcLevs][1][1];
        for (int k=0;k<srcLevs;k++) {
            tSrc[k] = interpolator.interp(src[k],srcMissingValue,dstMissingValue,mask,false);
        }
            
        double[][][] dst = new double[dstLevs][interpolator.dstSNDim][interpolator.dstWEDim];
        System.out.println("Interpolating 3d...");
        for (int dstK=0;dstK<dstLevs;dstK++) {
            for (int dstJ=0;dstJ<interpolator.dstSNDim;dstJ++) {
                for (int dstI=0;dstI<interpolator.dstWEDim;dstI++) {
                    if (mask[dstJ][dstI]==1) {
                        int srcI=dstI;
                        int srcJ=dstJ;

                        int srcK0=weight3Ds[dstK][dstJ][dstI].KK[0];
                        int srcK1=weight3Ds[dstK][dstJ][dstI].KK[1];
                        double srcW0=weight3Ds[dstK][dstJ][dstI].w[0];
                        double srcW1=weight3Ds[dstK][dstJ][dstI].w[1];
                        double tSrcK0=tSrc[srcK0][srcJ][srcI];
                        double tSrcK1=tSrc[srcK1][srcJ][srcI];

                        //System.out.println("srcK0="+srcK0);
                        //System.out.println("srcK1="+srcK1);

                        if (tSrcK0!=dstMissingValue && tSrcK1!=dstMissingValue) {
                            dst[dstK][dstJ][dstI]=(tSrcK0* srcW0+tSrcK1*srcW1);
                        } else if (tSrcK0!=dstMissingValue) {
                            dst[dstK][dstJ][dstI]=tSrcK0;
                        } else if (tSrcK1!=dstMissingValue) {
                            dst[dstK][dstJ][dstI]=tSrcK1;
                        } else {

                            int _srcK0=srcK0;
                            while (_srcK0>=0 && tSrc[_srcK0][srcJ][srcI]==dstMissingValue ){
                                _srcK0--;
                            }
                            if (_srcK0==-1) {
                                dst[dstK][dstJ][dstI]=dstMissingValue;
                                double lat = interpolator.getDstLAT()[dstJ][dstI];
                                double lon = interpolator.getDstLONG()[dstJ][dstI];
                                
                                //System.out.println(dstJ+";"+dstI+";"+lat+";"+lon);
                            } else {
                                dst[dstK][dstJ][dstI]=tSrc[_srcK0][srcJ][srcI];
                            }
                            
                        }
                    } else {
                        dst[dstK][dstJ][dstI]=dstMissingValue;
                    }
                    
                }
            }
        }
        
        return dst;
    }
}
