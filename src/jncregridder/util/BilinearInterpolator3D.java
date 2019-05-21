/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jncregridder.util;

/**
 *
 * @author raffaelemontella
 */
public class BilinearInterpolator3D extends Interpolator3DBase {
    private BilinearInterpolator interpolator=null;
    private Weight3D[][][] weight3Ds = null;

    //private boolean use_extrapolation=true;
    
    int srcLevs = 0; 
    
    public BilinearInterpolator3D(double[][] srcLAT, double[][] srcLON, double[][][] srcZ, double[][] dstLAT, double[][] dstLON, double[][][] dstZ, int[][] srcMASK, int[][] dstMASK) throws InterpolatorException {
        interpolator = new BilinearInterpolator(srcLAT, srcLON, dstLAT, dstLON, srcMASK, dstMASK);
        init(srcLAT, srcLON, srcZ, dstLAT, dstLON, dstZ, srcMASK, dstMASK);
        
        prepare();
    }

    /*
    public void prepare() {

        // Get the number of destination levels
        int dstLevs = dstZ.length;
        
        System.out.println("dstMinZ:"+dstMinZ);


        // Find the deepest source level interested by the vertical interpolation

        // Set the k index to 0
        int k=0;

        // For each source level, exit when the source level is deeper than the deepest destination level
        while (k<srcZ.length && Math.abs(srcZ[k][0][0])<=Math.abs(dstMinZ)) {

            // Persorm some output
            System.out.println("srcDEPTH:"+srcZ[k][0][0]);

            // Increment the level
            k++;
        }

        // Increase the number of source level by 1 (?)
        srcLevs=k+1;

        // Perform some output
        System.out.println("srcLevs:"+srcLevs);
        
        // Create a matrix of Weight3D with the size of destination levels, eta, xi
        weight3Ds = new Weight3D[dstLevs][interpolator.getDstSNDim()][interpolator.getDstWEDim()];

        // For each sigma destination level, dstK...
        for (int dstK=0;dstK<dstLevs;dstK++) {

            // Perform some output...
            System.out.println("k="+dstK);

            // For each destination eta, j...
            for (int dstJ=0;dstJ<interpolator.getDstSNDim();dstJ++) {

                // For each destination xi, i...
                for (int dstI=0;dstI<interpolator.getDstWEDim();dstI++) {

                    // Get the destination depth at the 3D point
                    double dstZatKJI = Math.abs(dstZ[dstK][dstJ][dstI]);
                    
                    
                    // Index of source level
                    int srcK=0;

                    // Search the deepest source level closer to the dstZatKJI
                    while ((Math.abs(srcZ[srcK][0][0]))<=Math.abs(dstZatKJI) && srcK<srcLevs) {

                        // Increase the index of the source level
                        srcK++;
                    }

                    // Index of the less depth closer to the dstZatKJI
                    int srcKmin=srcK-1;

                    // Index of the more depth closer to the dstZatKJI
                    int srcKmax=srcK;

                    // Check if the index of the more depth closer to the dstZatKJI is equals to the number of considered source levels
                    if (srcKmax==srcLevs) {

                        srcKmax=srcKmin;
                    }


                    // The less deep value (closer to the surface)
                    double srcZmin=Double.NaN;



                    // Check if index of the less depth closer to the dstZatKJI is less than 0
                    if (srcKmin < 0) {

                        // We have the surface...
                        srcZmin = 0;
                    } else {
                        // Get the value at the less deep point in the source depth grid
                        srcZmin=Math.abs(srcZ[srcKmin][0][0]);
                    }

                    // Get the value at the more deep point in the source depth grid
                    double srcZmax=Math.abs(srcZ[srcKmax][0][0]);

                    //System.out.println(dstZatKJI+" =>" + srcKmin+":"+srcZmin+" - "+srcKmax+":"+srcZmax);

                    // Evaluate the width of the level
                    double delta=srcZmax-srcZmin;
                    
                    // System.out.println(w1+" "+w2);

                    // Evaluate the weights
                    Weight3D weight3D = new Weight3D();
                    weight3D.h=dstZatKJI;

                    // Check if the delta is different thant 0 (the point is between two levels
                    if (delta!=0) {
                        // The point is between two levels

                        //weight3D.w[0] = (srcZmax-dstZatKJI)/delta;
                        //weight3D.w[1] = (dstZatKJI-srcZmin)/delta;

                        weight3D.w[0] = (dstZatKJI-srcZmin)/delta;
                        weight3D.w[1] = (srcZmax-dstZatKJI)/delta;
                    } else {
                        // The two levels are coincident

                        weight3D.w[0] = 0;
                        weight3D.w[1] = 1;
                    
                    }
                    // Save the indexes
                    weight3D.KK[0] = srcKmin;
                    weight3D.KK[1] = srcKmax;

                    // Save the weight object
                    weight3Ds[dstK][dstJ][dstI] = weight3D;


                }
            }
        }
        
        
    } 
    */

    public void prepare()
    {
        int dstLevs = dstZ.length;

        System.out.println("dstMinZ:" + dstMinZ);

        int k = 0;

        while ((k < srcZ.length) && (Math.abs(srcZ[k][0][0]) <= Math.abs(dstMinZ))) {
            System.out.println("srcDEPTH:" + srcZ[k][0][0]);
            k++;
        }

        srcLevs = (k + 1);
        System.out.println("srcLevs:" + srcLevs);

        weight3Ds = new Weight3D[dstLevs][interpolator.getDstSNDim()][interpolator.getDstWEDim()];

        for (int dstK = 0; dstK < dstLevs; dstK++) {

            System.out.println("k=" + dstK);
            for (int dstJ = 0; dstJ < interpolator.getDstSNDim(); dstJ++) {

                for (int dstI = 0; dstI < interpolator.getDstWEDim(); dstI++) {

                    double dstZatKJI = Math.abs(dstZ[dstK][dstJ][dstI]);

                    int srcK = 0;

                    double srcZat00 = 0.0D;

                    while (((srcZat00 = Math.abs(srcZ[srcK][0][0])) <= Math.abs(dstZatKJI)) && (srcK < srcLevs)) {
                        srcK++;
                    }


                    int srcKmin = srcK - 1;
                    int srcKmax = srcK;

                    if (srcKmax == srcLevs) {
                        srcKmax = srcKmin;
                    }

                    if (srcKmin < 0) {
                        srcKmin = 0;
                    }


                    double srcZmin = Math.abs(srcZ[srcKmin][0][0]);

                    double srcZmax = Math.abs(srcZ[srcKmax][0][0]);

                    double delta = srcZmax - srcZmin;


                    Weight3D weight3D = new Weight3D();


                    if (delta != 0.0D) {
                        weight3D.w[0] = ((dstZatKJI - srcZmin) / delta);
                        weight3D.w[1] = ((srcZmax - dstZatKJI) / delta);
                    }
                    else {
                        weight3D.w[0] = 0.0D;
                        weight3D.w[1] = 1.0D;
                    }


                    weight3D.KK[0] = srcKmin;
                    weight3D.KK[1] = srcKmax;


                    weight3Ds[dstK][dstJ][dstI] = weight3D;
                }
            }
        }
    }

    @Override
    public double[][][] interp(double[][][] src, double srcMissingValue, double dstMissingValue, double[] params)
            throws InterpolatorException {
        int dstLevs = dstZ.length;


        double[][][] tSrc = new double[srcLevs][1][1];
        for (int k = 0; k < srcLevs; k++) {
            System.out.println("<k=" + k + " depth:" + String.format("%4.2f", new Object[] { Double.valueOf(srcZ[k][0][0]) }) + " ");

            tSrc[k] = interpolator.interp(src[k], srcMissingValue, dstMissingValue, params);
            System.out.println(" >");
        }

        int[][] mask = interpolator.getDstMASK();

        double[][][] dst = new double[dstLevs][interpolator.getDstSNDim()][interpolator.getDstWEDim()];
        System.out.println("Interpolating 3d...");
        for (int dstK = 0; dstK < dstLevs; dstK++) {
            for (int dstJ = 0; dstJ < interpolator.getDstSNDim(); dstJ++) {
                for (int dstI = 0; dstI < interpolator.getDstWEDim(); dstI++) {
                    if (mask[dstJ][dstI] == 1) {
                        int srcI = dstI;
                        int srcJ = dstJ;

                        int srcK0 = weight3Ds[dstK][dstJ][dstI].KK[0];
                        int srcK1 = weight3Ds[dstK][dstJ][dstI].KK[1];
                        double srcW0 = weight3Ds[dstK][dstJ][dstI].w[0];
                        double srcW1 = weight3Ds[dstK][dstJ][dstI].w[1];
                        double tSrcK0 = tSrc[srcK0][srcJ][srcI];
                        double tSrcK1 = tSrc[srcK1][srcJ][srcI];

                        if ((tSrcK0 != dstMissingValue) && (tSrcK1 != dstMissingValue)) {
                            dst[dstK][dstJ][dstI] = (tSrcK0 * srcW0 + tSrcK1 * srcW1);
                        } else if (tSrcK0 != dstMissingValue) {
                            dst[dstK][dstJ][dstI] = tSrcK0;
                        } else if (tSrcK1 != dstMissingValue) {
                            dst[dstK][dstJ][dstI] = tSrcK1; }
                        else {
                            dst[dstK][dstJ][dstI] = dstMissingValue;
                        }
                    } else {
                        dst[dstK][dstJ][dstI] = dstMissingValue;
                    }
                }
            }
        }
        return dst;
    }

    /*
    @Override
    public double[][][] interp(double[][][] src,double srcMissingValue, double dstMissingValue, double[] params) throws InterpolatorException {
        int dstLevs = dstZ.length;   
          
        
        double[][][] tSrc = new double[srcLevs][1][1];
        for (int k=0;k<srcLevs;k++) {
            
                System.out.println("<k="+k + " depth:"+String.format("%4.2f",srcZ[k][0][0])+" ");
                
                tSrc[k] = interpolator.interp(src[k],srcMissingValue,dstMissingValue,params);
                System.out.println(" >");
        }
        
        int[][] mask = interpolator.getDstMASK();
        
        double[][][] dst = new double[dstLevs][interpolator.getDstSNDim()][interpolator.getDstWEDim()];
        System.out.println("Interpolating 3d...");
        for (int dstK=0;dstK<dstLevs;dstK++) {
            for (int dstJ=0;dstJ<interpolator.getDstSNDim();dstJ++) {
                for (int dstI=0;dstI<interpolator.getDstWEDim();dstI++) {
                    // Set the value as missing
                    dst[dstK][dstJ][dstI]=dstMissingValue;

                    // Check if the point is not masked
                    if (mask[dstJ][dstI]==1) {

                        // The point have to be valuated

                        // Set the source horizontal indexes
                        int srcI=dstI;
                        int srcJ=dstJ;

                        // Get the value of the upper (less depth) index
                        int srcK0=weight3Ds[dstK][dstJ][dstI].KK[0];

                        // Check if we are at the surface
                        if (srcK0>=0) {
                            // Interpolation between two source levels

                            // Get the value of the lower (more depth) index
                            int srcK1 = weight3Ds[dstK][dstJ][dstI].KK[1];

                            // Get the weight for the upper index
                            double srcW0 = weight3Ds[dstK][dstJ][dstI].w[0];

                            // Get the weight for the lower index
                            double srcW1 = weight3Ds[dstK][dstJ][dstI].w[1];

                            // Get the value related to the upper index
                            double tSrcK0 = tSrc[srcK0][srcJ][srcI];

                            // Get the value related to the lower index
                            double tSrcK1 = tSrc[srcK1][srcJ][srcI];


                            //System.out.println("srcK0="+srcK0);
                            //System.out.println("srcK1="+srcK1);


                            // This approach is less conservative (try to aviid missing values)

                            //if (tSrcK0 != dstMissingValue && tSrcK1 != dstMissingValue) {
                            //    dst[dstK][dstJ][dstI] = (tSrcK0 * srcW0 + tSrcK1 * srcW1);
                            //} else if (tSrcK0 != dstMissingValue) {
                            //    dst[dstK][dstJ][dstI] = tSrcK0;
                            //} else if (tSrcK1 != dstMissingValue) {
                            //    dst[dstK][dstJ][dstI] = tSrcK1;
                            //} else {
                            //    int _srcK0=srcK0;
                            //    while (_srcK0>=0 && tSrc[_srcK0][srcJ][srcI]==dstMissingValue ){
                            //        _srcK0--;
                            //    }
                            //    if (_srcK0==-1) {
                            //        dst[dstK][dstJ][dstI]=dstMissingValue;
                            //        double lat = interpolator.getDstLAT()[dstJ][dstI];
                            //        double lon = interpolator.getDstLON()[dstJ][dstI];

                                    //System.out.println(dstJ+";"+dstI+";"+lat+";"+lon);
                            //    } else {
                            //        dst[dstK][dstJ][dstI]=tSrc[_srcK0][srcJ][srcI];
                            //    }
                            //    dst[dstK][dstJ][dstI] = dstMissingValue;
                            //}

                            // Check if both source values are valid
                            if (tSrcK0 != dstMissingValue && tSrcK1 != dstMissingValue) {

                                // Interpolate
                                dst[dstK][dstJ][dstI] = (tSrcK0 * srcW0 + tSrcK1 * srcW1);
                            }

                        } else {
                            // We are between the surface and the first source level


                            if (use_extrapolation) {
                                // Extrapolation from the first two source levels

                                // Get the source value at the level closer to the surface
                                double tSrcK0 = tSrc[0][srcJ][srcI];

                                // Get the source value at the second level closer to the surface
                                double tSrcK1 = tSrc[1][srcJ][srcI];

                                // Check if both source values are valid
                                if (tSrcK0 != dstMissingValue && tSrcK1 != dstMissingValue) {

                                    // Get the Z value at the less deep point in the source depth grid
                                    double srcZmin = Math.abs(srcZ[0][0][0]);


                                    // Get the Z value at the more deep point in the source depth grid
                                    double srcZmax = Math.abs(srcZ[1][0][0]);

                                    // Calculate the rate
                                    double m = (tSrcK0 - tSrcK1) / (srcZmin - srcZmax);

                                    // Calculate the intercept
                                    double q = (srcZmin * tSrcK1 - srcZmax * tSrcK0) / (srcZmin - srcZmax);

                                    // Extrapolate
                                    dst[dstK][dstJ][dstI] = m * weight3Ds[dstK][dstJ][dstI].h + q;
                                }
                            } else {
                                // Don't use the extrapolation

                                // Get the source value at the level closer to the surface
                                double tSrcK0 = tSrc[0][srcJ][srcI];

                                // Check if the source value are valid
                                if (tSrcK0 != dstMissingValue) {

                                    // Assume the same value of the latest known level
                                    dst[dstK][dstJ][dstI] = tSrcK0;
                                }

                            }

                        }
                    }
                    
                }
            }
        }
        
        return dst;
    }
    */
    @Override
    public double[][] interp(double[][] src, double srcMissingValue, double dstMissingValue, double[] params) throws InterpolatorException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    

    
}
