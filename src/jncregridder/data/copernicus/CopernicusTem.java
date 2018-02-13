package jncregridder.data.copernicus;

import jncregridder.data.IPotentialTemperature;
import jncregridder.data.OceanGridEU;
import jncregridder.util.NCRegridderException;
import ucar.ma2.ArrayFloat;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Variable;

import java.io.IOException;

/*

netcdf myoc_d00_20180205_tem {
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
	float thetao(time, depth, lat, lon) ;
		thetao:_CoordinateAxes = "time depth lat lon time depth lat lon " ;
		thetao:_FillValue = 1.e+20f ;
		thetao:missing_value = 1.e+20f ;
		thetao:units = "degC" ;
		thetao:coordinates = "time depth lat lon" ;
		thetao:standard_name = "sea_water_potential_temperature" ;
		thetao:long_name = "temperature" ;
		thetao:_ChunkSizes = 1, 29, 76, 258 ;
	float bottomT(time, lat, lon) ;
		bottomT:_CoordinateAxes = "time lat lon time lat lon " ;
		bottomT:_FillValue = 1.e+20f ;
		bottomT:missing_value = 1.e+20f ;
		bottomT:units = "degC" ;
		bottomT:coordinates = "time lat lon" ;
		bottomT:standard_name = "sea_water_potential_temperature_at_sea_floor" ;
		bottomT:long_name = "Sea floor potential temperature" ;
		bottomT:_ChunkSizes = 1, 380, 1287 ;
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
		:title = "Potential Temperature (3D) - Daily Mean" ;
		:institution = "Istituto Nazionale di Geofisica e Vulcanologia - Bologna, Italy" ;
		:references = "Please check in CMEMS catalogue the INFO section for product MEDSEA_ANALYSIS_FORECAST_PHY_006_013 - http://marine.copernicus.eu" ;
		:source = "MFS EAS2" ;
		:Conventions = "CF-1.0" ;
		:history = "Data extracted from dataset http://localhost:8080/thredds/dodsC/sv03-med-ingv-tem-an-fc-d" ;
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

public class CopernicusTem extends OceanGridEU implements IPotentialTemperature {
    private double[][][] THETAO = null;
    private double[][] BOTTOMT = null;

    public void setTime(int localTime)  {
        THETAO = null;
        BOTTOMT = null;
        super.setTime(localTime);
    }



    public double[][][] getTHETAO() throws NCRegridderException { return load(VARIABLE_THETAO); }
    public double[][] getTHETAO(int depth) throws NCRegridderException { return load(VARIABLE_THETAO)[depth];}

    public double[][] getBOTTOMT() throws NCRegridderException { return load(VARIABLE_BOTTOMT)[0]; }

    public double[][][] getTemp() throws NCRegridderException { return getTHETAO(); }
    public double[][] getTemp(int depth) throws NCRegridderException { return getTHETAO(depth); }


    public static final int VARIABLE_THETAO=25;
    public static final int VARIABLE_BOTTOMT=28;

    public CopernicusTem(String url) throws IOException, NCRegridderException {
        super(url);
        load(VARIABLE_THETAO);
        load(VARIABLE_BOTTOMT);

    }

    public double[][][] load(int varId) throws NCRegridderException {
        double[][][] result=null;

        switch (varId) {


            case VARIABLE_THETAO:
                if (THETAO==null) {
                    Variable var = ncDataset.findVariable("thetao");
                    if (var!=null) {
                        try {
                            ArrayFloat.D4 a = (ArrayFloat.D4)var.read(new int[] { localTime,0,0,0 }, new int[] {1,depth,lat,lon});
                            float[][][][] fTHETAO = (float[][][][])a.copyToNDJavaArray();
                            THETAO = new double[depth][lat][lon];
                            for (int k=0;k<depth;k++) {
                                for (int j=0;j<lat;j++) {
                                    for (int i=0;i<lon;i++) {
                                        THETAO[k][j][i]=(double)fTHETAO[0][k][j][i];
                                    }
                                }
                            }
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable THETAO not found!");

                }
                result = THETAO;
                break;

            case VARIABLE_BOTTOMT:
                if (BOTTOMT==null) {
                    Variable var = ncDataset.findVariable("bottomT");
                    if (var!=null) {
                        try {
                            ArrayFloat.D3 a = (ArrayFloat.D3)var.read(new int[] { localTime,0,0 }, new int[] {1,lat,lon});
                            float[][][] fBOTTOMT = (float[][][])a.copyToNDJavaArray();
                            BOTTOMT = new double[lat][lon];
                            for (int j=0;j<lat;j++) {
                                for (int i=0;i<lon;i++) {
                                    BOTTOMT[j][i]=(double)fBOTTOMT[0][j][i];
                                }
                            }
                        } catch (IOException ex) {
                            throw new NCRegridderException(ex);
                        } catch (InvalidRangeException ex) {
                            throw new NCRegridderException(ex);
                        }
                    } else throw new NCRegridderException("Variable BOTTOMT not found!");

                }
                result = new double[1][1][1];
                result[0] = BOTTOMT;
                break;
        }


        if (result==null) {
            result = super.load(varId);
        }
        return result;
    }
}
