/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Native.implementation;

/**
 *
 * @author bickhart
 */
public class MrsFastIndex {
    private long IHashTable = 0L;
    
    public MrsFastIndex(String index){
        
    }
    
    @Override
    protected void finalize(){
        
    }
    
    public native void close();
    
    private native long initialize(String index);
    
    public native void rewindHash();
    
    public native long nextHash();
    
}
