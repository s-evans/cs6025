// H5B.java CS6025 Yizong Cheng January 2015
// Progressive image decoder
// Usage: java H5B < progressive.bmp > original.bmp

import java.io.*;
import java.util.*;

public class H5B
{
    static int numberOfValues = 256;
    int width, height;  // image dimensions
    short[][][] raw;      // the raw image stored here
    byte[] data = null;
    int index = 0;

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

    void readData()
    {
        data = new byte[height * width * 3];

        try {
            System.in.read( data );
        } catch ( IOException e ) {
            System.err.println( e.getMessage() );
            System.exit( 1 );
        }
    }

    void progressivelyDecode()
    {
        raw = new short[height][width][3];

        for ( int k = 0; k < 3; k++ ) {
            raw[0][0][k] = nextDatum();
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
                        raw[rpos][cpos + halfGap][k] =
                            unmapError( nextDatum(), raw[rpos][cpos][k] );

                        raw[rpos + halfGap][cpos][k] =
                            unmapError( nextDatum(), raw[rpos][cpos][k] );

                        raw[rpos + halfGap][cpos + halfGap][k] =
                            unmapError( nextDatum(), raw[rpos][cpos][k] );
                    }

                    cpos += gap;
                }

                rpos += gap;
            }

            gap = halfGap;
            size <<= 1;
        }
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

    short nextDatum()   // next symbol, maybe from the next codeword
    {
        int x = data[index++];

        if ( x < 0 ) {
            x += 256;
        }

        return ( short )x;
    }

    void outputRestored()   // complete the restored bmp file
    {
        for ( int i = 0; i < height; i++ ) {
            for ( int j = 0; j < width; j++ ) {
                for ( int k = 0; k < 3; k++ ) {
                    System.out.write( raw[i][j][k] );
                }
            }
        }

        System.out.flush();
    }

    public static void main( String[] args )
    {
        H5B h5 = new H5B();
        h5.readHeader();
        h5.readData();
        h5.progressivelyDecode();
        h5.outputRestored();
    }

}

