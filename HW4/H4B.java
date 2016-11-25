// H4B.java CS6026 Yizong Cheng January 2015
// Inverse of H4A.java
// Usage: java H4B < errors.bmp > restored.bmp

import java.io.*;
import java.util.*;

public class H4B
{
    static int borderValue = 128; // a,b,c for x on first row and column
    int width, height;  // image dimensions
    short[][][] raw;      // the raw image stored here


    void readHeader()
    {
        byte[] header = new byte[54];

        try {
            System.in.read( header );
            System.out.write( header );
        } catch ( IOException e ) {
            System.err.println( e.getMessage() );
            System.exit( 1 );
        }

        if ( header[0] != 'B' || header[1] != 'M'
                || header[14] != 40 || header[28] != 24 ) {
            System.exit( 1 );
        }

        int w1 = header[18];
        int w2 = header[19];

        if ( w1 < 0 ) {
            w1 += 256;
        }

        if ( w2 < 0 ) {
            w2 += 256;
        }

        width = w2 * 256 + w1;
        int h1 = header[22];
        int h2 = header[23];

        if ( h1 < 0 ) {
            h1 += 256;
        }

        if ( h2 < 0 ) {
            h2 += 256;
        }

        height = h2 * 256 + h1;
    }

    int predict( int a, int b, int c )
    {
        int x;

        if ( ( c >= a ) && ( c >= b ) ) {
            x = ( a >= b ) ? b : a;
        } else if ( ( c <= a ) && ( c <= b ) ) {
            x = ( a >= b ) ? a : b;
        } else {
            x = a + b - c;
        }

        return x;
    }


    public void deJpegls()
    {
        int a, b, c;

        for ( int i = 0; i < height; i++ ) {//  find the neighboring pixels
            for ( int j = 0; j < width; j++ ) {
                for ( int k = 0; k < 3; k++ ) {
                    if ( j == 0 ) {
                        a = borderValue;
                    } else {
                        a = raw[i][j - 1][k];
                    }

                    if ( i == 0 ) {
                        b = c = borderValue;
                    } else {
                        if ( j == 0 ) {
                            c = borderValue;
                        } else {
                            c = raw[i - 1][j - 1][k];
                        }

                        b = raw[i - 1][j][k];
                    }

                    int prediction = predict( a, b, c );
                    raw[i][j][k] = unmapError( raw[i][j][k], prediction );
                    System.out.write( raw[i][j][k] );
                }
            }
        }

        System.out.flush();
    }

    short unmapError( int error, int predicted )
    {
        int e = ( error % 2 == 0 ) ? error / 2 : -error / 2 - 1;

        int value = predicted + e;

        if ( value > 255 ) {
            value -= 256;
        } else if ( value < 0 ) {
            value += 256;
        }

        return ( short )value;
    }

    void readJpegls()
    {
        byte[] jpegls = new byte[height * width * 3];
        raw = new short[height][width][3];

        try {
            System.in.read( jpegls );
        } catch ( IOException e ) {
            System.err.println( e.getMessage() );
            System.exit( 1 );
        }

        int index = 0;

        for ( int i = 0; i < height; i++ ) {
            for ( int j = 0; j < width; j++ ) {
                for ( int k = 0; k < 3; k++ ) {
                    raw[i][j][k] = ( short )jpegls[index++];

                    if ( raw[i][j][k] < 0 ) {
                        raw[i][j][k] += 256;
                    }
                }
            }
        }
    }


    public static void main( String[] args )
    {
        H4B h4 = new H4B();
        h4.readHeader();
        h4.readJpegls();
        h4.deJpegls();
    }
}

