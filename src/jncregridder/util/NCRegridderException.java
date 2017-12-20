/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jncregridder.util;

/**
 *
 * @author raffaelemontella
 */
public class NCRegridderException extends Exception {

    public NCRegridderException(String msg) {
        super(msg);
    }

    public NCRegridderException(Exception ex) {
        super(ex);
    }

    public NCRegridderException(String msg, Exception ex) {
        super(msg, ex);
    }

}
