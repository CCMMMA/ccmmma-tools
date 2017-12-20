/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.uniparthenope.meteo;

import it.uniparthenope.meteo.codar.CODARData;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import jncregridder.roms.ROMSGrid;
import jncregridder.roms.ROMSHistory;
import jncregridder.util.KInterpolator;
import jncregridder.util.KrigingException;
import jncregridder.util.NCRegridderException;

/**
 *
 * @author raffaelemontella
 */
public class Surface2ThreeD {
    
    public static void main(String[] args) throws IOException, NCRegridderException, KrigingException {
        new Surface2ThreeD();
       
    }
    
    public Surface2ThreeD() throws IOException, NCRegridderException, KrigingException {
        CODARData codarData=new CODARData("/Users/raffaelemontella/tmp/myocean2roms/codar/TOTL_IMOI_2012_08_29_0000.tuv");
        System.out.println("LON: " + codarData.getMinLon()+ " - "+codarData.getMaxLon() +" : ");
        System.out.println("LAT: " + codarData.getMinLat()+ " - "+codarData.getMaxLat() +" : ");
        
        ROMSGrid romsGrid = new ROMSGrid("/Users/raffaelemontella/tmp/myocean2roms/roms/roms-grid-d04.nc");
        
        double[][] LONRHO=romsGrid.getLONRHO();
        double[][] LATRHO=romsGrid.getLATRHO();
        int xiRhoDim=LONRHO[0].length;
        int etaRhoDim=LATRHO.length;
        
        double[][] USRF_RHO = new double[etaRhoDim][xiRhoDim];
        double[][] VSRF_RHO = new double[etaRhoDim][xiRhoDim];
        
        
        codarData.regrid(LATRHO, LATRHO,USRF_RHO,VSRF_RHO);
        
        FileWriter fw = new FileWriter("/Users/raffaelemontella/tmp/myocean2roms/csv/codar.csv");
        PrintWriter pw = new PrintWriter(fw);
        pw.println("LON;LAT;USRF;VSRF");
        for (int j=0;j<etaRhoDim;j++) {
            for (int i=0;i<xiRhoDim;i++) {
                pw.println(LONRHO[j][i]+";"+LATRHO[j][i]+";"+USRF_RHO[j][i]+";"+VSRF_RHO[j][i]);
            }
        }
        
        pw.close();
        fw.close();
            
        
    }
    
}
