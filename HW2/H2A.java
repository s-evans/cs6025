// H2A.java CS6025 Yizong Cheng January 2015
// performs Burrows-Wheeler transform to reduce entropy
// Usage: java H2A < original > transformed

import java.io.*;
import java.util.*;

class H2A
{
    static int BLOCKSIZE = 8192;
    static int numberOfSymbols = 256;
    int[] s = new int[BLOCKSIZE * 2];  // text block repeated twice for sorting
    int length = 0;  // length of block
    int[] v = new int[BLOCKSIZE]; // vector for suffix sorting
    int[] L = new int[BLOCKSIZE];  // the Burrows-Wheeler transformation
    int I;  // the position of text after suffix sort
    int[] A = new int[numberOfSymbols];

    class Suffix implements Comparable
    {
        int position;

        public Suffix( int p )
        {
            position = p;
        }

        public int compareTo( Object obj )
        {
            Suffix o = ( Suffix ) obj;
            int k = 0;

            // Check bounds and compare characters at separate positions
            while ( k < length && s[position + k] == s[o.position + k] ) {
                k++;
            }

            if ( k == length ) {
                // Data blocks are equivalent
                return position - o.position;
            } else {
                // Return comparison result
                return s[position + k] - s[o.position + k];
            }
        }
    }

    void initializeA()
    {
        for ( int i = 0; i < numberOfSymbols; i++ ) {
            A[i] = i;
        }
    }

    void readBlock()
    {
        byte[] buffer = new byte[BLOCKSIZE];

        // Read a block from stdin
        try {
            length = System.in.read( buffer );
        } catch ( IOException e ) {
            System.err.println( e.getMessage() );
            System.exit( 1 );
        }

        // Verify return value
        if ( length <= 0 ) {
            return;
        }

        for ( int i = 0; i < length; i++ ) {
            int c = buffer[i];

            // Correct signedness of bytes
            if ( c < 0 ) {
                c += 256;
            }

            // Populate array and concatenate
            s[i] = s[length + i] = c;
        }
    }

    void suffixSort()
    {
        TreeSet<Suffix> tset = new TreeSet<Suffix>();

        // Sort the permutations of the data
        for ( int i = 0; i < length; i++ ) {
            tset.add( new Suffix( i ) );
        }

        int j = 0;

        // Populate V with indices of sorted permutations
        for ( Suffix o : tset ) {
            v[j++] = o.position;
        }
    }

    void wheeler()
    {
        L = new int[length];

        // Generate vector L, the last column of the sorted permutation matrix of the block
        // Find I, the index of the first character in the block in vector L
        for ( int i = 0; i < length; i++ ) {
            if ( v[i] == 0 ) {
                // v[i] == 0 means that we've found the first character of the block
                I = i; // position of text
                L[i] = s[length - 1];   // The last character
            } else {
                L[i] = s[v[i] - 1];
            }
        }

        // Output the value I using two bytes
        System.out.write( I / 256 );
        System.out.write( I % 256 );
    }

    void moveToFront()
    {
        int i, j, k;

        for ( i = 0; i < length; i++ ) {
            int t = L[i];

            // Find the value in the array
            for ( j = 0; t != A[j]; j++ );

            System.out.write( j );      // j is the position of L[i]

            // Copy all values in the array forward
            for ( k = j; k > 0; k-- ) {
                A[k] = A[k - 1];    // move L[i] to front
            }

            // Move to front
            A[0] = t;
        }
    }

    void encode()
    {
        initializeA();

        while ( true ) {
            readBlock();

            if ( length <= 0 ) {
                break;
            }

            suffixSort();
            wheeler();
            moveToFront();

            if ( length < BLOCKSIZE ) {
                break;
            }
        }

        System.out.flush();
    }

    public static void main( String[] args )
    {
        H2A h2 = new H2A();
        h2.encode();
    }
}

