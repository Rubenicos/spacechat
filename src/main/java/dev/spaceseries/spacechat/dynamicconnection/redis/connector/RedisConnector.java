package dev.spaceseries.spacechat.dynamicconnection.redis.connector;

import dev.spaceseries.spaceapi.lib.redis.jedis.Jedis;
import dev.spaceseries.spaceapi.lib.redis.jedis.JedisPubSub;
import dev.spaceseries.spacechat.SpaceChat;
import dev.spaceseries.spacechat.configuration.Config;
import dev.spaceseries.spacechat.dynamicconnection.redis.supervisor.RedisSupervisor;
import org.bukkit.Bukkit;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;

import static dev.spaceseries.spacechat.configuration.Config.*;

public class RedisConnector extends JedisPubSub {

    /**
     * Redis client
     */
    private Jedis client;

    /**
     * The supervisor
     */
    private final RedisSupervisor supervisor;

    /**
     * Construct redis connector
     */
    public RedisConnector(RedisSupervisor supervisor) {
        this.supervisor = supervisor;

        try {
            client = new Jedis(new URI(REDIS_URL.get(Config.get())));
        } catch (URISyntaxException e) {
            e.printStackTrace();
            client = null;
            return;
        }

        // subscribing to redis pub/sub is a blocking operation.
        // we need to make a new thread in order to not block the main thread....
        new Thread(() -> {
            // subscribe this class to chat channel
            client.subscribe(this, REDIS_CHAT_CHANNEL.get(Config.get()));
        });
    }

    /**
     * Shuts down the client
     */
    public void shutdown() {
        client.close();
    }

    @Override
    public void onMessage(String channel, String message) {
        // receiving
        // [channel] sent [message]
        if (channel.equalsIgnoreCase(REDIS_CHAT_CHANNEL.get(Config.get())))
            this.supervisor.receiveChatMessage(message);
    }

    @Override
    public void onSubscribe(String channel, int subscribedChannels) {
        // we have subscribed to [channel]. We are currently subscribed to [subscribedChannels] channels.
        //TODO logging in console
        SpaceChat.getInstance().getLogger().log(Level.INFO, "SpaceChat subscribed to the redis channel '" + channel + "'");
    }

    @Override
    public void onUnsubscribe(String channel, int subscribedChannels) {
        // we have unsubscribed from [channel]. We are currently subscribed to another [subscribedChannels] channels.
        //TODO logging in console
        SpaceChat.getInstance().getLogger().log(Level.INFO, "SpaceChat unsubscribed from the redis channel '" + channel + "'");
    }

    /**
     * Publish a message
     *
     * @param channel channel
     * @param message message
     */
    public void publish(String channel, String message) {
        // run async
        Bukkit.getScheduler().runTaskAsynchronously(SpaceChat.getInstance(), () -> {
            client.publish(channel, message);
        });
    }
}
