/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package workers;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import output.AbstractAlnOutput;
import readinput.SeqReadFactory;
import readinput.TempHitOutput;
import refindex.GeneralIndex;
import refindex.HashTable;

/**
 *
 * @author desktop
 */
public class allHitsMapper {
    private final int threads;
    private final int maxHits;
    private final HashTable refHash;
    private final SeqReadFactory reads;
    private final TempHitOutput tempFile;
    private ExecutorService executor;
    
    public allHitsMapper(HashTable refHash, SeqReadFactory reads, int threads, int maxHits, TempHitOutput tempFile){
        this.threads = threads;
        this.maxHits = maxHits;
        this.refHash = refHash;
        this.reads = reads;
        this.tempFile = tempFile;
        this.executor = Executors.newFixedThreadPool(threads);
    }
    
    public void Map(){
        Set<Long> readHKeys = reads.getHashKeys();
        for(Long hv : readHKeys){
            List<GeneralIndex> readList = reads.getReadList(hv);
            List<GeneralIndex> refList = refHash.getCandidates(hv);
            
            // If there is a corresponding hash in the reference genome!
            if(refList != null){
                executor.submit(new MapQueue(readList, refList, tempFile));
            }else{  // Print this out as a "null" hit for retrieval later
                
            }
        }
    }
    
    private class MapQueue implements Runnable{
        
        public MapQueue(List<GeneralIndex> refLocs, List<GeneralIndex> readLocs, TempHitOutput tempFile){
            // this needs to be an implementation of the MaqSeq funciton in mrsfast.c
        }

        @Override
        public void run() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
    }
}
