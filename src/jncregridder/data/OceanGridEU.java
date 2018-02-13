package jncregridder.data;

import jncregridder.util.NCRegridderException;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayInt;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;

import java.io.IOException;

public class OceanGridEU implements IOceanGrid{
    private String url;
    protected NetcdfDataset ncDataset;

    public Dimension dimTime;
    public Dimension dimLat;
    public Dimension dimLon;
    public Dimension dimDepth;

    private double[] LAT = null;
    private double[] LON = null;
    private double[] TIME = null;
    private double[] DEPTH = null;

    private double[][] LAT2 = null;
    private double[][] LON2 = null;

    protected int lat = -1;
    protected int lon = -1;
    protected int time = -1;
    protected int depth = -1;

    public double[] getLAT() throws NCRegridderException { return load(VARIABLE_LAT)[0][0]; }
    public double[] getLON() throws NCRegridderException { return load(VARIABLE_LON)[0][0]; }
    public double[] getDEPTH() throws NCRegridderException { return load(VARIABLE_DEPTH)[0][0]; }
    public double[] getTIME() throws NCRegridderException { return load(VARIABLE_TIME)[0][0]; }

    public double[][] getLAT2() throws NCRegridderException { return load(VARIABLE_LAT2)[0]; }
    public double[][] getLON2() throws NCRegridderException { return load(VARIABLE_LON2)[0]; }

    public double[][][] getZ() throws NCRegridderException {
        double[][][] Z = new double[depth][lat][lon];
        load(VARIABLE_DEPTH);
        for (int k=0;k<depth;k++) {
            for (int j=0;j<lat;j++) {
                for (int i=0;i<lon;i++) {
                    Z[k][j][i] = DEPTH[k];
                }
            }
        }
        return Z;

    }


    public static final int VARIABLE_LAT=11;
    public static final int VARIABLE_LON=12;
    public static final int VARIABLE_TIME=13;
    public static final int VARIABLE_DEPTH=14;
    public static final int VARIABLE_LAT2=15;
    public static final int VARIABLE_LON2=16;

    private boolean b3D=false;
    public boolean is3D() { return b3D; }

    protected int localTime=0;
    public void setTime(int localTime)  {
        this.localTime = localTime;
    }

    public OceanGridEU(String url) throws IOException, NCRegridderException {


        ncDataset = NetcdfDataset.openDataset(url);

        dimTime = ncDataset.findDimension("time");
        dimLat = ncDataset.findDimension("lat");
        dimLon = ncDataset.findDimension("lon");
        dimDepth = ncDataset.findDimension("depth");
        if (dimDepth!=null) {
            b3D = true;
        }

        time = dimTime.getLength();
        lat = dimLat.getLength();
        lon = dimLon.getLength();


        load(VARIABLE_LAT);
        load(VARIABLE_LON);
        load(VARIABLE_TIME);
        load(VARIABLE_LAT2);
        load(VARIABLE_LON2);

        if (is3D()) {
            depth = dimDepth.getLength();
            load(VARIABLE_DEPTH);
        }

    }

    public double[][][] load(int varId) throws NCRegridderException {
        double[][][] result=null;

        switch (varId) {
            case VARIABLE_LAT:
                if (LAT==null) {
                    Variable var = ncDataset.findVariable("lat");
                    if (var!=null) {
                        try {
                            ArrayFloat.D1 a = (ArrayFloat.D1)var.read(new int[] { 0 }, new int[] {lat});
                            float[] fLAT = (float[])a.copyToNDJavaArray();
                            LAT = new double[fLAT.length];
                            for (int i=0;i<fLAT.length;i++) {
                                LAT[i]=(double)fLAT[i];
                            }
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable LAT not found!");

                }
                result = new double[1][1][1];
                result[0][0] = LAT;
                break;

            case VARIABLE_LON:
                if (LON==null) {
                    Variable var = ncDataset.findVariable("lon");
                    if (var!=null) {
                        try {
                            ArrayFloat.D1 a = (ArrayFloat.D1)var.read(new int[] { 0 }, new int[] {lon});
                            float[] fLON = (float[])a.copyToNDJavaArray();
                            LON = new double[fLON.length];
                            for (int i=0;i<fLON.length;i++) {
                                LON[i]=(double)fLON[i];
                            }
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable LON not found!");

                }
                result = new double[1][1][1];
                result[0][0] = LON;
                break;

            case VARIABLE_TIME:
                if (TIME==null) {
                    Variable var = ncDataset.findVariable("time");
                    if (var!=null) {
                        try {
                            ArrayInt.D1 a = (ArrayInt.D1)var.read(new int[] { 0 }, new int[] {time});
                            int[] nTIME = (int[])a.copyToNDJavaArray();
                            TIME = new double[nTIME.length];
                            for (int i=0;i<nTIME.length;i++) {
                                TIME[i]=(double)nTIME[i];
                            }
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable TIME not found!");

                }
                result = new double[1][1][1];
                result[0][0] = TIME;
                break;

            case VARIABLE_DEPTH:
                if (DEPTH==null) {
                    Variable var = ncDataset.findVariable("depth");
                    if (var!=null) {
                        try {
                            ArrayFloat.D1 a = (ArrayFloat.D1)var.read(new int[] { 0 }, new int[] {depth});
                            float[] fDEPTH = (float[])a.copyToNDJavaArray();
                            DEPTH = new double[fDEPTH.length];
                            for (int i=0;i<fDEPTH.length;i++) {
                                DEPTH[i]=(double)fDEPTH[i];
                            }
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable DEPTH not found!");

                }
                result = new double[1][1][1];
                result[0][0] = DEPTH;
                break;

            case VARIABLE_LAT2:
                if (LAT2==null) {
                    load(VARIABLE_LAT);
                    LAT2 = new double[lat][lon];
                    for (int j=0;j<lat;j++) {
                        for (int i=0;i<lon;i++) {
                            LAT2[j][i]=LAT[j];
                        }
                    }
                }
                result = new double[1][1][1];
                result[0] = LAT2;
                break;

            case VARIABLE_LON2:
                if (LON2==null) {
                    load(VARIABLE_LON);
                    LON2 = new double[lat][lon];
                    for (int j=0;j<lat;j++) {
                        for (int i=0;i<lon;i++) {
                            LON2[j][i]=LON[i];
                        }
                    }
                }
                result = new double[1][1][1];
                result[0] = LON2;
                break;
        }
        if (result==null) throw new NCRegridderException("Unknown variable to load! (varId="+varId+")");
        return result;
    }

}
