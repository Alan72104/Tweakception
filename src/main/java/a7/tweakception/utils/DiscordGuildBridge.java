package a7.tweakception.utils;

import a7.tweakception.Tweakception;
import com.google.gson.JsonObject;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static a7.tweakception.tweaks.GlobalTweaks.getTicks;
import static a7.tweakception.tweaks.GlobalTweaks.isInHypixel;
import static a7.tweakception.utils.McUtils.*;

public class DiscordGuildBridge
{
    private static final String[] LIST;
    private static final Pattern PATTERN;
    private static final int DEF_LEVENSHTEIN_THRESHOLD = 5;
    private static final String SAME_MESSAGE_WARNING = "You cannot say the same message twice!";
    private final Matcher matcher = PATTERN.matcher("");
    private MinimalDiscordClient client = null;
    private long bridgingChannelId = 0;
    private final LevenshteinDistance levenshteinDistance = LevenshteinDistance.getDefaultInstance();
    // All should be in lower case
    private final ConcurrentHashMap<String, Integer> recentMsgs = new ConcurrentHashMap<>();
    private int levenshteinThreshold = DEF_LEVENSHTEIN_THRESHOLD;
    private final Matcher guildChatHeaderMatcher = Pattern.compile(
        "^(?<rank>\\[[^]]+?] )?(?<name>[A-Za-z0-9_]{1,16})(?<guildRank> \\[[^]]+?])?$").matcher("");
    
    public DiscordGuildBridge()
    {
    }
    
    public void onTick(TickEvent event)
    {
        if (event.phase == TickEvent.Phase.END)
        {
            if (!recentMsgs.isEmpty())
            {
                for (Map.Entry<String, Integer> entry : recentMsgs.entrySet())
                {
                    if (getTicks() - entry.getValue() >= 20 * 60 * 5)
                    {
                        recentMsgs.remove(entry.getKey());
                    }
                }
            }
        }
    }
    
    public void onChatReceivedGlobal(ClientChatReceivedEvent event)
    {
        if (event.type == 0 && client != null && client.isLoggedIn())
        {
            String msg = McUtils.cleanColor(event.message.getUnformattedText());
            if (msg.startsWith("Guild > "))
            {
                msg = msg.substring(8);
                // Assumes tht other "Guild > " messages don't contain a :
                int colon = msg.indexOf(':');
                if (colon > -1) // Guild chat
                {
                    if (guildChatHeaderMatcher.reset(msg.substring(0, colon)).matches())
                    {
                        String name = guildChatHeaderMatcher.group("name");
                        // Of course, when not nicked
                        boolean fromSelfBridge = name.equalsIgnoreCase(getPlayer().getName()) &&
                            msg.substring(colon + 2).startsWith("> ");
                        if (!fromSelfBridge)
                            sendToDiscord("> "+msg);
                    }
                }
                else if (msg.endsWith(" joined."))
                {
                    sendToDiscord("> "+msg);
                }
                else if (msg.endsWith(" left."))
                {
                    sendToDiscord("> "+msg);
                }
            }
        }
    }
    
    private void sendToGuild(String header, String msg)
    {
        if (isInHypixel() && !recentMsgs.containsKey(msg))
        {
            recentMsgs.put(msg.toLowerCase(), getTicks());
            McUtils.executeCommand("/gc " + header + msg);
        }
    }
    
    private void sendToDiscord(String msg)
    {
        if (client != null && client.isLoggedIn())
        {
            client.createMessage(bridgingChannelId, msg);
        }
    }
    
    private String sanitize(String msg)
    {
        return matcher.reset(msg).replaceAll("REDACTED");
    }
    
    // Do not pass the wrong ids
    public void connect(long bridgingChannelId)
    {
        String token;
        String clipboard = Utils.getClipboard();
        if (clipboard == null)
        {
            sendChat("GB: Clipboard does not contain a string");
            return;
        }
        try
        {
            String[] split = clipboard.split("\\.", 2);
            String decodedElement = new String(Base64.getDecoder().decode(split[0]), StandardCharsets.UTF_8);
            if (!decodedElement.matches("^\\d{17,19}$"))
            {
                sendChat("GB: Clipboard content is wrong");
                return;
            }
            token = clipboard;
        }
        catch (IllegalArgumentException e)
        {
            sendChat("GB: Clipboard content is wrong");
            return;
        }
        
        if (client != null)
        {
            sendChat("GB: Client is here");
            return;
        }
        this.bridgingChannelId = bridgingChannelId;
        client = new MinimalDiscordClient()
            .connectWithToken(token)
            .onError(s -> Tweakception.scheduler.add(() -> sendChat("GB-error: " + s)))
            .onLog(s -> Tweakception.scheduler.add(() -> sendChat("GB-log: " + s)))
            .onMessageCreated(msg ->
            {
                if (!(msg.has("guild_id") && msg.has("member")))
                    return;
                JsonObject user = msg.get("author").getAsJsonObject();
                long channelId = msg.get("channel_id").getAsLong();
                long userId = user.get("id").getAsLong();
                boolean bot = user.has("bot");
                boolean system = user.has("system");
                String content = msg.get("content").getAsString();
                // Check the correct channel, not from self, not from bot/system, content isn't empty
                if (channelId != bridgingChannelId)
                    return;
                if (userId == client.getClientUserId() &&
                    (content.startsWith("> ") || content.equals(SAME_MESSAGE_WARNING)))
                    return;
                if (bot || system || content.isEmpty())
                    return;
    
                JsonObject member = msg.get("member").getAsJsonObject();
                long guildId = msg.get("guild_id").getAsLong();
                String username = user.get("username").getAsString();
                String nick = member.has("nick") ? member.get("nick").isJsonNull() ? null : member.get("nick").getAsString() : null;
                
                // Final output should be: "> name: content"
                String displayName = sanitize(nick == null ? username : nick);
                String sanitizedContent = sanitize(content);
                String displayContent = sanitizedContent.substring(0,
                    Math.min(100 - 2 - displayName.length() - 2, sanitizedContent.length()));
                String normalized = displayContent.toLowerCase();
                boolean spam = false;
                if (recentMsgs.containsKey(normalized))
                    spam = true;
                else
                {
                    for (String recentMsg : recentMsgs.keySet())
                    {
                        int dist = levenshteinDistance.apply(normalized, recentMsg);
                        if (dist <= 5)
                        {
                            spam = true;
                            break;
                        }
                    }
                }
                
                if (spam)
                {
                    client.createReply(channelId, SAME_MESSAGE_WARNING, guildId, msg.get("id").getAsLong());
                    return;
                }
                
                Tweakception.scheduler.add(() -> sendToGuild("> "+displayName+": ",displayContent));
            });
    }
    
    public void disconnect()
    {
        if (client == null)
        {
            sendChat("GB: Where is client");
            return;
        }
        
        client.close();
        client = null;
    }
    
    public void disconnectNow()
    {
        if (client == null)
            return;
        
        client.close();
        client = null;
    }
    
    public void status()
    {
        if (client == null)
        {
            sendChat("GB: Where is client");
            return;
        }
        boolean running = client.isRunning();
        boolean on = client.isLoggedIn();
        sendChatf("GB: Client is %s, %s", running ? "running" : "not running", on ? "online" : "offline");
    }
    
    public void setLevenshteinThreshold(int t)
    {
        levenshteinThreshold = t == -1 ? DEF_LEVENSHTEIN_THRESHOLD : Utils.clamp(t, 0, 100);
        sendChat("GB: Set levenshtein threshold to " + levenshteinThreshold);
    }
    
    static
    {
        @SuppressWarnings("SpellCheckingInspection")
        String s =
            "MmcxYwoyIGdpcmxzIDEgY3VwCmFjcm90b21vcGhpbGlhCmFsYWJhbWEgaG90IHBvY2tldAphbGFza2FuIHBpcGVsaW5lCmFuYWwK" +
            "YW5pbGluZ3VzCmFudXMKYXBlc2hpdAphcnNlaG9sZQphc3MKYXNzaG9sZQphc3NtdW5jaAphdXRvIGVyb3RpYwphdXRvZXJvdGlj" +
            "CmJhYmVsYW5kCmJhYnkgYmF0dGVyCmJhYnkganVpY2UKYmFsbCBnYWcKYmFsbCBncmF2eQpiYWxsIGtpY2tpbmcKYmFsbCBsaWNr" +
            "aW5nCmJhbGwgc2FjawpiYWxsIHN1Y2tpbmcKYmFuZ2Jyb3MKYmFyZWJhY2sKYmFyZWx5IGxlZ2FsCmJhcmVuYWtlZApiYXN0YXJk" +
            "CmJhc3RhcmRvCmJhc3RpbmFkbwpiYncKYmRzbQpiZWFuZXIKYmVhbmVycwpiZWF2ZXIgY2xlYXZlcgpiZWF2ZXIgbGlwcwpiZXN0" +
            "aWFsaXR5CmJpZyBibGFjawpiaWcgYnJlYXN0cwpiaWcga25vY2tlcnMKYmlnIHRpdHMKYmltYm9zCmJpcmRsb2NrCmJpdGNoCmJp" +
            "dGNoZXMKYmxhY2sgY29jawpibG9uZGUgYWN0aW9uCmJsb25kZSBvbiBibG9uZGUgYWN0aW9uCmJsb3dqb2IKYmxvdyBqb2IKYmxv" +
            "dyB5b3VyIGxvYWQKYmx1ZSB3YWZmbGUKYmx1bXBraW4KYm9sbG9ja3MKYm9uZGFnZQpib25lcgpib29iCmJvb2JzCmJvb3R5IGNh" +
            "bGwKYnJvd24gc2hvd2VycwpicnVuZXR0ZSBhY3Rpb24KYnVra2FrZQpidWxsZHlrZQpidWxsZXQgdmliZQpidWxsc2hpdApidW5n" +
            "IGhvbGUKYnVuZ2hvbGUKYnVzdHkKYnV0dApidXR0Y2hlZWtzCmJ1dHRob2xlCmNhbWVsIHRvZQpjYW1naXJsCmNhbXNsdXQKY2Ft" +
            "d2hvcmUKY2FycGV0IG11bmNoZXIKY2FycGV0bXVuY2hlcgpjaG9jb2xhdGUgcm9zZWJ1ZHMKY2lyY2xlamVyawpjbGV2ZWxhbmQg" +
            "c3RlYW1lcgpjbGl0CmNsaXRvcmlzCmNsb3ZlciBjbGFtcHMKY2x1c3RlcmZ1Y2sKY29jawpjb2Nrcwpjb3Byb2xhZ25pYQpjb3By" +
            "b3BoaWxpYQpjb3JuaG9sZQpjb29uCmNvb25zCmNyZWFtcGllCmN1bQpjdW1taW5nCmN1bm5pbGluZ3VzCmN1bnQKZGFya2llCmRh" +
            "dGUgcmFwZQpkYXRlcmFwZQpkZWVwIHRocm9hdApkZWVwdGhyb2F0CmRlbmRyb3BoaWxpYQpkaWNrCmRpbGRvCmRpbmdsZWJlcnJ5" +
            "CmRpbmdsZWJlcnJpZXMKZGlydHkgcGlsbG93cwpkaXJ0eSBzYW5jaGV6CmRvZ2dpZSBzdHlsZQpkb2dnaWVzdHlsZQpkb2dneSBz" +
            "dHlsZQpkb2dneXN0eWxlCmRvZyBzdHlsZQpkb2xjZXR0CmRvbWluYXRpb24KZG9taW5hdHJpeApkb21tZXMKZG9ua2V5IHB1bmNo" +
            "CmRvdWJsZSBkb25nCmRvdWJsZSBwZW5ldHJhdGlvbgpkcCBhY3Rpb24KZHJ5IGh1bXAKZHZkYQplYXQgbXkgYXNzCmVjY2hpCmVq" +
            "YWN1bGF0aW9uCmVyb3RpYwplcm90aXNtCmVzY29ydApldW51Y2gKZmFnZ290CmZlY2FsCmZlbGNoCmZlbGxhdGlvCmZlbHRjaApm" +
            "ZW1hbGUgc3F1aXJ0aW5nCmZlbWRvbQpmaWdnaW5nCmZpbmdlcmJhbmcKZmluZ2VyaW5nCmZpc3RpbmcKZm9vdCBmZXRpc2gKZm9v" +
            "dGpvYgpmcm90dGluZwpmdWNrIGJ1dHRvbnMKZnVja3RhcmRzCmZ1ZGdlIHBhY2tlcgpmdWRnZXBhY2tlcgpmdXRhbmFyaQpnYW5n" +
            "IGJhbmcKZ2F5IHNleApnZW5pdGFscwpnaWFudCBjb2NrCmdpcmwgb24KZ2lybCBvbiB0b3AKZ2lybHMgZ29uZSB3aWxkCmdvYXRj" +
            "eApnb2F0c2UKZ29kIGRhbW4KZ29ra3VuCmdvbGRlbiBzaG93ZXIKZ29vZHBvb3AKZ29vIGdpcmwKZ29yZWdhc20KZ3JvcGUKZ3Jv" +
            "dXAgc2V4Cmctc3BvdApndXJvCmhhbmQgam9iCmhhbmRqb2IKaGFyZCBjb3JlCmhhcmRjb3JlCmhlbnRhaQpob21vZXJvdGljCmhv" +
            "bmtleQpob29rZXIKaG90IGNhcmwKaG90IGNoaWNrCmhvdyB0byBraWxsCmhvdyB0byBtdXJkZXIKaHVnZSBmYXQKaHVtcGluZwpp" +
            "bmNlc3QKaW50ZXJjb3Vyc2UKamFjayBvZmYKamFpbCBiYWl0CmphaWxiYWl0CmplbGx5IGRvbnV0Cmplcmsgb2ZmCmppZ2Fib28K" +
            "amlnZ2Fib28KamlnZ2VyYm9vCmppenoKanVnZ3MKa2lrZQpraW5iYWt1CmtpbmtzdGVyCmtub2JiaW5nCmxlYXRoZXIgcmVzdHJh" +
            "aW50CmxlYXRoZXIgc3RyYWlnaHQgamFja2V0CmxlbW9uIHBhcnR5CmxvbGl0YQpsb3ZlbWFraW5nCm1ha2UgbWUgY29tZQptYWxl" +
            "IHNxdWlydGluZwptYXN0dXJiYXRlCm1lbmFnZSBhIHRyb2lzCm1pbGYKbWlzc2lvbmFyeSBwb3NpdGlvbgptb3RoZXJmdWNrZXIK" +
            "bW91bmQgb2YgdmVudXMKbXIgaGFuZHMKbXVmZiBkaXZlcgptdWZmZGl2aW5nCm5hbWJsYQpuYXdhc2hpCm5lZ3JvCm5lb25hemkK" +
            "bmlnZ2EKbmlnZ2VyCm5pZyBub2cKbmltcGhvbWFuaWEKbmlwcGxlCm5pcHBsZXMKbnVkZQpudWRpdHkKbnltcGhvCm55bXBob21h" +
            "bmlhCm9jdG9wdXNzeQpvbW9yYXNoaQpvbmUgY3VwIHR3byBnaXJscwpvbmUgZ3V5IG9uZSBqYXIKb3JnYXNtCm9yZ3kKcGFlZG9w" +
            "aGlsZQpwYWtpCnBhbnRpZXMKcGFudHkKcGVkb2JlYXIKcGVkb3BoaWxlCnBlZ2dpbmcKcGVuaXMKcGhvbmUgc2V4CnBpZWNlIG9m" +
            "IHNoaXQKcGlzc2luZwpwaXNzIHBpZwpwaXNzcGlnCnBsYXlib3kKcGxlYXN1cmUgY2hlc3QKcG9sZSBzbW9rZXIKcG9ueXBsYXkK" +
            "cG9vZgpwb29uCnBvb250YW5nCnB1bmFueQpwb29wIGNodXRlCnBvb3BjaHV0ZQpwb3JuCnBvcm5vCnBvcm5vZ3JhcGh5CnByaW5j" +
            "ZSBhbGJlcnQgcGllcmNpbmcKcHRoYwpwdWJlcwpwdXNzeQpxdWVhZgpxdWVlZgpxdWltCnJhZ2hlYWQKcmFnaW5nIGJvbmVyCnJh" +
            "cGUKcmFwaW5nCnJhcGlzdApyZWN0dW0KcmV2ZXJzZSBjb3dnaXJsCnJpbWpvYgpyaW1taW5nCnJvc3kgcGFsbQpyb3N5IHBhbG0g" +
            "YW5kIGhlciA1IHNpc3RlcnMKcnVzdHkgdHJvbWJvbmUKc2FkaXNtCnNhbnRvcnVtCnNjYXQKc2NobG9uZwpzY2lzc29yaW5nCnNl" +
            "bWVuCnNleApzZXhvCnNoYXZlZCBiZWF2ZXIKc2hhdmVkIHB1c3N5CnNoZW1hbGUKc2hpYmFyaQpzaGl0YmxpbXAKc2hpdHR5CnNo" +
            "b3RhCnNocmltcGluZwpza2VldApzbGFudGV5ZQpzbHV0CnMmbQpzbXV0CnNuYXRjaApzbm93YmFsbGluZwpzb2RvbWl6ZQpzb2Rv" +
            "bXkKc3BpYwpzcGxvb2dlCnNwbG9vZ2UgbW9vc2UKc3Bvb2dlCnNwcmVhZCBsZWdzCnNwdW5rCnN0cmFwIG9uCnN0cmFwb24Kc3Ry" +
            "YXBwYWRvCnN0cmlwIGNsdWIKc3R5bGUgZG9nZ3kKc3VpY2lkZSBnaXJscwpzdWx0cnkgd29tZW4Kc3dhc3Rpa2EKc3dpbmdlcgp0" +
            "YWludGVkIGxvdmUKdGVhIGJhZ2dpbmcKdGhyZWVzb21lCnRocm9hdGluZwp0aWVkIHVwCnRpZ2h0IHdoaXRlCnRpdAp0aXRzCnRp" +
            "dHRpZXMKdGl0dHkKdG9uZ3VlIGluIGEKdG9wbGVzcwp0b3NzZXIKdG93ZWxoZWFkCnRyYW5ueQp0cmliYWRpc20KdHViIGdpcmwK" +
            "dHViZ2lybAp0dXNoeQp0d2F0CnR3aW5rCnR3aW5raWUKdHdvIGdpcmxzIG9uZSBjdXAKdW5kcmVzc2luZwp1cHNraXJ0CnVyZXRo" +
            "cmEgcGxheQp1cm9waGlsaWEKdmFnaW5hCnZlbnVzIG1vdW5kCnZpYnJhdG9yCnZpb2xldCB3YW5kCnZvcmFyZXBoaWxpYQp2b3ll" +
            "dXIKdnVsdmEKd2Fuawp3ZXRiYWNrCndldCBkcmVhbQp3aGl0ZSBwb3dlcgp3cmFwcGluZyBtZW4Kd3JpbmtsZWQgc3RhcmZpc2gK" +
            "eWFvaQp5ZWxsb3cgc2hvd2Vycwp5aWZmeQp6b29waGlsaWEK8J+WlQ==";
        byte[] decoded = Base64.getDecoder().decode(s);
        LIST = new String(decoded, StandardCharsets.UTF_8).split("\\R");
        StringBuilder sb = new StringBuilder();
        for (String e : LIST)
            sb.append(e).append('|');
        sb.deleteCharAt(sb.length() - 1);
        PATTERN = Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE);
    }
}
