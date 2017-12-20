/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jncregridder.util;

import java.io.Serializable;

/**
 *
 * @author raffaelemontella
 */
public class MoltiplicativeFactor implements Serializable {
    public int id;
    public double[] values;
    
    public MoltiplicativeFactor(int id, double[] values) {
        this.id = id;
        this.values = values;
    }
    
}

