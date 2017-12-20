/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.uniparthenope.meteo;

import java.io.IOException;
import jncregridder.etopo.ETOPOGrid;
import jncregridder.roms.ROMSGrid;
import jncregridder.util.IDWInterpolator;
import jncregridder.util.IdDoubleVectData;
import jncregridder.util.InterpolatorException;
import jncregridder.util.NCRegridderException;
import jncregridder.util.Stations;
import ucar.ma2.InvalidRangeException;

/**
 *
 * @author raffaelemontella
 */
public class Grid2ROMS {
    public static void main(String[] args) throws IOException, InvalidRangeException, InterpolatorException, NCRegridderException {
        new Grid2ROMS();
    }
    
    public Grid2ROMS() throws IOException, InvalidRangeException, InterpolatorException, NCRegridderException {
        String gridFilename="/Users/raffaelemontella/tmp/myocean2roms/roms/roms-grid-d04b.nc";
        String etopoFilename="/Users/raffaelemontella/tmp/myocean2roms/seafloor/ETOPO1_Bed_g_gmt4.nc";
        //ROMSGrid romsGrid = new ROMSGrid(350,200,gridFilename,12.50,39.50,0.01,0.01);
        ROMSGrid romsGrid = new ROMSGrid(gridFilename,12.50,39.50,(350*0.001)+12.50,(200*0.001)+39.50,0.001,0.001);
        ETOPOGrid etopoGrid = new ETOPOGrid(etopoFilename);
        etopoGrid.subSet(12,39,17,42);
        IDWInterpolator idwInterpolator = new IDWInterpolator(etopoGrid.getLAT(), etopoGrid.getLON(), romsGrid.getLATRHO(), romsGrid.getLONRHO(), null, null);
        double[][] h = idwInterpolator.interp(etopoGrid.getZ(), 1e+37, 1e+37,null);
        romsGrid.H=h;
        for(int j=0;j<romsGrid.LATRHO.length;j++) {
            for (int i=0;i<romsGrid.LONRHO[0].length;i++) {
                if (romsGrid.H[j][i]>=0) {
                    romsGrid.MASKRHO[j][i]=0;
                    romsGrid.H[j][i]=1e+37;
                } else {
                    romsGrid.MASKRHO[j][i]=1;
                    romsGrid.H[j][i]=Math.abs(romsGrid.H[j][i]);
                }
            }
        }
        
        
        romsGrid.make();
    }
    
}
