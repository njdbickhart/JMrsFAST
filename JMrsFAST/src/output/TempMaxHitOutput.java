/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package output;

import datatypes.seqMD;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import refindex.HashUtils;

/**
 *
 * @author desktop
 */
public class TempMaxHitOutput {
    private final int FileIndex;
    private Path tempFile;
    private RandomAccessFile out = null;
    
    public TempMaxHitOutput(int FileIndex){
        this.FileIndex = FileIndex;
    }
    
    public void CreateTempFile(Path BasePath){
        try{
            this.tempFile = Files.createTempFile(BasePath.toString(), "." + FileIndex + ".1.tmp");
        }catch(IOException ex){
            ex.printStackTrace();
        }
    }
    
    public void PrepChrTempWrite(String chr) throws IOException{
        if(out == null)
            out = new RandomAccessFile(tempFile.toFile(), "w");
        
        /* Chromosome header byte code convention
            4 bytes: code indicating that this is a chromosome listing (0)
            4 bytes: Length of chromosome name (n)
            n bytes: chromosome name
        */
        int flag = 0;
        int len = chr.length();
        out.write(HashUtils.intToByteArray(flag));
        out.write(HashUtils.intToByteArray(len));
        out.write(chr.getBytes(Charset.forName("UTF-8")));
    }
    
    public synchronized void WriteMaxHit(int readFactIndex, int samFlag, int position, seqMD mderrors) throws IOException{
        /* Hit byte code convention:
            4 bytes: read index for SeqReadFactory.readChunk
            4 bytes: samflag
            4 bytes: chromosome position
            4 bytes: Number of position errors
            4 bytes: Length of MDString (n)
            n bytes: MDString
        */
        out.write(HashUtils.intToByteArray(readFactIndex + 1));
        out.write(HashUtils.intToByteArray(samFlag));
        out.write(HashUtils.intToByteArray(position));
        out.write(HashUtils.intToByteArray(mderrors.mdErr));
        
        String MDTags = mderrors.getFullMDStr();
        out.write(HashUtils.intToByteArray(MDTags.length()));
        out.write(MDTags.getBytes(StandardCharsets.UTF_8));
    }
    
    public void PrepareForRead() throws IOException{
        if(out != null)
            out.close();
        
        out = new RandomAccessFile(tempFile.toFile(), "r");
    }
    
    public AbstractTempHit ReadMaxHit() throws IOException{
        AbstractTempHit line = null;
        byte[] temp = new byte[4];
        int teller = out.read(temp);
        if(teller == -1)
            return line;
        
        int index = HashUtils.byteArrayToInt(temp);
        if(index == 0){
            // This is a chromosome header
            TempMaxChr tline = new TempMaxChr();
            out.read(temp);
            
            int chrNamelen = HashUtils.byteArrayToInt(temp);
            byte[] chr = new byte[chrNamelen];
            out.read(chr);
            String chrName = new String(chr);
            tline.addChr(chrName);
            line = tline;
        }else{
            // this is a read hit line
            int readIndex = HashUtils.byteArrayToInt(temp);
            out.read(temp);
            int samFlag = HashUtils.byteArrayToInt(temp);
            out.read(temp);
            int position = HashUtils.byteArrayToInt(temp);
            out.read(temp);
            int mderrors = HashUtils.byteArrayToInt(temp);
            out.read(temp);
            int mdlen = HashUtils.byteArrayToInt(temp);
            
            byte[] mdstr = new byte[mdlen];
            out.read(mdstr);
            String mdtags = new String(mdstr);
            line = new TempMaxLine(readIndex, samFlag, position, mderrors, mdtags);
        }
        return line;
    }
    
    public class TempMaxChr extends AbstractTempHit{
        public String chrName;
               
        public void addChr(String chrName){
            this.chrName = chrName;
        }
        @Override
        public boolean isChrHeader() {
            return true;
        }
        
    }
    
    public class TempMaxLine extends AbstractTempHit{
        public int readFactIndex;
        public int samFlag;
        public int position;
        public int mdErrors;
        public String mdTags;
        
        public TempMaxLine(int readFactIndex, int samFlag, int position, int mdErrors, String mdTags){
            this.readFactIndex = readFactIndex;
            this.samFlag = samFlag;
            this.position = position;
            this.mdErrors = mdErrors;
            this.mdTags = mdTags;
        }
        
        @Override
        public boolean isChrHeader() {
            return false;
        }
        
    }
}
