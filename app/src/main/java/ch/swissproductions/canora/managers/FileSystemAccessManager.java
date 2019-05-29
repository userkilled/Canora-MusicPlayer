package ch.swissproductions.canora.managers;

import android.util.Log;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class FileSystemAccessManager {
    public FileSystemAccessManager(String path) {
        file = new File(path);
        writing = false;
        reading = false;
        Log.v(LOG_TAG, "FSAM Initialized, Path: " + file.getAbsolutePath());
    }

    public int write(byte[] data) {
        writeData(data);
        return 0;
    }

    public byte[] read() {
        return readData();
    }

    private File file;

    private boolean writing;
    private boolean reading;

    private String LOG_TAG = "FSAM";

    private byte[] readData() {
        try {
            if (file == null || !file.exists())
                return new byte[0];
            byte[] bFile = new byte[(int) file.length()];
            FileInputStream fileInputStream = new FileInputStream(file);
            fileInputStream.read(bFile);
            fileInputStream.close();
            return decompress(bFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    private void writeData(byte[] data) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            byte[] wd = compress(data);
            bos.write(wd);
            bos.flush();
            bos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Compression
    private byte[] compress(byte[] data) throws IOException {
        Log.v(LOG_TAG, file.getName() + " DECOMPRESSED SIZE: " + data.length + " BYTES");
        ByteArrayOutputStream os = new ByteArrayOutputStream(data.length);
        GZIPOutputStream gos = new GZIPOutputStream(os);
        gos.write(data);
        gos.close();
        byte[] compressed = os.toByteArray();
        os.close();
        Log.v(LOG_TAG, file.getName() + " COMPRESSED SIZE: " + compressed.length + " BYTES");
        return compressed;
    }

    private byte[] decompress(byte[] compressed) throws IOException {
        Log.v(LOG_TAG, file.getName() + " COMPRESSED SIZE: " + compressed.length + " BYTES");
        final int BUFFER_SIZE = 32;
        ByteArrayInputStream is = new ByteArrayInputStream(compressed);
        GZIPInputStream gis = new GZIPInputStream(is, BUFFER_SIZE);
        List<Byte> data = new ArrayList<>();
        int bytesRead;
        byte[] buffer = new byte[BUFFER_SIZE];
        while ((bytesRead = gis.read(buffer)) != -1) {
            for (int i = 0; i < bytesRead; i++) {
                data.add(buffer[i]);
            }
        }
        gis.close();
        is.close();
        byte[] ret = new byte[data.size()];
        for (int i = 0; i < data.size(); i++) {
            ret[i] = data.get(i);
        }
        Log.v(LOG_TAG, file.getName() + " DECOMPRESSED SIZE: " + ret.length + " BYTES");
        return ret;
    }
}
