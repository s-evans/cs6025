// H4C.java CS6025 Yizong Cheng January 2015
// Decode C1 coded files
// Usage: java H4C < encoded > original

import java.io.*;
import java.util.*;

class H4C
{
    static int fibSize = 12;  // fib[11] = 233
    static int[] fib = new int[] {
        1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233
    };
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

    int deFib()
    {
        int e = 0;
        boolean justHadOne = false;

        for ( int i = 0; i <= fibSize; i++ ) {
            int bit = inputBit();

            if ( bit < 0 ) {
                return -1;
            }

            if ( bit == 1 )
                if ( justHadOne ) {
                    break;
                } else {
                    justHadOne = true;
                    e += fib[i];
                }
            else {
                justHadOne = false;
            }
        }

        return e - 1;
    }

    void decode()
    {
        int c = 0;

        while ( ( c = deFib() ) >= 0 ) {
            System.out.write( c );
        }
    }

    public static void main( String[] args )
    {
        H4C h4 = new H4C();
        h4.decode();
    }
}

