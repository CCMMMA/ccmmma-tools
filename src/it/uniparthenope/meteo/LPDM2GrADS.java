/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.uniparthenope.meteo;

import it.uniparthenope.lpdm.LPDMHistory;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import jncregridder.roms.ROMSGrid;
import jncregridder.roms.ROMSHistory;
import jncregridder.util.KrigingException;
import jncregridder.util.NCRegridderException;
import ucar.ma2.InvalidRangeException;

/**
 *
 * @author raffaelemontella
 */
public class LPDM2GrADS {
    public static void main(String args[])
        throws IOException, NCRegridderException, InvalidRangeException, NoSuchAlgorithmException, KrigingException
    {
        
        try {
            
            if (args.length != 5) {
                System.out.println("Usage:");
                System.out.println("LPDM2GrADS gridFile romsHistoryFile lpdmHistoryFile outputPath fileNameRoot");
                System.exit(0);
            }
            new LPDM2GrADS(args[0],args[1],args[2], args[3], args[4]);
             
            /* 
            new LPDM2GrADS(
                    "/Users/raffaelemontella/tmp/myocean2roms/roms/roms-grid-d04.nc",
                    "/Users/raffaelemontella/tmp/myocean2roms/output/all_5giorni.nc",
                    "/Users/raffaelemontella/tmp/myocean2roms/lamp3d/lmp3_d04_20121007Z0000_120h.nc",
                    "/Users/raffaelemontella/tmp/myocean2roms/output/", "lmp3_d04_20121007Z0000_120h");
            */
            
        } catch (InvalidRangeException ex) {
            Logger.getLogger(LPDM2GrADS.class.getName()).log(Level.SEVERE, null, ex); 
        }
        
        
        
    }

    public static double round(double d, int n)
    {
        double a = Math.pow(10D, n);
        double dn = (double)(int)Math.round(d * a) / a;
        return dn;
    }

    public LPDM2GrADS(String gridPath, String historyPath,String lpdmHistoryPath, String outputPath, String fileNameRoot)
        throws IOException, NCRegridderException, InvalidRangeException, NoSuchAlgorithmException, KrigingException
    {
        undef = -9.99E+08F;
        LATRHO = (double[][])null;
        LONRHO = (double[][])null;
        MASKRHO = (double[][])null;
        boolean b3D = true;
        try
        {
            ROMSHistory romsHistory = new ROMSHistory(historyPath);
            ROMSGrid romsGrid = new ROMSGrid(gridPath);
            LPDMHistory lampHistory = new LPDMHistory(lpdmHistoryPath);
            double minMaxLatRho[] = romsGrid.getMinMaxLatRho();
            double minMaxLonRho[] = romsGrid.getMinMaxLonRho();
            
            minLatRho = minMaxLatRho[0];
            maxLatRho = minMaxLatRho[1];
            minLonRho = minMaxLonRho[0];
            maxLonRho = minMaxLonRho[1];
            stepLatRho = minMaxLatRho[2];
            stepLonRho = minMaxLonRho[2];
            System.out.println((new StringBuilder()).append(minLonRho).append("-").append(maxLonRho).append(":").append(stepLonRho).toString());
            System.out.println((new StringBuilder()).append(minLatRho).append("-").append(maxLatRho).append(":").append(stepLatRho).toString());
            dLatRho = maxLatRho - minLatRho;
            dLonRho = maxLonRho - minLonRho;
            cols = (int)(dLonRho / stepLonRho);
            rows = (int)(dLatRho / stepLatRho);
            levs = LEVS.length;
            
            LATRHO = romsHistory.getLATRHO();
            LONRHO = romsHistory.getLONRHO();
            MASKRHO = romsHistory.getMASKRHO();
            etaRho = romsHistory.dimEtaRho.getLength();
            xiRho = romsHistory.dimXiRho.getLength();
            sRho = romsHistory.dimSRho.getLength();
            xiRho--;
            etaRho--;
            System.out.println(etaRho+","+xiRho);
            
            romsHistory.setTime(0);
            Calendar cal = romsHistory.getTimeAsCalendar();
            String gradsStartDate = "00Z29aug2012";
            int year = cal.get(1);
            int month = cal.get(2) + 1;
            int day = cal.get(5);
            int hour = cal.get(11);
            gradsStartDate = (new StringBuilder()).append(String.format("%02d", new Object[] {
                Integer.valueOf(hour)
            })).append("Z").append(String.format("%02d", new Object[] {
                Integer.valueOf(day)
            })).append(months[month]).append(year).toString();
            int timeSteps = romsHistory.dimTime.getLength();
            System.out.println("Writing");
            String strFilePath = (new StringBuilder()).append(outputPath).append("/").append(fileNameRoot).append(".dat").toString();
            FileOutputStream fos = new FileOutputStream(strFilePath);
            FileChannel fc = fos.getChannel();
            double H[][] = romsGrid.getH();
            float dstH[][] = new float[rows][cols];
            orizInterp(H, dstH, true);
            for(int j = 0; j < rows; j++)
            {
                for(int i = 0; i < cols; i++)
                    if(dstH[j][i] != undef)
                        dstH[j][i] = -dstH[j][i];

            }

            

            
            double CONC[][][] = null;
            
            double tmp3d[][][][] = (double[][][][])null;
            
            float tmpCONC[][][] = null;
            float dstCONC[][][] = null;
            
            //timeSteps=6;
            
            boolean bCONC=false;
            
            
            romsHistory.setTime(0);
            lampHistory.setTime(0);
            
            for(int t = 0; t < timeSteps; t++)
            {
                System.out.println((new StringBuilder()).append("t=").append(t).toString());
                romsHistory.setTime(t);
                lampHistory.setTime(t);
                
                double ZR[][][] = romsHistory.getZR();
                float tmpZR[][][] = new float[sRho][rows][cols];
                for(int k = 0; k < sRho; k++)
                    orizInterp(ZR[k], tmpZR[k], false);

                
                
                
                try {
                    CONC = lampHistory.getCONC();
                    tmpCONC = new float[sRho][rows][cols];
                    dstCONC = new float[levs][rows][cols];
                    bCONC=true;
                } catch (NCRegridderException ex) {
                    bCONC=false;
                }
                
                
                System.out.println("Projecting 3D");
                for(int k = 0; k < sRho; k++)
                {
                    if (bCONC==true) orizInterp(CONC[k], tmpCONC[k], true);
                    
                    
                }
                

                System.out.println("Vertical interpolating 3D");
                if (bCONC==true) vertInterp(dstH, tmpZR, tmpCONC, dstCONC);
                
                
                if (bCONC==true) {
                    System.out.println("CONC");
                    writeVar(fc, dstCONC);
                }
            }

            fc.close();
            fos.close();
            strFilePath = (new StringBuilder()).append(outputPath).append("/").append(fileNameRoot).append(".ctl").toString();
            FileWriter fw = new FileWriter(strFilePath);
            PrintWriter pw = new PrintWriter(fw, true);
            int vars = 0;
            
            String controlText = "DSET  ^"+fileNameRoot+".dat\n";
            controlText+="TITLE Gridded Data Sample\n";
            controlText+="UNDEF "+undef+"\n";
            controlText+="OPTIONS byteswapped\n";
            controlText+="XDEF "+cols+" LINEAR "+minLonRho+"  "+stepLonRho+"\n";
            controlText+="YDEF "+rows+" LINEAR "+minLatRho+"  "+stepLatRho+"\n";
            controlText+="ZDEF   "+levs+" LEVELS";
            for(int l = 0; l < levs; l++) controlText += " "+LEVS[l];
            controlText += "\n";
            
            controlText += "TDEF   "+timeSteps+" LINEAR "+gradsStartDate+" 1hr\n";
            
            
            
            String varText = "";
            
            if (bCONC==true) {
                varText+="conc     "+levs+"  99  Concentration\n";
                vars++;
            }
            
            controlText += "VARS   "+vars+"\n";
            controlText += varText;
            controlText+="ENDVARS\n";
            
            pw.println(controlText);
            pw.close();
            fw.close();
            System.err.println("Done");
        }
        catch(IOException ex)
        {
            Logger.getLogger(ROMS2GrADS.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch(NCRegridderException ex)
        {
            Logger.getLogger(ROMS2GrADS.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void writeVar(FileChannel fc, float dst[][][]) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(rows * cols * levs * 4);
        int count = 0;
        for(int l = 0; l < levs; l++) {
            for(int j = 0; j < rows; j++) {
                for(int i = 0; i < cols; i++) {
                    bb.putFloat(count, dst[l][j][i]);
                    count += 4;
                }

            }
        }
        fc.write(bb);
    }

    
   

    private void writeVar(FileChannel fc, float dst[][])
        throws IOException
    {
        ByteBuffer bb = ByteBuffer.allocate(rows * cols * 4);
        int count = 0;
        for(int j = 0; j < rows; j++)
        {
            for(int i = 0; i < cols; i++)
            {
                bb.putFloat(count, dst[j][i]);
                count += 4;
            }

        }

        fc.write(bb);
    }

    

    public void orizInterp(double SRC[][], float DST[][], boolean bFix0)
    {
        for(int j = 0; j < rows; j++)
        {
            for(int i = 0; i < cols; i++)
                DST[j][i] = undef;

        }

        for(int j = 0; j < etaRho; j++)
        {
            for(int i = 0; i < xiRho; i++)
                if(MASKRHO[j][i] == 1.0D && !Double.isNaN(SRC[j][i]) && SRC[j][i] != 1e37)
                {
                    double lonRho = LONRHO[j][i];
                    double latRho = LATRHO[j][i];
                    double lonOffsetRho = lonRho - minLonRho;
                    double iWork = ((double)cols * lonOffsetRho) / dLonRho;
                    double latOffsetRho = latRho - minLatRho;
                    double jWork = ((double)rows * latOffsetRho) / dLatRho;
                    int iW = (int)Math.round(iWork);
                    int jW = (int)Math.round(jWork);
                    DST[jW][iW] = (float)SRC[j][i];
                }

        }

        for(int jWork = 0; jWork < rows; jWork++)
        {
            for(int iWork = 0; iWork < cols; iWork++)
            {
                if(bFix0 && DST[jWork][iWork] == 0.0F)
                    DST[jWork][iWork] = undef;
                if(DST[jWork][iWork] != undef)
                    continue;
                if(iWork - 1 >= 0 && iWork + 1 < cols && DST[jWork][iWork - 1] != undef && DST[jWork][iWork + 1] != undef)
                {
                    DST[jWork][iWork] = (DST[jWork][iWork - 1] + DST[jWork][iWork + 1]) / 2.0F;
                    continue;
                }
                if(iWork - 1 >= 0 && iWork + 2 < cols && DST[jWork][iWork - 1] != undef && DST[jWork][iWork + 1] == undef && DST[jWork][iWork + 2] != undef)
                {
                    DST[jWork][iWork] = (DST[jWork][iWork - 1] + DST[jWork][iWork + 2]) / 2.0F;
                    DST[jWork][iWork + 1] = (DST[jWork][iWork - 1] + DST[jWork][iWork + 2]) / 2.0F;
                    iWork++;
                    continue;
                }
                if(iWork - 1 >= 0 && iWork + 3 < cols && DST[jWork][iWork - 1] != undef && DST[jWork][iWork + 1] == undef && DST[jWork][iWork + 2] == undef && DST[jWork][iWork + 3] != undef)
                {
                    DST[jWork][iWork] = (DST[jWork][iWork - 1] + DST[jWork][iWork + 3]) / 2.0F;
                    DST[jWork][iWork + 1] = (DST[jWork][iWork - 1] + DST[jWork][iWork + 3]) / 2.0F;
                    DST[jWork][iWork + 2] = (DST[jWork][iWork - 1] + DST[jWork][iWork + 3]) / 2.0F;
                    iWork++;
                    iWork++;
                    continue;
                }
                if(iWork - 1 >= 0 && iWork + 4 < cols && DST[jWork][iWork - 1] != undef && DST[jWork][iWork + 1] == undef && DST[jWork][iWork + 2] == undef && DST[jWork][iWork + 3] == undef && DST[jWork][iWork + 4] != undef)
                {
                    DST[jWork][iWork] = (DST[jWork][iWork - 1] + DST[jWork][iWork + 4]) / 2.0F;
                    DST[jWork][iWork + 1] = (DST[jWork][iWork - 1] + DST[jWork][iWork + 4]) / 2.0F;
                    DST[jWork][iWork + 2] = (DST[jWork][iWork - 1] + DST[jWork][iWork + 4]) / 2.0F;
                    DST[jWork][iWork + 3] = (DST[jWork][iWork - 1] + DST[jWork][iWork + 4]) / 2.0F;
                    iWork++;
                    iWork++;
                    iWork++;
                }
            }

        }

    }

    private void vertInterp(float h[][], float z[][][], float src[][][], float dst[][][])
    {
        for(int l = 0; l < LEVS.length; l++)
        {
            float d = (float)LEVS[l];
            for(int j = 0; j < rows; j++)
            {
                for(int i = 0; i < cols; i++)
                    if(Math.abs(d) <= Math.abs(h[j][i]))
                    {
                        int k;
                        for(k = src.length - 1; k > 0 && z[k][j][i] != undef && Math.abs(z[k][j][i]) < Math.abs(d); k--);
                        if(z[k][j][i] == undef && k == src.length - 1)
                        {
                            // Mask
                            dst[l][j][i] = undef;
                            continue;
                        }
                        int k1 = k; // lower index
                        int k2 = k + 1; // upper index
                        if(k2 == src.length)
                        {
                            // Surface
                            dst[l][j][i] = src[k1][j][i];
                            continue;
                        }
                        float d1 = z[k1][j][i];
                        float d2 = z[k2][j][i];
                        if(d1 == undef && d2 == undef)
                        {
                            // Mask
                            dst[l][j][i] = undef;
                            continue;
                        }
                        if(d1 == undef)
                        {
                            // Bottom
                            dst[l][j][i] = src[k2][j][i];
                            continue;
                        }
                        if(d2 == undef)
                        {
                            // Surface
                            dst[l][j][i] = src[k1][j][i];
                        } else
                        {
                            float dd = Math.abs(d2 - d1);
                            float dz = Math.abs(d2 - d);
                            float r2 = dz / dd;
                            float r1 = 1.0F - r2;
                            dst[l][j][i] = r1 * src[k2][j][i] + r2 * src[k1][j][i];
                        }
                    } else
                    {
                        // Below the bottom
                        dst[l][j][i] = undef;
                    }

            }

        }

    }

    private String months[] = {
        null, "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", 
        "oct", "nov", "dec"
    };
    double minLatRho;
    double maxLatRho;
    double minLonRho;
    double maxLonRho;
    double stepLatRho;
    double stepLonRho;
    double dLatRho;
    double dLonRho;
    int cols;
    int rows;
    int levs;
    float undef;
    int xiRho;
    int etaRho;
    int etaU;
    int xiU;
    int etaV;
    int xiV;
    int sRho,sW;
    double LATRHO[][];
    double LONRHO[][];
    double MASKRHO[][];
    
    /*
    double LEVS[] = {
           -1.5,    -4.6,    -7.9,   -11.6,   -15.4,
          -19.6,   -24.1,   -28.9,   -34.1,   -39.7,
          -45.7,   -52.1,   -59.0,   -66.4,   -74.3,
          -82.8,   -92.0,  -101.7,  -112.2,  -123.4,
         -135.4,  -148.3,  -162.1,  -176.8,  -192.6,
         -209.4,  -227.5,  -246.8,  -267.5,  -289.6,
         -313.3,  -338.6,  -365.6,  -394.5,  -425.4,
         -458.5,  -493.8,  -531.6,  -571.9,  -615.1,
         -661.1,  -710.3,  -762.8,  -818.9,  -878.9,
         -942.8, -1011.2, -1084.1, -1161.9, -1245.0,
        -1333.6, -1428.2, -1529.1, -1636.6, -1751.3,
        -1873.5, -2003.8, -2142.7, -2290.6, -2448.2,
        -2615.9, -2794.6, -2984.7, -3186.9, -3402.1,
        -3630.7, -3873.8, -4132.1, -4406.5, -4697.7,
        -5006.8, -5334.65 };
    */
    
    double LEVS[] = {
        0, -2.5, -5, -10, -15, -20, -25, -50, -100, -200, -300
    };
    
    /*
    double LEVS[] = {
        -1D, -2D, -5D, -10D, -15D, -20D, -25D, -30D, -35D, -40D, 
        -45D, -50D, -60D, -70D, -80D, -90D, -100D, -125D, -150D, -175D, 
        -200D, -250D, -300D, -350D, -400D, -450D, -500D, -600D, -700D, -800D, 
        -900D, -1000D, -1100D
    };*/
    
}
