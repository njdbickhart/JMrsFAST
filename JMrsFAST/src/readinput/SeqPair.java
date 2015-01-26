/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package readinput;

/**
 *
 * @author desktop
 */
public class SeqPair {
    private final SeqRead[] reads = new SeqRead[2];
    
    public SeqPair(SeqRead a, SeqRead b){
        reads[0] = a;
        reads[1] = b;
    }
}
