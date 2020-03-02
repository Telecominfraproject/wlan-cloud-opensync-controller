package com.telecominfraproject.wlan.opensync.util;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class ZlibUtil {
    
    public static final int MAX_BUFFER_SIZE = 8192;

    public static byte[] compress(byte[] bytesToCompress) {
        if(bytesToCompress == null) {
            return null;
        }
        
        if(bytesToCompress.length==0) {
            return new byte[0];
        }
        
        Deflater deflater = new Deflater();
        deflater.setInput(bytesToCompress);
        deflater.finish();

        byte[] bytesCompressed = new byte[bytesToCompress.length];

        int numberOfBytesAfterCompression = deflater.deflate(bytesCompressed);
        
        if(numberOfBytesAfterCompression == 0) {
            throw new IllegalStateException("Compressed size is greater than original?");
        }

        byte[] returnValues = new byte[numberOfBytesAfterCompression];

        System.arraycopy(bytesCompressed, 0, returnValues, 0, numberOfBytesAfterCompression);

        return returnValues;
    }

    private static class SizeAndBuffer{
        int size;
        byte[] buffer;
        
        public SizeAndBuffer(int size, byte[] buffer) {
            this.size = size;
            this.buffer = buffer;
        }
    }
    
    public static byte[] decompress(byte[] bytesToDecompress)
    {
        if(bytesToDecompress == null) {
            return null;
        }
        
        if(bytesToDecompress.length == 0) {
            return new byte[0];
        }
        
        byte[] returnValues = null;

        Inflater inflater = new Inflater();
        int numberOfBytesToDecompress = bytesToDecompress.length;
        inflater.setInput(bytesToDecompress, 0, numberOfBytesToDecompress);

        int bufferSizeInBytes = numberOfBytesToDecompress;

        int numberOfBytesDecompressedSoFar = 0;
        List<SizeAndBuffer> bytesDecompressedSoFar = new ArrayList<>();

        try
        {
            while (!inflater.needsInput())
            {
                byte[] bytesDecompressedBuffer = new byte[bufferSizeInBytes];

                int numberOfBytesDecompressedThisTime = inflater.inflate(bytesDecompressedBuffer);
                numberOfBytesDecompressedSoFar += numberOfBytesDecompressedThisTime;

                bytesDecompressedSoFar.add(new SizeAndBuffer(numberOfBytesDecompressedThisTime, bytesDecompressedBuffer));
            }

            
            //now stitch together all the buffers we've collected
            returnValues = new byte[numberOfBytesDecompressedSoFar];
            int destPos = 0;
            int length = 0;
            
            for(SizeAndBuffer partBuffer : bytesDecompressedSoFar) {
                length = partBuffer.size;
                System.arraycopy(partBuffer.buffer, 0, returnValues, destPos, length);
                destPos += length;
            }

        } catch (DataFormatException dfe) {
            throw new IllegalStateException(dfe);
        }

        inflater.end();

        return returnValues;
    }

}
