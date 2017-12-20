/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jncregridder.util;

import java.io.Serializable;

/**
 *
 * @author raffaelemontella
 */
public class Dst2Src  implements Serializable {
    private Integer[][] iS=null;
    private Integer[][] jS=null;

    public Dst2Src(int jN,int iN) {
        iS = new Integer[jN][iN];
        jS = new Integer[jN][iN];
        for (int j=0;j<jN;j++) {
            for (int i=0;i<iN;i++) {
                iS[j][i]=-1;
                jS[j][i]=-1;
            }
        }
    }
    
    public int[] getJI(int dstJ, int dstI) {
        int[] result = new int[2];
        result[0] = jS[dstJ][dstI];
        result[1] = iS[dstJ][dstI];
        return result;
    }

    public void setJI(int dstJ, int dstI, int srcJ, int srcI) {
        jS[dstJ][dstI]=srcJ;
        iS[dstJ][dstI]=srcI;
    }

    public void setInvalidJI(int dstJ, int dstI) {
        jS[dstJ][dstI]=-1;
        iS[dstJ][dstI]=-1;
    }

    public boolean isInvalidJI(int dstJ, int dstI) {
        if (jS[dstJ][dstI]==-1 || iS[dstJ][dstI]==-1) return true;
        return false;
    }
    
    public boolean isValidJI(int dstJ, int dstI) {
        if (jS[dstJ][dstI]!=-1 && iS[dstJ][dstI]!=-1) return true;
        return false;
    }

    public double getValueJI(double[][] src, int[][] srcMask, int dstJ, int dstI) {
        double result=Double.NaN;
        if (isValidJI(dstJ, dstI)==true) {
            if (srcMask[jS[dstJ][dstI]][iS[dstJ][dstI]]==1) {
                result=src[jS[dstJ][dstI]][iS[dstJ][dstI]];
            }
        }
        return result;
    }
}
