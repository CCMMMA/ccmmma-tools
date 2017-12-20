/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jncregridder.wrf;

import flanagan.math.Fmath;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jncregridder.util.InterpolatorBase;
import jncregridder.util.InterpolatorParams;
import jncregridder.util.KInterpolator;
import jncregridder.util.NCRegridderException;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.ma2.ArrayFloat;
import ucar.nc2.Attribute;
 


/**
 *
 * @author raffaelemontella
 */
public class WRFData {
    
    private Logger logger = Logger.getLogger(WRFData.class.getName());
    
    
    private String url;
    private NetcdfDataset ncDataset;
    
    public GregorianCalendar gcSimStartDate;
    
    
    
    //////////////////////////////////////////////////
    
    public Dimension dimTime;
    public Dimension dimWestEast;
    public Dimension dimSouthNord; 
    public Dimension dimBottomTop;
    public Dimension dimWestEastStag;
    public Dimension dimSouthNordStag; 
    public Dimension dimBottomTopStag;
    
    public int southNorthDim,southNorthStagDim;
    public int westEastDim,westEastStagDim;
    public int bottomTopDim,bottomTopStagDim;
    
    public double PI = 3.141592653589793;
    public double OMEGA_E = 7.292e-5; // Angular rotation rate of the earth

   public double DEG_PER_RAD = 180./PI;
   public double RAD_PER_DEG = PI/180;

   // Mean Earth Radius in m.  The value below is consistent
   // with NCEP's routines and grids.
   public double EARTH_RADIUS_M = 6370000.;   // same as MM5 system
   public double EARTH_CIRC_M = 2.*PI*EARTH_RADIUS_M;

   public double G = 9.81;
   public double Rd = 287.04;
   public double Rv = 461.6;
   public double Rm = .608;
   // public double Cp = 1004.
   public double Cp = 7.*Rd/2.;
   public double Cv = Cp-Rd;
   public double CPMD = 0.887;
   public double RCP = Rd/Cp;
   public double T0 = 273.16;
   public double p0 = 100000.;
   public double GAMMA = 0.0065;
   public double GAMMA_RIP = Rd/Cp;
   public double GAMMAMD = Rm-CPMD;

   public double CELKEL = 273.15;
   public double RHOWAT = 1000.;
   public double EPS = 0.622;
   public double EZERO = 6.112;
   
   int mapProj = 0;
   int i3dflag = 0;
   double trueLat1 = 0;
   double trueLat2 = 0;
   double standLon = 0;
   
   
   public double dX,dY;
   public double cenLon, cenLat;
   public double poleLon, poleLat;
   public double bucketMM, bucketJ;
   public double moadCenLat;
   
   public int intervalSeconds;
   public int tacc;
  
   public GregorianCalendar endDate;
   public int verticalType = WRFData.VERTICALTYPE_NONE;
   public int iProgram=0;
   public boolean extrapolate=true;
   private int interpMethod;
   public double[] interpLevels = new double[100];
   public int numberOfZLevs = 0;
   public double[][][] vertArray=null;
   
   public String title;
   
    private double MISSING_VALUE = 1E30;
    
    public static final int INTERPMETHOD_DEFAULT=1;
    
    public static final int VERTICALTYPE_NONE=0;
    public static final int VERTICALTYPE_P=1;
    public static final int VERTICALTYPE_Z=2;
    
    public static final int VARIABLE_CUSTOM2D=98;
    public static final int VARIABLE_CUSTOM3D=99;
    public static final int VARIABLE_P=1;
    public static final int VARIABLE_PB=2;
    public static final int VARIABLE_PRES=3;
    public static final int VARIABLE_TK=4;
    public static final int VARIABLE_GEOPT=5;
    public static final int VARIABLE_T=6;
    public static final int VARIABLE_Z=7;
    public static final int VARIABLE_QV=8;
    public static final int VARIABLE_PH=9;
    public static final int VARIABLE_PHB=10;
    public static final int VARIABLE_SLP=11;
    public static final int VARIABLE_MU=12;
    public static final int VARIABLE_MUB=13;
    public static final int VARIABLE_ZNU=14;
    public static final int VARIABLE_ZNW=15;
    public static final int VARIABLE_PTOP=16;
    public static final int VARIABLE_PRESSURE=18;
    public static final int VARIABLE_U10=19;
    public static final int VARIABLE_V10=20;
    public static final int VARIABLE_U10M=21;
    public static final int VARIABLE_V10M=22;
    public static final int VARIABLE_U=23;
    public static final int VARIABLE_V=24;
    public static final int VARIABLE_TC=25;
    public static final int VARIABLE_HEIGHT=26;
    public static final int VARIABLE_THETA=27;
    public static final int VARIABLE_TT=28;
    public static final int VARIABLE_GHT=29;
    public static final int VARIABLE_XLONG=30;
    public static final int VARIABLE_XLAT=31;
    public static final int VARIABLE_PSFC=32;
    public static final int VARIABLE_HGT=33;
    public static final int VARIABLE_T2=34;
    public static final int VARIABLE_INSTRAIN=35;
    public static final int VARIABLE_RAINC=36;
    public static final int VARIABLE_RAINNC=37;
    public static final int VARIABLE_RH2=38;
    public static final int VARIABLE_Q2=39;
    public static final int VARIABLE_RH=40;
    public static final int VARIABLE_CLFR=41;
    public static final int VARIABLE_SWDOWN=42;
    public static final int VARIABLE_WDIR=43;
    public static final int VARIABLE_WSPD=44;
    public static final int VARIABLE_UUU=45;
    public static final int VARIABLE_VVV=46;
    public static final int VARIABLE_UST=47;
    public static final int VARIABLE_GLW=48;
            
    
    private double[][][] UUU = null;
    private double[][][] VVV = null;
    private double[][][] TT = null;
    private double[][][] PRESSURE=null;
    private double[][][] PRES=null;
    private double[][][] P=null;
    private double[][][] PB=null;
    private double[][][] PH=null;
    private double[][][] PHB=null;
    private double[][][] T=null;
    private double[][][] GEOPT=null;
    private double[][][] QV=null;
    private double[][][] TK=null;
    private double[][][] RH=null;
    private double[][][] Z=null;
    private double[][][] CUSTOM3D=null;
    private double[][] CUSTOM2D=null;
    private double[][] XLAT = null;
    private double[][] XLONG = null;
    private double[][] GHT = null;
    private double[][] SLP=null;
    private double[][] T2=null;
    private double[][] U10=null;
    private double[][] V10=null;
    private double[][] U10M=null;
    private double[][] V10M=null;
    private double[][] PSFC=null;
    private double[][] HGT=null;
    private double[][] MU=null;
    private double[][] MUB=null;
    private double[][] INSTRAIN=null;
    private double[][] RAINC=null;
    private double[][] RAINNC=null;
    private double[][] RH2=null;
    private double[][] Q2=null;
    private double[][] CLFR=null;
    private double[][] SWDOWN=null;
    private double[][] GLW=null;
    private double[][] WDIR=null;
    private double[][] WSPD=null;
    private double[][] UST=null;
    private double[] ZNU=null;
    private double[] ZNW=null;
    private double[] PTOP=null;
   
    
    
   
    private int localTime=0;
    
    public WRFData(String url, int interpMethod, double[] interpLevels) throws IOException, InvalidRangeException, NCRegridderException {
        
        ncDataset = NetcdfDataset.openDataset(url);
        
        List<Attribute> attrs = ncDataset.getGlobalAttributes();
        for (Attribute attr:attrs) {
            if (attr.getName().equals("SIMULATION_START_DATE")) {
                String simStartDate = attr.getStringValue();
                // 2011-09-20_00:00:00
                // 0123456789012345678
                // 2011-12-12Z0
                int year = Integer.parseInt(simStartDate.substring(0,4));
                int month = Integer.parseInt(simStartDate.substring(5,7));
                int day = Integer.parseInt(simStartDate.substring(8,10));
                int hour = Integer.parseInt(simStartDate.substring(11,13));
                int min = Integer.parseInt(simStartDate.substring(14,16));
                int sec = Integer.parseInt(simStartDate.substring(17,19));
                
                gcSimStartDate = new GregorianCalendar(year,month-1,day,hour,min,sec);
                
                logger.log(Level.INFO,
                        "Simulation start date: " + gcSimStartDate.get(GregorianCalendar.YEAR) +
                        "-"+(gcSimStartDate.get(GregorianCalendar.MONTH)+1)+
                        "-"+gcSimStartDate.get(GregorianCalendar.DAY_OF_MONTH)+
                        "Z"+gcSimStartDate.get(GregorianCalendar.HOUR_OF_DAY));
            
            } else if (attr.getName().equals("MAP_PROJ")) {
                mapProj = attr.getNumericValue().intValue();
                logger.log(Level.INFO, "Map projection " + mapProj);
            } else if (attr.getName().equals("TRUELAT1")) {
                trueLat1 = attr.getNumericValue().doubleValue();
                logger.log(Level.INFO, "True lat 1: " + trueLat1);
            } else if (attr.getName().equals("TRUELAT2")) {
                trueLat2 = attr.getNumericValue().doubleValue();
                logger.log(Level.INFO, "True lat 2: " + trueLat2);
            } else if (attr.getName().equals("STAND_LON")) {
                standLon = attr.getNumericValue().doubleValue();
            } else if (attr.getName().equals("TITLE")) {
                title = attr.getStringValue();
                if (title.indexOf("OUTPUT FROM GEOGRID")>-1) iProgram = 1; // output
                if (title.indexOf("OUTPUT FROM GRIDGEN")>-1) iProgram = 1; // output
                if (title.indexOf("OUTPUT FROM METGRID")>-1) iProgram = 3; // output
                if (title.indexOf("OUTPUT FROM OBSGRID")>-1) iProgram = 3; // output
                if (title.indexOf("OUTPUT FROM REAL_EM")>-1) iProgram = 6; // output
                if (title.indexOf("OUTPUT FROM WRF")>-1) iProgram = 8; // output
                if (iProgram==0) {
                    throw new NCRegridderException("Unknown file format: "+ title);
                }
                logger.log(Level.INFO, "Title: "+title+" (iProgram="+iProgram+")");
                
                
            } else if (attr.getName().equals("WEST-EAST_GRID_DIMENSION")) {
                westEastDim = attr.getNumericValue().intValue()-1;
                
            } else if (attr.getName().equals("SOUTH-NORTH_GRID_DIMENSION")) {
                southNorthDim = attr.getNumericValue().intValue()-1;
            
            } else if (attr.getName().equals("BOTTOM-TOP_GRID_DIMENSION")) {
                bottomTopDim = attr.getNumericValue().intValue()-1;
                if ( iProgram <= 1 ) bottomTopDim = 24; //  !!!  to make room for any 3D datasets
                
            } else if (attr.getName().equals("DX")) {
                dX = attr.getNumericValue().doubleValue();
            } else if (attr.getName().equals("DY")) {
                dY = attr.getNumericValue().doubleValue();
            } else if (attr.getName().equals("CEN_LAT")) {
                cenLat = attr.getNumericValue().doubleValue();
            } else if (attr.getName().equals("CEN_LON")) {
                cenLon = attr.getNumericValue().doubleValue();
            } else if (attr.getName().equals("MOAD_CEN_LAT")) {
                moadCenLat = attr.getNumericValue().doubleValue();
            } else if (attr.getName().equals("STAND_LON")) {
                standLon = attr.getNumericValue().doubleValue();
            } else if (attr.getName().equals("BUCKET_MM")) {
                bucketMM = attr.getNumericValue().doubleValue();
                if (bucketMM<0) bucketMM=0;
            } else if (attr.getName().equals("BUCKET_J")) {
                bucketJ = attr.getNumericValue().doubleValue();
                if (bucketJ<0) bucketJ=0;
            }  else if (attr.getName().equals("POLE_LAT")) {
                poleLat = attr.getNumericValue().doubleValue();
                if (poleLat<0) poleLat=0;
            } else if (attr.getName().equals("POLE_LON")) {
                poleLon = attr.getNumericValue().doubleValue();
                if (poleLon<0) poleLon=0;
            }
            
            
        }
        
        
        
        dimTime = ncDataset.findDimension("Time");
        dimWestEastStag = ncDataset.findDimension("west_east_stag");
        dimSouthNordStag = ncDataset.findDimension("south_north_stag");
        dimBottomTopStag = ncDataset.findDimension("bottom_top_stag");
        
        southNorthStagDim=dimSouthNordStag.getLength();
        westEastStagDim=dimWestEastStag.getLength();
        bottomTopStagDim=dimBottomTopStag.getLength();
        
        setInterpMethod(interpMethod);
        this.interpLevels=interpLevels;
        
        tacc=0;
        
        numberOfZLevs=0;
        if (interpMethod == -1 || interpMethod == 0) {
            interpLevels = new double[100];
            for (int k=0;k<interpLevels.length;k++) {
                interpLevels[k]= -99999.;
            }
        }
        if (Math.abs(interpMethod)==1) {
            verticalType=VERTICALTYPE_Z;
        }
        if (interpMethod==1 && interpLevels[0]>100) {
            verticalType=VERTICALTYPE_P;
        }
        if (interpMethod==1) {
            numberOfZLevs=interpLevels.length;
        }
        if (extrapolate) {
            if (verticalType!=VERTICALTYPE_P && verticalType!=VERTICALTYPE_Z) {
                extrapolate=false;
                System.out.println("WARNING: Can only extrapolate when interpolating to pressure/height fields");      
            }
        }
        
        
        
        
        if (iProgram > 6 && interpMethod!=0) {
            getInterpInfo();
        } else {
            extrapolate = false;
            verticalType = VERTICALTYPE_NONE;
            numberOfZLevs = bottomTopDim;
        }
        
        load(VARIABLE_XLAT);
        load(VARIABLE_XLONG);
    }
    
    private InterpolatorBase interpolator = null;
    public void setInterpolator(InterpolatorBase interpolator) {this.interpolator = interpolator;}
    
   public void setInterpMethod(int interpMethod) {
       if (interpMethod == -1 || interpMethod==0) {
           interpLevels = new double[100];
           for (int i=0;i<interpLevels.length;i++) {
               interpLevels[i]=-99999;
           }
       }
       if (interpMethod == 1) {
           verticalType=VERTICALTYPE_Z;
       }
       if (interpMethod == 1 && interpLevels[0] > 100) {
           verticalType=VERTICALTYPE_P;
       }
       if (interpMethod == 1) {
          numberOfZLevs = interpLevels.length;
           
       }
       if (extrapolate) {
           if (verticalType!=VERTICALTYPE_P && verticalType!=VERTICALTYPE_Z) {
               extrapolate = false;
               System.out.println("WARNING: Can only extrapolate when interpolating to pressure/height fields");
           }
       }
      
   }
   
   public int getInterpMethod() { return interpMethod; }
   
    
    
    public void calcUVMet() throws NCRegridderException {
        
        try {
            load(VARIABLE_XLONG);
            load(VARIABLE_XLAT);
            load(VARIABLE_U10);
            load(VARIABLE_V10);
        } catch (IOException ex) {
            throw new NCRegridderException(ex);
        } catch (InvalidRangeException ex) {
            throw new NCRegridderException(ex);
        }
        
        double cone = 1;
        
        double[][] aAlpha = new double[southNorthDim][westEastDim];
        double[][] aDiff  = new double[southNorthDim][westEastDim];
        
        if (mapProj==3) {
            if (i3dflag==1) {
            
            } else {
               
            }
        } else if (mapProj==1) {
            if (Math.abs(trueLat1-trueLat2)>0.1) {
                cone = (Fmath.antilog(Math.cos(trueLat1*RAD_PER_DEG))-
                        Fmath.antilog(Math.cos(trueLat2*RAD_PER_DEG)))/
                       (Fmath.antilog(Math.tan((90.-Math.abs(trueLat1))*RAD_PER_DEG*0.5))-
                       Fmath.antilog(Math.tan((90.-Math.abs(trueLat2))*RAD_PER_DEG*0.5)));
                
                //cone = (Fmath.antilog(Math.cos(trueLat1*RAD_PER_DEG))-
                //        Fmath.antilog(Math.cos(trueLat2*RAD_PER_DEG)))/
                //        Fmath.antilog(Math.tan((90.-Math.abs(trueLat1))*RAD_PER_DEG*0.5))-
                //        Fmath.antilog(Math.tan((90.-Math.abs(trueLat2))*RAD_PER_DEG*0.5));
            } else {
                cone = Math.sin(Math.abs(trueLat1)*RAD_PER_DEG);
            }
        } 
        
        
        for (int i=0;i<southNorthDim;i++) {
            for (int j=0;j<westEastDim;j++) {
                aDiff[i][j]=XLONG[i][j]-standLon;
            }
        }
        
        for (int i=0;i<southNorthDim;i++) {
            for (int j=0;j<westEastDim;j++) {
                if (aDiff[i][j] > 180 ) {
                    aDiff[i][j]-=360;
                }
                if (aDiff[i][j] < -180 ) {
                    aDiff[i][j]+=360;
                }
            }
        }
        
        for (int i=0;i<southNorthDim;i++) {
            for (int j=0;j<westEastDim;j++) {
                if (XLAT[i][j] < 0) {
                    aAlpha[i][j]=-aDiff[i][j]*cone*RAD_PER_DEG;            
                } else {
                    aAlpha[i][j]=aDiff[i][j]*cone*RAD_PER_DEG;
                }
            }
        }
        
        if (i3dflag==1) {
        
        } else {
            
           
            U10M = new double[southNorthDim][westEastDim];
            V10M = new double[southNorthDim][westEastDim];
            
            for (int i=0;i<southNorthDim;i++) {
                for (int j=0;j<westEastDim;j++) {
                    U10M[i][j]=V10[i][j]*Math.sin(aAlpha[i][j])+U10[i][j]*Math.cos(aAlpha[i][j]);
                    V10M[i][j]=V10[i][j]*Math.cos(aAlpha[i][j])-U10[i][j]*Math.sin(aAlpha[i][j]);

                }
            }
                
            
        }
        
        
    }
    
    
    public void setTime(int localTime)  {
    
        this.localTime = localTime;
        
        XLAT=null;
        XLONG=null;
        PTOP=null;
        PRESSURE=null;
        PRES=null;
        SLP=null;
        P=null;
        PB=null;
        PH=null;
        PHB=null;
        T=null;
        GEOPT=null;
        QV=null;
        TK=null;
        Z=null;
        SLP=null;
        T2=null;
        U10=null;
        V10=null;
        U10M=null;
        V10M=null;
        PSFC=null;
        HGT=null;
        MU=null;
        MUB=null;
        ZNU=null;
        ZNW=null;
        PTOP=null;
        INSTRAIN=null;
        RAINC=null;
        RAINNC=null;
        RH2=null;
        Q2=null;
        RH=null;
        CLFR=null;
        SWDOWN=null;
        GLW=null;
        UUU=null;
        VVV=null;
        WDIR=null;
        WSPD=null;
        UST=null;
    }
    
    public double[][] getWindDir() throws NCRegridderException, IOException, InvalidRangeException { return load(VARIABLE_WDIR)[0];}
    public double[][] getWindSpeed() throws NCRegridderException, IOException, InvalidRangeException { return load(VARIABLE_WSPD)[0];}
    
    
    public double[][] getSWDOWN() throws NCRegridderException, IOException, InvalidRangeException { return load(VARIABLE_SWDOWN)[0];}
    public double[][] getGLW() throws NCRegridderException, IOException, InvalidRangeException { return load(VARIABLE_GLW)[0];}
    
    public double[][] getCLFR() throws NCRegridderException, IOException, InvalidRangeException { return load(VARIABLE_CLFR)[0];}
    public double[][] getRH2() throws NCRegridderException, IOException, InvalidRangeException { return load(VARIABLE_RH2)[0];}
    
    
    public double[][] getInstRain() throws NCRegridderException, IOException, InvalidRangeException { return load(VARIABLE_INSTRAIN)[0];}
    public double[][] getRainC() throws NCRegridderException, IOException, InvalidRangeException { return load(VARIABLE_RAINC)[0];}
    public double[][] getRainNC() throws NCRegridderException, IOException, InvalidRangeException { return load(VARIABLE_RAINNC)[0];}
    public double[][] getUST() throws IOException, InvalidRangeException, NCRegridderException {
        return load(VARIABLE_UST)[0];
    }
    public double[][] getU10M() throws IOException, InvalidRangeException, NCRegridderException {
        return load(VARIABLE_U10M)[0];
    }
    public double[][] getV10M() throws IOException, InvalidRangeException, NCRegridderException { return load(VARIABLE_V10M)[0]; }
    
    public double[][] getXLAT() throws IOException, InvalidRangeException, NCRegridderException { return load(VARIABLE_XLAT)[0]; }
    public double[][] getXLONG() throws IOException, InvalidRangeException, NCRegridderException { return load(VARIABLE_XLONG)[0]; }
    public double[][] getSLP() throws NCRegridderException, IOException, InvalidRangeException { return load(VARIABLE_SLP)[0];}
    
    public double[][] getT2() throws NCRegridderException, IOException, InvalidRangeException { return load(VARIABLE_T2)[0];}
    
    public void store(int varId, double[][] var) throws NCRegridderException {
        double[][][] tmp=new double[1][1][1];
        tmp[0]=var;
        store(varId,tmp);
    }
    
    
    public void store(int varId, double[][][] var) throws NCRegridderException {
        switch (varId) {
            
            case VARIABLE_CUSTOM2D:
                CUSTOM2D=var[0];
                break;
                
            case VARIABLE_CUSTOM3D:
                CUSTOM3D=var;
                break;
                
            default:
                throw new NCRegridderException("Unknown variable");
                
        }
    }
    
    /*
     * Returns the minimum box
     */
    public double[][][] getLATLONGs() throws IOException, InvalidRangeException, NCRegridderException {
        if (XLAT==null) load(VARIABLE_XLAT);
        if (XLONG==null) load(VARIABLE_XLONG);
        
        double minXLAT=-999;
        double maxXLAT=+999;
        double minXLONG=-999;
        double maxXLONG=+999;
        double minXLATd=999;
        double minXLONGd=999;
        double maxXLATd=0;
        double maxXLONGd=0;
        double XLATd, XLONGd;
        
       
        
        
        
        
        for (int i=0;i<westEastDim; i++) {
            if (XLAT[0][i] > minXLAT) minXLAT=XLAT[0][i];
            if (XLAT[southNorthDim-1][i] < maxXLAT) maxXLAT=XLAT[southNorthDim-1][i];
        }
        
        for (int j=0;j<southNorthDim; j++) {
            if (XLONG[j][0] > minXLONG) minXLONG=XLONG[j][0];
            if (XLONG[j][westEastDim-1] < maxXLONG) maxXLONG=XLONG[j][westEastDim-1];
        }
        
        for (int j=0;j<southNorthDim-1; j++) {
            for (int i=0;i<westEastDim-1; i++) {
                
                XLATd=XLAT[j+1][i+1]-XLAT[j][i];
                if (XLATd>maxXLATd) maxXLATd=XLATd;
                if (XLATd<minXLATd) minXLATd=XLATd;

                XLONGd=XLONG[j+1][i+1]-XLONG[j][i];
                if (XLONGd>maxXLONGd) maxXLONGd=XLONGd;
                if (XLONGd<minXLONGd) minXLONGd=XLONGd;
                
            }            
        }
        
        
        //XLATd=(minXLATd+maxXLATd)/2;
        //XLONGd=(minXLONGd+maxXLONGd)/2;
        
        XLATd = minXLATd/2;
        XLONGd = minXLONGd/2;
        
        int rows = (int)((maxXLAT-minXLAT)/XLATd);
        int cols = (int)((maxXLONG-minXLONG)/XLONGd);
        
        
        double llX=minXLONG;
        double llY=minXLAT;
        
        
        System.out.println("rows:" + rows + " minXLAT:"+minXLAT+" maxXLAT:"+maxXLAT+" XLATd:"+XLATd);
        System.out.println("cols:" + cols + " minXLONG:"+minXLONG+" maxXLONG:"+maxXLONG+" XLONGd:"+XLONGd);
        
        
        double[][] lats = new double[rows][cols];
        double[][] longs = new double[rows][cols];
        
        
        for (int j=0;j<rows; j++) {
            double lat=llY+XLATd*j;
            double lon=llX;
            for (int i=0;i<cols; i++) {
                lon = llX+XLONGd*i;

                lats[j][i]=lat;
                longs[j][i]=lon;
            }
        }
        return new double[][][] {lats, longs }; 
        
        
    }
    
    public double[][][] load(int varId) throws IOException, InvalidRangeException, NCRegridderException {
        double[][][] result=null;
        
        switch (varId) {
            case VARIABLE_T:
                if (T==null) {
                    Variable  varT = ncDataset.findVariable("T");
                    if (varT!=null) {
                        T = new double[bottomTopDim][southNorthDim][westEastDim];

                        ArrayFloat.D4 aT = (ArrayFloat.D4)varT.read(new int[]  { localTime,0,0,0 }, new int [] {1,bottomTopDim,southNorthDim,westEastDim});
                        for (int k=0;k<bottomTopDim;k++) {
                            for (int j=0;j<southNorthDim; j++) {
                                for (int i=0;i<westEastDim; i++) {
                                    T[k][j][i] = aT.get(0,k,j,i);
                                }
                            }
                        }
                        
                    } else throw new NCRegridderException("T Variable not found!"); 
                }
                result = T;
            break;
                
            case VARIABLE_P:
                if (P==null) {
                    Variable  varP = ncDataset.findVariable("P");
                    if (varP!=null) {
                        P = ArrayFloatD4toDouble3D(varP);
                    } else throw new NCRegridderException("P Variable not found!"); 
                }
                result = P;
            break;
                
            case VARIABLE_PB:
                if (PB==null) {
                    Variable  varPB = ncDataset.findVariable("PB");
                    if (varPB!=null) {
                        PB = new double[bottomTopDim][southNorthDim][westEastDim];

                        ArrayFloat.D4 aPB = (ArrayFloat.D4)varPB.read(new int[]  { localTime,0,0,0 }, new int [] {1,bottomTopDim,southNorthDim,westEastDim});
                        for (int k=0;k<bottomTopDim;k++) {
                            for (int j=0;j<southNorthDim; j++) {
                                for (int i=0;i<westEastDim; i++) {
                                    PB[k][j][i] = aPB.get(0,k,j,i);
                                }
                            }
                        }
                        
                    } else throw new NCRegridderException("PB Variable not found!");
                }
                result = PB;
            break;
                
            case VARIABLE_PH:
                if (PH==null) {
                    Variable  varPH = ncDataset.findVariable("PH");
                    if (varPH!=null) {
                        PH = new double[bottomTopDim][southNorthDim][westEastDim];

                        ArrayFloat.D4 aPH = (ArrayFloat.D4)varPH.read(new int[]  { localTime,0,0,0 }, new int [] {1,bottomTopDim,southNorthDim,westEastDim});
                        for (int k=0;k<bottomTopDim;k++) {
                            for (int j=0;j<southNorthDim; j++) {
                                for (int i=0;i<westEastDim; i++) {
                                    PH[k][j][i] = aPH.get(0,k,j,i);
                                }
                            }
                        }                        
                    } else throw new NCRegridderException("PH Variable not found!"); 
                }
                result = PH;
            break;
                
            case VARIABLE_PHB:
                if (PHB==null) {
                    Variable  varPHB = ncDataset.findVariable("PHB");
                    if (varPHB!=null) {
                        PHB = new double[bottomTopDim][southNorthDim][westEastDim];

                        ArrayFloat.D4 aPHB = (ArrayFloat.D4)varPHB.read(new int[]  { localTime,0,0,0 }, new int [] {1,bottomTopDim,southNorthDim,westEastDim});
                        for (int k=0;k<bottomTopDim;k++) {
                            for (int j=0;j<southNorthDim; j++) {
                                for (int i=0;i<westEastDim; i++) {
                                    PHB[k][j][i] = aPHB.get(0,k,j,i);
                                }
                            }
                        }
                    } else throw new NCRegridderException("PB Variable not found!");
                }
                result = PHB;
            break;    
            
            case VARIABLE_PRES:
                if (PRES==null) {
                    if (iProgram>=6) {
                        try {
                            load(WRFData.VARIABLE_P);
                            load(WRFData.VARIABLE_PB);
                            PRES = new double[bottomTopDim][southNorthDim][westEastDim];
                            for (int k=0;k<bottomTopDim;k++) {
                                for (int j=0;j<southNorthDim;j++) {
                                    for (int i=0;i<westEastDim;i++) {
                                        PRES[k][j][i]=P[k][j][i]+PB[k][j][i];
                                        //System.out.println(PRES[k][j][i]+"="+P[k][j][i]+"+"+PB[k][j][i]);
                                    }
                                }
                            }
                        } catch (NCRegridderException ncre) {
                            if (iProgram==6) {
                                pressure();
                            }
                        }
                    }
                }
                result = PRES;
            break;
            
            case VARIABLE_TK:
                if (TK==null) {
                    if (iProgram>=6) {
                        load(WRFData.VARIABLE_T);
                        load(WRFData.VARIABLE_PRES);
                        TK = new double[bottomTopDim][southNorthDim][westEastDim];
                        for (int k=0;k<bottomTopDim;k++) {
                            for (int i=0;i<southNorthDim;i++) {
                                for (int j=0;j<westEastDim;j++) {
                                    TK[k][i][j]=(T[k][i][j]+300)*Math.pow((PRES[k][i][j])/p0,RCP);               
                                }
                            }
                        }   
                    }
                    
                    if (iProgram==3) {
                        /*
                        load(VARIABLE_TT);
                        result = TT;
                        */
                    }                    
                }
                result = TK;
            break;
                
            case VARIABLE_GEOPT:
                if(GEOPT==null) {
                    if (iProgram>=6) {
                       load(WRFData.VARIABLE_PH);
                       load(WRFData.VARIABLE_PHB); 
                       GEOPT = new double[bottomTopDim][southNorthDim][westEastDim];
                       for (int k=0;k<bottomTopDim;k++) {
                           for (int j=0;j<southNorthDim;j++) {
                               for (int i=0;i<westEastDim;i++) {
                                   GEOPT[k][j][i]=PH[k][j][i]+PHB[k][j][i];
                               }
                           }
                       }
                    }
                    if (iProgram==3) {
                        /*
                        load(VARIABLE_GHT);
                        result=GHT;
                        */
                    }
                }
                result = GEOPT;
            break;       
                
            case VARIABLE_Z:
                if (Z==null) {
                    load(WRFData.VARIABLE_GEOPT);
                    Z = new double[bottomTopDim][southNorthDim][westEastDim];
                    for (int k=0;k<bottomTopDim;k++) {
                        for (int j=0;j<southNorthDim;j++) {
                            for (int i=0;i<westEastDim;i++) {
                                Z[k][j][i]=GEOPT[k][j][i]/G;
                            }
                        }
                    }                  
                }
                result = Z;
            break;
                
            case VARIABLE_QV:
                if (QV==null) {
                    Variable var = ncDataset.findVariable("QVAPOR");
                    if (var!=null) {
                        QV = ArrayFloatD4toDouble3D(var);
                    } else throw new NCRegridderException("QVAPOR Variable not found!");
                }
                result = QV;
            break;
            
            case VARIABLE_UST:
                if (UST==null) {
                    Variable var = ncDataset.findVariable("UST");
                    if (var!=null) {
                        UST=ArrayFloatD3toDouble2D(var);
                        
                    } else throw new NCRegridderException("UST Variable not found!");
                }
                result = new double[1][1][1];                            
                result[0]=UST;
            break;    
                
            case VARIABLE_U10:
                if (U10==null) {
                    Variable var = ncDataset.findVariable("U10");
                    if (var!=null) {
                        U10=ArrayFloatD3toDouble2D(var);
                        
                    } else throw new NCRegridderException("U10 Variable not found!");
                }
                result = new double[1][1][1];                            
                result[0]=U10;
            break; 
            
            case VARIABLE_V10:
                if (V10==null) {
                    Variable var = ncDataset.findVariable("V10");
                    if (var!=null) {
                        V10 = ArrayFloatD3toDouble2D(var);
                    } else throw new NCRegridderException("V10 Variable not found!");
                }
                result = new double[1][1][1];
                result[0] = V10;
            break; 
            
            case VARIABLE_HGT:
                if (HGT==null) {
                    Variable var = ncDataset.findVariable("HGT");
                    if (var!=null) {
                        HGT=ArrayFloatD3toDouble2D(var);
                        
                    } else throw new NCRegridderException("HGT Variable not found!");
                }
                result = new double[1][1][1];                            
                result[0]=HGT;
            break; 
            
            case VARIABLE_XLONG:
                if (XLONG==null) {
                    Variable var = ncDataset.findVariable("XLONG");
                    if (var!=null) {
                        XLONG = ArrayFloatD3toDouble2D(var);
                    } else throw new NCRegridderException("XLONG Variable not found!");
                }
                result = new double[1][1][1];
                result[0] = XLONG;
            break; 
            
            case VARIABLE_XLAT:
                if (XLAT==null) {
                    Variable var = ncDataset.findVariable("XLAT");
                    if (var!=null) {
                        XLAT = ArrayFloatD3toDouble2D(var);
                        
                    } else throw new NCRegridderException("XLAT Variable not found!");
                }
                result = new double[1][1][1];
                result[0] = XLAT;
            break; 
                 
            case VARIABLE_PSFC:
                if (PSFC==null) {
                    Variable var = ncDataset.findVariable("PSFC");
                    if (var!=null) {
                        PSFC = ArrayFloatD3toDouble2D(var);
                    } else throw new NCRegridderException("PSFC Variable not found!");
                }
                result = new double[1][1][1];
                result[0] = PSFC;
            break; 
                
            case VARIABLE_GHT:
                if (GHT==null) {
                    Variable var = ncDataset.findVariable("GHT");
                    if (var!=null) {
                        GHT = ArrayFloatD3toDouble2D(var);
                    } else throw new NCRegridderException("GHT Variable not found!");
                }
                result = new double[1][1][1];
                result[0] = GHT;
            break; 
                
            case VARIABLE_MU:
                if (MU==null) {
                    Variable varMU = ncDataset.findVariable("MU");
                    if (varMU!=null) {
                        ArrayFloat.D3 aMU = (ArrayFloat.D3)varMU.read(new int[] { localTime,0,0}, new int[] {1,southNorthDim,westEastDim});
                        MU = new double [southNorthDim][westEastDim];
                        for (int i=0;i<southNorthDim;i++) {
                            for (int j=0;j<westEastDim; j++) {
                                MU[i][j]=aMU.get(0,i,j);                                    
                            }
                        }
                        
                    } else throw new NCRegridderException("MU Variable not found!");
                }
                result = new double[1][1][1];
                result[0] = MU;
            break;
            case VARIABLE_MUB:
                if (MUB==null) {
                    Variable varMUB = ncDataset.findVariable("MUB");
                    if (varMUB!=null) {
                        ArrayFloat.D3 aMUB = (ArrayFloat.D3)varMUB.read(new int[] { localTime,0,0}, new int[] {1,southNorthDim,westEastDim});
                        MUB = new double [southNorthDim][westEastDim];
                        for (int i=0;i<southNorthDim;i++) {
                            for (int j=0;j<westEastDim; j++) {
                                MUB[i][j]=aMUB.get(0,i,j);                                    
                            }
                        }
                        
                    } else throw new NCRegridderException("MUB Variable not found!");
                }
                result = new double[1][1][1];
                result[0] = MUB;
            break;
            
            case VARIABLE_ZNU:
                if (ZNU==null) {
                    Variable varZNU = ncDataset.findVariable("ZNU");
                    if (varZNU!=null) {
                        ArrayFloat.D2 aZNU = (ArrayFloat.D2)varZNU.read(new int[] { localTime,0}, new int[] {1,bottomTopDim});
                        ZNU = new double [bottomTopDim];
                        for (int k=0;k<bottomTopDim;k++) {
                            ZNU[k]=aZNU.get(0,k);                                    
                        }
                        
                    } else throw new NCRegridderException("ZNU Variable not found!");
                }
                result = new double[1][1][1];
                result[0][0] = ZNU;
            break;
            
            case VARIABLE_ZNW:
                if (ZNW==null) {
                    Variable varZNW = ncDataset.findVariable("ZNW");
                    if (varZNW!=null) {
                        ArrayFloat.D2 aZNW = (ArrayFloat.D2)varZNW.read(new int[] { localTime,0}, new int[] {1,bottomTopDim});
                        ZNW = new double [bottomTopDim];
                        for (int k=0;k<bottomTopDim;k++) {
                            ZNW[k]=aZNW.get(0,k);                                    
                        }
                        
                    } else throw new NCRegridderException("ZNW Variable not found!");
                }
                result = new double[1][1][1];
                result[0][0] = ZNW;
            break;
                
            case VARIABLE_PTOP:
                if (PTOP==null) {
                    Variable varPTOP = ncDataset.findVariable("P_TOP");
                    if (varPTOP!=null) {
                        ArrayFloat.D1 aPTOP = (ArrayFloat.D1)varPTOP.read(new int[] { localTime}, new int[] {1});
                        PTOP = new double[1];
                        PTOP[0]=aPTOP.get(0);                                    
                        
                    } else throw new NCRegridderException("P_TOP Variable not found!");
                }
                result = new double[1][1][1];
                result[0][0][0] = PTOP[0];
            break;
                    
                    
           case VARIABLE_PRESSURE:
                if (PRESSURE==null) {
                    load(WRFData.VARIABLE_PRES);
                    PRESSURE = new double [bottomTopDim][southNorthDim][westEastDim];
                    for (int k=0;k<bottomTopDim;k++) {
                        for (int j=0;j<southNorthDim;j++) {
                            for (int i=0;i<westEastDim; i++) {
                                PRESSURE[k][j][i]=PRES[k][j][i]*0.01;                                    
                            }
                        }
                    }                
                }
                result = PRESSURE;
            break; 
               
            case VARIABLE_SLP:
                if (SLP==null) {
                    calcSlp();
                }
                result = new double[1][1][1];
                result[0] = SLP;
            break;
                
            case VARIABLE_U10M:
                if (U10M==null || V10M==null) {
                    calcUVMet();
                }
                result = new double[1][1][1];
                result[0] = U10M;
            break;
                
            case VARIABLE_V10M:
                if (U10M==null || V10M==null) {
                    calcUVMet();
                }
                result = new double[1][1][1];
                result[0] = V10M;
            break;
                
            case VARIABLE_T2:
                if (T2==null) {
                    Variable var = ncDataset.findVariable("T2");
                    if (var!=null) {
                        T2 = ArrayFloatD3toDouble2D(var);                      
                    } else throw new NCRegridderException("T2 Variable not found!");
                }
                result = new double[1][1][1];
                result[0] = T2;
            break;
                
            case VARIABLE_Q2:
                if (Q2==null) {
                    Variable var = ncDataset.findVariable("Q2");
                    if (var!=null) {
                        Q2 = ArrayFloatD3toDouble2D(var);                      
                    } else throw new NCRegridderException("Q2 Variable not found!");
                }
                result = new double[1][1][1];
                result[0] = Q2;
            break;
                
            case VARIABLE_RAINC:
                if (RAINC==null) {
                    Variable var = ncDataset.findVariable("RAINC");
                    if (var!=null) {
                        RAINC = ArrayFloatD3toDouble2D(var);                      
                    } else throw new NCRegridderException("RAINC Variable not found!");
                }
                result = new double[1][1][1];
                result[0] = RAINC;
            break;
                
            case VARIABLE_RAINNC:
                if (RAINNC==null) {
                    Variable var = ncDataset.findVariable("RAINNC");
                    if (var!=null) {
                        RAINNC = ArrayFloatD3toDouble2D(var);                      
                    } else throw new NCRegridderException("RAINNC Variable not found!");
                }
                result = new double[1][1][1];
                result[0] = RAINNC;
            break;
                
            case VARIABLE_INSTRAIN:
                if (INSTRAIN==null) {
                    INSTRAIN= new double[southNorthDim][westEastDim];
                    if (localTime>0) {
                        load(WRFData.VARIABLE_RAINC);
                        load(WRFData.VARIABLE_RAINNC);

                        for (int j=0;j<southNorthDim;j++) {
                            for (int i=0;i<westEastDim; i++) {
                                INSTRAIN[j][i] = RAINNC[j][i]+RAINC[j][i];
                            }
                        }
                        
                        try {
                            Variable varRAINC = ncDataset.findVariable("RAINC");
                            Variable varRAINNC = ncDataset.findVariable("RAINNC");
                            ArrayFloat.D3 aRAINC = (ArrayFloat.D3)varRAINC.read(new int[] { localTime-1,0,0}, new int[] {1,southNorthDim,westEastDim});
                            ArrayFloat.D3 aRAINNC = (ArrayFloat.D3)varRAINNC.read(new int[] { localTime-1,0,0}, new int[] {1,southNorthDim,westEastDim});
                            
                            float[][][] tmpRAINC = (float[][][])aRAINC.copyToNDJavaArray();
                            float[][][] tmpRAINNC = (float[][][])aRAINNC.copyToNDJavaArray();
                            
                            for (int j=0;j<southNorthDim;j++) {
                                for (int i=0;i<westEastDim;i++) {
                                    INSTRAIN[j][i]=INSTRAIN[j][i]-(tmpRAINC[0][j][i]+tmpRAINNC[0][j][i]);
                                }
                            }
                        }
                        catch (InvalidRangeException ex1) { throw new NCRegridderException(ex1);}
                        catch (IOException ex1) { throw new NCRegridderException(ex1);}
                    } else {
                        for (int j=0;j<southNorthDim;j++) {
                            for (int i=0;i<westEastDim; i++) {
                                INSTRAIN[j][i] = 0;
                            }
                        }
                    }
                }
                result = new double[1][1][1];
                result[0] = INSTRAIN;
            break;
                
            case VARIABLE_RH:
                if (RH==null) {
                    load(VARIABLE_PRES);
                    load(VARIABLE_TK);
                    load(VARIABLE_QV);
                    
                    double tmp1, tmp2;
                    RH=new double[bottomTopDim][southNorthDim][westEastDim];
                    for (int k=0;k<bottomTopDim;k++) {
                        for (int j=0;j<southNorthDim;j++) {
                            for (int i=0;i<westEastDim;i++) {
                                tmp1 = 10.*0.6112*Math.exp(17.67*(TK[k][j][i]-T0)/(TK[k][j][i]-29.65));
                                tmp2 = EPS*tmp1/(0.01 * PRES[k][j][i] -  (1.-EPS)*tmp1);
                                RH[k][j][i] = 100.*Math.max(Math.min(QV[k][j][i]/tmp2,1.0),0.0);

                            }
                        }
                    }
                    
                }
                result = RH;
            break;
                
            case VARIABLE_RH2:
                if (RH2==null) {
                    load(VARIABLE_PSFC);
                    load(VARIABLE_T2);
                    load(VARIABLE_Q2);
                    
                    double tmp1, tmp2;
                    RH2=new double[southNorthDim][westEastDim];
                    for (int j=0;j<southNorthDim;j++) {
                        for (int i=0;i<westEastDim;i++) {
                            tmp1 = 10.*0.6112*Math.exp(17.67*(T2[j][i]-T0)/(T2[j][i]-29.65));
                            tmp2     = EPS*tmp1/(0.01 * PSFC[j][i] -  (1.-EPS)*tmp1);
                            RH2[j][i]=100.*Math.max(Math.min(Q2[j][i]/tmp2,1.0),0.0);
                        }
                    }
                    
                }
                result = new double[1][1][1];
                result[0] = RH2;
            break;
                
            case VARIABLE_CLFR:
                if (CLFR==null) {
                    load(VARIABLE_RH);
                    
                    CLFR=new double[southNorthDim][westEastDim];
                    for (int j=0;j<southNorthDim;j++) {
                        for (int i=0;i<westEastDim;i++) {
                                CLFR[j][i]=0;
                        }
                    }
                        
                    for (int j=0;j<southNorthDim;j++) {
                        for (int i=0;i<westEastDim;i++) {
                            for (int k=0;k<bottomTopDim;k++) {
                                CLFR[j][i]=Math.max(RH[k][j][i], CLFR[j][i]);
                            }
                            
                            CLFR[j][i]=4*CLFR[j][i]/100-3;
                            CLFR[j][i]=Math.min(CLFR[j][i],1);
                            CLFR[j][i]=Math.max(CLFR[j][i],0);
                        }
                    }
                    
                }
                result = new double[1][1][1];
                result[0] = CLFR;
            break;
                
            case VARIABLE_SWDOWN:
                if (SWDOWN==null) {
                    Variable var = ncDataset.findVariable("SWDOWN");
                    if (var!=null) {
                        SWDOWN=ArrayFloatD3toDouble2D(var);
                        
                    } else throw new NCRegridderException("SWDOWN Variable not found!");
                }
                result = new double[1][1][1];                            
                result[0]=SWDOWN;
                
            break;
            
            case VARIABLE_GLW:
                if (GLW==null) {
                    Variable var = ncDataset.findVariable("GLW");
                    if (var!=null) {
                        GLW=ArrayFloatD3toDouble2D(var);
                        
                    } else throw new NCRegridderException("GLW Variable not found!");
                }
                result = new double[1][1][1];                            
                result[0]=GLW;
                
            break;
                
            case VARIABLE_WDIR:
                if (WDIR==null) {
                    WDIR=new double[southNorthDim][westEastDim];
                    if (i3dflag==1) {
                        load(VARIABLE_UUU);
                        load(VARIABLE_VVV);
                        
                        for (int j=0;j<southNorthDim;j++) {
                            for (int i=0;i<westEastDim;i++) {
                                WDIR[j][i]=270. - Math.atan2(VVV[0][j][i],UUU[0][j][i]) * DEG_PER_RAD;
                            }
                        }
                    } else {
                        load(VARIABLE_U10);
                        load(VARIABLE_V10);
                        
                        for (int j=0;j<southNorthDim;j++) {
                            for (int i=0;i<westEastDim;i++) {
                                WDIR[j][i]=270. - Math.atan2(V10[j][i],U10[j][i]) * DEG_PER_RAD;
                            }
                        }
                    }
                    
                    
                    
                }
                result = new double[1][1][1];
                result[0] = WDIR;
            break;
                
                case VARIABLE_WSPD:
                if (WSPD==null) {
                    WSPD=new double[southNorthDim][westEastDim];
                    if (i3dflag==1) {
                        load(VARIABLE_UUU);
                        load(VARIABLE_VVV);
                        
                        for (int j=0;j<southNorthDim;j++) {
                            for (int i=0;i<westEastDim;i++) {
                                WSPD[j][i]=Math.pow(VVV[0][j][i]*VVV[0][j][i]+UUU[0][j][i]*UUU[0][j][i],.5);
                            }
                        }
                    } else {
                        load(VARIABLE_U10);
                        load(VARIABLE_V10);
                        
                        for (int j=0;j<southNorthDim;j++) {
                            for (int i=0;i<westEastDim;i++) {
                                WSPD[j][i]=Math.pow(V10[j][i]*V10[j][i]+U10[j][i]*U10[j][i],.5);
                            }
                        }
                    }
                    
                    
                    
                }
                result = new double[1][1][1];
                result[0] = WSPD;
            break;
        }
        if (result==null) throw new NCRegridderException("Unknown variable to load! (varId="+varId+")");
        return result;
    }
    
    private double[] ArrayFloatD2toDouble1D(Variable var) throws NCRegridderException { 
        double[] result1D=null;
        try {
            ArrayFloat.D2 a = (ArrayFloat.D2)var.read(new int[] { localTime,0}, new int[] {1,bottomTopDim});
            float[][] tmp = (float[][])a.copyToNDJavaArray();
            result1D = new double[bottomTopDim];
            for (int k=0;k<bottomTopDim;k++) {
                result1D[k]=tmp[0][k];
                
            }
        }
        catch (InvalidRangeException ex1) { throw new NCRegridderException(ex1);}
        catch (IOException ex1) { throw new NCRegridderException(ex1);}
        return result1D;
    }
    
    private double[][] ArrayFloatD3toDouble2D(Variable var) throws NCRegridderException { 
        double[][] result2D=null;
        try {
            ArrayFloat.D3 a = (ArrayFloat.D3)var.read(new int[] { localTime,0,0}, new int[] {1,southNorthDim,westEastDim});
            float[][][] tmp = (float[][][])a.copyToNDJavaArray();
            result2D = new double[southNorthDim][westEastDim];
            for (int j=0;j<southNorthDim;j++) {
                for (int i=0;i<westEastDim;i++) {
                    result2D[j][i]=tmp[0][j][i];
                }
            }
        }
        catch (InvalidRangeException ex1) { throw new NCRegridderException(ex1);}
        catch (IOException ex1) { throw new NCRegridderException(ex1);}
        return result2D;
    }
    
    private double[][][] ArrayFloatD4toDouble3D(Variable var) throws NCRegridderException { 
        double[][][] result3D=null;
        try {
            ArrayFloat.D4 a = (ArrayFloat.D4)var.read(new int[] { localTime,0,0,0}, new int[] {1,bottomTopDim,southNorthDim,westEastDim});
            float[][][][] tmp = (float[][][][])a.copyToNDJavaArray();
            result3D = new double[bottomTopDim][southNorthDim][westEastDim];
            for (int k=0;k<bottomTopDim;k++) {
                for (int j=0;j<southNorthDim;j++) {
                    for (int i=0;i<westEastDim;i++) {
                        result3D[k][j][i]=tmp[0][k][j][i];
                    }
                }
            }
        }
        catch (InvalidRangeException ex1) { throw new NCRegridderException(ex1);}
        catch (IOException ex1) { throw new NCRegridderException(ex1);}
        return result3D;
    }
    
    
    
    public double[][] calcSlp() throws IOException, InvalidRangeException, NCRegridderException {
        double TC = 273.16+17.5;
        double PCONST = 10000;
        
        
        
        load(WRFData.VARIABLE_PRES);
        load(WRFData.VARIABLE_QV);
        load(WRFData.VARIABLE_T);
        load(WRFData.VARIABLE_TK);
        load(WRFData.VARIABLE_Z);
        
        
        
        double[][] tSurf = new double[southNorthDim][westEastDim];
        double[][] tSeaLevel = new double[southNorthDim][westEastDim];
        
        int[][] level = new int[southNorthDim][westEastDim];
        
        
        for (int j=0;j<southNorthDim;j++) {
            for (int i=0;i<westEastDim;i++) {
                level[j][i]=-1;
                int k=0;
                
                while (k<bottomTopDim) {
                    double p1 = PRES[k][j][i];
                    double p2 = PRES[0][j][i]-PCONST;
                    //System.out.println("p1:"+p1+" p2:"+p2);
                    if (p1 < p2) {
                        level[j][i]=k;
                        break;
                    }
                    k++;       
                }
                
                if (level[j][i]==-1) {
                    double phPa = PRES[k][j][i]/100; 
                    String msg="Troubles finding level "+(PCONST)+" above ground. " +
                            "Problems first occurred at ("+i+","+j+"). "+
                            "Surface pressure =" + phPa +"hPa";
                    throw new NCRegridderException(msg);
                }
                
            }
        }
        
        double tS=0,tSL=0;
        
        for (int j=0;j<southNorthDim;j++) {
            for (int i=0;i<westEastDim;i++) {
                int klo = Math.max(level[j][i]-1,1);
                int khi = Math.min(klo+1, bottomTopDim-1);
                
                if (klo == khi) {
                    String msg="Trapping levels are weired. "+
                            "klo="+klo+" khi="+khi+" : and they should not be equal,";
                    throw new NCRegridderException(msg);
                }
                
                double plo = PRES[klo][j][i];
                double phi = PRES[khi][j][i];
                double qvlo = QV[klo][j][i];
                double qvhi = QV[khi][j][i];
                double tlo = T[klo][j][i]*(1 + 0.608*qvlo);
                double thi = TK[khi][j][i]*(1 + 0.608*qvhi);
                double zlo = Z[klo][j][i];
                double zhi = Z[khi][j][i];
                
                double p_at_pconst = PRES[0][j][i] - PCONST;
                double t_at_pconst = (thi-(thi-tlo)*Math.log(p_at_pconst/phi)*Math.log(plo/phi));
                double z_at_pconst = (zhi-(zhi-zlo)*Math.log(p_at_pconst/phi)*Math.log(plo/phi));

                tS  = t_at_pconst*Math.pow((PRES[0][j][i]/p_at_pconst),(GAMMA*Rd/G));
                tSL = t_at_pconst+GAMMA*z_at_pconst;
                
                tSurf[j][i]=tS;
                tSeaLevel[j][i]=tSL;
                
            }
            
        }
        
        for (int j=0;j<southNorthDim;j++) {
            for (int i=0;i<westEastDim;i++) {
                boolean l1 = tSeaLevel[j][i] < TC;
                boolean l2 = tSurf[j][i] <= TC;
                boolean l3 = !l1;
                if ( l2 && l1 ) {
                    tSL= TC;
                } else {
                    tSL=(TC-0.005*(Math.pow(tSurf[j][i]-TC,2)));
                    
                }
                tSeaLevel[j][i]=tSL;
            }
        }
        double slp=0,pres0=0;
        SLP = new double[southNorthDim][westEastDim];
        for (int j=0;j<southNorthDim;j++) {
            for (int i=0;i<westEastDim;i++) {
                double z_half_lowest = Z[0][j][i];
                double a = (2.*G*z_half_lowest);
                double b = tSeaLevel[j][i]+tSurf[j][i];
                pres0=PRES[0][j][i];
                slp=pres0*Math.exp(a/(Rd*b));
                SLP[j][i]=slp*0.01;
            }
        }
        
        return SLP;
    }
    
    
    double[][][] destag(double[][][] src) {
        int maxK=src.length;
        int maxJ=src[0].length;
        int maxI=src[0][0].length;
        
        int kk=0, jj=0, ii=0;
        
        if (maxK>bottomTopDim) { kk=1; }
        else if (maxJ>bottomTopDim) { jj=1; }
        else if (maxI>bottomTopDim) { ii=1; }
        else return src;
        
        double[][][] dst = new double[bottomTopDim][southNorthDim][westEastDim];
        for (int k=kk;k<bottomTopDim;k++) {
            for (int j=jj;j<southNorthDim;j++) {
                for (int i=ii;i<westEastDim;i++) {
                    dst[k][j][i]=0.5*(src[k][j][i]+src[k+kk][j+jj][i+ii]);
                }
            }
        }
        
        return dst;
    }
    
    double[][] destag(double[][] src) {
        int maxJ=src.length;
        int maxI=src[0].length;
        
        int jj=0, ii=0;
        
        if (maxJ>bottomTopDim) { jj=1; }
        else if (maxI>bottomTopDim) { ii=1; }
        else return src;
        
        double[][] dst = new double[southNorthDim][westEastDim];
        for (int j=jj;j<southNorthDim;j++) {
            for (int i=ii;i<westEastDim;i++) {
                dst[j][i]=0.5*(src[j][i]+src[j+jj][i+ii]);
            }
        }
        
        return dst;
    }
    
    public double[][][] interp( int varId ) throws IOException, InvalidRangeException, NCRegridderException {
        
        double[][][] dataOut = null;
        double[] dataIn1D = new double[bottomTopDim], zData1D= new double[bottomTopDim], dataOut1D=null;
        int i,j,k,kk;
        double pTarget, dpMin, dp, pBot, zBot;
        double tBotExtrap, tvBotExtrap, expon, exponi;
        double zLev, pLev, tLev, gamma;
        int kUpper, kIn;
        
        double[][][] dataIn=null;
        
        
        
        
        double[][][] zData = vertArray;
        
        expon=287.04f*.0065f/9.81f;
        exponi=1/expon;
        
        switch (varId) {
            case VARIABLE_P:
                load(VARIABLE_P);
                dataIn = P;
            break;
            /*    
            case VARIABLE_U:
                load(VARIABLE_U);
                nx = westEastStagDim;
                ny = southNorthDim;
                nz = bottomTopDim;
                dataIn = destag(U);
            break;
            */    
            case VARIABLE_SLP:
                load(VARIABLE_SLP);
                dataIn= new double[1][1][1];
                dataIn[0] = SLP;
                //dataIn= double2DtoDoubled3D(SLP);
            break;
        }
        
        int nzout = dataIn.length;
        int nyout = dataIn[0].length;
        int nxout = dataIn[0][0].length;
        
        
        
        
        
        
        if ((iProgram > 6 ) && (nzout == bottomTopDim) && ((verticalType==VERTICALTYPE_P) || (verticalType==VERTICALTYPE_Z))) {
            dataOut = new double[nzout][nyout][nxout];
            for (i=0;i<southNorthDim;i++) {
                for (j=0;j<westEastDim;j++) {
                    for (k=0;k<bottomTopDim;k++) {
                        dataIn1D[k] = dataIn[k][i][j];
                        zData1D[k] = zData[k][i][j];
                    }
                    
                    dataOut1D=interp1D(dataIn1D, zData1D, bottomTopDim);
                   
                    for (k=0;k<numberOfZLevs;k++) {
                        dataOut[k][i][j] = dataOut1D[k];
                    }
                }
                
            }
            nzout = numberOfZLevs;
        } else {
            dataOut = new double [nzout][nyout][nxout];
            for (k=0;k<nzout;k++) {
                for (i=0;i<nyout;i++) {
                    for (j=0;j<nxout;j++) {
                        dataOut[k][i][j] = dataIn[k][i][j];
                    }
                }
            }
        }
        
        if (extrapolate==false || iProgram < 8 || nzout < bottomTopDim ) {
            return dataOut;
        }
        
        // First find where about 400hPA/7Km is located
        kk=0;
        for (k=0;k<nzout;k++) {
            kk=k;
            if (verticalType==VERTICALTYPE_P && interpLevels[k]<=400) { break; }
            if (verticalType==VERTICALTYPE_Z && interpLevels[k]<=7) { break; }
        }
        
        if (verticalType==VERTICALTYPE_P) {
            // Need to do something of special with height and temperature
            if (( varId==VARIABLE_HEIGHT || varId==VARIABLE_GEOPT) ||
                varId==VARIABLE_TC || varId==VARIABLE_TK || 
                varId==VARIABLE_THETA) {
                for (k=0;k<kk;k++) {
                    for (i=0;i<southNorthDim;i++) {
                        for (j=0;j<westEastDim;j++) {
                            if (dataOut[k][i][j]==MISSING_VALUE && 100*interpLevels[k]<PSFC[i][j]) {
                                // We are below the first model level, but above the ground
                                // We need meter for the calculations so, GEOPT/G
                                
                                zLev=(((100*interpLevels[k] - PRES[0][i][j]*HGT[i][j] +
                                    (PSFC[i][j]-100*interpLevels[k])*GEOPT[1][i][j]/9.81) /
                                    (PSFC[i][j]-PRES[1][i][j])));
                                
                                if (varId==VARIABLE_HEIGHT) dataOut[k][i][j] = zLev / 1000;
                                if (varId==VARIABLE_GEOPT) dataOut[k][i][j] = zLev / 9.81;
                                if (varId==VARIABLE_TK || varId==VARIABLE_TC || varId==VARIABLE_THETA) {
                                    tLev = TK[0][i][j] + (GEOPT[0][i][j]/9.81-zLev)*0.0065;
                                    if (varId==VARIABLE_TK) dataOut[k][i][j] = tLev;
                                    if (varId==VARIABLE_TC) dataOut[k][i][j] = tLev-273.15;
                                    if (varId==VARIABLE_THETA) {
                                        gamma = (287.04/1004)*(1+(0.608-0.887)*QV[k][i][j]);
                                        dataOut[k][i][j] = tLev * Math.pow((1000 / interpLevels[k]),gamma);
                                    }
                                    
                                }
                            } else if (dataOut[k][i][j]==MISSING_VALUE) {
                            /*
!             We are below both the ground and the lowest data level.
!             First, find the model level that is closest to a "target" pressure
!             level, where the "target" pressure is delta-p less that the local
!             value of a horizontally smoothed surface pressure field.  We use
!             delta-p = 150 hPa here. A standard lapse rate temperature profile
!             passing through the temperature at this model level will be used
!             to define the temperature profile below ground.  This is similar
!             to the Benjamin and Miller (1990) method, except that for
!             simplicity, they used 700 hPa everywhere for the "target" pressure.
!             Code similar to what is implemented in RIP4

                             */
                                pTarget = (PSFC[i][j]*0.01)-150;
                                dpMin=1.e4;
                                kUpper=0;
                                for (kIn=nzout-1;kIn>=0;kIn--) {
                                    kUpper = kIn;
                                    dp = Math.abs(PRES[kIn][i][j]*0.01-pTarget);
                                    if (dp > dpMin) {
                                        break;
                                    }
                                    dpMin = Math.min(dpMin,dp);
                                }
                                
                                pBot = Math.max(PRES[0][i][j],PSFC[i][j]);
                                zBot = Math.min(GEOPT[0][i][j]/9.81, HGT[i][j]); // Need height in meters
                                
                                tBotExtrap = TK[kUpper][i][j] * Math.pow((pBot/PRES[kUpper][i][j]),expon);
                                tvBotExtrap = virtual(tBotExtrap,QV[0][i][j]);
                                
                                // Calculation uses heigth in meter, but we want the output in km
                                zLev = (zBot + 
                                        tvBotExtrap/.0065*
                                        (1-Math.pow(100*interpLevels[k]/pBot,expon)));
                                
                                if (varId==VARIABLE_HEIGHT) dataOut[k][i][j] = zLev / 1000;
                                if (varId==VARIABLE_GEOPT) dataOut[k][i][j] = zLev / 9.81;
                                if (varId==VARIABLE_TK || varId==VARIABLE_TC || varId==VARIABLE_THETA) {
                                    tLev = TK[0][i][j] + (GEOPT[0][i][j]/9.81-zLev)*0.0065;
                                    if (varId==VARIABLE_TK) dataOut[k][i][j] = tLev;
                                    if (varId==VARIABLE_TC) dataOut[k][i][j] = tLev-273.15;
                                    if (varId==VARIABLE_THETA) {
                                        gamma = (287.04/1004)*(1+(0.608-0.887)*QV[k][i][j]);
                                        dataOut[k][i][j] = tLev * Math.pow((1000 / interpLevels[k]),gamma);
                                    }
                                    
                                }
                                
                            }
                        }
                    }
                }
            }
        }
        if (verticalType==VERTICALTYPE_Z) {
            // Need to do something of special with height and temperature
            
            if (varId==VARIABLE_PRESSURE || varId==VARIABLE_TK || varId==VARIABLE_TC || varId==VARIABLE_THETA) {
                for (k=0;k<kk;k++) {
                    for (i=0;i<southNorthDim;i++) {
                        for (j=0;j<westEastDim;j++) {
                            if (dataOut[k][i][j]==MISSING_VALUE && 1000*interpLevels[k]>HGT[i][j]) {
                                // We are below the first model level, but above the ground
                                // We need meter for the calculations so, GEOPT/G
                                pLev=(((1000*interpLevels[k] - GEOPT[0][i][j]/9.81)*PSFC[i][j] +
                                    (HGT[i][j]-1000*interpLevels[k])*PRES[1][i][j]) /
                                    (HGT[i][j]-GEOPT[1][i][j]/9.81));
                                
                                if (varId==VARIABLE_PRESSURE) dataOut[k][i][j] = pLev * .01;
                                if (varId==VARIABLE_TK || varId==VARIABLE_TC || varId==VARIABLE_THETA) {
                                    tLev = TK[0][i][j] + (GEOPT[0][i][j]/9.81-1000*interpLevels[k])*0.0065;
                                    if (varId==VARIABLE_TK) dataOut[k][i][j] = tLev;
                                    if (varId==VARIABLE_TC) dataOut[k][i][j] = tLev-273.15;
                                    if (varId==VARIABLE_THETA) {
                                        gamma = (287.04/1004)*(1+(0.608-0.887)*QV[k][i][j]);
                                        dataOut[k][i][j] = tLev * Math.pow((1000 / pLev),gamma);
                                    }
                                    
                                }
                                else if (dataOut[k][i][j]==MISSING_VALUE) {
                            /*
!             We are below both the ground and the lowest data level.
!             First, find the model level that is closest to a "target" pressure
!             level, where the "target" pressure is delta-p less that the local
!             value of a horizontally smoothed surface pressure field.  We use
!             delta-p = 150 hPa here. A standard lapse rate temperature profile
!             passing through the temperature at this model level will be used
!             to define the temperature profile below ground.  This is similar
!             to the Benjamin and Miller (1990) method, except that for
!             simplicity, they used 700 hPa everywhere for the "target" pressure.
!             Code similar to what is implemented in RIP4
                            
                             */
                                    pTarget = (PSFC[i][j]*0.01)-150;
                                    dpMin=1.e4;
                                    kUpper=0;
                                    for (kIn=nzout-1;kIn>=0;kIn--) {
                                        kUpper = kIn;
                                        dp = Math.abs(PRES[kIn][i][j]*0.01-pTarget);
                                        if (dp > dpMin) {
                                            break;
                                        }
                                        dpMin = Math.min(dpMin,dp);
                                    }

                                    pBot = Math.max(PRES[0][i][j],PSFC[i][j]);
                                    zBot = Math.min(GEOPT[0][i][j]/9.81, HGT[i][j]); // Need height in meters

                                    tBotExtrap = TK[kUpper][i][j] * Math.pow((pBot/PRES[kUpper][i][j]),expon);
                                    tvBotExtrap = virtual(tBotExtrap,QV[0][i][j]);

                                    // Calculation uses heigth in meter, but we want the output in km
                                    pLev = pBot * Math.pow(1+0.0065/tvBotExtrap*(zBot-1000*interpLevels[k]),exponi);

                                    if (varId==VARIABLE_PRESSURE) dataOut[k][i][j] = pLev * 0.01;

                                    if (varId==VARIABLE_TK || varId==VARIABLE_TC || varId==VARIABLE_THETA) {
                                        tLev = TK[0][i][j] + (GEOPT[0][i][j]/9.81-1000*interpLevels[k])*0.0065;
                                        if (varId==VARIABLE_TK) dataOut[k][i][j] = tLev;
                                        if (varId==VARIABLE_TC) dataOut[k][i][j] = tLev-273.15;
                                        if (varId==VARIABLE_THETA) {
                                            gamma = (287.04/1004)*(1+(0.608-0.887)*QV[k][i][j]);
                                            dataOut[k][i][j] = tLev * Math.pow((1000 / pLev),gamma);
                                        }

                                    }
                                
                                }
                            }
                        }
                    }
                }                
            }
        }
            
        // All fields and geopt at higher levels come here
        for (i=0;i<nxout;i++) {
            for (j=0;j<nyout;j++) {
                for (k=0;k<kk;k++) {
                    if (dataOut[k][i][j]==MISSING_VALUE) {
                        dataOut[k][i][j]=dataIn[0][i][j];
                    }
                }
                for (k=kk+1;k<nzout;k++) {
                    if (dataOut[k][i][j]==MISSING_VALUE) {
                        dataOut[k][i][j]=dataIn[nzout-1][i][j];
                    }
                }
            }

        }
        
        return dataOut;
    }
    
    public double virtual(double tmp, double rmix) {
        return tmp*(0.622+rmix)/(0.622*(1.+rmix));
    }
    
    public double[] interp1D(double[] a, double[] xa, int na) {
        double[] b = new double[numberOfZLevs];
        int nIn;
        int nOut;
        double w1, w2;
        boolean interp;
        
        if (verticalType==VERTICALTYPE_P) {
            for (nOut=0;nOut<numberOfZLevs;nOut++) {
                b[nOut] = MISSING_VALUE;
                interp=false;
                nIn = 0;
                
                while (!interp && nIn<na-1 ) {
                    if (xa[nIn] >= interpLevels[nOut] && xa[nIn+1]<=interpLevels[nOut]) {
                        interp=true;
                        w1 = (xa[nIn+1]-interpLevels[nOut])/(xa[nIn+1]-xa[nIn]);
                        w2 = 1-w1;
                        b[nOut] = w1*a[nIn]+w2*a[nIn+1];
                    }
                    nIn++;
                } 
            }
        } else {
            for (nOut=0;nOut<numberOfZLevs;nOut++) {
                b[nOut] = MISSING_VALUE;
                interp=false;
                nIn = 0;
                
                while (!interp && nIn<na-1 ) {
                    if (xa[nIn] <= interpLevels[nOut] && xa[nIn+1]>=interpLevels[nOut]) {
                        interp=true;
                        w1 = (xa[nIn+1]-interpLevels[nOut])/(xa[nIn+1]-xa[nIn]);
                        w2 = 1-w1;
                        b[nOut] = w1*a[nIn]+w2*a[nIn+1];
                    }
                    nIn++;
                } 
            }
        }
        
        return b;
    }
    
    public void getInterpInfo() throws IOException, InvalidRangeException {
        int[] locOfMinZ;
        int found=0;
        ArrayFloat.D3 aTmp3D = null;
        double[][][] PHtmp = null;
        double[][][] PHBtmp = null;
        
        logger.log(Level.INFO, "getInterpInfo...");
        
        
        if (verticalType==VERTICALTYPE_P) {
            logger.log(Level.INFO, "getInterpInfo: verticalTyep=p");
            
            if (ncDataset.findVariable("P")==null) found++;
            if (ncDataset.findVariable("PB")==null) found++;
            
            if (found!=0 && iProgram==6) {
                // Probably wrfinput prior than v3.1 so it may not have P and PB - lets try something else
                found = 0;
                logger.log(Level.INFO, "INFO: probably old wrfinput data - Try getting MU and MUB");
                
                if (ncDataset.findVariable("QVAPOR")==null) found++;
                if (ncDataset.findVariable("MU")==null) found++;
                if (ncDataset.findVariable("MUB")==null) found++;
                if (ncDataset.findVariable("P_TOP")==null) found++;
                if (ncDataset.findVariable("ZNU")==null) found++;
                if (ncDataset.findVariable("ZNW")==null) found++;
            }
            
            if (found==0) {
                logger.log(Level.INFO, "Interpolating to PRESSURE levels");
            } else {
                logger.log(Level.INFO, "WARNING: Asked to interpolate to PRESSURE, but we don't have enough information. Will output data on MODEL LEVELS");
                verticalType=VERTICALTYPE_NONE;
                extrapolate=false;
            }
        }
        if (verticalType==VERTICALTYPE_Z && interpMethod==1) {
            logger.log(Level.INFO, "getInterpInfo: verticalTyep=p and interpMethod==1");
            
            if (ncDataset.findVariable("PH")==null) found++;
            if (ncDataset.findVariable("PHB")==null) found++;
            
            if (found==0) {
                logger.log(Level.INFO, "Interpolating to USER SPECIFIED HEIGHT levels");
            } else {
                logger.log(Level.INFO, "WARNING: Asked to interpolate to USER SPECIFIED HEIGHT, but we don't have enough information. Will output data on MODEL LEVELS");
                verticalType=VERTICALTYPE_NONE;
                extrapolate=false;
            }
        }
        if (verticalType==VERTICALTYPE_Z && interpMethod==-1) {
            logger.log(Level.INFO, "getInterpInfo: verticalTyep=z and interpMethod=-1");
            
            found = 0;
            
            Variable varPH = ncDataset.findVariable("PH");
            if (varPH!=null) {
                
                PHtmp = new double[varPH.getShape(0)][varPH.getShape(1)][varPH.getShape(2)];
                aTmp3D = (ArrayFloat.D3)varPH.read(new int[] {0,0,0}, new int[] {varPH.getShape(0),varPH.getShape(1),varPH.getShape(2)});
                for (int k=1;k<varPH.getShape(0);k++) {
                    for (int i=0;i<varPH.getShape(1);i++) {
                        for (int j=0;j<varPH.getShape(2);j++) {
                    
                            PHtmp[k][i][j]=0.5*(aTmp3D.get(k-1, i, j)-1)+aTmp3D.get(k,i,j);                           
                        }
                    }
                }
            } else found++;
            Variable varPHB = ncDataset.findVariable("PHB");
            if (varPHB!=null) {
                
                PHBtmp = new double[varPHB.getShape(0)][varPHB.getShape(1)][varPHB.getShape(2)];
                aTmp3D = (ArrayFloat.D3)varPHB.read(new int[] {0,0,0}, new int[] {varPHB.getShape(0),varPHB.getShape(1),varPHB.getShape(2)});
                for (int k=1;k<varPHB.getShape(0);k++) {
                    for (int i=0;i<varPHB.getShape(1);i++) {
                        for (int j=0;j<varPHB.getShape(2);j++) {
                    
                            PHBtmp[k][i][j]=0.5*(aTmp3D.get(k-1, i, j)-1)+aTmp3D.get(k,i,j);                           
                        }
                    }
                }
            } else found++;
            if (found==0) {
                for (int k=0;k<varPH.getShape(0);k++) {
                    for (int i=0;i<varPH.getShape(1);i++) {
                        for (int j=0;j<varPH.getShape(2);j++) {
                            PHtmp[k][i][j]=(PHtmp[k][i][j]+PHBtmp[k][i][j])/9.81/1000;
                        }
                    }
                }
                numberOfZLevs = bottomTopDim;
                locOfMinZ = minloc(varPH.getShape(1),varPH.getShape(2),PHtmp[0]);
                for (int k=0;k<numberOfZLevs;k++) {
                    interpLevels[k]=PHtmp[k][locOfMinZ[0]][locOfMinZ[1]];
                }
                interpLevels[0]=interpLevels[0]+0.002;
                interpLevels[0]=Math.max(interpLevels[0],interpLevels[1]/2); // No neg value
                interpLevels[numberOfZLevs-1]=interpLevels[numberOfZLevs-1]-0.002;
                System.out.println("Interpolating to GENERATED HEIGHT LEVELS.");
            }
            else {
                System.out.println("WARNING: Asked to interpolate to generated height, but we do not have information.\nWill output data on MODEL LEVELS");
                verticalType=VERTICALTYPE_NONE;
                extrapolate=false;
            }
        }
        
        logger.log(Level.INFO, "getInterpInfo: verticalTyep="+verticalType+" extrapolate="+extrapolate);
        
    }
    
    
    
    private void getFields(int localTime) throws IOException, InvalidRangeException, NCRegridderException {
        setTime(localTime);
        
        if (verticalType!=VERTICALTYPE_NONE) {
            getInterpArray(localTime);
        }
        
        load(VARIABLE_PRES);
        load(VARIABLE_TK);
        load(VARIABLE_GEOPT);
        load(VARIABLE_QV);
        load(VARIABLE_PSFC);
        load(VARIABLE_HGT);
        load(VARIABLE_XLAT);
        load(VARIABLE_XLONG);
        load(VARIABLE_PTOP);
        
    }
    
    private void getInterpArray(int localTime) throws IOException, InvalidRangeException, NCRegridderException {
        int found=0;
        
        setTime(localTime);
        
        
        
        vertArray=null;
        if (verticalType==VERTICALTYPE_P) {
            // First find out wich input variables are available - depend on input file
            
            load(WRFData.VARIABLE_PRES);
            
            vertArray = new double[bottomTopDim][southNorthDim][westEastDim];
            for (int k=0;k<bottomTopDim;k++) {
                for (int j=0;j<southNorthDim; j++) {
                    for (int i=0;i<westEastDim; i++) {
                        vertArray[k][j][i] = PRES[k][j][i]*0.01; // Pressure array in hPa
                    }
                }
            }
            
        }
        
        if (verticalType==VERTICALTYPE_Z) {
            found=0;
            
            load(WRFData.VARIABLE_PH);
            load(WRFData.VARIABLE_PHB);
            
            vertArray = new double[bottomTopDim-1][southNorthDim][westEastDim];
            for (int k=0;k<bottomTopDim-1;k++) {
                for (int j=0;j<southNorthDim; j++) {
                    for (int i=0;i<westEastDim; i++) {
                        //System.out.println("k:"+k+" j:"+j+" i:"+i);
                        vertArray[k][j][i] = (0.5 * (PH[k][j][i]+PH[k+1][j][i]) + 0.5 * (PHB[k][j][i]+PHB[k+1][j][i])/G/1000); // height in Km


                    }
                }                
            }
        }        
    }
    
    private void pressure() throws IOException, InvalidRangeException, NCRegridderException {
        double[] rdnw = new double[bottomTopStagDim];
        double[] rdn = new double[bottomTopDim];
        double dnw,dn;
        double qvf1,qvf2;
        double pBase;
        
        load(WRFData.VARIABLE_ZNW);
        load(WRFData.VARIABLE_ZNU);
        load(WRFData.VARIABLE_QV);
        load(WRFData.VARIABLE_MU);
        load(WRFData.VARIABLE_MUB);
        load(WRFData.VARIABLE_PTOP);
        
        PRES=new double[bottomTopDim][southNorthDim][westEastDim];
        
        for (int k=0;k<bottomTopDim;k++) {
            dnw = ZNW[k+1]-ZNW[k];
            rdnw[k]=1/dnw;
        }
        for (int k=0;k<bottomTopDim;k++) {
            dn = .5*(1/rdnw[k+1])+1/rdnw[k];
            rdn[k]=1/dn;
        }
        
        for (int j=0;j<southNorthDim;j++) {
            for (int i=0;i<westEastDim;i++) {
                // Get pressure perturbation at model top
                int k = bottomTopDim;
                qvf1 = QV[k][j][i] * 0.001;
                qvf2 = 1/(1+qvf1);
                qvf1 = qvf1 * qvf2;
                PRES[k][j][i] = -0.5 * (MU[j][i]+qvf1*MUB[j][i])/rdnw[k]/qvf2;
                
                // Now get pressure perturbation at level below
                for (k=0;k<bottomTopDim-1;k++) {
                    qvf1=0.5*(QV[k][j][i]+QV[k+1][j][i])*0.001;
                    qvf2=1/(1+qvf1);
                    qvf1=qvf1*qvf2;
                    PRES[k][j][i] = PRES[k+1][j][i] - (MU[j][i]+qvf1*MUB[j][i])/qvf2/rdn[k];
                }
                
                // Finally compute base state pressure and add to pressure perturbation to get total pressure
                for (k=0;k<bottomTopDim;k++) {
                    pBase=ZNU[k]*MUB[j][i]+PTOP[0];
                    PRES[k][j][i] = PRES[k][j][i] -pBase; // Pa
                }
            }
        }
        
  
    }
    
    public int[] minloc(int m, int n, double[][] a) {
        int[] result = new int[2];
         
        double value=a[0][0];
        for (int i=0;i<m;i++) {
            for (int j=0;j<n;j++) {
                if (a[i][j]<value) {
                    value=a[i][j];
                    result[0]=i;
                    result[1]=j;
                }
            }
        }
        return result;
    }
    
    
    
    public void saveAsGRD(String fileName,int varId, double cX,double cY, double dxy, int cols, int rows) throws IOException, InvalidRangeException, Exception {
        double llX = cX-(cols/2)*dxy;
        double urX = cX+(cols/2)*dxy;
        double llY = cY-(cols/2)*dxy;
        double urY = cY+(cols/2)*dxy;
        
        saveAsGRD(fileName, varId, cols, rows , llX,llY,urX, urY);
    }
    public void saveAsGRD(String fileName,int varId,int cols, int rows, double llX,double llY, double urX, double urY) throws IOException, InvalidRangeException, Exception {
        
        double dX = (urX-llX)/cols;
        double dY = (urY-llY)/rows;
        double min = 0;
        double max = 1;
        
        double[][] dstLat = new double[rows][cols];
        double[][] dstLong = new double[rows][cols];
        for (int j=0;j<rows; j++) {
            for (int i=0;i<cols; i++) {
                dstLat[j][i]=llY+j*dY;
                dstLong[j][i]=llX+i*dX;
            }
        }
        
        if (interpolator==null) {
            load(VARIABLE_XLAT);
            load(VARIABLE_XLONG);
            InterpolatorParams params = new InterpolatorParams();
            params.put("subset",1);
            interpolator = new KInterpolator(XLAT, XLONG, dstLat, dstLong,null,null,params);
            
        }
        
        double[][] dVar=CUSTOM2D;
        if (varId==VARIABLE_SLP) {
            dVar=SLP;
        } 
        
        double[][] dst = interpolator.interp(dVar,1e37,1e37,null);
        
        FileWriter fw = new FileWriter(fileName+".grd");
        PrintWriter pw = new PrintWriter(fw);

        
        pw.println("DSAA");
        pw.println(cols + " " +rows);
        pw.println(llX + " " + (llX+cols*dX));
        pw.println(llY + " " + (llY+rows*dY));
        pw.println(min+" "+max);
        for (int j=0;j<rows; j++) {
            String strOut="";
            for (int i=0;i<cols; i++) {
                
                //System.out.println("j:"+j+" i:"+i+" slp="+dst[j][i]);
                
                strOut+=String.format("%6.2f ", dst[j][i] );

            }
            strOut.trim();
            pw.println(strOut);
        }

        pw.close();
        fw.close();
        
    }
    
    
     public static double dist(double minLon, double minLat, double maxLon, double maxLat) {
        double dLon=maxLon-minLon;
        double dLat=maxLat-minLat;


        double R = 6371;
        double dLatR = Math.toRadians(dLat);
        double dLonR = Math.toRadians(dLon);
        double dMinLatR = Math.toRadians(minLat);
        double dMaxLatR = Math.toRadians(maxLat);

        double a = Math.sin( dLatR / 2 ) * Math.sin( dLatR / 2 ) +
                Math.cos( dMinLatR) * Math.cos( dMaxLatR) *
                Math.sin( dLonR / 2 ) * Math.sin( dLonR / 2 );
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double d = R * c;

        return d;
    }

    
}


