// H6.java CS6025 Yizong Cheng January 2015
// Decoding Golomb code LG(2,32)
// Usage: java H3A LG232.txt < original > encoded
// Usage: java H6 < encoded > decoded

import java.io.*;
import java.util.*;

public class H6
{

    static final int GolombK = 2;
    static final int GolombLimit = 23;
    static final int[] powersOf2 = new int[] {
        1, 2, 4, 8, 16, 32, 64, 128
    }; // used by deGolomb
    int buf = 0;
    int position = 0;

    int inputBit()   // 0, 1, or -1 for end of file
    {
        if ( position == 0 ) {
            try {
                buf = System.in.read();

                if ( buf < 0 ) {
                    return -1;
                }

                position = 0x80;
            } catch ( IOException e ) {
                System.err.println( e );
                return -1;
            }
        }

        int t = ( ( buf & position ) == 0 ) ? 0 : 1;
        position >>= 1;
        return t;
    }

    int deGolomb()  // get the next codeword, return the symbol it encodes
    {
        int q = 0;
        int bit = 0;

        while ( ( bit = inputBit() ) == 0 ) {
            ++q;
        }

        if ( bit == -1 ) {
            return -1;
        }

        int L = 2; // L = log2(b) = log2(2^k) = log2(2^2) = log2(4) = 2

        if ( q >= GolombLimit ) {
            L = 8; // L = log2(range) = log2(256) = 8
            q = 0;
        }

        int r = 0;

        for ( int i = L - 1; i >= 0 ; --i ) {
            if ( ( bit = inputBit() ) < 0 ) {
                return -1;
            }

            if ( bit == 1 ) {
                r += powersOf2[i];
            }
        }

        return (q << GolombK) + r;
    }

    void decode()
    {
        int symbol = -1;

        while ( ( symbol = deGolomb() ) >= 0 ) {
            System.out.write( symbol );
        }

        System.out.flush();
    }

    public static void main( String[] args )
    {
        H6 h6 = new H6();
        h6.decode();
    }

}

