package jncregridder.data;

import jncregridder.util.NCRegridderException;

public interface IPotentialTemperature extends IOceanGrid {
    public double[][][] getTemp() throws NCRegridderException;
    public double[][] getTemp(int depth) throws NCRegridderException;
}
