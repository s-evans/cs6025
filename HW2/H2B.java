// H2B.java CS6025 Yizong Cheng January 2015

import java.io.*;
import java.lang.*;

class H2B
{
    static int BLOCKSIZE = 8192;
    static int numberOfSymbols = 256;
    int length = 0;  // length of block
    int[] A = new int[numberOfSymbols];
    int[] L = new int[BLOCKSIZE];  // the Burrows-Wheeler transformation
    int[] F = new int[BLOCKSIZE];
    int[] T = new int[BLOCKSIZE];
    int I;  // the position of text after suffix sort

    void initializeA()
    {
        for ( int i = 0; i < numberOfSymbols; i++ ) {
            A[i] = i;
        }
    }

    void readBlock()
    {
        byte[] buffer = new byte[BLOCKSIZE + 2];

        try {
            length = System.in.read( buffer ) - 2;
        } catch ( IOException e ) {
            System.err.println( e.getMessage() );
            System.exit( 1 );
        }

        if ( length <= 0 ) {
            return;
        }

        int i1 = buffer[0];

        // Fix byte signedness
        if ( i1 < 0 ) {
            i1 += 256;
        }

        int i0 = buffer[1];

        // Fix byte signedness
        if ( i0 < 0 ) {
            i0 += 256;
        }

        // Unpack first two bytes to get I
        I = i1 * 256 + i0;

        for ( int i = 0; i < length; i++ ) {
            int j = buffer[i + 2];

            // Fix bytes signedness
            if ( j < 0 ) {
                j += 256;
            }

            // Populate L
            int t = A[j];
            L[i] = t;

            // Move all symbols back in the array
            for ( int k = j; k > 0; k-- ) {
                A[k] = A[k - 1];
            }

            A[0] = t; // move to front
        }
    }

    void deBW()
    {
        // Generate F from L by lexicographically sorting it
        for ( int i = 0; i < length; i++ ) {
            int j = i - 1;

            for ( ; j >= 0; j-- ) { 
                if ( L[i] < F[j] ) {
                    F[j + 1] = F[j];
                } else {
                    break;
                }
            }

            F[j + 1] = L[i];
        }

        int j = 0;

        // Generate T
        for ( int i = 0; i < length; i++ ) {
            // On symbol transition in F, reset j to zero
            if ( i > 0 && F[i] > F[i - 1] ) {
                j = 0;
            }

            // Find runs of matching characters in L
            for ( ; j < length; j++ ) {
                if ( L[j] == F[i] ) {
                    break;
                }
            }

            // T of i is a run count / index of next character
            T[i] = j++;
        }

        // Now we have I, L, F, and T
        // Your code here for printing the decoded block using System.out.write().

        // Handle the first byte
        int idx = I;
        System.out.write( F[idx] );

        // Iterate over the remaining bytes
        for ( int i = 1 ; i < length ; ++i ) {
            // Get the new index and output the data
            idx = T[idx];
            System.out.write( F[idx] );
        }

    }

    void decode()
    {
        initializeA();

        while ( true ) {
            readBlock();

            if ( length <= 0 ) {
                return;
            }

            deBW();

            if ( length < BLOCKSIZE ) {
                return;
            }
        }
    }

    public static void main( String[] args )
    {
        H2B h2 = new H2B();
        h2.decode();
    }
}

