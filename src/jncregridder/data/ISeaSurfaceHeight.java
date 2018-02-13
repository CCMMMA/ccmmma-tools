package jncregridder.data;

import jncregridder.util.NCRegridderException;

public interface ISeaSurfaceHeight extends IOceanGrid{
    public double[][] getZeta() throws NCRegridderException;
}
