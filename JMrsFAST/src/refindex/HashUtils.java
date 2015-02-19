/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package refindex;

import java.util.Arrays;
import readinput.StringUtils;

/**
 *
 * @author desktop
 */
public class HashUtils {
    public static int byteArrayToInt(byte[] b) {
        int value = 0;
        for (int i = 0; i < b.length; i++) {
            int shift = (b.length - 1 - i) * 8;
            value += (b[i] & 0x000000FF) << shift;
        }
        return value;
    }
    
    public static byte[] intToByteArray(int a){
        byte[] ret = new byte[4];
        ret[3] = (byte) (a & 0xFF);   
        ret[2] = (byte) ((a >> 8) & 0xFF);   
        ret[1] = (byte) ((a >> 16) & 0xFF);   
        ret[0] = (byte) ((a >> 24) & 0xFF);
        return ret;
    }
    
    public static byte[] getVariableByteArray(int len){
        return new byte[len];
    }
    
    public static byte[] getByteSlice(byte[] block, int startpos, int len){
        byte[] temp = new byte[len];
        int counter = 0;
        for(int i = startpos; i < startpos + len; i++){
            temp[counter] = block[i];
        }
        return temp;
    }
    
    // Returns number of bytes written to buffer
    public static int encodeVariableByte(byte[] buffer, int value){
            int t = 0;
            do {
                    buffer[t++] = (byte) (value & 127);
                    value /= 128;
            } while (value != 0);
            buffer[t-1] |= 128;
            return t;
    }
    
    // Returns number of bytes written from buffer
    public static int decodeVariableByte(byte[] buffer, int index, int result){
            int i = 0;
            byte[] temp = Arrays.copyOf(buffer, buffer.length + 4);
            byte[] subtemp = intToByteArray(index);
            
            for(int b = buffer.length - 1; b < subtemp.length; b++){
                temp[b] = subtemp[i++];
            }
            i = 0;
            byte t;
            result = 0;
            do {
                    t = buffer[i];
                    result |= ((t&127) <<(7*i));
                    i++;
            } while ((t & 128) == 0);
            return i;
    }
    
    public static int hashVal(char[] seq, int windowSize){
            int i=0;
            int val=0;

            while(i<windowSize)
            {
                    if (StringUtils.BaseHashVal(seq[i]) == -1)
                            return -1; 
                    val = (val << 2) | StringUtils.BaseHashVal(seq[i++]); 
            }
            return val;
    }

    
    public static int checkSumVal(char[] seq, int checkSumLength){
            int i=0;
            int val=0;

            while(i<checkSumLength)
            {
                    if (StringUtils.BaseHashVal(seq[i]) == -1)
                            return -1; 
                    val = (val << 2) | StringUtils.BaseHashVal(seq[i++]); 
            }
            return val;
    }
    
    // Divided by eight, because it's 8 bytes per 21 bases
    public static int calculateCompressedLen(int normalLen){
            return (((normalLen / 21) + ((normalLen%21 > 0)?1:0))/8);
    }
}
