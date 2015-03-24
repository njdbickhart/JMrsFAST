/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package refindex;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author desktop
 */
public class AlphaCounter {
    // The QGram data array is a series of 4 bytes per location
    // Byte order is:
    //  A = 0
    //  C = 1
    //  G = 2
    //  T = 3
    //  N is ignored
    public byte[] counter;
    public int threadID = -1;   // Default value to suggest that this was a threadless addition
    
    public AlphaCounter(int counterSize, int threadID){
        counter = new byte[counterSize];
        this.threadID = threadID;
    }
    
    // In order to combine the contents of the threading into one object
    public AlphaCounter(List<byte[]> a){
        int totalLen = 0;
        for(byte[] b : a){
            totalLen += b.length;
        }
        
        counter = new byte[totalLen];
        int curPos = 0;
        for(byte[] b : a){
            System.arraycopy(b, 0, counter, curPos, b.length);
            curPos += b.length;
        }
    }
    
    public void copyByte(int counterIdx){
        int actualPos = (counterIdx * 4);
        int previousPos = ((counterIdx - 1) * 4);
        for(int i = 0; i < 4; i++){
            this.counter[actualPos + i] = this.counter[previousPos + i];
        }
    }
    
    public void increaseByte(int counterIdx, byte val){
        int actualPos = (counterIdx * 4) + (int) val;
        this.counter[actualPos] += 1;
    }
    
    public void decreaseByte(int counterIdx, byte val){
        int actualPos = (counterIdx * 4) + (int) val;
        this.counter[actualPos] -= 1;
    }
    
    public byte[] getCounterAtLoc(int counterIdx){
        byte[] count = new byte[4];
        int actualPos = (counterIdx * 4);
        count = Arrays.copyOfRange(this.counter, actualPos, actualPos + 4);
        return count;
    }
}
