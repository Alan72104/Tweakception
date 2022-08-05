package a7.tweakception.tweaks;

import a7.tweakception.Tweakception;
import a7.tweakception.config.Configuration;
import a7.tweakception.utils.DumpUtils;
import a7.tweakception.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

import static a7.tweakception.utils.McUtils.*;
import static a7.tweakception.utils.Utils.f;

public class APIManager extends Tweak
{
    public static class APIManagerConfig
    {
        public String apiKey = "";
    }
    private final APIManagerConfig c;
    // Value can be "null" indicating the name is invalid
    // Name should always be all lower case
    private static final Map<String, String> NAME_TO_UUID = new HashMap<>();
    private static final Map<String, JsonObject> UUID_TO_HYPIXEL_PLAYER_INFO = new HashMap<>();
    private static final Map<String, JsonObject> UUID_TO_SKYBLOCK_PLAYER_INFO = new HashMap<>();
    public static final String UUID_NOT_AVAILABLE = "null";
    public static final JsonObject INFO_NOT_AVAILABLE = new JsonObject();
    private static final int HYPIXEL_REQUEST_COOLDOWN = 600; // ms
    private boolean debug = false;
    private final Gson gson = new Gson();
    private long lastHypixelRequestTime = 0L;
    private boolean requestingUuid = false;
    private final Set<String> hypixelUuidsInRequest = new HashSet<>();
    
    public APIManager(Configuration configuration)
    {
        super(configuration);
        c = configuration.config.apiManager;
    }
    
    // Gets the player uuid instantly or queue up a request if there isn't an active request
    public String getPlayerUUID(String name)
    {
        return getPlayerUUID(name, null);
    }
    
    // Gets the player uuid instantly or queue up a request if there isn't an active request
    // onRequestComplete only fires if the data isn't in cache and needs to request
    public String getPlayerUUID(String name, Consumer<String> onRequestComplete)
    {
        String nameLower = name.toLowerCase();
        
        String uuid = NAME_TO_UUID.get(nameLower);
        
        if (uuid != null)
            return uuid;
        
        getPlayerUUIDAsync(nameLower, result ->
        {
            if (result != null)
            {
                NAME_TO_UUID.put(nameLower, result);
                if (onRequestComplete != null)
                    onRequestComplete.accept(result);
            }
        });
        return null;
    }
    
    // Queues up a request if it's been HYPIXEL_REQUEST_COOLDOWN since the last request,
    // and there isn't an ongoing request for the player
    public JsonObject getHypixelPlayerInfo(String name)
    {
        return getHypixelPlayerInfo(name, null);
    }
    
    // Queues up a request if it's been HYPIXEL_REQUEST_COOLDOWN since the last request,
    // and there isn't an ongoing request for the player
    // onRequestComplete only fires if the data isn't in cache and needs to request
    public JsonObject getHypixelPlayerInfo(String name, Consumer<JsonObject> onRequestComplete)
    {
        name = name.toLowerCase();
        String uuid = NAME_TO_UUID.get(name);
        
        if (uuid == null)
        {
            getPlayerUUID(name, res -> getHypixelPlayerInfoInternal(res, onRequestComplete));
            return null;
        }
        
        return getHypixelPlayerInfoInternal(uuid, onRequestComplete);
    }
    
    private JsonObject getHypixelPlayerInfoInternal(String uuid, Consumer<JsonObject> onRequestComplete)
    {
        if (uuid.equals(UUID_NOT_AVAILABLE))
            return INFO_NOT_AVAILABLE;
        
        if (UUID_TO_HYPIXEL_PLAYER_INFO.containsKey(uuid))
            return UUID_TO_HYPIXEL_PLAYER_INFO.get(uuid);
        
        if (hypixelUuidsInRequest.contains(uuid))
            return null;
        
        hypixelUuidsInRequest.add(uuid);
        
        Map<String, String> args = new HashMap<>();
        args.put("uuid", uuid);
        getHypixelApiAsync(c.apiKey, "player", args,
            result ->
            {
                hypixelUuidsInRequest.remove(uuid);
                
                if (result == null)
                    return;
                
                if (result.has("success") && result.get("success").getAsBoolean() &&
                    result.has("player"))
                {
                    if (!result.get("player").isJsonObject())
                    {
                        UUID_TO_HYPIXEL_PLAYER_INFO.put(uuid, INFO_NOT_AVAILABLE);
                        return;
                    }
                    
                    JsonObject player = result.get("player").getAsJsonObject();
                    UUID_TO_HYPIXEL_PLAYER_INFO.put(uuid, player);
                    if (onRequestComplete != null)
                        onRequestComplete.accept(player);
                }
                else
                    UUID_TO_HYPIXEL_PLAYER_INFO.put(uuid, INFO_NOT_AVAILABLE);
            });
        
        return null;
    }
    
    // Queues up a request if it's been HYPIXEL_REQUEST_COOLDOWN since the last request,
    // and there isn't an ongoing request for the player
    public JsonObject getSkyblockPlayerInfo(String name)
    {
        return getSkyblockPlayerInfo(name, null);
    }
    
    // Queues up a request if it's been HYPIXEL_REQUEST_COOLDOWN since the last request,
    // and there isn't an ongoing request for the player
    // onRequestComplete only fires if the data isn't in cache and needs to request
    public JsonObject getSkyblockPlayerInfo(String name, Consumer<JsonObject> onRequestComplete)
    {
        name = name.toLowerCase();
        String uuid = NAME_TO_UUID.get(name);
        
        if (uuid == null)
        {
            getPlayerUUID(name, res -> getSkyblockPlayerInfoInternal(res, onRequestComplete));
            return null;
        }
        
        return getSkyblockPlayerInfoInternal(uuid, onRequestComplete);
    }
    
    private JsonObject getSkyblockPlayerInfoInternal(String uuid, Consumer<JsonObject> onRequestComplete)
    {
        if (uuid.equals(UUID_NOT_AVAILABLE))
            return INFO_NOT_AVAILABLE;
        
        if (UUID_TO_SKYBLOCK_PLAYER_INFO.containsKey(uuid))
            return UUID_TO_SKYBLOCK_PLAYER_INFO.get(uuid);
        
        if (hypixelUuidsInRequest.contains(uuid))
            return null;
        
        hypixelUuidsInRequest.add(uuid);
        
        HashMap<String, String> args = new HashMap<>();
        args.put("uuid", uuid);
        
        getHypixelApiAsync(c.apiKey, "skyblock/profiles", args,
            result ->
            {
                hypixelUuidsInRequest.remove(uuid);
                
                if (result == null)
                    return;
                
                if (result.has("success") && result.get("success").getAsBoolean() &&
                    result.has("profiles"))
                {
                    if (!result.get("profiles").isJsonArray())
                    {
                        UUID_TO_SKYBLOCK_PLAYER_INFO.put(uuid, INFO_NOT_AVAILABLE);
                        return;
                    }
                    
                    JsonArray profiles = result.get("profiles").getAsJsonArray();
                    
                    JsonObject selectedProfileMember = null;
                    long biggestLastSave = 0L;
                    // Find the most recently used profile
                    for (int i = 0; i < profiles.size(); i++)
                    {
                        JsonObject profile = profiles.get(i).getAsJsonObject();
                        JsonObject members = profile.get("members").getAsJsonObject();
                        JsonObject member = members.get(uuid).getAsJsonObject();
                        if (member.has("last_save"))
                        {
                            long lastSave = member.get("last_save").getAsLong();
                            if (lastSave > biggestLastSave)
                            {
                                selectedProfileMember = member;
                                biggestLastSave = lastSave;
                            }
                        }
                        else
                            selectedProfileMember = member;
                    }
                    
                    UUID_TO_SKYBLOCK_PLAYER_INFO.put(uuid, selectedProfileMember);
                    if (onRequestComplete != null)
                        onRequestComplete.accept(selectedProfileMember);
                }
                else
                    UUID_TO_SKYBLOCK_PLAYER_INFO.put(uuid, INFO_NOT_AVAILABLE);
            });
        
        return null;
    }
    
    // Queues up a request if it's been HYPIXEL_REQUEST_COOLDOWN since the last request
    // callback will be scheduled on the main thread
    private void getHypixelApiAsync(String apiKey, String method, Map<String, String> args,
                                    Consumer<JsonObject> onComplete)
    {
        if (apiKey == null || apiKey.equals(""))
        {
            onComplete.accept(null);
            return;
        }
        
        if (System.currentTimeMillis() - lastHypixelRequestTime < HYPIXEL_REQUEST_COOLDOWN)
        {
            onComplete.accept(null);
            return;
        }
        
        lastHypixelRequestTime = System.currentTimeMillis();
        
        String url = makeHypixelApiUrl(apiKey, method, args);
        
        System.out.println(f("AM: hypixel api request started, url: %s", url));
        
        getApiAsync(url,
            result -> Tweakception.scheduler.add(() ->
            {
                System.out.println(f("AM: hypixel api request completed, url: %s", url));
                
                onComplete.accept(result);
            }),
            () -> Tweakception.scheduler.add(() ->
            {
                System.out.println(f("AM: hypixel api request failed, url: %s", url));
                
                onComplete.accept(null);
            }));
    }
    
    // Queues up a request if there isn't an active request,
    // callback will be scheduled on the main thread
    private void getPlayerUUIDAsync(String name, Consumer<String> onComplete)
    {
        if (requestingUuid)
        {
            onComplete.accept(null);
            return;
        }
        
        System.out.println(f("AM: uuid request started, player: %s", name));
        
        requestingUuid = true;
        
        getApiAsync("https://api.mojang.com/users/profiles/minecraft/" + name,
            (jsonObject) ->
            {
                if (jsonObject.has("id") && jsonObject.get("id").isJsonPrimitive() &&
                    ((JsonPrimitive)jsonObject.get("id")).isString())
                {
                    String uuid = jsonObject.get("id").getAsString();
                    
                    Tweakception.scheduler.add(() ->
                    {
                        System.out.println(f("AM: uuid request completed, player: %s, uuid: %s", name, uuid));
                        
                        onComplete.accept(uuid);
                        requestingUuid = false;
                    });
                }
                else
                {
                    Tweakception.scheduler.add(() ->
                    {
                        System.out.println(f("AM: uuid request completed with failed result, player: %s", name));
                        
                        onComplete.accept(UUID_NOT_AVAILABLE);
                        requestingUuid = false;
                    });
                }
            },
            () -> Tweakception.scheduler.add(() ->
            {
                System.out.println(f("AM: uuid request failed, player: %s", name));
                
                onComplete.accept(UUID_NOT_AVAILABLE);
                requestingUuid = false;
            })
        );
    }
    
    // This executes off the main thread
    private void getApiAsync(String url, Consumer<JsonObject> onSuccess, Runnable onError)
    {
        Tweakception.threadPool.submit(() ->
        {
            try
            {
                onSuccess.accept(getApiSync(url));
            }
            catch (Exception e)
            {
                System.out.println(f("AM: api request failed, url: %s, exception: %s", url, e.toString()));
                onError.run();
            }
        });
    }
    
    private JsonObject getApiSync(String urlS) throws IOException
    {
        URL url = new URL(urlS);
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        
        String response = IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
        
        JsonObject json = gson.fromJson(response, JsonObject.class);
        if (json == null)
            throw new ConnectException("Invalid JSON");
        
        return json;
    }
    
    private String makeHypixelApiUrl(String apiKey, String method, Map<String, String> args)
    {
        if (apiKey != null)
            args.put("key", apiKey.trim());
        StringBuilder url = new StringBuilder("https://api.hypixel.net/" + method);
        boolean first = true;
        for (Map.Entry<String, String> entry : args.entrySet())
        {
            if (first)
            {
                url.append("?");
                first = false;
            }
            else
            {
                url.append("&");
            }
            try
            {
                url.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.name()));
                url.append("=");
                url.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.name()));
            }
            catch (UnsupportedEncodingException ignored)
            {
            }
        }
        return url.toString();
    }
    
    public boolean hasApiKey()
    {
        return !c.apiKey.equals("");
    }
    
    
    public void toggleDebug()
    {
        debug = !debug;
        sendChat("AM: toggled debug " + debug);
    }
    
    public void setApiKey(String key)
    {
        if (key.equals(""))
        {
            c.apiKey = "";
            sendChat("AM: removed api key");
        }
        else
        {
            c.apiKey = key;
            sendChat("AM: set api key to: " + key);
        }
    }
    
    public void removeCache(String name)
    {
        name = name.toLowerCase();
        String key = NAME_TO_UUID.get(name);
        if (key != null && !key.equals(UUID_NOT_AVAILABLE))
        {
            UUID_TO_HYPIXEL_PLAYER_INFO.remove(key);
            UUID_TO_SKYBLOCK_PLAYER_INFO.remove(key);
        }
        NAME_TO_UUID.remove(name);
    }
    
    public void freeCaches()
    {
        NAME_TO_UUID.clear();
        UUID_TO_HYPIXEL_PLAYER_INFO.clear();
        UUID_TO_SKYBLOCK_PLAYER_INFO.clear();
        sendChat("AM: cleared all caches");
    }
    
    public void printCaches()
    {
        sendChat("AM: printing caches");
        
        for (Map.Entry<String, String> entry : NAME_TO_UUID.entrySet())
            sendChat("AM: NAME-UUID -> " + entry.getKey() + ": " +
                (entry.getValue().equals(UUID_NOT_AVAILABLE) ? "not available" : ('"' + entry.getValue() + '"')));
        
        for (Map.Entry<String, JsonObject> entry : UUID_TO_HYPIXEL_PLAYER_INFO.entrySet())
            sendChat("AM: UUID-HYINFO -> " + entry.getKey() + ": " +
                (entry.getValue() == INFO_NOT_AVAILABLE ? "not available" : "object"));
        
        for (Map.Entry<String, JsonObject> entry : UUID_TO_SKYBLOCK_PLAYER_INFO.entrySet())
            sendChat("AM: UUID-SBINFO -> " + entry.getKey() + ": " +
                (entry.getValue() == INFO_NOT_AVAILABLE ? "not available" : "object"));
        
        for (String s : hypixelUuidsInRequest)
            sendChat("AM: uuids in request -> " + s);
        
        sendChat("AM: done");
    }
    
    public void copySkyblockProfile(String name)
    {
        if (name.equals(""))
            name = getPlayer().getName();
        
        sendChat("AM: getting profile of " + name);
        
        String uuid = getPlayerUUID(name);
        if (uuid == null)
        {
            sendChat("AM: uuid not in cache, try again later");
            return;
        }
        
        if (uuid.equals(UUID_NOT_AVAILABLE))
        {
            sendChat("AM: failed retriving uuid for this player");
            return;
        }
        
        JsonObject obj = getSkyblockPlayerInfo(name);
        if (obj == null)
        {
            sendChat("AM: profile not in cache, try again later");
            return;
        }
        
        Utils.setClipboard(DumpUtils.prettifyJson(obj.toString()));
        sendChat("AM: copied profile of " + name + " to clipboard");
    }
}
