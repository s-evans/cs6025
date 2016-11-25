// H22.java CS6025 Yizong Cheng April 2015
// compute and compare the error-correcting codewords in QR version 1 symbols
// Usage: java H22 < test21.txt

import java.io.*;
import java.util.*;

public class H22
{

    static final int maxSize = 200;
    static final int formatLength = 15;
    static final int irreducible = 0x11d;
    static final int fieldSize = 256;
    static final int oneLessFieldSize = fieldSize - 1;
    static final int[] generator = new int[] {
        43, 139, 206, 78, 43, 239, 123, 206, 214, 147, 24, 99, 150,
        39, 243, 163, 136
    };
    static final int capacity = 26;
    static final int dataCapacity = 9;
    static final int correctionCapacity = 17;
    int[] G = new int[correctionCapacity + 1];
    int[] codewords = new int[capacity];
    String[] rawBitmap = new String[maxSize];
    int numberOfLines = 0;
    int version = 0;
    int width = 0;
    int height = 0;
    boolean[][] matrix = null;
    boolean[] format = new boolean[formatLength];
    boolean[] dataBitStream = null;
    int dataSpace = 0;
    int[] alog = new int[fieldSize];
    int[] log2 = new int[fieldSize];

    void readRawBitmap()
    {
        Scanner in = new Scanner( System.in );

        while ( in.hasNextLine() ) {
            rawBitmap[numberOfLines++] = in.nextLine();
        }
    }

    void getMatrix()
    {
        int firstRow = 0;

        for ( ; firstRow < numberOfLines; firstRow++ ) {
            if ( rawBitmap[firstRow].indexOf( "XXXXXXX " ) >= 0 ) {
                break;
            }
        }

        int leftPos = rawBitmap[firstRow].indexOf( "XXXXXXX " );
        int rightPos = rawBitmap[firstRow].lastIndexOf( " XXXXXXX" );
        width = rightPos + 8 - leftPos;
        height = width;
        version = ( width - 17 ) / 4;
        matrix = new boolean[height][width];

        for ( int i = 0; i < height; i++ ) {
            for ( int j = 0; j < width; j++ ) {
                matrix[i][j] = rawBitmap[firstRow + i].charAt( leftPos + j ) == 'X';
            }
        }

        dataSpace = width * height - 3 * 64 - 2 * 15 - 2 * ( width - 16 ) - 1;
        dataBitStream = new boolean[dataSpace];
    }

    void getFormatInformation()
    {
        for ( int i = 0; i < 6; i++ ) {
            format[i] = matrix[8][i];
        }

        format[6] = matrix[8][7];
        format[7] = matrix[8][8];
        format[8] = matrix[7][8];

        for ( int i = 0; i < 6; i++ ) {
            format[formatLength - 1 - i] = matrix[i][8];
        }

        for ( int i = 0; i < formatLength; i++ ) {
            if ( format[i] ) {
                System.out.print( "1" );
            } else {
                System.out.print( "0" );
            }
        }

        System.out.println();

    }

    void demask()
    {
        for ( int i = 0; i < 9; i++ ) {
            if ( i != 6 )
                for ( int j = 9; j < width - 8; j++ ) {
                    if ( i % 2 == 0 ) {
                        matrix[i][j] = !matrix[i][j];
                    }
                }
        }

        for ( int i = 9; i < height - 8; i++ ) {
            for ( int j = 0; j < width; j++ ) {
                if ( j != 6 ) {
                    if ( i % 2 == 0 ) {
                        matrix[i][j] = !matrix[i][j];
                    }
                }
            }
        }

        for ( int i = height - 8; i < height; i++ ) {
            for ( int j = 9; j < width; j++ ) {
                if ( i % 2 == 0 ) {
                    matrix[i][j] = !matrix[i][j];
                }
            }
        }
    }

    void getDataBitStream()
    {
        int n = 0;

        for ( int i = 0; i < 4; i++ ) {
            boolean up = ( i % 2 ) == 0;
            int x = width - 1 - 2 * i;

            if ( up ) {
                for ( int y = height - 1; y >= 9; y-- ) {
                    dataBitStream[n++] = matrix[y][x];
                    dataBitStream[n++] = matrix[y][x - 1];
                }
            } else {
                for ( int y = 9; y < height; y++ ) {
                    dataBitStream[n++] = matrix[y][x];
                    dataBitStream[n++] = matrix[y][x - 1];
                }
            }
        }

        int middleWidth = ( width - 17 ) / 2;

        for ( int i = 0; i < middleWidth; i++ ) {
            boolean up = ( i % 2 ) == 0;
            int x = width - 9 - 2 * i;

            if ( up ) {
                for ( int y = height - 1; y >= 0; y-- ) {
                    if ( y != 6 ) {
                        dataBitStream[n++] = matrix[y][x];
                        dataBitStream[n++] = matrix[y][x - 1];
                    }
                }
            } else {
                for ( int y = 0; y < height; y++ ) {
                    if ( y != 6 ) {
                        dataBitStream[n++] = matrix[y][x];
                        dataBitStream[n++] = matrix[y][x - 1];
                    }
                }
            }
        }

        for ( int i = 0; i < 4; i++ ) {
            boolean up = ( i % 2 ) == 0;
            int x = i == 0 ? 8 : 7 - 2 * i;

            if ( up ) {
                for ( int y = height - 9; y >= 9; y-- ) {
                    dataBitStream[n++] = matrix[y][x];
                    dataBitStream[n++] = matrix[y][x - 1];
                }
            } else {
                for ( int y = 9; y <= height - 9; y++ ) {
                    dataBitStream[n++] = matrix[y][x];
                    dataBitStream[n++] = matrix[y][x - 1];
                }
            }
        }
    }

    int nextSymbol( int position, int bitSize )
    {
        int result = 0;

        for ( int i = 0; i < bitSize; i++ ) {
            result <<= 1;

            if ( dataBitStream[position + i] ) {
                result |= 1;
            }
        }

        return result;
    }

    void makeLog2()
    {
        alog[0] = 1;

        for ( int i = 1; i < fieldSize; i++ ) {
            alog[i] = ( alog[i - 1] << 1 );

            if ( ( alog[i] & 0x100 ) != 0 ) {
                alog[i] ^= irreducible;
            }
        }

        for ( int i = 1; i < fieldSize; i++ ) {
            log2[alog[i]] = i;
        }
    }

    int inverse( int a )
    {
        return alog[oneLessFieldSize - log2[a]];
    }

    int mul( int a, int b )
    {
        if ( a == 0 || b == 0 ) {
            return 0;
        }

        return alog[( log2[a] + log2[b] ) % oneLessFieldSize];
    }

    void makeG()
    {
        G[0] = 1;

        for ( int i = 0; i < correctionCapacity; i++ ) {
            G[i + 1] = alog[generator[i]];
        }
    }

    void getRemainder()
    {
        System.out.println( "getRemainder" );

        for ( int i = 0; i < dataCapacity; i++ ) {
            codewords[i] = nextSymbol( i * 8, 8 );
        }

        for ( int i = dataCapacity; i < capacity; i++ ) {
            codewords[i] = 0;
        }

        for ( int i = 0; i < dataCapacity; i++ ) {
            if ( codewords[i] != 0 ) {
                for ( int j = 0; j < correctionCapacity; j++ ) {
                    codewords[i + j + 1] ^= mul( G[j + 1] , codewords[i] );
                }
            }
        }

        for ( int i = 0; i < correctionCapacity; i++ ) {
            System.out.println( codewords[dataCapacity + i] + " " + nextSymbol( ( dataCapacity + i ) * 8, 8 ) );
        }
    }

    int evaluatePolynomial( int[] coefficients, int x )
    {
        // coefficients are those for a polynomial starting with the one
        // for the term x to the highest power which is coefficients.length - 1
        int len = coefficients.length;
        int sum = coefficients[0];

        for ( int i = 1; i < len; i++ ) {
            sum = mul( sum, x ) ^ coefficients[i];
        }

        return sum;
    }

    void checkG()
    {
        System.out.println( "checkG" );
        for ( int i = 0; i < correctionCapacity; i++ ) {
            System.out.println( evaluatePolynomial( G, alog[i] ) );
        }
    }

    void readCodewords()
    {
        for ( int i = 0; i < capacity; i++ ) {
            codewords[i] = nextSymbol( i * 8, 8 );
        }
    }

    void computeSyndromes()
    {
        System.out.println( "computeSyndromes" );
        for ( int i = 0; i < correctionCapacity; i++ ) {
            System.out.println( evaluatePolynomial( codewords, alog[i] ) );
        }
    }

    public static void main( String[] args )
    {
        H22 h22 = new H22();
        h22.makeLog2();
        h22.makeG();
        h22.checkG();
        h22.readRawBitmap();
        h22.getMatrix();
        h22.demask();
        h22.getDataBitStream();
        h22.getRemainder();
        h22.readCodewords();
        h22.computeSyndromes();
    }

}

