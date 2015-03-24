/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package workers;

import datatypes.seqError;
import datatypes.seqMD;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import readinput.CompressedSeq;

/**
 *
 * @author bickhart
 */
public class WorkerUtils {
    private static final Logger log = Logger.getLogger(WorkerUtils.class.getName());
    
    public static seqMD getMDValue(CompressedSeq ref, int refGenLoc, CompressedSeq read, int readLen, int err){
        seqMD mdtags = new seqMD();
        int mod = refGenLoc % 21;
        long diff, diffMask = 7l;
        int shifts = (20 - mod) * 3;        
        
        if(err > 0 || err == -1){
            for(int i = 0; i < readLen; i++){
                // From MrsFAST.c 
                if (diffMask == 7)
                    {
                            diffMask = 0x7000000000000000;
                            tmpref = (*ref << refALS) | (*(1+ref) >> refARS);
                            ref++;
                            diff = (tmpref ^ *(cmpSeq++));
                    }
                    else
                            diffMask >>= 3;

                    if (diff & diffMask)		// ref[index + i - 1 ] != ver[i]
                    {
                            err++;
                            if (matchCnt)
                            {
                                    if (matchCnt < 10)
                                    {
                                            op[pp++]=_msf_numbers[matchCnt][0];
                                    }
                                    else if (matchCnt < 100)
                                    {
                                            op[pp++]=_msf_numbers[matchCnt][0];
                                            op[pp++]=_msf_numbers[matchCnt][1];
                                    }
                                    else
                                    {
                                            op[pp++]=_msf_numbers[matchCnt][0];
                                            op[pp++]=_msf_numbers[matchCnt][1];
                                            op[pp++]=_msf_numbers[matchCnt][2];
                                    }

                                    matchCnt = 0;
                            }
                            op[pp++] = alphabet[ (*refPos >> shifts) & 7 ];
                    }
                    else
                    {
                            matchCnt++;
                    }

                    if (shifts == 0)
                    {
                            refPos++;
                            shifts = 60;
                    }
                    else
                            shifts -= 3;
            }
        }else if (err == 0)
	{
		matchCnt = SEQ_LENGTH;
	}

	if (matchCnt>0)
	{
		if (matchCnt < 10)
		{
			op[pp++]=_msf_numbers[matchCnt][0];
		}
		else if (matchCnt < 100)
		{
			op[pp++]=_msf_numbers[matchCnt][0];
			op[pp++]=_msf_numbers[matchCnt][1];
		}
		else
		{
			op[pp++]=_msf_numbers[matchCnt][0];
			op[pp++]=_msf_numbers[matchCnt][1];
			op[pp++]=_msf_numbers[matchCnt][2];
		}
	}
	op[pp]='\0';
        
        return mdtags;
    }
    
    // I created a separate class to carry both int values to the calling class
    public static seqError countErrors(CompressedSeq ref, int refGenLoc, CompressedSeq read, int readCurLoc, int len, int allowedError, byte[] errorCounter){
        long tmpref = 0, tmpseq = 0, diff;
	int err = 0, errSamp = 0;
        seqError seqE = new seqError();

	while(len >= 21){
            try{
		tmpref = ref.retrieveCSeqFromList(refGenLoc);
		tmpseq = read.retrieveCSeqFromList(readCurLoc);
            }catch(Exception ex){
                log.log(Level.SEVERE, "[COUNTERROR] Error retrieving sequence from cSEQ!", ex);
            }
            refGenLoc += 21; 
            readCurLoc += 21;
            diff = (tmpref ^ tmpseq) & 0x7fffffffffffffffl;

            errSamp |= (tmpseq & jmrsfast.Constants.NMASK);

            err += errorCounter[Math.toIntExact(diff & 0xffffff)] 
                    + errorCounter[Math.toIntExact((diff>>24)&0xffffff)] 
                    + errorCounter[Math.toIntExact((diff>>48)&0xfffff)];
                        
            if (err > allowedError){
                seqE.error = err;
                seqE.sampleError = errSamp;
                seqE.isUsable = false;
                return seqE;
            }
            len -= 21;
	}

	if (len > 0){
            try{
		tmpref = ref.retrieveCSeqFromList(refGenLoc);
		tmpseq = read.retrieveCSeqFromList(readCurLoc);
            }catch(Exception ex){
                log.log(Level.SEVERE, "[COUNTERROR] Error retrieving sequence from cSEQ in len overflow!", ex);
            }
            refGenLoc += 21; 
            readCurLoc += 21;
            diff = (tmpref ^ tmpseq) & 0x7fffffffffffffffl;

            diff >>= (63 - len*3);
            tmpseq  >>= (63 - len*3);

            errSamp |= (tmpseq & jmrsfast.Constants.NMASK);

            err += errorCounter[Math.toIntExact(diff & 0xffffff)] 
                    + errorCounter[Math.toIntExact((diff>>24)&0xffffff)] 
                    + errorCounter[Math.toIntExact((diff>>48)&0xfffff)];

            if (err > allowedError){
                seqE.error = err;
                seqE.sampleError = errSamp;
                seqE.isUsable = false;
                return seqE;
            }
	}

	errSamp |= err;
        seqE.error = err;
        seqE.sampleError = errSamp;
	return seqE;
    }
    
    public static byte[] getErrorCounter(){
        byte[] errorCounter = new byte[2097152]; // 1 << 24
        for(int i = 0; i < 2097152; i++){
            errorCounter[i] = 0;
            for(int x = 0; x < 8; x++){
                if ((i & (7 << 3*x)) > 0){
                    errorCounter[i]++;
                }
            }
        }
        return errorCounter;
    }
}
