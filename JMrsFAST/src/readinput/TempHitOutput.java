/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package readinput;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import jmrsfast.Constants;

/**
 *
 * @author desktop
 */
public class TempHitOutput {
    protected Path tempFile1;
    protected Path tempFile2;
    private final int fileIndex;
    
    public TempHitOutput(int fileIndex){
        this.fileIndex = fileIndex;
    }
    
    public void createTemp(Path path){
        try {
            this.tempFile1 = Files.createTempFile(path.toString(), "." + fileIndex + ".1.tmp");
            this.tempFile1.toFile().deleteOnExit();
            this.tempFile2 = Files.createTempFile(path.toString(), "." + fileIndex + ".2.tmp");
            this.tempFile2.toFile().deleteOnExit();
        } catch (IOException ex) {
            Logger.getLogger(TempHitOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void OutputTempHits(CompressedHit[] hits) throws IOException{
        RandomAccessFile out = null;
        RandomAccessFile out1 = new RandomAccessFile(tempFile1.toFile(), "w");
        RandomAccessFile out2 = new RandomAccessFile(tempFile2.toFile(), "w");
        
        for(int i = 0; i < hits.length; i++){
            if(i % 2 == 0){
                out = out1;
            }else{
                out = out2;
            }
            out.writeByte(hits[i].size);
            
            if(hits[i].size > 0){
                for(int j = 0; j < hits[i].size; j++){
                    out.writeByte(hits[i].hits.get(j).loc[j % Constants.MAPCHUNKS]);
                    out.writeByte(hits[i].hits.get(j).err[j % Constants.MAPCHUNKS]);
                }
            }
        }
        for(CompressedHit c : hits){
            c.destroy();
        }
        
        out1.close();
        out2.close();
    }
}
