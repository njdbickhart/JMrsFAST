/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package readinput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import jmrsfast.Constants.MODE;
import refindex.GeneralIndex;

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
    private final List<SeqRead> readChunk;
    private final Map<Long, List<GeneralIndex>> HashTable = new ConcurrentHashMap<>();
    
    public SeqReadFactory(MODE mode, int bufferSize, int threadCount, int errThreshold, int windowSize, int checkSumLen, int seqLen){
        this.mode = mode;
        this.bufferSize = bufferSize;
        this.threadCount = threadCount;
        this.sLocs = new SamplingLocs(errThreshold, windowSize, seqLen);
        this.errThreshold = errThreshold;
        this.windowSize = windowSize;
        this.checkSumLen = checkSumLen;
        this.seqLen = seqLen;
        this.readChunk = new ArrayList<>(bufferSize);
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
    
    private void hashTmpSorting(){
        
    } 
    
    // Class designed to emulate Reads.c PreProcessReads
    private class ReadHasher implements Runnable{
        private final SamplingLocs slocs;
        private final List<SeqRead> reads;
        private final Map<Long, List<GeneralIndex>> HashTable;
        
        public ReadHasher(SamplingLocs slocs, List<SeqRead> reads, Map<Long, List<GeneralIndex>> HashTable){
            this.slocs = slocs;
            this.reads = reads;
            this.HashTable = HashTable;
        }
        
        @Override
        public void run() {
            Map<Long, List<SeqPair>> pairHolder = new HashMap<>();
            for(SeqRead r : reads){
                if(r.GetBaseCnt("N") > slocs.errThreshold){
                    continue; // This read had more "N" bases than we had errorthreshold
                }
                
                for (int j=0; j< slocs.samplingLocsSize; j++){
                    
                        long hvtmp = hashVal(_r_seq[i].seq+_r_samplingLocs[j]);
                        short cstmp = checkSumVal(_r_seq[i].seq+_r_samplingLocs[j]+WINDOW_SIZE);
                        if (hvtmp == -1 || cstmp == -1)
                        {
                                tmp[pos].hv = -1;
                                tmp[pos].checksum = 0;
                        }
                        else
                        {
                                tmp[pos].hv = hvtmp;
                                tmp[pos].checksum = cstmp;
                        }
                        tmp[pos].seqInfo = pos +(div*id*2*_r_samplingLocsSize);
                        pos++;
                }

                for (j=0; j<_r_samplingLocsSize; j++){
                        hvtmp = hashVal(_r_seq[i].rseq+_r_samplingLocs[j]);
                        cstmp = checkSumVal(_r_seq[i].rseq+_r_samplingLocs[j]+WINDOW_SIZE);

                        if (hvtmp == -1  || cstmp == -1)
                        {
                                tmp[pos].hv = -1;
                                tmp[pos].checksum = 0;
                        }
                        else
                        {
                                tmp[pos].hv = hvtmp;
                                tmp[pos].checksum = cstmp;
                        }
                        tmp[pos].seqInfo = pos+(div*id*2*_r_samplingLocsSize);
                        pos++;
                }
            }
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
}
