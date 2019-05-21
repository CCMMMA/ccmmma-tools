/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jncregridder.data.myocean;

import java.io.IOException;

import jncregridder.data.ICurrent;
import jncregridder.data.OceanGridEU;
import jncregridder.util.NCRegridderException;
import ucar.ma2.ArrayFloat;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Variable;

/**
 *
 * @author raffaelemontella
 */
public class MyOceanCur extends OceanGridEU implements ICurrent {

    public double getUndefinedValue() { return 1e20; }
    
    private double[][] SOMESTDY = null;
    private double[][] SOZOSDX1 = null;
    private double[][][] VOMECRTY = null;
    private double[][][] VOZOCRTX = null;
    
     public void setTime(int localTime)  {
        SOMESTDY = null;
        SOZOSDX1 = null;
        VOMECRTY = null;
        VOZOCRTX = null;
        super.setTime(localTime);
    }


    
    public double[][] getSOMESTDY() throws NCRegridderException { return load(VARIABLE_SOMESTDY)[0]; }
    public double[][] getSOZOSDX1() throws NCRegridderException { return load(VARIABLE_SOZOSDX1)[0]; }
    public double[][][] getVOMECRTY() throws NCRegridderException { return load(VARIABLE_VOMECRTY); }
    public double[][][] getVOZOCRTX() throws NCRegridderException { return load(VARIABLE_VOZOCRTX); }
    public double[][] getVOMECRTY(int depth) throws NCRegridderException { return load(VARIABLE_VOMECRTY)[depth];}
    public double[][] getVOZOCRTX(int depth) throws NCRegridderException { return load(VARIABLE_VOZOCRTX)[depth];}

    public double[][][] getCurU() throws NCRegridderException { return getVOMECRTY(); }
    public double[][][] getCurV() throws NCRegridderException { return getVOZOCRTX(); }
    
    public static final int VARIABLE_SOMESTDY=21;
    public static final int VARIABLE_SOZOSDX1=22;
    public static final int VARIABLE_VOMECRTY=23;
    public static final int VARIABLE_VOZOCRTX=24;
    
    public MyOceanCur(String url) throws IOException, NCRegridderException {
        super(url);
        load(VARIABLE_SOMESTDY);
        load(VARIABLE_SOZOSDX1);
        load(VARIABLE_VOMECRTY);
        load(VARIABLE_VOZOCRTX);
    }
    
    public double[][][] load(int varId) throws NCRegridderException {
        double[][][] result=null;

        switch (varId) {
            
            
            case VARIABLE_VOMECRTY:
                if (VOMECRTY==null) {
                    Variable var = ncDataset.findVariable("vomecrty");
                    if (var!=null) {
                        try {
                            ArrayFloat.D4 a = (ArrayFloat.D4)var.read(new int[] { localTime,0,0,0 }, new int[] {1,depth,lat,lon});
                            float[][][][] fVOMECRTY = (float[][][][])a.copyToNDJavaArray();
                            VOMECRTY = new double[depth][lat][lon];
                            for (int k=0;k<depth;k++) {
                                for (int j=0;j<lat;j++) {
                                    for (int i=0;i<lon;i++) {
                                        VOMECRTY[k][j][i]=(double)fVOMECRTY[0][k][j][i];
                                    }
                                }
                            }
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable VOMECRTY not found!");

                }
                result = VOMECRTY;
                break;
            
            case VARIABLE_VOZOCRTX:
                if (VOZOCRTX==null) {
                    Variable var = ncDataset.findVariable("vozocrtx");
                    if (var!=null) {
                        try {
                            ArrayFloat.D4 a = (ArrayFloat.D4)var.read(new int[] { localTime,0,0,0 }, new int[] {1,depth,lat,lon});
                            float[][][][] fVOZOCRTX = (float[][][][])a.copyToNDJavaArray();
                            VOZOCRTX = new double[depth][lat][lon];
                            for (int k=0;k<depth;k++) {
                                for (int j=0;j<lat;j++) {
                                    for (int i=0;i<lon;i++) {
                                        VOZOCRTX[k][j][i]=(double)fVOZOCRTX[0][k][j][i];
                                    }
                                }
                            }
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable VOZOCRTX not found!");

                }
                result = VOZOCRTX;
                break;
                
                
            case VARIABLE_SOMESTDY:
                if (SOMESTDY==null) {
                    Variable var = ncDataset.findVariable("somestdy");
                    if (var!=null) {
                        try {
                            ArrayFloat.D3 a = (ArrayFloat.D3)var.read(new int[] { localTime,0,0 }, new int[] {1,lat,lon});
                            float[][][] fSOMESTDY = (float[][][])a.copyToNDJavaArray();
                            SOMESTDY = new double[lat][lon];
                            for (int j=0;j<lat;j++) {
                                for (int i=0;i<lon;i++) {
                                    SOMESTDY[j][i]=(double)fSOMESTDY[0][j][i];
                                }
                            }
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable SOMESTDY not found!");

                }
                result = new double[1][1][1];
                result[0] = SOMESTDY;
                break;
                
            case VARIABLE_SOZOSDX1:
                if (SOZOSDX1==null) {
                    Variable var = ncDataset.findVariable("sozostdx");
                    if (var!=null) {
                        try {
                            ArrayFloat.D3 a = (ArrayFloat.D3)var.read(new int[] { localTime,0,0 }, new int[] {1,lat,lon});
                            float[][][] fSOZOSDX1 = (float[][][])a.copyToNDJavaArray();
                            SOZOSDX1 = new double[lat][lon];
                            for (int j=0;j<lat;j++) {
                                for (int i=0;i<lon;i++) {
                                    SOZOSDX1[j][i]=(double)fSOZOSDX1[0][j][i];
                                }
                            }
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable SOZOSDX1 not found!");

                }
                result = new double[1][1][1];
                result[0] = SOZOSDX1;
                break;
        }
        
        if (result==null) {
            result = super.load(varId);
        }
        return result;
    }
    
    
}
