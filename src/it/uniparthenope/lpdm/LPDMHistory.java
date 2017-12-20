/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.uniparthenope.lpdm;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import jncregridder.util.JulianDate;
import jncregridder.util.NCRegridderException;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;

/**
 *
 * @author raffaelemontella
 */
public class LPDMHistory {
    private String url;
    private NetcdfDataset ncDataset;
    
    public Dimension dimEtaRho;
    public Dimension dimXiRho;
    public Dimension dimSRho;
    public Dimension dimTime;
    
    int etaRho = -1;
    int xiRho = -1;
    int sRho = -1;
    
    private double[] s_rho = null;
    private double[] cs_r = null;
    
    public double[][][] CONC = null;
    
    private int localTime=0;
    private double oceanTime=0;
    
    public double[][][] getCONC() throws NCRegridderException { return load(VARIABLE_CONC); }
    
    public LPDMHistory(String url) throws IOException, NCRegridderException, InvalidRangeException {


        ncDataset = NetcdfDataset.openDataset(url);

        dimSRho = ncDataset.findDimension("s_rho");
        dimEtaRho = ncDataset.findDimension("eta_rho");
        dimXiRho = ncDataset.findDimension("xi_rho");
        dimTime = ncDataset.findDimension("ocean_time");

        
        sRho = dimSRho.getLength();
        etaRho = dimEtaRho.getLength();
        xiRho = dimXiRho.getLength();
        
        /*
        
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

        */
        
        
    }
    
    public static final int VARIABLE_CONC=11;
    
    
    private GregorianCalendar gcLocalTime = null;
    public Calendar getTimeAsCalendar() {
        return gcLocalTime;
    }
    
    
    public void setTime(int localTime) throws NCRegridderException  {

        this.localTime = localTime;
        CONC=null;
        /*
        Variable var = ncDataset.findVariable("ocean_time");
        if (var!=null) {
            try {
                ArrayDouble.D1 a = (ArrayDouble.D1)var.read(new int[] { localTime }, new int[] {1});
                oceanTime = ((double[])a.copyToNDJavaArray())[0];
                
                double julianOceanTime=oceanTime/86400+JulianDate.get19680523();
                int[] ymdh=JulianDate.fromJulian(julianOceanTime);
                
                gcLocalTime = new GregorianCalendar(ymdh[0], ymdh[1]-1, ymdh[2], ymdh[3], 0);
                
                CONC=null;
                
        
            } catch (IOException ex) {
                throw new NCRegridderException(ex);
            } catch (InvalidRangeException ex) {
                throw new NCRegridderException(ex);
            }
        } else throw new NCRegridderException("Variable ocean_time not found!");
        */
        
        
    }
    
    public double[][][] load(int varId) throws NCRegridderException {
        double[][][] result=null;

        switch (varId) {
             
            case VARIABLE_CONC:
                if (CONC==null) {
                    Variable var = ncDataset.findVariable("conc");
                    if (var!=null) {
                        try {
                            CONC=new double[sRho][etaRho][xiRho];
                            ArrayFloat.D4 a = (ArrayFloat.D4)var.read(new int[] { localTime,0,0, 0 }, new int[] {1,sRho,etaRho,xiRho});
                            
                            for (int k=0;k<sRho;k++) {
                                for (int j=0;j<etaRho; j++) {
                                    for (int i=0;i<xiRho; i++) {
                                        CONC[k][j][i] = a.get(0,k,j,i);
                                    }
                                }
                            }
                            
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable CONC not found!");

                }
                result = new double[1][1][1];
                result = CONC;
                break;
                
            
                

        }
        if (result==null) throw new NCRegridderException("Unknown variable to load! (varId="+varId+")");
        return result;
    }
    
    
}
