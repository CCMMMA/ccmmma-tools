/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jncregridder.util;

import java.io.FileNotFoundException;

/**
 *
 * @author raffaelemontella
 */
public class KrigingException extends Exception {
    
    KrigingException(String message) {
        super(message);
    }

    KrigingException(Exception ex) {
        super(ex);
    }
    
}
