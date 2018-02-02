/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jncregridder.util;

/**
 *
 * @author raffaelemontella
 */
public class BilinearInterpolator extends InterpolatorBase {
    
    private double TOLL=1e-6;
    
    public BilinearInterpolator(double[][] srcLAT, double[][] srcLON, double[][] dstLAT, double[][] dstLON, int[][] srcMASK,int[][] dstMASK) throws InterpolatorException {
        
        
        init(srcLAT,srcLON,dstLAT,dstLON,srcMASK,dstMASK);
        
        
    }

    

    @Override
    public double[][] interp(double[][] src, double srcMissingValue, double dstMissingValue, double[] params) throws InterpolatorException {
        
        if (src==null) throw new InterpolatorException("Can't interpolate null variables!");
        
        int dstEta=dstLAT.length;
        int dstXi=dstLAT[0].length;
        int srcEta=srcLAT.length;
        int srcXi=srcLAT[0].length;
        
        double srcLonMin=srcLON[0][0];
        double srcLatMin=srcLAT[0][0];
        double srcLonMax=srcLON[srcEta-1][srcXi-1];
        double srcLatMax=srcLAT[srcEta-1][srcXi-1];
        double srcLatDelta=srcLatMax-srcLatMin;
        double srcLonDelta=srcLonMax-srcLonMin;
        double srcLatStep=srcLatDelta/srcEta;
        double srcLonStep=srcLonDelta/srcXi;
        
        double dstLat,dstLon;


        int[] II=new int[4];
        int[] JJ=new int[4];
        double[] q=new double[4];
        double[] d = new double[4];
        double[] w = new double[4];
        
        double srcMean=0;
        int srcCount=0;
        double dstMean=0;
        int dstCount=0;

        // Allocate the destination matrix
        double[][] dst = new double[dstEta][dstXi];

        // For each row
        for (int dstJ=0;dstJ<dstEta;dstJ++) {
            // For each cel
            for (int dstI=0;dstI<dstXi;dstI++) {

                // Consider only sea (skip land)
                if (dstMASK[dstJ][dstI]==1) {

                    // Get the destination coordinates in longitude and latitudes
                    dstLon=dstLON[dstJ][dstI];
                    dstLat=dstLAT[dstJ][dstI];

                    // Evaluate the real indexes in the source matrix
                    double srcII=(dstLon-srcLonMin)/srcLonStep;
                    double srcJJ=(dstLat-srcLatMin)/srcLatStep;

                    // Consider the integer part of the indexes
                    int iR=(int)(srcII);
                    int jR=(int)(srcJJ);

                    // The number of weights considered for interpolation
                    int nW=0;

                    int b=0;
                    do {
                        b++;




                        // Temporary II vertices
                        int[] tII = new int[] { iR-(b-1),iR+b,iR+b,iR-(b-1) };

                        for (int i=0;i<tII.length;i++) {
                            if (tII[i]>=src[0].length) {
                                tII[i]=src[0].length-1;
                            }
                        }
                        /*
                        System.out.print(b+" tII:");
                        for (int tI:tII) {
                            System.out.print(tI+",");
                        }
                        System.out.println("");
                        */

                        // Temporary JJ vertices
                        int[] tJJ = new int[] { jR-(b-1),jR-(b-1),jR+b,jR+b };
                        for (int j=0;j<tJJ.length;j++) {
                            if (tJJ[j]>=src.length) {
                                tJJ[j]=src.length-1;
                            }
                        }

                        /*
                        System.out.print(b+" tJJ:");
                        for (int tJ:tJJ) {
                            System.out.print(tJ+",");
                        }
                        System.out.println("");
                        */

                        double[] tQ = {
                                src[tJJ[0]][tII[0]],
                                src[tJJ[0]][tII[2]],
                                src[tJJ[2]][tII[2]],
                                src[tJJ[2]][tII[0]]
                        };

                        /*
                        double q1=src[jR-(b-1)][iR-(b-1)];
                        double q2=src[jR-(b-1)][iR+b];
                        double q3=src[jR+b][iR+b];
                        double q4=src[jR+b][iR-(b-1)];

                        double[] tQ = {
                                q1,
                                q2,
                                q3,
                                q4
                        };

                        */
                        /*
                        double[] tQ = {
                                src[jR-(b-1)][iR-(b-1)],
                                src[jR-(b-1)][iR+b],
                                src[jR+b][iR+b],
                                src[jR+b][iR-(b-1)]
                        };
                        */

                        nW=0;
                        for (int l=0;l<4;l++) {
                            //System.out.println(tJJ[l]+" "+tII[l]+":"+tQ[l]);
                            if (Double.isNaN(tQ[l])==false && tQ[l]!=srcMissingValue) {
                                q[nW]=tQ[l];
                                JJ[nW]=tJJ[nW];
                                II[nW]=tII[nW];
                                nW++;
                            }
                        }
                        
                    } while (nW==0 & b<3);


                    double latIJ,lonIJ;
                    double dS=0;

                    if (nW==0) {
                        //throw new InterpolatorException("nW is 0! ("+dstLon+","+dstLat+")");
                        dst[dstJ][dstI]=dstMissingValue;
                    } else if (nW==1) {
                        dst[dstJ][dstI]=q[0];
                        srcMean+=q[0];
                        srcCount++;
                        
                    } else {
                        for ( int a=0;a<nW;a++) {

                            latIJ = srcLAT[JJ[a]][II[a]];
                            lonIJ = srcLON[JJ[a]][II[a]];


                            d[a] = Math.pow((latIJ-dstLAT[dstJ][dstI])*(latIJ-dstLAT[dstJ][dstI])+(lonIJ-dstLON[dstJ][dstI])*(lonIJ-dstLON[dstJ][dstI]),.5);
                            dS+=(1/d[a]);
                        }

                        double sW=0;
                        for ( int a=0;a<nW;a++) {
                            w[a] = (1/(d[a]))/(dS);
                            sW+=w[a];
                        }
                        if (sW<1-TOLL || sW>1+TOLL) throw new InterpolatorException("Bad w:"+sW);


                        dst[dstJ][dstI]=0;
                        for (int l=0;l<nW;l++) {
                            dst[dstJ][dstI]+=w[l]*q[l];
                            srcMean+=q[l];
                            srcCount++;
                        }
                    }
                    // System.out.println("dstLon:"+dstLon+" dstLat:"+dstLat+" nW="+nW+" b="+b+" dst="+dst[dstJ][dstI]);
                } else {
                    dst[dstJ][dstI]=dstMissingValue;
                }
                
                if (dst[dstJ][dstI]!=dstMissingValue) {
                    dstMean+=dst[dstJ][dstI];
                    dstCount++;
                }
            }
            
            
        }

        // Calculate the control values
        srcMean=srcMean/srcCount;
        dstMean=dstMean/dstCount;

        // Show the control values
        System.out.println("srcMean:"+srcMean+" dstMean:"+dstMean);

        // Return the results
        return dst;
    }
    
}
