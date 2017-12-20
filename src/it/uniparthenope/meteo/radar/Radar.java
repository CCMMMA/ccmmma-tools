/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.uniparthenope.meteo.radar;

import com.mindprod.ledatastream.LEDataInputStream;
import com.mindprod.ledatastream.LEDataOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.GregorianCalendar;
import jncregridder.util.InterpolatorBase;
import jncregridder.util.InterpolatorException;
import jncregridder.util.PolarInterpolator;
import ucar.ma2.ArrayDouble;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;

/**
 *
 * @author raffaelemontella
 */
public class Radar {
    private String[] months= { null,
        "jan","feb","mar",
        "apr","may","jun",
        "jul","aug","sep",
        "oct","nov","dec"
    };
    
    float undef=-9.99e+08f;
    private Dimension dstDimLat = null;
    private Dimension dstDimLon = null;
    private double[][] dstLAT=null;
    private double[][] dstLON=null;
    
    private double dstLonStep;
    private double dstLatStep;
    private double dstLatMin;
    private double dstLonMin;
    private double dstLatMax;
    private double dstLonMax;    
    private int rows,cols;
    private String scanId=null;
    private double lat0=40.8333;
    private double lon0=14.2333;
    private double rkm0=-1;
    
    private double[][] srcLAT=null;
    private double[][] srcLON=null;
    
    private int[][] dstMASK=null;
    private int[][] srcMASK=null;
    private double[][] srcData=null;
    private double[][] dstData=null;
    private int rData=120;
    private int nData=-1;
    
    
    private GregorianCalendar gcDate=null;
    
    
    public Radar(String cabContentPath) throws FileNotFoundException, IOException, RadarException {
        
        
        
        File dir = new File(cabContentPath);
        
        FilenameFilter filterBin = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith("-C.z");
            }
        };
        
        FilenameFilter filterScan = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".Scan");
            }
        };
        
        String[] childrenScan = dir.list(filterScan);
        if (childrenScan != null) {
            String filenameScan = childrenScan[0];
            FileReader fr = new FileReader(cabContentPath+File.separator+filenameScan);
            BufferedReader br = new BufferedReader(fr);
            scanId=br.readLine();
            br.close();
            fr.close();
            
            System.out.println("Scan: "+scanId);
            if (scanId.equals("Scan_0")==true) {
                // 36km
                rkm0=36;
                nData=123;
            } else if (scanId.equals("Scan_1")==true) {
                // 72km
                rkm0=72;
                nData=243;
            } else throw new RadarException("Unknown Scan ID: "+scanId+" !");
            
            // 201209141600
            // 012345678901
            
            String NCEPDate = filenameScan.replace("A00-", "").replace(".Scan","");
            int year=Integer.parseInt(NCEPDate.substring(0,4));
            int month=Integer.parseInt(NCEPDate.substring(4,6));;
            int day=Integer.parseInt(NCEPDate.substring(6,8));;
            int hour=Integer.parseInt(NCEPDate.substring(8,10));;
            int min=Integer.parseInt(NCEPDate.substring(10,12));;
            gcDate=new GregorianCalendar(year,month-1,day,hour,min,0);
            
            srcData = new double[nData][rData];

            String[] childrenBin = dir.list(filterBin);
            if (childrenBin != null) {
                for (int k=0; k<childrenBin.length; k++) {
                    // Get filename of file or directory
                    String filenameBin = childrenBin[k];

                    System.out.println("Processing: "+filenameBin);

                    FileInputStream fis = new FileInputStream(cabContentPath+File.separator+filenameBin);
                    LEDataInputStream ledis = new LEDataInputStream( fis );



                    for (int j=0; j < nData; j++) {
                        for (int i=0; i < rData; i++) {

                            short nextShort = ledis.readShort();
                            double nextDouble = (nextShort-64)/2.;
                            if (nextDouble>srcData[j][i]) {
                                srcData[j][i]=nextDouble;
                            }
                            //int nextInt = (buffer[0] & 0xFF) | (buffer[1] & 0xFF) << 8 | (buffer[2] & 0xFF) << 16 | (buffer[3] & 0xFF) << 24;

                        }
                    }


                    ledis.close();
                    fis.close();   
                }

                for (int j=0; j < nData; j++) {
                    for (int i=0; i < rData; i++) {
                        double rain=Math.pow(Math.pow(10, srcData[j][i]/10)/443.5,0.77);
                        srcData[j][i]=rain;
                    }
                }
            } 
        }
    }
    
    
    
    
    
    
    
    
    public void loadDstGrid(String dstGridFilename) throws IOException, InvalidRangeException {
        NetcdfDataset ncGrid=NetcdfDataset.openDataset(dstGridFilename);
        dstDimLat = ncGrid.findDimension("latitude");
        dstDimLon = ncGrid.findDimension("longitude");
        rows = dstDimLat.getLength();
        cols = dstDimLon.getLength();
        Variable varLatGrid = ncGrid.findVariable("latgrid");
        Variable varLonGrid = ncGrid.findVariable("longrid");
        ArrayDouble.D2 aLat = (ArrayDouble.D2)varLatGrid.read(new int[] { 0, 0 }, new int[] {rows,cols});
        ArrayDouble.D2 aLon = (ArrayDouble.D2)varLonGrid.read(new int[] { 0, 0 }, new int[] {rows,cols});
        dstLAT = (double[][])aLat.copyToNDJavaArray();
        dstLON = (double[][])aLon.copyToNDJavaArray();
        
        dstLatMin=dstLAT[0][0];
        dstLonMin=dstLON[0][0];
        dstLatMax=dstLAT[rows-1][cols-1];
        dstLonMax=dstLON[rows-1][cols-1];
        
        for (int j=0;j<rows;j++) {
            for (int i=0;i<cols;i++) {
                if (dstLAT[j][i]<dstLatMin) dstLatMin=dstLAT[j][i];
                if (dstLAT[j][i]>dstLatMax) dstLatMax=dstLAT[j][i];
                if (dstLON[j][i]<dstLonMin) dstLonMin=dstLON[j][i];
                if (dstLON[j][i]>dstLonMax) dstLonMax=dstLON[j][i];
            }
        }
        
        dstLonStep = (dstLonMax-dstLonMin)/cols;
        dstLatStep = (dstLatMax-dstLatMin)/rows;
        
        dstMASK=new int[rows][cols];
        
        for (int j=0;j<cols;j++) {
            for (int i=0;i<rows;i++) {
                dstMASK[j][i]=0;
                
            }
        }
        
    }
    
    public void createSrcGrid() {
        double[] rkm = new double[nData];
        for (int j=0;j<nData;j++) {
           rkm[j]=rkm0*(j-.5)/nData; 
        }
        double[] z= new double[rData];
        for (int i=0;i<rData;i++) {
            z[i]=Math.PI*(3.+((i-.5)*3.))/180;
        }
        
        //double lat1=lat0+Math.cos(z[z.length-1])*rkm[rkm.length-1]/111;
        //double lon1=lon0+Math.sin(z[z.length-1])*(rkm[rkm.length-1]/111)/Math.cos(Math.PI*lat1/180);
        //double r0=Math.pow((lon1-lon0)*(lon1-lon0)+(lat1-lat0)*(lat1-lat0), .5);
        
        srcMASK = new int[nData][rData];
        
        srcLON = new double[nData][rData];
        srcLAT = new double[nData][rData];
        
        for (int j=0;j<nData;j++) {
            for (int i=0;i<rData;i++) {
                srcLAT[j][i]=lat0+Math.cos(z[i])*rkm[j]/111;
                srcLON[j][i]=lon0+Math.sin(z[i])*(rkm[j]/111)/Math.cos(Math.PI*srcLAT[j][i]/180);
                if (j!=0) {
                    srcMASK[j][i]=1;
                } else {
                    // System.out.println(srcLON[j][i]+","+srcLAT[j][i]);
                    srcMASK[j][i]=0;
                }
            }
        }
        
        double srcLonMin=srcLON[0][0];
        double srcLonMax=srcLON[0][0];
        double srcLatMin=srcLAT[0][0];
        double srcLatMax=srcLAT[0][0];
        
        for (int j=0;j<nData;j++) {
            for (int i=0;i<rData;i++) {
                if (srcLON[j][i]<srcLonMin) srcLonMin=srcLON[j][i];
                if (srcLON[j][i]>srcLonMax) srcLonMax=srcLON[j][i];
                if (srcLAT[j][i]<srcLatMin) srcLatMin=srcLAT[j][i];
                if (srcLAT[j][i]>srcLatMax) srcLatMax=srcLAT[j][i];
            }
        }
        System.out.println("src:"+srcLonMin+";"+srcLatMin+"-"+srcLonMax+";"+srcLatMax);
    }
    
    public void saveAsGrADS(String outputPath, String fileNameBase) throws FileNotFoundException, IOException {
        String gradsStartDate="00Z29aug2012";
            
        int year = gcDate.get(Calendar.YEAR);
        int month = gcDate.get(Calendar.MONTH)+1;
        int day = gcDate.get(Calendar.DAY_OF_MONTH);
        int hour = gcDate.get(Calendar.HOUR_OF_DAY);
        int min = gcDate.get(Calendar.MINUTE);

        gradsStartDate=String.format("%02d", hour)+":"+String.format("%02d", min)+"Z"+String.format("%02d", day)+months[month]+year;

            
        String fileNameRoot=fileNameBase+String.format("%04d%02d%02dZ%02d%02d", year,month,day,hour,min);
        
        System.err.println("Writing");
        String strFilePath=outputPath+"/"+fileNameRoot+".dat";

        FileOutputStream fos = new FileOutputStream(strFilePath);
        LEDataOutputStream ledos = new LEDataOutputStream(fos);

        for(int j=0;j<rows;j++) {
            for (int i=0;i<cols;i++) {
                float value=(float)dstData[j][i];
                
                if (value<0.01) {
                    value=undef;
                } // else { System.out.println(dstData[j][i]+"->"+value); }
                ledos.writeFloat(value);

            }
        }
        
        for(int j=0;j<rows;j++) {
            for (int i=0;i<cols;i++) {
                ledos.writeFloat((float)dstMASK[j][i]);

            }
        }
        
        ledos.close();
        fos.close();
        
        
        strFilePath=outputPath+"/"+fileNameRoot+".ctl";
        FileWriter fw = new FileWriter(strFilePath);
        PrintWriter pw = new PrintWriter(fw,true);

        String controlText="DSET  ^"+fileNameRoot+".dat\n"+
            "TITLE Gridded Data Sample\n"+
            "UNDEF "+undef+"\n"+
            //"OPTIONS byteswapped\n"+
            "XDEF "+cols+" LINEAR "+dstLonMin+"  "+dstLonStep+"\n"+
            "YDEF "+rows+" LINEAR "+dstLatMin+"  "+dstLatStep+"\n"+
            "ZDEF   1 LEVELS 0\n"+
            "TDEF   1 LINEAR "+gradsStartDate+" 5mn\n"+
            "VARS      2\n"+
            "rain     0  99  rain\n"+
            "mask     0  99  data mask\n"+
            "ENDVARS\n";

        pw.println(controlText);
        pw.close();
        fw.close();
        System.err.println("Done");
        
    }
    
    public void reshape() throws InterpolatorException {
        InterpolatorBase interpolator=new PolarInterpolator(srcLAT,srcLON, dstLAT, dstLON, srcMASK, dstMASK,null);
        dstData = interpolator.interp(srcData, undef, undef, null);
        
    }

    public String getScanId() {
        return scanId;
    }

    public GregorianCalendar getDate() {
        return (GregorianCalendar)gcDate.clone();
    }

    public double[][] getRAIN() {
        return dstData;
    }

    public void compose(double[][] rain) {
        for (int j=0;j<rows;j++) {
            for (int i=0;i<cols;i++) {
                if ((dstData[j][i]==undef && rain[j][i]!=undef) || (dstData[j][i]==0.0 && rain[j][i]!=0.0)) {
                    dstData[j][i]=rain[j][i];
                }
            }
        }
    }
}
