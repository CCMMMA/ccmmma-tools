/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jncregridder.util;

import java.util.HashMap;

/**
 *
 * @author raffaelemontella
 */
public class InterpolatorParams extends HashMap<String,Object> {
    public double doubleValue(String key) throws InterpolatorException { 
        Object o = get(key);
        if (o==null) throw new InterpolatorException("Bad parameter name: "+key);
        return ((Double)(o)).doubleValue();
         
    }
}
