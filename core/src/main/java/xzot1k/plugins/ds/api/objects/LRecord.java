package xzot1k.plugins.ds.api.objects;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class LRecord implements LogRecord {
    // timestamp, shop_id, player_id, action, location, value
    private long timestamp;
    private UUID shopId, playerId;
    private String action, value;

    public LRecord(long timestamp, @NotNull UUID shopId, @NotNull UUID playerId, @NotNull String action, @NotNull String value) {
        setTimestamp(timestamp);
        setShopId(shopId);
        setPlayerId(playerId);
        setAction(action);
        setValue(value);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public UUID getShopId() {
        return shopId;
    }

    public void setShopId(UUID shopId) {
        this.shopId = shopId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public void setPlayerId(UUID playerId) {
        this.playerId = playerId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}