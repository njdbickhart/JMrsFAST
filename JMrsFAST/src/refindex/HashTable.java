/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package refindex;

import java.util.ArrayList;
import java.util.List;
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
    private int refGenNamelen;
    private String refGenName;
    private int refGenOffset = 0;
    private int refGenLength = 0;
    private int compRefGenLen = 0;
    private List<GeneralIndex> crefGen;
    private int moreMaps = 0;
    
    public HashTable(byte extraInfo){
        this.extraInfo = extraInfo;
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
        
        //int compSeqstart = blockCounter;
        int compSeqend = blockCounter + compRefGenLen;
        
        int HashTbleSize = byteArrayToInt(getByteSlice(block, compSeqend + 1, 4));
        
        // Decoding loop
        int index, i = 0, diff = 0, tmpSize = 0;
        long hashVal = 0l;
         
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
            }
        }
    }
}
