/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package readinput;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import jmrsfast.Constants.MODE;
import refindex.GeneralIndex;
import refindex.HashUtils;

/**
 *
 * @author desktop
 */
public class SeqReadFactory {
    private final int bufferSize;
    private MODE mode;
    private final int threadCount;
    private final SamplingLocs sLocs;
    private final int errThreshold;
    private final int windowSize;
    private final int checkSumLen;
    private final int seqLen;
    private ExecutorService executor;
    private final List<Future<SeqRead>> parallelBuffer;
    private List<SeqRead> readChunk;
    private final Map<Long, List<GeneralIndex>> HashTable = new ConcurrentHashMap<>();
    
    private final Logger log = Logger.getLogger(SeqReadFactory.class.getName());
    
    public SeqReadFactory(MODE mode, int bufferSize, int threadCount, int errThreshold, int windowSize, int checkSumLen, int seqLen){
        this.mode = mode;
        this.bufferSize = bufferSize;
        this.threadCount = threadCount;
        this.sLocs = new SamplingLocs(errThreshold, windowSize, seqLen);
        this.errThreshold = errThreshold;
        this.windowSize = windowSize;
        this.checkSumLen = checkSumLen;
        this.seqLen = seqLen;
        this.readChunk = Collections.synchronizedList(new ArrayList<>(bufferSize));
        this.executor = Executors.newFixedThreadPool(threadCount);
        this.parallelBuffer = Collections.synchronizedList(new ArrayList<>(bufferSize));
    }
    
    public void GenerateReadChunkFromFile(File fqFile){
        
    }
    
    public boolean BufferedReadChunk(String rname, String seq, String qual){
        SeqReadGenerator s = new SeqReadGenerator(seq, qual, rname);
        Future<SeqRead> t = executor.submit(s);
        
        parallelBuffer.add(t);
        // If we've reached the indicated buffer size, let's generate the hash table!
        if(parallelBuffer.size() >= bufferSize - 1){
            
            for(Future<SeqRead> f : parallelBuffer){
                try {
                    readChunk.add(f.get());
                } catch (InterruptedException | ExecutionException ex) {
                    log.getLogger(SeqReadFactory.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            // We want to maximize the number of threads during HashTable Generation, so let's split the readChunk into bit-sized threads
            int chunkCnt = readChunk.size() / threadCount;
            int offset = 0;
            while(offset < readChunk.size()){
                int endIdx = offset + chunkCnt;
                if(offset + (chunkCnt * 2) > readChunk.size() -1)
                    endIdx = readChunk.size() -1;
                
                executor.submit(new ReadHasher(sLocs, readChunk.subList(offset, endIdx), this.HashTable, this.windowSize, this.checkSumLen, offset));
            }
            
            executor.shutdown();
            
            while(executor.isTerminated()){}
            return true;
        }
        return false;
    }
    
    private class SamplingLocs{
        public int samplingLocsSize;
        public int samplingLocs[];
        public int samplingLocsSeg[];
        public int samplingLocsOffset[];
        public int samplingLocsLen[];
        public int samplingLocsLenFull[];
        public final int errThreshold;
        public final int windowSize;
        public final int seqLen;
        
        public SamplingLocs(int errThreshold, int windowSize, int seqLen){
            int i;
            this.errThreshold = errThreshold;
            this.windowSize = windowSize;
            this.seqLen = seqLen;
            samplingLocsSize = errThreshold + 1;
            samplingLocs = new int[samplingLocsSize + 1];
            for (i=0; i<samplingLocsSize; i++)
            {
                    samplingLocs[i] = (seqLen / samplingLocsSize) *i;
                    if ( samplingLocs[i] + windowSize > seqLen)
                            samplingLocs[i] = seqLen - windowSize;
            }
            samplingLocs[samplingLocsSize]=seqLen;

            samplingLocsSeg = new int[samplingLocsSize];
            samplingLocsOffset = new int[samplingLocsSize];
            samplingLocsLen = new int[samplingLocsSize];
            samplingLocsLenFull = new int[samplingLocsSize];
            for (i=0; i<samplingLocsSize; i++)
            {
                    samplingLocsSeg[i]		= samplingLocs[i] / (64*8/3);
                    samplingLocsOffset[i]	= samplingLocs[i] % (64*8/3);
                    samplingLocsLen[i]		= samplingLocs[i+1] - samplingLocs[i];
                    samplingLocsLenFull[i]	= seqLen - samplingLocs[i];
            }
        }
    }
        
    // Class designed to emulate Reads.c PreProcessReads
    private class ReadHasher implements Runnable{
        private final SamplingLocs slocs;
        private final List<SeqRead> reads;
        private final Map<Long, List<GeneralIndex>> HashTable;
        private final int windowSize;
        private final int checkSumLen;
        private final int offSetNum;
        
        public ReadHasher(SamplingLocs slocs, List<SeqRead> reads, Map<Long, List<GeneralIndex>> HashTable, int windowSize, int checkSumLen, int offSetNum){
            this.slocs = slocs;
            this.reads = reads;
            this.HashTable = HashTable;
            this.windowSize = windowSize;
            this.checkSumLen = checkSumLen;
            this.offSetNum = offSetNum;
        }
        
        @Override
        public void run() {
            Map<Long, List<SeqPair>> pairHolder = new HashMap<>();
            int pos = 0;
            for(SeqRead r : reads){
                if(r.GetBaseCnt("N") > slocs.errThreshold){
                    continue; // This read had more "N" bases than we had errorthreshold
                }
                
                for (int j=0; j< slocs.samplingLocsSize; j++){
                        
                        long hvtmp = HashUtils.hashVal(Arrays.copyOfRange(r.seq.toCharArray(), slocs.samplingLocs[j], r.seq.length()), this.windowSize);
                        int cstmp = HashUtils.checkSumVal(Arrays.copyOfRange(r.seq.toCharArray(), slocs.samplingLocs[j] + this.windowSize, r.seq.length()), this.checkSumLen);
                                                
                        if(!pairHolder.containsKey(hvtmp))
                            pairHolder.put(hvtmp, new ArrayList<>());
                        
                        pairHolder.get(hvtmp).add(new SeqPair());
                        if (hvtmp == -1 || cstmp == -1){                            
                            pairHolder.get(hvtmp).get(pairHolder.get(hvtmp).size() -1).checkSum = 0;
                        }else{
                            pairHolder.get(hvtmp).get(pairHolder.get(hvtmp).size() -1).checkSum = cstmp;
                        }
                        pairHolder.get(hvtmp).get(pairHolder.get(hvtmp).size() -1).seqInfo = pos + (this.offSetNum * slocs.samplingLocsSize);
                        pos++;
                }
                
                char[] rseq = StringUtils.ReverseComplement(r.seq).toCharArray();
                for (int j=0; j<slocs.samplingLocsSize; j++){
                        
                        long hvtmp = HashUtils.hashVal(Arrays.copyOfRange(rseq, slocs.samplingLocs[j], rseq.length), this.windowSize);
                        int cstmp = HashUtils.checkSumVal(Arrays.copyOfRange(rseq, slocs.samplingLocs[j] + this.windowSize, rseq.length), this.checkSumLen);
                        
                        if(!pairHolder.containsKey(hvtmp))
                            pairHolder.put(hvtmp, new ArrayList<>());
                        
                        pairHolder.get(hvtmp).add(new SeqPair());
                        if (hvtmp == -1  || cstmp == -1){
                                pairHolder.get(hvtmp).get(pairHolder.get(hvtmp).size() -1).checkSum = 0;
                        }else{
                                pairHolder.get(hvtmp).get(pairHolder.get(hvtmp).size() -1).checkSum = cstmp;
                        }
                        pairHolder.get(hvtmp).get(pairHolder.get(hvtmp).size() -1).seqInfo = pos + (this.offSetNum * slocs.samplingLocsSize);
                        pos++;
                }
            }
            
            int beg = 0, end = 0;
            for(Long hv : pairHolder.keySet()){
                if(!this.HashTable.containsKey(hv))
                    this.HashTable.put(hv, new ArrayList<>());
                end = beg + pairHolder.get(hv).size();
                this.HashTable.get(hv).add(new GeneralIndex(end - beg +1));
                
                for(SeqPair p : pairHolder.get(hv)){
                    this.HashTable.get(hv).add(new GeneralIndex(p.checkSum, p.seqInfo));
                }
                beg = end +1;
            }
            
            pairHolder.clear();
        }
        
    }
    
    // Private class to parallelize SeqRead compression and generation
    private class SeqReadGenerator implements Callable<SeqRead>{
        private final String seq;
        private final String qual;
        private final String rname;
        
        public SeqReadGenerator(String seq, String qual, String rname){
            this.seq = seq;
            this.qual = qual;
            this.rname = rname;
        }
        
        @Override
        public SeqRead call() throws Exception {
            return new SeqRead(seq, rname, qual);
        }
        
    }
    
    /*
    Getters
    */
    
    public SamplingLocs getSLocs(){
        return this.sLocs;
    }
    
    public List<GeneralIndex> getReadList(Long hv){
        if(this.HashTable.containsKey(hv))
            return this.HashTable.get(hv);
        else
            return null;
    }
    
    public List<SeqRead> getReadInfoList(){
        return this.readChunk;
    }
    
    public Set<Long> getHashKeys(){
        return this.HashTable.keySet();
    }
    
}
