/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package refindex;

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
    public HashTable(){
        
    }
}
