/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package workers;

import java.util.List;
import readinput.CompressedSeq;

/**
 *
 * @author bickhart
 */
public class WorkerUtils {
    
    // The first int returned is the error count. The second is the errorSamp (because Java does not allow two variable returns)
    public static int[] countErrors(CompressedSeq ref, int refCurOff, CompressedSeq read, int readCurOff, int len, int errorSamp, int allowedError){
        int refALS = refCurOff * 3;
	int segALS = readCurOff * 3;


	int refARS = 63 - refALS;
	int segARS = 63 - segALS;	

	long tmpref, tmpseq, diff;
	int err = 0;


	while(len >= 21)
	{
		tmpref = (*ref << refALS) | (*(1+ref) >> refARS);
		tmpseq = (*seq << segALS) | (*(1+seq) >> segARS);
		ref++; 
		seq++;
		diff = (tmpref ^ tmpseq) & 0x7fffffffffffffff;

		*errSamp |= (tmpseq & _msf_NMASK);


		err += _msf_errCnt[diff & 0xffffff] + _msf_errCnt[(diff>>24)&0xffffff] + _msf_errCnt[(diff>>48)&0xfffff];


		if (err > allowedErr)
			return errThreshold+1;
		len -= 21;
	}

	if (len)
	{
		tmpref = (*ref << refALS) | (*(1+ref) >> refARS);
		tmpseq = (*seq << segALS) | (*(1+seq) >> segARS);
		ref++; 
		seq++;
		diff = (tmpref ^ tmpseq) & 0x7fffffffffffffff;

		diff >>= (typeSize - len*3);
		tmpseq  >>= (typeSize - len*3);

		*errSamp |= (tmpseq & _msf_NMASK);


		err += _msf_errCnt[diff & 0xffffff] + _msf_errCnt[(diff>>24)&0xffffff] + _msf_errCnt[(diff>>48)&0xfffff];


		if (err > allowedErr)
			return errThreshold+1;
	}

	*errSamp |= err;
	return err;
    }
}
