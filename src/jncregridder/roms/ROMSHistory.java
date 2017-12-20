/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jncregridder.roms;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import jncregridder.util.JulianDate;
import jncregridder.util.NCRegridderException;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;

/**
 *
 * @author raffaelemontella
 */
public class ROMSHistory {
    
    private String url;
    private NetcdfDataset ncDataset;

    public Dimension dimSW;
    public Dimension dimSRho;
    public Dimension dimEtaRho;
    public Dimension dimXiRho;
    public Dimension dimEtaU;
    public Dimension dimXiU;
    public Dimension dimEtaV;
    public Dimension dimXiV;
    public Dimension dimTime;


    private double[][] LATRHO = null;
    private double[][] LONRHO = null;
    public double[][] LATU = null;
    public double[][] LATV = null;
    public double[][] LONU = null;
    public double[][] LONV = null;
    public double[][] ANGLE = null;
    public double[][] MASKRHO = null;
    public double[][] MASKU = null;
    public double[][] MASKV = null;
    public double[][] H = null;
    public double[][][] ZR = null;
    public double[][][] ZW = null;
    
    public double[] OCEANTIME = null;
    public double[][] ZETA = null;
    public double[][] SUSTR = null;
    public double[][] SVSTR = null;
    public double[][] UBAR = null;
    public double[][] VBAR = null;
    public double[][][] TEMP = null;
    public double[][][] SALT = null;
    public double[][][] U = null;
    public double[][][] V = null;
    public double[][][] W = null;

    int etaRho = -1;
    int xiRho = -1;
    int etaU = -1;
    int xiU = -1;
    int etaV = -1;
    int xiV = -1;
    int sRho = -1;
    int sW = -1;
    
    public double[][] getLATU() throws NCRegridderException { load(VARIABLE_LATU); return LATU; }
    public double[][] getLONU() throws NCRegridderException { load(VARIABLE_LONU); return LONU; }
    public double[][] getLATV() throws NCRegridderException { load(VARIABLE_LATV); return LATV; }
    public double[][] getLONV() throws NCRegridderException { load(VARIABLE_LONV); return LONV; }
    public double[][] getLATRHO() throws NCRegridderException { load(VARIABLE_LATRHO); return LATRHO; }
    public double[][] getLONRHO() throws NCRegridderException { load(VARIABLE_LONRHO); return LONRHO; }
    public double[][] getANGLE() throws NCRegridderException { return load(VARIABLE_ANGLE)[0]; }
    public double[][] getH() throws NCRegridderException { return load(VARIABLE_H)[0]; }
    public double[][][] getZR() throws NCRegridderException { return load(VARIABLE_ZR); }
    public double[][][] getZW() throws NCRegridderException { return load(VARIABLE_ZW); }
    public double[][] getZETA() throws NCRegridderException { return load(VARIABLE_ZETA)[0]; }
    public double[][] getMASKRHO() throws NCRegridderException { return load(VARIABLE_MASKRHO)[0]; }
    public double[][] getMASKU() throws NCRegridderException { return load(VARIABLE_MASKU)[0]; }
    public double[][] getMASKV() throws NCRegridderException { return load(VARIABLE_MASKV)[0]; }
    
    public double[][] getUBAR() throws NCRegridderException { return load(VARIABLE_UBAR)[0]; }
    public double[][] getVBAR() throws NCRegridderException { return load(VARIABLE_VBAR)[0]; }
    public double[][] getSUSTR() throws NCRegridderException { return load(VARIABLE_SUSTR)[0]; }
    public double[][] getSVSTR() throws NCRegridderException { return load(VARIABLE_SVSTR)[0]; }
    public double[][][] getU() throws NCRegridderException { return load(VARIABLE_U); }
    public double[][][] getV() throws NCRegridderException { return load(VARIABLE_V); }
    public double[][][] getW() throws NCRegridderException { return load(VARIABLE_W); }
    public double[][][] getTEMP() throws NCRegridderException { return load(VARIABLE_TEMP); }
    public double[][][] getSALT() throws NCRegridderException { return load(VARIABLE_SALT); }
    public double[] getOCEANTIME() throws NCRegridderException { return load(VARIABLE_OCEANTIME)[0][0]; }
    
    
    
    private double theta_s=3;
    private double theta_b=0;
    //private int N = 0;
    private double hc=Double.NaN;
    
    
    public double getThetaS() { return theta_s; }
    public double getThetaB() { return theta_b; }
    public double getHC() { return hc; }
    
    private double[] s_w = null;
    private double[] s_rho = null;
    private double[] cs_r = null;
    private double[] cs_w = null;
    
    public double[] getSRho() { return s_rho; }
    public double[] getSW() { return s_w; }
    public double[] getCSR() { return cs_r; }
    public double[] getCSW() { return cs_w; }
    
    private int localTime=0;
    private double oceanTime=0;
    
    public ROMSHistory(String url) throws IOException, NCRegridderException {


        ncDataset = NetcdfDataset.openDataset(url);

        dimSW = ncDataset.findDimension("s_w");
        dimSRho = ncDataset.findDimension("s_rho");
        dimEtaRho = ncDataset.findDimension("eta_rho");
        dimXiRho = ncDataset.findDimension("xi_rho");
        dimEtaU = ncDataset.findDimension("eta_u");
        dimXiU = ncDataset.findDimension("xi_u");
        dimEtaV = ncDataset.findDimension("eta_v");
        dimXiV = ncDataset.findDimension("xi_v");
        dimTime = ncDataset.findDimension("ocean_time");

        
        sW = dimSW.getLength();
        sRho = dimSRho.getLength();
        etaRho = dimEtaRho.getLength();
        xiRho = dimXiRho.getLength();
        etaU = dimEtaU.getLength();
        xiU = dimXiU.getLength();
        etaV = dimEtaV.getLength();
        xiV = dimXiV.getLength();
        
        
        
        Variable var = null;
        
        var = ncDataset.findVariable("s_rho");
        if (var!=null) {
            try {
                ArrayDouble.D1 a = (ArrayDouble.D1)var.read(new int[] { 0, }, new int[] {sRho});
                s_rho = (double[])a.copyToNDJavaArray();
            } catch (IOException ex) {
                throw new NCRegridderException(ex);
            } catch (InvalidRangeException ex) {
                throw new NCRegridderException(ex);
            }
        } else throw new NCRegridderException("Variable s_rho not found!");

        var = ncDataset.findVariable("Cs_r");
        if (var!=null) {
            try {
                ArrayDouble.D1 a = (ArrayDouble.D1)var.read(new int[] { 0, }, new int[] {sRho});
                cs_r = (double[])a.copyToNDJavaArray();
            } catch (IOException ex) {
                throw new NCRegridderException(ex);
            } catch (InvalidRangeException ex) {
                throw new NCRegridderException(ex);
            }
        } else throw new NCRegridderException("Variable Cs_r not found!");

        
        var = ncDataset.findVariable("s_w");
        if (var!=null) {
            try {
                ArrayDouble.D1 a = (ArrayDouble.D1)var.read(new int[] { 0, }, new int[] {sW});
                s_w = (double[])a.copyToNDJavaArray();
            } catch (IOException ex) {
                throw new NCRegridderException(ex);
            } catch (InvalidRangeException ex) {
                throw new NCRegridderException(ex);
            }
        } else throw new NCRegridderException("Variable s_w not found!");

        var = ncDataset.findVariable("Cs_w");
        if (var!=null) {
            try {
                ArrayDouble.D1 a = (ArrayDouble.D1)var.read(new int[] { 0, }, new int[] {sW});
                cs_w = (double[])a.copyToNDJavaArray();
            } catch (IOException ex) {
                throw new NCRegridderException(ex);
            } catch (InvalidRangeException ex) {
                throw new NCRegridderException(ex);
            }
        } else throw new NCRegridderException("Variable Cs_w not found!");

        

        load(VARIABLE_LATRHO);
        load(VARIABLE_LONRHO);
        load(VARIABLE_LATU);
        load(VARIABLE_LONU);
        load(VARIABLE_LATV);
        load(VARIABLE_LONV);
        load(VARIABLE_H);
        load(VARIABLE_OCEANTIME);
        
    }
    
    public static final int VARIABLE_LATRHO=11;
    public static final int VARIABLE_LONRHO=12;
    public static final int VARIABLE_LATU=13;
    public static final int VARIABLE_LATV=14;
    public static final int VARIABLE_LONU=15;
    public static final int VARIABLE_LONV=16;
    public static final int VARIABLE_ANGLE=18;
    public static final int VARIABLE_H=19;
    public static final int VARIABLE_ZR=20;
    public static final int VARIABLE_MASKRHO=21;
    public static final int VARIABLE_UBAR=22;
    public static final int VARIABLE_VBAR=23;
    public static final int VARIABLE_TEMP=24;
    public static final int VARIABLE_SALT=25;
    public static final int VARIABLE_U=26;
    public static final int VARIABLE_V=27;
    public static final int VARIABLE_W=28;
    public static final int VARIABLE_SUSTR=29;
    public static final int VARIABLE_SVSTR=30;
    public static final int VARIABLE_ZETA=31;
    public static final int VARIABLE_ZW=32;
    public static final int VARIABLE_OCEANTIME=33;
    public static final int VARIABLE_MASKU=34;
    public static final int VARIABLE_MASKV=35;
    
    private GregorianCalendar gcLocalTime = null;
    public Calendar getTimeAsCalendar() {
        return gcLocalTime;
    }
    
    
    public void setTime(int localTime) throws NCRegridderException  {

        this.localTime = localTime;
        
        Variable var = ncDataset.findVariable("ocean_time");
        if (var!=null) {
            try {
                ArrayDouble.D1 a = (ArrayDouble.D1)var.read(new int[] { localTime }, new int[] {1});
                oceanTime = ((double[])a.copyToNDJavaArray())[0];
                
                double julianOceanTime=oceanTime/86400+JulianDate.get19680523();
                int[] ymdh=JulianDate.fromJulian(julianOceanTime);
                
                gcLocalTime = new GregorianCalendar(ymdh[0], ymdh[1]-1, ymdh[2], ymdh[3], 0);
                
                UBAR=null;
                VBAR=null;
                TEMP=null;
                SALT=null;
                ZETA=null;
                U=null;
                V=null;
                SUSTR=null;
                SVSTR=null;
                ZR=null;
                ZW=null;
        
            } catch (IOException ex) {
                throw new NCRegridderException(ex);
            } catch (InvalidRangeException ex) {
                throw new NCRegridderException(ex);
            }
        } else throw new NCRegridderException("Variable ocean_time not found!");
        
        
        //W=null;
    }
    
    public double[][][] load(int varId) throws NCRegridderException {
        double[][][] result=null;

        switch (varId) {
            
            
            
            case VARIABLE_OCEANTIME:
                if (MASKRHO==null) {
                    Variable var = ncDataset.findVariable("ocean_time");
                    if (var!=null) {
                        try {
                            ArrayDouble.D1 a = (ArrayDouble.D1)var.read(new int[] { 0 }, new int[] {dimTime.getLength()});
                            OCEANTIME = (double[])a.copyToNDJavaArray();
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable OCEANTIME not found!");

                }
                result = new double[1][1][1];
                result[0][0] = OCEANTIME;
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
                
            case VARIABLE_ZETA:
                if (ZETA==null) {
                    Variable var = ncDataset.findVariable("zeta");
                    if (var!=null) {
                        try {
                            ZETA=new double[etaRho][xiRho];
                            ArrayFloat.D3 a = (ArrayFloat.D3)var.read(new int[] { localTime,0, 0 }, new int[] {1,etaRho,xiRho});
                            
                            for (int j=0;j<etaRho; j++) {
                                for (int i=0;i<xiRho; i++) {
                                    ZETA[j][i] = a.get(0,j,i);
                                }
                            }
                            
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable ZETA not found!");

                }
                result = new double[1][1][1];
                result[0] = ZETA;
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
                System.out.println("hc="+hc);
                break;
                
            case VARIABLE_ZR:
                if (ZR==null) {
                    load(VARIABLE_H);
                    load(VARIABLE_ZETA);
                    
                    ZR = new double[sRho][etaRho][xiRho];
                    System.out.println("Variable_ZR: hc="+hc);
                    double S=0;
                    for (int k = 0; k < sRho; k++) { 
                        for (int j = 0; j < etaRho; j++) { 
                            for (int i = 0; i < xiRho; i++) {
                                S=hc*s_rho[k]+((H[j][i]-hc)*cs_r[k]);
                                ZR[k][j][i]=S+ZETA[j][i]*(1+(S/H[j][i]));
                            }
                        }
                    }
                }
                result = ZR;
                break;
                
            case VARIABLE_ZW:
                if (ZW==null) {
                    load(VARIABLE_H);
                    load(VARIABLE_ZETA);
                    
                    ZW = new double[sW][etaRho][xiRho];
                    System.out.println("Variable_ZW: hc="+hc);
                    double S=0;
                    for (int k = 0; k < sW; k++) { 
                        for (int j = 0; j < etaRho; j++) { 
                            for (int i = 0; i < xiRho; i++) {
                                S=hc*s_w[k]+((H[j][i]-hc)*cs_w[k]);
                                ZW[k][j][i]=S+ZETA[j][i]*(1+(S/H[j][i]));
                            }
                        }
                    }
                }
                result = ZW;
                break;
                
            case VARIABLE_UBAR:
                if (UBAR==null) {
                    Variable var = ncDataset.findVariable("ubar");
                    if (var!=null) {
                        try {
                            UBAR=new double[etaU][xiU];
                            ArrayFloat.D3 a = (ArrayFloat.D3)var.read(new int[] { localTime,0, 0 }, new int[] {1,etaU,xiU});
                            
                            for (int j=0;j<etaU; j++) {
                                for (int i=0;i<xiU; i++) {
                                    UBAR[j][i] = a.get(0,j,i);
                                }
                            }
                            
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable UBAR not found!");

                }
                result = new double[1][1][1];
                result[0] = UBAR;
                break;
                
            case VARIABLE_VBAR:
                if (VBAR==null) {
                    Variable var = ncDataset.findVariable("vbar");
                    if (var!=null) {
                        try {
                            VBAR=new double[etaV][xiV];
                            ArrayFloat.D3 a = (ArrayFloat.D3)var.read(new int[] { localTime,0, 0 }, new int[] {1,etaV,xiV});
                            
                            for (int j=0;j<etaV; j++) {
                                for (int i=0;i<xiV; i++) {
                                    VBAR[j][i] = a.get(0,j,i);
                                }
                            }
                            
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable VBAR not found!");

                }
                result = new double[1][1][1];
                result[0] = VBAR;
                break;
                
                
            case VARIABLE_TEMP:
                if (TEMP==null) {
                    Variable var = ncDataset.findVariable("temp");
                    if (var!=null) {
                        try {
                            TEMP=new double[sRho][etaRho][xiRho];
                            ArrayFloat.D4 a = (ArrayFloat.D4)var.read(new int[] { localTime,0,0, 0 }, new int[] {1,sRho,etaRho,xiRho});
                            
                            for (int k=0;k<sRho;k++) {
                                for (int j=0;j<etaRho; j++) {
                                    for (int i=0;i<xiRho; i++) {
                                        TEMP[k][j][i] = a.get(0,k,j,i);
                                    }
                                }
                            }
                            
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable TEMP not found!");

                }
                result = TEMP;
                break;
                
            case VARIABLE_SALT:
                if (SALT==null) {
                    Variable var = ncDataset.findVariable("salt");
                    if (var!=null) {
                        try {
                            SALT=new double[sRho][etaRho][xiRho];
                            ArrayFloat.D4 a = (ArrayFloat.D4)var.read(new int[] { localTime,0,0, 0 }, new int[] {1,sRho,etaRho,xiRho});
                            
                            for (int k=0;k<sRho;k++) {
                                for (int j=0;j<etaRho; j++) {
                                    for (int i=0;i<xiRho; i++) {
                                        SALT[k][j][i] = a.get(0,k,j,i);
                                    }
                                }
                            }
                            
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable SALT not found!");

                }
                result = new double[1][1][1];
                result = SALT;
                break;
                
            case VARIABLE_W:
                if (W==null) {
                    Variable var = ncDataset.findVariable("w");
                    if (var!=null) {
                        try {
                            W=new double[sW][etaRho][xiRho];
                            ArrayFloat.D4 a = (ArrayFloat.D4)var.read(new int[] { localTime,0,0, 0 }, new int[] {1,sW,etaRho,xiRho});
                            
                            for (int k=0;k<sW;k++) {
                                for (int j=0;j<etaRho; j++) {
                                    for (int i=0;i<xiRho; i++) {
                                        W[k][j][i] = a.get(0,k,j,i);
                                    }
                                }
                            }
                            
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable W not found!");

                }
                result = new double[1][1][1];
                result = W;
                break;
                
            case VARIABLE_U:
                if (U==null) {
                    Variable var = ncDataset.findVariable("u");
                    if (var!=null) {
                        try {
                            U=new double[sRho][etaU][xiU];
                            ArrayFloat.D4 a = (ArrayFloat.D4)var.read(new int[] { localTime,0,0, 0 }, new int[] {1,sRho,etaU,xiU});
                            
                            for (int k=0;k<sRho;k++) {
                                for (int j=0;j<etaU; j++) {
                                    for (int i=0;i<xiU; i++) {
                                        U[k][j][i] = a.get(0,k,j,i);
                                    }
                                }
                            }
                            
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable U not found!");

                }
                result = new double[1][1][1];
                result = U;
                break;
                
            case VARIABLE_V:
                if (V==null) {
                    Variable var = ncDataset.findVariable("v");
                    if (var!=null) {
                        try {
                            V=new double[sRho][etaV][xiV];
                            ArrayFloat.D4 a = (ArrayFloat.D4)var.read(new int[] { localTime,0,0, 0 }, new int[] {1,sRho,etaV,xiV});
                            
                            for (int k=0;k<sRho;k++) {
                                for (int j=0;j<etaV; j++) {
                                    for (int i=0;i<xiV; i++) {
                                        V[k][j][i] = a.get(0,k,j,i);
                                    }
                                }
                            }
                            
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable V not found!");

                }
                result = new double[1][1][1];
                result = V;
                break;
                
                
                
            case VARIABLE_SUSTR:
                if (SUSTR==null) {
                    Variable var = ncDataset.findVariable("sustr");
                    if (var!=null) {
                        try {
                            SUSTR=new double[etaU][xiU];
                            ArrayFloat.D3 a = (ArrayFloat.D3)var.read(new int[] { localTime,0, 0 }, new int[] {1,etaU,xiU});
                            
                            for (int j=0;j<etaU; j++) {
                                for (int i=0;i<xiU; i++) {
                                    SUSTR[j][i] = a.get(0,j,i);
                                }
                            }
                            
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable SUSTR not found!");

                }
                result = new double[1][1][1];
                result[0] = SUSTR;
                break;
                
            case VARIABLE_SVSTR:
                if (SVSTR==null) {
                    Variable var = ncDataset.findVariable("svstr");
                    if (var!=null) {
                        try {
                            SVSTR=new double[etaV][xiV];
                            ArrayFloat.D3 a = (ArrayFloat.D3)var.read(new int[] { localTime,0, 0 }, new int[] {1,etaV,xiV});
                            
                            for (int j=0;j<etaV; j++) {
                                for (int i=0;i<xiV; i++) {
                                    SVSTR[j][i] = a.get(0,j,i);
                                }
                            }
                            
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable SVSTR not found!");

                }
                result = new double[1][1][1];
                result[0] = SVSTR;
                break;
                

        }
        if (result==null) throw new NCRegridderException("Unknown variable to load! (varId="+varId+")");
        return result;
    }
    
    public void saveAsCF(String filename) throws IOException, NCRegridderException, InvalidRangeException {
        NetcdfFileWriteable ncfWritable = NetcdfFileWriteable.createNew(url);
        
        ncfWritable.addGlobalAttribute("type", "Initial file");
        ncfWritable.addGlobalAttribute("title", "");
        ncfWritable.addGlobalAttribute("grd_file", "");
        ncfWritable.addGlobalAttribute("source", "");
        
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        ncfWritable.addGlobalAttribute("date", sdf.format(cal.getTime()));
        
        ncfWritable.addDimension("Y",dimEtaRho.getLength());
        ncfWritable.addDimension("X",dimXiRho.getLength());
        
        
        ncfWritable.addVariable("Y", DataType.FLOAT, "Y X");
        ncfWritable.addVariableAttribute("Y", "long_name", "lats");
        ncfWritable.addVariableAttribute("Y", "units","degrees_north");
        ncfWritable.addVariableAttribute("Y", "axis","Y");
        
        ncfWritable.addVariable("X", DataType.FLOAT, "Y X");
        ncfWritable.addVariableAttribute("X", "long_name", "lons");
        ncfWritable.addVariableAttribute("X", "units","degrees_east");
        ncfWritable.addVariableAttribute("X", "axis","X");
        
        
        
         
        ncfWritable.addVariable("ubar", DataType.FLOAT, "Y X");
        ncfWritable.addVariableAttribute("ubar", "long_name", "ubar");
        ncfWritable.addVariableAttribute("ubar", "units","ms-1");
        
        ncfWritable.addVariable("vbar", DataType.FLOAT, "Y X");
        ncfWritable.addVariableAttribute("vbar", "long_name", "vbar");
        ncfWritable.addVariableAttribute("vbar", "units","ms-1");
        
        
        ncfWritable.create();
        
        
        setTime(0);
        
        ArrayFloat.D2 outAUBAR = new ArrayFloat.D2(etaU, xiU);
        ArrayFloat.D2 outAVBAR = new ArrayFloat.D2(etaV, xiV);
        ArrayFloat.D2 outALATRHO = new ArrayFloat.D2(etaRho, xiRho);
        ArrayFloat.D2 outALONRHO = new ArrayFloat.D2(etaRho, xiRho);
        
        load(VARIABLE_LATRHO);
        load(VARIABLE_LONRHO);
        load(VARIABLE_UBAR);
        load(VARIABLE_VBAR);
        
        for (int j=0;j<etaRho;j++) {
            for (int i=0;i<xiRho;i++) {
                outALATRHO.set(j,i,(float)LATRHO[j][i]);
                outALONRHO.set(j,i,(float)LONRHO[j][i]);
            }
        }
        
        
        for (int j=0;j<etaU;j++) {
            for (int i=0;i<xiU;i++) {
                outAUBAR.set(j,i,(float)UBAR[j][i]);
            }
        }
        
        

        for (int j=0;j<etaV;j++) {
            for (int i=0;i<xiV;i++) {
                outAVBAR.set(j,i,(float)VBAR[j][i]);
            }
        }
        
        ncfWritable.write("ubar", outAUBAR);
        ncfWritable.write("vbar", outAVBAR);
        ncfWritable.write("X", outALONRHO);
        ncfWritable.write("Y", outALATRHO);
        
        ncfWritable.close();
        
        
    }

    public void saveAsGrADSDesc(String fileName) throws IOException {
        FileWriter fw=new FileWriter(fileName);
        PrintWriter pw = new PrintWriter(fw);
        
        int nXLevels=0;
        int nYLevels=0;
        
        ArrayList<String> alXLevels = new ArrayList<String>();
        ArrayList<String> alYLevels = new ArrayList<String>();
        
        
        String xLevels="";
        String yLevels="";
        
        int count=0;
        for (int j=0;j<etaV;j++) {
            System.out.println("j:"+j);
            for (int i=0;i<xiU;i++) {
                xLevels+=" "+String.format("%2.2f",LONRHO[j][i]);
                yLevels+=" "+String.format("%2.2f",LATRHO[j][i]);
                count++;
                nXLevels++;
                nYLevels++;
                if (count==10) {
                    alXLevels.add(xLevels); xLevels="";
                    alYLevels.add(yLevels); yLevels="";
                    
                    count=0;
                } 
            }
            
        }
        
        pw.println("DSET ^all.nc");
        pw.println("DTYPE netcdf");
        pw.println("TITLE 4-D Ocean Variables: Velocity Components");
        pw.println("UNDEF 1.e+37"); 
        pw.println("XDEF "+nXLevels+" levels "+alYLevels.get(0));
        for (int i=1;i<alXLevels.size();i++) {
            pw.println("  "+alXLevels.get(i));
        }
        
        pw.println("YDEF "+nYLevels+" levels "+alYLevels.get(0));
        for (int i=1;i<alYLevels.size();i++) {
            pw.println("  "+alYLevels.get(i));
        }
        pw.println("TDEF 1 linear 10jul2012 1hr");
        pw.println("VARS 2");
        pw.println("ubar     0  t,y,x  Zonal Velocity (cm/s)");
        pw.println("vbar     0  t,y,x  Meridional Velocity (cm/s)");
        pw.println("ENDVARS");
        
        pw.close();
        fw.close();
    }
/*
    public double[][][][] getDsgUV(double missingData) throws NCRegridderException {
        double[][] angle=null;
        angle=load(VARIABLE_ANGLE)[0];
        return getDsgUV(angle,missingData);
    } 
  */  
    public double[][][][] getDsgUV(double[][] angle, double missingData) throws NCRegridderException {
        double[][][] dsU=new double[sRho][etaRho][xiRho];
        double[][][] dsV=new double[sRho][etaRho][xiRho];
        double[][][][] result = new double[2][1][1][1];
        result[0]=dsU;
        result[1]=dsV;
        load(VARIABLE_U);
        load(VARIABLE_V);
        
        for (int k=0;k<sRho;k++) {
            destag(U[k], V[k], dsU[k], dsV[k], missingData);
            if (angle!=null) rotate(dsU[k],dsV[k],angle,missingData);
        }
        return result;
    }
    
    public double[][][] getDsgSTR(double[][] angle,double missingData) throws NCRegridderException {
        double[][] dsSUSTR=new double[etaRho][xiRho];
        double[][] dsSVSTR=new double[etaRho][xiRho];
        double[][][] result = new double[2][1][1];
        result[0]=dsSUSTR;
        result[1]=dsSVSTR;
        load(VARIABLE_SUSTR);
        load(VARIABLE_SVSTR);
        
        destag(SUSTR, SVSTR, dsSUSTR, dsSVSTR, missingData);
        if (angle!=null) rotate(dsSUSTR,dsSVSTR,angle,missingData);
        return result;
    }
    
    public double[][][] getDsgBAR(double[][] angle,double missingData) throws NCRegridderException {
        double[][] dsUBAR=new double[etaRho][xiRho];
        double[][] dsVBAR=new double[etaRho][xiRho];
        double[][][] result = new double[2][1][1];
        result[0]=dsUBAR;
        result[1]=dsVBAR;
        load(VARIABLE_UBAR);
        load(VARIABLE_VBAR);
        
        destag(UBAR, VBAR, dsUBAR, dsVBAR,missingData);
        if (angle!=null) rotate(dsUBAR,dsVBAR,angle,missingData);
        return result;
    }
    
    public void destag(double[][] USRC, double[][] VSRC, double[][] UDST, double[][] VDST, double missingData) {
         
         
         for (int j=0;j<etaRho;j++) {
            for (int i=0;i<xiRho;i++) {
                //System.out.println("j:"+j+" i:"+i);
                if (i<xiU-1) {
                    if (Double.isNaN(USRC[j][i])==false && USRC[j][i]!=missingData && Double.isNaN(USRC[j][i+1])==false && USRC[j][i+1]!=missingData) {
                        UDST[j][i] = (USRC[j][i]+USRC[j][i+1] )*0.5;
                    } else if (Double.isNaN(USRC[j][i])==false && USRC[j][i]!=missingData) {
                        UDST[j][i] = USRC[j][i];
                    } else if (Double.isNaN(USRC[j][i+1])==false && USRC[j][i+1]!=missingData) {
                        UDST[j][i] = USRC[j][i+1];
                    } else UDST[j][i]=missingData;
                } else {
                    UDST[j][i] = USRC[j][xiU-1];
                }
                
            }
         }
         
         for (int j=0;j<etaRho;j++) {
            for (int i=0;i<xiRho;i++) {
                
                if (j<etaV-1) {
                    if (Double.isNaN(VSRC[j][i])==false && VSRC[j][i]!=missingData && Double.isNaN(VSRC[j+1][i])==false && VSRC[j+1][i]!=missingData) {
                        VDST[j][i] = (VSRC[j][i]+VSRC[j+1][i] )*0.5;
                    } else if (Double.isNaN(VSRC[j][i])==false && VSRC[j][i]!=missingData) {
                        VDST[j][i] = VSRC[j][i];
                    } else if (Double.isNaN(VSRC[j+1][i])==false && VSRC[j+1][i]!=missingData) {
                        VDST[j][i] = VSRC[j+1][i];
                    } else VDST[j][i]=missingData;
                } else {
                    VDST[j][i] = VSRC[etaV-1][i];
                }
            }
         }
         
         for (int j=0;j<etaRho;j++) {
            for (int i=0;i<xiRho;i++) {
                if (Double.isNaN(UDST[j][i]) == true || UDST[j][i]==missingData || Double.isNaN(VDST[j][i]) == true || VDST[j][i]==missingData) {
                   UDST[j][i]=missingData;
                   VDST[j][i]=missingData;
                } else {
                    UDST[j][i]=+UDST[j][i];
                    VDST[j][i]=+VDST[j][i];
                }
            }
         }
     }

    public void rotate(double u[][], double v[][], double angle[][], double missingValue) throws NCRegridderException
    {
        if (u==null) throw new NCRegridderException("U is null!");
        if (v==null) throw new NCRegridderException("V is null!");
        if (angle==null) throw new NCRegridderException("ANGLE is null!");
        
        for(int j = 0; j < etaRho; j++)
        {
            for(int i = 0; i < xiRho; i++)
                if(!Double.isNaN(u[j][i]) && !Double.isNaN(v[j][i]) && !Double.isNaN(angle[j][i]) && u[j][i] != missingValue && v[j][i] != missingValue && angle[j][i] != missingValue)
                {
                    /*
                     * 
                    cosa = cos(angle);
                    sina = sin(angle);
                    u = ur.*cosa - vr.*sina;
                    v = vr.*cosa + ur.*sina;
                     */
                    double a = angle[j][i];
                    double sinA = Math.sin(a);
                    double cosA = Math.cos(a);
                    double rotU = u[j][i] * cosA - v[j][i] * sinA;
                    double rotV = v[j][i] * cosA + u[j][i] * sinA;
                    u[j][i] = rotU;
                    v[j][i] = rotV;
                }

        }

    }

    

}
