package cambridge.util;

import java.io.*;
import java.util.stream.Collectors;

public class FileIO
{
    public static File JAR_DIRECTORY;
    public static File TEXCONV_EXECUTABLE;

    static
    {
        try
        {
            JAR_DIRECTORY =
                new File(FileIO.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile();
        }
        catch (Exception e)
        {
            JAR_DIRECTORY = new File("./");
        }
        
        TEXCONV_EXECUTABLE = new File(JAR_DIRECTORY, "bin/texconv.exe");
    }

    public static String getResourceFileAsString(String fileName)
    {
        try (InputStream is = FileIO.class.getResourceAsStream(fileName))
        {
            if (is == null) return null;
            try (InputStreamReader isr = new InputStreamReader(is); BufferedReader reader =
                new BufferedReader(isr))
            {
                return reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        }
        catch (Exception ex) { return null; }
    }

    public static byte[] getResourceFile(String filename)
    {
        try (InputStream stream = FileIO.class.getResourceAsStream(filename))
        {
            if (stream == null) return null;
            return stream.readAllBytes();
        }
        catch (Exception ex) { return null; }
    }

    public static boolean write(String path, byte[] data)
    {
        File file = new File(path);
        if (file.getParentFile() != null)
            file.getParentFile().mkdirs();
        try
        {
            // System.out.println("[FileIO] Writing file to " + path);
            FileOutputStream stream = new FileOutputStream(path);
            stream.write(data);
            stream.close();
        }
        catch (IOException ex)
        {
            // System.out.println("[FileIO] Failed to write file to " + path);
            return false;
        }
        return true;
    }

    public static byte[] read(String path)
    {
        try
        {
            // System.out.println("[FileIO] Reading file at " + path);

            File file = new File(path);

            FileInputStream stream = new FileInputStream(file);

            long size = file.length();
            byte[] buffer = new byte[(int) size];
            stream.read(buffer);

            stream.close();
            return buffer;
        }
        catch (IOException ex)
        {
            // System.out.println(String.format("[%s] Failed to read file at path (%s), does it
            // exist?", "FileIO", path));
            return null;
        }
    }
}
