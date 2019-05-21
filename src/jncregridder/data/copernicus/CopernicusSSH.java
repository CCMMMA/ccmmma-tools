package jncregridder.data.copernicus;

import jncregridder.data.ISeaSurfaceHeight;
import jncregridder.data.OceanGridEU;
import jncregridder.util.NCRegridderException;
import ucar.ma2.ArrayFloat;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Variable;

import java.io.IOException;

/*

netcdf myoc_d00_20180205_ssh {
dimensions:
	time = 6 ;
	lat = 380 ;
	lon = 433 ;
variables:
	float zos(time, lat, lon) ;
		zos:_CoordinateAxes = "time lat lon time lat lon " ;
		zos:_FillValue = 1.e+20f ;
		zos:missing_value = 1.e+20f ;
		zos:units = "m" ;
		zos:coordinates = "time lat lon" ;
		zos:standard_name = "sea_surface_height_above_geoid" ;
		zos:long_name = "sea surface height" ;
		zos:_ChunkSizes = 1, 380, 1287 ;
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
	float lat(lat) ;
		lat:units = "degrees_north" ;
		lat:nav_model = "Grid T" ;
		lat:standard_name = "latitude" ;
		lat:long_name = "latitude" ;
		lat:axis = "Y" ;
		lat:_ChunkSizes = 380 ;
		lat:_CoordinateAxisType = "Lat" ;

// global attributes:
		:title = "Sea Surface Height (2D) - Daily Mean" ;
		:institution = "Istituto Nazionale di Geofisica e Vulcanologia - Bologna, Italy" ;
		:references = "Please check in CMEMS catalogue the INFO section for product MEDSEA_ANALYSIS_FORECAST_PHY_006_013 - http://marine.copernicus.eu" ;
		:source = "MFS EAS2" ;
		:Conventions = "CF-1.0" ;
		:history = "Data extracted from dataset http://localhost:8080/thredds/dodsC/sv03-med-ingv-ssh-an-fc-d" ;
		:time_min = 17567. ;
		:time_max = 17572. ;
		:julian_day_unit = "days since 1970-01-01 0:0:0" ;
		:latitude_min = 30.1875 ;
		:latitude_max = 45.9791679382324 ;
		:longitude_min = 6. ;
		:longitude_max = 24. ;
}

 */

public class CopernicusSSH extends OceanGridEU implements ISeaSurfaceHeight {
    public double getUndefinedValue() { return 1e20; }

    private double[][] ZOS = null;

    public void setTime(int localTime)  {
        ZOS = null;
        super.setTime(localTime);
    }

    public double[][] getZOS() throws NCRegridderException { return load(VARIABLE_ZOS)[0]; }
    public static final int VARIABLE_ZOS=27;

    public double[][] getZeta() throws NCRegridderException { return getZOS(); }

    public CopernicusSSH(String url) throws IOException, NCRegridderException {
        super(url);
        load(VARIABLE_ZOS);
    }

    public double[][][] load(int varId) throws NCRegridderException {
        double[][][] result=null;

        switch (varId) {

            case VARIABLE_ZOS:
                if (ZOS==null) {
                    Variable var = ncDataset.findVariable("zos");
                    if (var!=null) {
                        try {
                            ArrayFloat.D3 a = (ArrayFloat.D3)var.read(new int[] { localTime,0,0 }, new int[] {1,lat,lon});
                            float[][][] fZOS = (float[][][])a.copyToNDJavaArray();
                            ZOS = new double[lat][lon];
                            for (int j=0;j<lat;j++) {
                                for (int i=0;i<lon;i++) {
                                    ZOS[j][i]=(double)fZOS[0][j][i];
                                }
                            }
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable SOSSHEIG not found!");

                }
                result = new double[1][1][1];
                result[0] = ZOS;
                break;
        }

        if (result==null) {
            result = super.load(varId);
        }
        return result;
    }
}
