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
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

/**
 *
 * @author raffaelemontella
 */
public class PolarInterpolator extends InterpolatorBase {
    private String weightsFilename = null;
    
    
    
    private double srcLat0=Double.NaN;
    private double srcLon0=Double.NaN;
    private double srcAlpha=0;
    
    private Dst2Src dst2src = null;
    
    public PolarInterpolator(double[][] srcLAT, double[][] srcLON,double[][] dstLAT,double[][] dstLON, int[][] srcMASK, int[][] dstMASK, HashMap<String,Object> params) throws InterpolatorException {
        init(srcLAT, srcLON, dstLAT, dstLON, srcMASK, dstMASK);
        dst2src=new Dst2Src(dstLAT.length,dstLON[0].length);
        weightsFilename=System.getProperty("java.io.tmpdir")+File.separator+"w-"+this.getClass().getName()+"-"+getMD5()+".bin";
        prepare();
    }
    
    public String getMD5() throws InterpolatorException {
        String md5 = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(srcLAT);
            oos.writeObject(srcLON);
            oos.writeObject(dstLAT);
            oos.writeObject(dstLON);
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
            throw new InterpolatorException(ex);
        } catch (IOException ex) {
            throw new InterpolatorException(ex);
        }

        return md5;
    }
    
    public void prepare() throws InterpolatorException {
         try
         {
            if (weightsFilename!=null && weightsFilename.isEmpty()==false) {
                FileInputStream fileIn = new FileInputStream(weightsFilename);
                System.out.println("Reading weights:"+weightsFilename);
                ObjectInputStream in = new ObjectInputStream(fileIn);

                dst2src = (Dst2Src) in.readObject();

                in.close();
                fileIn.close();
                System.out.println("Done!");
            }
        }catch(FileNotFoundException ex) {
            //try {
                /*
                FileWriter fw = new FileWriter("/Users/raffaelemontella/tmp/myocean2roms/radar/dst2src.csv");
                PrintWriter pw = new PrintWriter(fw,true);

                pw.println("LON;LAT;srcJ;srcI");
                */
                for (int dstJ=0;dstJ<dstLAT.length;dstJ++) {
                    System.out.println(Math.round(100*(double)dstJ/(double)dstLAT.length));
                    for (int dstI=0;dstI<dstLON[0].length;dstI++) {
                        //if (dstMASK[dstJ][dstI]==1) {
                            double dstLon = dstLON[dstJ][dstI];
                            double dstLat = dstLAT[dstJ][dstI];

                            for (int srcJ=0;srcJ<srcLAT.length-1;srcJ++) {


                                for (int srcI=0;srcI<srcLON[0].length-1;srcI++) {

                                    double[] lats = { srcLAT[srcJ][srcI],srcLAT[srcJ][srcI+1],srcLAT[srcJ+1][srcI],srcLAT[srcJ+1][srcI+1] };
                                    double[] lons = { srcLON[srcJ][srcI],srcLON[srcJ][srcI+1],srcLON[srcJ+1][srcI],srcLON[srcJ+1][srcI+1] };
                                    double minLat=lats[0];
                                    double minLon=lons[0];
                                    double maxLat=lats[0];
                                    double maxLon=lons[0];

                                    for (int k=1;k<4;k++) {
                                        if (lats[k]<minLat) minLat=lats[k];
                                        if (lats[k]>maxLat) maxLat=lats[k];
                                        if (lons[k]<minLon) minLon=lons[k];
                                        if (lons[k]>maxLon) maxLon=lons[k];

                                    }


                                    if (dstLon>=minLon && dstLon<maxLon && dstLat>=minLat && dstLat<maxLat) {
                                        dst2src.setJI(dstJ,dstI,srcJ,srcI);
                                        

                                        // pw.println(dstLon+";"+dstLat+";"+srcJ+";"+srcI);
                                        srcI=srcLON[0].length;
                                        srcJ=srcLAT.length;
                                    } 
                                }
                            }
                        //} else {
                        //    dst2src.setInvalidJI(dstJ,dstI);
                        //}


                    }
                }
/*
                pw.close();
                fw.close();        
            } catch (IOException ex1) {
                throw new InterpolatorException(ex1);
            }*/
            if (weightsFilename!=null && weightsFilename.isEmpty()==false) {
                System.out.println("Writing weights:"+weightsFilename);
                try {
                    FileOutputStream fileOut = new FileOutputStream(weightsFilename);
                    ObjectOutputStream out = new ObjectOutputStream(fileOut);
                    out.writeObject(dst2src);
                    out.close();
                    fileOut.close();
                } catch (IOException ex1) {
                    throw new InterpolatorException(ex1);
                }
                System.out.println("Done");
            }
        } catch (IOException ex) {
            throw new InterpolatorException(ex);
        } catch (ClassNotFoundException ex) {
            throw new InterpolatorException(ex);
        }  
         
        for (int dstJ=0;dstJ<dstLAT.length;dstJ++) {
            for (int dstI=0;dstI<dstLON[0].length;dstI++) {
                if (dst2src.isValidJI(dstJ, dstI)==true) {
                    dstMASK[dstJ][dstI]=1;
                }
            }
        } 
    }

    @Override
    public double[][] interp(double[][] src, double srcMissingValue, double dstMissingValue, double[] params) throws InterpolatorException {
        double[][] dst = new double[dstLAT.length][dstLON[0].length];

        for (int dstJ=0;dstJ<dstLAT.length;dstJ++) {
            for (int dstI=0;dstI<dstLON[0].length;dstI++) {
                dst[dstJ][dstI]=dstMissingValue;
                if (dstMASK[dstJ][dstI]==1) {
                    dst[dstJ][dstI]=dst2src.getValueJI(src,srcMASK,dstJ,dstI);
                    if (Double.isNaN(dst[dstJ][dstI])==true) {
                        dst[dstJ][dstI]=dstMissingValue;
                    } 
                } 
            }
        }
        
        return dst;
    }
}
