/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.uniparthenope.meteo;

import com.sun.corba.se.spi.legacy.connection.GetEndPointInfoAgainException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import jncregridder.roms.ROMSGrid;
import jncregridder.util.IDWInterpolator;
import jncregridder.util.InterpolatorBase;
import jncregridder.util.InterpolatorException;
import jncregridder.util.InterpolatorParams;
import jncregridder.util.Linear2DInterpolator;
import jncregridder.util.NCRegridderException;
import jncregridder.wrf.WRFData;
import ucar.ma2.InvalidRangeException;

/**
 *
 * @author raffaelemontella
 */
public class WRF2WW3 {
    private double[] interpLevels = new double[] {1000.,950.,900.,850.,800.,750.,700.,650.,600.,550.,500.,450.,400.,350.,300.,250.,200.,150.,100};
    
    public static void main(String[] args) throws IOException, InvalidRangeException, NCRegridderException, InterpolatorException {
        new WRF2WW3("/Users/raffaelemontella/tmp/myocean2roms/wrf/wrf3_d03_20120828Z18.nc",6);
    }
    
    public WRF2WW3(String wrfDataFilename, int wrfTimeOffset) throws IOException, InvalidRangeException, NCRegridderException, InterpolatorException {
        
        WRFData wrfData1 = new WRFData(wrfDataFilename,WRFData.INTERPMETHOD_DEFAULT,interpLevels);
        
        double[][][] latLongs=wrfData1.getLATLONGs();
        double[][] wrfXYLAT=latLongs[0];
        double[][] wrfXYLON=latLongs[1];
        
        double latStep = wrfXYLAT[1][0]-wrfXYLAT[0][0];
        double lonStep = wrfXYLON[0][1]-wrfXYLON[0][0];
        double radius = 4*Math.min(latStep, lonStep);
        
        
        InterpolatorParams paramsWRF2XY=new InterpolatorParams();
        paramsWRF2XY.put("radius", radius);
        
        InterpolatorParams paramsXY2ROMS=new InterpolatorParams();
        paramsXY2ROMS.put("radfactor",9.);
        
        
        InterpolatorBase interpWRF2XY = new IDWInterpolator(wrfData1.getXLAT(),wrfData1.getXLONG(), wrfXYLAT, wrfXYLON, null, null,paramsWRF2XY);
        
        ROMSGrid romsGrid = new ROMSGrid("/Users/raffaelemontella/tmp/myocean2roms/roms/roms-grid-d04.nc");
        int etaRho = romsGrid.dimEtaRho.getLength();
        int xiRho = romsGrid.dimXiRho.getLength();
        
        
        // MASK at rho points
        double[][] MASKRHO = romsGrid.getMASKRHO();
        if (MASKRHO==null) {
            throw new NCRegridderException("MASKRHO is null");
        }
            
       
        // Binary masks
        int[][] dstMASKRHO = new int[etaRho][xiRho];
        
            
            
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
        
        InterpolatorBase interpRho = new Linear2DInterpolator(
            wrfXYLAT,wrfXYLON,
            romsGrid.getLATRHO(),romsGrid.getLONRHO(),null,dstMASKRHO,paramsXY2ROMS);
        
        
        
        wrfData1.setTime(wrfTimeOffset);
        
        double[][] slp = interpWRF2ROMS(interpWRF2XY,interpRho,wrfData1.getSLP(), 1e20, 1e37);
        double[][] t2m = interpWRF2ROMS(interpWRF2XY,interpRho,wrfData1.getT2(), 1e20, 1e37);
        double[][] u10m = interpWRF2ROMS(interpWRF2XY,interpRho,wrfData1.getU10M(), 1e20, 1e37);
        double[][] v10m = interpWRF2ROMS(interpWRF2XY,interpRho,wrfData1.getV10M(), 1e20, 1e37);
        
        double[][] romsLAT=romsGrid.getLATRHO();
        double[][] romsLON=romsGrid.getLONRHO();
        
        FileWriter fw = new FileWriter("/Users/raffaelemontella/tmp/myocean2roms/csv/roms-lonlat.csv");
        PrintWriter pw = new PrintWriter(fw);
        pw.println("LON;LAT;SLP;T2M;U10M;V10M");
        for (int j=0;j<romsLAT.length;j++) {
            for (int i=0;i<romsLON[0].length;i++) {
                pw.println(romsLON[j][i]+";"+romsLAT[j][i]+";"+slp[j][i]+";"+(t2m[j][i]-273.15)+";"+u10m[j][i]+";"+v10m[j][i]);
            }
        }
        
        pw.close();
        fw.close();
    }
    
    private double[][] interpWRF2ROMS(InterpolatorBase interpWRF2XY, InterpolatorBase interpROMS, double[][] src, double srcMissingValue, double dstMissingValue) throws InterpolatorException {
        double[][] tmp = interpWRF2XY.interp(src, srcMissingValue, dstMissingValue, null);
        double[][] dst = interpROMS.interp(tmp, srcMissingValue, dstMissingValue,null);
        return dst;
    }
}
