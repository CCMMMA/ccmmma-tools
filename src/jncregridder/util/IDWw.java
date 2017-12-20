/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jncregridder.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;

/**
 *
 * @author raffaelemontella
 */
public class IDWw  implements Serializable {
    private Double[][] wRow = null;
    private Integer[][] iRow = null;
    private int nDstStations = 0;
    
    public IDWw(int nDstStations) {
        this.nDstStations = nDstStations;
        wRow = new Double[nDstStations][];
        iRow = new Integer[nDstStations][];
    }
   
    public void setRow(int j, double[] wRow0) {
        
        int nRow0=0;
        for (int iRow0=0;iRow0<wRow0.length;iRow0++) {
            if (wRow0[iRow0]>0) {
                nRow0++;
            }
        }
        
        Double[]  weightRow = new Double[nRow0];
        Integer[] indexRow = new Integer[nRow0];
        
        int iRow1=0;
        for (int iRow0=0;iRow0<wRow0.length;iRow0++) {
            if (wRow0[iRow0]>0) {
                weightRow[iRow1]=wRow0[iRow0];
                indexRow[iRow1]=iRow0;
                iRow1++;
            }
        }
        wRow[j]=weightRow;
        iRow[j]=indexRow;
    }
    
    public Double[] getWeights(int j) {
        return wRow[j];
    }
    
    public Integer[] getIndexes(int j) {
        return iRow[j];
    }
}
