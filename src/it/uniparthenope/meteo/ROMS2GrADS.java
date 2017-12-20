package it.uniparthenope.meteo;

import java.io.*;
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
import ucar.nc2.Dimension;

// Referenced classes of package it.uniparthenope.meteo:
//            MyOcean2ROMS

public class ROMS2GrADS
{

    public static void main(String args[])
        throws IOException, NCRegridderException, InvalidRangeException, NoSuchAlgorithmException, KrigingException
    {
        try {
            
            if (args.length != 4) {
                System.out.println("Usage:");
                System.out.println("ROMS2GrADS gridFile historyFile outputPath fileNameRoot");
                System.exit(0);
            }
            new ROMS2GrADS(args[0],args[1],args[2], args[3]);
             
             
            // new ROMS2GrADS("/Users/raffaelemontella/tmp/myocean2roms/roms/roms-grid-d04.nc","/Users/raffaelemontella/tmp/myocean2roms/output/all_5giorni.nc","/Users/raffaelemontella/tmp/myocean2roms/output/", "with-w");
            
        } catch (InvalidRangeException ex) {
            Logger.getLogger(ROMS2GrADS.class.getName()).log(Level.SEVERE, null, ex); 
        }
        
        
    }

    public static double round(double d, int n)
    {
        double a = Math.pow(10D, n);
        double dn = (double)(int)Math.round(d * a) / a;
        return dn;
    }

    public ROMS2GrADS(String gridPath, String historyPath, String outputPath, String fileNameRoot)
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
            
            System.out.println("cols:"+cols+" rows:"+rows);
            
            LATRHO = romsHistory.getLATRHO();
            LONRHO = romsHistory.getLONRHO();
            MASKRHO = romsHistory.getMASKRHO();
            etaRho = romsHistory.dimEtaRho.getLength();
            xiRho = romsHistory.dimXiRho.getLength();
            etaU = romsHistory.dimEtaU.getLength();
            xiU = romsHistory.dimXiU.getLength();
            etaV = romsHistory.dimEtaV.getLength();
            xiV = romsHistory.dimXiV.getLength();
            sRho = romsHistory.dimSRho.getLength();
            sW = romsHistory.dimSW.getLength();
            
            double LATU[][] = romsHistory.getLATU();
            double LONU[][] = romsHistory.getLONU();
            double LATV[][] = romsHistory.getLATV();
            double LONV[][] = romsHistory.getLONV();
            
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

            

            double ZETA[][] = null;
            double TEMP[][][] = null;
            double SALT[][][] = null;
            double U[][][] = null;
            double V[][][] = null;
            double W[][][] = null;
            double SUSTR[][] = null;
            double SVSTR[][] = null;
            double UBAR[][] = null;
            double VBAR[][] = null;
            
            double tmp2d[][][] = (double[][][])null;
            double tmp3d[][][][] = (double[][][][])null;
            
            float dstZETA[][] = null;
            float dstSUSTR[][] = null;
            float dstSVSTR[][] = null;
            float dstUBAR[][] = null;
            float dstVBAR[][] = null;
            float tmpU[][][] = null;
            float tmpV[][][] = null;
            float tmpW[][][] = null;
            float tmpTEMP[][][] = null;
            float tmpSALT[][][] = null;
            float dstU[][][] = null;
            float dstV[][][] = null;
            float dstW[][][] = null;
            float dstTEMP[][][] = null;
            float dstSALT[][][] = null;
            
            //timeSteps=6;
            
            boolean bZETA=false;
            boolean bTEMP=false;
            boolean bSALT=false;
            boolean bUVW=false;
            boolean bBAR=false;
            boolean bSTR=false;
            
            
            romsHistory.setTime(0);
            double ANGLE[][] = romsGrid.getANGLE();
            float dstANGLE[][] = new float[rows][cols];
            
            orizInterp(ANGLE, dstANGLE, true);
            
            for(int t = 0; t < timeSteps; t++)
            {
                System.out.println((new StringBuilder()).append("t=").append(t).toString());
                romsHistory.setTime(t);
                
                double ZR[][][] = romsHistory.getZR();
                float tmpZR[][][] = new float[sRho][rows][cols];
                for(int k = 0; k < sRho; k++)
                    orizInterp(ZR[k], tmpZR[k], false);

                double ZW[][][] = romsHistory.getZW();
                float tmpZW[][][] = new float[sW][rows][cols];
                for(int k = 0; k < sW; k++)
                orizInterp(ZW[k], tmpZW[k], false);
                
                try {
                    ZETA = romsHistory.getZETA();
                    dstZETA = new float[rows][cols];
                    orizInterp(ZETA, dstZETA, true);
                    bZETA=true;
                } catch (NCRegridderException ex) {
                    bZETA=false;
                }
                try {
                    TEMP = romsHistory.getTEMP();
                    tmpTEMP = new float[sRho][rows][cols];
                    dstTEMP = new float[levs][rows][cols];
                    bTEMP=true;
                } catch (NCRegridderException ex) {
                    bTEMP=false;
                }
                try {
                    SALT = romsHistory.getSALT();
                    tmpSALT = new float[sRho][rows][cols];
                    dstSALT = new float[levs][rows][cols];
                    bSALT=true;
                } catch (NCRegridderException ex) {
                    bSALT=false;
                }
                
                try {
                    tmp3d = romsHistory.getDsgUV(ANGLE,1e37);
                    U = tmp3d[0];
                    V = tmp3d[1];
                    W = romsHistory.getW();
                    tmpU = new float[sRho][rows][cols];
                    tmpV = new float[sRho][rows][cols];
                    tmpW = new float[sW][rows][cols];
                    dstU = new float[levs][rows][cols];
                    dstV = new float[levs][rows][cols];
                    dstW = new float[levs][rows][cols];
                    bUVW=true;
                } catch (NCRegridderException ex) {
                    bUVW=false;
                }
                
                
                try {
                    tmp2d = romsHistory.getDsgSTR(ANGLE,1e37);
                    SUSTR = tmp2d[0];
                    SVSTR = tmp2d[1];
                    dstSUSTR = new float[rows][cols];
                    dstSVSTR = new float[rows][cols];
                    orizInterp(SUSTR, dstSUSTR, false);
                    orizInterp(SVSTR, dstSVSTR, false);
                    bSTR=true;
                } catch (NCRegridderException ex) {
                    bSTR=false;
                }
                
                try {
                    tmp2d = romsHistory.getDsgBAR(ANGLE,1e37);
                    UBAR = tmp2d[0];
                    VBAR= tmp2d[1];
                    dstUBAR = new float[rows][cols];
                    dstVBAR = new float[rows][cols];
                    orizInterp(UBAR, dstUBAR, false);
                    orizInterp(VBAR, dstVBAR, false);
                    bBAR=true;
                } catch (NCRegridderException ex) {
                    bBAR=false;
                }
                
                
                
                
                
                
                
                
                
                
                
                System.out.println("Projecting 2D");
                
                
                
                
                
                
                
                System.out.println("Projecting 3D");
                for(int k = 0; k < sRho; k++)
                {
                    if (bTEMP==true) orizInterp(TEMP[k], tmpTEMP[k], true);
                    if (bSALT==true) orizInterp(SALT[k], tmpSALT[k], true);
                    if (bUVW==true) {
                        orizInterp(U[k], tmpU[k], false);
                        orizInterp(V[k], tmpV[k], false);
                        
                    }
                    
                }
                if (bUVW==true) {
                    for(int k = 0; k < sW; k++) {
                        orizInterp(W[k], tmpW[k], false);
                    }
                }

                System.out.println("Vertical interpolating 3D");
                if (bTEMP==true) vertInterp(dstH, tmpZR, tmpTEMP, dstTEMP);
                if (bSALT==true) vertInterp(dstH, tmpZR, tmpSALT, dstSALT);
                if (bUVW==true) {
                    vertInterp(dstH, tmpZR, tmpU, dstU);
                    vertInterp(dstH, tmpZR, tmpV, dstV);
                    vertInterp(dstH, tmpZW, tmpW, dstW);
                }
                System.out.println("Writing 2D");
                
                writeVar(fc, dstH);
                writeVar(fc, dstANGLE);
                
                if (bZETA==true) writeVar(fc, dstZETA);
                if (bBAR==true) {
                    writeVar(fc, dstUBAR);
                    writeVar(fc, dstVBAR);
                }
                if (bSTR==true) {
                    writeVar(fc, dstSUSTR);
                    writeVar(fc, dstSVSTR);
                }
                
                    System.out.println("Writing 3D");
                if (bTEMP==true) {
                    System.out.println("TEMP");
                    writeVar(fc, dstTEMP);
                }
                
                if (bSALT==true) {
                    System.out.println("SALT");
                    writeVar(fc, dstSALT);
                }
                
                if (bUVW==true) {
                    System.out.println("U");
                    writeVar(fc, dstU);
                    System.out.println("V");
                    writeVar(fc, dstV);
                    System.out.println("W");
                    writeVar(fc, dstW);
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
            
            
            
            String varText = "depth    0  99  depth\n"; vars++;
            varText += "angle    0  99  angle between the xi axis and the east\n"; vars++;
            
            if (bZETA==true) {
                varText+="zeta     0  99  free-surface\n";
                vars++;
            }
            
            if (bUVW==true) {
                varText+="ubar     0  99  ubar\n";
                varText+="vbar     0  99  vbar\n";
                vars++;
                vars++;
            }
            
            if (bSTR==true) {
                varText+="sustr    0  99  u-stress\n";
                varText+="svstr    0  99  v-stress\n";
                vars++;
                vars++;
            }
            
            
            
            if (bTEMP==true) {
                varText+="temp     "+levs+"  99  Potential temperature\n";
                vars++;
            }
            if (bSALT==true) {
                varText+="salt     "+levs+"  99  Salinity\n";
                vars++;
            }
            
            if (bUVW==true) {
                varText+="u        "+levs+"  99  u\n";
                varText+="v        "+levs+"  99  u\n";
                varText+="w        "+levs+"  99  u\n";
                vars++;
                vars++;
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
                    //System.out.println("jW:"+jW+" iW:"+iW);
                    if (jW >=0 && jW<rows && iW >=0 && iW < cols) {
                        DST[jW][iW] = (float)SRC[j][i];
                    }
                    //} else {
                    //    System.out.println("lonRho:"+lonRho+" latRho:"+latRho);
                    //    System.out.println("minLonRho:"+minLonRho+" minLatRho:"+minLatRho);
                    //    System.out.println("lonOffsetRho:"+lonOffsetRho+" latOffsetRho:"+latOffsetRho);
                    //    System.out.println("jWork:"+jWork+" iWork:"+iWork);
                    //    System.out.println("jW:"+jW+" iW:"+iW);
                    //    
                    //    //throw new RuntimeException("bad!");
                    //}
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
