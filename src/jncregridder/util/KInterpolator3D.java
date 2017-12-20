/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jncregridder.util;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author raffaelemontella
 */
public class KInterpolator3D extends Interpolator3DBase {
    private double subset=1;
    public void setSubset(double subset) { this.subset=subset; }
    private Kriging kriging=new Kriging();
    
    
    
    private int[][][] dst2id = null;

    public KInterpolator3D(double[][] srcLAT, double[][] srcLON, double[][][] srcZ, double[][] dstLAT, double[][] dstLON, double[][][] dstZ, int[][] srcMASK, int[][] dstMASK,double subset) throws InterpolatorException {
        init(srcLAT, srcLON, srcZ, dstLAT, dstLON, dstZ, srcMASK, dstMASK);
        kriging.setPMode(1);
        this.subset=subset;
        
       
        
        
        dst2id = new int[dstDepthDim][dstSNDim][dstWEDim];
        
    }
    
    @Override
    public double[][][] interp(double[][][] src, double srcMissingValue,double dstMissingValue, double[] params) throws InterpolatorException {
        double[][][] dst = null;
        if (src==null) throw new InterpolatorException("Can't interpolate null variables!");
        
        IdDoubleVectData inData = new IdDoubleVectData();
        kriging.setInData(inData);
        double lon,lat,depth;
        int id=0;
        
        double min,max;
        
        do {
            Stations inStations = new Stations();
            kriging.setInStations(inStations);
            id=0;
            for (int k=0;k<srcDepthDim;k++) {
                for (int j=0;j<srcSNDim;j++) {
                    for (int i=0;i<srcWEDim;i++) {
                        lon=srcLON[j][i];
                        lat=srcLAT[j][i];
                        depth = srcZ[k][j][i];
                        if (srcMASK[j][i]==1  && src[k][j][i]!=srcMissingValue ) {
                            if (lon >= minLon && lon<=maxLon && lat>=minLat && lat<=maxLat && depth>=dstMinZ) {
                                if (Math.random() <= subset) {
                                    
                                    //System.out.println(lon+";"+lat+";"+depth);
                                    inStations.add(new Station(id,lon,lat,depth));

                                    double[] v = {src[k][j][i]};
                                    inData.put(id, v );
                                    id++;
                                }
                            }
                        }
                    }
                }
            }
            
            try {
                min=kriging.getMin();
                max=kriging.getMax();
            } catch (KrigingException ex) {
                throw new InterpolatorException(ex);
            }
            System.out.println("id:"+id+" srcMin:"+String.format("%4.2f",min)+" srcMax:"+String.format("%4.2f",max));
        } while (id==0 || Double.isNaN(min) || Double.isNaN(max));
        
        Stations inInterpolate = new Stations();
        kriging.setInInterpolate(inInterpolate);
        id=0;
        for (int k=0;k<dstDepthDim;k++) {
            for (int j=0;j<dstSNDim;j++) {
                for (int i=0;i<dstWEDim;i++) {
                    if (dstMASK[j][i]==1) {
                        lon=dstLON[j][i];
                        lat=dstLAT[j][i];
                        depth=dstZ[k][j][i];
                        inInterpolate.add(new Station(id,lon,lat,depth));
                        dst2id[k][j][i]=id;
                        id++;
                
                    } else { 
                        dst2id[k][j][i]=-1;
                    }
                }
            }
        }
            
            
            
            
        
        try {
            kriging.execute();
        } catch (KrigingException ex) {
            throw new InterpolatorException(ex);
        }
        
        IdDoubleVectData outData = kriging.getOutData();

        double dstMin=Double.NaN;
        double dstMax=Double.NaN;
        
        dst = new double[dstDepthDim][dstSNDim][dstWEDim];
        for (int k=0;k<dstDepthDim;k++) {
            for (int j=0;j<dstSNDim;j++) {
                for (int i=0;i<dstWEDim;i++) {
                    id = dst2id[k][j][i];
                    if (id!=-1) {
                        dst[k][j][i]=outData.get(id)[0];
                        if (Double.isNaN(dstMin)==true) {
                            dstMin=dst[k][j][i];
                        } else {
                            if (dst[k][j][i]<dstMin) {
                                dstMin=dst[k][j][i];
                            }
                        }
                        if (Double.isNaN(dstMax)==true) {
                            dstMax=dst[k][j][i];
                        } else {
                            if (dst[k][j][i]>dstMax) {
                                dstMax=dst[k][j][i];
                            }
                        }
                    } else {
                        dst[k][j][i]=dstMissingValue;
                    }

                }
            }
        }
        System.out.println("dstMin:"+String.format("%4.2f",dstMin)+" dstMax:"+String.format("%4.2f",dstMax));
        return dst;
    }

    @Override
    public double[][] interp(double[][] src, double srcMissingValue, double dstMissingValue, double[] params) throws InterpolatorException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    
    
}
