/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package datatypes;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author bickhart
 */
public class seqMD {
    public boolean isUseable = true;
    public List<MDTag> tags = new ArrayList<>(1);
    
    public void loadMDTag(int count, char symbol){
        tags.add(new MDTag(count, symbol));
    }
    
    public String getFullMDStr(){
        String complete = tags.stream()
                .map((t) -> t.getString())
                .reduce("", String::concat);
        return complete;
    }
    
    public class MDTag{
        public int count;
        public char symbol;
        
        public MDTag(int count, char symbol){
            this.count = count;
            this.symbol = symbol;
        }
        
        public String getString(){
            return String.valueOf(count) + String.valueOf(symbol);
        }
    }
}
