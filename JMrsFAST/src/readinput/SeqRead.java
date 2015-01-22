/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package readinput;

import static readinput.StringUtils.*;

/**
 * Individual class for sequence reads to be held in a container for later use
 * @author desktop
 */
public class SeqRead {
    private int hits;
    public CompressedSeq compSeq;
    public CompressedSeq compRevSeq;
    private String rName;
    // 0 = A, 1 = C, 2 = G, 3 = T, 4 = N
    private final short[] baseCnt = new short[5];
    
    public SeqRead(String seq, String rName){
        
    }
    
    private void CompressSeq(String seq){
        compSeq = new CompressedSeq(seq);
        
        // Now reverse strand
        compRevSeq = new CompressedSeq(ReverseComplement(seq));
    }
    
    private void BaseCount(String seq){
        for(int i = 0; i < seq.length(); i++){
            baseCnt[BaseToIndex(seq.charAt(i))] += 1;
        }
    }
    
    
}
