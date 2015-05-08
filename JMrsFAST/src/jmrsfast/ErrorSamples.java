/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jmrsfast;

/**
 *
 * @author desktop
 */
public class ErrorSamples {
    public final byte[] errorCount;
    
    public ErrorSamples(){
        errorCount = new byte[16777216];
        for (int i = 0; i < 16777216; i++){
                errorCount[i] = 0;
                for (int x = 0; x < 8; x++)
                        if ((i & (7 << 3*x)) > 0)
                                errorCount[i]++;
        }
    }
}
