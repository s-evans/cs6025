// H7C.java CS6025 Yizong Cheng February 2015
// PBM file viewer with 1/4 shrinkage
// Usage: java H7C < hand.pbm

import java.io.*;
import java.lang.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.JFrame;

public class H7C extends JFrame
{

    static final int BFSZ = 8192;
    static final int[] grayLevel = new int[] { 255, 191, 127, 63, 0 };
    byte[] buffer = new byte[BFSZ];
    int readLen = 0;
    int index = 0;
    int width = 0;
    int height = 0;
    boolean[][] twoRows = null;

    public H7C()
    {
        readPBMHeader();
        setSize( width, height + 30 );
        setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        setLocationRelativeTo( null );
        setTitle( "PBM Viewer" );
        setVisible( true );
    }

    void readPBMHeader()
    {
        try {
            readLen = System.in.read( buffer );
        } catch ( IOException e ) {
            System.err.println( e.getMessage() );
            System.exit( 1 );
        }

        if ( buffer[0] != 'P' || buffer[1] != '4' ) {
            System.err.println( " not P4" );
            System.exit( 1 );
        }

        int pos = 0;

        while ( buffer[pos++] != '\n' );

        int pos2 = pos + 1;

        while ( buffer[pos2++] != '\n' );

        String secondLine = new String( buffer, pos, pos2 - pos - 1 );
        String[] terms = secondLine.split( " " );
        width = Integer.parseInt( terms[0] ) / 2;
        height = Integer.parseInt( terms[1] ) / 2;
        index = pos2;
        twoRows = new boolean[2][width * 2];
    }

    void fillTowRows()
    {
        for ( int row = 0; row < 2; row++ ) {
            for ( int i = 0; i < width / 4; i++ ) {
                int b = getNextByte();

                for ( int j = 0, mask = 0x80; j < 8; j++, mask >>= 1 ) {
                    twoRows[row][i * 8 + j] = ( b & mask ) > 0;
                }
            }
        }
    }

    int getNextByte()
    {
        if ( index >= readLen ) {
            try {
                readLen = System.in.read( buffer );
            } catch ( IOException e ) {
                System.err.println( e.getMessage() );
                System.exit( 1 );
            }

            if ( readLen < 0 ) {
                return -1;
            }

            index = 0;
        }

        int b = buffer[index++];
        return b < 0 ? b + 256 : b;
    }

    void fillData()
    {
        int[] pix = new int[height * width];
        Graphics g = getGraphics();
        int n = 0;

        for ( int i = 0; i < height; i++ ) {
            fillTowRows();

            for ( int j = 0; j < width; j++ ) {
                int sum = 0;

                for ( int p = 0; p < 2; p++ ) {
                    for ( int q = 0; q < 2; q++ ) {
                        if ( twoRows[p][2 * j + q] ) {
                            sum++;
                        }
                    }
                }

                int gray = grayLevel[sum];
                pix[n++] = ( 255 << 24 ) | ( gray << 16 ) | ( gray << 8 ) | gray;
            }
        }

        Image im = createImage( new MemoryImageSource( width,
                                height, pix, 0, width ) );
        g.drawImage( im, 0, 30, null );
    }

    public static void main( String[] args )
    {
        H7C h7 = new H7C();
        h7.fillData();
    }

}

