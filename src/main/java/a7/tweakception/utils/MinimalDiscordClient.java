package a7.tweakception.utils;

import com.google.gson.*;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.Inflater;

public class MinimalDiscordClient implements Closeable
{
    public static final Gson gson = new Gson(); // Seems to be thread safe
    private static final int DISCORD_API_VERSION = 10;
    private static final String DISCORD_BASE_URL = "https://discord.com/api";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36";
    private static final String GATEWAY_URL = "wss://gateway.discord.gg"; // Bruh this never changes
    private static final String X_SUPER_PROPERTIES;
    private static final boolean COMPRESS = false;
    private final MinimalDiscordSocketClient socketClient;
    private Consumer<String> errorHandler = null;
    private Consumer<String> logHandler = null;
    private Consumer<JsonObject> messageCreatedHandler = null;
    private Consumer<JsonElement> otherEventHandler = null;
    private String username = "";
    private String token = "";
    
    static
    {
        JsonObject obj = newJsonBuilder()
            .put("os", "Windows")
            .put("browser", "Chrome")
            .put("device", "")
            .put("system_locale", "en")
            .put("browser_user_agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36")
            .put("browser_version", "110.0.0.0")
            .put("os_version", "10")
            .put("referrer", "")
            .put("referring_domain", "")
            .put("referrer_current", "")
            .put("referring_domain_current", "")
            .put("release_channel", "stable")
            .put("client_build_number", 177662)
            .put("client_event_source", (String) null)
            .put("design_id", 0)
            .getObj();
        X_SUPER_PROPERTIES = Base64.getEncoder().encodeToString(obj.toString().getBytes(StandardCharsets.UTF_8));
    }
    
    public MinimalDiscordClient()
    {
        String url = GATEWAY_URL+"/?v="+DISCORD_API_VERSION+"&encoding=json";
        if (COMPRESS)
            url += "&compress=zlib-stream";
        socketClient = new MinimalDiscordSocketClient(url);
    }
    
    // Opens a Create Message request
    public CompletableFuture<HttpURLConnection> createMessage(long channelId, String msg)
    {
        JsonObject obj = newJsonBuilder()
            .put("content", msg)
            .put("nonce", String.valueOf(System.currentTimeMillis()))
            .getObj();
        HttpURLConnection con;
        try
        {
            con = openRequest("POST", "/channels/"+channelId+"/messages");
            con.setRequestProperty("Authorization", socketClient.getToken());
            sendContent(con, obj.toString());
        }
        catch (IOException e)
        {
            CompletableFuture<HttpURLConnection> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
        
        HttpURLConnection finalCon = con;
        return CompletableFuture.supplyAsync(() ->
        {
            try
            {
                finalCon.getInputStream();
                validateResponse(finalCon);
            }
            catch (Exception e)
            {
                throw new CompletionException(e);
            }
            return finalCon;
        });
    }
    
    // Opens a Create Message request with the reply infos
    public CompletableFuture<HttpURLConnection> createReply(long channelId, String msg, long replyGuildId, long replyMessageId)
    {
        JsonObject obj = newJsonBuilder()
            .put("allowed_mentions", newJsonBuilder()
                .putArray("parse",
                    "users",
                    "roles",
                    "everyone"
                )
                .put("replied_user", true)
            )
            .put("message_reference", newJsonBuilder()
                .put("channel_id", String.valueOf(channelId))
                .put("guild_id", String.valueOf(replyGuildId))
                .put("message_id", String.valueOf(replyMessageId))
            )
            .put("content", msg)
            .put("nonce", String.valueOf(System.currentTimeMillis()))
            .getObj();
        HttpURLConnection con;
        try
        {
            con = openRequest("POST", "/channels/"+channelId+"/messages");
            con.setRequestProperty("Authorization", socketClient.getToken());
            sendContent(con, obj.toString());
        }
        catch (IOException e)
        {
            CompletableFuture<HttpURLConnection> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
        
        HttpURLConnection finalCon = con;
        return CompletableFuture.supplyAsync(() ->
        {
            try
            {
                finalCon.getInputStream();
                validateResponse(finalCon);
            }
            catch (Exception e)
            {
                throw new CompletionException(e);
            }
            return finalCon;
        });
    }
    
    // Opens a HttpURLConnection GET with the discord endpoint, without connecting
    public static HttpURLConnection openGet(String endpoint) throws IOException
    {
        return openRequest("GET", endpoint);
    }
    
    // Opens a HttpURLConnection POST with the discord endpoint, without connecting
    public static HttpURLConnection openPost(String endpoint) throws IOException
    {
        return openRequest("POST", endpoint);
    }
    
    // Opens a HttpURLConnection with the method, discord endpoint, without connecting
    public static HttpURLConnection openRequest(String method, String endpoint) throws IOException
    {
        URL url = new URL(DISCORD_BASE_URL + endpoint);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod(method);
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("X-Super-Properties", MinimalDiscordClient.X_SUPER_PROPERTIES);
        return con;
    }
    
    // Sends the request with content
    public static void sendContent(HttpURLConnection con, String content) throws IOException
    {
        if (content != null)
        {
            con.setDoOutput(true);
            try (OutputStreamWriter writer = new OutputStreamWriter(con.getOutputStream(), StandardCharsets.UTF_8))
            {
                writer.write(content);
                writer.flush();
            }
        }
    }
    
    // Requests the response
    public static String getResponseString(HttpURLConnection con) throws IOException
    {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8)))
        {
            return reader.lines().collect(Collectors.joining());
        }
    }
    
    // Throws exception if connection has an error code
    public static void validateResponse(HttpURLConnection con) throws IOException, RateLimitException, HttpException
    {
        int code = con.getResponseCode();
        if (code >= 400)
        {
            if (code == 429)
                throw new RateLimitException(con);
            else
                throw new HttpException(con, con.getResponseMessage());
        }
    }
    
    // Requests the gateway url
    public static String getGateway()
    {
        try
        {
            String res = getResponseString(openGet("/gateway"));
            return gson.fromJson(res, JsonObject.class).get("url").getAsString();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }
    
    // Connects to the socket with the provided token
    public MinimalDiscordClient connectWithToken(String token)
    {
        this.token = token;
        socketClient.connectWithToken(token);
        return this;
    }
    
    public long getClientUserId()
    {
        return socketClient.getClientUserId();
    }
    
    // Returns the socket running state
    public boolean isRunning()
    {
        return socketClient.running;
    }
    
    // Returns the socket logged in state
    public boolean isLoggedIn()
    {
        return socketClient.loggedIn;
    }
    
    // Sets the socket error string handler
    public MinimalDiscordClient onError(Consumer<String> errorHandler)
    {
        this.errorHandler = errorHandler;
        return this;
    }
    
    // Sets the socket log string handler
    public MinimalDiscordClient onLog(Consumer<String> logHandler)
    {
        this.logHandler = logHandler;
        return this;
    }
    
    // Sets the socket Message Create event handler
    // Params:
    // Msg object - https://discord.com/developers/docs/resources/channel#message-object
    public MinimalDiscordClient onMessageCreated(Consumer<JsonObject> handler)
    {
        this.messageCreatedHandler = handler;
        return this;
    }
    
    // Sets the socket event handler for other events
    // Params:
    // Event data - https://discord.com/developers/docs/topics/gateway-events#payload-structure
    public MinimalDiscordClient onOtherEventReceived(Consumer<JsonElement> handler)
    {
        this.otherEventHandler = handler;
        return this;
    }
    
    // Creates a fluent json builder
    public static JsonObjectBuilder newJsonBuilder()
    {
        return new JsonObjectBuilder();
    }
    
    private void error(String error)
    {
        if (errorHandler != null)
            errorHandler.accept(error);
    }
    
    private void log(String log)
    {
        if (logHandler != null)
            logHandler.accept(log);
    }
    
    private void messageCreated(JsonObject event)
    {
        if (messageCreatedHandler != null)
            messageCreatedHandler.accept(event);
    }
    
    
    private void otherEventReceived(JsonElement event)
    {
        if (otherEventHandler != null)
            otherEventHandler.accept(event);
    }
    
    @Override
    public void close()
    {
        socketClient.close();
    }
    
    public void closeNow()
    {
        try
        {
            socketClient.closeBlocking();
        }
        catch (InterruptedException ignored)
        {
        }
    }
    
    public static class Opcode
    {
        public static final int EVENT = 0;
        public static final int HEARTBEAT = 1;
        public static final int IDENTIFY = 2;
        public static final int PRESENCE_UPDATE = 3;
        public static final int VOICE_STATE_UPDATE = 4;
        public static final int RESUME = 6;
        public static final int RECONNECT = 7;
        public static final int REQUEST_GUILD_MEMBERS = 8;
        public static final int INVALID_SESSION = 9;
        public static final int HELLO = 10;
        public static final int HEARTBEAT_ACK = 11;
    }
    
    public static class CloseCode
    {
        public static final int UNKNOWN_ERROR = 4000;
        public static final int UNKNOWN_OPCODE = 4001;
        public static final int DECODE_ERROR = 4002;
        public static final int NOT_AUTHENTICATED = 4003;
        public static final int AUTHENTICATION_FAILED = 4004;
        public static final int ALREADY_AUTHENTICATED = 4005;
        public static final int INVALID_SEQ = 4007;
        public static final int RATE_LIMITED = 4008;
        public static final int SESSION_TIMED_OUT = 4009;
        public static final int INVALID_SHARD = 4010;
        public static final int SHARDING_REQUIRED = 4011;
        public static final int INVALID_API_VERSION = 4012;
        public static final int INVALID_INTENTS = 4013;
        public static final int DISALLOWED_INTENTS = 4014;
    }
    
    // Partially copied from https://github.com/discord-java/discord.jar
    private class MinimalDiscordSocketClient extends WebSocketClient
    {
        private String socketUrl = "";
        private String sessionId = "";
        private long clientUserId = 0;
        private String token = "";
        private boolean running = false;
        private boolean loggedIn = false;
        private Thread heartbeatThread = null;
        private Integer sequence = null;
        
        // Creates a new instance using the gateway url, without connecting
        private MinimalDiscordSocketClient(String url)
        {
            super(URI.create(url));
            socketUrl = url;
        }
        
        private String getToken()
        {
            return token;
        }
        
        private long getClientUserId()
        {
            return clientUserId;
        }
        
        // Connects to the socket and sets the token
        private void connectWithToken(String token)
        {
            if (running)
            {
                error("Cannot connect while already running");
                return;
            }
            
            if (socketUrl.startsWith("wss"))
            {
                try
                {
                    SSLContext sslContext;
                    sslContext = SSLContext.getInstance("TLS");
                    sslContext.init(null, null, null);
                    SSLSocketFactory factory = sslContext.getSocketFactory();
                    this.setSocketFactory(factory);
                }
                catch (Exception e)
                {
                    error(e.toString());
                    e.printStackTrace();
                    close();
                    return;
                }
            }
            log("Connecting");
            this.connect();
            this.token = token;
            running = true;
        }
        
        // Login with the token
        private void login()
        {
            log("Logging in");
            JsonObject identify = newJsonBuilder()
                .put("op", Opcode.IDENTIFY)
                .put("d", newJsonBuilder()
                    .put("token", token)
                    .put("properties", newJsonBuilder()
                        .put("os", System.getProperty("os.name"))
                        .put("browser", "disco")
                        .put("device", "disco")
                    )
                    .put("compress", COMPRESS)
                    .put("large_threshold", 50)
                )
                .getObj();
            send(identify.toString());
        }
        
        @Override
        public void onOpen(ServerHandshake handshakeData)
        {
            log("Socket connected");
            login();
        }
        
        @Override
        public void onMessage(ByteBuffer message)
        {
            try
            {
                if (COMPRESS)
                {
                    // Thanks to ShadowLordAlpha for code and debugging.
                    // Get the compressed message and inflate it
                    StringBuilder builder = new StringBuilder();
                    Inflater decompresser = new Inflater();
                    byte[] bytes = message.array();
                    decompresser.setInput(bytes, 0, bytes.length);
                    byte[] result = new byte[128];
                    while (!decompresser.finished())
                    {
                        int resultLength = decompresser.inflate(result);
                        builder.append(new String(result, 0, resultLength, StandardCharsets.UTF_8));
                    }
                    decompresser.end();
                    
                    // Send the inflated message to the TextMessage method
                    onMessage(builder.toString());
                }
                else
                {
                    String msg = new String(message.array(), StandardCharsets.UTF_8);
                    onMessage(msg);
                }
            }
            catch (Exception e)
            {
                error(e.toString());
                e.printStackTrace();
            }
        }
        
        @Override
        public void onClose(int code, String reason, boolean remote) {
            log("Disconnected");
            if (code != CloseCode.AUTHENTICATION_FAILED &&
                code != CloseCode.INVALID_SHARD &&
                code != CloseCode.SHARDING_REQUIRED &&
                running)
            {
                // No reconnecting for now
                log("Shutting down");
                close();
            }
            else
            {
                log("Shutting down");
                close();
            }
        }
        
        @Override
        public void onMessage(String message)
        {
            try
            {
                JsonObject obj = gson.fromJson(message, JsonObject.class);
                // op	integer	                Gateway opcode, which indicates the payload type
                // d	?mixed (any JSON value) Event data
                // s	?integer *	            Sequence number of event used for resuming sessions and heartbeating
                // t	?string *	            Event name
                //     * null when op is not 0 (Gateway Dispatch opcode).
                
                int op = obj.get("op").getAsInt();
                
                switch (op)
                {
                    case Opcode.EVENT:
                    {
                        sequence = obj.has("s") ? obj.get("s").getAsInt() : null;
                        String name = obj.get("t").getAsString();
                        
                        switch (name)
                        {
                            case "READY":
                            {
                                loggedIn = true;
                                JsonObject event = obj.get("d").getAsJsonObject();
                                JsonObject user = event.get("user").getAsJsonObject();
                                username = user.get("username").getAsString() + "#" + user.get("discriminator").getAsString();
                                sessionId = event.get("session_id").getAsString();
                                clientUserId = user.get("id").getAsLong();
                                log("Connected as " + username);
                                break;
                            }
                            case "MESSAGE_CREATE":
                            {
                                JsonObject event = obj.get("d").getAsJsonObject();
                                messageCreated(event);
                                break;
                            }
                            default:
                            {
                                otherEventReceived(obj.get("d"));
                                break;
                            }
                        }
                        break;
                    }
                    case Opcode.INVALID_SESSION:
                    {
                        loggedIn = false;
                        login();
                    }
                    case Opcode.HELLO:
                    {
                        loggedIn = true;
                        JsonObject event = obj.get("d").getAsJsonObject();
                        int interval = event.get("heartbeat_interval").getAsInt() - 1000 + new Random().nextInt(1000);
                        heartbeatThread = new Thread(() ->
                        {
                            StringBuilder sb = new StringBuilder();
                            while (true)
                            {
                                try
                                {
                                    sb.setLength(0);
                                    sb.append("{\"op\":").append(Opcode.HEARTBEAT)
                                        .append(",\"d\":").append(sequence).append("}");
                                    this.send(sb.toString());
                                    Thread.sleep(interval);
                                }
                                catch (InterruptedException e)
                                {
                                    if (!running)
                                        break;
                                }
                            }
                        });
                        heartbeatThread.start();
                    }
                }
            }
            catch (Exception e)
            {
                error("Msg: " + message);
                error(e.toString());
                e.printStackTrace();
            }
        }
        
        @Override
        public void onError(Exception e)
        {
            error("Internal error: " + e.toString());
            e.printStackTrace();
            close();
        }
        
        // Stops the socket
        @Override
        public void close()
        {
            running = false;
            if (heartbeatThread != null)
            {
                heartbeatThread.interrupt();
                try {
                    heartbeatThread.join();
                } catch (InterruptedException ignored) {
                }
                heartbeatThread = null;
            }
            super.close();
        }
    }
    
    public static class JsonObjectBuilder
    {
        private final JsonObject obj = new JsonObject();
        
        private JsonObjectBuilder() { }
        
        public JsonObjectBuilder put(String k, String v)
        {
            obj.addProperty(k, v);
            return this;
        }
        
        public JsonObjectBuilder put(String k, Number v)
        {
            obj.addProperty(k, v);
            return this;
        }
        
        public JsonObjectBuilder put(String k, Boolean v)
        {
            obj.addProperty(k, v);
            return this;
        }
        
        public JsonObjectBuilder put(String k, Character v)
        {
            obj.addProperty(k, v);
            return this;
        }
        
        public JsonObjectBuilder put(String k, JsonObjectBuilder v)
        {
            obj.add(k, v.getObj());
            return this;
        }
        
        public JsonObjectBuilder put(String k, JsonObject v)
        {
            obj.add(k, v);
            return this;
        }
        
        // Accepts an array containing the above types
        public JsonObjectBuilder putArray(String k, Object... array)
        {
            JsonArray jsonArray = new JsonArray();
            for (Object o : array)
            {
                if (o instanceof String)
                    jsonArray.add(new JsonPrimitive((String) o));
                else if (o instanceof Number)
                    jsonArray.add(new JsonPrimitive((Number) o));
                else if (o instanceof Boolean)
                    jsonArray.add(new JsonPrimitive((Boolean) o));
                else if (o instanceof Character)
                    jsonArray.add(new JsonPrimitive((Character) o));
                else if (o instanceof JsonObjectBuilder)
                    jsonArray.add(((JsonObjectBuilder) o).getObj());
                else if (o instanceof JsonObject)
                    jsonArray.add((JsonObject) o);
                else
                    throw new RuntimeException("Array contains non json object");
            }
            obj.add(k, jsonArray);
            return this;
        }
        
        public JsonObject getObj()
        {
            return obj;
        }
    }
    
    public static class RateLimitException extends Exception
    {
        public final HttpURLConnection con;
        
        public RateLimitException(HttpURLConnection con)
        {
            super("Ratelimited");
            this.con = con;
        }
    }
    
    public static class HttpException extends Exception
    {
        public final HttpURLConnection con;
        
        public HttpException(HttpURLConnection con, String response)
        {
            super(response);
            this.con = con;
        }
    }
}