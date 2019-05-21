/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jncregridder.data.myocean;
import java.io.IOException;

import jncregridder.data.IPotentialTemperature;
import jncregridder.data.OceanGridEU;
import jncregridder.util.NCRegridderException;
import ucar.ma2.ArrayFloat;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Variable;
/**
 *
 * @author Diana Di Luccio
 */
public class MyOceanTem extends OceanGridEU implements IPotentialTemperature {
    public double getUndefinedValue() { return 1e20; }

   private double[][][] VOTEMPER = null;
   
    public void setTime(int localTime)  {
        VOTEMPER = null;
        super.setTime(localTime);
    }
    
    
    
    public double[][][] getVOTEMPER() throws NCRegridderException { return load(VARIABLE_VOTEMPER); }
    public double[][] getVOTEMPER(int depth) throws NCRegridderException { return load(VARIABLE_VOTEMPER)[depth];}

    public double[][][] getTemp() throws NCRegridderException { return getVOTEMPER(); }
    public double[][] getTemp(int depth) throws NCRegridderException { return getVOTEMPER(depth); }

    public static final int VARIABLE_VOTEMPER=25;
    
    
    public MyOceanTem(String url) throws IOException, NCRegridderException {
        super(url);
        load(VARIABLE_VOTEMPER);
        
    }
    
    public double[][][] load(int varId) throws NCRegridderException {
        double[][][] result=null;

        switch (varId) {
            
            
            case VARIABLE_VOTEMPER:
                if (VOTEMPER==null) {
                    Variable var = ncDataset.findVariable("votemper");
                    if (var!=null) {
                        try {
                            ArrayFloat.D4 a = (ArrayFloat.D4)var.read(new int[] { localTime,0,0,0 }, new int[] {1,depth,lat,lon});
                            float[][][][] fVOTEMPER = (float[][][][])a.copyToNDJavaArray();
                            VOTEMPER = new double[depth][lat][lon];
                            for (int k=0;k<depth;k++) {
                                for (int j=0;j<lat;j++) {
                                    for (int i=0;i<lon;i++) {
                                        VOTEMPER[k][j][i]=(double)fVOTEMPER[0][k][j][i];
                                    }
                                }
                            }
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable VOTEMPER not found!");

                }
                result = VOTEMPER;
                break;
            
        }
        
        if (result==null) {
            result = super.load(varId);
        }
        return result;
    }
    
    
}
