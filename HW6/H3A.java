// H3A.java CS6025 Yizong Cheng January 2015
// Encode stdin with codebook which contains a bitstring for each byte
// Usage: java H3A codebook < original > encoded

import java.io.*;
import java.util.*;

class H3A
{
    static int BLOCKSIZE = 8192;
    static int numberOfSymbols = 256;
    byte[] buffer = new byte[BLOCKSIZE];
    int length = 0;  // length of block
    String[] codewords = new String[numberOfSymbols];
    int buf = 0;
    int position = 0;

    void readBlock()
    {
        try {
            length = System.in.read( buffer );
        } catch ( IOException e ) {
            System.err.println( e.getMessage() );
            System.exit( 1 );
        }
    }

    void readCode( String filename )
    {
        Scanner in = null;

        try {
            in = new Scanner( new File( filename ) );
        } catch ( FileNotFoundException e ) {
            System.err.println( filename + " not found" );
            System.exit( 1 );
        }

        for ( int i = 0; i < numberOfSymbols; i++ ) {
            codewords[i] = in.nextLine();
        }

        in.close();
    }

    void outputbits( String bitstring )
    {
        for ( int i = 0; i < bitstring.length(); i++ ) {
            buf <<= 1;

            if ( bitstring.charAt( i ) == '1' ) {
                buf |= 1;
            }

            position++;

            if ( position == 8 ) {
                position = 0;
                System.out.write( buf );
                buf = 0;
            }
        }
    }

    void encode()
    {
        while ( true ) {
            readBlock();

            for ( int i = 0; i < length; i++ ) {
                int c = buffer[i];

                // Correct byte signedness
                if ( c < 0 ) {
                    c += 256;
                }

                outputbits( codewords[c] );
            }

            if ( length < BLOCKSIZE ) {
                break;
            }
        }

        if ( position > 0 ) {
            buf <<= 8 - position;
            System.out.write( buf );
        }

        System.out.flush();
    }

    public static void main( String[] args )
    {

        H3A h3 = new H3A();
        h3.readCode( args[0] );
        h3.encode();
    }
}

