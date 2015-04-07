/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package output;

/**
 *
 * @author desktop
 */
public abstract class AbstractTempHit {
    /*
        This is an abstract base class that will allow for object oriented access
        to temporary hit files. The only required abstract method designates if this
        "line" from the binary file is actually a chromosome header and not an information
        "line."
    */
    public abstract boolean isChrHeader();
}
