/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jncregridder.data.myocean;
import java.io.IOException;

import jncregridder.data.ISalinity;
import jncregridder.data.OceanGridEU;
import jncregridder.util.NCRegridderException;
import ucar.ma2.ArrayFloat;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Variable;
/**
 *
 * @author Diana Di Luccio
 */
public class MyOceanSal extends OceanGridEU implements ISalinity {
   private double[][][] VOSALINE = null;
   
   public void setTime(int localTime)  {
        VOSALINE = null;
        super.setTime(localTime);
    }
    
    
    
    public double[][][] getVOSALINE() throws NCRegridderException { return load(VARIABLE_VOSALINE); }
    public double[][] getVOSALINE(int depth) throws NCRegridderException { return load(VARIABLE_VOSALINE)[depth];}

    public double[][][] getSalt() throws NCRegridderException { return getVOSALINE(); }
    public double[][] getSalt(int depth) throws NCRegridderException { return getVOSALINE(depth); }
    
    
    public static final int VARIABLE_VOSALINE=26;
    
    
    public MyOceanSal(String url) throws IOException, NCRegridderException {
        super(url);
        load(VARIABLE_VOSALINE);
        
    }
    
    public double[][][] load(int varId) throws NCRegridderException {
        double[][][] result=null;

        switch (varId) {
            
            
            case VARIABLE_VOSALINE:
                if (VOSALINE==null) {
                    Variable var = ncDataset.findVariable("vosaline");
                    if (var!=null) {
                        try {
                            ArrayFloat.D4 a = (ArrayFloat.D4)var.read(new int[] { localTime,0,0,0 }, new int[] {1,depth,lat,lon});
                            float[][][][] fVOSALINE = (float[][][][])a.copyToNDJavaArray();
                            VOSALINE = new double[depth][lat][lon];
                            for (int k=0;k<depth;k++) {
                                for (int j=0;j<lat;j++) {
                                    for (int i=0;i<lon;i++) {
                                        VOSALINE[k][j][i]=(double)fVOSALINE[0][k][j][i];
                                    }
                                }
                            }
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable VOSALINE not found!");

                }
                result = VOSALINE;
                break;
            
        }
        
        if (result==null) {
            result = super.load(varId);
        }
        return result;
    }
    
    
}
