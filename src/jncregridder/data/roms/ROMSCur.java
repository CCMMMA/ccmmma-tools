package jncregridder.data.roms;

import jncregridder.data.ICurrent;
import jncregridder.data.OceanGridEU;
import jncregridder.roms.ROMSHistory;
import jncregridder.util.NCRegridderException;
import ucar.ma2.ArrayFloat;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Variable;

import java.io.IOException;

public class ROMSCur implements ICurrent {
    private ROMSHistory romsHistory;

    public ROMSCur(String url) throws IOException, NCRegridderException {
        romsHistory=new ROMSHistory(url);
    }

    @Override
    public double[][][] getCurV() throws NCRegridderException {
        return new double[0][][];
    }

    @Override
    public double[][][] getCurU() throws NCRegridderException {
        return new double[0][][];
    }

    @Override
    public double[] getLAT() throws NCRegridderException {
        return new double[0];
    }

    @Override
    public double[] getLON() throws NCRegridderException {
        return new double[0];
    }

    @Override
    public double[] getDEPTH() throws NCRegridderException {
        return new double[0];
    }

    @Override
    public double[] getTIME() throws NCRegridderException {
        return new double[0];
    }

    @Override
    public double[][] getLAT2() throws NCRegridderException {
        return new double[0][];
    }

    @Override
    public double[][] getLON2() throws NCRegridderException {
        return new double[0][];
    }

    @Override
    public double[][][] getZ() throws NCRegridderException {
        return new double[0][][];
    }

    @Override
    public void setTime(int localTime) {

    }
}
