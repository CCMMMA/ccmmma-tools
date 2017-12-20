/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jncregridder.util;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author raffaelemontella
 */
public class InverseDistanceWeighting {
    
    private static double MACHINE_D_EPSILON;
    private String weightsFilename = null;
    
    protected void initMachineDEpsilon() {
        float fTmp = 0.5f;
        double dTmp = 0.5d;
        while( 1 + fTmp > 1 )
            fTmp = fTmp / 2;
        while( 1 + dTmp > 1 )
            dTmp = dTmp / 2;
        MACHINE_D_EPSILON = dTmp;
        
    }
    
    private final static double D_TOLERANCE = MACHINE_D_EPSILON * 10d;
    
    /**
     *A tolerance.
     */
    private static final double TOLL = 1.0d * 10E-8;
    
    private double radius = 1;
    public void setRadius(double radius) { this.radius=radius; }
    public double getRadius() { return radius; } 

    
   
    protected double pVariance = 0;

    
    // The collection of the measurement point, containing the position of the station.")
    private Stations srcStations = null;
    public void setSrcStations(Stations stations) { this.srcStations=stations; }
    
    
    
    // The measured data, to be interpolated.")
    private IdDoubleVectData srcData = null;
    public void setSrcData(IdDoubleVectData srcData) { this.srcData=srcData; }

    // The collection of the points in which the data needs to be interpolated.")
    private Stations dstStations = null;
    public void setDstStations(Stations stations) { this.dstStations=stations; }
    
    
    // The interpolated data.")
    private IdDoubleVectData dstData = null;
    public IdDoubleVectData getDstData() { return dstData; }
    
    private IDWw idww = null;
    
    public InverseDistanceWeighting() {
        srcStations=null;
        srcData=null;
        dstStations=null;
        dstData=new IdDoubleVectData();
        pVariance=Double.NaN;
        initMachineDEpsilon();
    }
    
    public InverseDistanceWeighting(Stations srcStations, IdDoubleVectData srcData, Stations dstStations, IdDoubleVectData dstData ) {
        
        this.srcStations=srcStations;
        this.srcData=srcData;
        this.dstStations=dstStations;
        this.dstData=dstData;
        pVariance= getVariance();
        initMachineDEpsilon();
    }
    
    
     
   private double[] getDataArrayByIndex(Integer idx) {
        int size = srcData.size();
        double[] result = new double[size];
        int i=0;
        Collection<double[]> tmp=srcData.values();
        Iterator<double[]> iTmp=tmp.iterator();
        while (iTmp.hasNext()) {
            result[i]=iTmp.next()[idx];
            i++;
        }
        
        return result;
    }
    
    public double getStdDev()
    {
        return Math.sqrt(getVariance());
    }
    
    public double getMean()
    {
        double[] values=getDataArrayByIndex(0);
        int count=0;
        double sum = 0.0;
        for(double a : values) {
            if (Double.isNaN(a)==false) {
                sum += a;
                count++;
            }
        }
        return sum/count;
    }
    
     public double getMin() throws InverseDistanceWeightingException
    {
        double[] values=getDataArrayByIndex(0);
        if (values==null) throw new InverseDistanceWeightingException("The values array is null!");
        
        double result = Double.NaN;
        for(double a : values) {
            if (Double.isNaN(a)==false) {
                if (Double.isNaN(result)==true) {
                    result=a;
                } else {
                    if (a<result) {
                        result=a;
                    }
                }
                
            }
        }
        if (Double.isNaN(result)==true) throw new InverseDistanceWeightingException("The min is NaN!");
        return result;
    }
     
     public double getMax() throws InverseDistanceWeightingException
    {
        double[] values=getDataArrayByIndex(0);
        if (values==null) throw new InverseDistanceWeightingException("The values array is null!");
        
        double result = Double.NaN;
        for(double a : values) {
            if (Double.isNaN(a)==false) {
                if (Double.isNaN(result)==true) {
                    result=a;
                } else {
                    if (a>result) {
                        result=a;
                    }
                }
                
            }
        }
        if (Double.isNaN(result)==true) throw new InverseDistanceWeightingException("The max is NaN!");
        return result;
    }

    public double getVariance()
    {
        double[] values=getDataArrayByIndex(0);
        int count=0;
        double mean = getMean();
        double temp = 0;
        for(double a :values) {
            if (Double.isNaN(a)==false) {
                temp += (mean-a)*(mean-a);
                count++;
            }
            
        }
        return temp/count;
    }
    
    private static boolean isNovalue(double v) {
        return Double.isNaN(v);
    }
    
    private static boolean dEq( double a, double b ) {
        if (Double.isNaN(a) && Double.isNaN(b)) {
            return true;
        }
        double diffAbs = Math.abs(a - b);
        return a == b ? true : diffAbs < D_TOLERANCE ? true : diffAbs / Math.max(Math.abs(a), Math.abs(b)) < D_TOLERANCE;
    }
    
    

   
  
   
    void createWeights() throws InverseDistanceWeightingException {
        
        
        try
         {
             if (weightsFilename!=null && weightsFilename.isEmpty()==false) {
            FileInputStream fileIn =
                          new FileInputStream(weightsFilename);
            System.out.println("Reading weights:"+weightsFilename);
            ObjectInputStream in = new ObjectInputStream(fileIn);
                
            idww = (IDWw) in.readObject();
                 
            in.close();
            fileIn.close();
            System.out.println("Done!");
             }
        }catch(FileNotFoundException ex)
        {
            System.out.println("Computing weights...");
        idww = new IDWw(dstStations.size());
        
        double d,d2;
        double r2=radius*radius;
        
        //String fileId=dstStations.toString()+"-"+srcStations.toString()+"-"+radius;
        //System.out.println(fileId);
        
        Iterator<Station> iDstStations = dstStations.iterator();
        while (iDstStations.hasNext()) {
            Station dstStation = iDstStations.next();
            //System.out.println("Dest station: "+ dstStation.id+ "/"+dstStations.get(dstStations.size()-1).id);
            double[] wRow0= new double[srcStations.size()];
            
            
            double dS=0;
            Iterator<Station> iSrcStations = srcStations.iterator();
            //Stations tmpSrcStaions = new Stations();
            while( iSrcStations.hasNext() ) {
                Station srcStation = iSrcStations.next();
                d2=
                        (srcStation.x-dstStation.x)*(srcStation.x-dstStation.x)+
                        (srcStation.y-dstStation.y)*(srcStation.y-dstStation.y)
                        ;
                if (d2<=r2) {
                    d=Math.pow(d2, .5);
                    //idww.put(dstStation.id+";"+srcStation.id, d);
                    wRow0[srcStation.id]=d;
                    dS+=1/d;
                    //tmpSrcStaions.add(srcStation);
                    
                } else wRow0[srcStation.id]=0;
                
                
            }
            
            //System.out.println("Normalization..."+tmpSrcStaions.size());
            double wS=0,nw;
            /*
            iSrcStations = tmpSrcStaions.iterator();
            while( iSrcStations.hasNext() ) {
                Station srcStation = iSrcStations.next();
                Double w = idww.get(dstStation.id+";"+srcStation.id);
                if (w!=null) {
                    nw = (1/w)/dS;
                    idww.put(dstStation.id+";"+srcStation.id, nw );
                    wS+=nw;
                }
            }
            */
            
            for (int iRow0=0;iRow0<wRow0.length;iRow0++) {
                if (wRow0[iRow0]>0) {
                    nw=(1/wRow0[iRow0])/dS;
                    wRow0[iRow0]=nw;
                    wS+=nw;
                    
                }
            }
            
            if (wS<1-TOLL || wS>1+TOLL) throw new InverseDistanceWeightingException("Bad w! "+wS);
            //System.out.println("Done!");
            
            idww.setRow(dstStation.id,wRow0);
            
        }
        if (weightsFilename!=null && weightsFilename.isEmpty()==false) {
        System.out.println("Writing weights:"+weightsFilename);
        try {
        FileOutputStream fileOut =
         new FileOutputStream(weightsFilename);
         ObjectOutputStream out =
                            new ObjectOutputStream(fileOut);
         out.writeObject(idww);
         out.close();
          fileOut.close();
        } catch (IOException ex1) {
                    throw new InverseDistanceWeightingException(ex1);
                }
          System.out.println("Done");
        }
        } catch (IOException ex) {
                    throw new InverseDistanceWeightingException(ex);
                } catch (ClassNotFoundException ex) {
                    throw new InverseDistanceWeightingException(ex);
                }  
    }

    void execute() throws InverseDistanceWeightingException {
        if (idww==null) throw new InverseDistanceWeightingException("No Weights!");
        pVariance= getVariance();

        /*
         * Store the station coordinates and measured data in the array.
         */
        Iterator<Station> iDstStations = dstStations.iterator();
        
        
        int c=0;
        
        System.out.println("execute...");
        while( iDstStations.hasNext() ) {
            double sum = 0.;
            Station dstStation = iDstStations.next();
            //System.out.println("Dest station: "+ dstStation.id+ "/"+dstStations.get(dstStations.size()-1).id);
            /*
            if ((c % 1000) == 0) {
                System.out.println("idw.atwork "+c+"/"+dstStations.size());
            }
             */
            double h0 = 0.0;
            double v=0;
            
            Integer[] indexes = idww.getIndexes(dstStation.id);
            Double[] weights = idww.getWeights(dstStation.id);
            
            for(int i=0;i<indexes.length;i++) {
                v = srcData.get(indexes[i])[0];
                h0 = h0 + weights[i] * v;
                sum = sum + weights[i];
            }        
            /*
            Iterator<Station> iSrcStations = srcStations.iterator();
            while( iSrcStations.hasNext() ) {
                Station srcStation = iSrcStations.next();
                Double w = idww.get(dstStation.id+";"+srcStation.id);
                if (w!=null) {
                    v = srcData.get(srcStation.id)[0];
                    h0 = h0 + w * v;
                    sum = sum + w;
                }
            }
             
             */
            double[] result = new double[1];
            result[0]=h0;
            dstData.put(dstStation.id, result );
            
            //System.out.println("id:"+dstStation.id+" x:"+dstStation.x+" y:"+dstStation.y+" z:"+dstStation.z+" h:"+h0);
            c++;
        }
        //System.out.println("done...");
        
        
    }

    void setWeightsFilename(String weightsFilename) {
        this.weightsFilename = weightsFilename;
    }

    
}
