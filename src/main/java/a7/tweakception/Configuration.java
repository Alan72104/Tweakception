package a7.tweakception;

import com.google.gson.Gson;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class Configuration
{
    private String path;
    private String fileName;
    private File dirFile;
    private File file;

    public Configuration(String folderPath) throws IOException
    {
        path = folderPath;
        fileName = "config.json";
        dirFile = new File(path);
        if (!dirFile.exists())
            dirFile.mkdirs();
        file = createFile(fileName);
//        try
//        {
//            BufferedReader reader = new BufferedReader(new InputStreamReader(
//                    Files.newInputStream(file.toPath()),
//                    StandardCharsets.UTF_8));
//            gson.fromJson(reader);
//        }
//        catch (IOException e)
//        {
//            throw new IOException(e);
//        }
    }

    public File createWriteFileWithCurrentDateTimeSuffix(String name, List<String> lines) throws IOException
    {
        File file = createFileWithCurrentDateTimeSuffix(name);

        BufferedWriter writer = new BufferedWriter(new FileWriter(file));

        for (String line : lines) {
            writer.write(line);
            writer.newLine();
        }

        writer.close();

        return file;
    }

    public File createFileWithCurrentDateTimeSuffix(String name) throws IOException
    {
        String[] split = name.split("\\.");
        String nameBase = split[0] + "_" + new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date(System.currentTimeMillis()));
        name = nameBase + "." + split[1];
        File file = createFile(name, false);

        int count = 1;
        while (file.exists())
        {
            name = nameBase + "_" + count + "." + split[1];
            file = createFile(name, false);
            count++;
        }

        file.createNewFile();

        return file;
    }

    public File createFile(String name) throws IOException
    {
        return createFile(name, true);
    }

    public File createFile(String name, boolean actuallyCreate) throws IOException
    {
        File file = new File(dirFile, name);
        if (actuallyCreate)
            file.createNewFile();
        return file;
    }
}
