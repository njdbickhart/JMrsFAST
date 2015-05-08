/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package workers;

import datatypes.SamplingLocs;
import datatypes.seqMD;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import readinput.SeqReadFactory;
import output.TempHitOutput;
import output.TempMaxHitOutput;
import readinput.CompressedSeq;
import readinput.SeqRead;
import refindex.AlphaCounter;
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
    private final TempMaxHitOutput tempFile;
    private ExecutorService executor;
    private final SamplingLocs slocs;
    
    public allHitsMapper(HashTable refHash, SeqReadFactory reads, int threads, int maxHits, TempMaxHitOutput tempFile, SamplingLocs slocs){
        this.threads = threads;
        this.maxHits = maxHits;
        this.refHash = refHash;
        this.reads = reads;
        this.tempFile = tempFile;
        this.slocs = slocs;
        this.executor = Executors.newFixedThreadPool(threads);
    }
    
    public void Map(){
        Set<Long> readHKeys = reads.getHashKeys();
        for(Long hv : readHKeys){
            List<GeneralIndex> readList = reads.getReadList(hv);
            List<GeneralIndex> refList = refHash.getCandidates(hv);
            
            // If there is a corresponding hash in the reference genome!
            if(refList != null){
                executor.submit(new MapQueue(refList, readList, reads.getReadInfoList(), refHash, tempFile, slocs));
            }else{  // Print this out as a "null" hit for retrieval later
                
            }
        }
    }
    
    private class MapQueue implements Runnable{
        private final List<GeneralIndex> refLocs;
        private final List<GeneralIndex> readLocs;
        private final TempMaxHitOutput tempFile;
        private final List<SeqRead> readChunk;
        private final CompressedSeq cRefGen;
        private final SamplingLocs slocs;
        private final AlphaCounter refAlphaCnt;
        private final int refGenOffset;
        private final int refGenLen;
        private final int maxDist;
        
        public MapQueue(List<GeneralIndex> refLocs, List<GeneralIndex> readLocs, List<SeqRead> readChunk, HashTable hash, TempMaxHitOutput tempFile, SamplingLocs slocs){
            // this needs to be an implementation of the MaqSeq funciton in mrsfast.c
            this.refLocs = refLocs;
            this.readLocs = readLocs;
            this.tempFile = tempFile;
            this.readChunk = readChunk;
            this.cRefGen = hash.getCRefGenSeq();
            this.slocs = slocs;
            this.refGenOffset = hash.refGenOffset;
            this.refGenLen = hash.refGenLength;
            this.refAlphaCnt = hash.aCount;
            this.maxDist = slocs.errThreshold << 1;
        }

        @Override
        public void run() {
            //GeneralIndex SeqInfo = this.readLocs.get(0);
            int rb = 0, re = 1, sb = 0, se = 1;
            int rs = this.readLocs.get(0).info;
            int ss = this.refLocs.get(0).info;
            
            // This is where they advance the SeqInfo and GenInfo arrays, but I'm not sure why
            while(rb < rs){
                while (re < rs && this.readLocs.get(re).checksum == this.readLocs.get(rb).checksum) re++;
                while (sb < ss && this.refLocs.get(sb).checksum < this.readLocs.get(rb).checksum) sb++;

                if (this.readLocs.get(rb).checksum == this.refLocs.get(sb).checksum)
                {
                        se = sb+1;
                        while (se < ss && this.refLocs.get(se).checksum == this.refLocs.get(sb).checksum) se++;
                        mapSeqList (sb, se-sb, rb, re-rb);			
                }
                rb = re;
                re++;
            }
        }
        
        private void mapSeqList(int refLocIdx, int refSampLocSize, int readLocIdx, int readSampLocSize){
            if (refSampLocSize < readSampLocSize){
                    mapSeqListBal(refLocIdx, refSampLocSize, readLocIdx, refSampLocSize, 1);
                    mapSeqList(refLocIdx, refSampLocSize, readLocIdx+refSampLocSize, readSampLocSize-refSampLocSize);		
            }else if (refSampLocSize > readSampLocSize){
                    mapSeqListBal(refLocIdx, readSampLocSize, readLocIdx, readSampLocSize, 1);
                    mapSeqList(refLocIdx+readSampLocSize, refSampLocSize-readSampLocSize, readLocIdx, readSampLocSize);
            }else{
                    mapSeqListBal(refLocIdx, refSampLocSize, readLocIdx, readSampLocSize, 1);
            }
        }
        
        private void mapSeqListBal(int refLocIdx, int refSampLocSize, int readLocIdx, int readSampLocSize, int direction){
            // l1 is the refLocIdx for the private listing of ref genome generalIndex sites
            // l2 is the readLocIdx for the private listing of read generalIndex sites
            if (refSampLocSize == 0 || readSampLocSize == 0){
                    return;
            }else if (refSampLocSize == readSampLocSize && refSampLocSize <= 200){
                    int j = 0;
                    int z = 0;
                    int genInfo;
                    int seqInfo;
                    CompressedSeq _tmpCmpSeq;
                    int tmp[] = new int[4], readAlph[] = new int[4], refGenAlph[] = new int[4];
                    //char *_tmpQual, *_tmpSeq;

                    if (direction > 0){
                            genInfo		= refLocIdx;
                            seqInfo		= readLocIdx;
                    }else{
                            genInfo		= readLocIdx;
                            seqInfo		= refLocIdx;
                    }


                    for (j=0; j<readSampLocSize; j++){
                            int re = slocs.samplingLocsSize * 2;
                            int r = this.readLocs.get(seqInfo + j).info / re;

                            int x = this.readLocs.get(seqInfo + j).info % re;
                            int o = x % slocs.samplingLocsSize;
                            int d = (x/slocs.samplingLocsSize > 0)?1:0;

                            if (this.readChunk.get(r).hits > maxHits)
                                    continue;

                            if (d > 0){
                                    _tmpCmpSeq = this.readChunk.get(r).compRevSeq;
                                    // 0 = A, 1 = C, 2 = G, 3 = T, 4 = N
                                    // Since this is reversed, we use the reverse complement to get 
                                    // the count
                                    tmp[0]=this.readChunk.get(r).GetBaseCnt("T");
                                    tmp[1]=this.readChunk.get(r).GetBaseCnt("G");
                                    tmp[2]=this.readChunk.get(r).GetBaseCnt("C");
                                    tmp[3]=this.readChunk.get(r).GetBaseCnt("A");
                                    readAlph = tmp;
                            }else{
                                    _tmpCmpSeq = this.readChunk.get(r).compSeq;
                                    readAlph[0] = this.readChunk.get(r).GetBaseCnt("A");
                                    readAlph[1] = this.readChunk.get(r).GetBaseCnt("C");
                                    readAlph[2] = this.readChunk.get(r).GetBaseCnt("G");
                                    readAlph[3] = this.readChunk.get(r).GetBaseCnt("T"); 
                            }


                            for (z=0; z<refSampLocSize; z++){

                                    int genLoc = this.refLocs.get(genInfo +z).info - slocs.samplingLocs[o];

                                    if (genLoc < this.refGenOffset || genLoc > this.refGenLen)
                                            continue;

                                    int err = -1;
                                    // "gl" is the base count for this region of the genome
                                    refGenAlph = this.refAlphaCnt.getCounterAtLoc(genLoc - 1);
                                    
                                    // Since SNPMode is meaningless to me, this basically just checks the 
                                    // alpha count between the ref genome and the read sample. If they differ
                                    // then the sequence is verified, the error is counted and the sample's MD
                                    // is generated
                                    if (Math.abs(refGenAlph[0]-readAlph[0]) 
                                            + Math.abs(refGenAlph[1]-readAlph[1]) 
                                            + Math.abs(refGenAlph[2]-readAlph[2]) 
                                            + Math.abs(refGenAlph[3]-readAlph[3]) <= this.maxDist)
                                            err = WorkerUtils.verifySeq(genLoc, this.cRefGen, 0, _tmpCmpSeq, o, this.slocs);

                                    if (err != -1){
                                        seqMD mderr = WorkerUtils.getMDValue(this.cRefGen, genLoc, _tmpCmpSeq, slocs.seqLen, err);
                                        
                                        if (!mderr.isUseable)
                                                continue;

                                        this.readChunk.get(r).hits++;

                                        //if (_msf_seqList[r].hits[0] == 1)
                                                //_msf_mappedSeqCnt[id]++;

                                        if (this.readChunk.get(r).hits > maxHits){
                                                //_msf_mappedSeqCnt[id]--;
                                                //_msf_mappingCnt[id] -= (maxHits+1);
                                                break;
                                        }

                                        int flag = 16 * d;
                                        int loc = genLoc + this.refGenOffset;

                                        try {
                                            this.tempFile.WriteMaxHit(r, flag, loc, mderr);
                                        } catch (IOException ex) {
                                            Logger.getLogger(allHitsMapper.class.getName()).log(Level.SEVERE, null, ex);
                                        }
                                    }

                            }
                    }
            }else{
                    int tmp1=refSampLocSize/2, tmp2= readSampLocSize/2;
                    if (tmp1 != 0)
                            mapSeqListBal(refLocIdx, tmp1, readLocIdx+tmp2, readSampLocSize-tmp2, direction);
                    mapSeqListBal(readLocIdx+tmp2, readSampLocSize-tmp2, refLocIdx+tmp1, refSampLocSize-tmp1, -direction);
                    if (tmp2 !=0)
                            mapSeqListBal(refLocIdx+tmp1, refSampLocSize-tmp1, readLocIdx, tmp2, direction);
                    if (tmp1 + tmp2 != 0)
                            mapSeqListBal(readLocIdx, tmp2, refLocIdx, tmp1, -direction);
            }
        }
    }
}
