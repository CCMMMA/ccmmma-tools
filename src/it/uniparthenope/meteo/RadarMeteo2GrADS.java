/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.uniparthenope.meteo;

import it.uniparthenope.meteo.radar.Radar;
import it.uniparthenope.meteo.radar.RadarException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import jncregridder.util.InterpolatorException;
import ucar.ma2.InvalidRangeException;



/**
 *
 * @author raffaelemontella
 */
public class RadarMeteo2GrADS {
    
    
    
    public static void main(String[] args) throws Exception {
        String cabContentPath="/Users/raffaelemontella/dev/radar/tmp";
        String currScanXDirname="A00-201801120600";
        String dstGridFilename="/Users/raffaelemontella/dev/radar/grids/latlon_radar_grads_dbl.nc";
        String outputPath="/Users/raffaelemontella/dev/radar/output";
        String fileNameBase="rdr1_d04_";

        double lat0=40.8333;
        double lon0=14.2333;
        double rkm0=108;
        double kmPerDeg=111;

        int nRange=240;
        int nAzimut=360;
        double a=128.3;
        double b=1.67;

        if (args.length!=13) {
            System.out.println("Usage:\n... lon0 lat0 rkm0 kmPerDeg nRange nAzimut a b cabContentPath currScanXDirname dstGridFilename outputPath fileNameBase");
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
        outputPath=args[11];
        fileNameBase=args[12];

        RadarMeteo2GrADS radarMeteo2GrADS = new RadarMeteo2GrADS(lon0,  lat0,  rkm0,  kmPerDeg,  nRange,  nAzimut,  a,  b, cabContentPath, currScanXDirname, dstGridFilename, outputPath, fileNameBase);
        
        
    }
    
    public RadarMeteo2GrADS(double lon0, double lat0, double rkm0, double kmPerDeg, int nRange, int nAzimut, double a, double b,String cabContentPath, String currScanXDirname, String dstGridFilename, String outputPath, String fileNameBase) {
        Radar rmCurrScanX;
        try {
            rmCurrScanX = new Radar(lon0,  lat0,  rkm0,  kmPerDeg,  nRange,  nAzimut,  a,  b,cabContentPath+File.separator+currScanXDirname);
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
                    rmCurrScanX.saveAsGrADS(outputPath,fileNameBase);
                } catch (InterpolatorException ex) {
                    Logger.getLogger(RadarMeteo2GrADS.class.getName()).log(Level.SEVERE, null, ex);
                }
            } catch (InvalidRangeException ex) {
                Logger.getLogger(RadarMeteo2GrADS.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(RadarMeteo2GrADS.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(RadarMeteo2GrADS.class.getName()).log(Level.SEVERE, null, ex);
        } catch (RadarException ex) {
            Logger.getLogger(RadarMeteo2GrADS.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    
    
}
