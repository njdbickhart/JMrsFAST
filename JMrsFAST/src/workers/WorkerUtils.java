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
    public static int[] countErrors(List<Long> refCompressedSeq, int refOff, CompressedSeq read, int seqOff, int len, int errorSamp, int allowedError){
        int refALS = refOff * 3;
	int segALS = seqOff * 3;


	int refARS = typeSize - refALS;
	int segARS = typeSize - segALS;	

	CompressedSeq tmpref, tmpseq, diff;
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
