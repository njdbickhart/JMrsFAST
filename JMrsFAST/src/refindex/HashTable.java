/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package refindex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import readinput.CompressedSeq;
import static refindex.HashUtils.byteArrayToInt;
import static refindex.HashUtils.calculateCompressedLen;
import static refindex.HashUtils.decodeVariableByte;
import static refindex.HashUtils.getByteSlice;

/**
 *
 * @author desktop
 */
public class HashTable {
    // From the MrsFAST source code:
    // 1 byte (extraInfo): Reserved; in case the contig has extra information
    // 2 bytes (len): Length of the reference genome name
    // n bytes (refGenName): Reference genome name
    // 4 bytes (refGenOfsset): Offset of the contig from the beginning of the chromosome
    // 4 bytes (refGenLength): Length of reference genome
    // n bytes (crefGen): compressed reference genome
    // 4 bytes (size): number of hashValues in hashTable with more than 0 locations
    // n bytes (bufferSize and buffer): array of bufferSize/buffer which includes encoded values of hashValue, count of locations
    
    private final byte extraInfo;
    private final int windowSize;
    private final int checkSumLen;
    private int refGenNamelen;
    private String refGenName;
    private int refGenOffset = 0;
    private int refGenLength = 0;
    private int compRefGenLen = 0;
    private List<GeneralIndex> refGenIdx;
    protected CompressedSeq cRefGen;
    protected final Map<Long, List<GeneralIndex>> HashTable = new ConcurrentHashMap<>();
    private int moreMaps = 0;
    
    public HashTable(byte extraInfo, int windowSize, int checkSumLen){
        this.extraInfo = extraInfo;
        this.windowSize = windowSize;
        this.checkSumLen = checkSumLen;
    }
    
    public void generateHashFromBlock(byte[] block, int HashTbleMemSize, int IOBufferSize) throws Exception{
        int blockCounter = 0;
        byte[] temp = {block[1], block[2]};
        refGenNamelen = byteArrayToInt(temp);
        
        refGenName = new String(getByteSlice(block, 3, refGenNamelen), "UTF-8");
        blockCounter = 3 + refGenNamelen + 1;
        
        refGenOffset = byteArrayToInt(getByteSlice(block, blockCounter, 4));
        blockCounter += 4;
        
        refGenLength = byteArrayToInt(getByteSlice(block, blockCounter, 4));
        blockCounter += 4;
        
        compRefGenLen = calculateCompressedLen(refGenLength);
        
        this.cRefGen = new CompressedSeq(getByteSlice(block, blockCounter, compRefGenLen));
        
        //int compSeqstart = blockCounter;
        int compSeqend = blockCounter + compRefGenLen;
        
        int HashTbleSize = byteArrayToInt(getByteSlice(block, compSeqend + 1, 4));
        
        // Decoding loop
        int index, i = 0, diff = 0, tmpSize = 0;
        long hashVal = 0l;
         
        //this.refGenIdx = Collections.synchronizedList(new ArrayList<>(HashTbleMemSize));
        while(i < HashTbleSize){
            int bytesToRead = byteArrayToInt(getByteSlice(block, blockCounter, 4));
            blockCounter += 4;
            byte[] buffer = getByteSlice(block, blockCounter, bytesToRead);
            blockCounter += bytesToRead;
            index = 0;
            
            while(index < bytesToRead){
                index += decodeVariableByte(buffer, index, diff);
                index += decodeVariableByte(buffer, index, tmpSize);
                
                hashVal += diff;
                
                // I need to figure out here what the hell is going on with the GeneralIndex array
                //this.refGenIdx.add(new GeneralIndex(refGenLength + 1));
                this.HashTable.put(hashVal, Collections.synchronizedList(new ArrayList<>(HashTbleMemSize)));
                this.HashTable.get(hashVal).add(new GeneralIndex(refGenLength + 1));
                
                i++;
            }
        }
        
        // Now, the authors stated that calculating the hash on the fly has huge benefits over disk IO
        // It's time to leverage easy threading to generate this table.
        int threadCount = Integer.valueOf(System.getProperty("java.util.concurrent.ForkJoinPool.common.parallelism"));
        ExecutorService jobRunner = Executors.newFixedThreadPool(threadCount);
        for(i = 0; i < threadCount; i++){
            jobRunner.submit(new GenerateHashOnFly(this.windowSize, this.checkSumLen, this.refGenLength, i, threadCount, this.cRefGen, this.HashTable));
        }
        
        jobRunner.shutdown();
        while(!jobRunner.isTerminated()){}
        
        // Now parallel quicksorting
        // This will be an unoptimized, default sort for now
        this.HashTable.entrySet().parallelStream().forEach(s -> {
            List<GeneralIndex> value = s.getValue();
            Collections.sort(value, Collections.reverseOrder());
        });
    }
    
    protected class GenerateHashOnFly implements Runnable{
        private final int windowSize;
        private final int checkSumLen;
        private final int refGenLen;
        private final int threadid;
        private final int maxThreads;
        private final CompressedSeq cRefGen;
        private final Map<Long, List<GeneralIndex>> HashTable;
        
        public GenerateHashOnFly(int windowSize, int checkSumLen, int refGenLen, int threadid, int maxThreads, CompressedSeq cRefGen, Map<Long, List<GeneralIndex>> HashTable){
            this.windowSize = windowSize;
            this.checkSumLen = checkSumLen;
            this.refGenLen = refGenLen;
            this.threadid = threadid;
            this.maxThreads = maxThreads;
            this.cRefGen = cRefGen;
            this.HashTable = HashTable;
        }
        
        
        @Override
        public void run() {
            int windowMaskSize = windowSize + checkSumLen;
            // I had to estimate the size of "unsigned long long" to be 64 bits here
            long windowMask =   0xffffffffffffffffl >> (64*8 - windowMaskSize*2);
            long checkSumMask = 0xffffffffffffffffl >> (64*8 - (checkSumLen)*2);
            if (checkSumLen == 0)
                    checkSumMask = 0;

            int cRefGenIdx = 0;
            long cdata = this.cRefGen.CompSeq.get(cRefGenIdx);

            int i = 0;
            long hv = 0;
            long hvtemp;
            long val;
            int  t = 0, stack = 1;
            int loc = - this.windowSize - checkSumLen + 1 ;
            Map<Long, Integer> hashIdxCount = new HashMap<>();
            // calculate refGen hashValues
            while (i++ < this.refGenLen ) // BORDER LINE CHECK
            {
                    loc++;
                    val = (cdata >> 60) & 7;
                    if (++t == 21){
                            t = 0;
                            cRefGenIdx++;
                            cdata = this.cRefGen.CompSeq.get(cRefGenIdx);
                    }else{
                            cdata <<= 3;
                    }

                    if (val != 4 && stack == windowMaskSize){
                            hv = ((hv << 2)|val)&windowMask;
                            hvtemp = hv >> (checkSumLen << 1);

                            if (hvtemp % this.maxThreads == this.threadid)
                            {
                                if(!hashIdxCount.containsKey(hvtemp))
                                    hashIdxCount.put(hvtemp, 0);
                                
                                int idx = hashIdxCount.get(hvtemp);
                                this.HashTable.get(hvtemp).get(idx).info = loc;
                                this.HashTable.get(hvtemp).get(idx).checksum = (short) (hv & checkSumMask);
                                hashIdxCount.put(hvtemp, idx);
                            }
                    }else{
                            if (val == 4) // Value of the 'N' Nucleotide
                            {
                                    stack = 1;
                                    hv = 0;
                            }
                            else
                            {
                                    stack ++;
                                    hv = (hv <<2)|val;
                            }

                    }
            }

        }
        
    }
    
    protected class QGramCounter implements Runnable{
        private final int threadid;
        private final int threadCount;
        private final int seqLen;
        private final int cRefGenLen;
        
	public QGramCounter(){
            
        }

        @Override
        public void run() {

            CompressedSeq *cnext, cdata;
            int i, t, val;

            int rgBlockSize = _ih_crefGenLen / THREAD_COUNT;
            int rgBlockStart = (rgBlockSize * id * 21);
            int rgBlockLen = rgBlockSize * 21;
            int rgBlockIt = rgBlockLen + SEQ_LENGTH - 1;
            if (id == THREAD_COUNT - 1)
            {
                    rgBlockLen = _ih_refGenLen - id*rgBlockSize*21;
                    rgBlockIt = rgBlockLen;
            }

            cnext = _ih_crefGen+(id*rgBlockSize);
            cdata = *(cnext++);
            t = 0;
            char outgoingChar[SEQ_LENGTH];
            unsigned int *copy = (unsigned int *)(_ih_alphCnt+4*rgBlockStart);
            unsigned char *cur = (unsigned char *)copy;		// current loc	// current loc
            *copy = 0;

            for (i = 0; i < SEQ_LENGTH; i++)
            {
                    val = (cdata >> 60) & 7;
                    outgoingChar[i] = val;

                    if (++t == 21)
                    {
                            t = 0;
                            cdata = *(cnext++);
                    }
                    else
                    {
                            cdata <<= 3;
                    }
                    if (val != 4)
                            (*(cur+val)) ++;
            }

            int o = 0;

            while (i++ < rgBlockIt) // BORDER LINE CHECK
            {
                    cur = (unsigned char *)++copy;
                    val = (cdata >> 60) & 7;
                    if (++t == 21)
                    {
                            t = 0;
                            cdata = *(cnext++);
                    }
                    else
                    {
                            cdata <<= 3;
                    }

                    *copy = *(copy-1);	// copies all 4 bytes at once
                    if (val != 4)
                            (*(cur + val)) ++;
                    if (outgoingChar[o]!= 4)
                            (*(cur + outgoingChar[o])) --;
                    outgoingChar[o] = val;
                    o = (++o == SEQ_LENGTH) ?0 :o;
            }
        }
    }
}
