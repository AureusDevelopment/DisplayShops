/*
 * Copyright (c) 2021 XZot1K, All rights reserved.
 */

package xzot1k.plugins.ds.api.objects;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xzot1k.plugins.ds.DisplayShops;
import xzot1k.plugins.ds.api.enums.EconomyCallType;
import xzot1k.plugins.ds.api.events.EconomyCallEvent;
import xzot1k.plugins.ds.api.events.MarketRegionRentEvent;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class MRegion implements MarketRegion {
    private final DisplayShops pluginInstance;
    private String marketId, rentedTimeStamp;
    private Region region;
    private UUID renter;
    private int extendedDuration;
    private BigDecimal cost, renewCost;

    public MRegion(DisplayShops pluginInstance, String marketId, Region region) {
        this.pluginInstance = pluginInstance;
        setMarketId(marketId);
        setRegion(region);
        setExtendedDuration(0);
        setRenter(null);
        setRentedTimeStamp(null);
        setCost(getPluginInstance().getConfig().getDouble("rent-cost"));
        setRenewCost(getPluginInstance().getConfig().getDouble("rent-renew-cost"));
    }

    @Override
    public void reset() {
        getPluginInstance().getManager().getShopMap().entrySet().parallelStream().forEach(entry -> {
            final Shop shop = entry.getValue();
            if (shop != null && isInRegion(shop.getBaseLocation())) {
                getPluginInstance().getServer().getScheduler().runTask(getPluginInstance(), () -> {
                    shop.dropStock();
                    shop.returnBalance();
                    resetHelper(shop);
                });
            }
        });

        getPluginInstance().getServer().getScheduler().runTask(getPluginInstance(), () -> {
            Player renter = this.getPluginInstance().getServer().getPlayer(getRenter());
            if (renter != null) getPluginInstance().getManager().sendMessage(renter,
                    Objects.requireNonNull(getPluginInstance().getLangConfig().getString("rent-expired"))
                            .replace("{id}", StringUtils.capitalize(this.getMarketId())));

            setRenter(null);
            setRentedTimeStamp(null);
            setExtendedDuration(0);

            getPluginInstance().getServer().getOnlinePlayers().parallelStream().forEach(player -> getPluginInstance().clearDisplayPackets(player));
        });
    }

    @Override
    public void resetHelper(@NotNull Shop shop) {
        shop.reset();
        shop.setOwnerUniqueId(UUID.randomUUID());
    }

    @Override
    public void updateRentedTimeStamp() {
        setRentedTimeStamp(getPluginInstance().getDateFormat().format(new Date(System.currentTimeMillis())));
    }

    @Override
    public boolean extendRent(@NotNull Player player) {
        final EconomyCallEvent economyCallEvent = EconomyCallEvent.call(player, null, EconomyCallType.RENT_RENEW, getRenewCost());
        if (economyCallEvent.failed()) return false;

        MarketRegionRentEvent rentEvent = new MarketRegionRentEvent(player, this, true);
        getPluginInstance().getServer().getPluginManager().callEvent(rentEvent);
        if (rentEvent.isCancelled()) return true;

        setExtendedDuration(getExtendedDuration() + getPluginInstance().getConfig().getInt("rent-extend-duration"));
        return true;
    }

    @Override
    public boolean rent(@NotNull Player player) {
        final EconomyCallEvent economyCallEvent = EconomyCallEvent.call(player, null, EconomyCallType.RENT, ((timeUntilExpire() > 0) ? getRenewCost() : getCost()));
        if (economyCallEvent.failed()) return false;

        MarketRegionRentEvent rentEvent = new MarketRegionRentEvent(player, this, false);
        getPluginInstance().getServer().getPluginManager().callEvent(rentEvent);
        if (rentEvent.isCancelled()) return true;

        updateRentedTimeStamp();
        setRenter(player.getUniqueId());
        getPluginInstance().getManager().getShopMap().entrySet().parallelStream().forEach(entry -> {
            final Shop shop = entry.getValue();
            if (shop != null && shop.getBaseLocation() != null && isInRegion(shop.getBaseLocation())) {
                shop.setOwnerUniqueId(player.getUniqueId());
                shop.setStock(0);
                shop.setStoredBalance(0.0);
            }
        });

        return true;
    }

    @Override
    public int timeUntilExpire() {
        int timeLeft = 0;
        if (getRentedTimeStamp() != null && !getRentedTimeStamp().isEmpty()) {
            try {
                Date rentedTime = getPluginInstance().getDateFormat().parse(getRentedTimeStamp());
                Date currentDate = new Date(System.currentTimeMillis());
                timeLeft = (int) ((long) (getPluginInstance().getConfig().getInt("rent-expire-duration")
                        + getExtendedDuration()) - (currentDate.getTime() - rentedTime.getTime()) / 1000L);
            } catch (Exception ignored) {}
        }
        return timeLeft;
    }

    @Override
    public String formattedTimeUntilExpire() {
        StringBuilder stringBuilder = new StringBuilder();
        long seconds = this.timeUntilExpire(),
                days = TimeUnit.SECONDS.toDays(seconds),
                hours = TimeUnit.SECONDS.toHours(seconds -= TimeUnit.DAYS.toSeconds(days)),
                minutes = TimeUnit.SECONDS.toMinutes(seconds -= TimeUnit.HOURS.toSeconds(hours));
        seconds -= TimeUnit.MINUTES.toSeconds(minutes);

        final boolean pluralize = getPluginInstance().getLangConfig().getBoolean("duration-format.pluralize");
        final String pluralizeLettering = getPluginInstance().getLangConfig().getString("duration-format.plural-lettering");

        if (days > 0L) {
            stringBuilder.append(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("duration-format.days"))
                    .replace("{amount}", getPluginInstance().getManager().formatNumber(days, false))
                    .replace("{plural}", ((days > 1L && pluralize) ? Objects.requireNonNull(pluralizeLettering) : "")));
        }

        if (hours > 0L) {
            if (days > 0L) stringBuilder.append(" ");
            stringBuilder.append(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("duration-format.hours"))
                    .replace("{amount}", getPluginInstance().getManager().formatNumber(hours, false))
                    .replace("{plural}", ((hours > 1L && pluralize) ? Objects.requireNonNull(pluralizeLettering) : "")));
        }

        if (minutes > 0L) {
            if (hours > 0L || days > 0L) stringBuilder.append(" ");
            stringBuilder.append(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("duration-format.minutes"))
                    .replace("{amount}", getPluginInstance().getManager().formatNumber(minutes, false))
                    .replace("{plural}", ((minutes > 1L && pluralize) ? Objects.requireNonNull(pluralizeLettering) : "")));
        }

        if (seconds > 0L) {
            if (minutes > 0L || hours > 0L || days > 0L) stringBuilder.append(" ");
            stringBuilder.append(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("duration-format.seconds"))
                    .replace("{amount}", getPluginInstance().getManager().formatNumber(seconds, false))
                    .replace("{plural}", ((seconds > 1L && pluralize) ? Objects.requireNonNull(pluralizeLettering) : "")));
        }

        return stringBuilder.toString();
    }

    @Override
    public synchronized void delete(boolean async) {
        if (async) {
            DisplayShops.getPluginInstance().getServer().getScheduler().runTaskAsynchronously(DisplayShops.getPluginInstance(), (Runnable) this::delete);
        } else delete();
    }

    private synchronized void delete() {
        try {
            PreparedStatement preparedStatement = DisplayShops.getPluginInstance().getDatabaseConnection()
                    .prepareStatement("delete from market_regions where id = '" + this.getMarketId() + "'");
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
            DisplayShops.getPluginInstance().log(Level.WARNING, "There was an issue deleting the market region "
                    + this.getMarketId() + " from the database (" + e.getMessage() + ").");
        }
    }

    @Override
    public synchronized void save(boolean async) {
        if (async) {
            DisplayShops.getPluginInstance().getServer().getScheduler().runTaskAsynchronously(DisplayShops.getPluginInstance(), (Runnable) this::save);
        } else save();
    }

    private synchronized void save() {
        try {
            final String pointOneString = (getRegion().getPointOne().getWorldName() + "," + getRegion().getPointOne().getX() + "," + getRegion().getPointOne().getY()
                    + "," + getRegion().getPointOne().getZ() + "," + getRegion().getPointOne().getYaw() + "," + getRegion().getPointOne().getPitch()),
                    pointTwoString = (getRegion().getPointTwo().getWorldName() + "," + getRegion().getPointTwo().getX() + "," + getRegion().getPointTwo().getY()
                            + "," + getRegion().getPointTwo().getZ() + "," + getRegion().getPointTwo().getYaw() + "," + getRegion().getPointTwo().getPitch());

            final String extraDataLine = (getCost() + "," + getRenewCost()),
                    host = getPluginInstance().getConfig().getString("mysql.host"), syntax,
                    renterId = (getRenter() != null ? getRenter().toString() : "");
            if (host == null || host.isEmpty())
                syntax = "INSERT OR REPLACE INTO market_regions(id, point_one, point_two, renter, extended_duration, rent_time_stamp, " +
                        "extra_data) VALUES('" + getMarketId() + "', '" + pointOneString.replace("'", "\\'")
                        .replace("\"", "\\\"") + "', '" + pointTwoString.replace("'", "\\'")
                        .replace("\"", "\\\"") + "', '" + renterId + "', '" + getExtendedDuration() + "', '" + getRentedTimeStamp() + "', '" + extraDataLine + "');";
            else
                syntax = "INSERT INTO market_regions(id, point_one, point_two, renter, extended_duration, rent_time_stamp, extra_data) VALUES( '"
                        + getMarketId() + "', '" + pointOneString.replace("'", "\\'").replace("\"", "\\\"") + "', '"
                        + pointTwoString.replace("'", "\\'").replace("\"", "\\\"") + "', '" + renterId + "', '" + getExtendedDuration()
                        + "', '" + getRentedTimeStamp() + "') ON DUPLICATE KEY UPDATE id = '" + getMarketId() + "'," + " point_one = '" + pointOneString.replace("'", "\\'")
                        .replace("\"", "\\\"") + "', point_two = '" + pointTwoString.replace("'", "\\'").replace("\"", "\\\"")
                        + "', renter = '" + renterId + "', extended_duration = '" + getExtendedDuration() + "', rent_time_stamp = '" + getRentedTimeStamp()
                        + "', extra_data = '" + extraDataLine + "';";

            PreparedStatement preparedStatement = getPluginInstance().getDatabaseConnection().prepareStatement(syntax);
            preparedStatement.execute();
            preparedStatement.close();
        } catch (Exception e) {
            e.printStackTrace();
            getPluginInstance().log(Level.WARNING, "There was an issue saving the market region '" + getMarketId() + "' (" + e.getMessage() + ").");
        }
    }

    @Override
    public boolean isInRegion(@NotNull Location location) {
        if (this.getRegion().getPointOne() == null || this.getRegion().getPointTwo() == null) return false;

        //System.out.println(location.getX() + "," +location.getY() + "," + location.getZ() + " - " + getRegion().getPointOne().getX() + "-" + getRegion().getPointTwo().getX()
        //+ " | "+ getRegion().getPointOne().getY() + "-" + getRegion().getPointTwo().getY() + "-" + getRegion().getPointOne().getZ() + "-" + getRegion().getPointTwo().getZ());

        return (location.getWorld() != null
                && (location.getWorld().getName().equalsIgnoreCase(this.getRegion().getPointOne().getWorldName())

                && (location.getX() >= this.getRegion().getPointOne().getX() && location.getX() <= this.getRegion().getPointTwo().getX()
                || location.getX() <= this.getRegion().getPointOne().getX() && location.getX() >= this.getRegion().getPointTwo().getX())

                && (location.getY() >= this.getRegion().getPointOne().getY() && location.getY() <= this.getRegion().getPointTwo().getY()
                || location.getY() <= this.getRegion().getPointOne().getY() && location.getY() >= this.getRegion().getPointTwo().getY())

                && (location.getZ() >= this.getRegion().getPointOne().getZ() && location.getZ() <= this.getRegion().getPointTwo().getZ()
                || location.getZ() <= this.getRegion().getPointOne().getZ() && location.getZ() >= this.getRegion().getPointTwo().getZ())));
    }

    @Override
    public boolean isInRegion(@NotNull LocationClone location) {
        if (this.getRegion().getPointOne() == null || this.getRegion().getPointTwo() == null) return false;

        return location.getWorldName().equalsIgnoreCase(this.getRegion().getPointOne().getWorldName())
                && (location.getX() >= this.getRegion().getPointOne().getX() && location.getX() <= this.getRegion().getPointTwo().getX()

                || location.getX() <= this.getRegion().getPointOne().getX() && location.getX() >= this.getRegion().getPointTwo().getX())
                && (location.getY() >= this.getRegion().getPointOne().getY() && location.getY() <= this.getRegion().getPointTwo().getY()

                || location.getY() <= this.getRegion().getPointOne().getY() && location.getY() >= this.getRegion().getPointTwo().getY())

                && (location.getZ() >= this.getRegion().getPointOne().getZ() && location.getZ() <= this.getRegion().getPointTwo().getZ()
                || location.getZ() <= this.getRegion().getPointOne().getZ() && location.getZ() >= this.getRegion().getPointTwo().getZ());
    }

    @Override
    public String getMarketId() {
        return this.marketId;
    }

    private void setMarketId(String marketId) {
        this.marketId = marketId;
    }

    @Override
    public Region getRegion() {
        return this.region;
    }

    private void setRegion(Region region) {
        this.region = region;
    }

    @Override
    public UUID getRenter() {
        return this.renter;
    }

    @Override
    public void setRenter(@Nullable UUID renter) {
        this.renter = renter;
    }

    @Override
    public String getRentedTimeStamp() {
        return this.rentedTimeStamp;
    }

    @Override
    public void setRentedTimeStamp(@Nullable String rentedTimeStamp) {
        this.rentedTimeStamp = rentedTimeStamp;
    }

    @Override
    public int getExtendedDuration() {
        return this.extendedDuration;
    }

    @Override
    public void setExtendedDuration(int extendedDuration) {
        this.extendedDuration = extendedDuration;
    }

    private DisplayShops getPluginInstance() {
        return this.pluginInstance;
    }

    public double getCost() {
        return cost.doubleValue();
    }

    public void setCost(double cost) {
        this.cost = new BigDecimal(cost);
    }

    public double getRenewCost() {
        return renewCost.doubleValue();
    }

    public void setRenewCost(double renewCost) {
        this.renewCost = new BigDecimal(renewCost);
    }

}