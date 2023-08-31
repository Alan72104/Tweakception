package a7.tweakception.config;

import a7.tweakception.utils.Utils;
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
    private final String path;
    private final String fileName;
    private final File dirFile;
    private final File file;
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
    }
    
    public void initConfig() throws Exception
    {
        if (!dirFile.exists())
        {
            boolean ignored = dirFile.mkdirs();
        }
        
        if (!file.exists())
        {
            boolean ignored = file.createNewFile();
            config = new TweakceptionConfig();
            writeConfig();
        }
        else
        {
            Utils.fileCopy(file, createFile(fileName + ".old"));
            loadConfig();
        }
    }
    
    public void loadConfig() throws Exception
    {
        try (BufferedReader reader = createReaderFor(file))
        {
            config = gson.fromJson(reader, TweakceptionConfig.class);
            if (config == null)
                config = new TweakceptionConfig();
        }
    }
    
    public void writeConfig() throws Exception
    {
        try (BufferedWriter writer = createWriterFor(file))
        {
            writer.write(gson.toJson(config));
        }
    }
    
    /**
     * Creates an utf-8 writer
     */
    public BufferedReader createReaderFor(File file) throws IOException
    {
        return new BufferedReader(new InputStreamReader(
            Files.newInputStream(file.toPath().toAbsolutePath()),
            StandardCharsets.UTF_8));
    }
    
    /**
     * Creates an utf-8 writer
     */
    public BufferedWriter createWriterFor(File file) throws IOException
    {
        return new BufferedWriter(new OutputStreamWriter(
            Files.newOutputStream(file.toPath().toAbsolutePath()),
            StandardCharsets.UTF_8));
    }
    
    /**
     * a_$_b.txt -> a_time_b.txt / a_time_b_1.txt
     * <p>
     * Example:
     * <blockquote><pre>
     *     try
     *     {
     *         File file = Tweakception.configuration.createWriteFileWithCurrentDateTime("name_$.txt", lines);
     *         sendChat("Dumped name");
     *         getPlayer().addChatMessage(new ChatComponentTranslation("Output written to file %s",
     *             McUtils.makeFileLink(file)));
     *         Desktop.getDesktop().open(file);
     *     }
     *     catch (IOException e)
     *     {
     *         sendChat("Failed to write or open file");
     *     }
     * </pre></blockquote>
     */
    public File createWriteFileWithCurrentDateTime(String name, List<String> lines) throws IOException
    {
        File file = createFileWithCurrentDateTime(name);
        
        try (BufferedWriter writer = createWriterFor(file))
        {
            for (int i = 0; i < lines.size(); i++)
            {
                String line = lines.get(i);
                writer.write(line);
                if (i < lines.size() - 1)
                    writer.newLine();
            }
        }
        
        return file;
    }
    
    /**
     * a_$_b.txt -> a_time_b.txt / a_time_b_1.txt
     */
    public File createFileWithCurrentDateTime(String name) throws IOException
    {
        // a_$_b.txt -> a_time_b.txt / a_time_b_1.txt
        int periodIndex = name.lastIndexOf(".");
        String base = name.substring(0, periodIndex);
        String ext = getValidFileName(name.substring(periodIndex));
        String[] baseSplit = base.split("\\$", 2);
        
        String newBase = getValidFileName(baseSplit[0]) +
            new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date(System.currentTimeMillis())) +
            (baseSplit.length > 1 ? getValidFileName(baseSplit[1]) : "");
        
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
    
    public static String getValidFileName(String s)
    {
        return s.replaceAll("[^A-Za-z\\d_\\-.]", "_");
    }
}
