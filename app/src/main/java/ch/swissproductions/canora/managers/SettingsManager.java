package ch.swissproductions.canora.managers;

import android.content.Context;
import android.util.Log;
import ch.swissproductions.canora.data.Constants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingsManager {
    public SettingsManager(Context mA) {
        String sp = mA.getCacheDir().getAbsolutePath() + "/settings";
        fsa = new FileSystemAccessManager(sp);
        data = readData();
        if (!verifyData(data)) {
            Log.e(LOG_TAG, "NONE/CORRUPT LOCAL DATA FOUND");
            data.clear();
            data.put(Constants.SETTING_SEARCHBY, "" + Constants.SEARCH_BYTITLE);
            data.put(Constants.SETTING_SORTBY, "" + Constants.SORT_BYTITLE);
            data.put(Constants.SETTING_VOLUME, "0.7");
            data.put(Constants.SETTING_REPEAT, "false");
            data.put(Constants.SETTING_SHUFFLE, "false");
            data.put(Constants.SETTING_THEME, Constants.THEME_DEFAULT);
            data.put(Constants.SETTING_EQUALIZERPRESET, "0");
            writeData(data);
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
        writeData(data);
    }

    private String LOG_TAG = "SETC";

    private Map<String, String> data; //Key = Constants.SETTING_...

    private FileSystemAccessManager fsa;

    //XML Abstraction Layer
    private Map<String, String> readData() {
        Map<String, String> ret = new HashMap<>();
        String XMLSTR = new String(fsa.read());
        if (XMLSTR.length() > 0) {
            ret = convertXMLtoMAP(XMLSTR);
        }
        return ret;
    }

    private void writeData(Map<String, String> data) {
        if (data.size() > 0) {
            String writeStr = convertMAPtoXML(data);
            fsa.write(writeStr.getBytes());
        }
    }

    //XML Conversion Methods
    private Map<String, String> convertXMLtoMAP(String xml) {
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
                ret.put(setl.item(i).getNodeName(), setl.item(i).getTextContent());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    private String convertMAPtoXML(Map<String, String> data) {
        String ret = "<?xml version=\"1.0\"?><settings>";
        for (Map.Entry<String, String> entry : data.entrySet()) {
            ret += "<" + encodeXML(entry.getKey()) + ">" + encodeXML(entry.getValue()) + "</" + encodeXML(entry.getKey()) + ">";
        }
        ret += "</settings>";
        return ret;
    }

    public static String encodeXML(CharSequence s) {
        StringBuilder sb = new StringBuilder();
        int len = s.length();
        for (int i = 0; i < len; i++) {
            int c = s.charAt(i);
            if (c >= 0xd800 && c <= 0xdbff && i + 1 < len) {
                c = ((c - 0xd7c0) << 10) | (s.charAt(++i) & 0x3ff);    // UTF16 decode
            }
            if (c < 0x80) {      // ASCII range: test most common case first
                if (c < 0x20 && (c != '\t' && c != '\r' && c != '\n')) {
                    // Illegal XML character, even encoded. Skip or substitute
                    sb.append("&#xfffd;");   // Unicode replacement character
                } else {
                    switch (c) {
                        case '&':
                            sb.append("&amp;");
                            break;
                        case '>':
                            sb.append("&gt;");
                            break;
                        case '<':
                            sb.append("&lt;");
                            break;
                        case '\'':
                            sb.append("&apos;");
                            break;
                        case '\"':
                            sb.append("&quot;");
                            break;
                        case '\n':
                            sb.append("&#10;");
                            break;
                        case '\r':
                            sb.append("&#13;");
                            break;
                        case '\t':
                            sb.append("&#9;");
                            break;

                        default:
                            sb.append((char) c);
                    }
                }
            } else if ((c >= 0xd800 && c <= 0xdfff) || c == 0xfffe || c == 0xffff) {
                // Illegal XML character, even encoded. Skip or substitute
                sb.append("&#xfffd;");   // Unicode replacement character
            } else {
                sb.append("&#x");
                sb.append(Integer.toHexString(c));
                sb.append(';');
            }
        }
        return sb.toString();
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
