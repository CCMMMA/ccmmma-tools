/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.uniparthenope.meteo;

import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.ExtendedData;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Point;
import de.micromata.opengis.kml.v_2_2_0.SchemaData;
import de.micromata.opengis.kml.v_2_2_0.SimpleData;

import it.uniparthenope.lpdm.LPDMHistory;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jncregridder.roms.ROMSGrid;
import jncregridder.util.KrigingException;
import jncregridder.util.NCRegridderException;
import ucar.ma2.InvalidRangeException;


class Source {
    private int id;
    private double lon;
    private double lat;
    private int i;
    private int j;
    private int k;
    private int nPartsPerHour=100;
    private int mode=0;
    private int sourceStart=-1;
    private int sourceEnd=-1;
    
    
    public Source(int id, double lon, double lat, int i, int j, int k, int mode) {
        this.id = id;
        this.lon = lon;
        this.lat = lat;
        this.i=i;
        this.j=j;
        this.k=k;
        this.mode=mode;
    }
    
    public int getId() { return id; }
    public int getI() { return i; }
    public int getJ() { return j; }
    public int getK() { return k; }
    public int getNPartsPerHour() { return nPartsPerHour; }
    public int getMode() { return mode; }
    public int getSourceStart() { return sourceStart; }
    public int getSourceEnd() { return sourceEnd; }
}

class Sources extends ArrayList<Source> {
    
    public String toNamelist() {
        String result="&ems\n" + " nsources      = "+this.size()+"\n";
        result+="id_source     = ";
        for (Source source:this) {
            result+=source.getId()+", ";
        }
        result+="\n";
        result+="i_source     = ";
        for (Source source:this) {
            result+=source.getI()+", ";
        }
        result+="\n";
        result+="j_source     = ";
        for (Source source:this) {
            result+=source.getJ()+", ";
        }
        result+="\n";
        result+="k_source     = ";
        for (Source source:this) {
            result+=source.getK()+", ";
        }
        result+="\n";
        
        result+="nPartsPerHour     = ";
        for (Source source:this) {
            result+=source.getNPartsPerHour()+", ";
        }
        result+="\n";
        
        result+="mode     = ";
        for (Source source:this) {
            result+=source.getMode()+", ";
        }
        result+="\n";
        
        result+="source_start     = ";
        for (Source source:this) {
            result+=source.getSourceStart()+", ";
        }
        result+="\n";
        
        result+="source_end     = ";
        for (Source source:this) {
            result+=source.getSourceEnd()+", ";
        }
        result+="\n";
        
        return result;
    }
    
}
    
/**
 *
 * @author raffaelemontella
 */
public class KML2LPDMSources {
    Sources sources = new Sources();
    
    public static void main(String args[])
        throws IOException, NCRegridderException, InvalidRangeException, NoSuchAlgorithmException, KrigingException
    {
        
        try {
            
            if (args.length != 3) {
                System.out.println("Usage:");
                System.out.println("KML2LPDMSources gridFile KMLSourcesFile outputFile");
                System.exit(0);
            }
            new KML2LPDMSources(args[0],args[1],args[2]);
             
            /* 
            new LPDM2GrADS(
                    "/Users/raffaelemontella/tmp/myocean2roms/roms/roms-grid-d04.nc",
                    "/Users/raffaelemontella/tmp/myocean2roms/output/all_5giorni.nc",
                    "/Users/raffaelemontella/tmp/myocean2roms/lamp3d/lmp3_d04_20121007Z0000_120h.nc",
                    "/Users/raffaelemontella/tmp/myocean2roms/output/", "lmp3_d04_20121007Z0000_120h");
            */
            
        } catch (InvalidRangeException ex) {
            Logger.getLogger(KML2LPDMSources.class.getName()).log(Level.SEVERE, null, ex); 
        }
        
        
        
    }

    private static double calcDistance(double lon1, double lat1, double lon2, double lat2) {
        
        double R = 6371; // km
        double dLat = Math.toRadians(lat2-lat1);
        double dLon = Math.toRadians(lon2-lon1);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
        Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(lat1) * Math.cos(lat2); 
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a)); 
        double d = R * c;
        
        return d;
    }

    private KML2LPDMSources(String gridFile, String kmlSourcesFile, String outputFile) throws InvalidRangeException, IOException, NCRegridderException {
        
        ROMSGrid romsGrid = new ROMSGrid(gridFile);
            
        double minMaxLatRho[] = romsGrid.getMinMaxLatRho();
        double minMaxLonRho[] = romsGrid.getMinMaxLonRho();

        double minLatRho = minMaxLatRho[0];
        double maxLatRho = minMaxLatRho[1];
        double minLonRho = minMaxLonRho[0];
        double maxLonRho = minMaxLonRho[1];
        
        double maxD=1;
        int id=1;
        Kml kml = Kml.unmarshal(new File(kmlSourcesFile));
        Document document = (Document) kml.getFeature();
        Folder folder = (Folder) document.getFeature().get(0);
        int folderSize = folder.getFeature().size();
        System.out.println(folderSize);
        for (int idx=0;idx<folderSize;idx++) {
            Placemark placemark = (Placemark)folder.getFeature().get(idx);
            ExtendedData extendedData=placemark.getExtendedData();
            List<SchemaData> schemaDataList = extendedData.getSchemaData();
            SchemaData schemaData=schemaDataList.get(0);
            List<SimpleData> simpleDataList=schemaData.getSimpleData();
            id=Integer.parseInt(simpleDataList.get(0).getValue());
            
            Point point = (Point)placemark.getGeometry();
            List<Coordinate> coordinates = point.getCoordinates();
            Coordinate coordinate=coordinates.get(0);
            double lon = coordinate.getLongitude();
            double lat = coordinate.getLatitude();
            //if (lon >= minLonRho && lon <=maxLonRho && lat>=minLatRho && lat<=maxLatRho) {
            //    count++;
                
            int minI=0,minJ=0;
            double d,minD=Double.MAX_VALUE;
            for (int j=0;j<romsGrid.getLATRHO().length;j++) {
                for (int i=0;i<romsGrid.getLATRHO()[0].length;i++) {
                    d=KML2LPDMSources.calcDistance(lon,lat,romsGrid.getLONRHO()[j][i],romsGrid.getLATRHO()[j][i]);
                    if (d<minD) {
                        minI=i;
                        minJ=j;
                        minD=d;
                    }
                }

            }

            if (minD<maxD) {
                int mode=0;
                if (romsGrid.getMASKRHO()[minJ][minI]==1) {
                    mode=1;
                } else {
                    
                    class GridPoint {
                        int j,i;
                        double lat,lon;
                        double d;
                        
                        public GridPoint(int i, int j, double lon, double lat, double d) {
                            this.i=i;
                            this.j=j;
                            this.lon=lon;
                            this.lat=lat;
                            this.d=d;
                        }
                    }
                    
                    int[] dI={
                        1,1,0,-1,-1,-1,0,1,
                        2,2,2,2,1,0,-1,-2,-2,-2,-2,-2,-1,0,1,2,
                        3,3,3,3,3,3,2,1,0,-1,-2,-3,-3,-3,-3,-3,-3,-3,-2,-1,0,1,2,3 };
                    int[] dJ={ 0,-1,-1,-1,0,1,1,1,
                        1,0,-1,-2,-2,-2,-2,-2,-1,0,1,2,2,2,2,2,
                        2,1,0,-1,-2,-3,-3,-3,-3,-3,-3,-3,-2,-1,0,1,2,3,3,3,3,3,3,3 };
                    
                    
                    ArrayList<GridPoint> gridPoints=new ArrayList<GridPoint>();
                    for (int r=0; r<dI.length;r++) {
                        int newI=minI+dI[r];
                        int newJ=minJ+dJ[r];
                        if (newJ<romsGrid.getMASKRHO().length && newI<romsGrid.getMASKRHO()[0].length) {
                            if (romsGrid.getMASKRHO()[newJ][newI]==1) {
                                gridPoints.add(
                                    new GridPoint(
                                        newI,newJ,
                                        romsGrid.getLONRHO()[newJ][newI],
                                        romsGrid.getLATRHO()[newJ][newI],
                                        KML2LPDMSources.calcDistance(lon,lat,romsGrid.getLONRHO()[newJ][newI],romsGrid.getLATRHO()[newJ][newI])
                                    )
                                );
                            }
                        }
                    }
                    
                    minD=Double.MAX_VALUE;
                    for (GridPoint gp:gridPoints) {
                        if (gp.d < minD) {
                            minD=gp.d;
                            minI=gp.i;
                            minJ=gp.j;
                            
                        }
                    }
                    
                    if (minD!=Double.MAX_VALUE) {
                        mode=1;
                    }
                    
                }
                sources.add(new Source(id,lon,lat,minI,minJ,0,mode));
                
            }
        }
        System.out.println(sources.toNamelist());
    }
}
