/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jncregridder.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author raffaelemontella
 */
public class KInterpolator extends InterpolatorBase  {
    private double subset=1;
    
    public KInterpolator(Stations stations, double[][] dstLAT, double[][] dstLON) {
        
    }
    

    public KInterpolator(double[][] srcLAT, double[][] srcLON, double[][] dstLAT, double[][] dstLON, int[][] srcMASK,int[][] dstMASK) throws InterpolatorException {
        init(srcLAT,srcLON,dstLAT,dstLON,srcMASK,dstMASK);
        this.subset=1;
        
        
        dst2id = new int[dstSNDim][dstWEDim];
    }
    public void setSubset(double subset) { this.subset=subset; }
    
    private Kriging kriging = new Kriging();
    private int[][] dst2id = null;
    
    
    
    public KInterpolator(double[][] srcLAT, double[][] srcLON, double[][] dstLAT, double[][] dstLON, int[][] srcMASK,int[][] dstMASK, InterpolatorParams params) throws InterpolatorException {
        
        
        
        init(srcLAT,srcLON,dstLAT,dstLON,srcMASK,dstMASK);
        this.subset=params.doubleValue("subset");
        
        dst2id = new int[dstSNDim][dstWEDim];
    }
    
    
    
    
    @Override
    public double[][] interp(double[][] src, double srcMissingValue,double dstMissingValue, double[] params) throws InterpolatorException {
        if (params!=null) {
            kriging.setIntegralScale(params);
        }
        double[][] dst = null;
        if (src==null) throw new InterpolatorException("Can't interpolate null variables!");
        
        
         
        Stations inStations=null;
        Stations inInterpolate=null;
        IdDoubleVectData inData = new IdDoubleVectData();
        kriging.setInData(inData);
        double lon,lat;
        int id=0;
        
        double min, max, mean;
        
        double ePerc=.015;
        double eLon=Math.abs(maxLon-minLon)*ePerc;
        double eLat=Math.abs(maxLat-minLat)*ePerc;
        double eMinLon=minLon-eLon;
        double eMaxLon=maxLon+eLon;
        double eMinLat=minLat-eLat;
        double eMaxLat=maxLat+eLat;
        

        do {
            inStations = new Stations();
            kriging.setInStations(inStations);
            id=0;
            for (int j=0;j<srcSNDim;j++) {
                for (int i=0;i<srcWEDim;i++) {
                    lon=srcLON[j][i];
                    lat=srcLAT[j][i];
                    if (srcMASK[j][i]==1 && src[j][i]!=srcMissingValue ) {
                        if (lon >= eMinLon && lon<=eMaxLon && lat>=eMinLat && lat<=eMaxLat ) {
                            if (Math.random() <= subset) {
                            inStations.add(new Station(id,lon,lat));

                            double[] v = {src[j][i]};
                            inData.put(id, v );
                            id++;
                            }
                        }
                    }
                }
            }
            try {
                min=kriging.getMin();
                max=kriging.getMax();
                mean=kriging.getMean();
            } catch (KrigingException ex) {
                throw new InterpolatorException(ex);
            }
            System.out.println("id:"+id+" srcMin:"+String.format("%4.4f",min)+" srcMax:"+String.format("%4.4f",max)+" srcMean:"+String.format("%4.4f",mean));
        } while (id==0 /*|| Double.isNaN(min) || Double.isNaN(max)*/);
        
        
        

        inInterpolate = new Stations();
        kriging.setInInterpolate(inInterpolate);
        id=0;
        for (int j=0;j<dstSNDim;j++) {
         for (int i=0;i<dstWEDim;i++) {
             if (dstMASK[j][i]==1) {
                inInterpolate.add(new Station(id,dstLON[j][i],dstLAT[j][i]));
                dst2id[j][i]=id;
                id++;
                
             } else { 
                 dst2id[j][i]=-1;
             }
         }
        }
            
            
            
            
         
        try {
            kriging.execute();
        } catch (KrigingException ex) {
            throw new InterpolatorException(ex);
        }

        IdDoubleVectData outData=kriging.getOutData();
        double dstMin=Double.NaN;
        double dstMax=Double.NaN;
        double dstMean=0;
        int count=0;
        dst = new double[dstSNDim][dstWEDim];
        for (int j=0;j<dstSNDim;j++) {
            for (int i=0;i<dstWEDim;i++) {
                id = dst2id[j][i];
                if (id!=-1) {
                    dst[j][i]=outData.get(id)[0];
                    if (Double.isNaN(dst[j][i])==false) {
                        dstMean+=dst[j][i];
                        count++;
                    }
                    if (Double.isNaN(dstMin)==true) {
                        dstMin=dst[j][i];
                    } else {
                        if (dst[j][i]<dstMin) {
                            dstMin=dst[j][i];
                        }
                    }
                    if (Double.isNaN(dstMax)==true) {
                        dstMax=dst[j][i];
                    } else {
                        if (dst[j][i]>dstMax) {
                            dstMax=dst[j][i];
                        }
                    }
                } else {
                    dst[j][i]=dstMissingValue;
                }

            }
        }
        if (count>0) {
            dstMean=dstMean/count;
        }
        System.out.println("dstMin:"+String.format("%4.4f",dstMin)+" dstMax:"+String.format("%4.4f",dstMax)+" dstMean:"+String.format("%4.4f",dstMean));
        return dst;
    }
    
    
}
