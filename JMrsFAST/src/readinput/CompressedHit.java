/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package readinput;
import java.util.ArrayList;
import java.util.List;
import jmrsfast.Constants;

/**
 *
 * @author desktop
 */
public class CompressedHit {
    public int size;
    public List<Locations> hits = new ArrayList<>();
    
    public void destroy(){
        this.size = 0;
        hits.stream()
                .forEach((s) -> {s.clear();});
        hits.clear();
    }
    
    public class Locations{
        public int[] loc = new int[Constants.MAPCHUNKS];
        public char[] err = new char[Constants.MAPCHUNKS];
        
        public void clear(){
            this.loc = null;
            this.err = null;
        }
    }
}
