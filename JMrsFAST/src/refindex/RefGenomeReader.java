/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package refindex;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 *
 * @author desktop
 */
public class RefGenomeReader {
    private String CurrentContigName;
    private String NextContigName;
    private final char[] GenomeArray = new char[jmrsfast.Constants.MAX_CONTIG_SIZE + 21];
    /**
     *
     */
    public int RefGenomeSize = 0;
    public int RefGenomeOffset = 0;

    /**
     *
     */
    public boolean isStarted = false;
    public readerState CurrentState = readerState.NOT_STARTED;
    private BufferedReader input;
    
    /**
     *
     * @param fasta
     */
    public RefGenomeReader(String fasta){
        try{
            this.input = Files.newBufferedReader(Paths.get(fasta), Charset.defaultCharset());
        }catch(IOException ex){
            ex.printStackTrace();
        }
    }
    
    /**
     *
     *  0 = no retrieval
     *  1 = Loaded fasta, didn't reach next contig
     *  2 = Loaded fasta, next contig is loaded
     *  3 = Loaded fasta, end of file
     */
    public void RetrieveGenomeArray(){
        this.RefGenomeSize = 0;
        int actualSize = 0;
        
        if(this.CurrentState == readerState.LOADED_EOF || this.CurrentState == readerState.FINISHED){
            this.CurrentState = readerState.FINISHED;
            return;
        }
        
        try{
            String line;
            if(isStarted && this.RefGenomeOffset == 0){
                // Just picked back up after reading a prior chromosome
                this.CurrentContigName = this.NextContigName;
            }
            
            if(! isStarted){
                line = this.input.readLine();
                this.CurrentContigName = line.trim().replaceAll(">", "");
                this.isStarted = true;
            }else if(this.RefGenomeOffset > 0){
                // This is a chunk from the previous chromosome
                for(int i = 0; i < jmrsfast.Constants.CONTIG_OVERLAP; i++){
                    this.GenomeArray[i] = this.GenomeArray[this.RefGenomeSize - jmrsfast.Constants.CONTIG_OVERLAP + i];
                    if(this.GenomeArray[i] == 'N')
                        actualSize++;
                }
                this.RefGenomeSize = jmrsfast.Constants.CONTIG_OVERLAP;
            }
            
            while((line = this.input.readLine()) != null){
                if(line.startsWith(">")){
                    this.NextContigName = line.trim().replaceAll(">", "");
                    this.RefGenomeOffset = 0;
                    this.CurrentState = readerState.LOADED_FULL_CONTIG;
                    //return 2;
                }
                char[] bases = line.trim().toUpperCase().toCharArray();
                for(int x = 0; x < bases.length; x++){
                    this.GenomeArray[this.RefGenomeSize++] = bases[x];
                    if(bases[x] != 'N')
                        actualSize++;
                    if((actualSize > jmrsfast.Constants.OFF_CONTIG_SIZE || this.RefGenomeSize > jmrsfast.Constants.MAX_CONTIG_SIZE) && this.RefGenomeSize % 21 == 0){
                        // We're splitting up larger chromosomes into chunks for lower memory overhead
                        this.RefGenomeOffset += this.RefGenomeSize;
                        this.CurrentState = readerState.LOADED_SPLIT_CONTIG;
                        //return 1;
                    }
                }
            }
            this.CurrentState = readerState.LOADED_EOF;
            //return 3;
        }catch(IOException ex){
            ex.printStackTrace();
        }
        this.CurrentState = readerState.NO_RETRIEVAL;
        //return 0;
    }
    
    public String GetCurrentRefName(){
        return this.CurrentContigName;
    }
    
    public char[] GetCurrentGenomeArray(){
        return this.GenomeArray;
    }
    
    public enum readerState{
        NOT_STARTED, NO_RETRIEVAL, LOADED_SPLIT_CONTIG, LOADED_FULL_CONTIG, LOADED_EOF, FINISHED
    }
}
