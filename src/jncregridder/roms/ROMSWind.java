
/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
*/
package jncregridder.roms;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import jncregridder.util.IDWInterpolator;
import jncregridder.util.InterpolatorBase;
import jncregridder.util.InterpolatorException;
import jncregridder.util.InterpolatorParams;
import jncregridder.util.JulianDate;
import jncregridder.util.Linear2DInterpolator;
import jncregridder.util.NCRegridderException;
import jncregridder.wrf.WRFData;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayDouble;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;


/**
*
* @author raffaelemontella
*/
public class ROMSWind {
   private ROMSGrid romsGrid;


   private static double MACHINE_D_EPSILON;


   protected void initMachineDEpsilon() {
       float fTmp = 0.5f;
       double dTmp = 0.5d;
       while( 1 + fTmp > 1 )
           fTmp = fTmp / 2;
       while( 1 + dTmp > 1 )
           dTmp = dTmp / 2;
       MACHINE_D_EPSILON = dTmp;

   }


   private final static double D_TOLERANCE = MACHINE_D_EPSILON * 10d;

   /**
    *A tolerance.
    */
   private static final double TOLL = 1.0d * 10E-8;


   private NetcdfFileWriter ncfWriterUVStress;

    private InterpolatorBase interpRho = null;
    private InterpolatorBase interpU = null;
    private InterpolatorBase interpV = null;
    private InterpolatorBase interpWRF2XY = null;

   private Variable time=null;

   private Variable Pair=null;
   private Variable Tair=null;
   private Variable Qair=null;

   // private Variable cloud;
   private Variable rain=null;
   private Variable swrad=null;
   //private Variable lwrad;
   private Variable lwrad_down=null;

   private Variable Uwind=null;
   private Variable Vwind=null;

   private Variable ocean_time=null;
   private Variable sustr=null;
   private Variable svstr=null;

   private Variable lat=null;
   private Variable lon=null;
   private Variable lat_u=null;
   private Variable lon_u=null;
   private Variable lat_v=null;
   private Variable lon_v=null;


    private int localTime=0;

    public void setTime(int localTime) {

        this.localTime = localTime;
    }




   public ROMSWind(String urlUVStress,ROMSGrid romsGrid) throws IOException, InvalidRangeException, NCRegridderException, InterpolatorException {
       this.romsGrid=romsGrid;
       ncfWriterUVStress = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4_classic, urlUVStress, null);
   }




   private double[][] interpWRF2ROMS(InterpolatorBase interpWRF2XY, InterpolatorBase interpROMS, double[][] src, double srcMissingValue, double dstMissingValue) throws InterpolatorException {
       double[][] tmp = interpWRF2XY.interp(src, srcMissingValue, dstMissingValue, null);
       double[][] dst = interpROMS.interp(tmp, srcMissingValue, dstMissingValue,null);
       return dst;
   }


    public void init(WRFData wrfData, int wrfTimeOffset) throws NCRegridderException, IOException, InterpolatorException, InvalidRangeException {
        initMachineDEpsilon();


        romsGrid.load(ROMSGrid.VARIABLE_LATRHO);
        romsGrid.load(ROMSGrid.VARIABLE_LONRHO);
        romsGrid.load(ROMSGrid.VARIABLE_ANGLE);
        romsGrid.load(ROMSGrid.VARIABLE_LATU);
        romsGrid.load(ROMSGrid.VARIABLE_LATV);
        romsGrid.load(ROMSGrid.VARIABLE_LONU);
        romsGrid.load(ROMSGrid.VARIABLE_LONV);




        ncfWriterUVStress.addDimension(null,romsGrid.dimEtaRho.getName(), romsGrid.dimEtaRho.getLength());
        ncfWriterUVStress.addDimension(null,romsGrid.dimXiRho.getName(), romsGrid.dimXiRho.getLength());
        ncfWriterUVStress.addDimension(null,romsGrid.dimEtaU.getName(), romsGrid.dimEtaU.getLength());
        ncfWriterUVStress.addDimension(null,romsGrid.dimXiU.getName(), romsGrid.dimXiU.getLength());
        ncfWriterUVStress.addDimension(null,romsGrid.dimEtaV.getName(), romsGrid.dimEtaV.getLength());
        ncfWriterUVStress.addDimension(null,romsGrid.dimXiV.getName(), romsGrid.dimXiV.getLength());

        ncfWriterUVStress.addUnlimitedDimension("ocean_time");

        lat=ncfWriterUVStress.addVariable(null,"lat", DataType.DOUBLE, "eta_rho xi_rho");
        lat.addAttribute(new Attribute("long_name","latitude of RHO-points"));
        lat.addAttribute(new Attribute("units","degree_north"));
        lat.addAttribute(new Attribute("field","lat_rho, scalar"));
        lat.addAttribute(new Attribute("standard_name","latitude"));
        lat.addAttribute(new Attribute("_CoordinateAxisType", "Lat"));

        lon=ncfWriterUVStress.addVariable(null,"lon", DataType.DOUBLE, "eta_rho xi_rho");
        lon.addAttribute(new Attribute("long_name","longitude of RHO-points"));
        lon.addAttribute(new Attribute("units","degree_east"));
        lon.addAttribute(new Attribute("field","lon_rho, scalar"));
        lon.addAttribute(new Attribute("standard_name","longitude"));
        lon.addAttribute(new Attribute("_CoordinateAxisType", "Lon"));

        time=ncfWriterUVStress.addVariable(null,"time", DataType.DOUBLE, "ocean_time");
        time.addAttribute(new Attribute("long_name", "atmospheric forcing time"));
        time.addAttribute(new Attribute("units", "days since 1968-05-23 00:00:00 GMT"));
        time.addAttribute(new Attribute("calendar", "gregorian"));


        Pair=ncfWriterUVStress.addVariable(null,"Pair", DataType.FLOAT, "ocean_time eta_rho xi_rho");
        Pair.addAttribute(new Attribute("long_name","Mean Sea Level Pressure"));
        Pair.addAttribute(new Attribute("units","millibar"));
        Pair.addAttribute(new Attribute("time","time"));
        Pair.addAttribute(new Attribute("_FillValue", 1e37f));

        Tair=ncfWriterUVStress.addVariable(null,"Tair", DataType.FLOAT, "ocean_time eta_rho xi_rho");
        Tair.addAttribute(new Attribute("long_name","Air Temperature (2m)"));
        Tair.addAttribute(new Attribute("units","Celsius"));
        Tair.addAttribute(new Attribute("time","time"));
        Tair.addAttribute(new Attribute("_FillValue", 1e37f));

        Qair=ncfWriterUVStress.addVariable(null,"Qair", DataType.FLOAT, "ocean_time eta_rho xi_rho");
        Qair.addAttribute(new Attribute("long_name","Relative Humidity (2m)"));
        Qair.addAttribute(new Attribute("units","percentage"));
        Qair.addAttribute(new Attribute("time","time"));
        Qair.addAttribute(new Attribute("_FillValue", 1e37f));

        rain=ncfWriterUVStress.addVariable(null,"rain", DataType.FLOAT, "ocean_time eta_rho xi_rho");
        rain.addAttribute(new Attribute("long_name","Rain fall rate"));
        rain.addAttribute(new Attribute("units","kilogram meter-2 second-1"));
        rain.addAttribute(new Attribute("time","time"));
        rain.addAttribute(new Attribute("_FillValue", 1e37f));

        swrad=ncfWriterUVStress.addVariable(null,"swrad", DataType.FLOAT, "ocean_time eta_rho xi_rho");
        swrad.addAttribute(new Attribute("long_name","Solar showtwave radiation"));
        swrad.addAttribute(new Attribute("units","watt meter-2"));
        swrad.addAttribute(new Attribute("time","time"));
        swrad.addAttribute(new Attribute("positive_value","downward flux, heating"));
        swrad.addAttribute(new Attribute("negative_value","upward flux, cooling"));
        swrad.addAttribute(new Attribute("_FillValue", 1e37f));

        lwrad_down=ncfWriterUVStress.addVariable(null,"lwrad_down", DataType.FLOAT, "ocean_time eta_rho xi_rho");
        lwrad_down.addAttribute(new Attribute("long_name","Net longwave radiation flux"));
        lwrad_down.addAttribute(new Attribute("units","watt meter-2"));
        lwrad_down.addAttribute(new Attribute("time","time"));
        lwrad_down.addAttribute(new Attribute("positive_value","downward flux, heating"));
        lwrad_down.addAttribute(new Attribute("negative_value","upward flux, cooling"));
        lwrad_down.addAttribute(new Attribute("_FillValue", 1e37f));

        Uwind=ncfWriterUVStress.addVariable(null,"Uwind", DataType.FLOAT, "ocean_time eta_rho xi_rho");
        Uwind.addAttribute(new Attribute("long_name", "Wind velocity, u-component (m s-1)"));
        Uwind.addAttribute(new Attribute("units", "m s-1"));
        Uwind.addAttribute(new Attribute("time","time"));
        Uwind.addAttribute(new Attribute("_FillValue", 1e37f));

        Vwind=ncfWriterUVStress.addVariable(null,"Vwind", DataType.FLOAT, "ocean_time eta_rho xi_rho");
        Vwind.addAttribute(new Attribute("long_name", "Wind velocity, v-component (m s-1)"));
        Vwind.addAttribute(new Attribute("units", "m s-1"));
        Vwind.addAttribute(new Attribute("time","time"));
        Vwind.addAttribute(new Attribute("_FillValue", 1e37f));

        lat_u=ncfWriterUVStress.addVariable(null,"lat_u", DataType.DOUBLE, "eta_u xi_u");
        lat_u.addAttribute(new Attribute("long_name","latitude of RHO-points"));
        lat_u.addAttribute(new Attribute("units","degree_north"));
        lat_u.addAttribute(new Attribute("standard_name","latitude"));
        lat_u.addAttribute(new Attribute("_CoordinateAxisType", "Lat"));

        lon_u=ncfWriterUVStress.addVariable(null,"lon_u", DataType.DOUBLE, "eta_u xi_u");
        lon_u.addAttribute(new Attribute("long_name","longitude of U-points"));
        lon_u.addAttribute(new Attribute("units","degree_east"));
        lon_u.addAttribute(new Attribute("standard_name","longitude"));
        lon_u.addAttribute(new Attribute("_CoordinateAxisType", "Lon"));

        lat_v=ncfWriterUVStress.addVariable(null,"lat_v", DataType.DOUBLE, "eta_v xi_v");
        lat_v.addAttribute(new Attribute("long_name","latitude of V-points"));
        lat_v.addAttribute(new Attribute("units","degree_north"));
        lat_v.addAttribute(new Attribute("standard_name","latitude"));
        lat_v.addAttribute(new Attribute("_CoordinateAxisType", "Lat"));

        lon_v=ncfWriterUVStress.addVariable(null,"lon_v", DataType.DOUBLE, "eta_v xi_v");
        lon_v.addAttribute(new Attribute("long_name","longitude of V-points"));
        lon_v.addAttribute(new Attribute("units","degree_east"));
        lon_v.addAttribute(new Attribute("standard_name","longitude"));
        lon_v.addAttribute(new Attribute("_CoordinateAxisType", "Lon"));

        ocean_time=ncfWriterUVStress.addVariable(null,"ocean_time", DataType.DOUBLE, "ocean_time");
        ocean_time.addAttribute(new Attribute("long_name", "surface ocean time"));
        ocean_time.addAttribute(new Attribute("units", "days since 1968-05-23 00:00:00 GMT"));
        ocean_time.addAttribute(new Attribute("calendar", "gregorian"));

        sustr=ncfWriterUVStress.addVariable(null,"sustr", DataType.FLOAT, "ocean_time eta_u xi_u");
        sustr.addAttribute(new Attribute("long_name", "Kinematic wind stress, u-component (m2 s-2)"));
        sustr.addAttribute(new Attribute("units", "Newton meter-2"));
        sustr.addAttribute(new Attribute("scale_factor", 1000.0));
        sustr.addAttribute(new Attribute("time","ocean_time"));
        sustr.addAttribute(new Attribute("_FillValue", 1e37f));

        svstr=ncfWriterUVStress.addVariable(null,"svstr", DataType.FLOAT, "ocean_time eta_v xi_v");
        svstr.addAttribute(new Attribute("long_name", "Kinematic wind stress, v-component (m2 s-2)"));
        svstr.addAttribute(new Attribute("units", "Newton meter-2"));
        svstr.addAttribute(new Attribute("scale_factor", 1000.0));
        svstr.addAttribute(new Attribute("time","ocean_time"));
        svstr.addAttribute(new Attribute("_FillValue", 1e37f));

        ncfWriterUVStress.create();


        System.out.println(romsGrid.dimEtaRho.getLength()+","+romsGrid.dimXiRho.getLength());


        int etaRho = romsGrid.dimEtaRho.getLength();
        int xiRho = romsGrid.dimXiRho.getLength();
        int etaU = romsGrid.dimEtaU.getLength();
        int xiU = romsGrid.dimXiU.getLength();
        int etaV = romsGrid.dimEtaV.getLength();
        int xiV = romsGrid.dimXiV.getLength();


        // MASK at rho points
        double[][] MASKRHO = romsGrid.getMASKRHO();
        if (MASKRHO==null) {
            throw new NCRegridderException("MASKRHO is null");
        }

        // MASK at u points
        double[][] MASKU = romsGrid.getMASKU();
        if (MASKU==null) {
            throw new NCRegridderException("MASKU is null");
        }

        // MASK at v points
        double[][] MASKV = romsGrid.getMASKV();
        if (MASKV==null) {
            throw new NCRegridderException("MASKV is null");
        }

        // Binary masks
        int[][] dstMASKRHO = new int[etaRho][xiRho];
        int[][] dstMASKU = new int[etaU][xiU];
        int[][] dstMASKV = new int[etaV][xiV];


        // Preparing rho destination mask
        for (int j=0;j<etaRho;j++) {
            for (int i=0;i<xiRho;i++) {
                if (MASKRHO[j][i]==1) {
                    dstMASKRHO[j][i]=1;
                } else {
                    dstMASKRHO[j][i]=0;
                }
            }
        }

        // Preparing u destination mask
        for (int j=0;j<etaU;j++) {
            for (int i=0;i<xiU;i++) {
                if (MASKU[j][i]==1) {
                    dstMASKU[j][i]=1;
                } else {
                    dstMASKU[j][i]=0;
                }
            }
        }

        // Preparing v destination mask
        for (int j=0;j<etaV;j++) {
            for (int i=0;i<xiV;i++) {
                if (MASKV[j][i]==1) {
                    dstMASKV[j][i]=1;
                } else {
                    dstMASKV[j][i]=0;
                }
            }
        }


        double[][][] latLongs=wrfData.getLATLONGs();
        double[][] wrfXYLAT=latLongs[0];
        double[][] wrfXYLON=latLongs[1];

        double latStep = wrfXYLAT[1][0]-wrfXYLAT[0][0];
        double lonStep = wrfXYLON[0][1]-wrfXYLON[0][0];
        double radius = 4*Math.min(latStep, lonStep);


        InterpolatorParams paramsWRF2XY=new InterpolatorParams();
        paramsWRF2XY.put("radius", radius);

        InterpolatorParams paramsXY2ROMS=new InterpolatorParams();
        paramsXY2ROMS.put("radfactor",9.);


        interpWRF2XY = new IDWInterpolator(wrfData.getXLAT(),wrfData.getXLONG(), wrfXYLAT, wrfXYLON, null, null,paramsWRF2XY);


        interpRho = new Linear2DInterpolator(
                wrfXYLAT,wrfXYLON,
                romsGrid.getLATRHO(),romsGrid.getLONRHO(),null,dstMASKRHO,paramsXY2ROMS);


        interpU = new Linear2DInterpolator(
                wrfXYLAT,wrfXYLON,
                romsGrid.getLATU(),romsGrid.getLONU(),null,dstMASKU,paramsXY2ROMS);

        interpV = new Linear2DInterpolator(
                wrfXYLAT,wrfXYLON,
                romsGrid.getLATV(),romsGrid.getLONV(),null,dstMASKV,paramsXY2ROMS);




        ArrayDouble.D2 outALonRho = new ArrayDouble.D2(etaRho,xiRho);
        ArrayDouble.D2 outALatRho = new ArrayDouble.D2(etaRho,xiRho);
        ArrayDouble.D2 outALonU = new ArrayDouble.D2(etaU,xiU);
        ArrayDouble.D2 outALatU = new ArrayDouble.D2(etaU,xiU);
        ArrayDouble.D2 outALonV = new ArrayDouble.D2(etaV,xiV);
        ArrayDouble.D2 outALatV = new ArrayDouble.D2(etaV,xiV);


        double[][] LONRHO=romsGrid.getLONRHO();
        double[][] LATRHO=romsGrid.getLATRHO();
        double[][] LONU=romsGrid.getLONU();
        double[][] LATU=romsGrid.getLATU();
        double[][] LONV=romsGrid.getLONV();
        double[][] LATV=romsGrid.getLATV();


        for(int j=0;j<etaRho;j++) {
            for (int i=0;i<xiRho;i++) {
                outALonRho.set(j, i, LONRHO[j][i]);
                outALatRho.set(j, i, LATRHO[j][i]);
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

        ncfWriterUVStress.write(lon, new int[]{0, 0}, outALonRho);
        ncfWriterUVStress.write(lat, new int[]{0, 0}, outALatRho);
        ncfWriterUVStress.write(lon_u, new int[]{0, 0}, outALonU);
        ncfWriterUVStress.write(lat_u, new int[]{0, 0}, outALatU);
        ncfWriterUVStress.write(lon_v, new int[]{0, 0}, outALonV);
        ncfWriterUVStress.write(lat_v, new int[]{0, 0}, outALatV);

        localTime=0;
    }

    public void add(WRFData wrfData, int wrfTimeOffset) throws NCRegridderException, InterpolatorException, IOException, InvalidRangeException  {

        int etaRho = romsGrid.dimEtaRho.getLength();
        int xiRho = romsGrid.dimXiRho.getLength();
        int etaU = romsGrid.dimEtaU.getLength();
        int xiU = romsGrid.dimXiU.getLength();
        int etaV = romsGrid.dimEtaV.getLength();
        int xiV = romsGrid.dimXiV.getLength();

        ArrayDouble.D1 outAWindTime = new ArrayDouble.D1(1);
        ArrayDouble.D1 outASmsTime = new ArrayDouble.D1(1);
        ArrayDouble.D1 outATAirTime = new ArrayDouble.D1(1);
        ArrayDouble.D1 outAPAirTime = new ArrayDouble.D1(1);
        ArrayDouble.D1 outAQAirTime = new ArrayDouble.D1(1);
        ArrayDouble.D1 outACloudTime = new ArrayDouble.D1(1);
        ArrayDouble.D1 outARainTime = new ArrayDouble.D1(1);
        ArrayDouble.D1 outASrfTime = new ArrayDouble.D1(1);

        ArrayDouble.D1[] outATimes = new ArrayDouble.D1[8];
        outATimes[0] = outAWindTime;
        outATimes[1] = outASmsTime;
        outATimes[2] = outATAirTime;
        outATimes[3] = outAPAirTime;
        outATimes[4] = outAQAirTime;
        outATimes[5] = outACloudTime;
        outATimes[6] = outARainTime;
        outATimes[7] = outASrfTime;

        for (int t=wrfTimeOffset;t<wrfData.dimTime.getLength();t++) {

            ArrayFloat.D3 outATAir = new ArrayFloat.D3(1,etaRho, xiRho);
            ArrayFloat.D3 outAPAir = new ArrayFloat.D3(1,etaRho, xiRho);
            ArrayFloat.D3 outAQAir = new ArrayFloat.D3(1,etaRho, xiRho);
            ArrayFloat.D3 outACloud = new ArrayFloat.D3(1,etaRho, xiRho);
            ArrayFloat.D3 outARain = new ArrayFloat.D3(1,etaRho, xiRho);
            ArrayFloat.D3 outASWRad = new ArrayFloat.D3(1,etaRho, xiRho);
            ArrayFloat.D3 outALWRadDown = new ArrayFloat.D3(1,etaRho, xiRho);
            //ArrayFloat.D3 outALWRad = new ArrayFloat.D3(1,etaRho, xiRho);
            ArrayFloat.D3 outAU10m = new ArrayFloat.D3(1,etaRho, xiRho);
            ArrayFloat.D3 outAV10m = new ArrayFloat.D3(1,etaRho, xiRho);


            ArrayFloat.D3 outASustr = new ArrayFloat.D3(1,etaU, xiU);
            ArrayFloat.D3 outASvstr = new ArrayFloat.D3(1,etaV, xiV);

            double[][] angleMatrix = romsGrid.getANGLE();
            double[][] t2mMatrix = null;
            double[][] rh2Matrix = null;
            double[][] slpMatrix = null;
            double[][] clfrMatrix = null;
            double[][] rainMatrix = null;
            double[][] swradMatrix = null;
            //double[][] lwradMatrix = null;
            double[][] lwrad_downMatrix = null;
            double[][] u10mMatrix = null;
            double[][] v10mMatrix = null;
            //double[][] ustMatrix = null;

            wrfData.setTime(t);

            double dDate = JulianDate.toJulian(wrfData.getDate());
            double dModOffset = JulianDate.get19680523();
            double dModDate = dDate - dModOffset;

            double dModJulianDate = dModDate ;
            for (ArrayDouble.D1 outATime:outATimes) {
                outATime.set(0, dModJulianDate);
            }

            System.out.println("tCount=:"+localTime + " dModJulianDate="+dModJulianDate);



            wrfData.calcSlp();
            wrfData.calcUVMet();

            System.out.println("Begin interpolation...");
            System.out.println("etaRho,xiRho");



            t2mMatrix = interpWRF2ROMS(interpWRF2XY, interpRho, wrfData.getT2(), 1e20, 1e37);
            slpMatrix = interpWRF2ROMS(interpWRF2XY, interpRho, wrfData.getSLP(), 1e20, 1e37);
            u10mMatrix = interpWRF2ROMS(interpWRF2XY, interpRho, wrfData.getU10M(), 1e20, 1e37);
            v10mMatrix = interpWRF2ROMS(interpWRF2XY, interpRho, wrfData.getV10M(), 1e20, 1e37);
            //ustMatrix = interpWRF2ROMS(interpWRF2XY, interpRho, wrfData.getUST(), 1e20, 1e37);


            for (int i=0;i<t2mMatrix.length;i++) {
                for (int j=0;j<t2mMatrix[0].length;j++) {
                    t2mMatrix[i][j]=t2mMatrix[i][j]-273.15;
                }
            }




            rh2Matrix        = interpWRF2ROMS(interpWRF2XY, interpRho, wrfData.getRH2(), 1e20, 1e37);
            clfrMatrix       = interpWRF2ROMS(interpWRF2XY, interpRho, wrfData.getCLFR(), 1e20, 1e37);
            rainMatrix       = interpWRF2ROMS(interpWRF2XY, interpRho, wrfData.getInstRain(), 1e20, 1e37);
            swradMatrix      = interpWRF2ROMS(interpWRF2XY, interpRho, wrfData.getSWDOWN(), 1e20, 1e37);
            lwrad_downMatrix = interpWRF2ROMS(interpWRF2XY, interpRho, wrfData.getGLW(), 1e20, 1e37);
            //lwrad = interpWRF2ROMS(interpWRF2XY, interpRho, wrfData.getGLW(), 1e20, 1e37);

            System.out.println("t2m:"+t2mMatrix[0][0]);
            System.out.println("slp:"+slpMatrix[0][0]);
            System.out.println("u10m,v10m:"+u10mMatrix[0][0]+" "+v10mMatrix[0][0]);
            System.out.println("rh2:"+rh2Matrix[0][0]);
            System.out.println("clfr:"+clfrMatrix[0][0]);
            System.out.println("rain:"+rainMatrix[0][0]);
            System.out.println("swrad:"+swradMatrix[0][0]);
            System.out.println("lwrad_down:"+lwrad_downMatrix[0][0]);
            //System.out.println("lwrad:"+lwrad[0][0]);

            System.out.println("------");


            for (int j=0;j<etaRho;j++) {
                for (int i=0;i<xiRho;i++) {
                    //double rotU10m=u10m[j][i]*Math.cos(-angle[j][i])-v10m[j][i]*Math.sin(-angle[j][i]);
                    //double rotV10m=-u10m[j][i]*Math.sin(-angle[j][i])-v10m[j][i]*Math.cos(-angle[j][i]);

                    double rotU10m = (u10mMatrix[j][i] * Math.cos(-angleMatrix[j][i]) + v10mMatrix[j][i] * Math.sin(-angleMatrix[j][i]));
                    double rotV10m = (v10mMatrix[j][i] * Math.cos(-angleMatrix[j][i]) - u10mMatrix[j][i] * Math.sin(-angleMatrix[j][i]));

                    outAU10m.set(0,j, i, (float)rotU10m);
                    outAV10m.set(0,j, i, (float)rotV10m);
                    outATAir.set(0,j, i, (float)t2mMatrix[j][i]);
                    outAPAir.set(0,j, i, (float)slpMatrix[j][i]);
                    outAQAir.set(0,j, i, (float)rh2Matrix[j][i]);
                    outACloud.set(0,j, i, (float)clfrMatrix[j][i]);
                    outARain.set(0,j, i, (float)rainMatrix[j][i]);
                    outACloud.set(0,j, i, (float)clfrMatrix[j][i]);
                    outASWRad.set(0,j, i, (float)swradMatrix[j][i]);
                    outALWRadDown.set(0,j, i, (float)lwrad_downMatrix[j][i]);

                }
            }


            System.out.println("etaU,xiU");



            t2mMatrix  = interpWRF2ROMS(interpWRF2XY, interpU, wrfData.getT2(), 1e20, 1e37);
            slpMatrix  = interpWRF2ROMS(interpWRF2XY, interpU, wrfData.getSLP(), 1e20, 1e37);
            u10mMatrix = interpWRF2ROMS(interpWRF2XY, interpU, wrfData.getU10M(), 1e20, 1e37);
            v10mMatrix = interpWRF2ROMS(interpWRF2XY, interpU, wrfData.getV10M(), 1e20, 1e37);
            //ustMatrix  = interpWRF2ROMS(interpWRF2XY, interpU, wrfData.getUST(), 1e20, 1e37);

            for (int i=0;i<t2mMatrix.length;i++) {
                for (int j=0;j<t2mMatrix[0].length;j++) {
                    t2mMatrix[i][j]=t2mMatrix[i][j]-273.15;
                }
            }

            for (int j=0;j<etaU;j++) {
                for (int i=0;i<xiU;i++) {
                    float sustr =calcStress(u10mMatrix[j][i],v10mMatrix[j][i],888.8,angleMatrix[j][i],slpMatrix[j][i],t2mMatrix[j][i])[0];
                    outASustr.set(0,j, i, sustr);
                }

            }

            System.out.println("etaV,xiV");


            t2mMatrix = interpWRF2ROMS(interpWRF2XY, interpV, wrfData.getT2(), 1e20, 1e37);
            slpMatrix = interpWRF2ROMS(interpWRF2XY, interpV, wrfData.getSLP(), 1e20, 1e37);
            u10mMatrix = interpWRF2ROMS(interpWRF2XY, interpV, wrfData.getU10M(), 1e20, 1e37);
            v10mMatrix = interpWRF2ROMS(interpWRF2XY, interpV, wrfData.getV10M(), 1e20, 1e37);
            //ustMatrix = interpWRF2ROMS(interpWRF2XY, interpV, wrfData.getUST(), 1e20, 1e37);


            for (int i=0;i<t2mMatrix.length;i++) {
                for (int j=0;j<t2mMatrix[0].length;j++) {
                    t2mMatrix[i][j]=t2mMatrix[i][j]-273.15;
                }
            }


            for (int j=0;j<etaV;j++) {
                for (int i=0;i<xiV;i++) {
                    float svstr =calcStress(u10mMatrix[j][i],v10mMatrix[j][i],888.8,angleMatrix[j][i],slpMatrix[j][i],t2mMatrix[j][i])[1];

                    outASvstr.set(0,j, i, svstr);

                }
            }

            System.out.println("End interpolation");


            System.out.println("Begin file writing...");


            ncfWriterUVStress.write(Uwind, new int[]{localTime, 0, 0}, outAU10m);
            ncfWriterUVStress.write(Vwind, new int[]{localTime, 0, 0}, outAV10m);
            ncfWriterUVStress.write(Pair, new int[]{localTime, 0, 0}, outAPAir);
            ncfWriterUVStress.write(Tair,new int[]{localTime, 0, 0}, outATAir);
            ncfWriterUVStress.write(Qair, new int[]{localTime, 0, 0}, outAQAir);
            ncfWriterUVStress.write(swrad, new int[]{localTime, 0, 0}, outASWRad);
            ncfWriterUVStress.write(lwrad_down, new int[]{localTime, 0, 0}, outALWRadDown);

            ncfWriterUVStress.write(sustr, new int[]{localTime, 0, 0}, outASustr);
            ncfWriterUVStress.write(svstr, new int[]{localTime, 0, 0}, outASvstr);

            for (ArrayDouble.D1 outATime:outATimes) {
                ncfWriterUVStress.write(time, new int [] { localTime }, outATime);
                ncfWriterUVStress.write(ocean_time, new int[]{localTime}, outATime);
            }


            System.out.println("End File Writing.");

            localTime++;
        }
    }

    ///////////////////////////////////////////////////////////

    @Override
    public void finalize() {
        try {
            ncfWriterUVStress.flush();
            ncfWriterUVStress.close();
        } catch (IOException ex) {

        }
    }

    ///////////////////////////////////////////////////////////

    float[] calcStress(double u10m, double v10m, double ust, double angle, double slp, double t2m) throws NCRegridderException {
        float[] result= new float[2];
        if (u10m != 1e37 && v10m!=1e37) {
            double rotU10m = (u10m * Math.cos(angle) + v10m * Math.sin(angle));
            double rotV10m = (v10m * Math.cos(angle) - u10m * Math.sin(angle));


            double aRotUV10m = Math.atan2(-rotV10m,(-rotU10m+1e-8));

            double aUV10m = Math.atan2(-u10m,-(v10m+1e-8));
            double aDelta = aUV10m-aRotUV10m;
            if (aDelta<angle-1e10 || aDelta>angle+1e10 ) {
                throw new NCRegridderException("Bad rotation (angle)! "+aDelta);
            }


            double rotUV10m2 = rotU10m*rotU10m+rotV10m*rotV10m;
            double rotUV10m = Math.pow(rotUV10m2,.5);
            double UV10m = Math.pow(u10m*u10m+v10m*v10m,.5);

            double delta=Math.abs(rotUV10m-UV10m);
            if (delta>1e-10) {
                throw new NCRegridderException("Bad rotation (module)! "+delta);
            }

            double rhoAir=slp*100/(287.058*(t2m+273.15));
            double dcU = 1.2875/1000;
            if (ust==999.9) {

                if (rotUV10m>7.5) {
                    dcU=(0.8+0.065*rotUV10m)/1000;
                }



            } else if (ust==888.8) {
                if (rotUV10m<3) {
                    dcU=2.17/1000;
                } else if (rotUV10m>=3 && rotUV10m<=6) {
                    dcU= (0.29 + 3.1/rotUV10m + 7.7/(rotUV10m*rotUV10m))/1000;
                } else if (rotUV10m>6 && rotUV10m<=26) {
                    dcU=(0.60 + 0.070*rotUV10m)/1000;
                } else {
                    dcU=2.42/1000;
                }

            } else {
                dcU=ust;
            }

            result[0]=-(float)(rhoAir*(dcU*dcU)*Math.sin(aRotUV10m));
            result[1]=-(float)(rhoAir*(dcU*dcU)*Math.cos(aRotUV10m));

            double aStress =Math.atan(result[1]/(result[0]+1e-8));
            if (aStress<aRotUV10m-1e10 || aRotUV10m>angle+1e10 ) {
                throw new NCRegridderException("Bad stress computation (angle)! "+aStress);
            }
        } else {
            result[0]=(float)1e37;
            result[1]=(float)1e37;
        }
        return result;
    }
}