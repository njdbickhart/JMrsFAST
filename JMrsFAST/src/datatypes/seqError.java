/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package datatypes;

/**
 *
 * @author desktop
 */
public class seqError {
    public boolean isUsable = true; // boolean value set when error is greater than the allowed error threshold
    public int error;
    public int sampleError;
    
    public void combine(seqError err){
        this.error += err.error;
        this.sampleError += err.sampleError;
    }
}
