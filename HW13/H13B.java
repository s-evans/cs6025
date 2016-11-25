// H13B.java CS6025 Yizong Cheng March 2015
// Implementing AES key unwrapping (RFC3394 alternative)
// Usage: java H13B kek keywrap > key

import java.io.*;
import java.util.*;

public class H13B
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
    static final int keyDataBlockSize = 8;
    static final int numberOfRounds = 11;
    int[] state = new int[blockSize];
    int[][] roundKey = new int[numberOfRounds][blockSize];
    String hexkey = null;
    int numberOfKeyDataBlocks = 0;
    int[][] R = null;
    int[] AA = new int[keyDataBlockSize];  // This is the A in RFC3394

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

    void readKek( String filename )
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

    void readKeyWrap( String filename )
    {
        Scanner in = null;

        try {
            in = new Scanner( new File( filename ) );
        } catch ( FileNotFoundException e ) {
            System.err.println( filename + " not found" );
            System.exit( 1 );
        }

        String line = in.nextLine();
        in.close();
        numberOfKeyDataBlocks = line.length() / 2 / keyDataBlockSize - 1;
        R = new int[numberOfKeyDataBlocks][keyDataBlockSize];

        for ( int i = 0; i < numberOfKeyDataBlocks; i++ ) {
            int offset = ( i + 1 ) * keyDataBlockSize * 2;

            for ( int j = 0; j < keyDataBlockSize; j++ ) {
                R[i][j] = Integer.parseInt(
                              line.substring( offset + j * 2, offset + ( j + 1 ) * 2 ), 16 );
            }
        }

        for ( int j = 0; j < keyDataBlockSize; j++ ) {
            AA[j] = Integer.parseInt( line.substring( j * 2, ( j + 1 ) * 2 ), 16 );
        }
    }

    void unwrap()
    {
        for ( int j = 5; j >= 0; j-- ) {
            for ( int i = numberOfKeyDataBlocks - 1; i >= 0; i-- ) {

                AA[keyDataBlockSize - 1] ^= j * numberOfKeyDataBlocks + i + 1;

                for ( int k = 0; k < keyDataBlockSize; k++ ) {
                    state[keyDataBlockSize + k] = AA[k];
                    state[k] = R[i][k];
                }

                blockDecipher();

                for ( int k = 0; k < keyDataBlockSize; k++ ) {
                    AA[k] = state[keyDataBlockSize + k];
                    R[i][k] = state[k];
                }
            }
        }
    }

    void outputResult()
    {
        for ( int k = 0; k < keyDataBlockSize; k++ ) {
            if ( AA[k] < 16 ) {
                System.out.print( "0" + Integer.toHexString( AA[k] ) );
            } else {
                System.out.print( Integer.toHexString( AA[k] ) );
            }
        }

        for ( int i = 0; i < numberOfKeyDataBlocks; i++ ) {
            for ( int k = 0; k < keyDataBlockSize; k++ ) {
                if ( R[i][k] < 16 ) {
                    System.out.print( "0" + Integer.toHexString( R[i][k] ) );
                } else {
                    System.out.print( Integer.toHexString( R[i][k] ) );
                }
            }
        }

    }


    public static void main( String[] args )
    {
        if ( args.length < 2 ) {
            System.err.println( "Usage: java H13B kek keywrap" );
            return;
        }

        H13B h13 = new H13B();
        h13.makeLog();
        h13.buildS();
        h13.readKek( args[0] );
        h13.expandKey();
        h13.readKeyWrap( args[1] );
        h13.unwrap();
        h13.outputResult();
    }

}

