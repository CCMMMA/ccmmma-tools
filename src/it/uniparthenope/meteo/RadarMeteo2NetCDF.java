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

        double lat0=40.8333;
        double lon0=14.2333;
        double rkm0=108;
        double kmPerDeg=111;

        int nRange=240;
        int nAzimut=360;
        double a=128.3;
        double b=1.67;


        if (args.length!=12) {
            System.out.println("Usage:\n... lon0 lat0 rkm0 kmPerDeg nRange nAzimut a b cabContentPath currScanXDirname dstGridFilename outputFilename");
            System.exit(-1);
        }

        lon0=Double.parseDouble(args[0]);
        lat0=Double.parseDouble(args[1]);
        rkm0=Double.parseDouble(args[2]);
        kmPerDeg=Double.parseDouble(args[3]);
        nRange=Integer.parseInt(args[4]);
        nAzimut=Integer.parseInt(args[5]);
        a=Double.parseDouble(args[6]);
        b=Double.parseDouble(args[7]);
        cabContentPath=args[8];
        currScanXDirname=args[9];
        dstGridFilename=args[10];
        outputFilename=args[11];


        RadarMeteo2NetCDF radarMeteo2NetCDF = new RadarMeteo2NetCDF(lon0,  lat0,  rkm0,  kmPerDeg,  nRange,  nAzimut,  a,  b, cabContentPath, currScanXDirname, dstGridFilename,  outputFilename);


    }

    public RadarMeteo2NetCDF(double lon0, double lat0, double rkm0, double kmPerDeg, int nRange, int nAzimut, double a, double b, String cabContentPath, String currScanXDirname, String dstGridFilename, String outputFilename) {
        Radar rmCurrScanX;
        try {
            rmCurrScanX = new Radar( lon0,  lat0,  rkm0,  kmPerDeg,  nRange,  nAzimut,  a,  b, cabContentPath+File.separator+currScanXDirname);
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

                            Radar rmPrevScan1 = new Radar(lon0,  lat0,  rkm0,  kmPerDeg,  nRange,  nAzimut,  a,  b,cabContentPath+File.separator+prevScan1CabDirname);
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
