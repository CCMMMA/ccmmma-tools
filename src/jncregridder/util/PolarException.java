/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jncregridder.util;

/**
 *
 * @author raffaelemontella
 */
class PolarException extends Exception {
    PolarException(String message) {
        super(message);
    }

    PolarException(Exception ex) {
        super(ex);
    }
}
