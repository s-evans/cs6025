// H23.java CS6025 Yizong Cheng April 2015
// with contribution by John Wnek April 2013
// QR version 1 error correction level H mask 001 only
// Usage: java H23 < test21.txt

import java.io.*;
import java.util.*;

public class H23
{

    static final int maxSize = 200; // max line length from input
    static final int maxErrors = 8;  // max number of errors to correct
    // static final int maxErrors = 8;  // max number of errors to correct
    static final int irreducible = 0x11d; // for GF(2^8)
    static final int fieldSize = 256; // GF(2^8) size
    static final int oneLessFieldSize = fieldSize - 1;  // modulus for addition of log2
    static final int[] generator = new int[] { // log2 of coeffs of generator polynomial
        43, 139, 206, 78, 43, 239, 123, 206, 214, 147, 24, 99, 150,
        39, 243, 163, 136
    };
    static final int capacity = 26; // total number of codewords/bytes in data region
    static final int correctionCapacity = 17;  // number of codewords or error correction
    int[] codewords = new int[capacity];
    String[] rawBitmap = new String[maxSize];  // a line of the raw symbol
    int numberOfLines = 0;
    int version = 0;
    int width = 0;
    int height = 0;
    boolean[][] matrix = null;  // for the QR matrix
    boolean[] dataBitStream = null; // data bits in QR forms a stream
    int dataSpace = 0;  // length of data bit stream
    int[] alog = new int[fieldSize];  // powers of 2 in GF(2^8)
    int[] log2 = new int[fieldSize];  // log2 of non-zero elements in GF(2^8)
    int[] errorPositions = new int[maxErrors];  // randomly generated error positions p(j)
    int[] errorMagnitudes = new int[maxErrors];  // randomly generated non-zero errors e(j)
    int[] syndromes = new int[correctionCapacity];  // evaluation of codwords on 2^i
    int[] locators = null; // error-location polynomial

    void readRawBitmap()   // read lines from qr1.txt
    {
        Scanner in = new Scanner( System.in );

        while ( in.hasNextLine() ) {
            rawBitmap[numberOfLines++] = in.nextLine();
        }
    }

    void getMatrix()   // fill the boolean matrix for the symbol
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
        dataBitStream = new boolean[dataSpace]; // number of bits in data region
    }

    void demask()   // demask based on mask 001
    {
        for ( int i = 0; i < 9; i++ ) {
            if ( i != 6 ) {
                for ( int j = 9; j < width - 8; j++ ) {
                    if ( i % 2 == 0 ) {
                        matrix[i][j] = !matrix[i][j];
                    }
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

    void getDataBitStream()  // get Version 1 data bits into dataBitStream
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

    int nextSymbol( int position, int bitSize ) // starting position an length of the substream of bits
    {
        // return the integer represented by a segment of dataBitStream
        int result = 0; // will add bits to result

        for ( int i = 0; i < bitSize; i++ ) {
            result <<= 1;

            if ( dataBitStream[position + i] ) {
                result |= 1;
            }
        }

        return result;
    }

    void makeLog2()  // alog is powers of 2 in GF(2^8) and log2 is discret log
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

    void addErrors()  // randomly generate 8 error locations and error values
    {
        Random random = new Random();

        for ( int i = 0; i < capacity; i++ ) {
            codewords[i] = nextSymbol( i * 8, 8 );    // data from true symbol
        }

        displayPolynomial( "Original Message", codewords ); // display the 26 codewords

        for ( int i = 0; i < maxErrors; i++ ) {
            errorPositions[i] = random.nextInt( capacity );
            boolean unique = false;

            while ( !unique ) {
                int j = 0;

                for ( ; j < i; j++ ) {
                    if ( errorPositions[i] == errorPositions[j] ) {
                        break;
                    }
                }

                if ( j == i ) {
                    unique = true;
                } else {
                    errorPositions[i] = random.nextInt( capacity );
                }
            }

            errorMagnitudes[i] = random.nextInt( oneLessFieldSize ) + 1;
            codewords[errorPositions[i]] ^= errorMagnitudes[i];  // adding errors
        }

        displayPolynomial( "Random Error Positions", errorPositions );
        displayPolynomial( "Random Error Magnitudes", errorMagnitudes );
        displayPolynomial( "Message with Errors", codewords );
    }

    int inverse( int a ) // multiplicative inverse of (non-zero) a in GF(2^8)
    {
        return alog[oneLessFieldSize - log2[a]];
    }

    int mul( int a, int b ) // multiplication in GF(2^8)
    {
        if ( a == 0 || b == 0 ) {
            return 0;
        }

        return alog[( log2[a] + log2[b] ) % oneLessFieldSize];
    }

    int evaluatePolynomial( int[] p, int x ) // what is p(x)
    {
        int len = p.length;
        int sum = p[0];

        for ( int i = 1; i < len; i++ ) {
            sum = mul( sum, x ) ^ p[i];
        }

        return sum;
    }

    void computeSyndromes()  // syndromes are codewords(2^i)
    {
        for ( int i = 0; i < correctionCapacity; i++ ) {
            syndromes[i] = evaluatePolynomial( codewords, alog[i] );
        }

        displayPolynomial( "Syndromes", syndromes );
    }

    void displayPolynomial( String title, int[] p ) // display array with title
    {
        System.out.print( title + "  " );

        for ( int i = 0; i < p.length - 1; i++ ) {
            System.out.print( p[i] + " " );
        }

        System.out.println( p[p.length - 1] + " " );
    }

    int[] shiftPolynomial( int[] p ) // xp(x)
    {
        int[] shifted = new int[p.length + 1];

        for ( int i = 0; i < p.length; i++ ) {
            shifted[i] = p[i];
        }

        shifted[p.length] = 0;
        return shifted;
    }

    int[] scalePolynomial( int[] p, int a ) // ap(x)
    {
        int[] scaled = new int[p.length];

        for ( int i = 0; i < p.length; i++ ) {
            scaled[i] = mul( p[i], a );
        }

        return scaled;
    }

    int[] addPolynomials( int[] p, int[] q ) // p + q
    {
        int[] tmp = new int[Math.max( p.length, q.length )];

        for ( int i = 0; i < p.length; i++ ) {
            tmp[i + tmp.length - p.length] = p[i];
        }

        for ( int i = 0; i < q.length; i++ ) {
            tmp[i + tmp.length - q.length] ^= q[i];
        }

        return tmp;
    }

    void solveLocatorsBerlekampMassey()
    {
        System.out.println( "\nBerlekamp-Massey Decoder" );
        int[] ep = new int[1];
        ep[0] = 1;  // approximation for error locators poly
        int[] op = new int[1];
        op[0] = 1;
        int[] np = null;

        for ( int i = 0; i < syndromes.length; i++ ) { // Iterate over syndromes
            op = shiftPolynomial( op );
            int delta = syndromes[i]; // discrepancy from

            for ( int j = 1; j < ep.length; j++ ) { // recurrence for syndromes
                delta ^= mul( ep[ep.length - 1 - j], syndromes[i - j] );
            }

            if ( delta != 0 ) { // has discrepancy
                if ( op.length > ep.length ) {
                    np = scalePolynomial( op, delta );
                    op = scalePolynomial( ep, inverse( delta ) );
                    ep = np;
                }

                ep = addPolynomials( ep, scalePolynomial( op, delta ) );
                displayPolynomial( "Iteration " + i, ep ); // display Berlekamp-Massey steps
            }
        }

        locators = ep;
        displayPolynomial( "Error-Location Polynomial", locators );
    }

    void solveErrorPositions()   // find roots of error-location polynomial then error positions
    {
        errorPositions = new int[maxErrors];

        for ( int i = 0, j = 0; i < capacity; i++ ) {

            // Find B sub j inverses which are roots of error locator polynomial
            // B sub i = a to the ith power
            // B sub j = a to the jth power where sigma( B sub j ) = 0

            if ( evaluatePolynomial( locators, inverse( alog[ i ] ) ) == 0 ) {
                errorPositions[j++] = capacity - i - 1;
            }
        }

        displayPolynomial( "Decoded Error Positions", errorPositions );

    }

    public static void main( String[] args )
    {
        H23 h23 = new H23();
        h23.makeLog2();
        h23.readRawBitmap();
        h23.getMatrix();
        h23.demask();
        h23.getDataBitStream();
        h23.addErrors();
        h23.computeSyndromes();
        h23.solveLocatorsBerlekampMassey();
        h23.solveErrorPositions();
    }

}

