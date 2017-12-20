
package jncregridder.myocean;

import java.io.IOException;
import jncregridder.util.NCRegridderException;
import ucar.ma2.ArrayFloat;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Variable;

public class MyOceanSSH extends MyOceanGrid {
    
    private double[][] SOSSHEIG = null;  
    
     public void setTime(int localTime)  {
        SOSSHEIG = null;
        super.setTime(localTime);
    }
    
    public double[][] getSOSSHEIG() throws NCRegridderException { return load(VARIABLE_SOSSHEIG)[0]; }
    public static final int VARIABLE_SOSSHEIG=27;
    
    public MyOceanSSH(String url) throws IOException, NCRegridderException {
        super(url);
        load(VARIABLE_SOSSHEIG);    
    }
    
    public double[][][] load(int varId) throws NCRegridderException {
        double[][][] result=null;

        switch (varId) {

            case VARIABLE_SOSSHEIG:
                if (SOSSHEIG==null) {
                    Variable var = ncDataset.findVariable("sossheig");
                    if (var!=null) {
                        try {
                            ArrayFloat.D3 a = (ArrayFloat.D3)var.read(new int[] { localTime,0,0 }, new int[] {1,lat,lon});
                            float[][][] fSOSSHEIG = (float[][][])a.copyToNDJavaArray();
                            SOSSHEIG = new double[lat][lon];
                            for (int j=0;j<lat;j++) {
                                for (int i=0;i<lon;i++) {
                                    SOSSHEIG[j][i]=(double)fSOSSHEIG[0][j][i];
                                }
                            }
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable SOSSHEIG not found!");

                }
                result = new double[1][1][1];
                result[0] = SOSSHEIG;
                break;
        }
        
        if (result==null) {
            result = super.load(varId);
        }
        return result;
    }
 
}
