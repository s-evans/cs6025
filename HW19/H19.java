// H19.java CS6025 Yizong Cheng March 2015
// Converting PNG to BMP
// Usage: java H19 < file.png > file.bmp

import java.io.*;
import java.util.*;
import java.util.zip.*;

public class H19
{
    static final int headerSize = 33;
    static final int BMPHeaderSize = 54;
    static final int chunkHeaderSize = 8;
    static final int dataSize = 2000000;
    static final int crcSize = 4;
    static final int[] signature = new int[] {
        137, 80, 78, 71, 13, 10, 26, 10,
        0, 0, 0, 13, 73, 72, 68, 82
    };
    byte[] headerBuffer = new byte[headerSize];
    byte[] chunkHeaderBuffer = new byte[chunkHeaderSize];
    byte[] dataBuffer = new byte[dataSize];
    byte[] crcBuffer = new byte[crcSize];
    byte[] BMPHeader = new byte[BMPHeaderSize];
    byte[] resultBuffer = null;
    int compressedDataLength = 0;
    int decompressedDataLength = 0;
    int width = 0;
    int height = 0;
    int lineWidth = 0;
    int bitDepth = -1;
    int colorType = -1;
    int compressionMethod = -1;
    int filterMethod = -1;
    int interlace = -1;

    // read header of .png and get all parameters
    void readHeader()
    {
        int len = 0;

        try {
            len = System.in.read( headerBuffer );
        } catch ( IOException e ) {
            System.err.println( e.getMessage() );
            System.exit( 1 );
        }

        if ( len != headerSize ) {
            System.err.println( " no header " );
            System.exit( 1 );
        }

        for ( int i = 0; i < 16; i++ ) {
            int a = headerBuffer[i];

            if ( a < 0 ) {
                a += 256;
            }

            if ( a != signature[i] ) {
                System.err.println( " not PNG " );
                System.exit( 1 );
            }
        }

        for ( int i = 0; i < 4; i++ ) {
            int a = headerBuffer[i + 16];

            if ( a < 0 ) {
                a += 256;
            }

            width <<= 8;
            width += a;
        }

        for ( int i = 0; i < 4; i++ ) {
            int a = headerBuffer[i + 20];

            if ( a < 0 ) {
                a += 256;
            }

            height <<= 8;
            height += a;
        }

        bitDepth = headerBuffer[24];
        colorType = headerBuffer[25];
        compressionMethod = headerBuffer[26];
        filterMethod = headerBuffer[27];
        interlace = headerBuffer[28];

        if ( bitDepth != 8 || colorType != 6 || compressionMethod != 0
                || filterMethod != 0 || interlace != 0 ) {
            System.err.println( "decoder not implemented" );
            System.exit( 1 );
        }

        // 4 bytes for each pixel and 1 for filter type of the scanline
        lineWidth = width * 4 + 1;
    }

    // read the next chunk
    // return false if it is an IEND
    // if it is an IDAT, enter the data in databuffer
    boolean readChunk()
    {
        int len = 0;

        // read chunk header
        try {
            len = System.in.read( chunkHeaderBuffer );
        } catch ( IOException e ) {
            System.err.println( e.getMessage() );
            System.exit( 1 );
        }

        if ( len != chunkHeaderSize ) {
            System.err.println( " no chunk header " );
            System.exit( 1 );
        }

        // get chunk data length
        int chunkDataLength = 0;

        for ( int i = 0; i < 4; i++ ) {
            int a = chunkHeaderBuffer[i];

            if ( a < 0 ) {
                a += 256;
            }

            chunkDataLength <<= 8;
            chunkDataLength += a;
        }

        // get chunk type
        String chunkType = new String( chunkHeaderBuffer, 4, 4 );

        try {
            if ( chunkType.equals( "IEND" ) ) {
                return false;
            }

            if ( chunkType.equals( "IDAT" ) ) {
                // place data in dataBuffer
                len = System.in.read( dataBuffer, compressedDataLength, chunkDataLength );
                compressedDataLength += chunkDataLength;
            } else if ( chunkDataLength > 0 ) {
                byte[] tmpBuffer = new byte[chunkDataLength];
                len = System.in.read( tmpBuffer );
            }

            if ( len != chunkDataLength ) {
                System.err.println( " no chunk data " );
                System.exit( 1 );
            }

            // get CRC for the chunk
            len = System.in.read( crcBuffer );

            if ( len != crcSize ) {
                System.err.println( " no CRC " );
                System.exit( 1 );
            }
        } catch ( IOException e ) {
            System.err.println( e.getMessage() );
            System.exit( 1 );
        }

        return true;
    }

    // go through all chunks
    void readData()
    {
        while ( readChunk() );
    }

    // use Inflater to decompress data in dataBuffer
    // decompressed data in resultBuffer
    void decompress()
    {
        resultBuffer = new byte[width * height * 4 + height];
        Inflater decompresser = new Inflater();
        decompresser.setInput( dataBuffer, 0, compressedDataLength );
        int totalBytes = 0;
        try {
            while ( !decompresser.finished() ) { 
                int returnedBytes;
                returnedBytes = decompresser.inflate( resultBuffer, totalBytes, resultBuffer.length );
                totalBytes += returnedBytes;
            }
        } catch ( DataFormatException e ) {
            System.err.println( e.getMessage() );
            System.exit( 1 );
        }  
    }

    // reverse filter method 0
    void reverseFilter()
    {
        int offset = 0; // beginning position of the current scanline
        int a, b, c, x, r;

        //   c b
        //   a x
        // x is resultBuffer[offset + j]
        // r is its value after filter reversed
        for ( int i = 0; i < height; i++ ) { // one scanline a time
            int filterType = resultBuffer[offset]; // filter type byte

            for ( int j = 1; j < lineWidth; j++ ) {
                // get a, b, c, x as nonnegative integers
                if ( j < 4 ) {
                    a = 0;
                } else {
                    a = resultBuffer[offset + j - 4];
                }

                if ( i == 0 ) {
                    b = c = 0;
                } else {
                    b = resultBuffer[offset + j - lineWidth];

                    if ( j < 4 ) {
                        c = 0;
                    } else {
                        c = resultBuffer[offset + j - lineWidth - 4];
                    }
                }

                x = resultBuffer[offset + j];

                if ( a < 0 ) {
                    a += 256;
                }

                if ( b < 0 ) {
                    b += 256;
                }

                if ( c < 0 ) {
                    c += 256;
                }

                if ( x < 0 ) {
                    x += 256;
                }

                // reverse filter for the 5 filter types
                switch ( filterType ) {
                    case 0:
                        break;

                    case 1:
                        if ( j >= 4 ) {
                            r = x + a;

                            if ( r >= 256 ) {
                                r -= 256;
                            }

                            resultBuffer[offset + j] = ( byte )r;
                        }

                        break;

                    case 2:
                        r = x + b;

                        if ( r >= 256 ) {
                            r -= 256;
                        }

                        resultBuffer[offset + j] = ( byte )r;
                        break;

                    case 3:
                        r = x + ( a + b ) / 2;

                        if ( r >= 256 ) {
                            r -= 256;
                        }

                        resultBuffer[offset + j] = ( byte )r;
                        break;

                    case 4:
                        r = x + paeth( a, b, c, x );

                        if ( r >= 256 ) {
                            r -= 256;
                        }

                        resultBuffer[offset + j] = ( byte )r;
                        break;

                    default:
                        ;
                }
            }

            offset += lineWidth; // goto the next scanline
        }

    }

    // Paeth prediction for filter type 4
    int paeth( int a, int b, int c, int x )
    {
        int p = a + b - c;
        int pa = a <= p ? p - a : a - p;
        int pb = b <= p ? p - b : b - p;
        int pc = c <= p ? p - c : c - p;
        return ( pa <= pb && pa <= pc ) ? a :
               ( pb <= pc ? b : c );
    }

    // fill 4 bytes in BMPHeader at offset with a number
    void fillNumber( int offset, int number )
    {
        int k = 0;

        for ( ; k < 4; k++ ) {
            BMPHeader[offset + k] = ( byte )( number % 256 );
            number /= 256;

            if ( number == 0 ) {
                break;
            }
        }
    }

    // fill non-zero parameters in BMPHeader
    void fillBMPHeader()
    {
        for ( int i = 0; i < BMPHeaderSize; i++ ) {
            BMPHeader[i] = 0;
        }

        BMPHeader[0] = 'B';
        BMPHeader[1] = 'M';
        int rawDataSize = width * height * 3;
        fillNumber( 2, rawDataSize + BMPHeaderSize );
        BMPHeader[10] = 54;
        BMPHeader[14] = 40;
        fillNumber( 18, width );
        fillNumber( 22, height );
        BMPHeader[26] = 1;
        BMPHeader[28] = 24;
        fillNumber( 34, rawDataSize );
    }

    // lossless BMP as output
    void toBMP()
    {
        try {
            fillBMPHeader();
            System.out.write( BMPHeader );
            byte[] BMPData = new byte[width * height * 3];
            int m = 0;

            for ( int i = height - 1; i >= 0; --i ) {
                ++m; // Skip filter type which leads each line
                for ( int j = 0; j < width; ++j ) {
                    for ( int k = 0; k < 3; ++k ) {
                        // i * width * 3 = rows bottom to top
                        // j * 3 = pixels grouped left to right
                        // 2 - k = colors listed in reverse order from PNG
                        BMPData[i * width * 3 + j * 3 + 2 - k] = resultBuffer[m++];
                    }
                    ++m; // skip the alpha parameter
                }
            }

            System.out.write( BMPData );
        } catch ( IOException e ) {
            System.err.println( e.getMessage() );
            System.exit( 1 );
        }
    }

    public static void main( String[] args )
    {
        H19 h19 = new H19();
        h19.readHeader();
        h19.readData();
        h19.decompress();
        h19.reverseFilter();
        h19.toBMP();
    }

}

