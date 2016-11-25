// H25C.java CS6025 Yizong Cheng April 2015
// YUV video viewer for CIF 352x288
// Usage: java H25C yuvFile [pause in ms between frames]

import java.lang.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.JFrame;

public class H25C extends JFrame
{
    static final int width = 352;
    static final int height = 288;
    static final int halfWidth = width / 2;
    static final int halfHeight = height / 2;
    InputStream in = null;
    int pause = 50;
    int[] pix = new int[height * width];
    int frameSize = height * ( width + halfWidth );
    byte[] raw = new byte[frameSize];
    int[][][] yuv = new int[height][width][3];

    public H25C( String filename )
    {
        try {
            in = new FileInputStream( filename );
        } catch ( FileNotFoundException e ) {
            System.err.println( filename + " not found" );
            System.exit( 1 );
        }

        setSize( width, height + 30 );
        setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        setLocationRelativeTo( null );
        setTitle( filename );
        setVisible( true );
    }

    int readFrame()
    {
        int len = 0;

        try {
            len = in.read( raw );
        } catch ( IOException e ) {
            System.err.println( "IOException" );
            System.exit( 1 );
        }

        return len;
    }

    void raw2yuv()
    {
        int index = 0;

        for ( int i = 0; i < height; i++ ) {
            for ( int j = 0; j < width; j++ ) {
                yuv[i][j][0] = ( raw[index] < 0 ) ? ( raw[index] + 256 ) * 298 : raw[index] * 298;
                index++;
            }
        }

        for ( int i = 0; i < halfHeight; i++ ) {
            for ( int j = 0; j < halfWidth; j++ ) {
                int i2 = i << 1;
                int j2 = j << 1;
                yuv[i2][j2][1] =
                    yuv[i2][j2 + 1][1] =
                        yuv[i2 + 1][j2][1] =
                            yuv[i2 + 1][j2 + 1][1] =
                                ( raw[index] < 0 ) ? raw[index] + 128 : raw[index] - 128;
                index++;
            }
        }

        for ( int i = 0; i < halfHeight; i++ ) {
            for ( int j = 0; j < halfWidth; j++ ) {
                int i2 = i << 1;
                int j2 = j << 1;
                yuv[i2][j2][2] =
                    yuv[i2][j2 + 1][2] =
                        yuv[i2 + 1][j2][2] =
                            yuv[i2 + 1][j2 + 1][2] =
                                ( raw[index] < 0 ) ? raw[index] + 128 : raw[index] - 128;
                index++;
            }
        }
    }

    void yuv2rgb()
    {
        int index = 0;

        for ( int i = 0; i < height; i++ ) {
            for ( int j = 0; j < width; j++ ) {
                int red = yuv[i][j][0] + 409 * yuv[i][j][2] + 128;
                red >>= 8;

                if ( red < 0 ) {
                    red = 0;
                } else if ( red > 255 ) {
                    red = 255;
                }

                int green = yuv[i][j][0] - 100 * yuv[i][j][1] - 208 * yuv[i][j][2] + 128;
                green >>= 8;

                if ( green < 0 ) {
                    green = 0;
                } else if ( green > 255 ) {
                    green = 255;
                }

                int blue = yuv[i][j][0] + 516 * yuv[i][j][1] + 128;
                blue >>= 8;

                if ( blue < 0 ) {
                    blue = 0;
                } else if ( blue > 255 ) {
                    blue = 255;
                }

                pix[index++] = ( 255 << 24 ) | ( red << 16 ) | ( green << 8 ) | blue;
            }

        }
    }

    public void playback()
    {
        Graphics g = getGraphics();

        while ( true ) {
            int len = readFrame();

            if ( len < frameSize ) {
                return;
            }

            raw2yuv();
            yuv2rgb();
            Image im = createImage( new MemoryImageSource( width, height, pix, 0, width ) );
            g.drawImage( im, 0, 30, null );

            try {
                Thread.sleep( pause );
            } catch ( InterruptedException e ) {
            }
        }
    }

    public static void main( String[] args )
    {
        if ( args.length < 1 ) {

            System.out.println( "Usage: java H25C yuvFile" );

            System.exit( 0 );
        }

        H25C h25 = new H25C( args[0] );

        if ( args.length > 1 ) {
            h25.pause = Integer.parseInt( args[1] );
        }

        h25.playback();
    }

}

