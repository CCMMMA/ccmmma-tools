package jncregridder.data.copernicus;

/*

netcdf myoc_d00_20180205_cur {
dimensions:
	time = 6 ;
	depth = 136 ;
	lat = 380 ;
	lon = 433 ;
variables:
	int time(time) ;
		time:units = "days since 1970-01-01 0:0:0" ;
		time:calendar = "standard" ;
		time:long_name = "time" ;
		time:standard_name = "time" ;
		time:axis = "T" ;
		time:_ChunkSizes = 1 ;
		time:_CoordinateAxisType = "Time" ;
	float lon(lon) ;
		lon:units = "degrees_east" ;
		lon:nav_model = "Grid T" ;
		lon:standard_name = "longitude" ;
		lon:long_name = "longitude" ;
		lon:axis = "X" ;
		lon:_ChunkSizes = 1287 ;
		lon:_CoordinateAxisType = "Lon" ;
	float depth(depth) ;
		depth:units = "m" ;
		depth:nav_model = "Grid T" ;
		depth:positive = "down" ;
		depth:standard_name = "depth" ;
		depth:long_name = "depth" ;
		depth:axis = "Z" ;
		depth:_ChunkSizes = 141 ;
		depth:_CoordinateAxisType = "Height" ;
		depth:_CoordinateZisPositive = "down" ;
	float lat(lat) ;
		lat:units = "degrees_north" ;
		lat:nav_model = "Grid T" ;
		lat:standard_name = "latitude" ;
		lat:long_name = "latitude" ;
		lat:axis = "Y" ;
		lat:_ChunkSizes = 380 ;
		lat:_CoordinateAxisType = "Lat" ;
	float uo(time, depth, lat, lon) ;
		uo:_CoordinateAxes = "time depth lat lon time depth lat lon " ;
		uo:_FillValue = 1.e+20f ;
		uo:missing_value = 1.e+20f ;
		uo:units = "m/s" ;
		uo:coordinates = "time depth lat lon" ;
		uo:standard_name = "eastward_sea_water_velocity" ;
		uo:long_name = "zonal current" ;
		uo:_ChunkSizes = 1, 29, 76, 258 ;
	float vo(time, depth, lat, lon) ;
		vo:_CoordinateAxes = "time depth lat lon time depth lat lon " ;
		vo:_FillValue = 1.e+20f ;
		vo:missing_value = 1.e+20f ;
		vo:units = "m/s" ;
		vo:coordinates = "time depth lat lon" ;
		vo:standard_name = "northward_sea_water_velocity" ;
		vo:long_name = "meridional current" ;
		vo:_ChunkSizes = 1, 47, 127, 429 ;

// global attributes:
		:title = "Horizontal Velocity (3D) - Daily Mean" ;
		:institution = "Istituto Nazionale di Geofisica e Vulcanologia - Bologna, Italy" ;
		:references = "Please check in CMEMS catalogue the INFO section for product MEDSEA_ANALYSIS_FORECAST_PHY_006_013 - http://marine.copernicus.eu" ;
		:source = "MFS EAS2" ;
		:Conventions = "CF-1.0" ;
		:history = "Data extracted from dataset http://localhost:8080/thredds/dodsC/sv03-med-ingv-cur-an-fc-d" ;
		:time_min = 17567. ;
		:time_max = 17572. ;
		:julian_day_unit = "days since 1970-01-01 0:0:0" ;
		:z_min = 3.16574740409851 ;
		:z_max = 5328.59375 ;
		:latitude_min = 30.1875 ;
		:latitude_max = 45.9791679382324 ;
		:longitude_min = 6. ;
		:longitude_max = 24. ;
}

 */

import jncregridder.data.ICurrent;
import jncregridder.data.OceanGridEU;
import jncregridder.util.NCRegridderException;
import ucar.ma2.ArrayFloat;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Variable;

import java.io.IOException;

public class CopernicusCur extends OceanGridEU implements ICurrent {
    public double getUndefinedValue() { return 1e20; }
    private double[][][] VO = null;
    private double[][][] UO = null;

    public double[][][] getVO() throws NCRegridderException { return load(VARIABLE_VO); }
    public double[][][] getUO() throws NCRegridderException { return load(VARIABLE_UO); }

    public double[][][] getCurU() throws NCRegridderException { return getUO(); }
    public double[][][] getCurV() throws NCRegridderException { return getVO(); }

    public void setTime(int localTime)  {
        VO = null;
        UO = null;
        super.setTime(localTime);
    }

    public static final int VARIABLE_VO=21;
    public static final int VARIABLE_UO=22;



    public CopernicusCur(String url) throws IOException, NCRegridderException {
        super(url);
        load(VARIABLE_VO);
        load(VARIABLE_UO);
    }

    public double[][][] load(int varId) throws NCRegridderException {
        double[][][] result=null;

        switch (varId) {


            case VARIABLE_VO:
                if (VO==null) {
                    Variable var = ncDataset.findVariable("vo");
                    if (var!=null) {
                        try {
                            ArrayFloat.D4 a = (ArrayFloat.D4)var.read(new int[] { localTime,0,0,0 }, new int[] {1,depth,lat,lon});
                            float[][][][] fVO = (float[][][][])a.copyToNDJavaArray();
                            VO = new double[depth][lat][lon];
                            for (int k=0;k<depth;k++) {
                                for (int j=0;j<lat;j++) {
                                    for (int i=0;i<lon;i++) {
                                        VO[k][j][i]=(double)fVO[0][k][j][i];
                                    }
                                }
                            }
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable VO not found!");

                }
                result = VO;
                break;

            case VARIABLE_UO:
                if (UO==null) {
                    Variable var = ncDataset.findVariable("uo");
                    if (var!=null) {
                        try {
                            ArrayFloat.D4 a = (ArrayFloat.D4)var.read(new int[] { localTime,0,0,0 }, new int[] {1,depth,lat,lon});
                            float[][][][] fUO = (float[][][][])a.copyToNDJavaArray();
                            UO = new double[depth][lat][lon];
                            for (int k=0;k<depth;k++) {
                                for (int j=0;j<lat;j++) {
                                    for (int i=0;i<lon;i++) {
                                        UO[k][j][i]=(double)fUO[0][k][j][i];
                                    }
                                }
                            }
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable UO not found!");

                }
                result = UO;
                break;



        }

        if (result==null) {
            result = super.load(varId);
        }
        return result;
    }

}
