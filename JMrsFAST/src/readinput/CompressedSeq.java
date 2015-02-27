/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package readinput;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import static readinput.StringUtils.BaseToIndex;
import refindex.HashUtils;

/**
 *
 * @author desktop
 */
public class CompressedSeq {
    // Every 21 bases are stored within a 64bit unsigned integer
    public List<Long> CompSeq;
    
    public CompressedSeq(byte[] b) throws Exception{
        this.CompSeq = HashUtils.sectionByteArraysToLongList(b);
    }
    
    public CompressedSeq(String seq){
        int i = 0, pos = 0;
        this.CompSeq = Collections.synchronizedList(new ArrayList<Long>(seq.length() / 21));
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
    
    public Stream<Long> getStream(){
        return this.CompSeq.stream();
    }
    
    public void destroy(){
        this.CompSeq.clear();
        this.CompSeq = null;
    }
}
