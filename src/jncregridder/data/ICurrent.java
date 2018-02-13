package jncregridder.data;

import jncregridder.util.NCRegridderException;

public interface ICurrent extends IOceanGrid {
    public double[][][] getCurV() throws NCRegridderException;
    public double[][][] getCurU() throws NCRegridderException;
}
