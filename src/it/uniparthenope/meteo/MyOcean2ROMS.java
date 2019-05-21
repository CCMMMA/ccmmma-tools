/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.uniparthenope.meteo;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

import jncregridder.data.ICurrent;
import jncregridder.data.IPotentialTemperature;
import jncregridder.data.ISalinity;
import jncregridder.data.ISeaSurfaceHeight;
import jncregridder.data.copernicus.CopernicusCur;
import jncregridder.data.copernicus.CopernicusSSH;
import jncregridder.data.copernicus.CopernicusSal;
import jncregridder.data.copernicus.CopernicusTem;
import jncregridder.data.myocean.MyOceanCur;
import jncregridder.data.myocean.MyOceanSSH;
import jncregridder.data.myocean.MyOceanSal;
import jncregridder.data.myocean.MyOceanTem;
import jncregridder.roms.ROMSBoundary;
import jncregridder.roms.ROMSGrid;
import jncregridder.roms.ROMSInit;
import jncregridder.roms.ROMSUtil;
import jncregridder.util.*;
import ucar.ma2.InvalidRangeException;

/**
 *
 * @author raffaelemontella
 */
public class MyOcean2ROMS {

    private boolean DEBUG=false;

    private static final int MYOCEAN=0;
    private static final int COPERNICUS=1;
    private static final int ROMS=2;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, NCRegridderException, KrigingException, NoSuchAlgorithmException, InterpolatorException, InvalidRangeException { // Diana

        /*
        new MyOcean2ROMS(
                "/Users/raffaelemontella/dev/ccmmma/roms/data/Campania_max200m_withC3andC4.nc",
                "/Users/raffaelemontella/dev/ccmmma/roms/tmp/myocean2roms/data/",
                "20181215",
                "d03",
                "/Users/raffaelemontella/dev/ccmmma/roms/init/ini-d03.nc",
                "/Users/raffaelemontella/dev/ccmmma/roms/init/bry-d03.nc",
                2,
                "/Users/raffaelemontella/dev/ccmmma/roms/csv/",
                1,
                1,
                1
        );
*/


        try {
            
            if (args.length != 11) {
                System.out.println("Usage:");
                System.out.println("MyOcean2ROMS gridPath dataPath YYYYMMDD domainId initFilename boundaryFilename method csvPath subset2D subset3D format");
                System.out.println("method: 0 -Kriging; 1-Fast Kriging; 2-Bilinear (suggested); 3-Bicubic; 4-IDW");
                System.out.println("format: 0 -MyOcean; 1-Copernicus");
                System.exit(0);
            }
            new MyOcean2ROMS(args[0],args[1],args[2],args[3],args[4],args[5], Integer.parseInt(args[6]),args[7],Double.parseDouble(args[8]),Double.parseDouble(args[9]),Integer.parseInt(args[10]));
        } catch (InvalidRangeException ex) {// Diana
            Logger.getLogger(MyOcean2ROMS.class.getName()).log(Level.SEVERE, null, ex); // Diana
        }


    }
    
    private ROMSBoundary romsBoundary=null; 
    private ROMSInit romsInit=null;
    
    public MyOcean2ROMS(String gridPath, String dataPath, String ncepDate, String domainId, String initPath, String boundaryPath, int method, String csvPath, double subset2D, double subset3D,int format ) throws IOException, NCRegridderException, InvalidRangeException, KrigingException, NoSuchAlgorithmException, InterpolatorException { // Diana aggiunta eccezioni
        
        
        // Path to the foms grid
        String romsGridPath  = gridPath;

        // Path to the myocean current data
        String myOceanPathCur= dataPath+"/myoc_d00_"+ncepDate+"_cur.nc";

        // Path to the myocean temperature data
        String myOceanPathTem= dataPath+"/myoc_d00_"+ncepDate+"_tem.nc";

        // Path to the myocean salinity data
        String myOceanPathSal= dataPath+"/myoc_d00_"+ncepDate+ "_sal.nc";

        // Path to the myocean sea surface height data
        String myOceanPathSSH= dataPath+"/myoc_d00_"+ncepDate+"_ssh.nc";

        // Path to the output init file
        String romsInitPath  = initPath;

        // Path to the output boundary file
        String romsBoundaryPath = boundaryPath;
        
        try {
            
            // Open ROMS grid data
            ROMSGrid romsGrid = new ROMSGrid(romsGridPath);

            // Get dimension size
            int etaRho = romsGrid.dimEtaRho.getLength();
            int xiRho = romsGrid.dimXiRho.getLength();
            int etaU = romsGrid.dimEtaU.getLength();
            int xiU = romsGrid.dimXiU.getLength();
            int etaV = romsGrid.dimEtaV.getLength();
            int xiV = romsGrid.dimXiV.getLength();

            // Perform some output
            System.out.println("Rho:\n\teta:"+etaRho+"\txi:"+xiRho);
            System.out.println("U:\n\teta:"+etaU+"\txi:"+xiU);
            System.out.println("V:\n\teta:"+etaV+"\txi:"+xiV);
            
            
            
           
            // MASK at rho points
            double[][] MASKRHO = romsGrid.getMASKRHO();
            if (MASKRHO==null) {
                throw new NCRegridderException("MASKRHO is null");
            }
            
            // MASK at u points
            double[][] MASKU = romsGrid.getMASKU();
            if (MASKU==null) {
                throw new NCRegridderException("MASKU is null");
            }
            
            // MASK at v points
            double[][] MASKV = romsGrid.getMASKV();
            if (MASKV==null) {
                throw new NCRegridderException("MASKV is null");
            }
            
            // Binary masks
            int[][] dstMASKRHO = new int[etaRho][xiRho];
            int[][] dstMASKU = new int[etaU][xiU];
            int[][] dstMASKV = new int[etaV][xiV];
            
            
            // Preparing rho destination mask
            for (int j=0;j<etaRho;j++) {
                for (int i=0;i<xiRho;i++) {
                    if (MASKRHO[j][i]==1) {
                        dstMASKRHO[j][i]=1;
                    } else {
                        dstMASKRHO[j][i]=0;
                    }
                }
            }
            
            // Preparing u destination mask
            for (int j=0;j<etaU;j++) {
                for (int i=0;i<xiU;i++) {
                    if (MASKU[j][i]==1) {
                        dstMASKU[j][i]=1;
                    } else {
                        dstMASKU[j][i]=0;
                    }
                }
            }
            
            // Preparing v destination mask
            for (int j=0;j<etaV;j++) {
                for (int i=0;i<xiV;i++) {
                    if (MASKV[j][i]==1) {
                        dstMASKV[j][i]=1;
                    } else {
                        dstMASKV[j][i]=0;
                    }
                }
            }
            
            
            
            // LAT at rho points
            double[][] LATRHO = romsGrid.getLATRHO();
            
            // LON at rho points
            double[][] LONRHO = romsGrid.getLONRHO();
            
            // Z at rho/sigma pints 
            double[][][] romsZ = romsGrid.getZ();
            
            
            // LAT,LON at u points
            double[][] LATU = romsGrid.getLATU();
            double[][] LONU = romsGrid.getLONU();
            
            // LAT,LON at v points
            double[][] LATV = romsGrid.getLATV();
            double[][] LONV = romsGrid.getLONV();

            // Get the angle between the xi axis and the real east
            double[][] ANGLE = romsGrid.getANGLE();

            // Perform some output
            System.out.println("LATU:"+LATU.length+";"+LATU[0].length);
            System.out.println("LONU:"+LONU.length+";"+LONU[0].length);
            System.out.println("LATV:"+LATV.length+";"+LATV[0].length);
            System.out.println("LONV:"+LONV.length+";"+LONV[0].length);
            System.out.println("ANGLE:"+ANGLE.length+";"+ANGLE[0].length);

            // A generic counter
            int count=0;

            FileWriter fw;
            PrintWriter pw;

            if (DEBUG) {
                // Just for debugging/controlling:
                // For each sigma level, the depths for each lat and long
                // Write a csv file with LAT, LON, DEPTH
                for (int k = 0; k < romsZ.length; k++) {
                    String fileName = csvPath + "/sigma-" + k + ".csv";
                    System.out.println("DEBUG: sigma file -- " + fileName);

                    fw = new FileWriter(fileName);
                    pw = new PrintWriter(fw);
                    pw.println("LON;LAT;DEPTH");
                    for (int j = 0; j < romsZ[k].length; j++) {
                        for (int i = 0; i < romsZ[k][j].length; i++) {
                            pw.println(LONRHO[j][i] + ";" + LATRHO[j][i] + ";" + romsZ[k][j][i]);
                        }
                    }
                    pw.flush();
                    pw.close();
                    fw.close();
                }
            }

            // To be really generic, we use interfaces to:

            // ...potential temperature
            IPotentialTemperature dataTem=null;

            // ...salinity
            ISalinity dataSal=null;

            // ...sea surface height
            ISeaSurfaceHeight dataSSH=null;

            // ...current
            ICurrent dataCur=null;


            // The variable format could have the following enumerative values:
            // MYOCEAN    -- Old myocean format
            // COPERNICUS -- New copernicus format
            // ROMS       -- for offline nesting, not yet implemented

            switch (format) {
                case MYOCEAN:
                    dataTem=new MyOceanTem(myOceanPathTem);
                    dataSal = new MyOceanSal(myOceanPathSal);
                    dataSSH = new MyOceanSSH(myOceanPathSSH);
                    dataCur = new MyOceanCur(myOceanPathCur);
                    break;

                case COPERNICUS:
                    dataTem=new CopernicusTem(myOceanPathTem);
                    dataSal = new CopernicusSal(myOceanPathSal);
                    dataSSH = new CopernicusSSH(myOceanPathSSH);
                    dataCur = new CopernicusCur(myOceanPathCur);
                    break;

                case ROMS:
                    break;

                default:
                    dataTem=new MyOceanTem(myOceanPathTem);
                    dataSal = new MyOceanSal(myOceanPathSal);
                    dataSSH = new MyOceanSSH(myOceanPathSSH);
                    dataCur = new MyOceanCur(myOceanPathCur);
            }


            // LAT at XY points
            double[][] LATXY = dataCur.getLAT2();
            
            // LON at XY points
            double[][] LONXY = dataCur.getLON2();

            // Z at XY points
            double[][][] myOceanZ = dataCur.getZ();

            // Convert depth in positive up values (meaning: depths are negative)
            for (int k=0;k<myOceanZ.length;k++) {
                for (int j=0;j<myOceanZ[0].length;j++) {
                    for (int i=0;i<myOceanZ[0][0].length;i++) {
                        if (myOceanZ[k][j][i]!=dataCur.getUndefinedValue()) {
                            myOceanZ[k][j][i]=-Math.abs(myOceanZ[k][j][i]);
                        }
                    }
                }    
            }

            // Get the number of time frames
            double time[] = dataCur.getTIME();

            // Set the number of forcing time steps
            int forcingTimeSteps=time.length;

            // Instantiate a ROMS init file
            romsInit = new ROMSInit(romsInitPath,romsGrid,ncepDate,forcingTimeSteps); 

            // Create a vector of doubles sized as the number of forcing time steps
            double oceanTime[] = new double[forcingTimeSteps];

            // Set the value of each ocean time as delta form the simultation starting date
            for (int t=0;t<forcingTimeSteps;t++) {
                oceanTime[t]=romsInit.getModSimStartDate()+t;
            }

            // Set the scrum time
            double scrumTime[] = new double[forcingTimeSteps];
            for (int t=0;t<forcingTimeSteps;t++) {
                scrumTime[t]=t*86400;
            }

            // Initialize the ROMS init
            romsInit.setOceanTime(oceanTime);
            romsInit.setScrumTime(scrumTime);
            romsInit.make();

            // Instantiate a ROMS boundary file
            romsBoundary = new ROMSBoundary(romsBoundaryPath,romsGrid,ncepDate, forcingTimeSteps);

            // Initialize the ROMS boundary
            romsBoundary.setOceanTime(oceanTime);
            romsBoundary.make();

            // Limit the forcing time steps to 1 for debugging reasons
            if (DEBUG) {
                forcingTimeSteps = 1;
                System.out.println("WARING *** ONLY " + forcingTimeSteps + " TIME STEP IS CONSIDERED FOR DEBUGGING ***");
            }

            // For each time step in myocean data
            for (int t=0;t<forcingTimeSteps;t++) {
                System.out.println("Time:"+t+" "+oceanTime[t]);
                
                // Set the time step
                dataCur.setTime(t);
                dataTem.setTime(t);
                dataSal.setTime(t);
                dataSSH.setTime(t);
                
                // 2D sea surface height
                double[][] valuesSSH = dataSSH.getZeta();

                // Instantiate a source mask
                int[][] srcMASK = new int[LATXY.length][LATXY[0].length];

                // For each j...
                for (int j=0;j<LATXY.length;j++) {
                    // For each i...
                    for (int i=0;i<LATXY[0].length;i++) {

                        // Check if the value is not masked
                        if (Double.isNaN(valuesSSH[j][i])==false && valuesSSH[j][i]!=dataSSH.getUndefinedValue()) {
                            srcMASK[j][i]=1;
                        } else {
                            srcMASK[j][i]=0;
                        }
                    }
                }



                if (DEBUG) {

                    // Just for debugging/controlling:
                    // Write a csv file with LAT, LON, surface current magnitude
                    String fileName = csvPath+"/myocean-ssh.csv";
                    System.out.println("DEBUG: sigma file -- " + fileName);

                    fw = new FileWriter(fileName);
                    pw = new PrintWriter(fw);
                    pw.println("LON;LAT;HEIG");
                    for (int j=0;j<LATXY.length;j++) {
                        for (int i=0;i<LATXY[j].length;i++) {
                            double heig=valuesSSH[j][i];
                            if (Double.isNaN(heig)==false) pw.println(LONXY[j][i]+";"+LATXY[j][i]+";"+ heig);
                        }
                    }
                    pw.flush();
                    pw.close();
                    fw.close();
                }

                double[] pIntegralScale = {1,1,1};

                double[][] SSHE_ROMS= null;
                double[][][] TEM_ROMS = null;
                double[][][] SAL_ROMS = null;
                double[][][] U_ROMS = null;
                double[][][] V_ROMS = null;


                // 3D current U component
                double[][][] valuesU = dataCur.getCurU();

                // 3D current V component
                double[][][] valuesV = dataCur.getCurV();

                System.out.println("Rotating 3D UV...");
                for(int k=0;k<myOceanZ.length;k++) {
                    // Rotate each point
                    ROMSUtil.rotate(valuesU[k],valuesV[k],ANGLE,dataCur.getUndefinedValue());
                }
            
                // 3D temperature
                double[][][] valuesTem = dataTem.getTemp();

                // 3D salinity
                double[][][] valuesSal = dataSal.getSalt();

            
                System.out.println("Preparing 3D...");
            
            
                //int method=1;

                if (method==0) {
                    System.out.println("KRIGING INTERPOLATION");

                    InterpolatorParams params = new InterpolatorParams();
                    params.put("subset", subset2D);
                    KInterpolator kInterpolatorRho = new KInterpolator(LATXY,LONXY,LATRHO,LONRHO,srcMASK,dstMASKRHO,params);

                    kInterpolatorRho.setSubset(subset2D);
                    SSHE_ROMS=kInterpolatorRho.interp(valuesSSH,dataSSH.getUndefinedValue(),1e+37, null );


                    KInterpolator3D interpolator3DRho = new KInterpolator3D(LATXY,LONXY,myOceanZ,LATRHO,LONRHO,romsZ,srcMASK,dstMASKRHO,subset3D);
                    KInterpolator3D interpolator3DU = new KInterpolator3D(LATXY,LONXY,myOceanZ,LATU,LONU,romsZ,srcMASK,dstMASKU,subset3D);
                    KInterpolator3D interpolator3DV = new KInterpolator3D(LATXY,LONXY,myOceanZ,LATV,LONV,romsZ,srcMASK,dstMASKV,subset3D);



                    TEM_ROMS = interpolator3DRho.interp(valuesTem,dataTem.getUndefinedValue(),1e+37,null);
                    SAL_ROMS = interpolator3DRho.interp(valuesSal,dataSal.getUndefinedValue(),1e+37,null); // Diana
                    U_ROMS = interpolator3DU.interp(valuesU,dataCur.getUndefinedValue(),1e+37,pIntegralScale);
                    V_ROMS = interpolator3DV.interp(valuesV,dataCur.getUndefinedValue(),1e+37,pIntegralScale);



                } else if (method==1) {

                    System.out.println("FAST KRIGING INTERPOLATION");

                    InterpolatorParams params = new InterpolatorParams();
                    params.put("subset", subset2D);
                    KInterpolator kInterpolatorRho = new KInterpolator(LATXY,LONXY,LATRHO,LONRHO,srcMASK,dstMASKRHO,params);

                    kInterpolatorRho.setSubset(subset2D);
                    SSHE_ROMS=kInterpolatorRho.interp(valuesSSH,dataSSH.getUndefinedValue(),1e+37, null );

                    FastKInterpolator3D interpolator3DRho = new FastKInterpolator3D(LATXY,LONXY,myOceanZ,LATRHO,LONRHO,romsZ,srcMASK,dstMASKRHO,subset3D);
                    FastKInterpolator3D interpolator3DU = new FastKInterpolator3D(LATXY,LONXY,myOceanZ,LATU,LONU,romsZ,srcMASK,dstMASKU,subset3D);
                    FastKInterpolator3D interpolator3DV = new FastKInterpolator3D(LATXY,LONXY,myOceanZ,LATV,LONV,romsZ,srcMASK,dstMASKV,subset3D);

                    System.out.println("Interpolating U");
                    U_ROMS = interpolator3DU.interp(valuesU,dataCur.getUndefinedValue(),1e+37,pIntegralScale);

                    System.out.println("Interpolating V");
                    V_ROMS = interpolator3DV.interp(valuesV,dataCur.getUndefinedValue(),1e+37,pIntegralScale);

                    System.out.println("Interpolating TEMP");
                    TEM_ROMS = interpolator3DRho.interp(valuesTem,dataTem.getUndefinedValue(),1e+37,null); // Diana
                    System.out.println("Interpolating SAL");
                    SAL_ROMS = interpolator3DRho.interp(valuesSal,dataSal.getUndefinedValue(),1e+37,null); // Diana





                } else if (method==2) {

                    System.out.println("BILINEAR INTERPOLATION");

                    // Create a 2D bilinear interpolator on Rho points
                    BilinearInterpolator bilinearInterpolatorRho = new BilinearInterpolator(LATXY,LONXY,LATRHO,LONRHO,srcMASK,dstMASKRHO);

                    // Interpolate the SSH
                    System.out.println("Interpolating SSH");
                    SSHE_ROMS=bilinearInterpolatorRho.interp(valuesSSH, dataSSH.getUndefinedValue(),1e+37, null );

                    // Create a 3D bilinear interpolator on Rho points
                    BilinearInterpolator3D interpolator3DRho = new BilinearInterpolator3D(LATXY,LONXY,myOceanZ,LATRHO,LONRHO,romsZ,srcMASK,dstMASKRHO);

                    // Create a 3D bilinear interpolator on U points
                    BilinearInterpolator3D interpolator3DU = new BilinearInterpolator3D(LATXY,LONXY,myOceanZ,LATU,LONU,romsZ,srcMASK,dstMASKU);

                    // Create a 3D bilinear interpolator on V points
                    BilinearInterpolator3D interpolator3DV = new BilinearInterpolator3D(LATXY,LONXY,myOceanZ,LATV,LONV,romsZ,srcMASK,dstMASKV);

                    System.out.println("Interpolating U");
                    U_ROMS = interpolator3DU.interp(valuesU,dataCur.getUndefinedValue(),1e+37,pIntegralScale);

                    System.out.println("Interpolating V");
                    V_ROMS = interpolator3DV.interp(valuesV,dataCur.getUndefinedValue(),1e+37,pIntegralScale);

                    System.out.println("Interpolating TEMP");
                    TEM_ROMS = interpolator3DRho.interp(valuesTem,dataTem.getUndefinedValue(),1e+37,null); // Diana
                    System.out.println("Interpolating SAL");
                    SAL_ROMS = interpolator3DRho.interp(valuesSal,dataSal.getUndefinedValue(),1e+37,null); // Diana





                } else if (method==3) {

                    System.out.println("BICUBIC INTERPOLATION");

                    BiCubicInterpolator biCubicInterpolatorRho = new BiCubicInterpolator(LATXY,LONXY,LATRHO,LONRHO,srcMASK,dstMASKRHO);

                    SSHE_ROMS=biCubicInterpolatorRho.interp(valuesSSH,dataSSH.getUndefinedValue(),1e+37, null );

                    BiCubicInterpolator3D interpolator3DRho = new BiCubicInterpolator3D(LATXY,LONXY,myOceanZ,LATRHO,LONRHO,romsZ,srcMASK,dstMASKRHO);
                    BiCubicInterpolator3D interpolator3DU = new BiCubicInterpolator3D(LATXY,LONXY,myOceanZ,LATU,LONU,romsZ,srcMASK,dstMASKU);
                    BiCubicInterpolator3D interpolator3DV = new BiCubicInterpolator3D(LATXY,LONXY,myOceanZ,LATV,LONV,romsZ,srcMASK,dstMASKV);

                    System.out.println("Interpolating U");
                    U_ROMS = interpolator3DU.interp(valuesU,dataCur.getUndefinedValue(),1e+37,pIntegralScale);

                    System.out.println("Interpolating V");
                    V_ROMS = interpolator3DV.interp(valuesV,dataCur.getUndefinedValue(),1e+37,pIntegralScale);

                    System.out.println("Interpolating TEMP");
                    TEM_ROMS = interpolator3DRho.interp(valuesTem,dataTem.getUndefinedValue(),1e+37,null); // Diana
                    System.out.println("Interpolating SAL");
                    SAL_ROMS = interpolator3DRho.interp(valuesSal,dataSal.getUndefinedValue(),1e+37,null); // Diana

                } else if (method==4) {

                    System.out.println("IDW INTERPOLATION");

                    IDWInterpolator idwInterpolatorRho = new IDWInterpolator(LATXY,LONXY,LATRHO,LONRHO,srcMASK,dstMASKRHO);

                    SSHE_ROMS=idwInterpolatorRho.interp(valuesSSH,dataSSH.getUndefinedValue(),1e+37, null );

                    IDWInterpolator3D interpolator3DRho = new IDWInterpolator3D(LATXY,LONXY,myOceanZ,LATRHO,LONRHO,romsZ,srcMASK,dstMASKRHO);
                    IDWInterpolator3D interpolator3DU = new IDWInterpolator3D(LATXY,LONXY,myOceanZ,LATU,LONU,romsZ,srcMASK,dstMASKU);
                    IDWInterpolator3D interpolator3DV = new IDWInterpolator3D(LATXY,LONXY,myOceanZ,LATV,LONV,romsZ,srcMASK,dstMASKV);

                    System.out.println("Interpolating U");
                    U_ROMS = interpolator3DU.interp(valuesU,dataCur.getUndefinedValue(),1e+37,pIntegralScale);

                    System.out.println("Interpolating V");
                    V_ROMS = interpolator3DV.interp(valuesV,dataCur.getUndefinedValue(),1e+37,pIntegralScale);

                    System.out.println("Interpolating TEMP");
                    TEM_ROMS = interpolator3DRho.interp(valuesTem,dataTem.getUndefinedValue(),1e+37,null); // Diana
                    System.out.println("Interpolating SAL");
                    SAL_ROMS = interpolator3DRho.interp(valuesSal,dataSal.getUndefinedValue(),1e+37,null); // Diana

                }


                if (DEBUG) {
                    // Just for debugging/controlling:
                    // For each sigma level, the depths for each lat and long
                    // Write a csv file with LAT, LON, DEPTH
                    for (int k = 0; k < romsZ.length; k++) {
                        String fileName = csvPath + "/roms-" + k + ".csv";
                        System.out.println("DEBUG: roms file -- " + fileName);

                        fw = new FileWriter(fileName);
                        pw = new PrintWriter(fw);
                        pw.println("LON;LAT;TEM;SAL");
                        for (int j = 0; j < LATRHO.length; j++) {
                            for (int i = 0; i < LATRHO[j].length; i++) {
                                //System.out.println("k:"+k+" j:"+j+" i:"+i);
                                //double uv= Math.sqrt(
                                //        (U_ROMS[k][j][i]*U_ROMS[k][j][i])+
                                //        (V_ROMS[k][j][i]*V_ROMS[k][j][i]));


                                double tem = TEM_ROMS[k][j][i];
                                double sal = SAL_ROMS[k][j][i];

                                //if (Double.isNaN(uv)) uv=1e+37;
                                if (Double.isNaN(tem)) tem = 1e+37;
                                if (Double.isNaN(sal)) sal = 1e+37;

                                pw.println(LONRHO[j][i] + ";" + LATRHO[j][i] + ";" +
                                        tem + ";" + sal);
                            }
                        }
                        pw.flush();
                        pw.close();
                        fw.close();
                    }
                }


                // Instantiate UBAR and VBAR
                double[][] UBAR = new double[LATU.length][LONU[0].length];
                double[][] VBAR = new double[LATV.length][LONV[0].length];

            
                // Perform integration over the water column

                // For each j...
                for (int j=0;j<LATU.length;j++) {

                    // For each i...
                    for (int i=0;i<LONU[j].length;i++) {

                        // Check if the value must be computed
                        if (MASKRHO[j][i]==1) {

                            // Set the UBAR value to 0
                            UBAR[j][i]=0;

                            // Reset the count
                            count=0;

                            // For each sigma level k...
                            for (int k=0;k<romsZ.length;k++) {

                                // Check if the value is not undefined
                                if (U_ROMS[k][j][i]!=1e37) {

                                    // Add the U component to UBAR
                                    UBAR[j][i]+=U_ROMS[k][j][i];

                                    // Increment the counter
                                    count++;
                                }
                            }

                            // Check if the counter is more than 0
                            if (count>0) {

                                // Calculate the average value
                                UBAR[j][i]=UBAR[j][i]/count;
                            }
                        } else {
                            // Set the value to undefined
                            UBAR[j][i]=1e37;
                        }

                    }
                }

                // For each j...
                for (int j=0;j<LATV.length;j++) {

                    // For each i...
                    for (int i=0;i<LONV[j].length;i++) {

                        // Check if the value must be computed
                        if (MASKRHO[j][i]==1) {

                            // Set the VBAR value to 0
                            VBAR[j][i]=0;

                            // Reset the counter
                            count=0;

                            // For each sigma level k
                            for (int k=0;k<romsZ.length;k++) {

                                // Check if the value is not undefined
                                if (V_ROMS[k][j][i]!=1e37) {

                                    // Add the V component to VBAR
                                    VBAR[j][i]+=V_ROMS[k][j][i];

                                    // Increase the counter
                                    count++;
                                }
                            }

                            // Check if the counter is greater than 0
                            if (count>0) {

                                // Calculate the average value
                                VBAR[j][i]=VBAR[j][i]/count;
                            }
                        } else {
                            // Set the value to undefined
                            VBAR[j][i]=1e37;
                        }
                    }
                }
            

                //if (t==0) {
                System.out.println("Time:"+t+" Saving init file...");
                
                romsInit.setSALT(SAL_ROMS);
                romsInit.setTEMP(TEM_ROMS);
                romsInit.setZETA(SSHE_ROMS);
                romsInit.setUBAR(UBAR);
                romsInit.setVBAR(VBAR);
                romsInit.setU(U_ROMS);
                romsInit.setV(V_ROMS);
                
                romsInit.write(t);

                System.out.println("2D vars");
                System.out.println("\tZETA min:"+String.format("%4.2f",romsInit.getMin(ROMSInit.VARIABLE_ZETA))+" max:"+String.format("%4.2f",romsInit.getMax(ROMSInit.VARIABLE_ZETA)));
                System.out.println("\tUBAR min:"+String.format("%4.2f",romsInit.getMin(ROMSInit.VARIABLE_UBAR))+" max:"+String.format("%4.2f",romsInit.getMax(ROMSInit.VARIABLE_UBAR)));
                System.out.println("\tVBAR min:"+String.format("%4.2f",romsInit.getMin(ROMSInit.VARIABLE_VBAR))+" max:"+String.format("%4.2f",romsInit.getMax(ROMSInit.VARIABLE_VBAR)));

                System.out.println("3D vars");
                System.out.println("\tSALT min:"+String.format("%4.2f",romsInit.getMin(ROMSInit.VARIABLE_SALT))+" max:"+String.format("%4.2f",romsInit.getMax(ROMSInit.VARIABLE_SALT)));
                System.out.println("\tTEMP min:"+String.format("%4.2f",romsInit.getMin(ROMSInit.VARIABLE_TEMP))+" max:"+String.format("%4.2f",romsInit.getMax(ROMSInit.VARIABLE_TEMP)));
                System.out.println("\tU min:"+String.format("%4.2f",romsInit.getMin(ROMSInit.VARIABLE_U))+" max:"+String.format("%4.2f",romsInit.getMax(ROMSInit.VARIABLE_U)));
                System.out.println("\tV min:"+String.format("%4.2f",romsInit.getMin(ROMSInit.VARIABLE_V))+" max:"+String.format("%4.2f",romsInit.getMax(ROMSInit.VARIABLE_V)));

                    
                //}
                System.out.println("Time:"+t+" Saving bry file...");
                romsBoundary.setSALT(SAL_ROMS);
                romsBoundary.setTEMP(TEM_ROMS);
                romsBoundary.setZETA(SSHE_ROMS);

                romsBoundary.setUBAR(UBAR);
                romsBoundary.setVBAR(VBAR);
                romsBoundary.setU(U_ROMS);
                romsBoundary.setV(V_ROMS);
                
                romsBoundary.write(t);
                
            }
            
            romsInit.close();
            romsBoundary.close();
            
            
                
            
         } catch (IOException ex) {
            Logger.getLogger(MyOcean2ROMS.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NCRegridderException ex) {
            Logger.getLogger(MyOcean2ROMS.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // _________________________________________________________
        

                

    }


}
    



