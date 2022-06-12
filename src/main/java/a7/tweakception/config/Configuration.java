package a7.tweakception.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
    private final Gson gson;
    public TweakceptionConfig config;

    public Configuration(String folderPath) throws Exception
    {
        path = folderPath;
        fileName = "config.json";
        dirFile = new File(path);
        if (!dirFile.exists())
        {
            boolean ignored = dirFile.mkdirs();
        }
        file = createFile(fileName);
        gson = new GsonBuilder().setPrettyPrinting().create();

        if (!file.exists())
        {
            boolean ignored = file.createNewFile();
            config = new TweakceptionConfig();
            writeConfig();
        }
        else
        {
            loadConfig();
        }
    }

    public void loadConfig() throws Exception
    {
        BufferedReader reader = createReaderFor(file);
        config = gson.fromJson(reader, TweakceptionConfig.class);
        if (config == null)
            config = new TweakceptionConfig();
        reader.close();
    }

    public void writeConfig() throws Exception
    {
        BufferedWriter writer = createWriterFor(file);
        writer.write(gson.toJson(config));
        writer.close();
    }

    // Use this for default of utf-8
    public BufferedReader createReaderFor(File file) throws IOException
    {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                Files.newInputStream(file.toPath().toAbsolutePath()),
                StandardCharsets.UTF_8));
        return bufferedReader;
    }

    // Use this for default of utf-8
    public BufferedWriter createWriterFor(File file) throws IOException
    {
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(
                Files.newOutputStream(file.toPath().toAbsolutePath()),
                StandardCharsets.UTF_8));
        return bufferedWriter;
    }

    public File createWriteFileWithCurrentDateTime(String name, List<String> lines) throws IOException
    {
        File file = createFileWithCurrentDateTime(name);

        BufferedWriter writer = createWriterFor(file);

        for (String line : lines) {
            writer.write(line);
            writer.newLine();
        }

        writer.close();

        return file;
    }

    public File createFileWithCurrentDateTime(String name) throws IOException
    {
        // a_$_b.txt -> a_time_b.txt / a_time_b_1.txt
        int periodIndex = name.lastIndexOf(".");
        String base = name.substring(0, periodIndex);
        String ext = ensureValidFileName(name.substring(periodIndex));
        String[] baseSplit = base.split("\\$", 2);

        String newBase = ensureValidFileName(baseSplit[0]) +
                new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date(System.currentTimeMillis())) +
                (baseSplit.length > 1 ? ensureValidFileName(baseSplit[1]) : "");

        name = newBase + ext;

        File file = createFile(name, false);

        int count = 1;
        while (file.exists())
        {
            name = newBase + "_" + count + ext;
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

    public static String ensureValidFileName(String s)
    {
        return s.replaceAll("[^A-Za-z\\d_\\-.]", "_");
    }
}
