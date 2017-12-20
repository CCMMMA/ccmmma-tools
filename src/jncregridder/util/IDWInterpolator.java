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
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author raffaelemontella
 */
public class IDWInterpolator extends InterpolatorBase  {
    
    private Stations srcStations=null;
    private Stations dstStations=null;
    private double radParam=4.5;
    
    
    private InverseDistanceWeighting idw = null;

    public IDWInterpolator(double[][] srcLAT, double[][] srcLON, double[][] dstLAT, double[][] dstLON, int[][] srcMASK,int[][] dstMASK) throws InterpolatorException {
        init(srcLAT,srcLON,dstLAT,dstLON,srcMASK,dstMASK);
        
        this.idw = new InverseDistanceWeighting();
        double dLon=srcMaxLon-srcMinLon;
        double dLat=srcMaxLat-srcMaxLat;
        double radius = radParam*Math.max((dLon/dstWEDim),(dLat/dstSNDim));
        
        // radius = (Double)params[0];
        
        idw.setRadius(radius);
        try {
            idw.setWeightsFilename(System.getProperty("java.io.tmpdir")+File.separator+"weights-idw-"+getMD5()+".bin");
        } catch (NCRegridderException ex) {
            throw new InterpolatorException(ex);
        }
        createSrcStations();
        createDstStations();
        
        try {
            idw.createWeights();
        } catch (InverseDistanceWeightingException ex) {
            throw new InterpolatorException(ex);
        }
        
        
    }
    
    
    
    
    
    
    
    public IDWInterpolator(double[][] srcLAT, double[][] srcLON, double[][] dstLAT, double[][] dstLON, int[][] srcMASK,int[][] dstMASK,InterpolatorParams params) throws InterpolatorException {
        this.idw = new InverseDistanceWeighting();
        
        
        init(srcLAT,srcLON,dstLAT,dstLON,srcMASK,dstMASK);
        if (params.containsKey("radfactor")==true) {
            this.radParam=params.doubleValue("radfactor");
            this.idw = new InverseDistanceWeighting();
            double dLon=srcMaxLon-srcMinLon;
            double dLat=srcMaxLat-srcMaxLat;
            double radius = radParam*Math.max((dLon/dstWEDim),(dLat/dstSNDim));
            idw.setRadius(radius);
        }
        
        if (params.containsKey("radius")==true) {
            idw.setRadius(params.doubleValue("radius"));
        }
        
        try {
            idw.setWeightsFilename(System.getProperty("java.io.tmpdir")+File.separator+"weights-idw-"+getMD5()+".bin");
        } catch (NCRegridderException ex) {
            throw new InterpolatorException(ex);
        }
        createSrcStations();
        createDstStations();
        
        try {
            idw.createWeights();
        } catch (InverseDistanceWeightingException ex) {
            throw new InterpolatorException(ex);
        } 
        
        
    }
    
    public String getMD5() throws NCRegridderException {
        String md5 = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(srcLAT);
            oos.writeObject(srcLON);
            oos.writeObject(dstLAT);
            oos.writeObject(dstLON);
            oos.writeObject(new Double(idw.getRadius()));
            oos.close();

            byte[] bytesOfMessage = baos.toByteArray();

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] thedigest = md.digest(bytesOfMessage);

            StringBuffer hexString = new StringBuffer();
            for (int i=0;i<thedigest.length;i++) {
                hexString.append(Integer.toHexString(0xFF & thedigest[i]));
            }
            md5 = hexString.toString();


        } catch (NoSuchAlgorithmException ex) {
            throw new NCRegridderException(ex);
        } catch (IOException ex) {
            throw new NCRegridderException(ex);
        }

        return md5;
    }
    
    private void createSrcStations() {
        double lon,lat;
        
        
        srcStations = new Stations();
        idw.setSrcStations(srcStations);
        int id = 0;
        for (int j=0;j<srcSNDim;j++) {
            for (int i=0;i<srcWEDim;i++) {
                lon=srcLON[j][i];
                lat=srcLAT[j][i];
                if (srcMASK[j][i]==1) {

                        srcStations.add(new Station(id,lon,lat,j,i));
                        id++;

                }
            }
        }
            
        
    }
    
    private void createDstStations() {
        dstStations = new Stations();
        idw.setDstStations(dstStations);
        int id=0;
        for (int j=0;j<dstSNDim;j++) {
         for (int i=0;i<dstWEDim;i++) {
             if (dstMASK[j][i]==1) {
                dstStations.add(new Station(id,dstLON[j][i],dstLAT[j][i],j,i));
                
                id++;
                
             } 
         }
        }   
    }
    
    @Override
    public double[][] interp(double[][] src, double srcMissingValue,double dstMissingValue, double[] params) throws InterpolatorException {
        if (params!=null) {
            //kriging.setIntegralScale(params);
        }
        double[][] dst = null;
        if (src==null) throw new InterpolatorException("Can't interpolate null variables!");
        
        
        
         
        
        IdDoubleVectData srcData = new IdDoubleVectData();
        idw.setSrcData(srcData);
        double lon,lat;
        
        
        double min, max, mean;
        
        Iterator<Station> iSrcStations = srcStations.iterator();
        while (iSrcStations.hasNext()) {
            Station srcStation = iSrcStations.next();
            double[] v = {src[srcStation.j][srcStation.i]};
            srcData.put(srcStation.id, v );
        }
        try {
                min=idw.getMin();
                max=idw.getMax();
                mean=idw.getMean();
            } catch (InverseDistanceWeightingException ex) {
                throw new InterpolatorException(ex);
            }
            System.out.println("srcMin:"+String.format("%4.4f",min)+" srcMax:"+String.format("%4.4f",max)+" srcMean:"+String.format("%4.4f",mean));

        
        
         
        try {
            idw.execute();
        } catch (InverseDistanceWeightingException ex) {
            throw new InterpolatorException(ex);
        }

        IdDoubleVectData dstData=idw.getDstData();
        double dstMin=Double.NaN;
        double dstMax=Double.NaN;
        double dstMean=0;
        int count=0;
        
        
        dst = new double[dstSNDim][dstWEDim];
        for (int j=0;j<dstSNDim;j++) {
            for (int i=0;i<dstWEDim;i++) {
                dst[j][i]=dstMissingValue;
            }
        }
        
        Iterator<Station> iDstStations = dstStations.iterator();
        while (iDstStations.hasNext()) {
            Station dstStation = iDstStations.next();
            int i=dstStation.i;
            int j=dstStation.j;
            int id=dstStation.id;
            dst[j][i]=dstData.get(id)[0];
            
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
        }
        
       
        if (count>0) {
            dstMean=dstMean/count;
        }
        System.out.println("dstMin:"+String.format("%4.4f",dstMin)+" dstMax:"+String.format("%4.4f",dstMax)+" dstMean:"+String.format("%4.4f",dstMean));
        return dst;
    }
}
