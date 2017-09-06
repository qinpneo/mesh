package Utils;

import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Created by paqin on 06/09/2017.
 */

public class SDCardWriter {
    public static String LOG_FILE_POST_FIX = "LOG";
    public static String CONFIG_FILE_POST_FIX = "CONFIG";

    private String folerName;
    private File currentDir;
    private String conflictFix;


    private SDCardWriter(String folder, String conflictPostFix) {
        this.folerName = folder;
        this.conflictFix = conflictPostFix;
        this.init();
    }

    private void init() {
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File (sdCard.getAbsolutePath() + "/" + folerName);
        if(dir.exists()) {
            if(dir.isFile()) {
                dir = new File (sdCard.getAbsolutePath()+ "/" + folerName + "/" + conflictFix);
                dir.mkdirs();
            }
        } else {
            dir.mkdirs();
        }
        this.currentDir = dir;
    }


    public static SDCardWriter buildSDCardWriter(String folder) {
        return buildSDCardWriter(folder, "aemon");
    }


    public static SDCardWriter buildSDCardWriter(String folder, String conflictFix) {
        return new SDCardWriter(folder, conflictFix);
    }

    public  void writeLogFile(StringBuilder builder) {
        String dirPath = this.currentDir.getAbsolutePath();
        String logFileName = getBiggestAvailable(LOG_FILE_POST_FIX);
        String fullPath = dirPath + "/" + logFileName;
        try {

            BufferedWriter out = new BufferedWriter
                    (new OutputStreamWriter(new FileOutputStream(fullPath),"UTF-8"));
            out.append(builder);
            out.flush();
            out.close();
        } catch (Exception ex) {

        }

    }

    public String getBiggestAvailable(String postfix) {
        String biggest = "0000";
        String postPart = "." + postfix.toUpperCase();
        for (File f : currentDir.listFiles()) {
            if (f.isFile()) {
                String name = f.getName();

                if(name.length() ==  postPart.length() + 4) {
                    if(name.toUpperCase().endsWith(postPart)) {
                        String name1 = name.substring(0, 3);
                        if(name1.compareTo(biggest) > 0) {
                            biggest = name1;
                        }
                    }

                }
            }
        }
        int number = Integer.parseInt(biggest);
        number = number + 1;
        NumberFormat nf = new DecimalFormat("0000");
        return nf.format(number) + postPart;
    }
}
