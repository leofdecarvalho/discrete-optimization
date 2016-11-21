package input;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Leo on 17/11/2016.
 */
public class HandleFile {

    public static List<String> getLines(String ...args) throws IOException {


        String fileName = null;

        // get the temp file name
        for (String arg : args) {
            if (arg.startsWith("-file=")) {
                fileName = arg.substring(6);
            }
        }
        if (fileName == null)
            throw new FileNotFoundException("File not defined or not founded") ;

        // read the lines out of the file
        List<String> lines = new ArrayList<String>();

        ClassLoader classLoader = HandleFile.class.getClassLoader();
        BufferedReader input;
        try {

            input = new BufferedReader(new FileReader(classLoader.getResource(fileName).getFile()));
        } catch (Exception e) {
            input = new BufferedReader(new FileReader(new File(fileName)));
        }
        try {
            String line = null;
            while ((line = input.readLine()) != null) {
                lines.add(line);
            }
        } finally {
            input.close();
        }

        return lines;

    }
}
