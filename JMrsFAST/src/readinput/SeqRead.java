/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package readinput;

import java.util.ArrayList;
import java.util.List;
import static readinput.StringUtils.*;

/**
 * Individual class for sequence reads to be held in a container for later use
 * @author desktop
 */
public class SeqRead {
    private final List<CompressedHit> hits = new ArrayList<>();
    public CompressedSeq compSeq;
    public CompressedSeq compRevSeq;
    private final String rName;
    private final String qual;
    // 0 = A, 1 = C, 2 = G, 3 = T, 4 = N
    private final byte[] baseCnt = new byte[5];
    
    public SeqRead(String seq, String rName, String qual){
        this.rName = rName;
        this.qual = qual;
        CompressSeq(seq);
        BaseCount(seq);
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
    
    public void AddHit(CompressedHit h){
        this.hits.add(h);
    }
    
    public int GetBaseCnt(String c){
        return (int) baseCnt[BaseToIndex(c.charAt(0))];
    }
}
