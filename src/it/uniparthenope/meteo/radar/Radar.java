/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.uniparthenope.meteo.radar;

import com.mindprod.ledatastream.LEDataInputStream;
import com.mindprod.ledatastream.LEDataOutputStream;

import java.io.*;
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
    private double rkm0=108;
    private double kmPerDeg=111;
    
    private double[][] srcLAT=null;
    private double[][] srcLON=null;
    
    private int[][] dstMASK=null;
    private int[][] srcMASK=null;

    private double[][] srcReflectivity=null;
    private double[][] srcRain=null;

    private double[][] dstReflectivity=null;
    private double[][] dstRain=null;

    private int nRange=240;
    private int nAzimut=360;
    private double a=128.3;
    private double b=1.67;



    private double azimutRes=1;
    
    
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
                //rkm0=36;
                //nData=123;

            } else if (scanId.equals("Scan_1")==true) {
                // 72km
                //rkm0=72;
                //nData=243;
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


            srcReflectivity = new double[nAzimut+1][nRange];
            for (int j=0;j<nAzimut;j++) {
                for (int i=0;i<nRange;i++) {
                    srcReflectivity[j][i]=-999;
                }
            }

            srcRain = new double[nAzimut+1][nRange];


            String[] childrenBin = dir.list(filterBin);
            if (childrenBin != null) {
                for (int k=0; k<childrenBin.length; k++) {
                    // Get filename of file or directory
                    String filenameBin = childrenBin[k];

                    System.out.println("Processing: "+filenameBin);

                    FileInputStream fis = new FileInputStream(cabContentPath+File.separator+filenameBin);
                    LEDataInputStream ledis = new LEDataInputStream( fis );



                    for (int j=0; j < nAzimut; j++) {
                        Header header=new Header(ledis);

                        for (int i=0; i < nRange; i++) {
                            int nextInt=ledis.readUnsignedShort();

                            int dataFormat=header.getDataFormat();
                            double q_Z2Level=(Math.pow(2,dataFormat)-1)*(-(-32)/(95.5-(-32)));
                            double m_Z2Level=(Math.pow(2,dataFormat)-1)/(95.5-(-32));
                            double dBZ=(nextInt-q_Z2Level)/m_Z2Level;

                            if (dBZ>srcReflectivity[j][i]) {
                                srcReflectivity[j][i]=dBZ;
                            }

                            //int nextInt = (buffer[0] & 0xFF) | (buffer[1] & 0xFF) << 8 | (buffer[2] & 0xFF) << 16 | (buffer[3] & 0xFF) << 24;

                        }
                    }


                    ledis.close();
                    fis.close();   
                }


                double r10, rain;
                //a=443.5;
                //b=1.3;

                for (int j=0; j < nAzimut; j++) {
                    for (int i=0; i < nRange; i++) {
                        r10=Math.pow(10,srcReflectivity[j][i]/10);
                        rain=Math.pow(r10/a,1/b);
                        srcRain[j][i]=rain;
                    }
                }
            }
            for (int i=0; i < nRange; i++) {
                srcReflectivity[nAzimut][i]=srcReflectivity[0][i];
                srcRain[nAzimut][i]=srcRain[0][i];
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
        double[] rkm = new double[nRange];
        for (int i=0;i<nRange;i++) {
           rkm[i]=rkm0*(i-.5)/nRange;
        }
        double[] z= new double[nAzimut];
        for (int j=0;j<nAzimut;j++) {
            //z[i]=Math.PI*(3.+((i-.5)*3.))/180;
            z[j]=Math.PI*(azimutRes+((j-.5)*azimutRes))/180;
        }
        
        //double lat1=lat0+Math.cos(z[z.length-1])*rkm[rkm.length-1]/111;
        //double lon1=lon0+Math.sin(z[z.length-1])*(rkm[rkm.length-1]/111)/Math.cos(Math.PI*lat1/180);
        //double r0=Math.pow((lon1-lon0)*(lon1-lon0)+(lat1-lat0)*(lat1-lat0), .5);
        
        srcMASK = new int[nAzimut+1][nRange];
        
        srcLON = new double[nAzimut+1][nRange];
        srcLAT = new double[nAzimut+1][nRange];
        
        for (int j=0;j<nAzimut;j++) {
            for (int i=0;i<nRange;i++) {
                srcLAT[j][i]=lat0+Math.cos(z[j])*rkm[i]/kmPerDeg;
                srcLON[j][i]=lon0+Math.sin(z[j])*(rkm[i]/kmPerDeg)/Math.cos(Math.PI*srcLAT[j][i]/180);
                if (i!=0) {
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
        
        for (int j=0;j<nAzimut;j++) {
            for (int i=0;i<nRange;i++) {
                if (srcLON[j][i]<srcLonMin) srcLonMin=srcLON[j][i];
                if (srcLON[j][i]>srcLonMax) srcLonMax=srcLON[j][i];
                if (srcLAT[j][i]<srcLatMin) srcLatMin=srcLAT[j][i];
                if (srcLAT[j][i]>srcLatMax) srcLatMax=srcLAT[j][i];
            }
        }
        for (int i=0; i < nRange; i++) {
            srcLON[nAzimut][i]=srcLON[0][i];
            srcLAT[nAzimut][i]=srcLAT[0][i];
            srcMASK[nAzimut][i]=srcMASK[0][i];
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
                float value=(float)dstReflectivity[j][i];
                if (value<-30) {
                    value=undef;
                }
                ledos.writeFloat(value);
            }
        }


        for(int j=0;j<rows;j++) {
            for (int i=0;i<cols;i++) {
                float value=(float)dstRain[j][i];
                
                if (value<0.1) {
                    value=undef;
                }
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
            "VARS      3\n"+
            "ref1     0  99  ref1\n"+
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
        dstReflectivity = interpolator.interp(srcReflectivity, undef, undef, null);
        dstRain = interpolator.interp(srcRain, undef, undef, null);
        
    }

    public String getScanId() {
        return scanId;
    }

    public GregorianCalendar getDate() {
        return (GregorianCalendar)gcDate.clone();
    }

    public double[][] getRAIN() {
        return dstRain;
    }

    public void compose(double[][] rain) {
        for (int j=0;j<rows;j++) {
            for (int i=0;i<cols;i++) {
                if ((dstRain[j][i]==undef && rain[j][i]!=undef) || (dstRain[j][i]==0.0 && rain[j][i]!=0.0)) {
                    dstRain[j][i]=rain[j][i];
                }
            }
        }
    }
}
