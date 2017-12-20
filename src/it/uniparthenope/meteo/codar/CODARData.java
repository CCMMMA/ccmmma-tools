/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.uniparthenope.meteo.codar;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import jncregridder.util.IdDoubleVectData;
import jncregridder.util.KInterpolator;
import jncregridder.util.Kriging;
import jncregridder.util.KrigingException;
import jncregridder.util.Station;
import jncregridder.util.Stations;

/**
 *
 * @author raffaelemontella
 */
public class CODARData {
    
    private Stations stations=new Stations();
    private IdDoubleVectData srfUData = new IdDoubleVectData();
    private IdDoubleVectData srfVData = new IdDoubleVectData();
    
    private double minLon,minLat,maxLon,maxLat;
    
    public double getMinLon() { return minLon; }
    public double getMinLat() { return minLat; }
    public double getMaxLon() { return maxLon; }
    public double getMaxLat() { return maxLat; }
    
        
    public CODARData(String url) throws FileNotFoundException, IOException {
    
        double oldLon=0,oldLat=0;
    
        FileReader fr=new FileReader(url);
        BufferedReader br=new BufferedReader(fr);
        String line=null;
        int id=0;
        while((line = br.readLine()).equals("%TableStart:")==false);
        line = br.readLine();
        line = br.readLine();
        while((line = br.readLine()).equals("%TableEnd:")==false) {
            line=line.trim();
            while(line.indexOf("  ")!=-1) line=line.replace("  "," ");
            String[] parts = line.split(" ");
            double lon=Double.parseDouble(parts[0]);
            double lat=Double.parseDouble(parts[1]);
            
            if (id==0) {
                minLon=lon;
                maxLon=lon;
                minLat=lat;
                maxLat=lat;
                
                
            } else {
                if (lon>maxLon) maxLon=lon;
                if (lat>maxLat) maxLat=lat;
                if (lon<minLon) minLon=lon;
                if (lat<minLat) minLat=lat;
                
                
                
                
            }
            
            double srfU=Double.parseDouble(parts[2])/100;
            double srfV=Double.parseDouble(parts[3])/100;
            
            Station station = new Station(id,lon,lat);
            stations.add(station);
            double[] uData = new double[1];
            double[] vData = new double[1];
            uData[0]=srfU;
            vData[0]=srfV;
            srfUData.put(id,uData );
            srfVData.put(id,vData );
            oldLon=lon;
            oldLat=lat;
            id++;
        }
        
        br.close();
        fr.close();
        
        
    }   
    
    public void regrid(double[][] dstLAT, double[][] dstLON, double[][] uSrf, double[][] vSrf) throws KrigingException {
        
        int[][] dst2id = new int[dstLAT.length][dstLON[0].length];
        
        Stations dstStations = new Stations();
        int id=0;
        for (int j=0;j<dstLAT.length;j++) {
            for (int i=0;i<dstLON[0].length;i++) {
                Station station = new Station(id,dstLON[j][i],dstLAT[j][i]);
                dstStations.add(station);
                dst2id[j][i]=id;
                id++;
            }
        }
        
        Kriging krigingU = new Kriging();
        krigingU.setInStations(stations);
        krigingU.setInData(srfUData);
        krigingU.setInInterpolate(dstStations);
        krigingU.execute();
        
        Kriging krigingV = new Kriging();
        krigingV.setInStations(stations);
        krigingV.setInData(srfVData);
        krigingV.setInInterpolate(dstStations);
        krigingV.execute();
        
        
        
        IdDoubleVectData dataU=krigingU.getOutData();
        IdDoubleVectData dataV=krigingV.getOutData();
        
        for (int j=0;j<dstLAT.length;j++) {
            for (int i=0;i<dstLON[0].length;i++) {
                uSrf[j][i]=dataU.get(dst2id[j][i])[0];
                vSrf[j][i]=dataV.get(dst2id[j][i])[0];
                
                
            }
        }
        
        
    }
}
