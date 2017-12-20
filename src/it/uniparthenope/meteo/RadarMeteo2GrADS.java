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
        String cabContentPath="/Users/raffaelemontella/tmp/myocean2roms/radar/work/cabs";
        String currScanXDirname="A00-201209141600";
        String dstGridFilename="/Users/raffaelemontella/tmp/myocean2roms/radar/latlon_radar_grads_dbl.nc";
        String outputPath="/Users/raffaelemontella/tmp/myocean2roms/radar/";
        String fileNameBase="rdr1_d04_";
        
        if (args.length!=5) {
            System.out.println("Usage:\n... cabContentPath currScanXDirname dstGridFilename outputPath fileNameBase");
            System.exit(-1);
        } 
        
        cabContentPath=args[0];
        currScanXDirname=args[1];
        dstGridFilename=args[2];
        outputPath=args[3];
        fileNameBase=args[4];
        
        RadarMeteo2GrADS radarMeteo2GrADS = new RadarMeteo2GrADS(cabContentPath, currScanXDirname, dstGridFilename, outputPath, fileNameBase);
        
        
    }
    
    public RadarMeteo2GrADS(String cabContentPath, String currScanXDirname, String dstGridFilename, String outputPath, String fileNameBase) {
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
