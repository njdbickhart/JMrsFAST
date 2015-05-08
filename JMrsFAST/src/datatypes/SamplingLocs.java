/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package datatypes;

import jmrsfast.ErrorSamples;

/**
 *
 * @author Dbick
 */
public class SamplingLocs{
        public int samplingLocsSize;
        public int samplingLocs[];
        public int samplingLocsSeg[];
        public int samplingLocsOffset[];
        public int samplingLocsLen[];
        public int samplingLocsLenFull[];
        public final int errThreshold;
        public final int windowSize;
        public final int seqLen;
        public final byte[] errorCount;
        
        public SamplingLocs(int errThreshold, int windowSize, int seqLen, ErrorSamples errors){
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
            
            int x;
            this.errorCount = errors.errorCount;
        }
    }
