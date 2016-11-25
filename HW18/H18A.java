// H18A.java CS6025 Yizong Cheng March 2015
// LZW encoding
// Usage: java H18A < original > encoded

import java.io.*;
import java.util.*;

class Node
{
    Node nextSibling;
    Node children;
    int symbol;
    int code;

    public Node( Node n, Node c, int s, int d )
    {
        nextSibling = n;
        children = c;
        symbol = s;
        code = d;
    }

    Node findChild( int s )
    {
        Node k = children;

        for ( ; k != null; k = k.nextSibling ) { 
            if ( s == k.symbol ) {
                break;
            }
        }

        return k;
    }

    void addChild( int s, int d )
    {
        Node newChild = new Node( children, null, s, d );
        children = newChild;
    }

}

public class H18A
{

    static final int bufSize = 65536;
    static final int dictionaryCapacity = 4096;
    byte[] buffer = new byte[bufSize];
    int dictionarySize = 0;
    Node dictionary = null;
    int dataLength = 0;
    int dataPosition = 0;

    void readBlock()
    {
        try {
            dataLength = System.in.read( buffer );
        } catch ( IOException e ) {
            System.err.println( "IOException" );
            System.exit( 1 );
        }
    }

    void initializeDictionary()
    {
        dictionary = new Node( null, null, -1, -1 );

        for ( int i = 0; i < 256; i++ ) {
            dictionary.addChild( i, i );
        }

        dictionarySize = 256;
        dataPosition = 0;
    }

    int longestMatch()
    {
        int codeword = -1;
        Node node = dictionary;
        Node parentNode = null;

        while ( dataPosition < dataLength && node != null ) {
            parentNode = node;
            node = node.findChild( buffer[dataPosition++] );
        }

        if ( node == null ) {
            dataPosition--;
            codeword = parentNode.code;

            if ( dictionarySize < dictionaryCapacity ) {
                parentNode.addChild( buffer[dataPosition], dictionarySize++ );
            }
        } else {
            codeword = node.code;
        }

        return codeword;
    }

    void encodeBlock()
    {
        while ( dataPosition < dataLength ) {
            int codeword = longestMatch();

            if ( codeword >= 0 ) {
                System.out.println( codeword );
            }
        }
    }

    public static void main( String[] args )
    {
        H18A h18 = new H18A();
        h18.initializeDictionary();
        h18.readBlock();
        h18.encodeBlock();
    }

}

