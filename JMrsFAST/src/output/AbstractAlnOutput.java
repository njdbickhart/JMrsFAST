/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package output;

/**
 *
 * @author bickhart
 */
public abstract class AbstractAlnOutput <T>{
    
    public abstract void loadMapReads();
    
    public abstract T getFormatOuput();
}
