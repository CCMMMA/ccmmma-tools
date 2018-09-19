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
import jncregridder.util.*;
import ucar.ma2.InvalidRangeException;

/**
 *
 * @author raffaelemontella
 */
public class MyOcean2ROMS {

    private static final int MYOCEAN=0;
    private static final int COPERNICUS=1;
    private static final int ROMS=2;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, NCRegridderException, KrigingException, NoSuchAlgorithmException, InterpolatorException, InvalidRangeException { // Diana

/*
        new MyOcean2ROMS(
                "/Users/raffaelemontella/dev/ccmmma/roms/data/Campania_200m_extended_v2.nc",
                "/Users/raffaelemontella/dev/ccmmma/roms/myocean/20180731",
                "20180731",
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
        
        
        
        String romsGridPath  = gridPath;
        String myOceanPathCur= dataPath+"/myoc_d00_"+ncepDate+"_cur.nc";
        String myOceanPathTem= dataPath+"/myoc_d00_"+ncepDate+"_tem.nc";
        String myOceanPathSal= dataPath+"/myoc_d00_"+ncepDate+ "_sal.nc";
        String myOceanPathSSH= dataPath+"/myoc_d00_"+ncepDate+"_ssh.nc";
        String romsInitPath  = initPath; // Diana
        String romsBoundaryPath = boundaryPath;
        
        try {
            
            // Open ROMS grid data
            ROMSGrid romsGrid = new ROMSGrid(romsGridPath);
            
            int etaRho = romsGrid.dimEtaRho.getLength();
            int xiRho = romsGrid.dimXiRho.getLength();
            int etaU = romsGrid.dimEtaU.getLength();
            int xiU = romsGrid.dimXiU.getLength();
            int etaV = romsGrid.dimEtaV.getLength();
            int xiV = romsGrid.dimXiV.getLength();
            
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
            
            double[][] ANGLE = romsGrid.getANGLE();
            
            System.out.println("LATU:"+LATU.length+";"+LATU[0].length);
            System.out.println("LONU:"+LONU.length+";"+LONU[0].length);
            System.out.println("LATV:"+LATV.length+";"+LATV[0].length);
            System.out.println("LONV:"+LONV.length+";"+LONV[0].length);
            System.out.println("ANGLE:"+ANGLE.length+";"+ANGLE[0].length);
            
            double angle=0;
            int count=0;
            for(int j=0;j<ANGLE.length;j++) {
                for(int i=0;i<ANGLE[j].length;i++) {
                    if (Double.isNaN(ANGLE[j][i])==false && ANGLE[j][i]!=1e20) {
                        angle+=ANGLE[j][i];
                        count++;
                    }
                }
            }
            angle=angle/count;
            System.out.println("Average grid angle:"+angle);
            
            
            // Just for debugging/controlling:
            // Write a csv file with LAT, LON, DEPTH
            for (int k=0;k<romsZ.length;k++) {
                FileWriter fw = new FileWriter(csvPath+"/sigma-"+k+".csv");
                PrintWriter pw = new PrintWriter(fw);
                pw.println("LON;LAT;DEPTH");
                for (int j=0;j<romsZ[k].length;j++) {
                    for (int i=0;i<romsZ[k][j].length;i++) {
                        pw.println(LONRHO[j][i]+";"+LATRHO[j][i]+";"+romsZ[k][j][i]);
                    }
                }
                pw.flush();
                pw.close();
                fw.close();
            }

            IPotentialTemperature dataTem=null;
            ISalinity dataSal=null;
            ISeaSurfaceHeight dataSSH=null;
            ICurrent dataCur=null;



            switch (format) {
                case MYOCEAN:
                    dataTem=new MyOceanTem(myOceanPathTem);
                    dataSal = new MyOceanSal(myOceanPathSal); // Diana
                    dataSSH = new MyOceanSSH(myOceanPathSSH); // Diana
                    dataCur = new MyOceanCur(myOceanPathCur);
                    break;

                case COPERNICUS:
                    dataTem=new CopernicusTem(myOceanPathTem);
                    dataSal = new CopernicusSal(myOceanPathSal); // Diana
                    dataSSH = new CopernicusSSH(myOceanPathSSH); // Diana
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
            
            for (int k=0;k<myOceanZ.length;k++) {
                for (int j=0;j<myOceanZ[0].length;j++) {
                    for (int i=0;i<myOceanZ[0][0].length;i++) {
                        if (myOceanZ[k][j][i]!=1e20) {
                            myOceanZ[k][j][i]=-myOceanZ[k][j][i];
                        }
                    }
                }    
            }
            
            double time[] = dataCur.getTIME();
            int forcingTimeSteps=time.length;
            
            
            
            romsInit = new ROMSInit(romsInitPath,romsGrid,ncepDate,forcingTimeSteps); 
            
            double oceanTime[] = new double[forcingTimeSteps];
            for (int t=0;t<forcingTimeSteps;t++) {
                oceanTime[t]=romsInit.getModSimStartDate()+t;
            }
            
            double scrumTime[] = new double[forcingTimeSteps];
            for (int t=0;t<forcingTimeSteps;t++) {
                scrumTime[t]=t*86400;
            }
            
            romsInit.setOceanTime(oceanTime);
            romsInit.setScrumTime(scrumTime);
            romsInit.make();
            
            romsBoundary = new ROMSBoundary(romsBoundaryPath,romsGrid,ncepDate, forcingTimeSteps);
            romsBoundary.setOceanTime(oceanTime);
            romsBoundary.make();
            //forcingTimeSteps=1;
            // For each time step in myocean data
            for (int t=0;t<forcingTimeSteps;t++) {
                System.out.println("Time:"+t+" "+oceanTime[t]);
                
                // Set the time step
                dataCur.setTime(t);
                dataTem.setTime(t);
                dataSal.setTime(t);
                dataSSH.setTime(t);
                
                // Surface current U component
                //double[][] SOZOSDX1 = dataCur.getSOZOSDX1();

                // Surface current V component
                //double[][] SOMESTDY = dataCur.getSOMESTDY();

                // Rotate 
            
                // 2D sea surface height 
                double[][] SOSSHEIG = dataSSH.getZeta(); //Diana

                int[][] srcMASK = new int[LATXY.length][LATXY[0].length];
                for (int j=0;j<LATXY.length;j++) {
                    for (int i=0;i<LATXY[0].length;i++) {
                        if (SOSSHEIG[j][i]!=1e20) {
                            srcMASK[j][i]=1;
                        } else {
                            srcMASK[j][i]=0;
                        }
                    }
                }

                FileWriter fw;
                PrintWriter pw;
            /*
                // Just for debugging/controlling:
                // Write a csv file with LAT, LON, surface current magnitude
                FileWriter fw = new FileWriter(csvPath+"/myocean-ssh.csv");
                PrintWriter pw = new PrintWriter(fw);
                pw.println("LON;LAT;HEIG");
                for (int j=0;j<LATXY.length;j++) {
                    for (int i=0;i<LATXY[j].length;i++) {
                        double srf=Math.sqrt(
                                (SOZOSDX1[j][i]*SOZOSDX1[j][i])+
                                (SOMESTDY[j][i]*SOMESTDY[j][i]));
                        double heig=SOSSHEIG[j][i];
                        if (Double.isNaN(srf)) srf=1e+20;
                        //if (Double.isNaN(heig)) heig=1e+20;
                        if (Double.isNaN(heig)==false) pw.println(LONXY[j][i]+";"+LATXY[j][i]+";"+
                                heig);
                    }
                }
                pw.flush();
                pw.close();
                fw.close();
            */
                double[] pIntegralScale = {1,1,1};

                double[][] SSHE_ROMS= null;
                double[][][] TEM_ROMS = null;
                double[][][] SAL_ROMS = null;
                double[][][] U_ROMS = null;
                double[][][] V_ROMS = null;

            
                //BiCubicInterpolator biCubicInterpolatorRho = new BiCubicInterpolator(LATXY,LONXY,LATRHO,LONRHO,srcMASK,dstMASKRHO);
                //SSHE_ROMS=biCubicInterpolatorRho.interp(SOSSHEIG,1e+20,1e+37, null );


                // Create a 2D interpolator
                // Source: myocean grid
                // Destination: roms grid
                //Interpolator interpolatorRho = new Interpolator(LATXY,LONXY,LATRHO,LONRHO,true);
                //Interpolator interpolatorU = new Interpolator(LATXY,LONXY,LATU,LONU,true);
                //Interpolator interpolatorV = new Interpolator(LATXY,LONXY,LATV,LONV,true);

                // Interpolate surface u-v components
                // usrfNanPoints / vsrfNanPoints: Array of points to be fixed
                //double[][] USRF_ROMS=interpolatorRho.interp(SOZOSDX1,1e+20,1e+37,MASKRHO,true);
                //double[][] VSRF_ROMS=interpolatorRho.interp(SOMESTDY,1e+20,1e+37,MASKRHO,true);
                //double[][] SSHE_ROMS=interpolatorRho.interp(SOSSHEIG,1e+20,1e+37,MASKRHO,true);

                // Just for debugging/controlling:
                // Write a csv file with LAT, LON, surface current magnitude
                /*
                fw = new FileWriter(csvPath+"/roms-ssh.csv");
                pw = new PrintWriter(fw);
                pw.println("LON;LAT;HEIG");
                for (int j=0;j<LATRHO.length;j++) {
                    for (int i=0;i<LATRHO[j].length;i++) {

                        if (Double.isNaN(SSHE_ROMS[j][i])==false) pw.println(LONRHO[j][i]+";"+LATRHO[j][i]+";"+SSHE_ROMS[j][i]);
                    }
                }
                pw.flush();
                pw.close();
                fw.close();
                */
            
            
                // 3D current U component
                double[][][] VOZOCRTX = dataCur.getCurU();

                // 3D current V component
                double[][][] VOMECRTY = dataCur.getCurV();

                System.out.println("Rotating 3D UV...");
                for(int k=0;k<myOceanZ.length;k++) {
                    rotate(VOZOCRTX[k],VOMECRTY[k],angle,1e20);
                }
            
                // 3D temperature
                double[][][] VOTEMPER = dataTem.getTemp(); //Diana

                // 3D salinity
                double[][][] VOSALINE = dataSal.getSalt(); //Diana

            
                System.out.println("Preparing 3D...");
            
            
                //int method=1;

                if (method==0) {
                    InterpolatorParams params = new InterpolatorParams();
                    params.put("subset", subset2D);
                    KInterpolator kInterpolatorRho = new KInterpolator(LATXY,LONXY,LATRHO,LONRHO,srcMASK,dstMASKRHO,params);

                    kInterpolatorRho.setSubset(subset2D);
                    SSHE_ROMS=kInterpolatorRho.interp(SOSSHEIG,1e+20,1e+37, null );


                    KInterpolator3D interpolator3DRho = new KInterpolator3D(LATXY,LONXY,myOceanZ,LATRHO,LONRHO,romsZ,srcMASK,dstMASKRHO,subset3D);
                    KInterpolator3D interpolator3DU = new KInterpolator3D(LATXY,LONXY,myOceanZ,LATU,LONU,romsZ,srcMASK,dstMASKU,subset3D);
                    KInterpolator3D interpolator3DV = new KInterpolator3D(LATXY,LONXY,myOceanZ,LATV,LONV,romsZ,srcMASK,dstMASKV,subset3D);



                    TEM_ROMS = interpolator3DRho.interp(VOTEMPER,1e+20,1e+37,null);
                    SAL_ROMS = interpolator3DRho.interp(VOSALINE,1e+20,1e+37,null); // Diana
                    U_ROMS = interpolator3DU.interp(VOZOCRTX,1e+20,1e+37,pIntegralScale);
                    V_ROMS = interpolator3DV.interp(VOMECRTY,1e+20,1e+37,pIntegralScale);



                } else if (method==1) {
                    InterpolatorParams params = new InterpolatorParams();
                    params.put("subset", subset2D);
                    KInterpolator kInterpolatorRho = new KInterpolator(LATXY,LONXY,LATRHO,LONRHO,srcMASK,dstMASKRHO,params);

                    kInterpolatorRho.setSubset(subset2D);
                    SSHE_ROMS=kInterpolatorRho.interp(SOSSHEIG,1e+20,1e+37, null );

                    FastKInterpolator3D interpolator3DRho = new FastKInterpolator3D(LATXY,LONXY,myOceanZ,LATRHO,LONRHO,romsZ,srcMASK,dstMASKRHO,subset3D);
                    FastKInterpolator3D interpolator3DU = new FastKInterpolator3D(LATXY,LONXY,myOceanZ,LATU,LONU,romsZ,srcMASK,dstMASKU,subset3D);
                    FastKInterpolator3D interpolator3DV = new FastKInterpolator3D(LATXY,LONXY,myOceanZ,LATV,LONV,romsZ,srcMASK,dstMASKV,subset3D);

                    System.out.println("Interpolating U");
                    U_ROMS = interpolator3DU.interp(VOZOCRTX,1e+20,1e+37,pIntegralScale);

                    System.out.println("Interpolating V");
                    V_ROMS = interpolator3DV.interp(VOMECRTY,1e+20,1e+37,pIntegralScale);

                    System.out.println("Interpolating TEMP");
                    TEM_ROMS = interpolator3DRho.interp(VOTEMPER,1e+20,1e+37,null); // Diana
                    System.out.println("Interpolating SAL");
                    SAL_ROMS = interpolator3DRho.interp(VOSALINE,1e+20,1e+37,null); // Diana





                } else if (method==2) {

                    BilinearInterpolator bilinearInterpolatorRho = new BilinearInterpolator(LATXY,LONXY,LATRHO,LONRHO,srcMASK,dstMASKRHO);

                    SSHE_ROMS=bilinearInterpolatorRho.interp(SOSSHEIG,1e+20,1e+37, null );

                    BilinearInterpolator3D interpolator3DRho = new BilinearInterpolator3D(LATXY,LONXY,myOceanZ,LATRHO,LONRHO,romsZ,srcMASK,dstMASKRHO);
                    BilinearInterpolator3D interpolator3DU = new BilinearInterpolator3D(LATXY,LONXY,myOceanZ,LATU,LONU,romsZ,srcMASK,dstMASKU);
                    BilinearInterpolator3D interpolator3DV = new BilinearInterpolator3D(LATXY,LONXY,myOceanZ,LATV,LONV,romsZ,srcMASK,dstMASKV);

                    System.out.println("Interpolating U");
                    U_ROMS = interpolator3DU.interp(VOZOCRTX,1e+20,1e+37,pIntegralScale);

                    System.out.println("Interpolating V");
                    V_ROMS = interpolator3DV.interp(VOMECRTY,1e+20,1e+37,pIntegralScale);

                    System.out.println("Interpolating TEMP");
                    TEM_ROMS = interpolator3DRho.interp(VOTEMPER,1e+20,1e+37,null); // Diana
                    System.out.println("Interpolating SAL");
                    SAL_ROMS = interpolator3DRho.interp(VOSALINE,1e+20,1e+37,null); // Diana





                } else if (method==3) {

                    BiCubicInterpolator biCubicInterpolatorRho = new BiCubicInterpolator(LATXY,LONXY,LATRHO,LONRHO,srcMASK,dstMASKRHO);

                    SSHE_ROMS=biCubicInterpolatorRho.interp(SOSSHEIG,1e+20,1e+37, null );

                    BiCubicInterpolator3D interpolator3DRho = new BiCubicInterpolator3D(LATXY,LONXY,myOceanZ,LATRHO,LONRHO,romsZ,srcMASK,dstMASKRHO);
                    BiCubicInterpolator3D interpolator3DU = new BiCubicInterpolator3D(LATXY,LONXY,myOceanZ,LATU,LONU,romsZ,srcMASK,dstMASKU);
                    BiCubicInterpolator3D interpolator3DV = new BiCubicInterpolator3D(LATXY,LONXY,myOceanZ,LATV,LONV,romsZ,srcMASK,dstMASKV);

                    System.out.println("Interpolating U");
                    U_ROMS = interpolator3DU.interp(VOZOCRTX,1e+20,1e+37,pIntegralScale);

                    System.out.println("Interpolating V");
                    V_ROMS = interpolator3DV.interp(VOMECRTY,1e+20,1e+37,pIntegralScale);

                    System.out.println("Interpolating TEMP");
                    TEM_ROMS = interpolator3DRho.interp(VOTEMPER,1e+20,1e+37,null); // Diana
                    System.out.println("Interpolating SAL");
                    SAL_ROMS = interpolator3DRho.interp(VOSALINE,1e+20,1e+37,null); // Diana

                } else if (method==4) {

                    IDWInterpolator idwInterpolatorRho = new IDWInterpolator(LATXY,LONXY,LATRHO,LONRHO,srcMASK,dstMASKRHO);

                    SSHE_ROMS=idwInterpolatorRho.interp(SOSSHEIG,1e+20,1e+37, null );

                    IDWInterpolator3D interpolator3DRho = new IDWInterpolator3D(LATXY,LONXY,myOceanZ,LATRHO,LONRHO,romsZ,srcMASK,dstMASKRHO);
                    IDWInterpolator3D interpolator3DU = new IDWInterpolator3D(LATXY,LONXY,myOceanZ,LATU,LONU,romsZ,srcMASK,dstMASKU);
                    IDWInterpolator3D interpolator3DV = new IDWInterpolator3D(LATXY,LONXY,myOceanZ,LATV,LONV,romsZ,srcMASK,dstMASKV);

                    System.out.println("Interpolating U");
                    U_ROMS = interpolator3DU.interp(VOZOCRTX,1e+20,1e+37,pIntegralScale);

                    System.out.println("Interpolating V");
                    V_ROMS = interpolator3DV.interp(VOMECRTY,1e+20,1e+37,pIntegralScale);

                    System.out.println("Interpolating TEMP");
                    TEM_ROMS = interpolator3DRho.interp(VOTEMPER,1e+20,1e+37,null); // Diana
                    System.out.println("Interpolating SAL");
                    SAL_ROMS = interpolator3DRho.interp(VOSALINE,1e+20,1e+37,null); // Diana

                }
            
            
                for (int k=0;k<romsZ.length;k++) {
                    fw = new FileWriter(csvPath+"/roms-"+k+".csv");
                    pw = new PrintWriter(fw);
                    pw.println("LON;LAT;TEM;SAL");
                    for (int j=0;j<LATRHO.length;j++) {
                        for (int i=0;i<LATRHO[j].length;i++) {
                            //System.out.println("k:"+k+" j:"+j+" i:"+i);
                            //double uv= Math.sqrt(
                            //        (U_ROMS[k][j][i]*U_ROMS[k][j][i])+
                            //        (V_ROMS[k][j][i]*V_ROMS[k][j][i]));


                            double tem=TEM_ROMS[k][j][i];
                            double sal=SAL_ROMS[k][j][i];

                            //if (Double.isNaN(uv)) uv=1e+37;
                            if (Double.isNaN(tem)) tem=1e+37;
                            if (Double.isNaN(sal)) sal=1e+37;

                            pw.println(LONRHO[j][i]+";"+LATRHO[j][i]+";"+
                                    tem+";"+sal);
                        }
                    }
                    pw.flush();
                    pw.close();
                    fw.close();
                }
            
            
                double[][] UBAR = new double[LATU.length][LONU[0].length];
                double[][] VBAR = new double[LATV.length][LONV[0].length];

            
           
            
            
                for (int j=0;j<LATU.length;j++) {
                    for (int i=0;i<LONU[j].length;i++) {
                        if (MASKRHO[j][i]==1) {
                            UBAR[j][i]=0;
                            count=0;
                            for (int k=0;k<romsZ.length;k++) {
                                if (U_ROMS[k][j][i]!=1e37) {
                                    UBAR[j][i]+=U_ROMS[k][j][i];
                                    count++;
                                }
                            }
                            if (count>0) {
                                UBAR[j][i]=UBAR[j][i]/count;
                            }
                        } else {
                            UBAR[j][i]=1e37;
                        }

                    }
                }

                for (int j=0;j<LATV.length;j++) {
                    for (int i=0;i<LONV[j].length;i++) {
                        if (MASKRHO[j][i]==1) {
                            VBAR[j][i]=0;
                            count=0;
                            for (int k=0;k<romsZ.length;k++) {
                                if (V_ROMS[k][j][i]!=1e37) {
                                    VBAR[j][i]+=V_ROMS[k][j][i];
                                    count++;
                                }
                            }
                            if (count>0) {
                                VBAR[j][i]=VBAR[j][i]/count;
                            }
                        } else {
                            VBAR[j][i]=1e37;
                        }
                    }
                }
            
                /*
                rotate(UBAR,VBAR,ANGLE,1e37);

                for (int k=0;k<romsZ.length;k++) {
                    rotate(U_ROMS[k],V_ROMS[k],ANGLE,1e37);
                }
                */
            
            
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
    
    public void rotate(double[][] u, double[][] v, double angle, double missingValue) {
        double rotU, rotV;
        
        int maxJ=v.length;
        int maxI=v[0].length;
        
        for (int j=0;j<maxJ;j++) {
            for (int i=0;i<maxI;i++) {
                if (
                    Double.isNaN(u[j][i])==false &&
                    Double.isNaN(v[j][i])==false &&
                    u[j][i] != missingValue &&
                    v[j][i] != missingValue 
                    
                ) {
                    rotU = (u[j][i] * Math.cos(angle) - v[j][i] * Math.sin(angle));
                    rotV = (v[j][i] * Math.cos(angle) + u[j][i] * Math.sin(angle));

                    u[j][i]=rotU;
                    v[j][i]=rotV;
                }
            }
            
        }
        
    }
    
    public void rotate(double[][] u, double[][] v, double[][] angle, double missingValue) {
        double rotU, rotV;
        
        for (int j=0;j<v.length;j++) {
            for (int i=0;i<u[j].length;i++) {
                if (
                    Double.isNaN(u[j][i])==false &&
                    Double.isNaN(v[j][i])==false &&
                    Double.isNaN(angle[j][i]) == false &&
                    u[j][i] != missingValue &&
                    v[j][i] != missingValue &&
                    angle[j][i] != missingValue
                ) {
                    rotU = (u[j][i] * Math.cos(angle[j][i]) - v[j][i] * Math.sin(angle[j][i]));
                    rotV = (v[j][i] * Math.cos(angle[j][i]) + u[j][i] * Math.sin(angle[j][i]));

                    u[j][i]=rotU;
                    v[j][i]=rotV;
                }
            }
            
        }
        
    }
}
    



