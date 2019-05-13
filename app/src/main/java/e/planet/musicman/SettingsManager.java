package e.planet.musicman;

import android.content.Context;
import android.util.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class SettingsManager {
    public SettingsManager(Context mA) {
        mainAct = mA;
        settingsFile = new File(mainAct.getExternalCacheDir().getAbsolutePath() + "/settings.xml");
        Log.v(LOG_TAG, "SETTINGS PATH: " + settingsFile.getAbsolutePath());
        data = getDataAsMap(settingsFile.getAbsolutePath());
        if (!verifyData(data)) {
            Log.v(LOG_TAG, "NONE/CORRUPT LOCAL DATA FOUND");
            data.clear();
            data.put(Constants.SETTING_SEARCHBY, "" + Constants.SEARCH_BYTITLE);
            data.put(Constants.SETTING_SORTBY, "" + Constants.SORT_BYTITLE);
            data.put(Constants.SETTING_VOLUME, "0.7");
            data.put(Constants.SETTING_REPEAT, "false");
            data.put(Constants.SETTING_SHUFFLE, "false");
            writeDataAsXML(settingsFile.getAbsolutePath(), data);
        }
    }

    public String getSetting(String index) {
        if (data.get(index) != null)
            return data.get(index);
        else
            return "";
    }

    public void putSetting(String index, String dataS) {
        data.put(index, dataS);
        writeDataAsXML(settingsFile.getAbsolutePath(), data);
    }

    private File settingsFile;
    private String LOG_TAG = "SETC";
    private Context mainAct;
    private Map<String, String> data; //Key = Constants.SETTING_...

    //XML Abstraction Layer
    private Map<String, String> getDataAsMap(String path) {
        Log.v(LOG_TAG, "GETDATAASMAP");
        Map<String, String> ret = new HashMap<>();
        String XMLSTR = readFromDisk(path);
        if (XMLSTR.length() > 0) {
            ret = getMapFromXML(XMLSTR);
        }
        return ret;
    }

    private void writeDataAsXML(String path, Map<String, String> data) {
        if (data.size() > 0) {
            String writeStr = getXmlFromMap(data);
            writeToDisk(path, writeStr);
        }
    }

    //File System Abstraction Layer
    private String readFromDisk(String path) {
        Log.v(LOG_TAG, "READFROMDISK");
        File rf = new File(path);
        if (!rf.exists())
            return "";
        byte[] bFile = new byte[(int) rf.length()];
        String ret = "";
        try {
            //convert file into array of bytes
            FileInputStream fileInputStream = new FileInputStream(rf);
            fileInputStream.read(bFile);
            fileInputStream.close();
            ret = decompress(bFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.v(LOG_TAG, "XML RED FROM DISK: " + ret);
        return ret;
    }

    private void writeToDisk(String path, String data) {
        Log.v(LOG_TAG, "WRITING: " + data);
        try {
            FileOutputStream fos = new FileOutputStream(new File(path));
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            byte[] tdat = compress(data);
            bos.write(tdat);
            bos.flush();
            bos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Compression
    public static byte[] compress(String string) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream(string.length());
        GZIPOutputStream gos = new GZIPOutputStream(os);
        gos.write(string.getBytes());
        gos.close();
        byte[] compressed = os.toByteArray();
        os.close();
        return compressed;
    }

    public static String decompress(byte[] compressed) throws IOException {
        final int BUFFER_SIZE = 32;
        ByteArrayInputStream is = new ByteArrayInputStream(compressed);
        GZIPInputStream gis = new GZIPInputStream(is, BUFFER_SIZE);
        StringBuilder string = new StringBuilder();
        byte[] data = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = gis.read(data)) != -1) {
            string.append(new String(data, 0, bytesRead));
        }
        gis.close();
        is.close();
        return string.toString();
    }

    //XML Conversion Methods
    private Map<String, String> getMapFromXML(String xml) {
        Map<String, String> ret = new HashMap<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            StringBuilder xmlStringBuilder = new StringBuilder();
            xmlStringBuilder.append(xml);
            ByteArrayInputStream input = new ByteArrayInputStream(xmlStringBuilder.toString().getBytes("UTF-8"));
            Document doc = builder.parse(input);
            Element root = doc.getDocumentElement();
            NodeList setl = root.getChildNodes();
            for (int i = 0; i < setl.getLength(); i++) {
                Log.v(LOG_TAG, "KEY: " + setl.item(i).getNodeName() + " VALUE: " + setl.item(i).getTextContent());
                ret.put(setl.item(i).getNodeName(), setl.item(i).getTextContent());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    private String getXmlFromMap(Map<String, String> data) {
        String ret = "<?xml version=\"1.0\"?><settings>";
        for (Map.Entry<String, String> entry : data.entrySet()) {
            ret += "<" + entry.getKey() + ">" + entry.getValue() + "</" + entry.getKey() + ">";
        }
        ret += "</settings>";
        Log.v(LOG_TAG, "BUILT XML STRING: " + ret);
        return ret;
    }

    //Misc
    private boolean verifyData(Map<String, String> dat) {
        if (dat.size() < 1) {
            return false;
        }
        List<String> sett = Constants.Settings.theSettings.getSettingsList();
        for (int i = 0; i < sett.size(); i++) {
            boolean etemp = false;
            for (Map.Entry<String, String> entry : dat.entrySet()) {
                if (entry.getKey().equals(sett.get(i)) && entry.getValue().length() > 0) {
                    etemp = true;
                    break;
                }
            }
            if (!etemp) {
                return false;
            }
        }
        return true;
    }
}
