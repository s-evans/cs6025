// H5C.java CS6025 Yizong Cheng January 2015
// Displaying image progressively
// Usage: java H5C < progressive.bmp

import java.lang.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.JFrame;

public class H5C extends JFrame
{
    static int numberOfValues = 256;
    int width, height;  // image dimensions
    short[][][] raw;      // the raw image stored here
    byte[] data = null;
    int index = 0;

    public H5C()
    {
        readHeader();
        setSize( width, height + 30 );
        setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        setLocationRelativeTo( null );
        setTitle( "Progressive" );
        setVisible( true );
    }

    void readHeader()
    {
        byte[] header = new byte[54];

        try {
            System.in.read( header );
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
        int[] pix = new int[height * width];
        Graphics g = getGraphics();
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

                        for ( int p = 0; p < halfGap; p++ ) for ( int q = 0; q < halfGap; q++ ) {
                                raw[rpos + p][cpos + q][k] = raw[rpos][cpos][k];
                                raw[rpos + p][cpos + halfGap + q][k] = raw[rpos][cpos + halfGap][k];
                                raw[rpos + halfGap + p][cpos + q][k] = raw[rpos + halfGap][cpos][k];
                                raw[rpos + halfGap + p][cpos + halfGap + q][k] =
                                    raw[rpos + halfGap][cpos + halfGap][k];
                            }
                    }

                    cpos += gap;
                }

                rpos += gap;
            }

            gap = halfGap;
            size <<= 1;
            int n = 0;

            for ( int i = height - 1; i >= 0; i-- )
                for ( int j = 0; j < width; j++ ) {
                    pix[n++] = ( 255 << 24 ) | ( raw[i][j][2] << 16 ) |
                               ( raw[i][j][1] << 8 ) | raw[i][j][0];
                }

            Image im = createImage( new MemoryImageSource( width,
                                    height, pix, 0, width ) );
            g.drawImage( im, 0, 30, null );

            try {
                Thread.sleep( 1000 );
            } catch ( InterruptedException e ) {}
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

    short nextDatum()
    {
        int x = data[index++];

        if ( x < 0 ) {
            x += 256;
        }

        return ( short )x;
    }

    public static void main( String[] args )
    {
        H5C h5 = new H5C();
        h5.readData();
        h5.progressivelyDecode();
    }
}

