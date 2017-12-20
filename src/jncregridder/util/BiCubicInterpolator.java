/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jncregridder.util;

import flanagan.interpolation.BiCubicInterpolation;

/**
 *
 * @author raffaelemontella
 */
public class BiCubicInterpolator extends InterpolatorBase {

    private double TOLL=1e-6;
    private BiCubicInterpolation biCubicInterpolation=null;
    
    public BiCubicInterpolator(double[][] srcLAT, double[][] srcLON, double[][] dstLAT, double[][] dstLON, int[][] srcMASK,int[][] dstMASK) throws InterpolatorException {
        
        
        init(srcLAT,srcLON,dstLAT,dstLON,srcMASK,dstMASK);
        
        
        
    }
    
    // Calculate the derivatives
        private void calcDeriv(double[] x1, double[] x2, double[][] y, double [][] dydx1, double [][] dydx2, double [][] d2ydx1dx2) throws InterpolatorException{
            
            int nPoints = x1.length;
            int mPoints = x2.length;

            // Numerical differentiation using only provided data points
                int iip =0;
                int iim =0;
                int jjp =0;
                int jjm =0;
                for(int i=0; i<nPoints; i++){
                    iip = i+1;
    	            if(iip>=nPoints)iip = nPoints-1;
    	            iim = i-1;
    	            if(iim<0)iim = 0;
    	            for(int j=0; j<mPoints; j++){
    	                jjp = j+1;
    	                if(jjp>=mPoints)jjp = mPoints-1;
    	                jjm = j-1;
    	                if(jjm<0)jjm = 0;
                        
                        double[] yy = new double[8];
                        
                        yy[0]=y[iip][j];
                        yy[1]=y[iim][j];
                        yy[2]=y[i][jjp];
                        yy[3]=y[i][jjm];
                        yy[4]=y[iip][jjp];
                        yy[5]=y[iip][jjm];
                        yy[6]=y[iim][jjp];
                        yy[7]=y[iim][jjm];
                        
                        double myy=0;
                        int myyCount=0;
                        for (int l=0;l<yy.length;l++) {
                            if (Double.isNaN(yy[l])==false) {
                                myy+=yy[l];
                                myyCount++;
                            }
                        }
                        myy=myy/myyCount;
                        if (Double.isNaN(myy)==true) myy=TOLL;
                        for (int l=0;l<yy.length;l++) {
                            if (Double.isNaN(yy[l])==true) {
                                yy[l]=myy;
                                
                            }
                        }
                        
                        
    	                dydx1[i][j] = (yy[0] - yy[1])/(x1[iip] - x1[iim]);
    	                dydx2[i][j] = (yy[2] - yy[3])/(x2[jjp] - x2[jjm]);
    	                d2ydx1dx2[i][j] = (yy[4] - yy[5] - yy[6] + yy[7])/((x1[iip] - x1[iim])*(x2[jjp] - x2[jjm]));
                    
                        if (Double.isNaN(dydx1[i][j])==true)//dydx1[i][j]=TOLL;
                            throw new InterpolatorException("dydx1 is NaN in i:"+i+" j:"+j);
                        
                        if (Double.isNaN(dydx2[i][j])==true) //dydx2[i][j]=TOLL;
                            throw new InterpolatorException("dydx2 is NaN in i:"+i+" j:"+j);
                        
                        if (Double.isNaN(d2ydx1dx2[i][j])==true) //d2ydx1dx2[i][j]=TOLL;
                            throw new InterpolatorException("d2ydx1dx2 is NaN in i:"+i+" j:"+j);
                    }
                }
            
    	}

    

    @Override
    public double[][] interp(double[][] src, double srcMissingValue, double dstMissingValue, double[] params) throws InterpolatorException {
        
        if (src==null) throw new InterpolatorException("Can't interpolate null variables!");
        
        double srcMean=0;
        int srcCount=0;
        double dstMean=0;
        int dstCount=0;
        
        int dstEta=dstLAT.length;
        int dstXi=dstLAT[0].length;
        int srcEta=srcLAT.length;
        int srcXi=srcLAT[0].length;
        
        double srcLonMin=srcLON[0][0];
        double srcLatMin=srcLAT[0][0];
        double srcLonMax=srcLON[srcEta-1][srcXi-1];
        double srcLatMax=srcLAT[srcEta-1][srcXi-1];
        double srcLatDelta=srcLatMax-srcLatMin;
        double srcLonDelta=srcLonMax-srcLonMin;
        double srcLatStep=srcLatDelta/srcEta;
        double srcLonStep=srcLonDelta/srcXi;
        
        double dstLonMin=dstLON[0][0];
        double dstLatMin=dstLAT[0][0];
        double dstLonMax=dstLON[dstEta-1][dstXi-1];
        double dstLatMax=dstLAT[dstEta-1][dstXi-1];
        
        for (int j=0;j<dstLAT.length;j++) {
            for (int i=0;i<dstLON[0].length;i++) {
                if (dstLAT[j][i]<dstLatMin) {
                    dstLatMin=dstLAT[j][i];
                }
                if (dstLAT[j][i]>dstLatMax) {
                    dstLatMax=dstLAT[j][i];
                }
                if (dstLON[j][i]<dstLonMin) {
                    dstLonMin=dstLON[j][i];
                }
                if (dstLON[j][i]>dstLonMax) {
                    dstLonMax=dstLON[j][i];
                }
            }
        }
        
        
        int srcJmin = (int)((dstLatMin-srcLatMin)/srcLatStep)-2;
        int srcJmax = (int)((dstLatMax-srcLatMin)/srcLatStep)+2;
        int srcImin = (int)((dstLonMin-srcLonMin)/srcLonStep)-2;
        int srcImax = (int)((dstLonMax-srcLonMin)/srcLonStep)+2;
        
        srcJmax=-1;
        srcJmin=-1;
        srcImax=-1;
        srcImin=-1;
        
        double lat,lon;
        for (int j=0;j<srcLAT.length;j++) {
            lat = srcLAT[j][0];
            if (lat >= dstLatMin && srcJmin==-1) {
                srcJmin=j;
            }
            if (lat >= dstLatMax && srcJmax==-1) {
                srcJmax=j;
            }
            
        }
        
        for (int i=0;i<srcLON[0].length;i++) {
            lon = srcLON[0][i];
            if (lon >= dstLonMin && srcImin==-1) {
                srcImin=i;
            }
            if (lon >= dstLonMax && srcImax==-1) {
                srcImax=i;
            }
        }
        
        srcJmax+=4;
        srcImax+=4;
        srcJmin-=4;
        srcImin-=4;
        
        int srcJsize = srcJmax-srcJmin;
        int srcIsize = srcImax-srcImin;
        
        
        double[] lons=new double[srcIsize];
        double[] lats=new double[srcJsize];
        double[][] srcData = new double[srcJsize][srcIsize];
        
        
        for(int j=0;j<srcJsize;j++) {
            for(int i=0;i<srcIsize;i++) {
                lons[i] = srcLON[0][i+srcImin];
                lats[j] = srcLAT[j+srcJmin][0];
                srcData[j][i] = src[j+srcJmin][i+srcImin];
                if (Double.isNaN(srcData[j][i])==true ||
                    srcData[j][i]==srcMissingValue) {
                    srcData[j][i]=Double.NaN;
                } else {
                    srcMean+=srcData[j][i];
                    srcCount++;
                }
                //System.out.println(lons[i]+","+lats[j]+"->"+srcData[j][i]);
            }
        }
        /*
        double[][] dydx1=new double[lats.length][lons.length];
        double[][] dydx2=new double[lats.length][lons.length];
        double[][] d2ydx1dx2 = new double[lats.length][lons.length];
        
        calcDeriv(lats, lons, srcData, dydx1, dydx2, d2ydx1dx2);
        
        biCubicInterpolation = new BiCubicInterpolation(lats, lons, srcData,dydx1,dydx2,d2ydx1dx2);
        */
        biCubicInterpolation = new BiCubicInterpolation(lats, lons, srcData,0);
        
        
        double dstLat,dstLon;
        
        
        
        
        double[][] dst = new double[dstEta][dstXi];
        for (int dstJ=0;dstJ<dstEta;dstJ++) {
            for (int dstI=0;dstI<dstXi;dstI++) {
                if (dstMASK[dstJ][dstI]==1) {
                    dstLon=dstLON[dstJ][dstI];
                    dstLat=dstLAT[dstJ][dstI];
                    
                    dst[dstJ][dstI]=biCubicInterpolation.interpolate(dstLat, dstLon);
                    if (Double.isNaN(dst[dstJ][dstI])==true) dst[dstJ][dstI]=dstMissingValue;
                     //System.out.println("dstLon:"+dstLon+" dstLat:"+dstLat+" dst="+dst[dstJ][dstI]);
                } else {
                    dst[dstJ][dstI]=dstMissingValue;
                }
                
                if (dst[dstJ][dstI]!=dstMissingValue) {
                    dstMean+=dst[dstJ][dstI];
                    dstCount++;
                }
            }
            
            
        }
        srcMean=srcMean/srcCount;
        dstMean=dstMean/dstCount;
        System.out.println("srcMean:"+srcMean+" dstMean:"+dstMean);
        return dst;
    }
    
}
