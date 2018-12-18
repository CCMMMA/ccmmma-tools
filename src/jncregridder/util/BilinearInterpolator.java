/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jncregridder.util;

import it.uniparthenope.meteo.Haversine;

import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

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

        class Point {
            int j;
            int i;
            double dist;
            double value;
            double w;

            Point (int j, int i, double dist, double value) {
                this.j=j;
                this.i=i;
                this.dist=dist;
                this.value=value;
            }

            Point (int j, int i, double value) {
                this.j=j;
                this.i=i;
                this.dist=-1;
                this.value=value;
            }

            @Override
            public  String toString() {
                return j+","+i+","+value+","+dist;
            }
        }





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


                    Vector<Point> pointsBilinear=new Vector<>();

                    for (int j=jR;j<=jR+1;j++) {

                        int jj=j;
                        if (j>=src.length) {
                            jj=src.length-1;
                        }

                        for (int i = iR ;i<=iR+1; i++) {

                            int ii=i;
                            if (i>=src[0].length) {
                                ii=src[0].length-1;
                            }

                            if (Double.isNaN(src[jj][ii]) == false && src[jj][ii] != srcMissingValue) {
                                pointsBilinear.add(new Point(jj, ii,src[jj][ii]));
                            }
                        }
                    }

                    // Check if we have just 4 points (regular case)
                    if (pointsBilinear.size()==4) {
                        // Perform regular bilinear interpolation

                        double x1=srcLON[pointsBilinear.get(0).j][pointsBilinear.get(0).i];
                        double y1=srcLAT[pointsBilinear.get(0).j][pointsBilinear.get(0).i];
                        double x2=srcLON[pointsBilinear.get(1).j][pointsBilinear.get(1).i];
                        double y2=srcLAT[pointsBilinear.get(2).j][pointsBilinear.get(2).i];

                        dst[dstJ][dstI]=
                                (
                                        pointsBilinear.get(0).value * (x2 - dstLon) * (y2 - dstLat) +
                                                pointsBilinear.get(1).value * (dstLon - x1) * (y2 - dstLat) +
                                                pointsBilinear.get(2).value * (x2 - dstLon) * (dstLat - y1) +
                                                pointsBilinear.get(3).value * (dstLon - x1) * (dstLat - y1)
                                ) / (
                                        (x2 - x1) * (y2 - y1) + 0.0
                                );
                        srcMean+=(pointsBilinear.get(0).value+pointsBilinear.get(1).value+pointsBilinear.get(2).value+pointsBilinear.get(3).value);
                        srcCount=srcCount+4;
/*
                        System.out.println("---------------------------------");
                        System.out.println("BILINEAR:"+dst[dstJ][dstI]);
                        for (Point point:pointsBilinear) {
                            System.out.println(point);
                        }
                        System.out.println("---------------------------------");
*/
                    }
                    else  {
                        // With just 0, 1, 2 or 3 points, let we use the IDW

                        Vector<Point> pointsIDW=new Vector<>();
                        int size=0;

                        do {
                            size++;
                            for (int j = jR - size; j <= jR + size; j++) {
                                int jj = j;
                                if (j >= src.length) {
                                    jj = src.length - 1;
                                }
                                if (j < 0) {
                                    jj = 0;
                                }
                                for (int i = iR - size; i <= iR + size; i++) {
                                    int ii = i;
                                    if (i >= src[0].length) {
                                        ii = src[0].length - 1;
                                    }
                                    if (i < 0) {
                                        ii = 0;
                                    }
                                    if (Double.isNaN(src[jj][ii]) == false && src[jj][ii] != srcMissingValue) {
                                        pointsIDW.add(
                                                new Point(
                                                        jj, ii,
                                                        Haversine.distance(
                                                                dstLAT[dstJ][dstI],
                                                                dstLON[dstJ][dstI],
                                                                srcLAT[jj][ii],
                                                                srcLON[jj][ii]
                                                        ),
                                                        src[jj][ii]
                                                )
                                        );
                                    }
                                }
                            }
                        } while (pointsIDW.size()==0 && size<4);

                        if (pointsIDW.size()>0) {
                            double weighted_values_sum = 0.0;
                            double sum_of_weights = 0.0;
                            double weight;
                            for (Point point : pointsIDW) {
                                weight = 1/point.dist;
                                sum_of_weights += weight;
                                weighted_values_sum += weight * point.value;
                                srcMean += point.value;
                                srcCount++;
                            }
                            dst[dstJ][dstI]=weighted_values_sum / sum_of_weights;

/*
                        System.out.println("---------------------------------");
                        System.out.println("IDW:"+dst[dstJ][dstI]);
                        for (Point point:pointsIDW) {
                            System.out.println(point);
                        }
                        System.out.println("---------------------------------");
                        */
                        } else {
                            // We have no choice than a missing value
                            dst[dstJ][dstI] = dstMissingValue;
                        }
                    }

                } else {
                    // No other choice
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
