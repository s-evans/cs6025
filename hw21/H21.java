// H21.java CS6025 Yizong Cheng April 2015
// reading a QR version 1 symbol
// Usage: java H21 < qr1.txt

import java.io.*;
import java.util.*;

public class H21
{

    static final int maxSize = 200;
    static final int formatLength = 15;
    static final int capacity = 26;
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

    void readRawBitmap()   // read the symbol as lines
    {
        Scanner in = new Scanner( System.in );

        while ( in.hasNextLine() ) {
            rawBitmap[numberOfLines++] = in.nextLine();
        }
    }

    void getMatrix()   // find rows and columns for the symbol matrix
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

    void demask()   // assume 001
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

    void getMessage()
    {
        int mode = nextSymbol( 0, 4 );
        int messageLength = nextSymbol( 4, 8 );

        for ( int i = 0; i < messageLength * 4 ; i++ ) {
            System.out.println( nextSymbol( 12 + i * 8, 8 ) );
        }

        System.out.println();
    }

    public static void main( String[] args )
    {
        H21 h21 = new H21();
        h21.readRawBitmap();
        h21.getMatrix();
        h21.demask();
        h21.getFormatInformation();
        h21.getDataBitStream();
        h21.getMessage();
    }

}

