// H25B.java CS6025 Yizong Cheng April 2015
// decoding and viewing compressed CIF 352x288 200 frames
// Usage: java H25B compressedVideo [pause in ms between frames]

import java.lang.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.JFrame;
import java.util.zip.*;

public class H25B extends JFrame
{
    static final int width = 352;
    static final int height = 288;
    static final int halfWidth = width / 2;
    static final int halfHeight = height / 2;
    static final int numberOfFrames = 200;
    static final int frameSize = width * height * 3 / 2;
    byte[] buffer = new byte[frameSize * numberOfFrames];
    byte[] result = new byte[numberOfFrames * frameSize];
    int compressedDataLength = 0;
    InputStream in = null;
    int pause = 30;
    int[] pix = new int[height * width];
    byte[] raw = new byte[frameSize];
    int[][][] yuv = new int[height][width][3];

    public H25B( String filename )
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

    void readCompressedData()
    {
        try {
            compressedDataLength = in.read( result );
        } catch ( IOException e ) {
            System.err.println( "IOException" );
            System.exit( 1 );
        }
    }

    void decompress()
    {
        try {
            Inflater decompresser = new Inflater();
            decompresser.setInput( result, 0, compressedDataLength );
            int resultLength = decompresser.inflate( buffer );
            decompresser.end();

            if ( resultLength != numberOfFrames * frameSize ) {
                System.err.println( resultLength );
                System.exit( 1 );
            }
        } catch ( java.util.zip.DataFormatException ex ) {
            System.err.println( ex.getMessage() );
            System.exit( 1 );
        }
    }

    void getFrame( int frame )
    {
        int frameBase = frame * frameSize;

        if ( frame > 0 ) { 
            for ( int j = 0; j < frameSize; j++ ) {
                int u = buffer[frameBase - frameSize + j];
                int diff = buffer[frameBase + j];
                buffer[frameBase + j] = (byte) ( diff + u );
            }
        }

        for ( int i = 0; i < frameSize; i++ ) {
            raw[i] = buffer[frameBase + i];
        }
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

        for ( int i = 0; i < numberOfFrames; i++ ) {
            getFrame( i );
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

            System.out.println( "Usage: java H25B compressedVideo" );

            System.exit( 0 );
        }

        H25B h25 = new H25B( args[0] );

        if ( args.length > 1 ) {
            h25.pause = Integer.parseInt( args[1] );
        }

        h25.readCompressedData();
        h25.decompress();
        h25.playback();
    }

}

