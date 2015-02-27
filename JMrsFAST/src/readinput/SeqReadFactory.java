/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package readinput;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    private final int seqLen;
    private final List<SeqRead> readChunk;
    private final Map<Long, List<GeneralIndex>> HashTable = new ConcurrentHashMap<>();
    
    public SeqReadFactory(MODE mode, int bufferSize, int threadCount, int errThreshold, int windowSize, int seqLen){
        this.mode = mode;
        this.bufferSize = bufferSize;
        this.threadCount = threadCount;
        this.sLocs = new SamplingLocs(errThreshold, windowSize, seqLen);
        this.errThreshold = errThreshold;
        this.windowSize = windowSize;
        this.seqLen = seqLen;
    }
    
    private class SamplingLocs{
        public int samplingLocsSize;
        public int samplingLocs[];
        public int samplingLocsSeg[];
        public int samplingLocsOffset[];
        public int samplingLocsLen[];
        public int samplingLocsLenFull[];
        
        public SamplingLocs(int errThreshold, int windowSize, int seqLen){
            int i;
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
}
