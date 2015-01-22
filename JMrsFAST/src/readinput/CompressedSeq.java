/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package readinput;

import java.util.ArrayList;
import java.util.List;
import static readinput.StringUtils.BaseToIndex;

/**
 *
 * @author desktop
 */
public class CompressedSeq {
    // Every 21 bases are stored within a 64bit unsigned integer
    private final List<Long> CompSeq = new ArrayList<>();
    
    public CompressedSeq(String seq){
        int i = 0, pos = 0;
        // Start with forward sequence string
        CompSeq.add(0l);
        while(pos < seq.length()){
            CompSeq.set(i, ((CompSeq.get(i) << 3)) | BaseToIndex(seq.charAt(pos++)));
            
            if(++i == 21){
                i = 0;
                
                if(pos < seq.length())  // This prevents the addition of a new element if the sequence is exactly 21 chars
                    CompSeq.add(0l);
            }
        }
        if(i > 0){
            CompSeq.set(i, CompSeq.get(i) << (3 * (21 - i)));
        }
    }
    
}
