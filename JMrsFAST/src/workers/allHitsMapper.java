/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package workers;

import datatypes.SamplingLocs;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import readinput.SeqReadFactory;
import output.TempHitOutput;
import output.TempMaxHitOutput;
import readinput.CompressedSeq;
import readinput.SeqRead;
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
                executor.submit(new MapQueue(readList, refList, reads.getReadInfoList(), refHash.getCRefGenSeq(), tempFile, slocs));
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
        
        public MapQueue(List<GeneralIndex> refLocs, List<GeneralIndex> readLocs, List<SeqRead> readChunk, CompressedSeq cRefGen, TempMaxHitOutput tempFile, SamplingLocs slocs){
            // this needs to be an implementation of the MaqSeq funciton in mrsfast.c
            this.refLocs = refLocs;
            this.readLocs = readLocs;
            this.tempFile = tempFile;
            this.readChunk = readChunk;
            this.cRefGen = cRefGen;
            this.slocs = slocs;
        }

        @Override
        public void run() {
            GeneralIndex SeqInfo = this.readLocs.get(0);
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
            if (refSampLocSize == 0 || readSampLocSize == 0)
            {
                    return;
            }
            else if (refSampLocSize == readSampLocSize && refSampLocSize <= 200)
            {
                    int j = 0;
                    int z = 0;
                    int genInfo;
                    int seqInfo;
                    CompressedSeq _tmpCmpSeq;
                    char tmp[];
                    unsigned char *alph, *gl;
                    char rqual[QUAL_LENGTH];
                    rqual[QUAL_LENGTH] = '\0';
                    char *_tmpQual, *_tmpSeq;

                    if (direction > 0){
                            genInfo		= refLocIdx;
                            seqInfo		= readLocIdx;
                    }else{
                            genInfo		= readLocIdx;
                            seqInfo		= refLocIdx;
                    }


                    for (j=0; j<readSampLocSize; j++)
                    {
                            int re = slocs.samplingLocsSize * 2;
                            int r = seqInfo[j].info / re;

                            int x = seqInfo[j].info % re;
                            int o = x % slocs.samplingLocsSize;
                            char d = (x/slocs.samplingLocsSize)?1:0;

                            if (_msf_seqList[r].hits[0] > maxHits)
                                    continue;

                            if (d)
                            {
                                    _tmpCmpSeq = _msf_seqList[r].crseq;
                                    tmp[0]=_msf_seqList[r].alphCnt[3];
                                    tmp[1]=_msf_seqList[r].alphCnt[2];
                                    tmp[2]=_msf_seqList[r].alphCnt[1];
                                    tmp[3]=_msf_seqList[r].alphCnt[0];
                                    alph = tmp;
                                    _tmpQual = &rqual[0];
                                    reverse(_msf_seqList[r].qual, _tmpQual, QUAL_LENGTH);
                                    _tmpSeq = _msf_seqList[r].rseq;
                            }
                            else
                            {
                                    _tmpCmpSeq = _msf_seqList[r].cseq;
                                    alph = _msf_seqList[r].alphCnt;
                                    _tmpQual = _msf_seqList[r].qual;
                                    _tmpSeq = _msf_seqList[r].seq;
                            }


                            for (z=0; z<refSampLocSize; z++)
                            {

                                    int genLoc = genInfo[z].info-_msf_samplingLocs[o];

                                    if (genLoc < _msf_refGenBeg || genLoc > _msf_refGenEnd)
                                            continue;

                                    int err = -1;
                                    gl = _msf_alphCnt + ((genLoc-1)<<2);

                                    if ( SNPMode || abs(gl[0]-alph[0]) + abs(gl[1]-alph[1]) + abs(gl[2]-alph[2]) + abs(gl[3]-alph[3]) <= _msf_maxDistance )
                                            err = verifySeq(genLoc, _tmpCmpSeq, o, id);

                                    if (err != -1)
                                    {
                                            unsigned char mderr = calculateMD(genLoc, _tmpCmpSeq, _tmpSeq, _tmpQual, err, &_msf_op[id]);
                                            unsigned char mdlen = strlen(_msf_op[id]);
                                            if (mderr < 0)
                                                    continue;

                                            _msf_mappingCnt[id]++;
                                            _msf_seqList[r].hits[0]++;

                                            if (_msf_seqList[r].hits[0] == 1)
                                                    _msf_mappedSeqCnt[id]++;

                                            if (_msf_seqList[r].hits[0] > maxHits)
                                            {
                                                    _msf_mappedSeqCnt[id]--;
                                                    _msf_mappingCnt[id] -= (maxHits+1);
                                                    break;
                                            }

                                            int tmpOut;
                                            int flag = 16 * d;
                                            int loc = genLoc + _msf_refGenOffset;

                                            pthread_mutex_lock(&_msf_writeLock);
                                            tmpOut = fwrite(&r, sizeof(int), 1, _msf_hitsTempFile);
                                            tmpOut = fwrite(&flag, sizeof(int), 1, _msf_hitsTempFile);
                                            tmpOut = fwrite(&loc, sizeof(int), 1, _msf_hitsTempFile);
                                            if (SNPMode)
                                                    tmpOut = fwrite(&err, sizeof(char), 1, _msf_hitsTempFile);
                                            tmpOut = fwrite(&mderr, sizeof(char), 1, _msf_hitsTempFile);
                                            tmpOut = fwrite(&mdlen, sizeof(char), 1, _msf_hitsTempFile);
                                            tmpOut = fwrite(_msf_op[id], sizeof(char), mdlen, _msf_hitsTempFile);
                                            pthread_mutex_unlock(&_msf_writeLock);
                                    }

                            }
                    }
            }
            else
            {
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
