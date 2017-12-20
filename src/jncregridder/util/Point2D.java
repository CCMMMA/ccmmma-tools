/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jncregridder.util;

/**
 *
 * @author raffaelemontella
 */
public class Point2D {
    private double lon;
    private double lat;
    private int i;
    private int j;
    private double value;
    
    public Point2D(int j, int i, double lat, double lon, double value) {
        this.j = j;
        this.i = i;
        this.lat = lat;
        this.lon = lon;
        this.value = value;
    }
    
    public double getLON() { return lon; }
    public double getLAT() { return lat; }
    public int getI() { return i; }
    public int getJ() { return j; }
}
