/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package refindex;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import readinput.CompressedSeq;

/**
 *
 * @author desktop
 */
public class RefGenomeHasher {
    private final int windowSize;
    private String fastaFile;
    private RandomAccessFile index;
    
    public RefGenomeHasher(String fastaFile, int windowSize){
        this.windowSize = windowSize;
        this.fastaFile = fastaFile;
    }
    
    public void GenerateRefGenomeHash(){
        MetaData metadata = getGenomeMetaData();
        
        InitializeIndex(metadata);
        
    }
    
    // 1 byte (extraInfo): Reserved; in case the contig has extra information (flag variable)
    // 2 bytes (len): Length of the reference genome name
    // n bytes (refGenName): Reference genome name
    // 4 bytes (refGenOffset): Offset of the contig from the beginning of the chromosome
    // 4 bytes (refGenLength): Length of reference genome
    // 4 bytes (crefGenLen) : bytes of compressed ref genome stored in this block
    // n bytes (crefGen): compressed reference genome
    // 4 bytes (size): number of hashValues in hashTable with more than 0 locations
    // n bytes (bufferSize and buffer): array of bufferSize/buffer which includes encoded values of hashValue, count of locations
    private void saveRefGenomeHash(char[] refgen, int numHash, String chrName, int refGenOffset, int refGenLen, int flag){
        try {
            this.index.write(flag);
            
            this.index.write(HashUtils.intToTwoByteArray(chrName.length()));
            this.index.write(chrName.getBytes());
            
            this.index.write(HashUtils.intToByteArray(refGenOffset));
            this.index.write(HashUtils.intToByteArray(refGenLen));
            
            CompressedSeq temp = new CompressedSeq(refgen);
            this.index.write(temp.CompSeq.size() * 8);
            this.index.write(temp.getByteList());
            
            this.index.write(HashUtils.intToByteArray(numHash));
            
            //TODO: incorporate HashTable into the hash saver
        } catch (IOException ex) {
            Logger.getLogger(RefGenomeHasher.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    private MetaData getGenomeMetaData(){
        MetaData metadata = new MetaData();
        int genomeSize = 0;
        try(BufferedReader input = Files.newBufferedReader(Paths.get(this.fastaFile), Charset.defaultCharset())){
            String line = input.readLine();
            if(line.startsWith(">")){
                metadata.chrCount += 1;
                metadata.chrNames.add(line.trim().replaceAll(">", ""));
            }else{
                throw new IOException("Improperly formatted fasta file!");
            }
            
            while((line = input.readLine()) != null){
                if(line.startsWith(">")){
                    metadata.chrCount += 1;
                    metadata.chrNames.add(line.trim().replaceAll(">", ""));
                    metadata.chrSizes.add(genomeSize);
                    genomeSize = 0;
                }else{
                    genomeSize += line.trim().length();
                }
            }
            metadata.chrSizes.add(genomeSize);
        }catch(IOException ex){
            ex.printStackTrace();
        }
        
        return metadata;
    }
    
    // file header:
    // 1 byte (magicNumber): Magic number of HashTable (0: <v3, 1: bisulfite <v1.26.4, 2: >v3, 3: initial jmrsfast)
    // 1 byte (WINDOW_SIZE): Windows Size of indexing
    // 4 bytes (_ih_hsahTableMemSize): HashTbleMemSize: maximum number of elements that can be saved.
    // 4 bytes (_ih_IOBufferSize): memory required for reading hash table. In case the value is changed for loading. [ 1<<24]
    // 4 bytes (CONTIG_MAX_SIZE): maximum number of characters that can be in a contig. In case the value is changed for loading
    // n bytes (genomeMetaInfo): number of chromosomes, their names and lengths
    private void InitializeIndex(MetaData metadata){
        String indexFile = this.fastaFile + ".idx";
        try {
            index = new RandomAccessFile(indexFile, "w");
            index.writeByte(3);
            index.writeByte(this.windowSize);
            // TODO: finalize initialization method
            
            // Initial value for maximum hash table size
            byte[] temp = HashUtils.intToByteArray(0);
            index.write(temp);
            
            temp = HashUtils.intToByteArray(1 << 24);
            index.write(temp);
            
            temp = HashUtils.intToByteArray(jmrsfast.Constants.MAX_CONTIG_SIZE);
            index.write(temp);
            
            byte[] meta = metadata.getByteEncoding();
            index.write(meta);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(RefGenomeHasher.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(RefGenomeHasher.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
    }
    
    
    
    // genomeMetaInfo structure:
    // 4 bytes (numOfChrs): number of chromosomes in file
    // for each chromosome we have the following
    // 4 bytes (nameLen): length of the chromosome name
    // n bytes (name): chromosome name
    // 4 bytes (genSize): length of the chromosome in characters
    private class MetaData{
        public int chrCount = 0;
        public List<Integer> chrSizes = new ArrayList<>();
        public List<String> chrNames = new ArrayList<>();
        
        public byte[] getByteEncoding(){
            List<Byte> code = new ArrayList<>();
            byte[] temp = HashUtils.intToByteArray(chrCount);
            code.addAll(addBytes(temp));
            
            for(int j = 0; j < chrSizes.size(); j++){
                int name = chrNames.get(j).length();
                temp = HashUtils.intToByteArray(name);
                code.addAll(addBytes(temp));
                
                code.addAll(addBytes(chrNames.get(j).getBytes()));
                
                temp = HashUtils.intToByteArray(chrSizes.get(j));
                code.addAll(addBytes(temp));
            }
            
            byte[] block = new byte[code.size()];
            for(int i = 0; i < code.size(); i++){
                block[i] = (byte) code.get(i);
            }
            return block;
        }
        
        private List<Byte> addBytes(byte[] temp){
            List<Byte> code = new ArrayList<>();
            for(byte b : temp){
                code.add(b);
            }
            return code;
        }
    }
}
