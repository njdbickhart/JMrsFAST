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
        MDTag temp = new MDTag();
        temp.addCount(count);
        temp.addSymbol(symbol);
        tags.add(temp);
    }
    
    public void loadMDTag(char symbol){
        MDTag temp = new MDTag();
        temp.addSymbol(symbol);
        tags.add(temp);
    }
    
    public void loadMDTag(int count){
        MDTag temp = new MDTag();
        temp.addCount(count);
        tags.add(temp);
    }
    
    public String getFullMDStr(){
        String complete = tags.stream()
                .map((t) -> t.getString())
                .reduce("", String::concat);
        return complete;
    }
    
    public class MDTag{
        public int count = -1;
        public char symbol = 'Q';
        
        public void addCount(int count){
            this.count = count;
        }
        
        public void addSymbol(char symbol){
            this.symbol = symbol;
        }
        
        public String getString(){
            String value = null;
            if(count != -1)
                value += String.valueOf(count);
            if(symbol != 'Q')
                value += String.valueOf(symbol);
            return value;
        }
    }
}
