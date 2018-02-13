package jncregridder.data.copernicus;

import jncregridder.data.ISalinity;
import jncregridder.data.OceanGridEU;
import jncregridder.util.NCRegridderException;
import ucar.ma2.ArrayFloat;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Variable;

import java.io.IOException;

/*

netcdf myoc_d00_20180205_sal {
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
	float so(time, depth, lat, lon) ;
		so:_CoordinateAxes = "time depth lat lon time depth lat lon " ;
		so:_FillValue = 1.e+20f ;
		so:missing_value = 1.e+20f ;
		so:units = "1e-3" ;
		so:coordinates = "time depth lat lon" ;
		so:standard_name = "sea_water_salinity" ;
		so:long_name = "salinity" ;
		so:_ChunkSizes = 1, 29, 76, 258 ;
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

// global attributes:
		:title = "Salinity (3D) - Daily Mean" ;
		:institution = "Istituto Nazionale di Geofisica e Vulcanologia - Bologna, Italy" ;
		:references = "Please check in CMEMS catalogue the INFO section for product MEDSEA_ANALYSIS_FORECAST_PHY_006_013 - http://marine.copernicus.eu" ;
		:source = "MFS EAS2" ;
		:Conventions = "CF-1.0" ;
		:history = "Data extracted from dataset http://localhost:8080/thredds/dodsC/sv03-med-ingv-sal-an-fc-d" ;
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

public class CopernicusSal extends OceanGridEU implements ISalinity {
    private double[][][] SO = null;

    public void setTime(int localTime)  {
        SO = null;
        super.setTime(localTime);
    }



    public double[][][] getSO() throws NCRegridderException { return load(VARIABLE_SO); }
    public double[][] getSO(int depth) throws NCRegridderException { return load(VARIABLE_SO)[depth];}

    public double[][][] getSalt() throws NCRegridderException { return getSO(); }
    public double[][] getSalt(int depth) throws NCRegridderException { return getSO(depth); }


    public static final int VARIABLE_SO=26;


    public CopernicusSal(String url) throws IOException, NCRegridderException {
        super(url);
        load(VARIABLE_SO);

    }

    public double[][][] load(int varId) throws NCRegridderException {
        double[][][] result=null;

        switch (varId) {


            case VARIABLE_SO:
                if (SO==null) {
                    Variable var = ncDataset.findVariable("so");
                    if (var!=null) {
                        try {
                            ArrayFloat.D4 a = (ArrayFloat.D4)var.read(new int[] { localTime,0,0,0 }, new int[] {1,depth,lat,lon});
                            float[][][][] fSO = (float[][][][])a.copyToNDJavaArray();
                            SO = new double[depth][lat][lon];
                            for (int k=0;k<depth;k++) {
                                for (int j=0;j<lat;j++) {
                                    for (int i=0;i<lon;i++) {
                                        SO[k][j][i]=(double)fSO[0][k][j][i];
                                    }
                                }
                            }
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable SO not found!");

                }
                result = SO;
                break;

        }

        if (result==null) {
            result = super.load(varId);
        }
        return result;
    }
}
