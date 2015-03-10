/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package refindex;

/**
 *
 * @author bickhart
 */
public class GeneralIndex implements Comparable<GeneralIndex>{
    public int checksum;
    public int info;
    
    public GeneralIndex(){
        
    }
    public GeneralIndex(int info){
        this.checksum = 0;
        this.info = info;
    }
    public GeneralIndex(int checksum, int info){
        this.checksum = checksum;
        this.info = info;
    }
    
    @Override
    public int compareTo(GeneralIndex t) {
        return this.checksum - t.checksum;
    }
    
    
}
