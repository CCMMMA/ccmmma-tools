package it.uniparthenope.meteo;

import it.uniparthenope.meteo.radar.Radar;
import it.uniparthenope.meteo.radar.RadarException;
import jncregridder.util.InterpolatorException;
import ucar.ma2.InvalidRangeException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RadarMeteo2NetCDF {

    public static void main(String[] args) throws Exception {
        String cabContentPath="/Users/raffaelemontella/dev/ccmmma/radar/tmp";
        String currScanXDirname="A00-201806100510";
        String dstGridFilename="/Users/raffaelemontella/dev/ccmmma/radar/grids/latlon_radar_grads_dbl.nc";
        String outputFilename="/Users/raffaelemontella/dev/ccmmma/radar/output/rdr1_d04_20180610Z0510.nc";


        if (args.length!=4) {
            System.out.println("Usage:\n... cabContentPath currScanXDirname dstGridFilename outputFilename");
            System.exit(-1);
        }

        cabContentPath=args[0];
        currScanXDirname=args[1];
        dstGridFilename=args[2];
        outputFilename=args[3];


        RadarMeteo2NetCDF radarMeteo2NetCDF = new RadarMeteo2NetCDF(cabContentPath, currScanXDirname, dstGridFilename,  outputFilename);


    }

    public RadarMeteo2NetCDF(String cabContentPath, String currScanXDirname, String dstGridFilename, String outputFilename) {
        Radar rmCurrScanX;
        try {
            rmCurrScanX = new Radar(cabContentPath+File.separator+currScanXDirname);
            try {
                System.out.println("Load destination grid");
                rmCurrScanX.loadDstGrid(dstGridFilename);

                System.out.println("Create source grid");
                rmCurrScanX.createSrcGrid();
                try {
                    System.out.println("Reshaping...");
                    rmCurrScanX.reshape();


                    if (rmCurrScanX.getScanId().equals("Scan_0")) {
                        GregorianCalendar gcDate = rmCurrScanX.getDate();
                        gcDate.add(GregorianCalendar.MINUTE, -5);
                        String prevScan1CabDirname=String.format("A00-%04d%02d%02d%02d%02d",
                                gcDate.get(GregorianCalendar.YEAR),
                                gcDate.get(GregorianCalendar.MONTH)+1,
                                gcDate.get(GregorianCalendar.DAY_OF_MONTH),
                                gcDate.get(GregorianCalendar.HOUR_OF_DAY),
                                gcDate.get(GregorianCalendar.MINUTE));
                        System.out.println("Try to use Scan_1 to compose ("+prevScan1CabDirname+")");
                        File tryFile=new File(cabContentPath+File.separator+prevScan1CabDirname);
                        if (tryFile.exists()==true) {

                            Radar rmPrevScan1 = new Radar(cabContentPath+File.separator+prevScan1CabDirname);
                            System.out.println("Load destination grid");
                            rmPrevScan1.loadDstGrid(dstGridFilename);
                            System.out.println("Create source grid");
                            rmPrevScan1.createSrcGrid();
                            System.out.println("Reshaping...");
                            rmPrevScan1.reshape();
                            double[][] rain = rmPrevScan1.getRAIN();
                            System.out.println("Composing...");
                            rmCurrScanX.compose(rain);
                        } else {
                            System.out.println("No Scan_1 available!");
                        }
                    }
                    rmCurrScanX.saveAsNetCDF(outputFilename);
                } catch (InterpolatorException ex) {
                    Logger.getLogger(RadarMeteo2NetCDF.class.getName()).log(Level.SEVERE, null, ex);
                }
            } catch (InvalidRangeException ex) {
                Logger.getLogger(RadarMeteo2NetCDF.class.getName()).log(Level.SEVERE, null, ex);
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(RadarMeteo2NetCDF.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(RadarMeteo2NetCDF.class.getName()).log(Level.SEVERE, null, ex);
        } catch (RadarException ex) {
            Logger.getLogger(RadarMeteo2NetCDF.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
