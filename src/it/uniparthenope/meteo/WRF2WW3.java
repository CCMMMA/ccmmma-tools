/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.uniparthenope.meteo;

import com.sun.corba.se.spi.legacy.connection.GetEndPointInfoAgainException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;


import jncregridder.util.IDWInterpolator;
import jncregridder.util.InterpolatorBase;
import jncregridder.util.InterpolatorException;
import jncregridder.util.InterpolatorParams;
import jncregridder.util.Linear2DInterpolator;
import jncregridder.util.NCRegridderException;
import jncregridder.wrf.WRFData;
import jncregridder.ww3.WW3Grid;
import jncregridder.ww3.WW3Wind;
import ucar.ma2.InvalidRangeException;

/**
 *
 * @author raffaelemontella
 */
public class WRF2WW3 {

    private double[] interpLevels = new double[] {1000.,950.,900.,850.,800.,750.,700.,650.,600.,550.,500.,450.,400.,350.,300.,250.,200.,150.,100};

    public static void main(String[] args) throws IOException, NCRegridderException, InvalidRangeException, InterpolatorException {


        new WRF2WW3(
                "/Users/raffaelemontella/dev/ccmmma/ww3/data/grids/d01.grd",
                "/Users/raffaelemontella/dev/ccmmma/wrf/wrfout/",
                0,
                "/Users/raffaelemontella/dev/ccmmma/ww3/wrf5");

        /*
        try {

            if (args.length != 4) {
                System.out.println("Usage:");
                System.out.println("WRF2WW3 gridFilename wrfFilename|wrfFilespath timeOffset forcingFilename");
                System.exit(0);
            }
            new WRF2WW3(args[0],args[1],Integer.parseInt(args[2]), args[3]);

        } catch (InvalidRangeException ex) {
            Logger.getLogger(WRF2ROMS.class.getName()).log(Level.SEVERE, null, ex);
        }
        */



    }

    public WRF2WW3(  String ww3GridFilename,  String wrfFilenameOrFilesPath, int timeOffset, String ww3ForcingBasename) throws IOException, InvalidRangeException, NCRegridderException, InterpolatorException {

        // Open the ROMS Grid
        WW3Grid ww3Grid = new WW3Grid(ww3GridFilename);

        System.out.println("ww3Grid :"+ww3Grid.getName()+" "+
                ww3Grid.getCols()+","+ww3Grid.getRows()+" "+
                ww3Grid.getDX()+","+ww3Grid.getDY()+" "+
                ww3Grid.getLLX()+","+ww3Grid.getLLY()+" "+
                ww3Grid.getURX()+","+ww3Grid.getURY());

        WW3Wind ww3Wind= new WW3Wind(ww3ForcingBasename,ww3Grid);

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
                                ww3Wind.init(wrfData,timeOffset);
                            }
                            System.out.println("Adding.");
                            ww3Wind.add(wrfData, timeOffset);
                            count++;
                        }
                    }
                }
            }
        } else {
            WRFData wrfData = new WRFData(wrfFilenameOrFilesPath, WRFData.INTERPMETHOD_DEFAULT, interpLevels);
            ww3Wind.init(wrfData, timeOffset);
            ww3Wind.add(wrfData, timeOffset);
        }
    }
}
