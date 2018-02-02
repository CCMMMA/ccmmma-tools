/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jncregridder.roms;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import jncregridder.util.JulianDate;
import jncregridder.util.NCRegridderException;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayInt;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
//import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

/**
 *
 * @author raffaelemontella
 */
public class ROMSBoundary {
    private String url;
    private ROMSGrid romsGrid;
    
    
    //private NetcdfFileWriteable ncfWritable;
    private NetcdfFileWriter ncfWritable;
    
    
    
    public Dimension dimEtaRho;
    public Dimension dimXiRho;
    public Dimension dimEtaPsi;
    public Dimension dimXiPsi;
    public Dimension dimEtaU;
    public Dimension dimXiU;
    public Dimension dimEtaV;
    public Dimension dimXiV;
    public Dimension dimSRho;
    public Dimension dimSW;
    public Dimension dimOne, dimTwo, dimFour, dimBath;
    public Dimension dimOceanTime;
    
    public double[] OCEAN_TIME=null;
    private double[][] UBAR = null;
    private double[][] VBAR = null;
    private double[][] ZETA = null;
    
    private double[][][] TEMP = null;
    private double[][][] SALT = null;
    private double[][][] U = null;
    private double[][][] V = null;
    /*
    public double[][] UBAR_WEST = null;
    public double[][] UBAR_EAST = null;
    public double[][] UBAR_NORTH = null;
    public double[][] UBAR_SOUTH = null;
    
    public double[][] VBAR_WEST = null;
    public double[][] VBAR_EAST = null;
    public double[][] VBAR_NORTH = null;
    public double[][] VBAR_SOUTH = null;
    
    public double[][] ZETA_WEST = null;
    public double[][] ZETA_EAST = null;
    public double[][] ZETA_NORTH = null;
    public double[][] ZETA_SOUTH = null;
    
    public double[][][] TEMP_WEST = null;
    public double[][][] TEMP_EAST = null;
    public double[][][] TEMP_NORTH = null;
    public double[][][] TEMP_SOUTH = null;
    
    public double[][][] SALT_WEST = null;
    public double[][][] SALT_EAST = null;
    public double[][][] SALT_NORTH = null;
    public double[][][] SALT_SOUTH = null;
    
    public double[][][] U_WEST = null;
    public double[][][] U_EAST = null;
    public double[][][] U_NORTH = null;
    public double[][][] U_SOUTH = null;
    
    public double[][][] V_WEST = null;
    public double[][][] V_EAST = null;
    public double[][][] V_NORTH = null;
    public double[][][] V_SOUTH = null;
    */
    
    Calendar gcSimStartDate = null;
    
    private Variable lat_rho;
    private Variable lon_rho;
    private Variable lat_u;
    private Variable lon_u;
    private Variable lat_v;
    private Variable lon_v;
    private Variable s_rho;
    
    private Variable angle;
    private Variable z_r;
    private Variable Cs_w;
    private Variable s_w;
    
    private Variable ocean_time;
    
    
    private Variable hc;
    private Variable Cs_r;
    private Variable sc_r;
    
    
    private Variable theta_b;
    private Variable theta_s;
    private Variable Tcline;
    
    private Variable h;
    
    
    private Variable temp_west, temp_east, temp_south, temp_north;
    private Variable salt_west, salt_east, salt_south, salt_north;
    private Variable zeta_west, zeta_east, zeta_south, zeta_north;
    private Variable u_west, u_east, u_south, u_north;
    private Variable v_west, v_east, v_south, v_north;
    private Variable ubar_west, ubar_east, ubar_south, ubar_north;
    private Variable vbar_west, vbar_east, vbar_south, vbar_north;
    
    private double dModSimStartDate;
    public double getModSimStartDate() { return dModSimStartDate; }
    
    public static void main(String[] args) throws IOException, NCRegridderException, InvalidRangeException {
         String urlBoundary="/Users/raffaelemontella/tmp/myocean2roms/init/bry-d03.nc";
        String urlGrid="/Users/raffaelemontella/tmp/myocean2roms/roms/roms-grid-d03.nc";
        String ncepDate="20130716Z00";
        int forcingTimeSteps=5;
        ROMSGrid romsGrid = new ROMSGrid(urlGrid);
        ROMSBoundary romsBoundary = new  ROMSBoundary(urlBoundary, romsGrid, ncepDate, forcingTimeSteps);
    }
    
    
    
    
    
    public ROMSBoundary(String url,ROMSGrid romsGrid, String ncepDate, int forcingTimeSteps) throws IOException, InvalidRangeException, NCRegridderException {
        this.url = url;
        this.romsGrid = romsGrid;
        
        
        
        gcSimStartDate = GregorianCalendar.getInstance();
        int year=Integer.parseInt(ncepDate.substring(0,4));
        int month=Integer.parseInt(ncepDate.substring(4,6))-1;
        int day=Integer.parseInt(ncepDate.substring(6,8));
        
        System.out.println("gcSimStartDate:"+year+"-"+month+"-"+day);
        gcSimStartDate.set(year, month, day, 0, 0, 0);
        
        double dSimStartDate = JulianDate.toJulian(gcSimStartDate);
        double dModOffset = JulianDate.get19680523();
        dModSimStartDate = dSimStartDate - dModOffset;
        System.out.println("dModSimStartDate:"+dModSimStartDate);
        
        
        dimEtaRho = new Dimension(romsGrid.dimEtaRho.getName(),romsGrid.dimEtaRho);
        dimXiRho = new Dimension(romsGrid.dimXiRho.getName(),romsGrid.dimXiRho);
        dimEtaU = new Dimension(romsGrid.dimEtaU.getName(),romsGrid.dimEtaU);;
        dimXiU = new Dimension(romsGrid.dimXiU.getName(),romsGrid.dimXiU);;
        dimEtaV = new Dimension(romsGrid.dimEtaV.getName(),romsGrid.dimEtaV);;
        dimXiV = new Dimension(romsGrid.dimXiV.getName(),romsGrid.dimXiV);;
        dimSRho = new Dimension("s_rho",romsGrid.getSRho().length);
        dimSW = new Dimension("s_w",romsGrid.getSW().length);
        
        dimOceanTime = new Dimension("ocean_time", forcingTimeSteps);
        
        dimOne = new Dimension("one", 1);
        dimTwo = new Dimension("two", 2);
        dimFour = new Dimension("four", 4);
        dimBath = new Dimension("bath", 1);
        
        OCEAN_TIME=new double[forcingTimeSteps];
          
        ncfWritable =  NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4_classic, url, null);
        
        ncfWritable.addGroupAttribute(null,new Attribute("type", "Boundary forcing file"));
        ncfWritable.addGroupAttribute(null,new Attribute("title", "Boundary forcing file (BRY) used for foring of the ROMS model"));
        ncfWritable.addGroupAttribute(null,new Attribute("grd_file", romsGrid.getUrl()));
        ncfWritable.addGroupAttribute(null,new Attribute("source", "University of Napoli Parthenope Weather Centre http://meteo.uniparthenope.it"));
        
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        ncfWritable.addGroupAttribute(null, new Attribute("date", sdf.format(cal.getTime())));
                
        ncfWritable.addDimension(null,dimOceanTime.getName(), dimOceanTime.getLength());
        ncfWritable.addDimension(null,romsGrid.dimEtaRho.getName(), romsGrid.dimEtaRho.getLength());
        ncfWritable.addDimension(null,romsGrid.dimXiRho.getName(), romsGrid.dimXiRho.getLength());
        ncfWritable.addDimension(null,romsGrid.dimEtaU.getName(), romsGrid.dimEtaU.getLength());
        ncfWritable.addDimension(null,romsGrid.dimXiU.getName(), romsGrid.dimXiU.getLength());
        ncfWritable.addDimension(null,romsGrid.dimEtaV.getName(), romsGrid.dimEtaV.getLength());
        ncfWritable.addDimension(null,romsGrid.dimXiV.getName(), romsGrid.dimXiV.getLength());
        ncfWritable.addDimension(null,"s_rho", romsGrid.getSRho().length);
        ncfWritable.addDimension(null,dimSW.getName(),dimSW.getLength());
                
        ncfWritable.addDimension(null,dimOne.getName(), dimOne.getLength());
        ncfWritable.addDimension(null,dimTwo.getName(), dimTwo.getLength());
        ncfWritable.addDimension(null,dimBath.getName(), dimBath.getLength());
                
        ocean_time=ncfWritable.addVariable(null,"ocean_time", DataType.DOUBLE, "ocean_time");
        ocean_time.addAttribute(new Attribute("long_name", "ocean forcing time"));
        ocean_time.addAttribute(new Attribute("units", "days since 1968-05-23 00:00:00 GMT"));
        ocean_time.addAttribute(new Attribute("calendar", "gregorian"));
        
        angle=ncfWritable.addVariable(null,"angle", DataType.DOUBLE, "eta_rho xi_rho");
        angle.addAttribute(new Attribute("long_name","angle between xu axis and east"));
        angle.addAttribute(new Attribute("units","radiant"));
        
        theta_b=ncfWritable.addVariable(null,"theta_b", DataType.DOUBLE, "one");
        theta_b.addAttribute(new Attribute("long_name","S-coordinate surface control parameter"));
        theta_b.addAttribute(new Attribute("units","nondimensional"));
          
        theta_s=ncfWritable.addVariable(null,"theta_s", DataType.DOUBLE, "one");
        theta_s.addAttribute(new Attribute("long_name","S-coordinate bottom control parameter"));
        theta_s.addAttribute(new Attribute("units","nondimensional"));
      
        Tcline=ncfWritable.addVariable(null,"Tcline", DataType.DOUBLE, "");
        
        z_r=ncfWritable.addVariable(null,"z_r", DataType.DOUBLE, "s_rho eta_rho xi_rho");
        z_r.addAttribute(new Attribute("long_name","Sigma layer to depth matrix"));
        z_r.addAttribute(new Attribute("units","meter"));
        
        hc=ncfWritable.addVariable(null,"hc", DataType.DOUBLE, "one");
        hc.addAttribute(new Attribute("long_name","S-coordinate parameter, critical depth"));
        hc.addAttribute(new Attribute("units","meter"));
        
        Cs_w=ncfWritable.addVariable(null,"Cs_w", DataType.DOUBLE, "s_w");
        Cs_w.addAttribute(new Attribute("long_name","S-coordinate stretching curves at W-points"));
        Cs_w.addAttribute(new Attribute("valid_min","-1"));
        Cs_w.addAttribute(new Attribute("valid_max","0"));
        Cs_w.addAttribute(new Attribute("field","s_w, scalar"));
        
        Cs_r=ncfWritable.addVariable(null,"Cs_r", DataType.DOUBLE, "s_rho");
        Cs_r.addAttribute(new Attribute("long_name","S-coordinate stretching curves at RHO-points"));
        Cs_r.addAttribute(new Attribute("units","nondimensional"));
          
        s_w=ncfWritable.addVariable(null,"s_w", DataType.DOUBLE, "s_w");
        s_w.addAttribute(new Attribute("long_name","S-coordinate at W-points"));
        s_w.addAttribute(new Attribute("valid_min","-1"));
        s_w.addAttribute(new Attribute("valid_max","0"));
        s_w.addAttribute(new Attribute("standard_name","ocean_s_coordinate_g1"));
        s_w.addAttribute(new Attribute("formula_terms","s: s_w C: Cs_w eta: zeta depth: h depth_c: hc"));
        s_w.addAttribute(new Attribute("field","s_w, scalar"));
        
        sc_r=ncfWritable.addVariable(null,"sc_r", DataType.DOUBLE, "s_rho");
        sc_r.addAttribute(new Attribute("long_name","S-coordinate at RHO-points"));
        sc_r.addAttribute(new Attribute("units","nondimensional"));
                
        s_rho=ncfWritable.addVariable(null,"s_rho", DataType.DOUBLE, "s_rho");
        s_rho.addAttribute(new Attribute("long_name", "oS-coordinate at RHO-points"));
        s_rho.addAttribute(new Attribute("valid_min", "-1.0"));
        s_rho.addAttribute(new Attribute("valid_max", "0.0"));
        s_rho.addAttribute(new Attribute("positive", "up"));
        s_rho.addAttribute(new Attribute("standard_name", "ocean_s_coordinate_g1"));
        s_rho.addAttribute(new Attribute("formula_terms", ""));
        s_rho.addAttribute(new Attribute("field", "s_rho, scalar"));
        s_rho.addAttribute(new Attribute("_CoordinateTransformType", "Vertical"));
        s_rho.addAttribute(new Attribute("_CoordinateAxes", "s_rho"));
        s_rho.addAttribute(new Attribute("_CoordinateAxisType", "GeoZ"));
        s_rho.addAttribute(new Attribute("_CoordinateZisPositive", "up"));
        
        h=ncfWritable.addVariable(null,"h", DataType.DOUBLE, "eta_rho xi_rho");
        h.addAttribute(new Attribute("long_name","Final bathymetry at RHO-points"));
        h.addAttribute(new Attribute("units","meters"));
        h.addAttribute(new Attribute("field","bath, scalar"));
            
        lat_rho=ncfWritable.addVariable(null,"lat_rho", DataType.DOUBLE, "eta_rho xi_rho");
        lat_rho.addAttribute(new Attribute("long_name","latitude of RHO-points"));
        lat_rho.addAttribute(new Attribute("units","degree_north"));
        lat_rho.addAttribute(new Attribute("field","lat_rho, scalar"));
        lat_rho.addAttribute(new Attribute("standard_name","latitude"));
        lat_rho.addAttribute(new Attribute("_CoordinateAxisType", "Lat"));

        lon_rho=ncfWritable.addVariable(null,"lon_rho", DataType.DOUBLE, "eta_rho xi_rho");
        lon_rho.addAttribute(new Attribute("long_name","longitude of RHO-points"));
        lon_rho.addAttribute(new Attribute("units","degree_east"));
        lon_rho.addAttribute(new Attribute("field","lon_rho, scalar"));
        lon_rho.addAttribute(new Attribute("standard_name","longitude"));
        lon_rho.addAttribute(new Attribute("_CoordinateAxisType", "Lon"));
        
        lat_u=ncfWritable.addVariable(null,"lat_u", DataType.DOUBLE, "eta_u xi_u");
        lat_u.addAttribute(new Attribute("long_name","latitude of RHO-points"));
        lat_u.addAttribute(new Attribute("units","degree_north"));
        lat_u.addAttribute(new Attribute("standard_name","latitude"));
        lat_u.addAttribute(new Attribute("_CoordinateAxisType", "Lat"));

        lon_u=ncfWritable.addVariable(null,"lon_u", DataType.DOUBLE, "eta_u xi_u");
        lon_u.addAttribute(new Attribute("long_name","longitude of U-points"));
        lon_u.addAttribute(new Attribute("units","degree_east"));
        lon_u.addAttribute(new Attribute("standard_name","longitude"));
        lon_u.addAttribute(new Attribute("_CoordinateAxisType", "Lon"));

        lat_v=ncfWritable.addVariable(null,"lat_v", DataType.DOUBLE, "eta_v xi_v");
        lat_v.addAttribute(new Attribute("long_name","latitude of V-points"));
        lat_v.addAttribute(new Attribute("units","degree_north"));
        lat_v.addAttribute(new Attribute("standard_name","latitude"));
        lat_v.addAttribute(new Attribute("_CoordinateAxisType", "Lat"));

        lon_v=ncfWritable.addVariable(null,"lon_v", DataType.DOUBLE, "eta_v xi_v");
        lon_v.addAttribute(new Attribute("long_name","longitude of V-points"));
        lon_v.addAttribute(new Attribute("units","degree_east"));
        lon_v.addAttribute(new Attribute("standard_name","longitude"));
        lon_v.addAttribute(new Attribute("_CoordinateAxisType", "Lon"));
        
        
        /* temp */
        
        temp_west=ncfWritable.addVariable(null,"temp_west", DataType.DOUBLE, "ocean_time s_rho eta_rho");
        temp_west.addAttribute(new Attribute("long_name","potential temperature western boundary conditions"));
        temp_west.addAttribute(new Attribute("units","Celsius"));
        temp_west.addAttribute(new Attribute("field","temp_west, scalar, series"));
        temp_west.addAttribute(new Attribute("missing_value", romsGrid.getNoData()));
        temp_west.addAttribute(new Attribute("time", "ocean_time"));
        
        temp_east=ncfWritable.addVariable(null,"temp_east", DataType.DOUBLE, "ocean_time s_rho eta_rho");
        temp_east.addAttribute(new Attribute("long_name","potential temperature eastern boundary conditions"));
        temp_east.addAttribute(new Attribute("units","Celsius"));
        temp_east.addAttribute(new Attribute("field","temp_east, scalar, series"));
        temp_east.addAttribute(new Attribute("missing_value", romsGrid.getNoData()));
        temp_east.addAttribute(new Attribute("time", "ocean_time"));
        
        temp_south=ncfWritable.addVariable(null,"temp_south", DataType.DOUBLE, "ocean_time s_rho xi_rho");
        temp_south.addAttribute(new Attribute("long_name","potential temperature southern boundary conditions"));
        temp_south.addAttribute(new Attribute("units","Celsius"));
        temp_south.addAttribute(new Attribute("field","temp_south, scalar, series"));
        temp_south.addAttribute(new Attribute("missing_value", romsGrid.getNoData()));
        temp_south.addAttribute(new Attribute("time", "ocean_time"));
        
        temp_north=ncfWritable.addVariable(null,"temp_north", DataType.DOUBLE, "ocean_time s_rho xi_rho");
        temp_north.addAttribute(new Attribute("long_name","potential temperature northen boundary conditions"));
        temp_north.addAttribute(new Attribute("units","Celsius"));
        temp_north.addAttribute(new Attribute("field","temp_south, scalar, series"));
        temp_north.addAttribute(new Attribute("missing_value", romsGrid.getNoData()));
        temp_north.addAttribute(new Attribute("time", "ocean_time"));
        
        /* salt */
        
        salt_west=ncfWritable.addVariable(null,"salt_west", DataType.DOUBLE, "ocean_time s_rho eta_rho");
        salt_west.addAttribute(new Attribute("long_name","salinity western boundary conditions"));
        salt_west.addAttribute(new Attribute("units","PSU"));
        salt_west.addAttribute(new Attribute("field","salt_west, scalar, series"));
        salt_west.addAttribute(new Attribute("missing_value", romsGrid.getNoData()));
        salt_west.addAttribute(new Attribute("time", "ocean_time"));
        
        salt_east=ncfWritable.addVariable(null,"salt_east", DataType.DOUBLE, "ocean_time s_rho eta_rho");
        salt_east.addAttribute(new Attribute("long_name","salinity eastern boundary conditions"));
        salt_east.addAttribute(new Attribute("units","PSU"));
        salt_east.addAttribute(new Attribute("field","salt_east, scalar, series"));
        salt_east.addAttribute(new Attribute("missing_value", romsGrid.getNoData()));
        salt_east.addAttribute(new Attribute("time", "ocean_time"));
        
        salt_south=ncfWritable.addVariable(null,"salt_south", DataType.DOUBLE, "ocean_time s_rho xi_rho");
        salt_south.addAttribute(new Attribute("long_name","salinity southern boundary conditions"));
        salt_south.addAttribute(new Attribute("units","PSU"));
        salt_south.addAttribute(new Attribute("field","salt_south, scalar, series"));
        salt_south.addAttribute(new Attribute("missing_value", romsGrid.getNoData()));
        salt_south.addAttribute(new Attribute("time", "ocean_time"));
        
        salt_north=ncfWritable.addVariable(null,"salt_north", DataType.DOUBLE, "ocean_time s_rho xi_rho");
        salt_north.addAttribute(new Attribute("long_name","salinity northen boundary conditions"));
        salt_north.addAttribute(new Attribute("units","PSU"));
        salt_north.addAttribute(new Attribute("field","salt_north, scalar, series"));
        salt_north.addAttribute(new Attribute("missing_value", romsGrid.getNoData()));
        salt_north.addAttribute(new Attribute("time", "ocean_time"));
        
        /* zeta */
        
        zeta_west=ncfWritable.addVariable(null,"zeta_west", DataType.DOUBLE, "ocean_time eta_rho");
        zeta_west.addAttribute(new Attribute("long_name","free-surface western boundary conditions"));
        zeta_west.addAttribute(new Attribute("units","meter"));
        zeta_west.addAttribute(new Attribute("field","zeta_west, scalar, series"));
        zeta_west.addAttribute(new Attribute("missing_value", romsGrid.getNoData()));
        zeta_west.addAttribute(new Attribute("time", "ocean_time"));
        
        zeta_east=ncfWritable.addVariable(null,"zeta_east", DataType.DOUBLE, "ocean_time eta_rho");
        zeta_east.addAttribute(new Attribute("long_name","free-surface eastern boundary conditions"));
        zeta_east.addAttribute(new Attribute("units","meter"));
        zeta_east.addAttribute(new Attribute("field","zeta_east, scalar, series"));
        zeta_east.addAttribute(new Attribute("missing_value", romsGrid.getNoData()));
        zeta_east.addAttribute(new Attribute("time", "ocean_time"));
        
        zeta_south=ncfWritable.addVariable(null,"zeta_south", DataType.DOUBLE, "ocean_time xi_rho");
        zeta_south.addAttribute(new Attribute("long_name","free-surface southern boundary conditions"));
        zeta_south.addAttribute(new Attribute("units","meter"));
        zeta_south.addAttribute(new Attribute("field","zeta_south, scalar, series"));
        zeta_south.addAttribute(new Attribute("missing_value", romsGrid.getNoData()));
        zeta_south.addAttribute(new Attribute("time", "ocean_time"));
        
        zeta_north=ncfWritable.addVariable(null,"zeta_north", DataType.DOUBLE, "ocean_time xi_rho");
        zeta_north.addAttribute(new Attribute("long_name","free-surface northen boundary conditions"));
        zeta_north.addAttribute(new Attribute("units","meter"));
        zeta_north.addAttribute(new Attribute("field","zeta_north, scalar, series"));
        zeta_north.addAttribute(new Attribute("missing_value", romsGrid.getNoData()));
        zeta_north.addAttribute(new Attribute("time", "ocean_time"));
        

        /* u */
        
        u_west=ncfWritable.addVariable(null,"u_west", DataType.DOUBLE, "ocean_time s_rho eta_u");
        u_west.addAttribute(new Attribute("long_name","3D U-momentum western boundary conditions"));
        u_west.addAttribute(new Attribute("units","meter second-1"));
        u_west.addAttribute(new Attribute("field","u_west, scalar, series"));
        u_west.addAttribute(new Attribute("missing_value", romsGrid.getNoData()));
        u_west.addAttribute(new Attribute("time", "ocean_time"));
        
        u_east=ncfWritable.addVariable(null,"u_east", DataType.DOUBLE, "ocean_time s_rho eta_u");
        u_east.addAttribute(new Attribute("long_name","3D U-momentum eastern boundary conditions"));
        u_east.addAttribute(new Attribute("units","meter second-1"));
        u_east.addAttribute(new Attribute("field","u_east, scalar, series"));
        u_east.addAttribute(new Attribute("missing_value", romsGrid.getNoData()));
        u_east.addAttribute(new Attribute("time", "ocean_time"));
        
        u_south=ncfWritable.addVariable(null,"u_south", DataType.DOUBLE, "ocean_time s_rho xi_u");
        u_south.addAttribute(new Attribute("long_name","3D U-momentum southern boundary conditions"));
        u_south.addAttribute(new Attribute("units","meter second-1"));
        u_south.addAttribute(new Attribute("field","u_south, scalar, series"));
        u_south.addAttribute(new Attribute("missing_value", romsGrid.getNoData()));
        u_south.addAttribute(new Attribute("time", "ocean_time"));
        
        u_north=ncfWritable.addVariable(null,"u_north", DataType.DOUBLE, "ocean_time s_rho xi_u");
        u_north.addAttribute(new Attribute("long_name","3D U-momentum northen boundary conditions"));
        u_north.addAttribute(new Attribute("units","meter second-1"));
        u_north.addAttribute(new Attribute("field","u_north, scalar, series"));
        u_north.addAttribute(new Attribute("missing_value", romsGrid.getNoData()));
        u_north.addAttribute(new Attribute("time", "ocean_time"));
        
        /* v */
        
        v_west=ncfWritable.addVariable(null,"v_west", DataType.DOUBLE, "ocean_time s_rho eta_v");
        v_west.addAttribute(new Attribute("long_name","3D V-momentum western boundary conditions"));
        v_west.addAttribute(new Attribute("units","meter second-1"));
        v_west.addAttribute(new Attribute("field","v_west, scalar, series"));
        v_west.addAttribute(new Attribute("missing_value", romsGrid.getNoData()));
        v_west.addAttribute(new Attribute("time", "ocean_time"));
        
        v_east=ncfWritable.addVariable(null,"v_east", DataType.DOUBLE, "ocean_time s_rho eta_v");
        v_east.addAttribute(new Attribute("long_name","3D V-momentum eastern boundary conditions"));
        v_east.addAttribute(new Attribute("units","meter second-1"));
        v_east.addAttribute(new Attribute("field","v_east, scalar, series"));
        v_east.addAttribute(new Attribute("missing_value", romsGrid.getNoData()));
        v_east.addAttribute(new Attribute("time", "ocean_time"));
        
        v_south=ncfWritable.addVariable(null,"v_south", DataType.DOUBLE, "ocean_time s_rho xi_v");
        v_south.addAttribute(new Attribute("long_name","3D V-momentum southern boundary conditions"));
        v_south.addAttribute(new Attribute("units","meter second-1"));
        v_south.addAttribute(new Attribute("field","v_south, scalar, series"));
        v_south.addAttribute(new Attribute("missing_value", romsGrid.getNoData()));
        v_south.addAttribute(new Attribute("time", "ocean_time"));
        
        v_north=ncfWritable.addVariable(null,"v_north", DataType.DOUBLE, "ocean_time s_rho xi_v");
        v_north.addAttribute(new Attribute("long_name","3D V-momentum northen boundary conditions"));
        v_north.addAttribute(new Attribute("units","meter second-1"));
        v_north.addAttribute(new Attribute("field","v_north, scalar, series"));
        v_north.addAttribute(new Attribute("missing_value", romsGrid.getNoData()));
        v_north.addAttribute(new Attribute("time", "ocean_time"));
        
        /* vbar */
        
        vbar_west=ncfWritable.addVariable(null,"vbar_west", DataType.DOUBLE, "ocean_time eta_v");
        vbar_west.addAttribute(new Attribute("long_name","2D V-momentum western boundary conditions"));
        vbar_west.addAttribute(new Attribute("units","meter second-1"));
        vbar_west.addAttribute(new Attribute("field","vbar_west, scalar, series"));
        vbar_west.addAttribute(new Attribute("missing_value", romsGrid.getNoData()));
        vbar_west.addAttribute(new Attribute("time", "ocean_time"));
        
        vbar_east=ncfWritable.addVariable(null,"vbar_east", DataType.DOUBLE, "ocean_time eta_v");
        vbar_east.addAttribute(new Attribute("long_name","2D V-momentum eastern boundary conditions"));
        vbar_east.addAttribute(new Attribute("units","meter second-1"));
        vbar_east.addAttribute(new Attribute("field","vbar_east, scalar, series"));
        vbar_east.addAttribute(new Attribute("missing_value", romsGrid.getNoData()));
        vbar_east.addAttribute(new Attribute("time", "ocean_time"));
        
        vbar_south=ncfWritable.addVariable(null,"vbar_south", DataType.DOUBLE, "ocean_time xi_v");
        vbar_south.addAttribute(new Attribute("long_name","2D V-momentum southern boundary conditions"));
        vbar_south.addAttribute(new Attribute("units","meter second-1"));
        vbar_south.addAttribute(new Attribute("field","vbar_south, scalar, series"));
        vbar_south.addAttribute(new Attribute("missing_value", romsGrid.getNoData()));
        vbar_south.addAttribute(new Attribute("time", "ocean_time"));
        
        vbar_north=ncfWritable.addVariable(null,"vbar_north", DataType.DOUBLE, "ocean_time xi_v");
        vbar_north.addAttribute(new Attribute("long_name","2D V-momentum northen boundary conditions"));
        vbar_north.addAttribute(new Attribute("units","meter second-1"));
        vbar_north.addAttribute(new Attribute("field","vbar_north, scalar, series"));
        vbar_north.addAttribute(new Attribute("missing_value", romsGrid.getNoData()));
        vbar_north.addAttribute(new Attribute("time", "ocean_time"));
        
        /* ubar */
        
        ubar_west=ncfWritable.addVariable(null,"ubar_west", DataType.DOUBLE, "ocean_time eta_u");
        ubar_west.addAttribute(new Attribute("long_name","2D U-momentum western boundary conditions"));
        ubar_west.addAttribute(new Attribute("units","meter second-1"));
        ubar_west.addAttribute(new Attribute("field","ubar_west, scalar, series"));
        ubar_west.addAttribute(new Attribute("missing_value", romsGrid.getNoData()));
        ubar_west.addAttribute(new Attribute("time", "ocean_time"));
        
        ubar_east=ncfWritable.addVariable(null,"ubar_east", DataType.DOUBLE, "ocean_time eta_u");
        ubar_east.addAttribute(new Attribute("long_name","2D U-momentum eastern boundary conditions"));
        ubar_east.addAttribute(new Attribute("units","meter second-1"));
        ubar_east.addAttribute(new Attribute("field","ubar_east, scalar, series"));
        ubar_east.addAttribute(new Attribute("missing_value", romsGrid.getNoData()));
        ubar_east.addAttribute(new Attribute("time", "ocean_time"));
        
        ubar_south=ncfWritable.addVariable(null,"ubar_south", DataType.DOUBLE, "ocean_time xi_u");
        ubar_south.addAttribute(new Attribute("long_name","2D U-momentum southern boundary conditions"));
        ubar_south.addAttribute(new Attribute("units","meter second-1"));
        ubar_south.addAttribute(new Attribute("field","ubar_south, scalar, series"));
        ubar_south.addAttribute(new Attribute("missing_value", romsGrid.getNoData()));
        ubar_south.addAttribute(new Attribute("time", "ocean_time"));
        
        ubar_north=ncfWritable.addVariable(null,"ubar_north", DataType.DOUBLE, "ocean_time xi_u");
        ubar_north.addAttribute(new Attribute("long_name","2D U-momentum northen boundary conditions"));
        ubar_north.addAttribute(new Attribute("units","meter second-1"));
        ubar_north.addAttribute(new Attribute("field","u_north, scalar, series"));
        ubar_north.addAttribute(new Attribute("missing_value", romsGrid.getNoData()));
        ubar_north.addAttribute(new Attribute("time", "ocean_time"));
        
        ncfWritable.create();
    }

    public void make() throws IOException, InvalidRangeException, NCRegridderException {
        double dSimStartDate = JulianDate.toJulian(gcSimStartDate);
        double dModOffset = JulianDate.get19680523();
        double dModSimStartDate = dSimStartDate - dModOffset;
        System.out.println("dModSimStartDate:"+dModSimStartDate);
        
        int etaRho = dimEtaRho.getLength();
        int xiRho = dimXiRho.getLength();
        int etaU = dimEtaU.getLength();
        int xiU = dimXiU.getLength();
        int etaV = dimEtaV.getLength();
        int xiV = dimXiV.getLength();
        int sRho = dimSRho.getLength();
        int one = dimOne.getLength();
        int oceanTime = dimOceanTime.getLength();
        
        ArrayDouble.D1 outAThetaS = new ArrayDouble.D1(one);
        ArrayDouble.D1 outAThetaB = new ArrayDouble.D1(one);
        ArrayDouble.D1 outASCR = new ArrayDouble.D1(sRho);
        ArrayDouble.D1 outAHC = new ArrayDouble.D1(one);
        ArrayDouble.D1 outACSR = new ArrayDouble.D1(sRho);
        
        ArrayDouble.D1 outAOceanTime = new ArrayDouble.D1(oceanTime);
        
        for (int t=0;t<oceanTime;t++) {
            outAOceanTime.set(t, OCEAN_TIME[t]);
        }
        
        ArrayDouble.D0 outATCline = new ArrayDouble.D0();
        
        
        for (int k=0;k<sRho;k++) {
            outASCR.set(k, romsGrid.getSRho()[k]);
            outACSR.set(k, romsGrid.getCSR()[k]);
            
        }
        
        outAHC.set(0,romsGrid.getHC());
        outAThetaS.set(0,romsGrid.getThetaS());
        outAThetaB.set(0,romsGrid.getThetaB());
        
        
        outATCline.set(50);
        
        ArrayDouble.D2 outALonRho = new ArrayDouble.D2(etaRho,xiRho);
        ArrayDouble.D2 outALatRho = new ArrayDouble.D2(etaRho,xiRho);
        ArrayDouble.D2 outALonU = new ArrayDouble.D2(etaU,xiU);
        ArrayDouble.D2 outALatU = new ArrayDouble.D2(etaU,xiU);
        ArrayDouble.D2 outALonV = new ArrayDouble.D2(etaV,xiV);
        ArrayDouble.D2 outALatV = new ArrayDouble.D2(etaV,xiV);
        ArrayDouble.D2 outAHRHO = new ArrayDouble.D2(etaRho,xiRho);
        
        double[][] LONRHO=romsGrid.getLONRHO();
        double[][] LATRHO=romsGrid.getLATRHO();
        double[][] LONU=romsGrid.getLONU();
        double[][] LATU=romsGrid.getLATU();
        double[][] LONV=romsGrid.getLONV();
        double[][] LATV=romsGrid.getLATV();
        double[][] HRHO=romsGrid.getH();
        
         for(int j=0;j<etaRho;j++) {
            for (int i=0;i<xiRho;i++) {
                outALonRho.set(j, i, LONRHO[j][i]);
                outALatRho.set(j, i, LATRHO[j][i]);
                outAHRHO.set(j, i, HRHO[j][i]);
            }
        }
        
        for(int j=0;j<etaU;j++) {
            for (int i=0;i<xiU;i++) {
                outALonU.set(j, i, LONU[j][i]);
                outALatU.set(j, i, LATU[j][i]);
            }
        }
        
        for(int j=0;j<etaV;j++) {
            for (int i=0;i<xiV;i++) {
                outALonV.set(j, i, LONV[j][i]);
                outALatV.set(j, i, LATV[j][i]);
            }
        }
        
        ncfWritable.write(h, new int [] { 0,0 }, outAHRHO);
        ncfWritable.write(lon_rho, new int [] { 0,0 }, outALonRho);
        ncfWritable.write(lat_rho, new int [] { 0,0 }, outALatRho);
        ncfWritable.write(lon_u, new int [] { 0,0 }, outALonU);
        ncfWritable.write(lat_u, new int [] { 0,0 }, outALatU);
        ncfWritable.write(lon_v, new int [] { 0,0 }, outALonV);
        ncfWritable.write(lat_v, new int [] { 0,0 }, outALatV);
       
        ncfWritable.write(theta_s, new int [] { 0 }, outAThetaS);
        ncfWritable.write(theta_b, new int [] { 0 }, outAThetaB);
        ncfWritable.write(Tcline, outATCline);
        
        
        ncfWritable.write(ocean_time, new int [] { 0 }, outAOceanTime);
        
        ncfWritable.write(sc_r, new int [] { 0 }, outASCR);
        ncfWritable.write(s_rho, new int [] { 0 }, outASCR);
        ncfWritable.write(hc, new int [] { 0 }, outAHC);
        ncfWritable.write(Cs_r, new int [] { 0 }, outACSR);
        
        
               
        ncfWritable.flush();
    }
    
    public void write(int localTime) throws IOException, InvalidRangeException, NCRegridderException {
        double dSimStartDate = JulianDate.toJulian(gcSimStartDate);
        double dModOffset = JulianDate.get19680523();
        double dModSimStartDate = dSimStartDate - dModOffset;
        System.out.println("dModSimStartDate:"+dModSimStartDate);
        
        int etaRho = dimEtaRho.getLength();
        int xiRho = dimXiRho.getLength();
        int etaU = dimEtaU.getLength();
        int xiU = dimXiU.getLength();
        int etaV = dimEtaV.getLength();
        int xiV = dimXiV.getLength();
        int sRho = dimSRho.getLength();
        int one = dimOne.getLength();
        
        ArrayDouble.D3 outASALT_WEST = new ArrayDouble.D3(1, sRho, etaRho);
        ArrayDouble.D3 outASALT_EAST = new ArrayDouble.D3(1, sRho, etaRho);
        ArrayDouble.D3 outASALT_SOUTH = new ArrayDouble.D3(1, sRho, xiRho);
        ArrayDouble.D3 outASALT_NORTH = new ArrayDouble.D3(1, sRho, xiRho);
        
        ArrayDouble.D3 outATEMP_WEST = new ArrayDouble.D3(1, sRho, etaRho);
        ArrayDouble.D3 outATEMP_EAST = new ArrayDouble.D3(1, sRho, etaRho);
        ArrayDouble.D3 outATEMP_SOUTH = new ArrayDouble.D3(1, sRho, xiRho);
        ArrayDouble.D3 outATEMP_NORTH = new ArrayDouble.D3(1, sRho, xiRho);
        
        ArrayDouble.D3 outAU_WEST = new ArrayDouble.D3(1, sRho, etaU);
        ArrayDouble.D3 outAU_EAST = new ArrayDouble.D3(1, sRho, etaU);
        ArrayDouble.D3 outAU_SOUTH = new ArrayDouble.D3(1, sRho, xiU);
        ArrayDouble.D3 outAU_NORTH = new ArrayDouble.D3(1, sRho, xiU);

        ArrayDouble.D3 outAV_WEST = new ArrayDouble.D3(1, sRho, etaV);
        ArrayDouble.D3 outAV_EAST = new ArrayDouble.D3(1, sRho, etaV);
        ArrayDouble.D3 outAV_SOUTH = new ArrayDouble.D3(1, sRho, xiV);
        ArrayDouble.D3 outAV_NORTH = new ArrayDouble.D3(1, sRho, xiV);

        ArrayDouble.D2 outAUBAR_WEST = new ArrayDouble.D2(1, etaU);
        ArrayDouble.D2 outAUBAR_EAST = new ArrayDouble.D2(1, etaU);
        ArrayDouble.D2 outAUBAR_SOUTH = new ArrayDouble.D2(1, xiU);
        ArrayDouble.D2 outAUBAR_NORTH = new ArrayDouble.D2(1, xiU);

        ArrayDouble.D2 outAVBAR_WEST = new ArrayDouble.D2(1, etaV);
        ArrayDouble.D2 outAVBAR_EAST = new ArrayDouble.D2(1, etaV);
        ArrayDouble.D2 outAVBAR_SOUTH = new ArrayDouble.D2(1, xiV);
        ArrayDouble.D2 outAVBAR_NORTH = new ArrayDouble.D2(1, xiV);
        
        ArrayDouble.D2 outAZETA_WEST = new ArrayDouble.D2(1, etaRho);
        ArrayDouble.D2 outAZETA_EAST = new ArrayDouble.D2(1, etaRho);
        ArrayDouble.D2 outAZETA_SOUTH = new ArrayDouble.D2(1, xiRho);
        ArrayDouble.D2 outAZETA_NORTH = new ArrayDouble.D2(1, xiRho);
        
        
        /* 3D */    
        for (int k=0;k<sRho;k++) {
            
            for (int j=0;j<etaRho;j++) {
                outATEMP_WEST.set(0,k,j,TEMP[k][j][0]);
                outASALT_WEST.set(0,k,j,SALT[k][j][0]);
                
                outATEMP_EAST.set(0,k,j,TEMP[k][j][dimXiRho.getLength()-1]); 
                outASALT_EAST.set(0,k,j,SALT[k][j][dimXiRho.getLength()-1]);   
            }
            

            for (int i=0;i<xiRho;i++) {
                outATEMP_SOUTH.set(0,k,i,TEMP[k][0][i]);
                outASALT_SOUTH.set(0,k,i,SALT[k][0][i]);
                
                outATEMP_NORTH.set(0,k,i,TEMP[k][dimEtaRho.getLength()-1][i]); 
                outASALT_NORTH.set(0,k,i,SALT[k][dimEtaRho.getLength()-1][i]);     
            }

            for (int j=0;j<etaU;j++) {
                outAU_WEST.set(0,k,j,U[k][j][0]);
                outAU_EAST.set(0,k,j,U[k][j][dimXiU.getLength()-1]);

            }

            for (int i=0;i<xiU;i++) {
                outAU_SOUTH.set(0,k,i,U[k][0][i]);
                outAU_NORTH.set(0,k,i,U[k][dimEtaU.getLength()-1][i]);

            }

            for (int j=0;j<etaV;j++) {
                outAV_WEST.set(0,k,j,V[k][j][0]);
                outAV_EAST.set(0,k,j,V[k][j][dimXiV.getLength()-1]);

            }

            for (int i=0;i<xiV;i++) {
                outAV_SOUTH.set(0,k,i,V[k][0][i]);
                outAV_NORTH.set(0,k,i,V[k][dimEtaV.getLength()-1][i]);

            }
        }

        /* 2D */
        for (int j=0;j<etaRho;j++) {
            outAZETA_WEST.set(0,j,ZETA[j][0]);
            outAZETA_EAST.set(0,j,ZETA[j][dimXiRho.getLength()-1]);
        }

        for (int j=0;j<etaU;j++) {
            outAUBAR_WEST.set(0,j,UBAR[j][0]);
            outAUBAR_EAST.set(0,j,UBAR[j][dimXiU.getLength()-1]);
        }

        for (int j=0;j<etaV;j++) {
            outAVBAR_WEST.set(0,j,VBAR[j][0]);
            outAVBAR_EAST.set(0,j,VBAR[j][dimXiV.getLength()-1]);
        }

        for (int i=0;i<xiRho;i++) {
            outAZETA_SOUTH.set(0,i,ZETA[0][i]);
            outAZETA_NORTH.set(0,i,ZETA[dimEtaRho.getLength()-1][i]);
        }

        for (int i=0;i<xiU;i++) {
            outAUBAR_SOUTH.set(0,i,UBAR[0][i]);
            outAUBAR_NORTH.set(0,i,UBAR[dimEtaU.getLength()-1][i]);
        }

        for (int i=0;i<xiV;i++) {
            outAVBAR_SOUTH.set(0,i,VBAR[0][i]);
            outAVBAR_NORTH.set(0,i,VBAR[dimEtaV.getLength()-1][i]);
        }
        
        
        ncfWritable.write(u_west, new int [] { localTime,0,0 }, outAU_WEST);
        ncfWritable.write(u_east, new int [] { localTime,0,0 }, outAU_EAST);
        ncfWritable.write(u_south, new int [] { localTime,0,0 }, outAU_SOUTH);
        ncfWritable.write(u_north, new int [] { localTime,0,0 }, outAU_NORTH);
        
        ncfWritable.write(v_west, new int [] { localTime,0,0 }, outAV_WEST);
        ncfWritable.write(v_east, new int [] { localTime,0,0 }, outAV_EAST);
        ncfWritable.write(v_south, new int [] { localTime,0,0 }, outAV_SOUTH);
        ncfWritable.write(v_north, new int [] { localTime,0,0 }, outAV_NORTH);
        
        ncfWritable.write(salt_west, new int [] { localTime,0,0 }, outASALT_WEST);
        ncfWritable.write(salt_east, new int [] { localTime,0,0 }, outASALT_EAST);
        ncfWritable.write(salt_south, new int [] { localTime,0,0 }, outASALT_SOUTH);
        ncfWritable.write(salt_north, new int [] { localTime,0,0 }, outASALT_NORTH);
        
        ncfWritable.write(temp_west, new int [] { localTime,0,0 }, outATEMP_WEST);
        ncfWritable.write(temp_east, new int [] { localTime,0,0 }, outATEMP_EAST);
        ncfWritable.write(temp_south, new int [] { localTime,0,0 }, outATEMP_SOUTH);
        ncfWritable.write(temp_north, new int [] { localTime,0,0 }, outATEMP_NORTH);
        
        ncfWritable.write(zeta_west, new int [] { localTime,0 }, outAZETA_WEST);
        ncfWritable.write(zeta_east, new int [] { localTime,0 }, outAZETA_EAST);
        ncfWritable.write(zeta_south, new int [] { localTime,0 }, outAZETA_SOUTH);
        ncfWritable.write(zeta_north, new int [] { localTime,0}, outAZETA_NORTH);
        
        ncfWritable.write(ubar_west, new int [] { localTime,0 }, outAUBAR_WEST);
        ncfWritable.write(ubar_east, new int [] { localTime,0 }, outAUBAR_EAST);
        ncfWritable.write(ubar_south, new int [] { localTime,0 }, outAUBAR_SOUTH);
        ncfWritable.write(ubar_north, new int [] { localTime,0 }, outAUBAR_NORTH);
        
        ncfWritable.write(vbar_west, new int [] { localTime,0 }, outAVBAR_WEST);
        ncfWritable.write(vbar_east, new int [] { localTime,0 }, outAVBAR_EAST);
        ncfWritable.write(vbar_south, new int [] { localTime,0 }, outAVBAR_SOUTH);
        ncfWritable.write(vbar_north, new int [] { localTime,0 }, outAVBAR_NORTH);
               
        ncfWritable.flush();
        
    }
    
    public void close() throws IOException {
        ncfWritable.close();
    }
 
    public void setOceanTime(double[] oceanTime) { this.OCEAN_TIME=oceanTime; }
    public void setUBAR(double[][] UBAR) { this.UBAR = UBAR; }
    public void setVBAR(double[][] VBAR) { this.VBAR = VBAR; }
    public void setZETA(double[][] ZETA) { this.ZETA = ZETA; }
    public void setSALT(double[][][] SALT) { this.SALT = SALT; }
    public void setTEMP(double[][][] TEMP) { this.TEMP = TEMP; }
    public void setU(double[][][] U) { this.U = U; }
    public void setV(double[][][] V) { this.V = V; }
    
    
}
