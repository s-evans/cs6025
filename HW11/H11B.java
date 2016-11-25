// H11B.java CS6025 Yizong Cheng February 2015
// Implementing AES decryption
// Usage: java H11B key < encrypted > original

import java.io.*;
import java.util.*;

public class H11B
{

    static final int numberOfBits = 8;
    static final int fieldSize = 1 << numberOfBits;
    static final int irreducible = 0x11b;
    static final int logBase = 3;
    static final byte[][] A = new byte[][] {
        {1, 1, 1, 1, 1, 0, 0, 0},
        {0, 1, 1, 1, 1, 1, 0, 0},
        {0, 0, 1, 1, 1, 1, 1, 0},
        {0, 0, 0, 1, 1, 1, 1, 1},
        {1, 0, 0, 0, 1, 1, 1, 1},
        {1, 1, 0, 0, 0, 1, 1, 1},
        {1, 1, 1, 0, 0, 0, 1, 1},
        {1, 1, 1, 1, 0, 0, 0, 1}
    };
    static final byte[] B = new byte[] { 0, 1, 1, 0, 0, 0, 1, 1};
    static final byte[][] Gi = new byte[][] {
        {14, 9, 13, 11},
        {11, 14, 9, 13},
        {13, 11, 14, 9},
        {9, 13, 11, 14}
    };
    int[] alog = new int[fieldSize];
    int[] log = new int[fieldSize];
    int[] S = new int[fieldSize];
    int[] Si = new int[fieldSize];
    static final int blockSize = 16;
    static final int numberOfRounds = 11;
    int[] state = new int[blockSize];
    int[][] roundKey = new int[numberOfRounds][blockSize];
    String hexkey = null;

    int modMultiply( int a, int b, int m )
    {
        int product = 0;

        for ( ; b > 0; b >>= 1 ) {
            if ( ( b & 1 ) > 0 ) {
                product ^= a;
            }

            a <<= 1;

            if ( ( a & fieldSize ) > 0 ) {
                a ^= m;
            }
        }

        return product;
    }

    void makeLog()
    {
        alog[0] = 1;

        for ( int i = 1; i < fieldSize; i++ ) {
            alog[i] = modMultiply( logBase, alog[i - 1], irreducible );
        }

        for ( int i = 1; i < fieldSize; i++ ) {
            log[alog[i]] = i;
        }
    }

    int logMultiply( int a, int b )
    {
        return ( a == 0 || b == 0 ) ? 0 : alog[( log[a] + log[b] ) % ( fieldSize - 1 )];
    }

    int multiplicativeInverse( int a )
    {
        return alog[fieldSize - 1 - log[a]];
    }

    void buildS()
    {
        int[] bitColumn = new int[8];

        for ( int i = 0; i < fieldSize; i++ ) {
            int inverse = i < 2 ? i : multiplicativeInverse( i );

            for ( int k = 0; k < 8; k++ ) {
                bitColumn[k] = inverse >> ( 7 - k ) & 1;
            }

            S[i] = 0;

            for ( int k = 0; k < 8; k++ ) {
                int bit = B[k];

                for ( int l = 0; l < 8; l++ ) {
                    if ( bitColumn[l] == 1 ) {
                        bit ^= A[k][l];
                    }
                }

                S[i] ^= bit << 7 - k;
            }

            Si[S[i]] = i;
        }
    }

    int readBlock()
    {
        byte[] data = new byte[blockSize];
        int len = 0;

        try {
            len = System.in.read( data );
        } catch ( IOException e ) {
            System.err.println( e.getMessage() );
            System.exit( 1 );
        }

        if ( len <= 0 ) {
            return len;
        }

        for ( int i = 0; i < len; i++ ) {
            if ( data[i] < 0 ) {
                state[i] = data[i] + fieldSize;
            } else {
                state[i] = data[i];
            }
        }

        for ( int i = len; i < blockSize; i++ ) {
            state[i] = 0;
        }

        return len;
    }

    void inverseSubBytes()
    {
        for ( int i = 0; i < blockSize; i++ ) {
            state[i] = Si[state[i]];
        }
    }

    void swap( int[] values, int x, int y ) 
    {
        int temp = values[x];
        values[x] = values[y];
        values[y] = temp;
    }

    void inverseShiftRows()
    {
        int temp = state[7];
        state[7] = state[11];
        state[11] = state[15];
        state[15] = state[3];
        state[3] = temp;

        temp = state[13];
        state[13] = state[9];
        state[9] = state[5];
        state[5] = state[1];
        state[1] = temp;

        swap(state, 6, 14);

        swap(state, 2, 10);
    }

    void inverseMixColumns()
    {
        int[] temp = new int[4];

        for ( int k = 0; k < 4; k++ ) {
            for ( int i = 0; i < 4; i++ ) {
                temp[i] = 0;

                for ( int j = 0; j < 4; j++ ) {
                    temp[i] ^= logMultiply( Gi[j][i], state[k * 4 + j] );
                }
            }

            for ( int i = 0; i < 4; i++ ) {
                state[k * 4 + i] = temp[i];
            }
        }
    }

    void readKey( String filename )
    {
        Scanner in = null;

        try {
            in = new Scanner( new File( filename ) );
        } catch ( FileNotFoundException e ) {
            System.err.println( filename + " not found" );
            System.exit( 1 );
        }

        hexkey = in.nextLine();
        in.close();
    }

    void expandKey()
    {
        for ( int i = 0; i < blockSize; i++ ) {
            roundKey[0][i] =
                Integer.parseInt( hexkey.substring( i * 2, ( i + 1 ) * 2 ), 16 );
        }

        int rcon = 1;

        for ( int i = 1; i < numberOfRounds; i++ ) {
            roundKey[i][0] = S[roundKey[i - 1][13]] ^ rcon;
            rcon <<= 1;

            if ( rcon > 0xFF ) {
                rcon ^= irreducible;
            }

            roundKey[i][1] = S[roundKey[i - 1][14]];
            roundKey[i][2] = S[roundKey[i - 1][15]];
            roundKey[i][3] = S[roundKey[i - 1][12]];

            for ( int k = 0; k < 4; k++ ) {
                roundKey[i][k] ^= roundKey[i - 1][k];
            }

            for ( int k = 4; k < blockSize; k++ ) {
                roundKey[i][k] = roundKey[i][k - 4] ^ roundKey[i - 1][k];
            }
        }
    }

    void inverseAddRoundKey( int round )
    {
        for ( int k = 0; k < blockSize; k++ ) {
            state[k] ^= roundKey[numberOfRounds - round - 1][k];
        }
    }

    void blockDecipher()
    {
        inverseAddRoundKey( 0 );

        for ( int i = 1; i < numberOfRounds; i++ ) {
            inverseSubBytes();
            inverseShiftRows();
            inverseAddRoundKey( i );

            if ( i < numberOfRounds - 1 ) {
                inverseMixColumns();
            }
        }
    }

    void writeBlock()
    {
        byte[] data = new byte[blockSize];

        for ( int i = 0; i < blockSize; i++ ) {
            data[i] = ( byte )( state[i] );
        }

        try {
            System.out.write( data );
        } catch ( IOException e ) {
            System.err.println( e.getMessage() );
            System.exit( 1 );
        }
    }

    void decrypt()
    {
        while ( readBlock() > 0 ) {
            blockDecipher();
            writeBlock();
        }

        System.out.flush();
    }

    public static void main( String[] args )
    {
        if ( args.length < 1 ) {
            System.err.println( "Usage: java H11B key < encrypted > original" );
            return;
        }

        H11B h11 = new H11B();
        h11.makeLog();
        h11.buildS();
        h11.readKey( args[0] );
        h11.expandKey();
        h11.decrypt();
    }
}

