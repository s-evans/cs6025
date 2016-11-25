// H1B.java CS6025 Yizong Cheng January 2015
// Huffman decoder
// Usage:  java H1B < encoded > original

import java.io.*;
import java.util.*;

public class H1B {

    int[][] codetree = null;
    int buf = 0;
    int position = 0;
    int actualNumberOfSymbols = 0;
    int compressed_size = 0;
    int uncompressed_size = 0;

    void readTree() { // read Huffman tree
        try {
            actualNumberOfSymbols = System.in.read();
            codetree = new int[actualNumberOfSymbols * 2 - 1][2];

            for ( int i = 0; i < actualNumberOfSymbols * 2 - 1; i++ ) {
                codetree[i][0] = System.in.read();
                codetree[i][1] = System.in.read();
            }
        } catch ( IOException e ) {
            System.err.println( e );
            System.exit( 1 );
        }
    }

    int inputBit() { // get one bit from System.in
        // If there are no bits left in the cache
        if ( position == 0 ) {
            try {
                // Read a byte
                buf = System.in.read();
                compressed_size++;

                // Check return value
                if ( buf < 0 ) {
                    return -1;
                }

                // Set up counter
                position = 0x80;
            } catch ( IOException e ) {
                System.err.println( e );
                return -1;
            }
        }

        // Get the next leading bit and populate an int with 0 or 1
        int t = ( ( buf & position ) == 0 ) ? 0 : 1;

        // Shift out the bit
        position >>= 1;

        // Return bit populated int
        return t;
    }

    void decode() {
        int bit = -1;
        int k = 0;

        while ( ( bit = inputBit() ) >= 0 ) {
            k = codetree[k][bit];
            if ( codetree[k][0] == 0 ) {
                System.out.write( codetree[k][1] );
                uncompressed_size++;
                k = 0;
            }
        }

        System.out.flush();
    }

    public static void main( String[] args ) {
        H1B h1 = new H1B();
        h1.readTree();
        h1.decode();
    }
}

