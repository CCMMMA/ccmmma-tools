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
import jncregridder.roms.ROMSBoundary;
import jncregridder.roms.ROMSGrid;
import jncregridder.roms.ROMSHistory;
import jncregridder.roms.ROMSInit;
import jncregridder.util.FastKInterpolator3D;
import jncregridder.util.IDWInterpolator;
import jncregridder.util.InterpolatorException;
import jncregridder.util.InterpolatorParams;
import jncregridder.util.KInterpolator;
import jncregridder.util.KrigingException;
import jncregridder.util.NCRegridderException;
import ucar.ma2.InvalidRangeException;

/**
 *
 * @author raffaelemontella
 */
public class ROMS2ROMS {
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, NCRegridderException, KrigingException, NoSuchAlgorithmException, InterpolatorException, InvalidRangeException { // Diana
        try {
            
            if (args.length != 10) {
                System.out.println("Usage:");
                System.out.println("ROMS2ROMS srcGridPath dstGridPath srcDataPath YYYYMMDD domainId initFilename boundaryFilename csvPath subset2D subset3D");
                System.exit(0);
            }
            new ROMS2ROMS(args[0],args[1],args[2],args[3],args[4],args[5],args[6],args[7],Double.parseDouble(args[8]),Double.parseDouble(args[9]));
        } catch (InvalidRangeException ex) {// Diana
            Logger.getLogger(MyOcean2ROMS.class.getName()).log(Level.SEVERE, null, ex); // Diana
        } 
    }
    
    private ROMSInit romsInit=null;
    private ROMSBoundary romsBoundary=null;
    private ROMSHistory romsHistory=null;

    private ROMS2ROMS(String srcGridPath, String dstGridPath, String dataPath, String ncepDate, String domainId, String initPath, String boundaryPath, String csvPath, double subset2D, double subset3D) throws IOException, NCRegridderException, InvalidRangeException, KrigingException, NoSuchAlgorithmException, InterpolatorException { // Diana aggiunta eccezioni
        String srcRomsGridPath  = srcGridPath;
        String dstRomsGridPath  = dstGridPath;
        String romsHistoryPath = dataPath;
        String romsInitPath  = initPath; // Diana
        String romsBoundaryPath = boundaryPath;
        
        try {
            
            System.out.println("Destination:");
            
            // Open ROMS grid data
            ROMSGrid dstRomsGrid = new ROMSGrid(dstRomsGridPath);
            
            int dstEtaRho = dstRomsGrid.dimEtaRho.getLength();
            int dstXiRho = dstRomsGrid.dimXiRho.getLength();
            int dstEtaU = dstRomsGrid.dimEtaU.getLength();
            int dstXiU = dstRomsGrid.dimXiU.getLength();
            int dstEtaV = dstRomsGrid.dimEtaV.getLength();
            int dstXiV = dstRomsGrid.dimXiV.getLength();
            
            System.out.println("Rho:\n\teta:"+dstEtaRho+"\txi:"+dstXiRho);
            System.out.println("U:\n\teta:"+dstEtaU+"\txi:"+dstXiU);
            System.out.println("V:\n\teta:"+dstEtaV+"\txi:"+dstXiV);
            
            
            
           
            // MASK at rho points
            int[][] dstMASKRHO = dstRomsGrid.getMASKRHOasInt();
            if (dstMASKRHO==null) {
                throw new NCRegridderException("Destination MASKRHO is null");
            }
            
            // MASK at u points
            int[][] dstMASKU = dstRomsGrid.getMASKUasInt();
            if (dstMASKU==null) {
                throw new NCRegridderException("Destination MASKU is null");
            }
            
            // MASK at v points
            int[][] dstMASKV = dstRomsGrid.getMASKVasInt();
            if (dstMASKV==null) {
                throw new NCRegridderException("Destination MASKV is null");
            }
            
            
            
            
            
            // LAT at rho points
            double[][] dstLATRHO = dstRomsGrid.getLATRHO();
            
            // LON at rho points
            double[][] dstLONRHO = dstRomsGrid.getLONRHO();
            
            // Z at rho/sigma pints 
            double[][][] dstZ = dstRomsGrid.getZ();
            
            
            // LAT,LON at u points
            double[][] dstLATU = dstRomsGrid.getLATU();
            double[][] dstLONU = dstRomsGrid.getLONU();
            
            // LAT,LON at v points
            double[][] dstLATV = dstRomsGrid.getLATV();
            double[][] dstLONV = dstRomsGrid.getLONV();
            
            double[][] dstANGLE = dstRomsGrid.getANGLE();
            
            System.out.println("LATU:"+dstLATU.length+";"+dstLATU[0].length);
            System.out.println("LONU:"+dstLONU.length+";"+dstLONU[0].length);
            System.out.println("LATV:"+dstLATV.length+";"+dstLATV[0].length);
            System.out.println("LONV:"+dstLONV.length+";"+dstLONV[0].length);
            System.out.println("ANGLE:"+dstANGLE.length+";"+dstANGLE[0].length);
            
            double dstAngle=0;
            int count=0;
            for(int j=0;j<dstANGLE.length;j++) {
                for(int i=0;i<dstANGLE[j].length;i++) {
                    if (Double.isNaN(dstANGLE[j][i])==false && dstANGLE[j][i]!=1e20) {
                        dstAngle+=dstANGLE[j][i];
                        count++;
                    }
                }
            }
            dstAngle=dstAngle/count;
            System.out.println("Average grid angle:"+dstAngle);
            
            
            // Just for debugging/controlling:
            // Write a csv file with LAT, LON, DEPTH
            for (int k=0;k<dstZ.length;k++) {
                FileWriter fw = new FileWriter(csvPath+"/dst-sigma-"+k+".csv");
                PrintWriter pw = new PrintWriter(fw);
                pw.println("LON;LAT;DEPTH");
                for (int j=0;j<dstZ[k].length;j++) {
                    for (int i=0;i<dstZ[k][j].length;i++) {
                        pw.println(dstLONRHO[j][i]+";"+dstLATRHO[j][i]+";"+dstZ[k][j][i]);
                    }
                }
                pw.flush();
                pw.close();
                fw.close();
            }
            
            ROMSGrid srcRomsGrid = new ROMSGrid(srcRomsGridPath);
            // 
            
            System.out.println("Source:");
            
            int srcEtaRho = srcRomsGrid.dimEtaRho.getLength();
            int srcXiRho = srcRomsGrid.dimXiRho.getLength();
            int srcEtaU = srcRomsGrid.dimEtaU.getLength();
            int srcXiU = srcRomsGrid.dimXiU.getLength();
            int srcEtaV = srcRomsGrid.dimEtaV.getLength();
            int srcXiV = srcRomsGrid.dimXiV.getLength();
            
            System.out.println("Rho:\n\teta:"+srcEtaRho+"\txi:"+srcXiRho);
            System.out.println("U:\n\teta:"+srcEtaU+"\txi:"+srcXiU);
            System.out.println("V:\n\teta:"+srcEtaV+"\txi:"+srcXiV);
            
            
            
           
            // MASK at rho points
            int[][] srcMASKRHO = srcRomsGrid.getMASKRHOasInt();
            if (srcMASKRHO==null) {
                throw new NCRegridderException("Destination MASKRHO is null");
            }
            
            // MASK at u points
            int[][] srcMASKU = srcRomsGrid.getMASKUasInt();
            if (srcMASKU==null) {
                throw new NCRegridderException("Destination MASKU is null");
            }
            
            // MASK at v points
            int[][] srcMASKV = srcRomsGrid.getMASKVasInt();
            if (srcMASKV==null) {
                throw new NCRegridderException("Destination MASKV is null");
            }
            
            
            
            
            
            // LAT at rho points
            double[][] srcLATRHO = srcRomsGrid.getLATRHO();
            
            // LON at rho points
            double[][] srcLONRHO = srcRomsGrid.getLONRHO();
            
            // Z at rho/sigma pints 
            double[][][] srcZ = srcRomsGrid.getZ();
            
            
            // LAT,LON at u points
            double[][] srcLATU = srcRomsGrid.getLATU();
            double[][] srcLONU = srcRomsGrid.getLONU();
            
            // LAT,LON at v points
            double[][] srcLATV = srcRomsGrid.getLATV();
            double[][] srcLONV = srcRomsGrid.getLONV();
            
            double[][] srcANGLE = srcRomsGrid.getANGLE();
            
            System.out.println("LATU:"+srcLATU.length+";"+srcLATU[0].length);
            System.out.println("LONU:"+srcLONU.length+";"+srcLONU[0].length);
            System.out.println("LATV:"+srcLATV.length+";"+srcLATV[0].length);
            System.out.println("LONV:"+srcLONV.length+";"+srcLONV[0].length);
            System.out.println("ANGLE:"+srcANGLE.length+";"+srcANGLE[0].length);
            
            double srcAngle=0;
            count=0;
            for(int j=0;j<srcANGLE.length;j++) {
                for(int i=0;i<srcANGLE[j].length;i++) {
                    if (Double.isNaN(srcANGLE[j][i])==false && srcANGLE[j][i]!=1e20) {
                        srcAngle+=srcANGLE[j][i];
                        count++;
                    }
                }
            }
            srcAngle=srcAngle/count;
            System.out.println("Average grid angle:"+srcAngle);
            
            
            // Just for debugging/controlling:
            // Write a csv file with LAT, LON, DEPTH
            for (int k=0;k<srcZ.length;k++) {
                FileWriter fw = new FileWriter(csvPath+"/src-sigma-"+k+".csv");
                PrintWriter pw = new PrintWriter(fw);
                pw.println("LON;LAT;DEPTH");
                for (int j=0;j<srcZ[k].length;j++) {
                    for (int i=0;i<srcZ[k][j].length;i++) {
                        pw.println(srcLONRHO[j][i]+";"+srcLATRHO[j][i]+";"+srcZ[k][j][i]);
                    }
                }
                pw.flush();
                pw.close();
                fw.close();
            }
            
            
            romsHistory = new ROMSHistory(romsHistoryPath);
            double oceanTime[] = romsHistory.getOCEANTIME();
            
            
            int forcingTimeSteps=romsHistory.dimTime.getLength();
            
            double scrumTime[] = new double[forcingTimeSteps];
            for (int t=0;t<forcingTimeSteps;t++) {
                if (t>0){ 
                    scrumTime[t]=(oceanTime[t]-oceanTime[0])*86400;
                } else {
                    scrumTime[t]=0;
                }
            }
            
            romsInit = new ROMSInit(romsInitPath,dstRomsGrid,ncepDate,forcingTimeSteps); // Diana
            romsInit.setOceanTime(oceanTime);
            romsInit.setScrumTime(scrumTime);
            romsInit.make();
            romsBoundary = new ROMSBoundary(romsBoundaryPath,dstRomsGrid,ncepDate, forcingTimeSteps);
            romsBoundary.setOceanTime(oceanTime);
            romsBoundary.make();
            
            // For each time step in myocean data
            for (int t=0;t<forcingTimeSteps;t++) {
                System.out.println("Time:"+t+" "+oceanTime[t]);
                romsHistory.setTime(t);
                
                // Surface current U component
                double[][] srcUBAR = romsHistory.getUBAR();

                // Surface current V component
                double[][] srcVBAR = romsHistory.getVBAR(); 

                // Rotate 
            
                // 2D sea surface height 
                double[][] srcZETA = romsHistory.getZETA(); //Diana
                
                double[] pIntegralScale = {1,1,1};

                double[][] dstZETA= null;
                double[][] dstVBAR= null;
                double[][] dstUBAR= null;
                double[][][] dstTEMP = null;
                double[][][] dstSALT = null;
                double[][][] dstU = null;
                double[][][] dstV = null;
                
                // 3D current U component
                double[][][] srcU = romsHistory.getU();

                // 3D current V component
                double[][][] srcV = romsHistory.getV();

                System.out.println("Rotating 3D UV...");
                for(int k=0;k<srcZ.length;k++) {
                    rotate(srcU[k],srcV[k],dstANGLE,1e37);
                }
            
                // 3D temperature
                double[][][] srcTEMP = romsHistory.getTEMP(); //Diana

                // 3D salinity
                double[][][] srcSALT = romsHistory.getSALT(); //Diana
                
                InterpolatorParams params=new InterpolatorParams();;
                
            
                System.out.println("Preparing 2D...");
                /*
                
                params.put("subset", subset2D);
                KInterpolator kInterpolatorRho = new KInterpolator(
                        srcLATRHO,
                        srcLONRHO,
                        dstLATRHO,
                        dstLONRHO,
                        srcMASKRHO,
                        dstMASKRHO,
                        params);

                kInterpolatorRho.setSubset(subset2D);
                dstZETA =kInterpolatorRho.interp(srcZETA,1e+37,1e+37, null );
                dstUBAR =kInterpolatorRho.interp(srcUBAR,1e+37,1e+37, null );
                dstVBAR =kInterpolatorRho.interp(srcVBAR,1e+37,1e+37, null );
                */
                params.put("radius", 0.5);
                IDWInterpolator iDWInterpolator=new IDWInterpolator(srcLATRHO,
                        srcLONRHO,
                        dstLATRHO,
                        dstLONRHO,
                        srcMASKRHO,
                        dstMASKRHO,
                        params);
                dstZETA =iDWInterpolator.interp(srcZETA,1e+37,1e+37, null );
                dstUBAR =iDWInterpolator.interp(srcUBAR,1e+37,1e+37, null );
                dstVBAR =iDWInterpolator.interp(srcVBAR,1e+37,1e+37, null );
                
                System.out.println("Preparing 3D...");
                
                FastKInterpolator3D interpolator3DRho = new FastKInterpolator3D(srcLATRHO,srcLONRHO,srcZ,dstLATRHO,dstLONRHO,dstZ,srcMASKRHO,dstMASKRHO,subset3D);
                FastKInterpolator3D interpolator3DU = new FastKInterpolator3D(srcLATU,srcLONU,srcZ,dstLATU,dstLONU,dstZ,srcMASKU,dstMASKU,subset3D);
                FastKInterpolator3D interpolator3DV = new FastKInterpolator3D(srcLATV,srcLONV,srcZ,dstLATV,dstLONV,dstZ,srcMASKV,dstMASKV,subset3D);

                System.out.println("Interpolating U");
                dstU = interpolator3DU.interp(srcU,1e+37,1e+37,pIntegralScale);

                System.out.println("Interpolating V");
                dstV = interpolator3DV.interp(srcV,1e37,1e+37,pIntegralScale);

                System.out.println("Interpolating TEMP");
                dstTEMP = interpolator3DRho.interp(srcTEMP,1e+37,1e+37,null); // Diana
                System.out.println("Interpolating SAL");
                dstSALT = interpolator3DRho.interp(srcSALT,1e+37,1e+37,null); // Diana
                
            
                System.out.println("Time:"+t+" Saving init file...");

                romsInit.setUBAR(dstUBAR);
                romsInit.setVBAR(dstVBAR);
                romsInit.setZETA(dstZETA);

                romsInit.setU(dstU);
                romsInit.setV(dstV);



                romsInit.setTEMP(dstTEMP);
                romsInit.setSALT(dstSALT);


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

                    
                
                System.out.println("Time:"+t+" Saving bry file...");
                romsBoundary.setSALT(dstSALT);
                romsBoundary.setTEMP(dstTEMP);
                romsBoundary.setUBAR(dstUBAR);
                romsBoundary.setVBAR(dstVBAR);
                romsBoundary.setZETA(dstZETA);
                romsBoundary.setU(dstU);
                romsBoundary.setV(dstV);
                // do something
                romsBoundary.write(t);
                
            }
            
            romsInit.close();
            romsBoundary.close();
            
        } catch (IOException ex) {
            Logger.getLogger(MyOcean2ROMS.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NCRegridderException ex) {
        Logger.getLogger(MyOcean2ROMS.class.getName()).log(Level.SEVERE, null, ex);
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
