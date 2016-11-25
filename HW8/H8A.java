// H8A.java CS6025 Yizong Cheng February 2015
// wavelet transform of a BMP image
// Usage: java H8A < lenaRGB.bmp > lenawave.bmp

import java.io.*;

public class H8A
{
    int width, height;
    int[][][] raw;
    int[][][] coeffs;

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
        raw = new int[height][width][3];
        coeffs = new int[height][width][3];

        try {
            System.in.read( image );
        } catch ( IOException e ) {
            System.err.println( e.getMessage() );
            System.exit( 1 );
        }

        int index = 0;

        for ( int i = 0; i < height; i++ ) {
            for ( int j = 0; j < width; j++ ) {
                for ( int k = 0; k < 3; k++ ) {
                    raw[i][j][k] = image[index++];

                    if ( raw[i][j][k] < 0 ) {
                        raw[i][j][k] += 256;
                    }
                }
            }
        }
    }

    void rowTransform()
    {
        int halfWidth = width / 2;

        for ( int k = 0; k < 3; k++ )
            for ( int i = 0; i < height; i++ ) {

                for ( int j = 0; j < halfWidth - 1; j++ ) {
                    coeffs[i][2 * j + 1][k] = raw[i][2 * j + 1][k] -
                                              ( raw[i][2 * j][k] + raw[i][2 * j + 2][k] ) / 2;
                }

                coeffs[i][width - 1][k] = raw[i][width - 1][k] - raw[i][width - 2][k];

                for ( int j = 1; j < halfWidth; j++ ) {
                    coeffs[i][2 * j][k] = raw[i][2 * j][k] +
                                          ( coeffs[i][2 * j - 1][k] + coeffs[i][2 * j + 1][k] + 2 ) / 4;
                }

                coeffs[i][0][k] = raw[i][0][k] + ( coeffs[i][1][k] + 1 ) / 2;
            }
    }

    void columnTransform()
    {
        int halfHeight = height / 2;

        for ( int k = 0; k < 3; k++ ) {
            for ( int j = 0; j < width; j++ ) {

                for ( int i = 0; i < halfHeight - 1; i++ ) {
                    raw[2 * i + 1][j][k] = coeffs[2 * i + 1][j][k] -
                                           ( coeffs[2 * i][j][k] + coeffs[2 * i + 2][j][k] ) / 2;
                }

                raw[height - 1][j][k] = coeffs[height - 1][j][k] - coeffs[height - 2][j][k];

                for ( int i = 1; i < halfHeight; i++ ) {
                    raw[2 * i][j][k] = coeffs[2 * i][j][k] +
                                       ( raw[2 * i - 1][j][k] + raw[2 * i + 1][j][k] + 2 ) / 4;
                }

                raw[0][j][k] = coeffs[0][j][k] + ( raw[1][j][k] + 1 ) / 2;
            }
        }
    }

    void shrink()
    {
        int threshold = 12;

        for ( int k = 0; k < 3; k++ ) {
            for ( int i = 0; i < height; i++ ) {
                for ( int j = 0; j < width; j++ ) {
                    if ( i % 2 > 0 || j % 2 > 0 ) {
                        if ( raw[i][j][k] > 0 && raw[i][j][k] <= threshold ) {
                            raw[i][j][k] = 0;
                        }

                        if ( raw[i][j][k] < 0 && raw[i][j][k] >= -threshold ) {
                            raw[i][j][k] = 0;
                        }

                        if ( raw[i][j][k] != 0 ) {
                            raw[i][j][k] = raw[i][j][k] / 8 + 128;
                        }
                    }

                    if ( raw[i][j][k] < 0 ) {
                        raw[i][j][k] = 0;
                    }

                    if ( raw[i][j][k] > 255 ) {
                        raw[i][j][k] = 255;
                    }
                }
            }
        }
    }

    void dumpRaw()
    {
        int numberOfZeros = 0;

        for ( int i = 0; i < height; i++ ) {
            for ( int j = 0; j < width; j++ ) {
                for ( int k = 0; k < 3; k++ ) {
                    if ( raw[i][j][k] == 0 ) {
                        numberOfZeros++;
                    }

                    System.out.write( raw[i][j][k] );
                }
            }
        }

        System.err.println( ( numberOfZeros * 100 / height / width / 3 ) + "% zeros" );
        System.out.flush();
    }

    public static void main( String[] args )
    {
        H8A h8 = new H8A();
        h8.readHeader();
        h8.readImage();
        h8.rowTransform();
        h8.columnTransform();
        h8.shrink();
        h8.dumpRaw();
    }

}

