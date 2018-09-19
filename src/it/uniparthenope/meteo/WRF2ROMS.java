/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.uniparthenope.meteo;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
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

        /*
        new WRF2ROMS(
                "/Users/raffaelemontella/dev/ccmmma/roms/data/Campania_270m.nc",
                "/Users/raffaelemontella/dev/ccmmma/roms/wrfout/",
                0,
                "/Users/raffaelemontella/dev/ccmmma/roms/forcing/wind-d03.nc");
        */
        try {
            
            if (args.length != 4) {
                System.out.println("Usage:");
                System.out.println("WRF2ROMS gridFilename wrfFilename|wrfFilespath timeOffset forcingFilename");
                System.exit(0);
            }
            new WRF2ROMS(args[0],args[1],Integer.parseInt(args[2]), args[3]);
            
        } catch (InvalidRangeException ex) {
            Logger.getLogger(WRF2ROMS.class.getName()).log(Level.SEVERE, null, ex); 
        } 

         
        
            
    }

    public WRF2ROMS(  String romsGridFilename,  String wrfFilenameOrFilesPath, int timeOffset, String romsForcingFilename) throws IOException, InvalidRangeException, NCRegridderException, InterpolatorException {

        // Open the ROMS Grid
        ROMSGrid romsGrid = new ROMSGrid(romsGridFilename);

        ROMSWind romsWind=new ROMSWind(romsForcingFilename,romsGrid);;

        File folder = new File(wrfFilenameOrFilesPath);
        if (folder.isDirectory()) {
            File[] listOfFiles = folder.listFiles();
            Arrays.sort(listOfFiles);
            if (listOfFiles != null) {
                int count = 0;
                for (int i = 0; i < listOfFiles.length; i++) {
                    if (listOfFiles[i].isFile()) {
                        if (listOfFiles[i].getName().startsWith("wrf")) {
                            String wrfDataFilename = listOfFiles[i].getPath();
                            System.out.println("File " + wrfDataFilename);
                            WRFData wrfData = new WRFData(wrfDataFilename, WRFData.INTERPMETHOD_DEFAULT, interpLevels);
                            if (count == 0) {
                                System.out.println("Inizialization.");
                                romsWind.init(wrfData, timeOffset);
                            }
                            System.out.println("Adding.");
                            romsWind.add(wrfData, timeOffset);
                            count++;
                        }
                    }
                }
            }
        } else {
            WRFData wrfData = new WRFData(wrfFilenameOrFilesPath, WRFData.INTERPMETHOD_DEFAULT, interpLevels);
            romsWind.init(wrfData, timeOffset);
            romsWind.add(wrfData, timeOffset);
        }
    }
    
   

    
    
}
