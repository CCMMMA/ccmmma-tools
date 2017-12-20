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
import java.util.Vector;







class Weight implements Serializable {
    public double[] w = new double[4];
    public int[] II;
    public int[] JJ;
    public boolean masked=false;
}

 


/**
 *
 * @author raffaelemontella
 */
public class CustomInterpolator {
    private boolean cache=true;
    
    private double[][] srcLAT=null;
    private double[][] srcLONG=null;
    private double[][] dstLAT=null;
    private double[][] dstLONG=null;
    
    public double[][] getDstLAT() { return dstLAT; }
    public double[][] getDstLONG() { return dstLONG; }
    
    
    private Weight[][] weights=null;
    
    int srcSNDim = -1;
    int srcWEDim = -1;
    int dstSNDim = -1;
    int dstWEDim = -1;
    
    GridPolygons gridPolygons = null;
    
    public CustomInterpolator(double[][] srcLAT, double[][] srcLONG, double[][] dstLAT, double[][] dstLONG, boolean cache) throws NCRegridderException {
        this.cache=cache;
        
        if (srcLAT==null) throw new NCRegridderException("Source grid latitudes are null!");
        if (srcLONG==null) throw new NCRegridderException("Source grid lonfitudes are null!");
        if (dstLAT==null) throw new NCRegridderException("Destination grid latitudes are null!");
        if (dstLONG==null) throw new NCRegridderException("Destination grid lonfitudes are null!");
        
        this.srcLAT = srcLAT;
        this.srcLONG = srcLONG;
        this.dstLAT = dstLAT;
        this.dstLONG = dstLONG;
        
        srcSNDim = srcLAT.length;
        srcWEDim = srcLAT[0].length;
        
        dstSNDim = dstLAT.length;
        dstWEDim = dstLAT[0].length;
        
        if (cache==true) {
            
            String fileName = System.getProperty("java.io.tmpdir")+File.separator+getMD5()+".interp";
            try {
                System.out.println("Trying to recover "+fileName+" ...");
                load(fileName);
                System.out.println("...done!");
            } catch (FileNotFoundException ex) {
                System.out.println("...cache not found.");
            }
        }
    }
    
    public CustomInterpolator(String fileName) throws FileNotFoundException, NCRegridderException  {
        load(fileName);
    }
    
    public void load(String fileName) throws FileNotFoundException, NCRegridderException {
        FileInputStream fis = new FileInputStream(fileName);
        ObjectInputStream ois;
        try {
            ois = new ObjectInputStream(fis);
            try {
                srcLAT = (double[][])ois.readObject();
                srcLONG = (double[][])ois.readObject();
                dstLAT = (double[][])ois.readObject();
                dstLONG = (double[][])ois.readObject();
                
                srcSNDim = srcLAT.length;
                srcWEDim = srcLAT[0].length;

                dstSNDim = dstLAT.length;
                dstWEDim = dstLAT[0].length;

                weights = new Weight[dstSNDim][dstWEDim];


                for (int j=0;j<dstSNDim;j++) {
                    for (int i=0;i<dstWEDim;i++) {
                        Object obj = ois.readObject();
                        weights[j][i] = (Weight)obj;

                    }
                }


                ois.close();
                fis.close();
                
            } catch (ClassNotFoundException ex) {
                throw new NCRegridderException(ex);
            }
        } catch (IOException ex) {
            throw new NCRegridderException(ex);
        }
        
        
        
        
    }
    
    public void saveAs(String fileName) throws FileNotFoundException, IOException {
        FileOutputStream fos = new FileOutputStream(fileName);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(srcLAT);
        oos.writeObject(srcLONG);
        oos.writeObject(dstLAT);
        oos.writeObject(dstLONG);
        for (int j=0;j<dstSNDim;j++) {
            for (int i=0;i<dstWEDim;i++) {
                oos.writeObject(weights[j][i]);
            }
        }
        fos.close();
    }
    
    public void saveAsCSV(String fileName) throws IOException {
        FileWriter fw = new FileWriter(fileName);
        PrintWriter pw = new PrintWriter(fw);
        pw.println("LONG,LAT,w0,w1,w3,w3");
        for (int j=0;j<dstSNDim;j++) {
            for (int i=0;i<dstWEDim;i++) {
                Weight weight = weights[j][i];
                double xLat = dstLAT[j][i];
                double xLong = dstLONG[j][i];
                String out=xLong+","+xLat+",";
                if (weight.masked==true) {
                    out+="-99999,-99999,-99999,-99999";
                } else {
                    out+=(weight.w[0]+","+
                            weight.w[1]+","+
                            weight.w[2]+","+
                            weight.w[3]);
                }
                pw.println(out);
            }
        }
    }
    
    
    
    
    
    public void prepare() throws NCRegridderException  {
        
        GridPolygons gridPolygons = new GridPolygons(srcSNDim,srcWEDim,srcLAT,srcLONG);
        
        double filter = gridPolygons.getRadiusMax2();
        int span = 4;
        int method = 0;
        
        weights = new Weight[dstSNDim][dstWEDim];
        
        System.out.println("srcSNDim="+srcSNDim+ " dstSNDim="+dstSNDim);
        System.out.println("srcWEDim="+srcWEDim+ " dstWEDim="+dstWEDim);
        
        
        for (int j=0;j<dstSNDim;j++) {
            System.out.println("%"+((double)(j)/(double)dstSNDim)*100);
            for (int i=0;i<dstWEDim;i++) {
               
                double xLon = dstLONG[j][i];
                double xLat = dstLAT[j][i];
                
                int aMin,bMin,aMax,bMax;
                int[] abMinMax = gridPolygons.calculateSearchRange(xLon, xLat, filter,span, method);
                aMin=abMinMax[0];
                aMax=abMinMax[1];
                bMin=abMinMax[2];
                bMax=abMinMax[3];
                
                int iR=-1;
                int jR=-1;
                
                        
                for (int b=bMin;b<bMax;b++) {
                    for (int a=aMin;a<aMax;a++) {
                        GridPolygon cell = gridPolygons.get(b,a);

                        if (cell.contains(xLon,xLat)) {
                            iR=cell.i;
                            jR=cell.j;
                            b=bMax;
                            a=aMax;
                        }
                    }
                }
                
                
                Weight weight = new Weight();
                
                if (jR==-1 || iR==-1) {
                    weight.masked=true;
                } else {
                    weight.masked=false;
                    int[] II = new int[] { iR,iR+1,iR+1,iR };
                    int[] JJ = new int[] { jR,jR,jR+1,jR+1 };

                    weight.II = II;
                    weight.JJ = JJ;

                    double latIJ,lonIJ;

                    double[] d = new double[4];

                    double dS=0;
                    for ( int a=0;a<4;a++) {
               
                        latIJ = srcLAT[JJ[a]][II[a]];
                        lonIJ = srcLONG[JJ[a]][II[a]];

               
                        d[a] = Math.pow((latIJ-dstLAT[j][i])*(latIJ-dstLAT[j][i])+(lonIJ-dstLONG[j][i])*(lonIJ-dstLONG[j][i]),.5);
                        dS+=(1/d[a]);

                    }

                    for ( int a=0;a<4;a++) {
                        weight.w[a] = (1/(d[a]))/(dS);      
                        if (Double.isNaN(weight.w[a])) {
                            weight.w[a]=1;
                        }
                    }
                }
                weights[j][i]=weight;
            }
            
        }
        
        if (cache==true) {
            String fileName = System.getProperty("java.io.tmpdir")+File.separator+getMD5()+".interp";
            try {
                System.out.println("Saving cache in "+fileName+" ...");
                saveAs(fileName);
                System.out.println("...done!");
            } catch (FileNotFoundException ex) {
                throw new NCRegridderException(ex);
            } catch (IOException ex) {
                throw new NCRegridderException(ex);
            }
        }
    }
    
    public String getMD5() throws NCRegridderException {
        String md5 = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(srcLAT);
            oos.writeObject(srcLONG);
            oos.writeObject(dstLAT);
            oos.writeObject(dstLONG);
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
    
    public double[][] interp(double[][] src, double srcMissingValue,double dstMissingValue) throws NCRegridderException {
        return interp(src, srcMissingValue, dstMissingValue, null, null);
    }
    
    public double[][] interp(double[][] src, double srcMissingValue,double dstMissingValue,double[][] mask, boolean bFix) throws NCRegridderException {
        // Create a vector of Not a number points
        Vector<Point2D> nanPoints =  null;
        
        if (bFix==true) nanPoints = new Vector<Point2D>();
        
        // Interpolate
        double[][] work = interp(src, srcMissingValue, dstMissingValue,mask, nanPoints);
        
        // fix
        if (bFix==true) fix(work,dstMissingValue,nanPoints);
        return work;
    }
    
    public double[][] interp(double[][] src, double srcMissingValue, double dstMissingValue, double[][] mask, Vector<Point2D> nanPoints) throws NCRegridderException {
        if (src==null) throw new NCRegridderException("Can't interpolate null variables!");
        if (weights==null) {
            prepare();
        }
        if (weights==null) throw new NCRegridderException("Can't interpolate without weights!");
        double[][] dst = new double[dstSNDim][dstWEDim];
        
        
        for (int j=0;j<dstSNDim;j++) {
            for (int i=0;i<dstWEDim;i++) {
                if (mask!=null && mask[j][i]==0) {
                    dst[j][i]=dstMissingValue;
                } else {
                    if (weights[j][i].masked==false) {
                        double value = 0;
                        int nNans=0;

                        for (int a=0;a<4;a++) {
                            int ii=weights[j][i].II[a];
                            int jj=weights[j][i].JJ[a];
                            double ww=weights[j][i].w[a];

                            if (Double.isNaN(ww)) {
                                String msg=String.format("NaN! j=%d i=%d w[%d]=%f",
                                        j,i,a,ww);
                                throw new NCRegridderException(msg);
                            }

                            double srcValue=src[ jj ][ ii ];

                            if (Double.isNaN(srcValue)==false && srcValue!=srcMissingValue) {
                                value += ww * srcValue;

                            } else {
                                nNans++;
                            }
                        }
                        if (nNans>0) {

                            value = 0;
                            int nCount=0;
                            for (int a=0;a<4;a++) {
                                int ii=weights[j][i].II[a];
                                int jj=weights[j][i].JJ[a];
                                double srcValue=src[ jj ][ ii ];

                                if (Double.isNaN(srcValue)==false && srcValue!=srcMissingValue) {
                                    value += srcValue;
                                    nCount++;
                                } 
                            }
                            if (nCount>0) {
                                value=value/nCount;
                                // System.out.println("j="+j+" i="+i+" backup:"+value);
                            } else {
                                value=dstMissingValue;
                            }

                        } else {
                            // System.out.println("j="+j+" i="+i+" regular:"+value);
                        }

                        if (value==dstMissingValue) {
                            
                            //System.out.println("NaN!!!");
                            if (nanPoints!=null && mask!=null) {
                                if (mask[j][i]==1) {
                                    System.out.println("j="+j+" i="+i+" Zella:"+dstMissingValue);
                                    Point2D nanPoint=new Point2D(j, i, dstLAT[j][i], dstLONG[j][i], value);
                                    nanPoints.add(nanPoint);
                                }
                            }


                        }
                        dst[j][i]=value;
                    } else {
                        // System.out.println("masked");
                        dst[j][i]=dstMissingValue;
                    }
                }
            }
        }
        
        return dst;
    }
    
    public Vector<Point2D> fix(double[][] work, double missingValue, Vector<Point2D>  nanPoints) {
        Vector<Point2D>  toFix0 = null;
        Vector<Point2D>  toFix = nanPoints;
        
        // Do until there are still points to fix or
        // until there are no fixable points
        do {
            
            toFix0=toFix;
            toFix=null;
            System.out.println(toFix0.size()+" zelles to fix...");
        
            for (Point2D point2D:toFix0) {
                int i0=point2D.getI();
                int j0=point2D.getJ();
                int l2=1;

                double value=0;
                int nCount=0;

                int jMin=j0-l2;
                int jMax=j0+l2;
                int iMin=i0-l2;
                int iMax=i0+l2;

                if (jMin<0) jMin=0;
                if (iMin<0) iMin=0;
                if (jMax>work.length-1) jMax=work.length-1;
                if (iMax>work[0].length-1) iMax=work[0].length-1;

                for(int j=jMin;j<jMax;j++) {
                    for(int i=iMin;i<iMax;i++) {
                        //System.out.println("j="+j+" i="+i);
                        if (work[j][i]!=missingValue) {
                            //System.out.println("work="+work[j][i]);
                            value=+work[j][i];
                            nCount++;
                        } 
                    }
                }
                if (nCount>0) {
                    //System.out.print("nCount:"+nCount+" value:"+value);
                    work[j0][i0]=value/nCount;
                    //System.out.println(" work["+j0+"]["+i0+"]="+work[j0][i0]);

                } else {
                    if (toFix==null) toFix=new Vector<Point2D>();
                    toFix.add(point2D);
                }
            }

            if (toFix!=null) System.out.println("Before:"+toFix0.size()+"... Still "+toFix.size()+" zelles left!");
        } while (toFix!=null && toFix.size()<toFix0.size());
        
        return toFix;
    }
}
