// H3B.java CS6025 Yizong Cheng January 2015
// Decode Gamma coded files
// Usage: java H3B < encoded > original

import java.io.*;
import java.util.*;

class H3B
{
    static final int[] powersOf2 = new int[] {
        1, 2, 4, 8, 16, 32, 64, 128
    };
    int length = 0;  // length of block
    int buf = 0;
    int position = 0;

    int inputBit()
    {
        if ( position == 0 )
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

        int t = ( ( buf & position ) == 0 ) ? 0 : 1;
        position >>= 1;
        return t;
    }

    int deGamma()
    {
        int bit = -1;
        int offsetLength = 0;

        while ( ( bit = inputBit() ) == 1 ) {
            ++offsetLength;
        }

        if ( bit < 0 ) {
            return -1;
        }

        int number = powersOf2[offsetLength];

        while ( offsetLength > 0 ) {
           if ( ( bit = inputBit() ) < 0 ) {
                return -1;
            }

           number += bit * powersOf2[offsetLength - 1];

            --offsetLength;
        }

        return number - 1;
    }

    void decode()
    {
        int c = 0;

        while ( ( c = deGamma() ) >= 0 ) {
            System.out.write( c );
        }
    }

    public static void main( String[] args )
    {
        H3B h3 = new H3B();
        h3.decode();
    }
}

