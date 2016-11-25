// H5A.java CS6025 Yizong Cheng January 2015
// Progressive image encoder
// Usage: java H5A < image.bmp > progressive.bmp

import java.io.*;
import java.util.*;

public class H5A
{
    static int numberOfValues = 256;
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

    void readImage()
    {
        byte[] image = new byte[height * width * 3];
        raw = new short[height][width][3];

        try {
            System.in.read( image );
        } catch ( IOException e ) {
            System.err.println( e.getMessage() );
            System.exit( 1 );
        }

        int index = 0;

        for ( int i = 0; i < height; i++ )
            for ( int j = 0; j < width; j++ )
                for ( int k = 0; k < 3; k++ ) {
                    raw[i][j][k] = ( short )image[index++];

                    if ( raw[i][j][k] < 0 ) {
                        raw[i][j][k] += 256;
                    }
                }
    }

    void progressive()
    {
        for ( int k = 0; k < 3; k++ ) {
            System.out.write( raw[0][0][k] );
        }

        int size = 1;
        int gap = height;

        while ( gap > 1 ) {
            int halfGap = gap >> 1;
            int rpos = 0;
            int cpos = 0;

            for ( int i = 0; i < size; i++ ) {
                cpos = 0;

                for ( int j = 0; j < size; j++ ) {
                    for ( int k = 0; k < 3; k++ ) {
                        System.out.write(
                            mapError( raw[rpos][cpos + halfGap][k], raw[rpos][cpos][k] ) );

                        System.out.write(
                            mapError( raw[rpos + halfGap][cpos][k], raw[rpos][cpos][k] ) );

                        System.out.write(
                            mapError( raw[rpos + halfGap][cpos + halfGap][k], raw[rpos][cpos][k] ) );
                    }

                    cpos += gap;
                }

                rpos += gap;
            }

            gap = halfGap;
            size <<= 1;
        }

        System.out.flush();
    }

    // map the prediction error to nonnegatives
    int mapError( int value, int prediction )
    {
        int e = value - prediction;         //  prediction error

        if ( e > 127 ) {
            e -= 256;    //  putting error in [-128, 127]
        } else if ( e < -128 ) {
            e += 256;
        }

        e = ( e >= 0 ) ? e * 2 + 1 : -e * 2; //  into 0 -1 1 -2 2 array
        return e - 1;
    }

    public static void main( String[] args )
    {
        H5A h5 = new H5A();
        h5.readHeader();
        h5.readImage();
        h5.progressive();
    }
}

