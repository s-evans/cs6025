// H11C.java CS6025 Yizong Cheng February 2015
// Implementing AES encryption
// Usage: java H11C

import java.io.*;
import java.util.*;

public class H11C
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
    static final byte[][] G = new byte[][] {
        {2, 1, 1, 3},
        {3, 2, 1, 1},
        {1, 3, 2, 1},
        {1, 1, 3, 2}
    };
    int[] alog = new int[fieldSize];
    int[] log = new int[fieldSize];
    int[] S = new int[fieldSize];
    static final int blockSize = 16;
    static final int numberOfRounds = 11;
    int[] state1 = new int[blockSize];
    int[] state2 = new int[blockSize];
    int[][] roundKey = new int[numberOfRounds][blockSize];
    Random random = new Random();

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
        }
    }

    void randomBlock()
    {
        for ( int i = 0; i < blockSize; i++ ) {
            state2[i] = state1[i] = random.nextInt( fieldSize );
        }

        int index = random.nextInt( blockSize );
        state2[index]++;

        if ( state2[index] >= fieldSize ) {
            state2[index] = 0;
        }
    }

    void subBytes( int[] state )
    {
        for ( int i = 0; i < blockSize; i++ ) {
            state[i] = S[state[i]];
        }
    }

    void shiftRows( int[] state )
    {
        int temp = state[2];
        state[2] = state[10];
        state[10] = temp;
        temp = state[6];
        state[6] = state[14];
        state[14] = temp;
        temp = state[1];
        state[1] = state[5];
        state[5] = state[9];
        state[9] = state[13];
        state[13] = temp;
        temp = state[3];
        state[3] = state[15];
        state[15] = state[11];
        state[11] = state[7];
        state[7] = temp;
    }

    void mixColumns( int[] state )
    {
        int[] temp = new int[4];

        for ( int k = 0; k < 4; k++ ) {
            for ( int i = 0; i < 4; i++ ) {
                temp[i] = 0;

                for ( int j = 0; j < 4; j++ ) {
                    temp[i] ^= logMultiply( G[j][i], state[k * 4 + j] );
                }
            }

            for ( int i = 0; i < 4; i++ ) {
                state[k * 4 + i] = temp[i];
            }
        }
    }

    void expandKey()
    {
        for ( int i = 0; i < blockSize; i++ ) {
            roundKey[0][i] = random.nextInt( fieldSize );
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

    void addRoundKey( int[] state, int round )
    {
        for ( int k = 0; k < blockSize; k++ ) {
            state[k] ^= roundKey[round][k];
        }
    }

    void blockCipher()
    {
        addRoundKey( state1, 0 );
        addRoundKey( state2, 0 );
        System.out.print( "0 " );
        numberOfFlips();

        for ( int i = 1; i < numberOfRounds; i++ ) {
            subBytes( state1 );
            subBytes( state2 );
            System.out.print( i + " " );
            numberOfFlips();
            shiftRows( state1 );
            shiftRows( state2 );
            System.out.print( i + " " );
            numberOfFlips();

            if ( i < numberOfRounds - 1 ) {
                mixColumns( state1 );
                mixColumns( state2 );
            }

            System.out.print( i + " " );
            numberOfFlips();
            addRoundKey( state1, i );
            addRoundKey( state2, i );
        }
    }

    void numberOfFlips()
    {
        int total = 0;

        for ( int i = 0; i < blockSize; i++ ) {
            int diff = state1[i] ^ state2[i];
            int nf = 0;

            for ( int mask = 0x80; mask > 0; mask >>= 1 ) {
                if ( ( diff & mask ) > 0 ) {
                    nf++;
                }
			}

            total += nf;
            System.out.print( nf + " " );
        }

        System.out.println( total );
    }

    public static void main( String[] args )
    {
        H11C h11 = new H11C();
        h11.makeLog();
        h11.buildS();
        h11.expandKey();
        h11.randomBlock();
        h11.blockCipher();
    }
}

