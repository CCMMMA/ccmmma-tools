/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jncregridder.etopo;


import java.io.IOException;
import jncregridder.util.IdDoubleVectData;
import jncregridder.util.Stations;
import jncregridder.util.Station;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayInt;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;

/**
 *
 * @author raffaelemontella
 */
public class ETOPOGrid {
    
    double[] lats = null;
    double[] lons = null;
    
    Dimension dimX = null;
    Dimension dimY = null;
    
    private double[][] LAT = null;
    private double[][] LON = null;
    private double[][] Z = null;
    
    public double[][] getLAT() { return LAT; }
    public double[][] getLON() { return LON; }
    public double[][] getZ() { return Z; }
    
    
    private NetcdfDataset ncdBathymetry = null;
    
    public ETOPOGrid(String url) throws IOException, InvalidRangeException {
        
        ncdBathymetry = NetcdfDataset.openDataset(url);

        
        dimX = ncdBathymetry.findDimension("x");
        dimY = ncdBathymetry.findDimension("y");
        Variable varX = ncdBathymetry.findVariable("x");
        Variable varY = ncdBathymetry.findVariable("y");
        
        ArrayDouble.D1 aY = (ArrayDouble.D1) varY.read(new int[] {0}, new int[] {dimY.getLength()});
        ArrayDouble.D1 aX = (ArrayDouble.D1) varX.read(new int[] {0}, new int[] {dimX.getLength()});
        lats = (double[]) aY.copyTo1DJavaArray();
        lons = (double[]) aX.copyTo1DJavaArray();
        
        
        
        /*
        int i,j;
        
        j=srcMinJ;
        while (lats[j]<llY) { j++; }
        srcMinJ=j-2;
        if (srcMinJ<0) srcMinJ=0;
        
        j=srcMaxJ-1;
        while (lats[j]>urY) { j--; }
        srcMaxJ=j+2;
        if (srcMaxJ>(dimY.getLength()-1)) srcMaxJ=dimX.getLength()-1;
        
        i=srcMinI;
        while (longs[i]<llX) { i++; }
        srcMinI=i-2;
        if (srcMinI<0) srcMinI=0;
        
        i=srcMaxI-1;
        while (longs[i]>urX) { i--; }
        srcMaxI=i+2;
        if (srcMaxI>(dimX.getLength()-1)) srcMaxJ=dimX.getLength()-1;
        
        int srcCols = srcMaxI-srcMinI;
        int srcRows = srcMaxJ-srcMinJ;
        
        System.out.println("Src dims: "+srcRows+","+srcCols);
        System.out.println("Src subset: "+srcMinJ+" "+srcMaxJ+", "+srcMinI+" "+srcMaxI);
        
        double[][] srcLAT = new double [srcRows][srcCols];
        double[][] srcLONG = new double [srcRows][srcCols];
        
        for (j=0;j<srcRows;j++) {
            for (i=0;i<srcCols;i++) {
                srcLAT[j][i]=lats[srcMinJ+j];
                srcLONG[j][i]=longs[srcMinI+i];
            }
        }
        
        System.out.println("src:"+ srcLAT[0][0]+","+srcLONG[0][0]+" - "+ srcLAT[srcRows-1][srcCols-1]+","+srcLONG[srcRows-1][srcCols-1]);
        System.out.println("dst:"+ dstLAT[0][0]+","+dstLONG[0][0]+" - "+ dstLAT[dstRows-1][dstCols-1]+","+dstLONG[dstRows-1][dstCols-1]);
        
        Interpolator interpolator = new Interpolator(srcLAT,srcLONG,dstLAT,dstLONG,true);
        
        
        
        Variable varZ = ncdBathymetry.findVariable("z");
        
        ArrayFloat.D2 aZ = (ArrayFloat.D2)varZ.read(new int[] { srcMinJ,srcMinI}, new int[] {srcRows,srcCols});
        float[][] tmp = (float[][])aZ.copyToNDJavaArray();
        double[][] src = new double[srcRows][srcCols];
        for (j=0;j<srcRows;j++) {
            for (i=0;i<srcCols;i++) {
                src[j][i]=(double)tmp[j][i];
            }
        }
        
        data=interpolator.interp(src);

        for (j=0;j<dstRows; j++) {
            for (i=0;i<dstCols; i++) {
                if (j==0 && i==0) {
                    min = data[0][0];
                    max = min;
                } else {
                    min = Math.min(min, data[j][i]);
                    max = Math.max(max, data[j][i]);
                }
            }
        }

        */
    }

    public void subSet(double minLon, double minLat, double maxLon, double maxLat) throws IOException, InvalidRangeException {
        Stations result=null;
        
        int srcMinJ=0;
        int srcMinI=0;
        int srcMaxJ=dimY.getLength();
        int srcMaxI=dimX.getLength();
        
        int i,j;
        
        j=srcMinJ;
        while (lats[j]<minLat) { j++; }
        srcMinJ=j-2;
        if (srcMinJ<0) srcMinJ=0;
        
        j=srcMaxJ-1;
        while (lats[j]>maxLat) { j--; }
        srcMaxJ=j+2;
        if (srcMaxJ>(dimY.getLength()-1)) srcMaxJ=dimX.getLength()-1;
        
        i=srcMinI;
        while (lons[i]<minLon) { i++; }
        srcMinI=i-2;
        if (srcMinI<0) srcMinI=0;
        
        i=srcMaxI-1;
        while (lons[i]>maxLon) { i--; }
        srcMaxI=i+2;
        if (srcMaxI>(dimX.getLength()-1)) srcMaxJ=dimX.getLength()-1;
        
        int srcCols = srcMaxI-srcMinI;
        int srcRows = srcMaxJ-srcMinJ;
        
        System.out.println("Src dims: "+srcRows+","+srcCols);
        System.out.println("Src subset: "+srcMinJ+" "+srcMaxJ+", "+srcMinI+" "+srcMaxI);
        
        LAT = new double [srcRows][srcCols];
        LON = new double [srcRows][srcCols];
        
        Z = new double [srcRows][srcCols];
        
        for (j=0;j<srcRows;j++) {
            for (i=0;i<srcCols;i++) {
                LAT[j][i]=lats[srcMinJ+j];
                LON[j][i]=lons[srcMinI+i];
            }
        }
        
        Variable varZ = ncdBathymetry.findVariable("z");
        
        ArrayInt.D2 aZ = (ArrayInt.D2)varZ.read(new int[] { srcMinJ,srcMinI}, new int[] {srcRows,srcCols});
        int[][] tmp = (int[][])aZ.copyToNDJavaArray();
        double[][] src = new double[srcRows][srcCols];
        for (j=0;j<srcRows;j++) {
            for (i=0;i<srcCols;i++) {
                Z[j][i]=(double)tmp[j][i];
                
            }
        }
        
        
    }

    public void fillStations(double minLon, double minLat, double maxLon, double maxLat, Stations stations, IdDoubleVectData values  ) throws IOException, InvalidRangeException {
        
        
        subSet(minLon,minLat,maxLon,maxLat);
        int id=0;
        double[] z=null;
        for (int j=0;j<LAT.length;j++) {
            for (int i=0;i<LON[0].length;i++) {
                stations.add(new Station(id, LON[j][i], LAT[j][i]));
                z= new double[1];
                z[0]= Z[j][i];
                values.put(id,z);
                id++;
            }
        }
        
    }
    
}
