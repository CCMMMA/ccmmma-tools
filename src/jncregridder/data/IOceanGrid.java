package jncregridder.data;

import jncregridder.util.NCRegridderException;

public interface IOceanGrid {
    public double[] getLAT() throws NCRegridderException;
    public double[] getLON() throws NCRegridderException;
    public double[] getDEPTH() throws NCRegridderException;
    public double[] getTIME() throws NCRegridderException;
    public double[][] getLAT2() throws NCRegridderException;
    public double[][] getLON2() throws NCRegridderException;
    public double [][][] getZ() throws NCRegridderException;
    public void setTime(int localTime);
    public double getUndefinedValue();
}
