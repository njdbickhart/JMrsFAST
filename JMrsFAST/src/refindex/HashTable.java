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
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
    private final int seqLen;
    private int refGenNamelen;
    private String refGenName;
    private int refGenOffset = 0;
    private int refGenLength = 0;
    private int compRefGenLen = 0;
    private List<GeneralIndex> refGenIdx;
    protected CompressedSeq cRefGen;
    protected final Map<Long, List<GeneralIndex>> HashTable = new ConcurrentHashMap<>();
    protected AlphaCounter aCount;
    private int moreMaps = 0;
    
    public HashTable(byte extraInfo, int windowSize, int checkSumLen, int seqLen){
        this.extraInfo = extraInfo;
        this.windowSize = windowSize;
        this.checkSumLen = checkSumLen;
        this.seqLen = seqLen;
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
        
        // Finally, q-gram counting
        List<Future<byte[]>> holder = new ArrayList<>(threadCount);
        jobRunner = Executors.newFixedThreadPool(threadCount);
        for(i = 0; i < threadCount; i++){
            holder.add(jobRunner.submit(new QGramCounter(i, threadCount, seqLen, compRefGenLen, cRefGen)));
        }
        
        jobRunner.shutdown();
        while(!jobRunner.isTerminated()){}
        
        List<byte[]> holderF = new ArrayList<>(threadCount);
        for(Future<byte[]> h : holder){
            holderF.add(h.get());
        }
        holder = null;
        aCount = new AlphaCounter(holderF);
        holderF = null;
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
    
    // The QGram data array is a series of 4 bytes per compressed location
    // Byte order is:
    //  A = 0
    //  C = 1
    //  G = 2
    //  T = 3
    //  N is ignored
    protected class QGramCounter implements Callable<byte[]>{
        private final int threadid;
        private final int seqLen;
        private final int rgBlockStart;
        private final int rgBlockLen;
        private final int rgBlockIt;
        private final CompressedSeq cRefGen;
        private AlphaCounter aCount;
        
        // The constructor calculates the portions of the compressed reference sequence to process in this thread
	public QGramCounter(int threadid, int threadCount, int seqLen, int cRefGenLen, CompressedSeq cRefGen){
            int rgBlockSize = cRefGenLen / threadCount;
            rgBlockStart = (rgBlockSize * threadid * 21);
            if (threadid == threadCount - 1)
            {
                rgBlockLen = cRefGenLen - threadid * rgBlockSize *21;
                rgBlockIt = rgBlockLen;
            }else{
                rgBlockLen = rgBlockSize * 21;
                rgBlockIt = rgBlockLen + seqLen - 1;
            }
            
            this.threadid = threadid;
            this.seqLen = seqLen;
            this.cRefGen = cRefGen;
        }

        @Override
        public byte[] call() throws Exception {
            int i, t, aCounter = 0, cSeqIdx = rgBlockStart;
            long cdata, val;
            
            // assign memory to the counter class
            aCount = new AlphaCounter(4 * rgBlockLen, threadid);
            
            cdata = cRefGen.CompSeq.get(cSeqIdx++);
            t = 0;
            byte outgoingChar[] = new byte[seqLen];

            for (i = 0; i < seqLen; i++){
                    val = (cdata >> 60) & 7;
                    outgoingChar[i] = (byte) val;

                    if (++t == 21){
                            t = 0;
                            cdata = cRefGen.CompSeq.get(cSeqIdx++);
                    }else{
                            cdata <<= 3;
                    }
                    if (val != 4)
                            aCount.increaseByte(aCounter, (byte) val);
            }

            int o = 0;

            while (i++ < rgBlockIt) // BORDER LINE CHECK
            {
                    ++aCounter;
                    val = (cdata >> 60) & 7;
                    if (++t == 21){
                            t = 0;
                            cdata = cRefGen.CompSeq.get(cSeqIdx++);
                    }else{
                            cdata <<= 3;
                    }

                    aCount.copyByte(cSeqIdx);
                    if (val != 4)
                            aCount.increaseByte(aCounter, (byte) val);
                    if (outgoingChar[o]!= 4)
                        aCount.decreaseByte(aCounter, outgoingChar[o]);
                    outgoingChar[o] = (byte) val;
                    o = (++o == seqLen) ?0 :o;
            }
            return aCount.counter;
        }
    }
    
    public List<GeneralIndex> getCandidates(long hv){
        if(this.HashTable.containsKey(hv))
            return this.HashTable.get(hv);
        else
            return null; // returning null for no values within the hash table
    }
}
