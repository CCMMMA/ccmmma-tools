package jncregridder.data;

import jncregridder.util.NCRegridderException;

public interface ISalinity extends IOceanGrid{
    public double[][][] getSalt() throws NCRegridderException;
    public double[][] getSalt(int depth) throws NCRegridderException;
}
