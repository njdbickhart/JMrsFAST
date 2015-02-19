/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package refindex;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import static refindex.HashUtils.byteArrayToInt;

/**
 * This will contain the binary header from the reference index that MrsFAST generates
 * It will also create sub-chromosome HashTables on demand
 * @author desktop
 */
public class HashTableFactory {
    // From the MrsFast source code
    // file header:
    // 1 byte (magicNumber): Magic number of HashTable (0: <v3, 1: bisulfite <v1.26.4, 2: >v3)
    // 1 byte (WINDOW_SIZE): Windows Size of indexing
    // 4 bytes (_ih_hsahTableMemSize): HashTbleMemSize: maximum number of elements that can be saved.
    // 4 bytes (_ih_IOBufferSize): memory required for reading hash table. In case the value is changed for loading.
    // 4 bytes (CONTIG_MAX_SIZE): maximum number of characters that can be in a contig. In case the value is changed for loading
    // n bytes (genomeMetaInfo): number of chromosomes, their names and lengths
    private RandomAccessFile reader;
    private byte magicNumber;
    private byte windowSize;
    private int HashTbleMemSize;
    private int HashTbleIOBuffer;
    private int ContigMaxSize;
    private List<String> ChrOrder;
    private Map<String, Integer> ChrInfo;
    //private List<String> ChrNames;
    //private List<Integer> ChrSizes;
    private long TableBeginningPos;
    
    public HashTableFactory(File indexFile){
        try {
            reader = new RandomAccessFile(indexFile, "r");
            this.magicNumber = reader.readByte();
            this.windowSize = reader.readByte();
            
            // Now to deal with more complex compressed bytes
            byte[] temp = new byte[4];
            reader.read(temp);
            this.HashTbleMemSize = byteArrayToInt(temp);
            
            reader.read(temp);
            this.HashTbleIOBuffer = byteArrayToInt(temp);
            
            reader.read(temp);
            int chrcount = byteArrayToInt(temp);
            this.ChrInfo = new HashMap<>(chrcount);
            this.ChrOrder = new ArrayList<>(chrcount);
            //this.ChrNames = new ArrayList<>(chrcount);
            //this.ChrSizes = new ArrayList<>(chrcount);
            
            for(int i = 0; i < chrcount; i++){
                reader.read(temp);
                int chrnamesize = byteArrayToInt(temp);
                
                byte[] name = new byte[chrnamesize];
                reader.read(name);
                //this.ChrNames.add(new String(name, "UTF-8"));
                this.ChrOrder.add(new String(name, "UTF-8"));
                
                reader.read(temp);
                //this.ChrSizes.add(byteArrayToInt(temp));
                this.ChrInfo.put(new String(name, "UTF-8"), byteArrayToInt(temp));
            }
            
            this.TableBeginningPos = reader.getFilePointer();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(HashTableFactory.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(HashTableFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private HashTable LoadChrTable(String chr){
        HashTable hash = null;
        try{
            if(!this.ChrInfo.containsKey(chr))
                throw new Exception("[HASHTBLFACT] Could not load index for chromosome: " + chr + "!");
            byte[] block = new byte[this.ChrInfo.get(chr)];
            
            reader.read(block);
            hash  = new HashTable(block[0]);
            
            hash.generateHashFromBlock(block, this.HashTbleMemSize, this.HashTbleIOBuffer);
        }catch(Exception ex){
            Logger.getLogger(HashTableFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
        return hash;
    }
    
    public void RewindTable(){
        try {
            this.reader.seek(TableBeginningPos);
        } catch (IOException ex) {
            Logger.getLogger(HashTableFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void CloseIdxFile(){
         try {
             this.reader.close();
         }catch (IOException ex){
             Logger.getLogger(HashTableFactory.class.getName()).log(Level.SEVERE, null, ex);
         }
    }
}
