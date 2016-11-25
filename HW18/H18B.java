// H18B.java CS6025 Yizong Cheng March 2015
// LZW decoding
// java H18B < encoded > original

import java.io.*;
import java.util.*;

public class H18B
{

    static final int dictionaryCapacity = 4096;
    int[] prefix = new int[dictionaryCapacity];
    int[] lastSymbol = new int[dictionaryCapacity];
    int dictionarySize = 0;
    int firstSymbol = 0;

    void initializeDictionary()
    {
        for ( int i = 0; i < 256; i++ ) {
            prefix[i] = -1;
            lastSymbol[i] = i;
        }

        dictionarySize = 256;
    }

    void outputPhrase( int index ) // recursive
    {
        if ( index >= 0 && index < dictionarySize ) {
            outputPhrase( prefix[index] );
            System.out.write( lastSymbol[index] );

            if ( index < 256 ) {
                firstSymbol = lastSymbol[index];
            }
        }
    }

    void decode()
    {
        Scanner in = new Scanner( System.in );

        while ( in.hasNextLine() ) {
            int codeword = Integer.parseInt( in.nextLine() );
            outputPhrase( codeword );

            if ( dictionarySize < dictionaryCapacity ) {
                lastSymbol[dictionarySize - 1] = firstSymbol;
                prefix[dictionarySize++] = codeword;
            }
        }
    }

    public static void main( String[] args )
    {
        H18B h18 = new H18B();
        h18.initializeDictionary();
        h18.decode();
    }

}

