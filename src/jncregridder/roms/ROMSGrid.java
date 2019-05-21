/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jncregridder.roms;

import it.uniparthenope.meteo.Haversine;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import jncregridder.util.IDWInterpolator;
import jncregridder.util.IdDoubleVectData;
import jncregridder.util.InterpolatorException;
import jncregridder.util.KInterpolator;
import jncregridder.util.Kriging;
import jncregridder.util.KrigingException;
import jncregridder.util.NCRegridderException;
import jncregridder.util.Station;
import jncregridder.util.Stations;
import ucar.ma2.ArrayChar;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.ma2.ArrayDouble;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.nc2.NetcdfFileWriteable;

/**
 *
 * @author raffaelemontella
 */
public class ROMSGrid {

    private String url;
    public String getUrl() { return url; }
    
    private NetcdfDataset ncDataset;
    
    private NetcdfFileWriteable ncfWritable;
    
    private double noData = 1e37;
    

    public Dimension dimEtaRho;
    public Dimension dimXiRho;
    public Dimension dimEtaPsi;
    public Dimension dimXiPsi;
    public Dimension dimEtaU;
    public Dimension dimXiU;
    public Dimension dimEtaV;
    public Dimension dimXiV;
    //public Dimension dimSRho;
    public Dimension dimOne, dimTwo, dimFour, dimBath;

    public double[][] LATRHO = null;
    public double[][] LONRHO = null;
    public double[][] LATU = null;
    public double[][] LATV = null;
    public double[][] LONU = null;
    public double[][] LONV = null;
    public double[][] LONPSI = null;
    public double[][] LATPSI = null;
    public double[][] ANGLE = null;
    public double[][] MASKRHO = null;
    public double[][] MASKU = null;
    public double[][] MASKV = null;
    public double[][] H = null;
    public double[][][] Z = null;

    public double[][] ALPHA=null;
    public double[][] DMDE=null;
    public double[][] DNDX=null;
    public double[][] F=null;
    public double[][] HRAW=null;
    public double[][] MASKPSI=null;
    public double[][] PM=null;
    public double[][] PN=null;
    public double[][] XRHO=null;
    public double[][] YRHO=null;
    public double[][] XPSI=null;
    public double[][] YPSI=null;
    public double[][] XU=null;
    public double[][] YU=null;
    public double[][] XV=null;
    public double[][] YV=null;
    
    
    int etaRho = -1;
    int xiRho = -1;
    int etaU = -1;
    int xiU = -1;
    int etaV = -1;
    int xiV = -1;
    int etaPsi = -1;
    int xiPsi = -1;
    
    int one=1;
    int two=2;
    int bath=1;

    

    public double[][] getLATU() throws NCRegridderException { load(VARIABLE_LATU); return LATU; }
    public double[][] getLONU() throws NCRegridderException { load(VARIABLE_LONU); return LONU; }
    public double[][] getLATV() throws NCRegridderException { load(VARIABLE_LATV); return LATV; }
    public double[][] getLONV() throws NCRegridderException { load(VARIABLE_LONV); return LONV; }
    public double[][] getLATRHO() throws NCRegridderException { load(VARIABLE_LATRHO); return LATRHO; }
    public double[][] getLONRHO() throws NCRegridderException { load(VARIABLE_LONRHO); return LONRHO; }
    public double[][] getANGLE() throws NCRegridderException { return load(VARIABLE_ANGLE)[0]; }
    public double[][] getH() throws NCRegridderException { return load(VARIABLE_H)[0]; }
    public double[][][] getZ() throws NCRegridderException { return load(VARIABLE_Z); }
    public double[][] getMASKRHO() throws NCRegridderException { return load(VARIABLE_MASKRHO)[0]; }
    public double[][] getMASKU() throws NCRegridderException { return load(VARIABLE_MASKU)[0]; }
    public double[][] getMASKV() throws NCRegridderException { return load(VARIABLE_MASKV)[0]; }
    
    public int[][] getMASKRHOasInt() throws NCRegridderException {
        double[][] mask=getMASKRHO();
        int[][] result=new int[etaRho][xiRho];
        for (int j=0;j<etaRho;j++) {
            for (int i=0;i<xiRho;i++) {
                result[j][i]=(int)mask[j][i];
            }
        }
        return result;
    }
    
    public int[][] getMASKUasInt() throws NCRegridderException {
        double[][] mask=getMASKU();
        int[][] result=new int[etaU][xiU];
        for (int j=0;j<etaU;j++) {
            for (int i=0;i<xiU;i++) {
                result[j][i]=(int)mask[j][i];
            }
        }
        return result;
    }
    
     public int[][] getMASKVasInt() throws NCRegridderException {
        double[][] mask=getMASKV();
        int[][] result=new int[etaV][xiV];
        for (int j=0;j<etaV;j++) {
            for (int i=0;i<xiV;i++) {
                result[j][i]=(int)mask[j][i];
            }
        }
        return result;
    }
    
    
    private double theta_s=3;
    private double theta_b=0;
    private int N = 30;
    private double hc_threshold = 5.0D;
    private double hc=Double.NaN;
    private double zeta=0;
    
    public double getThetaS() { return theta_s; }
    public double getThetaB() { return theta_b; }
    public double getHC() { return hc; }
    
    private double[] s_rho = null;
    private double[] s_w = null;
    private double[] cs_r = null;
    private double[] cs_w = null;
    
    public double[] getSW() { return s_w; }
    public double[] getSRho() { return s_rho; }
    public double[] getCSR() { return cs_r; }
    public double[] getCSW() { return cs_w; }
    
    private double x1=0;
    private double e1=0;
    private double depthMin=10;
    private double depthMax=10000;
    private char spherical='T';
    
    public ROMSGrid(int cols, int rows,String url, double minLonRho, double minLatRho, double lonStep,double latStep) throws IOException {
        double maxLonRho=minLonRho + (cols*lonStep);
        double maxLatRho=minLatRho + (rows*latStep);
        
        create(url,minLonRho,minLatRho,maxLonRho,maxLatRho,lonStep,latStep);
    }
    
    public void create(String url, double minLonRho, double minLatRho, double maxLonRho, double maxLatRho, double lonStep, double latStep) throws IOException {
        double minLonV = minLonRho;
        double minLatV = minLatRho-latStep/2;
        double minLonU = minLonRho-lonStep/2;
        double minLatU = minLatRho;
        double minLatPsi = minLatRho-latStep/2;
        double minLonPsi = minLonRho-lonStep/2;
        
        etaRho = (int)(Math.round((maxLatRho-minLatRho)/latStep));
        xiRho = (int)(Math.round((maxLonRho-minLonRho)/lonStep));
        etaU = etaRho;
        xiU = xiRho-1;
        etaV = etaRho-1;
        xiV = xiRho;
        etaPsi = etaRho-1;
        xiPsi = xiRho-1;
        
        
        ANGLE=new double[etaRho][xiRho];
        ALPHA=new double[etaRho][xiRho];
        DMDE=new double[etaRho][xiRho];
        DNDX=new double[etaRho][xiRho];
        H=new double[etaRho][xiRho];
        F=new double[etaRho][xiRho];
        HRAW=new double[etaRho][xiRho];
        MASKRHO=new double[etaRho][xiRho];
        MASKU=new double[etaU][xiU];
        MASKV=new double[etaV][xiV];
        MASKPSI=new double[etaPsi][xiPsi];
        PM=new double[etaRho][xiRho];
        PN=new double[etaRho][xiRho];
        XRHO=new double[etaRho][xiRho];
        YRHO=new double[etaRho][xiRho];
        XPSI=new double[etaPsi][xiPsi];
        YPSI=new double[etaPsi][xiPsi];
        XU=new double[etaU][xiU];
        YU=new double[etaU][xiU];
        XV=new double[etaV][xiV];
        YV=new double[etaV][xiV];
        
        
        LATRHO=new double[etaRho][xiRho];
        LONRHO=new double[etaRho][xiRho];
        LATPSI=new double[etaPsi][xiPsi];
        LONPSI=new double[etaPsi][xiPsi];
        LATU=new double[etaU][xiU];
        LONU=new double[etaU][xiU];
        LATV=new double[etaV][xiV];
        LONV=new double[etaV][xiV];
        
        for (int j=0;j<etaRho;j++) {
            for (int i=0;i<xiRho;i++) {
                LATRHO[j][i]=minLatRho+j*latStep;
                LONRHO[j][i]=minLonRho+i*lonStep;
                ANGLE[j][i]=0;
            }
        }
        
        for (int j=0;j<etaPsi;j++) {
            for (int i=0;i<xiPsi;i++) {
                LATPSI[j][i]=minLatPsi+j*latStep;
                LONPSI[j][i]=minLonPsi+i*lonStep;
            }
        }
        
        for (int j=0;j<etaU;j++) {
            for (int i=0;i<xiU;i++) {
                LATU[j][i]=minLatU+j*latStep;
                LONU[j][i]=minLonU+i*lonStep;
            }
        }
        
        for (int j=0;j<etaV;j++) {
            for (int i=0;i<xiV;i++) {
                LATV[j][i]=minLatV+j*latStep;
                LONV[j][i]=minLonV+i*lonStep;
            }
        }
        
        
        
        
        
        dimEtaRho = new Dimension("eta_rho",etaRho);
        dimXiRho = new Dimension("xi_rho",xiRho);;
        dimEtaU = new Dimension("eta_u",etaU);;
        dimXiU = new Dimension("xi_u",xiU);;
        dimEtaV = new Dimension("eta_v",etaV);;
        dimXiV = new Dimension("xi_v",xiV);;
        dimEtaPsi = new Dimension("eta_psi",etaPsi);;
        dimXiPsi = new Dimension("xi_psi",xiPsi);;
        
        dimOne = new Dimension("one", 1);
        dimTwo = new Dimension("two", 2);
        dimFour = new Dimension("four", 4);
        dimBath = new Dimension("bath", 1);
        
        
        ncfWritable = NetcdfFileWriteable.createNew(url);
        
        ncfWritable.addGlobalAttribute("type", "Grid file");
        ncfWritable.addGlobalAttribute("title", "");
        ncfWritable.addGlobalAttribute("source", "Grid2ROMS http://meteo.uniparthenope.it");
        
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        ncfWritable.addGlobalAttribute("date", sdf.format(cal.getTime()));
        
        ncfWritable.addDimension(dimEtaRho.getName(),dimEtaRho.getLength());
        ncfWritable.addDimension(dimXiRho.getName(),dimXiRho.getLength());
        ncfWritable.addDimension(dimEtaU.getName(),dimEtaU.getLength());
        ncfWritable.addDimension(dimXiU.getName(),dimXiU.getLength());
        ncfWritable.addDimension(dimEtaV.getName(),dimEtaV.getLength());
        ncfWritable.addDimension(dimXiV.getName(),dimXiV.getLength());
        ncfWritable.addDimension(dimEtaPsi.getName(),dimEtaPsi.getLength());
        ncfWritable.addDimension(dimXiPsi.getName(),dimXiPsi.getLength());
        
        ncfWritable.addDimension(dimOne.getName(), dimOne.getLength());
        ncfWritable.addDimension(dimTwo.getName(), dimTwo.getLength());
        ncfWritable.addDimension(dimBath.getName(), dimBath.getLength());
        
        ncfWritable.addVariable("h", DataType.DOUBLE, "eta_rho xi_rho");
        ncfWritable.addVariableAttribute("h", "long_name","Final bathymetry at RHO-points");
        ncfWritable.addVariableAttribute("h", "units","meters");
        ncfWritable.addVariableAttribute("h", "field","bath, scalar");
        
        ncfWritable.addVariable("lat_rho", DataType.DOUBLE, "eta_rho xi_rho");
        ncfWritable.addVariableAttribute("lat_rho", "long_name","latitude of RHO-points");
        ncfWritable.addVariableAttribute("lat_rho", "units","degree_north");
        ncfWritable.addVariableAttribute("lat_rho", "field","lat_rho, scalar");
        ncfWritable.addVariableAttribute("lat_rho", "standard_name","latitude");
        ncfWritable.addVariableAttribute("lat_rho", "_CoordinateAxisType", "Lat");
        
        ncfWritable.addVariable("lon_rho", DataType.DOUBLE, "eta_rho xi_rho");
        ncfWritable.addVariableAttribute("lon_rho", "long_name","longitude of RHO-points");
        ncfWritable.addVariableAttribute("lon_rho", "units","degree_east");
        ncfWritable.addVariableAttribute("lon_rho", "field","lon_rho, scalar");
        ncfWritable.addVariableAttribute("lon_rho", "standard_name","longitude");
        ncfWritable.addVariableAttribute("lon_rho", "_CoordinateAxisType", "Lon");
        
        ncfWritable.addVariable("lat_u", DataType.DOUBLE, "eta_u xi_u");
        ncfWritable.addVariableAttribute("lat_u", "long_name","latitude of U-points");
        ncfWritable.addVariableAttribute("lat_u", "units","degree_north");
        ncfWritable.addVariableAttribute("lat_u", "field","lat_u, scalar");
        ncfWritable.addVariableAttribute("lat_u", "standard_name","latitude");
        ncfWritable.addVariableAttribute("lat_u", "_CoordinateAxisType", "Lat");
        
        ncfWritable.addVariable("lon_u", DataType.DOUBLE, "eta_u xi_u");
        ncfWritable.addVariableAttribute("lon_u", "long_name","longitude of U-points");
        ncfWritable.addVariableAttribute("lon_u", "units","degree_east");
        ncfWritable.addVariableAttribute("lon_u", "field","lon_u, scalar");
        ncfWritable.addVariableAttribute("lon_u", "standard_name","longitude");
        ncfWritable.addVariableAttribute("lon_u", "_CoordinateAxisType", "Lon");
        
        ncfWritable.addVariable("lat_v", DataType.DOUBLE, "eta_v xi_v");
        ncfWritable.addVariableAttribute("lat_v", "long_name","latitude of V-points");
        ncfWritable.addVariableAttribute("lat_v", "units","degree_north");
        ncfWritable.addVariableAttribute("lat_v", "field","lat_v, scalar");
        ncfWritable.addVariableAttribute("lat_v", "standard_name","latitude");
        ncfWritable.addVariableAttribute("lat_v", "_CoordinateAxisType", "Lat");
        
        ncfWritable.addVariable("lon_v", DataType.DOUBLE, "eta_v xi_v");
        ncfWritable.addVariableAttribute("lon_v", "long_name","longitude of V-points");
        ncfWritable.addVariableAttribute("lon_v", "units","degree_east");
        ncfWritable.addVariableAttribute("lon_v", "field","lon_v, scalar");
        ncfWritable.addVariableAttribute("lon_v", "standard_name","longitude");
        ncfWritable.addVariableAttribute("lon_v", "_CoordinateAxisType", "Lon");
        
        ncfWritable.addVariable("lat_psi", DataType.DOUBLE, "eta_psi xi_psi");
        ncfWritable.addVariableAttribute("lat_psi", "long_name","latitude of Psi-points");
        ncfWritable.addVariableAttribute("lat_psi", "units","degree_north");
        ncfWritable.addVariableAttribute("lat_psi", "field","lat_psi, scalar");
        ncfWritable.addVariableAttribute("lat_psi", "standard_name","latitude");
        ncfWritable.addVariableAttribute("lat_psi", "_CoordinateAxisType", "Lat");
        
        ncfWritable.addVariable("lon_psi", DataType.DOUBLE, "eta_v xi_v");
        ncfWritable.addVariableAttribute("lon_psi", "long_name","longitude of Psi-points");
        ncfWritable.addVariableAttribute("lon_psi", "units","degree_east");
        ncfWritable.addVariableAttribute("lon_psi", "field","lon_psi, scalar");
        ncfWritable.addVariableAttribute("lon_psi", "standard_name","longitude");
        ncfWritable.addVariableAttribute("lon_psi", "_CoordinateAxisType", "Lon");
        
        ncfWritable.addVariable("xl", DataType.DOUBLE, "one");
        ncfWritable.addVariableAttribute("xl", "long_name","domain length in the XI-direction");
        ncfWritable.addVariableAttribute("xl", "units","meter");
        
        ncfWritable.addVariable("el", DataType.DOUBLE, "one");
        ncfWritable.addVariableAttribute("el", "long_name","domain length in the ETA-direction");
        ncfWritable.addVariableAttribute("el", "units","meter");
        
        ncfWritable.addVariable("depthmin", DataType.DOUBLE, "one");
        ncfWritable.addVariableAttribute("depthmin", "long_name","Shallow bathymetry clipping depth");
        ncfWritable.addVariableAttribute("depthmin", "units","meter");
        
        ncfWritable.addVariable("depthmax", DataType.DOUBLE, "one");
        ncfWritable.addVariableAttribute("depthmax", "long_name","Deep bathymetry clipping depth");
        ncfWritable.addVariableAttribute("depthmax", "units","meter");
        
        ncfWritable.addVariable("spherical", DataType.CHAR, "one");
        ncfWritable.addVariableAttribute("spherical", "long_name","Grid type logical switch");
        ncfWritable.addVariableAttribute("spherical", "option_T","spherical");
        
        ncfWritable.addVariable("angle", DataType.DOUBLE, "eta_rho xi_rho");
        ncfWritable.addVariableAttribute("angle", "long_name","angle between xi axis and east");
        ncfWritable.addVariableAttribute("angle", "units","degree");
        
       
        ncfWritable.addVariable("hraw", DataType.DOUBLE, "eta_rho xi_rho");
        ncfWritable.addVariableAttribute("hraw", "long_name","Working bathymetry at RHO-points");
        ncfWritable.addVariableAttribute("hraw", "units","meter");
        
        ncfWritable.addVariable("alpha", DataType.DOUBLE, "eta_rho xi_rho");
        ncfWritable.addVariableAttribute("alpha", "long_name","Weights between coarse and fine grids at RHO-points");
       
        ncfWritable.addVariable("f", DataType.DOUBLE, "eta_rho xi_rho");
        ncfWritable.addVariableAttribute("f", "long_name","Coriolis parameter at RHO-points");
        ncfWritable.addVariableAttribute("f", "units","second-1");
        
        ncfWritable.addVariable("pm", DataType.DOUBLE, "eta_rho xi_rho");
        ncfWritable.addVariableAttribute("pm", "long_name","curvilinear coordinate metric in XI");
        ncfWritable.addVariableAttribute("pm", "units","meter-1");
        
        ncfWritable.addVariable("pn", DataType.DOUBLE, "eta_rho xi_rho");
        ncfWritable.addVariableAttribute("pn", "long_name","curvilinear coordinate metric in ETA");
        ncfWritable.addVariableAttribute("pn", "units","meter-1");
        
        ncfWritable.addVariable("dndx", DataType.DOUBLE, "eta_rho xi_rho");
        ncfWritable.addVariableAttribute("dndx", "long_name","xi derivative of inverse metric factor pn");
        ncfWritable.addVariableAttribute("dndx", "units","meter");
        
        ncfWritable.addVariable("dmde", DataType.DOUBLE, "eta_rho xi_rho");
        ncfWritable.addVariableAttribute("dmde", "long_name","xi derivative of inverse metric factor pm");
        ncfWritable.addVariableAttribute("dmde", "units","meter");
        
        ncfWritable.addVariable("x_rho", DataType.DOUBLE, "eta_rho xi_rho");
        ncfWritable.addVariableAttribute("x_rho", "long_name","x location of RHO-points");
        ncfWritable.addVariableAttribute("x_rho", "units","meter");
        
        ncfWritable.addVariable("y_rho", DataType.DOUBLE, "eta_rho xi_rho");
        ncfWritable.addVariableAttribute("y_rho", "long_name","y location of RHO-points");
        ncfWritable.addVariableAttribute("y_rho", "units","meter");
        
        ncfWritable.addVariable("x_psi", DataType.DOUBLE, "eta_psi xi_psi");
        ncfWritable.addVariableAttribute("x_psi", "long_name","x location of PSI-points");
        ncfWritable.addVariableAttribute("x_psi", "units","meter");
        
        ncfWritable.addVariable("y_psi", DataType.DOUBLE, "eta_psi xi_psi");
        ncfWritable.addVariableAttribute("y_psi", "long_name","y location of PSI-points");
        ncfWritable.addVariableAttribute("y_psi", "units","meter");
        
        ncfWritable.addVariable("x_u", DataType.DOUBLE, "eta_u xi_u");
        ncfWritable.addVariableAttribute("x_u", "long_name","x location of U-points");
        ncfWritable.addVariableAttribute("x_u", "units","meter");
        
        ncfWritable.addVariable("y_u", DataType.DOUBLE, "eta_u xi_u");
        ncfWritable.addVariableAttribute("y_u", "long_name","y location of U-points");
        ncfWritable.addVariableAttribute("y_u", "units","meter");
        
        ncfWritable.addVariable("x_v", DataType.DOUBLE, "eta_v xi_v");
        ncfWritable.addVariableAttribute("x_v", "long_name","x location of V-points");
        ncfWritable.addVariableAttribute("x_v", "units","meter");
        
        ncfWritable.addVariable("y_v", DataType.DOUBLE, "eta_v xi_v");
        ncfWritable.addVariableAttribute("y_v", "long_name","y location of V-points");
        ncfWritable.addVariableAttribute("y_v", "units","meter");
        
        ncfWritable.addVariable("mask_rho", DataType.DOUBLE, "eta_rho xi_rho");
        ncfWritable.addVariableAttribute("mask_rho", "long_name","mask on RHO-points");
        ncfWritable.addVariableAttribute("mask_rho", "option_0","land");
        ncfWritable.addVariableAttribute("mask_rho", "option_1","water");
        
        ncfWritable.addVariable("mask_psi", DataType.DOUBLE, "eta_psi xi_psi");
        ncfWritable.addVariableAttribute("mask_psi", "long_name","mask on PSI-points");
        ncfWritable.addVariableAttribute("mask_psi", "option_0","land");
        ncfWritable.addVariableAttribute("mask_psi", "option_1","water");
        
        
        ncfWritable.addVariable("mask_u", DataType.DOUBLE, "eta_u xi_u");
        ncfWritable.addVariableAttribute("mask_u", "long_name","mask on U-points");
        ncfWritable.addVariableAttribute("mask_u", "option_0","land");
        ncfWritable.addVariableAttribute("mask_u", "option_1","water");
        
        ncfWritable.addVariable("mask_v", DataType.DOUBLE, "eta_v xi_v");
        ncfWritable.addVariableAttribute("mask_v", "long_name","mask on U-points");
        ncfWritable.addVariableAttribute("mask_v", "option_0","land");
        ncfWritable.addVariableAttribute("mask_v", "option_1","water");
        
        ncfWritable.create();
    }
     
    public ROMSGrid(String url, double minLonRho, double minLatRho, double maxLonRho, double maxLatRho, double lonStep,double latStep) throws IOException {
        
        create(url, minLonRho, minLatRho, maxLonRho, maxLatRho, lonStep, latStep);
    }
    
    public void make() throws IOException, InvalidRangeException {
        
        
        ArrayDouble.D1 outAX1 = new ArrayDouble.D1(one);
        ArrayDouble.D1 outAE1 = new ArrayDouble.D1(one);
        ArrayDouble.D1 outADepthMin = new ArrayDouble.D1(one);
        ArrayDouble.D1 outADepthMax = new ArrayDouble.D1(one);
        ArrayChar.D1 outASpherical = new ArrayChar.D1(one);
        
        ArrayDouble.D2 outALonRho = new ArrayDouble.D2(etaRho,xiRho);
        ArrayDouble.D2 outALatRho = new ArrayDouble.D2(etaRho,xiRho);
        ArrayDouble.D2 outALonPsi = new ArrayDouble.D2(etaPsi,xiPsi);
        ArrayDouble.D2 outALatPsi = new ArrayDouble.D2(etaPsi,xiPsi);
        ArrayDouble.D2 outALonU = new ArrayDouble.D2(etaU,xiU);
        ArrayDouble.D2 outALatU = new ArrayDouble.D2(etaU,xiU);
        ArrayDouble.D2 outALonV = new ArrayDouble.D2(etaV,xiV);
        ArrayDouble.D2 outALatV = new ArrayDouble.D2(etaV,xiV);
        ArrayDouble.D2 outAH = new ArrayDouble.D2(etaRho,xiRho);
        ArrayDouble.D2 outAHRAW = new ArrayDouble.D2(etaRho,xiRho);
        ArrayDouble.D2 outAF = new ArrayDouble.D2(etaRho,xiRho);
        ArrayDouble.D2 outAALPHA = new ArrayDouble.D2(etaRho,xiRho);
        ArrayDouble.D2 outAANGLE = new ArrayDouble.D2(etaRho,xiRho);
        ArrayDouble.D2 outAMASKRHO = new ArrayDouble.D2(etaRho,xiRho);
        ArrayDouble.D2 outAMASKPSI = new ArrayDouble.D2(etaPsi,xiPsi);
        ArrayDouble.D2 outAMASKU = new ArrayDouble.D2(etaU,xiU);
        ArrayDouble.D2 outAMASKV = new ArrayDouble.D2(etaV,xiV);
        ArrayDouble.D2 outAXRHO = new ArrayDouble.D2(etaRho,xiRho);
        ArrayDouble.D2 outAYRHO = new ArrayDouble.D2(etaRho,xiRho);
        ArrayDouble.D2 outAXPSI = new ArrayDouble.D2(etaPsi,xiPsi);
        ArrayDouble.D2 outAYPSI = new ArrayDouble.D2(etaPsi,xiPsi);
        ArrayDouble.D2 outAXU = new ArrayDouble.D2(etaU,xiU);
        ArrayDouble.D2 outAYU = new ArrayDouble.D2(etaU,xiU);
        ArrayDouble.D2 outAXV = new ArrayDouble.D2(etaV,xiV);
        ArrayDouble.D2 outAYV = new ArrayDouble.D2(etaV,xiV);
        ArrayDouble.D2 outADNDX = new ArrayDouble.D2(etaRho,xiRho);
        ArrayDouble.D2 outADMDE = new ArrayDouble.D2(etaRho,xiRho);
        ArrayDouble.D2 outAPN = new ArrayDouble.D2(etaRho,xiRho);
        ArrayDouble.D2 outAPM = new ArrayDouble.D2(etaRho,xiRho);
        
        
        
        
        for(int j=0;j<etaRho;j++) {
            for (int i=0;i<xiRho;i++) {
                outALonRho.set(j, i, LONRHO[j][i]);
                outALatRho.set(j, i, LATRHO[j][i]);
                outAH.set(j, i, H[j][i]);
                outAF.set(j, i, F[j][i]);
                outAALPHA.set(j, i, ALPHA[j][i]);
                outAANGLE.set(j, i, ANGLE[j][i]);
                outAPN.set(j, i, PN[j][i]);
                outAPM.set(j, i, PM[j][i]);
                outADNDX.set(j, i, DNDX[j][i]);
                outADMDE.set(j, i, DMDE[j][i]);
                outAXRHO.set(j, i, XRHO[j][i]);
                outAYRHO.set(j, i, YRHO[j][i]);
                outAMASKRHO.set(j, i, MASKRHO[j][i]);
                outAHRAW.set(j, i, HRAW[j][i]);
            }
        }
        
        for(int j=0;j<etaPsi;j++) {
            for (int i=0;i<xiPsi;i++) {
                outALonPsi.set(j, i, LONPSI[j][i]);
                outALatPsi.set(j, i, LATPSI[j][i]);
                outAXPSI.set(j, i, XPSI[j][i]);
                outAYPSI.set(j, i, YPSI[j][i]);
                outAMASKPSI.set(j, i, MASKPSI[j][i]);
            }
        }
        
        for(int j=0;j<etaU;j++) {
            for (int i=0;i<xiU;i++) {
                outALonU.set(j, i, LONU[j][i]);
                outALatU.set(j, i, LATU[j][i]);
                outAXU.set(j, i, XU[j][i]);
                outAYU.set(j, i, YU[j][i]);
                outAMASKU.set(j, i, MASKU[j][i]);
            }
        }
        
        for(int j=0;j<etaV;j++) {
            for (int i=0;i<xiV;i++) {
                outALonV.set(j, i, LONV[j][i]);
                outALatV.set(j, i, LATV[j][i]);
                outAXV.set(j, i, XV[j][i]);
                outAYV.set(j, i, YV[j][i]);
                outAMASKV.set(j, i, MASKV[j][i]);
            }
        }
        
        
        
        outAX1.set(0,x1);
        outAE1.set(0,e1);
        outADepthMin.set(0,depthMin);
        outADepthMax.set(0,depthMax);
        outASpherical.set(0,spherical);
        
        ncfWritable.write("mask_rho", new int [] { 0,0 }, outAMASKRHO);
        ncfWritable.write("mask_psi", new int [] { 0,0 }, outAMASKPSI);
        ncfWritable.write("mask_u", new int [] { 0,0 }, outAMASKU);
        ncfWritable.write("mask_v", new int [] { 0,0 }, outAMASKV);
        
        ncfWritable.write("dndx", new int [] { 0,0 }, outADNDX);
        ncfWritable.write("dmde", new int [] { 0,0 }, outADMDE);
        ncfWritable.write("pn", new int [] { 0,0 }, outAPN);
        ncfWritable.write("pm", new int [] { 0,0 }, outAPM);
        ncfWritable.write("angle", new int [] { 0,0 }, outAANGLE);
        ncfWritable.write("alpha", new int [] { 0,0 }, outAALPHA);
        ncfWritable.write("f", new int [] { 0,0 }, outAF);
        ncfWritable.write("h", new int [] { 0,0 }, outAH);
        ncfWritable.write("hraw", new int [] { 0,0 }, outAHRAW);
        ncfWritable.write("lon_rho", new int [] { 0,0 }, outALonRho);
        ncfWritable.write("lat_rho", new int [] { 0,0 }, outALatRho);
        ncfWritable.write("lon_psi", new int [] { 0,0 }, outALonPsi);
        ncfWritable.write("lat_psi", new int [] { 0,0 }, outALatPsi);
        ncfWritable.write("lon_u", new int [] { 0,0 }, outALonU);
        ncfWritable.write("lat_u", new int [] { 0,0 }, outALatU);
        ncfWritable.write("lon_v", new int [] { 0,0 }, outALonV);
        ncfWritable.write("lat_v", new int [] { 0,0 }, outALatV);
        
        ncfWritable.write("xl", outAX1);
        ncfWritable.write("el", outAE1);
        ncfWritable.write("depthmin", outADepthMin);
        ncfWritable.write("depthmax", outADepthMax);
        ncfWritable.write("spherical", outASpherical);
        
        ncfWritable.close();
    }
    


    public ROMSGrid(String url) throws IOException, NCRegridderException {

        this.url=url;
        ncDataset = NetcdfDataset.openDataset(url);

        dimEtaRho = ncDataset.findDimension("eta_rho");
        dimXiRho = ncDataset.findDimension("xi_rho");
        dimEtaPsi = ncDataset.findDimension("eta_psi");
        dimXiPsi = ncDataset.findDimension("xi_psi");
        dimEtaU = ncDataset.findDimension("eta_u");
        dimXiU = ncDataset.findDimension("xi_u");
        dimEtaV = ncDataset.findDimension("eta_v");
        dimXiV = ncDataset.findDimension("xi_v");

        etaRho = dimEtaRho.getLength();
        xiRho = dimXiRho.getLength();
        etaPsi = dimEtaPsi.getLength();
        xiPsi = dimXiPsi.getLength();
        etaU = dimEtaU.getLength();
        xiU = dimXiU.getLength();
        etaV = dimEtaV.getLength();
        xiV = dimXiV.getLength();
        
        double ds=1.0/N;
        double[] lev = new double[N];
        for (int i=0;i<lev.length;i++) {
            lev[i]=(1+i)-0.5;
        }

        s_rho = new double[lev.length];
        for (int i=0;i<s_rho.length;i++) {
            s_rho[i]=(lev[i]-N)*ds;

        }
        cs_r = new double[lev.length];
        
        s_w = new double[lev.length+1];
        s_w[0] = -1;
        
        cs_w = new double[lev.length+1];
        cs_w[0] = -1;
        
        
        double cff1=(1/Math.sinh(theta_s));
        double cff2=(.5/Math.tanh(0.5*theta_s));
        
        if (theta_s>0) {
            double[] pTheta = new double[s_rho.length];
            double[] rTheta = new double[s_rho.length];
            for (int i=0;i<pTheta.length;i++) {
                pTheta[i]=Math.sinh(theta_s*s_rho[i])*cff1;
                rTheta[i]=Math.tanh(theta_s*(s_rho[i]+0.5))/(2.0*Math.tanh(0.5*theta_s)-0.5);
                cs_r[i]=(1.0-theta_b)*pTheta[i]+theta_b*rTheta[i];
                cs_w[i+1]=(1.0-theta_b)*cff1*Math.sinh(theta_s*s_w[i+1])+theta_b*(cff2*Math.tanh(theta_s*(s_w[i+1]+0.5))-0.5);
            }
        } else {
            for (int i=0;i<s_rho.length;i++) {
                cs_r[i]=s_rho[i];
                cs_w[i+1]=s_w[i+1];
            }
        }

        load(VARIABLE_LATRHO);
        load(VARIABLE_LONRHO);
        load(VARIABLE_LATPSI);
        load(VARIABLE_LONPSI);
        load(VARIABLE_LATU);
        load(VARIABLE_LONU);
        load(VARIABLE_LATV);
        load(VARIABLE_LONV);
        load(VARIABLE_H);
    }

    public static final int VARIABLE_LATRHO=11;
    public static final int VARIABLE_LONRHO=12;
    public static final int VARIABLE_LATU=13;
    public static final int VARIABLE_LATV=14;
    public static final int VARIABLE_LONU=15;
    public static final int VARIABLE_LONV=16;
    public static final int VARIABLE_ANGLE=18;
    public static final int VARIABLE_H=19;
    public static final int VARIABLE_Z=20;
    public static final int VARIABLE_MASKRHO=21;
    public static final int VARIABLE_MASKU=22;
    public static final int VARIABLE_MASKV=23;
    public static final int VARIABLE_LATPSI=24;
    public static final int VARIABLE_LONPSI=25;


    public double[][][] load(int varId) throws NCRegridderException {
        double[][][] result=null;

        switch (varId) {
            case VARIABLE_MASKU:
                if (MASKU==null) {
                    Variable var = ncDataset.findVariable("mask_u");
                    if (var!=null) {
                        try {
                            ArrayDouble.D2 a = (ArrayDouble.D2)var.read(new int[] { 0, 0 }, new int[] {etaU,xiU});
                            MASKU = (double[][])a.copyToNDJavaArray();
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable MASKU not found!");

                }
                result = new double[1][1][1];
                result[0] = MASKU;
                break;
                
           case VARIABLE_MASKV:
                if (MASKV==null) {
                    Variable var = ncDataset.findVariable("mask_v");
                    if (var!=null) {
                        try {
                            ArrayDouble.D2 a = (ArrayDouble.D2)var.read(new int[] { 0, 0 }, new int[] {etaV,xiV});
                            MASKV = (double[][])a.copyToNDJavaArray();
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable MASKV not found!");

                }
                result = new double[1][1][1];
                result[0] = MASKV;
                break;    
                
            case VARIABLE_MASKRHO:
                if (MASKRHO==null) {
                    Variable var = ncDataset.findVariable("mask_rho");
                    if (var!=null) {
                        try {
                            ArrayDouble.D2 a = (ArrayDouble.D2)var.read(new int[] { 0, 0 }, new int[] {etaRho,xiRho});
                            MASKRHO = (double[][])a.copyToNDJavaArray();
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable MASKRHO not found!");

                }
                result = new double[1][1][1];
                result[0] = MASKRHO;
                break;
                
            case VARIABLE_LATRHO:
                if (LATRHO==null) {
                    Variable var = ncDataset.findVariable("lat_rho");
                    if (var!=null) {
                        try {
                            ArrayDouble.D2 a = (ArrayDouble.D2)var.read(new int[] { 0, 0 }, new int[] {etaRho,xiRho});
                            LATRHO = (double[][])a.copyToNDJavaArray();
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable LATRHO not found!");

                }
                result = new double[1][1][1];
                result[0] = LATRHO;
                break;

            case VARIABLE_LONRHO:
                if (LONRHO==null) {
                    Variable var = ncDataset.findVariable("lon_rho");
                    if (var!=null) {
                        try {
                            ArrayDouble.D2 a = (ArrayDouble.D2)var.read(new int[] { 0, 0 }, new int[] {etaRho,xiRho});
                            LONRHO = (double[][])a.copyToNDJavaArray();
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable LONRHO not found!");

                }
                result = new double[1][1][1];
                result[0] = LONRHO;
                break;
                
             case VARIABLE_LATPSI:
                if (LATPSI==null) {
                    Variable var = ncDataset.findVariable("lat_psi");
                    if (var!=null) {
                        try {
                            ArrayDouble.D2 a = (ArrayDouble.D2)var.read(new int[] { 0, 0 }, new int[] {etaPsi,xiPsi});
                            LATPSI = (double[][])a.copyToNDJavaArray();
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable LATPSI not found!");

                }
                result = new double[1][1][1];
                result[0] = LATPSI;
                break;

            case VARIABLE_LONPSI:
                if (LONPSI==null) {
                    Variable var = ncDataset.findVariable("lon_psi");
                    if (var!=null) {
                        try {
                            ArrayDouble.D2 a = (ArrayDouble.D2)var.read(new int[] { 0, 0 }, new int[] {etaPsi,xiPsi});
                            LONPSI = (double[][])a.copyToNDJavaArray();
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable LONPSI not found!");

                }
                result = new double[1][1][1];
                result[0] = LONPSI;
                break;
                
                case VARIABLE_LATU:
                if (LATU==null) {
                    Variable var = ncDataset.findVariable("lat_u");
                    if (var!=null) {
                        try {
                            ArrayDouble.D2 a = (ArrayDouble.D2)var.read(new int[] { 0, 0 }, new int[] {etaU,xiU});
                            
                            LATU = new double[etaU][xiU];
                            for (int j=0;j<etaU;j++) {
                                for (int i=0;i<xiU;i++) {
                                    LATU[j][i]=a.get(j,i);
                                }
                            }
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable LATU not found!");

                }
                result = new double[1][][];
                result[0] = LATU;
                
                
                
                break;

            case VARIABLE_LATV:
                if (LATV==null) {
                    Variable var = ncDataset.findVariable("lat_v");
                    if (var!=null) {
                        try {
                            ArrayDouble.D2 a = (ArrayDouble.D2)var.read(new int[] { 0, 0 }, new int[] {etaV,xiV});
                            LATV = (double[][])a.copyToNDJavaArray();
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable LATV not found!");

                }
                result = new double[1][1][1];
                result[0] = LATV;
                break;

            case VARIABLE_LONU:
                if (LONU==null) {
                    Variable var = ncDataset.findVariable("lon_u");
                    if (var!=null) {
                        try {
                            ArrayDouble.D2 a = (ArrayDouble.D2)var.read(new int[] { 0, 0 }, new int[] {etaU,xiU});
                            LONU = (double[][])a.copyToNDJavaArray();
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable LONU not found!");

                }
                result = new double[1][1][1];
                result[0] = LONU;
                break;


            case VARIABLE_LONV:
                if (LONV==null) {
                    Variable var = ncDataset.findVariable("lon_v");
                    if (var!=null) {
                        try {
                            ArrayDouble.D2 a = (ArrayDouble.D2)var.read(new int[] { 0, 0 }, new int[] {etaV,xiV});
                            LONV = (double[][])a.copyToNDJavaArray();
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                            }
                    } else throw new NCRegridderException("Variable LONV not found!");

                }
                result = new double[1][1][1];
                result[0] = LONV;
                break;

            case VARIABLE_ANGLE:
                if (ANGLE==null) {
                    Variable var = ncDataset.findVariable("angle");
                    if (var!=null) {
                        try {
                            ArrayDouble.D2 a = (ArrayDouble.D2)var.read(new int[] { 0, 0 }, new int[] {etaRho,xiRho});
                            ANGLE = (double[][])a.copyToNDJavaArray();
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable ANGLE not found!");

                }
                result = new double[1][1][1];
                result[0] = ANGLE;
                break;
                
            case VARIABLE_H:
                if (H==null) {
                    Variable var = ncDataset.findVariable("h");
                    if (var!=null) {
                        try {
                            ArrayDouble.D2 a = (ArrayDouble.D2)var.read(new int[] { 0, 0 }, new int[] {etaRho,xiRho});
                            H = (double[][])a.copyToNDJavaArray();
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable H not found!");

                }
                result = new double[1][1][1];
                result[0] = H;
                //System.out.println("H="+H);
                
                hc=Double.NaN;
                for (int j=0;j<etaRho;j++) {
                    for (int i=0;i<xiRho;i++) {
                        
                        if (H[j][i]!=Double.NaN) {
                            // System.out.println("H["+j+"]["+i+"]="+H[j][i]);
                            if (j==0 && i==0) {
                                hc = H[j][i];
                            }
                            else if ( H[j][i] < hc)  {
                                
                                hc = H[j][i];
                            }
                        }
                    }
                }

                // Check if the hc is less than the threshold
                if (hc < hc_threshold) {
                    hc = hc_threshold;
                }

                System.out.println("hc="+hc);
                break;
                
            case VARIABLE_Z:
                if (Z==null) {
                    load(VARIABLE_H);
                    
                    

                    Z = new double[s_rho.length][etaRho][xiRho];
                    System.out.println("Variable_Z: hc="+hc);
                    double S=0;
                    for (int k = 0; k < s_rho.length; k++) { 
                        for (int j = 0; j < etaRho; j++) { 
                            for (int i = 0; i < xiRho; i++) {
                                S=hc*s_rho[k]+((H[j][i]-hc)*cs_r[k]);
                                Z[k][j][i]=S+zeta*(1+(S/H[j][i]));
                            }
                        }
                    }
                }
                result = Z;
                break;

        }
        if (result==null) throw new NCRegridderException("Unknown variable to load! (varId="+varId+")");
        return result;
    }

    public double[] getMinMaxLatRho() throws NCRegridderException {
        double[] result=new double[3];
        
        int count = 0;
        HashSet<Double> steps=new HashSet<Double>();
        double delta;
        load(VARIABLE_LATRHO);
        result[0] = LATRHO[0][0];
        result[1] = LATRHO[0][0];
        result[2] = 0;
        for (int j = 0; j < etaRho; j++) { 
            for (int i = 0; i < xiRho; i++) {
                if (LATRHO[j][i]<result[0]) {
                    result[0]=LATRHO[j][i];
                }
                if (LATRHO[j][i]>result[1]) {
                    result[1]=LATRHO[j][i];
                }
                /*
                if (j!=0) {
                    delta=Math.abs(LATRHO[j][i]-LATRHO[j-1][i]);
                    result[2]=result[2]+delta;
                    count++;
                    
                    
                    //System.out.println(delta);
                }
                */
                if (j>0 && i>0) {
                    //double d=Haversine.distance(LATRHO[j][i],LONRHO[j][i],LATRHO[j][i],LONRHO[j][i-1]);
                    delta=Math.abs(LATRHO[j][i]-LATRHO[j-1][i]);
                    if (delta==0) {
                        delta=Math.abs(LATRHO[j][i]-LATRHO[j][i-1]);
                    }
                    //System.out.println(j+","+i+":"+LATRHO[j][i]+" "+LATRHO[j-1][i]+" -> "+Math.abs(LATRHO[j][i]-LATRHO[j-1][i]));
                    //System.out.println(j+","+i+":"+LATRHO[j][i]+" "+LATRHO[j][i-1]+" -> "+Math.abs(LATRHO[j][i]-LATRHO[j][i-1]));
                    steps.add(delta);
                }
            }
        }
        double s=0;
        for (Double s1:steps) {
            s=s+s1;
            count++;
        }
        result[2]=s/(double)count;
        return result;
    }

    public double[] getMinMaxLonRho() throws NCRegridderException {
        double[] result=new double[3];
        
        int count = 0;
        double delta;
        HashSet<Double> steps=new HashSet<Double>();
        
        load(VARIABLE_LONRHO);
        result[0] = LONRHO[0][0];
        result[1] = LONRHO[0][0];
        result[2] = 0;
        System.out.println(result[0]);
        
        for (int j = 0; j < etaRho; j++) { 
            for (int i = 0; i < xiRho; i++) {
                
                if (LONRHO[j][i]<result[0]) {
                    result[0]=LONRHO[j][i];
                }
                if (LONRHO[j][i]>result[1]) {
                    result[1]=LONRHO[j][i];
                }
                
                /*
                if (i!=0) {
                    result[2]=result[2]+Math.abs(LONRHO[j][i]-LONRHO[j][i-1]);
                    count++;
                }
                */
                
                if (j>0 && i>0) {
                    //double d=Haversine.distance(LATRHO[j][i],LONRHO[j][i],LATRHO[j][i],LONRHO[j][i-1]);
                    delta=Math.abs(LONRHO[j][i]-LONRHO[j][i-1]);
                    if (delta==0) {
                        delta=Math.abs(LONRHO[j][i]-LONRHO[j-1][i]);
                    }
                    steps.add(delta);
                }
                
            }
        }
        double s=0;
        for (Double s1:steps) {
            s=s+s1;
            count++;
        }
        result[2]=s/(double)count;
        return result;
    }

    private IdDoubleVectData srcValues=null;
    private Stations srcStations=null;
    public void setSrcStations(Stations srcStations) {
        this.srcStations=srcStations;
    }
    public void setSrcValues(IdDoubleVectData srcValues) {
        this.srcValues=srcValues;
    }

    public void prepare() throws InterpolatorException {
        int[][] dst2id = new int[etaRho][xiRho];
        Stations dstStations = new Stations();
        
        int id=0;
        for (int j=0;j<etaRho;j++) {
            for (int i=0;i<xiRho;i++) {
                Station station = new Station(id,LONRHO[j][i],LATRHO[j][i]);
                dstStations.add(station);
                dst2id[j][i]=id;
                id++; 
            }
        }
        System.out.println(dstStations.size());
        
        IdDoubleVectData dstValues = new IdDoubleVectData();
        Kriging kriging = new Kriging(srcStations, srcValues, dstStations, dstValues);
        
        try {
            kriging.execute();
        } catch (KrigingException ex) {
            throw new InterpolatorException(ex);
        }

        IdDoubleVectData outData=kriging.getOutData();
        
        int count=0;
        H = new double[etaRho][xiRho];
        for (int j=0;j<etaRho;j++) {
            for (int i=0;i<xiRho;i++) {
                id = dst2id[j][i];
                H[j][i]=outData.get(id)[0];
            }
        }
                    
        
    }

    public int getVStreching() {
        return 1;
    }

    public double getNoData() {
        return noData;
    }

   
    
    
}
