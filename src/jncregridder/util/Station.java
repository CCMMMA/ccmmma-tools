/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jncregridder.util;

/**
 *
 * @author raffaelemontella
 */
public class Station extends Coordinate {
    int id;
    int i;
    int j;
    
    public Station(int id,double x, double y) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = Double.NaN;
    }
    
    public Station(int id,double x, double y, int j, int i) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = Double.NaN;
        this.i = i;
        this.j = j;
    }
    
    public Station(int id,double x, double y, double z) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    
}
