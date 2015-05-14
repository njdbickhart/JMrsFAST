/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jmrsfast;

/**
 *
 * @author desktop
 */
public class Constants {
    public static final int MAPCHUNKS = 15;
    public static enum MODE {PAIREDEND, SINGLEEND}; 
    public static final long NMASK = 0x4924924924924924l;
    public static final int CONTIG_OVERLAP = 1050;
    public static final int MAX_CONTIG_SIZE = 150_000_000;
    public static final int OFF_CONTIG_SIZE = 80_000_000;
    public static final char[] alphabet = new char[]{'A', 'C', 'G', 'T', 'N'};
}
