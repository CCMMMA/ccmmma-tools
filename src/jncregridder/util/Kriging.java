/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jncregridder.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import Jama.Matrix; 
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author raffaelemontella
 */
public  class Kriging {
    
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
    
   

    
    // The collection of the measurement point, containing the position of the station.")
    private Stations inStations = null;
    public void setInStations(Stations stations) { this.inStations=stations; }
    
    
    
    // The measured data, to be interpolated.")
    private IdDoubleVectData inData = null;
    public void setInData(IdDoubleVectData inData) { this.inData=inData; }

    // The collection of the points in which the data needs to be interpolated.")
    private Stations inInterpolate = null;
    public void setInInterpolate(Stations stations) { this.inInterpolate=stations; }
    
    // The field of the interpolated points collections, defining the id.")
    private String fInterpolateid = null;

    // The interpolated data.")
    private IdDoubleVectData outData = null;
    public IdDoubleVectData getOutData() { return outData; }
    

    /**
     * Define the mode. It is possible 4 alternatives: <li>mode ==0, the value
     * to calculate are in a non-regular grid (the coordinates are stored in a
     * {@link FeatureCollection}, pointsToInterpolate. This is a 2-D
     * interpolation, so the z coordinates are null. <li>mode ==1, the value to
     * calculate are in a non-regular grid (the coordinates are stored in a
     * {@link FeatureCollection}, pointsToInterpolate. This is a 3-D
     * interpolation.. <li>mode ==2, the value to calculate are in a regular
     * grid (the coordinates are stored in a {@link GridCoverage2D},
     * gridToInterpolate. This is a 2-D interpolation. <li>mode ==3, the value
     * to calculate are in a regular grid (the coordinates are stored in a
     * {@link GridCoverage2D}, gridToInterpolate. This is a 3-D interpolation,
     * so the grid have to contains a dem.
     */
    // The interpolation mode.")
    protected int pMode = 0;

    /**
     * The integral scale, this is necessary to calculate the variogram if the
     * program use {@link Kriging2.variogram(rx,ry,rz)}.
     */
    // The integral scale.")
    private double[] pIntegralscale = { 1,1,1 };
    public void setIntegralScale(double[] pInteralscale) { this.pIntegralscale=pInteralscale; }
    public double[] getIntegralScale() { return pIntegralscale; } 

    /**
     * Variance of the measure field.
     */
    // The variance.")
    protected double pVariance = 0;

    /**
     * The logarithm selector, if it's true then the models runs with the log of
     * the data.
     */
    // Switch for logaritmic run selection.")
    private boolean doLogarithmic = false;

    
    private int defaultVariogramMode = 0;

    // The range if the models runs with the gaussian variogram.")
    private double pA;

    // The sill if the models runs with the gaussian variogram.")
    private double pS;

    // Is the nugget if the models runs with the gaussian variogram.")
    private double pNug;
    
    public Kriging() {
        inStations=null;
        inData=null;
        inInterpolate=null;
        outData=null;
        pVariance=Double.NaN;
        initMachineDEpsilon();
    }
    
    public Kriging(Stations srcStations, IdDoubleVectData srcData, Stations dstStations, IdDoubleVectData dstData ) {
        
        this.inStations=srcStations;
        this.inData=srcData;
        this.inInterpolate=dstStations;
        this.outData=dstData;
        pVariance= getVariance();
        initMachineDEpsilon();
    }
    
    
     
   private double[] getDataArrayByIndex(Integer idx) {
        int size = inData.size();
        double[] result = new double[size];
        int i=0;
        Collection<double[]> tmp=inData.values();
        Iterator<double[]> iTmp=tmp.iterator();
        while (iTmp.hasNext()) {
            result[i]=iTmp.next()[idx];
            i++;
        }
        
        return result;
    }
    
    public double getStdDev()
    {
        return Math.sqrt(getVariance());
    }
    
    public double getMean()
    {
        double[] values=getDataArrayByIndex(0);
        int count=0;
        double sum = 0.0;
        for(double a : values) {
            if (Double.isNaN(a)==false) {
                sum += a;
                count++;
            }
        }
        return sum/count;
    }
    
     public double getMin() throws KrigingException
    {
        double[] values=getDataArrayByIndex(0);
        if (values==null) throw new KrigingException("The values array is null!");
        
        double result = Double.NaN;
        for(double a : values) {
            if (Double.isNaN(a)==false) {
                if (Double.isNaN(result)==true) {
                    result=a;
                } else {
                    if (a<result) {
                        result=a;
                    }
                }
                
            }
        }
        //if (Double.isNaN(result)==true) throw new KrigingException("The min is NaN!");
        return result;
    }
     
     public double getMax() throws KrigingException
    {
        double[] values=getDataArrayByIndex(0);
        if (values==null) throw new KrigingException("The values array is null!");
        
        double result = Double.NaN;
        for(double a : values) {
            if (Double.isNaN(a)==false) {
                if (Double.isNaN(result)==true) {
                    result=a;
                } else {
                    if (a>result) {
                        result=a;
                    }
                }
                
            }
        }
        //if (Double.isNaN(result)==true) throw new KrigingException("The max is NaN!");
        return result;
    }

    public double getVariance()
    {
        double[] values=getDataArrayByIndex(0);
        int count=0;
        double mean = getMean();
        double temp = 0;
        for(double a :values) {
            if (Double.isNaN(a)==false) {
                temp += (mean-a)*(mean-a);
                count++;
            }
            
        }
        return temp/count;
    }
    
    private static boolean isNovalue(double v) {
        return Double.isNaN(v);
    }
    
    private static boolean dEq( double a, double b ) {
        if (Double.isNaN(a) && Double.isNaN(b)) {
            return true;
        }
        double diffAbs = Math.abs(a - b);
        return a == b ? true : diffAbs < D_TOLERANCE ? true : diffAbs / Math.max(Math.abs(a), Math.abs(b)) < D_TOLERANCE;
    }
    
    /**
     * Verify if the current station (i) is already into the arrays.
     *      
     * @param xStation the x coordinate of the stations
     * @param yStation the y coordinate of the stations
     * @param zStation the z coordinate of the stations
     * @param hStation the h value of the stations
     * @param xTmp
     * @param yTmp
     * @param zTmp
     * @param hTmp
     * @param i the current index
     * @param doMean if the h value of a double station have different value then do the mean.
     * @param pm
     * @return true if there is already this station.
     * @throws Exception
     */
    private boolean verifyDoubleStation( double[] xStation, double[] yStation, double[] zStation, double[] hStation,
            double xTmp, double yTmp, double zTmp, double hTmp, int i, boolean doMean) throws IllegalArgumentException {

        for( int j = 0; j < i - 1; j++ ) {

            if (dEq(xTmp, xStation[j]) && dEq(yTmp, yStation[j]) && dEq(zTmp, zStation[j]) && dEq(hTmp, hStation[j])) {
                if (!doMean) {
                    throw new IllegalArgumentException("verifyStation.equalsStation1" + xTmp + "/" + yTmp);
                }
                return true;
            } else if (dEq(xTmp, xStation[j]) && dEq(yTmp, yStation[j]) && dEq(zTmp, zStation[j])) {
                if (!doMean) {
                    throw new IllegalArgumentException("verifyStation.equalsStation2" + xTmp + "/" + yTmp);
                }
                if (!isNovalue(hStation[j]) && !isNovalue(hTmp)) {
                    hStation[j] = (hStation[j] + hTmp) / 2;
                } else {
                    hStation[j] = Double.NaN;
                }
                return true;
            }
        }
        return false;
    }
    
    
    
    
    
    
    
    public void execute() throws KrigingException  {
        verifyInput();
        
        pVariance= getVariance();

        List<Double> xStationList = new ArrayList<Double>();
        List<Double> yStationList = new ArrayList<Double>();
        List<Double> zStationList = new ArrayList<Double>();
        List<Double> hStationList = new ArrayList<Double>();

        /*
         * counter for the number of station with measured value !=0.
         */
        int n1 = 0;
        /*
         * Store the station coordinates and measured data in the array.
         */
        Iterator<Station> stationsIter = inStations.iterator();
        try {
            while( stationsIter.hasNext() ) {
                Station feature = stationsIter.next();
                int id = feature.id;
                double z = 0;
                if (pMode == 1) {
                    z = feature.z;
                   
                }
                Coordinate coordinate = feature;
                double[] h = inData.get(id);
                if (h == null || isNovalue(h[0])) {
                    /*
                     * skip data for non existing stations, they are
                     * allowed. Also skip novalues.
                     */
                    continue;
                }
                if (defaultVariogramMode == 0) {
                    if (Math.abs(h[0]) >= TOLL) {
                        xStationList.add(coordinate.x);
                        yStationList.add(coordinate.y);
                        zStationList.add(z);
                        hStationList.add(h[0]);
                        n1 = n1 + 1;
                    }
                } else if (defaultVariogramMode == 1) {
                    if (Math.abs(h[0]) >= 0) {
                        xStationList.add(coordinate.x);
                        yStationList.add(coordinate.y);
                        zStationList.add(z);
                        hStationList.add(h[0]);
                        n1 = n1 + 1;
                    }

                }
            }
        } finally {
            
        }

        int nStaz = xStationList.size();
        /*
         * The coordinates of the station points plus in last position a place
         * for the coordinate of the point to interpolate.
         */
        double[] xStation = new double[nStaz + 1];
        double[] yStation = new double[nStaz + 1];
        double[] zStation = new double[nStaz + 1];
        double[] hStation = new double[nStaz + 1];
        boolean areAllEquals = true;
        if (nStaz != 0) {
            xStation[0] = xStationList.get(0);
            yStation[0] = yStationList.get(0);
            zStation[0] = zStationList.get(0);
            hStation[0] = hStationList.get(0);
            double previousValue = hStation[0];
            
            for( int i = 1; i < nStaz; i++ ) {

                double xTmp = xStationList.get(i);
                double yTmp = yStationList.get(i);
                double zTmp = zStationList.get(i);
                double hTmp = hStationList.get(i);
                
                boolean doubleStation = verifyDoubleStation(xStation, yStation, zStation, hStation, xTmp, yTmp,
                        zTmp, hTmp, i, false);
                if (!doubleStation) {
                    xStation[i] = xTmp;
                    yStation[i] = yTmp;
                    zStation[i] = zTmp;
                    hStation[i] = hTmp;
                    if (areAllEquals && hStation[i] != previousValue) {
                        areAllEquals = false;
                    }
                    previousValue = hStation[i];
                }
                 
                 
            }
        }
        HashMap<Integer, Coordinate> pointsToInterpolateId2Coordinates = new HashMap<Integer, Coordinate>();
        int numPointToInterpolate = getNumPoint(inInterpolate);
        
        
        
        /*
         * if the isLogarithmic is true then execute the model with log value.
         */
        double[] result = new double[numPointToInterpolate];

        if (pMode == 0 || pMode == 1) {
            pointsToInterpolateId2Coordinates = getCoordinate(numPointToInterpolate, inInterpolate, fInterpolateid);
        }
        
        
        
        Set<Integer> pointsToInterpolateIdSet = pointsToInterpolateId2Coordinates.keySet();
        Iterator<Integer> idIterator = pointsToInterpolateIdSet.iterator();
        int j = 0;
        int[] idArray = new int[inInterpolate.size()];
        if (n1 != 0) {
            if (doLogarithmic) {
                for( int i = 0; i < nStaz; i++ ) {
                    if (hStation[i] > 0.0) {
                        hStation[i] = Math.log(hStation[i]);
                    }
                }
            }

            /*
             * calculating the covariance matrix.
             */
            double[][] covarianceMatrix = covMatrixCalculating(xStation, yStation, zStation, n1);
            /*
             * extract the coordinate of the points where interpolated.
             */

            /*
             * initialize the solution and its variance vector.
             */

            if (!areAllEquals && n1 > 1) {
                if (pMode==1) System.out.println("3D!"); else System.out.println("2D!");
                System.out.println("kriging.working "+n1+"->"+inInterpolate.size());
                while( idIterator.hasNext() ) {
                    double sum = 0.;
                    int id = idIterator.next();
                    
                    
                    //if ((id % 100) == 0) {
                    //    System.out.println("kriging.atwork "+id);
                    //}
                    
                             
                         
                    idArray[j] = id;
                    
                    Coordinate coordinate = (Coordinate) pointsToInterpolateId2Coordinates.get(id);
                    xStation[n1] = coordinate.x;
                    yStation[n1] = coordinate.y;
                    zStation[n1] = coordinate.z;
                    //if (pMode==1) System.out.println("id:"+id+" z:"+coordinate.z);
                    
                    /*
                     * calculating the right hand side of the kriging linear system.
                     */
                    double[] knowsTerm = knowsTermsCalculating(xStation, yStation, zStation, n1);

                    /*
                     * solve the linear system, where the result is the weight.
                     */
                    
                    Matrix a = new Matrix(covarianceMatrix);
                    Matrix b = new Matrix(knowsTerm, knowsTerm.length);
                    
                    //System.out.println("<solve>");
                    Matrix x = a.solve(b);
                    //System.out.println("</solve>");
                    double[] moltiplicativeFactor = x.getColumnPackedCopy();
                      
                    double h0 = 0.0;
                    for( int k = 0; k < n1; k++ ) {
                        h0 = h0 + moltiplicativeFactor[k] * hStation[k];
                        sum = sum + moltiplicativeFactor[k];
                    }

                    if (doLogarithmic) {
                        h0 = Math.exp(h0);
                    }
                    result[j] = h0;
                    j++;
                    if (Math.abs(sum - 1) >= TOLL) {
                        System.out.println("Error in the coffeicients calculation");
                        throw new KrigingException("Error in the coffeicients calculation");
                    }

                }
                // pm.worked(1);
            } else if (n1 == 1 || areAllEquals) {
                double tmp = hStation[0];
                int k = 0;
                System.out.println("kriging.setequalsvalue");
                System.out.println("kriging.working "+inInterpolate.size());
                while( idIterator.hasNext() ) {
                    int id = idIterator.next();
                    result[k] = tmp;
                    idArray[k] = id;
                    k++;
                    // pm.worked(1);
                }

            }
            // pm.done();
            if (pMode == 0 || pMode == 1) {
                storeResult(result, idArray);
                
                
                 
                
                
            } 
            
        } else {
            System.out.println("No rain for this time step");
            j = 0;
            double[] value = inData.values().iterator().next();
            while( idIterator.hasNext() ) {
                int id = idIterator.next();
                idArray[j] = id;
                result[j] = value[0];
                j++;
            }
            if (pMode == 0 || pMode == 1) {
                storeResult(result, idArray);
            }
        }
    }

    
    
    
    

    /**
     * Verify the input of the model.
     */
    private void verifyInput() {
        if (inData == null || inStations == null) {
            throw new NullPointerException("kriging.stationproblem");
        }
        if (pMode < 0 || pMode > 3) {
            throw new IllegalArgumentException("kriging.defaultMode");
        }
        

        if (defaultVariogramMode != 0 && defaultVariogramMode != 1) {
            throw new IllegalArgumentException("kriging.variogramMode");
        }
        if (defaultVariogramMode == 0) {
            if (pVariance == 0 || pIntegralscale[0] == 0 || pIntegralscale[1] == 0 || pIntegralscale[2] == 0) {

                System.out.println("kriging.noParam");
                System.out.println("varianza " + pVariance);
                System.out.println("Integral scale x " + pIntegralscale[0]);
                System.out.println("Integral scale y " + pIntegralscale[1]);
                System.out.println("Integral scale z " + pIntegralscale[2]);
                 
                 
            }
        }
        if (defaultVariogramMode == 1) {
            if (pNug == 0 || pS == 0 || pA == 0) {
                System.out.println("kriging.noParam");
                System.out.println("Nugget " + pNug);
                System.out.println("Sill " + pS);
                System.out.println("Range " + pA);
            }
        }

        if ((pMode == 1 || pMode == 0) && inInterpolate == null) {
            throw new NullPointerException("kriging.noPoint");
        }
        // if ((mode == 2 || mode == 3) && gridToInterpolate == null) {
        // throw new NullPointerException("problema nei punti da interpolare");
        // }

    }

    /**
     * Store the result in a HashMap (if the mode is 0 or 1)
     * 
     * @param result2
     *            the result of the model
     * @param id
     *            the associated id of the calculating points.
     * @throws SchemaException
     * @throws SchemaException
     */
    private void storeResult( double[] result2, int[] id )  {
        if (pMode == 0 || pMode == 1) {
            outData = new IdDoubleVectData();
            for( int i = 0; i < result2.length; i++ ) {
                outData.put(id[i], new double[]{result2[i]});
            }
        }
    }

    
    /**
     * Extract the coordinate of a FeatureCollection in a HashMap with an ID as
     * a key.
     * @param nStaz
     * @param collection
     * @throws Exception if a field of elevation isn't the same of the collection
     */
    private HashMap<Integer, Coordinate> getCoordinate( int nStaz,
            Stations collection, String idField ) throws KrigingException {
        
        HashMap<Integer, Coordinate> id2CoordinatesMap = new HashMap<Integer, Coordinate>();
        Iterator<Station> iterator = collection.iterator();
        Coordinate coordinate = null;
        try {
            while( iterator.hasNext() ) {
                Station feature = iterator.next();
                int name = feature.id;
                coordinate = feature;
                double z = 0;
                if (pMode == 1) {
                    try {
                        z = feature.z;
                    } catch (NullPointerException e) {
                        throw new KrigingException("kriging.noPointZ");
                    }
                }
                coordinate.z = z;
                id2CoordinatesMap.put(name, coordinate);
            }
        } finally {
            
        }

        return id2CoordinatesMap;
    }
    /**
     * Return the number of features.
     * 
     * @param collection
     * @return
     * @throws ModelsIOException 
     */
    private int getNumPoint( Stations collection ) throws KrigingException {
        int nStaz = 0;
        if (collection != null) {
            nStaz = collection.size();
        }
        if (nStaz == 0) {
            throw new KrigingException("Didn't find any point in the FeatureCollection");
        }
        return nStaz;
    }

    /**
     * The gaussian variogram
     * 
     * @param c0
     *            nugget.
     * @param a
     *            range.
     * @param sill
     *            sill.
     * @param rx
     *            x distance.
     * @param ry
     *            y distance.
     * @param rz
     *            z distance.
     * @return the variogram value
     */
    private double variogram( double c0, double a, double sill, double rx, double ry, double rz ) {
        if (isNovalue(rz)) {
            rz = 0;
        }
        double h2 = Math.sqrt(rx * rx + rz * rz + ry * ry);
        return c0 + sill * (1 - Math.exp(-(h2 * h2) / (a * a)));

    }

    /**
     * 
     * @param rx
     *            x distance.
     * @param ry
     *            y distance.
     * @param rz
     *            z distance.
     * @return
     */
    private double variogram( double rx, double ry, double rz ) {
        if (isNovalue(rz)) {
            rz = 0;
        }
        double h2 = (rx / pIntegralscale[0]) * (rx / pIntegralscale[0]) + (ry / pIntegralscale[1]) * (ry / pIntegralscale[1])
                + (rz / pIntegralscale[2]) * (rz / pIntegralscale[2]);
        if (h2 < TOLL) {
            return pVariance;
        } else {
            return pVariance * Math.exp(-Math.sqrt(h2));
        }

    }

    /**
     * 
     * 
     * @param x
     *            the x coordinates.
     * @param y
     *            the y coordinates.
     * @param z
     *            the z coordinates.
     * @param n
     *            the number of the stations points.
     * @return
     */
    private double[][] covMatrixCalculating( double[] x, double[] y, double[] z, int n ) {
        double[][] ap = new double[n + 1][n + 1];
        if (defaultVariogramMode == 0) {
            for( int j = 0; j < n; j++ ) {
                for( int i = 0; i <= j; i++ ) {
                    double rx = x[i] - x[j];
                    double ry = y[i] - y[j];
                    double rz = 0;
                    if (pMode == 1) {
                        rz = z[i] - z[j];
                    }
                    double tmp = variogram(rx, ry, rz);

                    ap[j][i] = tmp;
                    ap[i][j] = tmp;

                }
            }
        } else if (defaultVariogramMode == 1) {
            for( int j = 0; j < n; j++ ) {
                for( int i = 0; i < n; i++ ) {
                    double rx = x[i] - x[j];
                    double ry = y[i] - y[j];
                    double rz = 0;
                    if (pMode == 1) {
                        rz = z[i] - z[j];
                    }
                    double tmp = variogram(pNug, pA, pS, rx, ry, rz);

                    ap[j][i] = tmp;
                    ap[i][j] = tmp;

                }
            }

        }
        for( int i = 0; i < n; i++ ) {
            ap[i][n] = 1.0;
            ap[n][i] = 1.0;

        }
        ap[n][n] = 0;
        return ap;

    }

    /**
     * 
     * @param x
     *            the x coordinates.
     * @param y
     *            the y coordinates.
     * @param z
     *            the z coordinates.
     * @param n
     *            the number of the stations points.
     * @return
     */
    private double[] knowsTermsCalculating( double[] x, double[] y, double[] z, int n ) {

        double[] gamma = new double[n + 1];
        if (defaultVariogramMode == 0) {
            for( int i = 0; i < n; i++ ) {
                double rx = x[i] - x[n];
                double ry = y[i] - y[n];
                double rz = z[i] - z[n];
                gamma[i] = variogram(rx, ry, rz);
            }
        } else if (defaultVariogramMode == 1) {
            for( int i = 0; i < n; i++ ) {
                double rx = x[i] - x[n];
                double ry = y[i] - y[n];
                double rz = z[i] - z[n];
                gamma[i] = variogram(pNug, pA, pS, rx, ry, rz);
            }

        }
        gamma[n] = 1.0;
        return gamma;

    }

    public void setPMode(int pMode) {
        this.pMode = pMode;
    }

    
}
