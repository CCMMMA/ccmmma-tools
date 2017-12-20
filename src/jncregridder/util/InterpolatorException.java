/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jncregridder.util;

/**
 *
 * @author raffaelemontella
 */
public class InterpolatorException extends Exception {

    public InterpolatorException(String string) {
        super(string);
    }

    public InterpolatorException(Exception ex) {
        super(ex);
    }
    
}
