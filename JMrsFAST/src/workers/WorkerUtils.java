/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package workers;

import datatypes.seqError;
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
    
    // The first int returned is the error count. The second is the errorSamp (because Java does not allow two variable returns)
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
