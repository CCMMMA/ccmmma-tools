/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jncregridder.roms;


import it.uniparthenope.meteo.MyOcean2ROMS;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import jncregridder.util.CustomInterpolator;
import jncregridder.util.JulianDate;
import jncregridder.util.NCRegridderException;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayInt;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;


/**
 *
 * @author raffaelemontella
 */
    public class ROMSInit {
    private String url;
    private ROMSGrid romsGrid;
    
    
    //private NetcdfFileWriteable ncfWritable;
    private NetcdfFileWriter ncfWritable;
    
    private double noData = 1e37;
    
    private double[] OCEAN_TIME =null;
    private double[] SCRUM_TIME =null;
    private double[][] UBAR = null;
    private double[][] VBAR = null;
    private double[][] ZETA = null;
    
    private double[][][] TEMP = null;
    private double[][][] SALT = null;
    private double[][][] U = null;
    private double[][][] V = null;
    
    public static final int VARIABLE_LATRHO=11;
    public static final int VARIABLE_LONRHO=12;
    public static final int VARIABLE_LATU=13;
    public static final int VARIABLE_LATV=14;
    public static final int VARIABLE_LONU=15;
    public static final int VARIABLE_LONV=16;
    public static final int VARIABLE_ANGLE=18;
    public static final int VARIABLE_H=19;
    public static final int VARIABLE_ZETA=20;
    public static final int VARIABLE_MASKRHO=21;
    public static final int VARIABLE_UBAR=22;
    public static final int VARIABLE_VBAR=23;
    public static final int VARIABLE_TEMP=24;
    public static final int VARIABLE_SALT=25;
    public static final int VARIABLE_U=26;
    public static final int VARIABLE_V=27;
    public static final int VARIABLE_W=28;
    
    public void setOceanTime(double[] oceanTime) { this.OCEAN_TIME=oceanTime; }
    public void setScrumTime(double[] scrumTime) { this.SCRUM_TIME=scrumTime; }
    public void setUBAR(double[][] UBAR) { this.UBAR = UBAR; }
    public void setVBAR(double[][] VBAR) { this.VBAR = VBAR; }
    public void setZETA(double[][] ZETA) { this.ZETA = ZETA; }
    public void setSALT(double[][][] SALT) { this.SALT = SALT; }
    public void setTEMP(double[][][] TEMP) { this.TEMP = TEMP; }
    public void setU(double[][][] U) { this.U = U; }
    public void setV(double[][][] V) { this.V = V; }
    
    public Dimension dimEtaRho;
    public Dimension dimXiRho;
    public Dimension dimEtaU;
    public Dimension dimXiU;
    public Dimension dimEtaV;
    public Dimension dimXiV;
    public Dimension dimSRho;
    public Dimension dimOne;
    public Dimension dimOceanTime;
    public Dimension dimScrumTime;
    
    Calendar gcSimStartDate = null;
    
    private Variable lat_rho;
    private Variable lon_rho;
    private Variable lat_u;
    private Variable lon_u;
    private Variable lat_v;
    private Variable lon_v;
    private Variable s_rho;
    
    private Variable ocean_time;
    private Variable scrum_time;
    
    private Variable hc;
    private Variable Cs_r;
    private Variable sc_r;
    private Variable tend;
    
    private Variable theta_b;
    private Variable theta_s;
    private Variable Tcline;
    
    private Variable h;
    private Variable temp;
    private Variable salt;
    private Variable zeta;
    
    private Variable ubar;
    private Variable vbar;
    private Variable u;
    private Variable v;
    
    private double dModSimStartDate;
    public double getModSimStartDate() { return dModSimStartDate; }
    
    public ROMSInit(String url,ROMSGrid romsGrid, String ncepDate, int forcingTimeSteps) throws IOException, InvalidRangeException, NCRegridderException {
        this.url = url;
        this.romsGrid = romsGrid;
        
        gcSimStartDate = GregorianCalendar.getInstance();
        int year=Integer.parseInt(ncepDate.substring(0,4));
        int month=Integer.parseInt(ncepDate.substring(4,6));
        int day=Integer.parseInt(ncepDate.substring(6,8));
        
        gcSimStartDate = new GregorianCalendar(year,month-1,day,0,0,0);
        
        System.out.println("gcSimStartDate:"+year+"-"+month+"-"+day);
        
        double dSimStartDate = JulianDate.toJulian(gcSimStartDate);
        double dModOffset = JulianDate.get19680523();
        dModSimStartDate = dSimStartDate - dModOffset;
        System.out.println("dModSimStartDate:"+dModSimStartDate);

        
        ncfWritable =  NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, url, null);
        
        ncfWritable.addGroupAttribute(null,new Attribute("type", "Initial file"));
        ncfWritable.addGroupAttribute(null,new Attribute("title", "Initialization file (INI) used for foring of the ROMS model"));
        ncfWritable.addGroupAttribute(null,new Attribute("grd_file", romsGrid.getUrl()));
        ncfWritable.addGroupAttribute(null,new Attribute("source", "University of Napoli Parthenope Weather Centre http://meteo.uniparthenope.it"));
        
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        ncfWritable.addGroupAttribute(null, new Attribute("date", sdf.format(cal.getTime())));
               
        dimOceanTime=ncfWritable.addDimension(null,"ocean_time", forcingTimeSteps);
        dimScrumTime=ncfWritable.addDimension(null,"scrum_time", forcingTimeSteps);
        dimEtaRho=ncfWritable.addDimension(null,romsGrid.dimEtaRho.getName(), romsGrid.dimEtaRho.getLength());
        dimXiRho=ncfWritable.addDimension(null,romsGrid.dimXiRho.getName(), romsGrid.dimXiRho.getLength());
        dimEtaU=ncfWritable.addDimension(null,romsGrid.dimEtaU.getName(), romsGrid.dimEtaU.getLength());
        dimXiU=ncfWritable.addDimension(null,romsGrid.dimXiU.getName(), romsGrid.dimXiU.getLength());
        dimEtaV=ncfWritable.addDimension(null,romsGrid.dimEtaV.getName(), romsGrid.dimEtaV.getLength());
        dimXiV=ncfWritable.addDimension(null,romsGrid.dimXiV.getName(), romsGrid.dimXiV.getLength());
        dimSRho=ncfWritable.addDimension(null,"s_rho", romsGrid.getSRho().length);
                
        dimOne=ncfWritable.addDimension(null,"one", 1);
                
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
                
        temp=ncfWritable.addVariable(null,"temp", DataType.DOUBLE, "ocean_time s_rho eta_rho xi_rho");
        temp.addAttribute(new Attribute("long_name","potential temperature"));
        temp.addAttribute(new Attribute("units","Celsius"));
        temp.addAttribute(new Attribute("coordinates","lon_rho lat_rho sc_r ocean_time"));
        temp.addAttribute(new Attribute("_FillValue", 1e37f));
        temp.addAttribute(new Attribute("time", "ocean_time"));
        
        salt=ncfWritable.addVariable(null,"salt", DataType.DOUBLE, "ocean_time s_rho eta_rho xi_rho");
        salt.addAttribute(new Attribute("long_name","salinity"));
        salt.addAttribute(new Attribute("units","PSU"));
        salt.addAttribute(new Attribute("coordinates","lon_rho lat_rho sc_r ocean_time"));
        salt.addAttribute(new Attribute("_FillValue", 1e37f));
        salt.addAttribute(new Attribute("time", "ocean_time"));
                
        ubar=ncfWritable.addVariable(null,"ubar", DataType.DOUBLE, "ocean_time eta_u xi_u");
        ubar.addAttribute(new Attribute("long_name","vertically integrated u-momentum component"));
        ubar.addAttribute(new Attribute("units","meter second-1"));
        ubar.addAttribute(new Attribute("coordinates","lon_u lat_u ocean_time"));
        ubar.addAttribute(new Attribute("_FillValue", 1e37f));
        ubar.addAttribute(new Attribute("time", "ocean_time"));
        
        vbar=ncfWritable.addVariable(null,"vbar", DataType.DOUBLE, "ocean_time eta_v xi_v");
        vbar.addAttribute(new Attribute("long_name","vertically integrated v-momentum component"));
        vbar.addAttribute(new Attribute("units","meter second-1"));
        vbar.addAttribute(new Attribute("coordinates","lon_v lat_v ocean_time"));
        vbar.addAttribute(new Attribute("_FillValue", 1e37f));
        vbar.addAttribute(new Attribute("time", "ocean_time"));
         
        u=ncfWritable.addVariable(null,"u", DataType.DOUBLE, "ocean_time s_rho eta_u xi_u");
        u.addAttribute(new Attribute("long_name","u-momentum component"));
        u.addAttribute(new Attribute("units","meter second-1"));
        u.addAttribute(new Attribute("coordinates","lon_u lat_u sc_r ocean_time"));
        u.addAttribute(new Attribute("_FillValue", 1e37f));
        
        v=ncfWritable.addVariable(null,"v", DataType.DOUBLE, "ocean_time s_rho eta_v xi_v");
        v.addAttribute(new Attribute("long_name","v-momentum component"));
        v.addAttribute(new Attribute("units","meter second-1"));
        v.addAttribute(new Attribute("coordinates","lon_v lat_v sc_r ocean_time"));
        v.addAttribute(new Attribute("_FillValue", 1e37f));
        v.addAttribute(new Attribute("time", "ocean_time"));
                
        zeta=ncfWritable.addVariable(null,"zeta", DataType.DOUBLE, "ocean_time eta_rho xi_rho");
        zeta.addAttribute(new Attribute("long_name","free-surface"));
        zeta.addAttribute(new Attribute("units","meter"));
        zeta.addAttribute(new Attribute("coordinates","lon_rho lat_rho ocean_time"));
        zeta.addAttribute(new Attribute("_FillValue", 1e37f));
        zeta.addAttribute(new Attribute("time", "ocean_time"));
        
        theta_b=ncfWritable.addVariable(null,"theta_b", DataType.DOUBLE, "one");
        theta_b.addAttribute(new Attribute("long_name","S-coordinate surface control parameter"));
        theta_b.addAttribute(new Attribute("units","nondimensional"));
          
        theta_s=ncfWritable.addVariable(null,"theta_s", DataType.DOUBLE, "one");
        theta_s.addAttribute(new Attribute("long_name","S-coordinate bottom control parameter"));
        theta_s.addAttribute(new Attribute("units","nondimensional"));
      
        Tcline=ncfWritable.addVariable(null,"Tcline", DataType.DOUBLE, "");
         
        ocean_time=ncfWritable.addVariable(null,"ocean_time", DataType.DOUBLE, "ocean_time");
        ocean_time.addAttribute(new Attribute("long_name", "ocean forcing time"));
        ocean_time.addAttribute(new Attribute("units", "days since 1968-05-23 00:00:00 GMT"));
        ocean_time.addAttribute(new Attribute("calendar", "gregorian"));
                
        hc=ncfWritable.addVariable(null,"hc", DataType.DOUBLE, "one");
        hc.addAttribute(new Attribute("long_name","S-coordinate parameter, critical depth"));
        hc.addAttribute(new Attribute("units","meter"));
        
        scrum_time=ncfWritable.addVariable(null,"scrum_time", DataType.DOUBLE, "scrum_time");
        scrum_time.addAttribute(new Attribute("long_name","time since initialization"));
        scrum_time.addAttribute(new Attribute("units","second"));
        
        tend=ncfWritable.addVariable(null,"tend", DataType.DOUBLE, "one");
        tend.addAttribute(new Attribute("long_name","end processing day"));
        tend.addAttribute(new Attribute("units","day"));
        
        Cs_r=ncfWritable.addVariable(null,"Cs_r", DataType.DOUBLE, "s_rho");
        Cs_r.addAttribute(new Attribute("long_name","S-coordinate stretching curves at RHO-points"));
        Cs_r.addAttribute(new Attribute("units","nondimensional"));
        
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
        
        ncfWritable.create();

    }
    
    public void make() throws IOException, InvalidRangeException, NCRegridderException {
        
        
        int etaRho = dimEtaRho.getLength();
        int xiRho = dimXiRho.getLength();
        int etaU = dimEtaU.getLength();
        int xiU = dimXiU.getLength();
        int etaV = dimEtaV.getLength();
        int xiV = dimXiV.getLength();
        int sRho = dimSRho.getLength();
        int one = dimOne.getLength();
        int oceanTime = dimOceanTime.getLength();
        int scrumTime = dimScrumTime.getLength();

        ArrayDouble.D1 outAThetaS = new ArrayDouble.D1(one);
        ArrayDouble.D1 outAThetaB = new ArrayDouble.D1(one);
        ArrayDouble.D1 outATEnd = new ArrayDouble.D1(one);
        ArrayDouble.D1 outAScrumTime = new ArrayDouble.D1(scrumTime);
        ArrayDouble.D1 outAOceanTime = new ArrayDouble.D1(oceanTime);
        ArrayDouble.D1 outASCR = new ArrayDouble.D1(sRho);
        ArrayDouble.D1 outAHC = new ArrayDouble.D1(one);
        ArrayDouble.D1 outACSR = new ArrayDouble.D1(sRho);
        
        ArrayDouble.D0 outATCline = new ArrayDouble.D0();
        
        for (int t=0;t<oceanTime;t++) {
            outAOceanTime.set(t,OCEAN_TIME[t]);
            outAScrumTime.set(t,SCRUM_TIME[t]);
        }
        
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
        ncfWritable.write(tend, new int [] { 0 }, outATEnd);
        ncfWritable.write(Tcline, outATCline);
        
        ncfWritable.write(scrum_time, new int [] { 0 }, outAScrumTime);
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
        int oceanTime = dimOceanTime.getLength();


        ArrayDouble.D4 outAU = new ArrayDouble.D4(1,sRho,etaU, xiU);
        ArrayDouble.D4 outAV = new ArrayDouble.D4(1,sRho,etaV, xiV);
        ArrayDouble.D4 outASALT = new ArrayDouble.D4(1,sRho,etaRho, xiRho);
        ArrayDouble.D4 outATEMP = new ArrayDouble.D4(1,sRho,etaRho, xiRho);
        ArrayDouble.D3 outAUBAR = new ArrayDouble.D3(1,etaU, xiU);
        ArrayDouble.D3 outAVBAR = new ArrayDouble.D3(1,etaV, xiV);
        ArrayDouble.D3 outAZETA = new ArrayDouble.D3(1,etaRho, xiRho);
   
        
        
        // outAOceanTime.set(t,(86400*(dModSimStartDate+t)));
        // outAScrumTime.set(t,(86400*(dModSimStartDate+t)));
        
        
        
        
            
            
      
        for (int k=0;k<sRho;k++) {
            for (int j=0;j<etaU;j++) {
                for (int i=0;i<xiU;i++) {
                    outAU.set(0,k,j,i,U[k][j][i]);
                }
            }

            for (int j=0;j<etaV;j++) {
                for (int i=0;i<xiV;i++) {
                    outAV.set(0,k,j,i,V[k][j][i]);
                }
            }

            for (int j=0;j<etaRho;j++) {
                for (int i=0;i<xiRho;i++) {
                outATEMP.set(0,k,j,i,TEMP[k][j][i]);
                outASALT.set(0,k,j,i,SALT[k][j][i]);
                }
            }
        }

        for (int j=0;j<etaU;j++) {
            for (int i=0;i<xiU;i++) {
                outAUBAR.set(0,j,i,UBAR[j][i]);
            }
        }

        for (int j=0;j<etaV;j++) {
            for (int i=0;i<xiV;i++) {
                outAVBAR.set(0,j,i,VBAR[j][i]);
            }
        }

        for (int j=0;j<etaRho;j++) {
            for (int i=0;i<xiRho;i++) {
                outAZETA.set(0,j,i,ZETA[j][i]);
            }
        }
        
        
        
        //ncfWritable.write("u", outAU);
        ncfWritable.write(u, new int [] { localTime,0,0,0 }, outAU);
        //ncfWritable.write("v", outAV);
        ncfWritable.write(v, new int [] { localTime,0,0,0 }, outAV);
        //ncfWritable.write("ubar", outAUBAR);
        ncfWritable.write(ubar, new int [] { localTime,0,0 }, outAUBAR);
        //ncfWritable.write("vbar", outAVBAR);
        ncfWritable.write(vbar, new int [] { localTime,0,0 }, outAVBAR);
        //ncfWritable.write("zeta", outAZETA);
        ncfWritable.write(zeta, new int [] { localTime,0,0,0 }, outAZETA);
        //ncfWritable.write("temp", outATEMP);
        ncfWritable.write(temp, new int [] { localTime,0,0,0 }, outATEMP);
        //ncfWritable.write("salt", outASALT);
        ncfWritable.write(salt, new int [] { localTime,0,0,0 }, outASALT);
        
        ncfWritable.flush();
    }
    
    public void close() throws IOException {
        ncfWritable.close();
    }
     
    private double[] a3D21D(double[][][] a3D) {
        int nK=a3D.length;
        int nJ=a3D[0].length;
        int nI=a3D[0][0].length;
        
        double[] a1D = new double[nK*nJ*nI];
        int count=0;
        
        for (int k=0;k<nK;k++) {
            for (int j=0;j<nJ;j++) {
                for (int i=0;i<nI;i++) {
                    a1D[count]=a3D[k][j][i];
                    count++;
                }
            }
        } 
        return a1D;
    }
    
    private double[] a2D21D(double[][] a2D) {
        int nJ=a2D.length;
        int nI=a2D[0].length;
        
        double[] a1D = new double[nJ*nI];
        int count=0;
        
        for (int j=0;j<nJ;j++) {
            for (int i=0;i<nI;i++) {
                a1D[count]=a2D[j][i];
                count++;
            }
        }
         
        return a1D;
    }
    
    public double[] getDataArrayByVariableId(int variableId) throws NCRegridderException {
           double[] values=null;
        
        switch (variableId) {
            case VARIABLE_TEMP:
                values = a3D21D(TEMP);
                break;
            case VARIABLE_SALT:
                values = a3D21D(SALT);
                break;
            case VARIABLE_U:
                values = a3D21D(U);
                break;
            case VARIABLE_V:
                values = a3D21D(V);
                break;
            case VARIABLE_UBAR:
                values = a2D21D(UBAR);
                break;
            case VARIABLE_VBAR:
                values = a2D21D(VBAR);
                break;
            case VARIABLE_ZETA:
                values = a2D21D(ZETA);
                break;
        
        }
        
        
        
        if (values==null) throw new NCRegridderException("The values array is null!");
        
        
        return values;
    }
    
    public boolean isNoData(double a) {
        if (a==noData) return true;
        return false;
    }
  
    public double getMin(int variableId) throws NCRegridderException {
        double[] values = getDataArrayByVariableId(variableId);
     
        double result = Double.NaN;
        for(double a : values) {
            if (Double.isNaN(a)==false && isNoData(a)==false) {
                if (Double.isNaN(result)==true) {
                    result=a;
                } else {
                    if (a<result) {
                        result=a;
                    }
                }
                
            }
        }
        if (Double.isNaN(result)==true) throw new NCRegridderException("The min is NaN!");
        return result;
    }
    
    public double getMax(int variableId) throws NCRegridderException {
        double[] values = getDataArrayByVariableId(variableId);
     
        double result = Double.NaN;
        for(double a : values) {
            if (Double.isNaN(a)==false && isNoData(a)==false) {
                if (Double.isNaN(result)==true) {
                    result=a;
                } else {
                    if (a>result) {
                        result=a;
                    }
                }
                
            }
        }
        if (Double.isNaN(result)==true) throw new NCRegridderException("The max is NaN!");
        return result;
    }
}
