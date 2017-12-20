/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.uniparthenope.meteo;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jncregridder.roms.ROMSGrid;
import jncregridder.roms.ROMSWind;
import jncregridder.util.InterpolatorException;
import jncregridder.util.NCRegridderException;
import jncregridder.wrf.WRFData;
import ucar.ma2.InvalidRangeException;

/**
 *
 * @author raffaelemontella
 */
public class WRF2ROMS {
    
    private double[] interpLevels = new double[] {1000.,950.,900.,850.,800.,750.,700.,650.,600.,550.,500.,450.,400.,350.,300.,250.,200.,150.,100};
    
    public static void main(String[] args) throws IOException, NCRegridderException, InvalidRangeException, InterpolatorException {
        try {
            
            if (args.length != 6) {
                System.out.println("Usage:");
                System.out.println("WRF2ROMS gridFilename wrfFilename timeOffset forcingBulkFilename forcingUVStressFilename csvPath");
                System.exit(0);
            }
            new WRF2ROMS(args[1],Integer.parseInt(args[2]), args[0], args[3], args[4],args[5]);
            
        } catch (InvalidRangeException ex) {
            Logger.getLogger(WRF2ROMS.class.getName()).log(Level.SEVERE, null, ex); 
        } 
        
         
        
            
    }

    public WRF2ROMS(String wrfDataFilename, int wrfTimeOffset, String romsGridFilename, String romsForcingBulkFilename, String romsForcingUVStressFilename, String csvPath) throws IOException, InvalidRangeException, NCRegridderException, InterpolatorException {
        
        WRFData wrfData = new WRFData(wrfDataFilename,WRFData.INTERPMETHOD_DEFAULT,interpLevels);
        ROMSGrid romsGrid = new ROMSGrid(romsGridFilename);
        ROMSWind romsWind = new ROMSWind(romsForcingBulkFilename, romsForcingUVStressFilename,romsGrid,wrfData,wrfTimeOffset,csvPath);
        romsWind.make();
    }
    
   

    
    
}
