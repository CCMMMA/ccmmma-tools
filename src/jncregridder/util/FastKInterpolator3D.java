/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jncregridder.util;

import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;



/**
 *
 * @author raffaelemontella
 */
public class FastKInterpolator3D extends Interpolator3DBase {
    private double subset=1;
    public void setSubset(double subset) { this.subset=subset; }
    private KInterpolator interpolator=null;
    private InterpolatorParams interpolatorParams=null;
    private Weight3D[][][] weight3Ds = null;
    
    private int NTHREDS=2;
    
    int srcLevs = 0; 
    
    public FastKInterpolator3D(double[][] srcLAT, double[][] srcLON, double[][][] srcZ, double[][] dstLAT, double[][] dstLON, double[][][] dstZ, int[][] srcMASK, int[][] dstMASK,double subset) throws InterpolatorException {
        NTHREDS=Runtime.getRuntime().availableProcessors();
        
        interpolatorParams = new InterpolatorParams();
        interpolatorParams.put("subset",subset);
        this.srcLAT = srcLAT;
        this.srcLON = srcLON;
        this.dstLAT = dstLAT;
        this.dstLON = dstLON;
        this.srcMASK = srcMASK;
        this.dstMASK = dstMASK;
        interpolator = new KInterpolator(srcLAT, srcLON, dstLAT, dstLON, srcMASK, dstMASK, interpolatorParams);
        init(srcLAT, srcLON, srcZ, dstLAT, dstLON, dstZ, srcMASK, dstMASK);
        this.subset=subset;
        
        
        prepare();
    }
    
    public void prepare() {
        int dstLevs = dstZ.length;
        
        System.out.println("dstMinZ:"+dstMinZ);
        int k=0;
        while (k<srcZ.length && Math.abs(srcZ[k][0][0])<=Math.abs(dstMinZ)) {
            System.out.println("srcDEPTH:"+srcZ[k][0][0]);
            k++;
        }
        
        srcLevs=k+1;
        System.out.println("srcLevs:"+srcLevs);
        
        
        weight3Ds = new Weight3D[dstLevs][interpolator.getDstSNDim()][interpolator.getDstWEDim()];
        for (int dstK=0;dstK<dstLevs;dstK++) {
            System.out.println("k="+dstK);
            for (int dstJ=0;dstJ<interpolator.getDstSNDim();dstJ++) {
                for (int dstI=0;dstI<interpolator.getDstWEDim();dstI++) {
                    
                    double dstZatKJI = Math.abs(dstZ[dstK][dstJ][dstI]);
                    
                    

                    int srcK=0;
                    double srcZat00=0;
                    
                    

                    while ((srcZat00=Math.abs(srcZ[srcK][0][0]))<=Math.abs(dstZatKJI) && srcK<srcLevs) {
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
                    
                    double srcZmin=Math.abs(srcZ[srcKmin][0][0]);
                    double srcZmax=Math.abs(srcZ[srcKmax][0][0]);

                    //System.out.println(dstZatKJI+" =>" + srcKmin+":"+srcZmin+" - "+srcKmax+":"+srcZmax);
                    double delta=srcZmax-srcZmin;
                    
                    // System.out.println(w1+" "+w2);
                    Weight3D weight3D = new Weight3D();
                    if (delta!=0) {
                        weight3D.w[0] = (srcZmax-dstZatKJI)/delta;
                        weight3D.w[1] = (dstZatKJI-srcZmin)/delta;
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
    
    class InterpolatorJob implements Runnable {
        private KInterpolator interpolator=null;
        private double[][][] src = null;
        double srcMissingValue,dstMissingValue;
        double[] params;
        private double[][][] tSrc = null;
        private int k;
        public InterpolatorJob(double[][][] src,double srcMissingValue, double dstMissingValue, double[] params, double[][][] tSrc,int k) throws InterpolatorException {
            this.src = src;
            this.srcMissingValue = srcMissingValue;
            this.dstMissingValue = dstMissingValue;
            this.params=params;
            this.tSrc=tSrc;
            this.k = k;
            this.interpolator= new KInterpolator(srcLAT, srcLON, dstLAT, dstLON, srcMASK, dstMASK, interpolatorParams);
            
        }
        
        public void run() {
            try {
                System.out.println("<k="+this.k + " depth:"+String.format("%4.2f",srcZ[this.k][0][0])+" ");
                    
                    
                tSrc[k] = this.interpolator.interp(this.src[this.k],this.srcMissingValue,this.dstMissingValue,this.params);
                System.out.println(" >");
            } catch (InterpolatorException ex) {
                Logger.getLogger(FastKInterpolator3D.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
    }
    
    
    
    @Override
    public double[][][] interp(double[][][] src,double srcMissingValue, double dstMissingValue, double[] params) throws InterpolatorException {
        int dstLevs = dstZ.length;   
          
        
        double[][][] tSrc = new double[srcLevs][1][1];
        System.out.println("----");
        if (NTHREDS>1) {
            try {
                ExecutorService executor = Executors.newFixedThreadPool(NTHREDS);
                for (int k=0;k<srcLevs;k++) {
                    Runnable interpolatorJob = new InterpolatorJob(src,srcMissingValue,dstMissingValue,params,tSrc,k);
                    executor.execute(interpolatorJob);
                }
                // This will make the executor accept no new threads
                // and finish all existing threads in the queue
                executor.shutdown();
                // Wait until all threads are finish
                executor.awaitTermination(1, TimeUnit.DAYS);
            } catch (InterruptedException ex) {
                throw new InterpolatorException(ex);
            }
        } else {
            for (int k=0;k<srcLevs;k++) {
                System.out.println("<k="+k + " depth:"+String.format("%4.2f",srcZ[k][0][0])+" ");
                tSrc[k] = interpolator.interp(src[k],srcMissingValue,dstMissingValue,params);
                System.out.println(" >");
            }
            
        }
        System.out.println("----");
        int[][] mask = interpolator.getDstMASK();
        
        double[][][] dst = new double[dstLevs][interpolator.getDstSNDim()][interpolator.getDstWEDim()];
        System.out.println("Interpolating 3d...");
        for (int dstK=0;dstK<dstLevs;dstK++) {
            for (int dstJ=0;dstJ<interpolator.getDstSNDim();dstJ++) {
                for (int dstI=0;dstI<interpolator.getDstWEDim();dstI++) {
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
                                double lon = interpolator.getDstLON()[dstJ][dstI];
                                
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

    @Override
    public double[][] interp(double[][] src, double srcMissingValue, double dstMissingValue, double[] params) throws InterpolatorException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    

    
    
}
