package xzot1k.plugins.ds.api.eco;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xzot1k.plugins.ds.DisplayShops;

import java.util.UUID;

public abstract class EcoHook {
    private final String currencyName;
    private String altName, symbol, format;
    private int decimalPlacement;
    private boolean rawPlaceholderValue, useFormat;

    public EcoHook(@NotNull String currencyName, @Nullable EcoHandler... ecoHandler) {
        this.currencyName = currencyName;
        setAltName(null);
        setSymbol("$");
        setDecimalPlacement(2);
        setRawPlaceholderValue(false);
        setUseFormat(false);
        setFormat(null);
        loadAndRegister((ecoHandler != null && ecoHandler.length > 0 && ecoHandler[0] != null)
                ? ecoHandler[0] : DisplayShops.getPluginInstance().getEconomyHandler());
    }

    private void loadAndRegister(@NotNull EcoHandler ecoHandler) {
        ecoHandler.loadExtraData(getCurrencyName(), this);
        ecoHandler.getEconomyRegistry().put(getCurrencyName(), this);
    }

    public abstract String getSingularName();

    public abstract String getPluralName();

    public abstract boolean deposit(@NotNull UUID playerUniqueId, double amount);

    public abstract boolean deposit(@NotNull OfflinePlayer player, double amount);

    public abstract boolean withdraw(@NotNull UUID playerUniqueId, double amount);

    public abstract boolean withdraw(@NotNull OfflinePlayer player, double amount);

    public abstract double getBalance(@NotNull UUID playerUniqueId);

    public abstract double getBalance(@NotNull OfflinePlayer player);

    /**
     * @return The economy name based on available options.
     */
    public String getName() {
        return ((getAltName() != null && !getAltName().isEmpty()) ? getAltName()
                : ((getPluralName() != null && !getPluralName().isEmpty()) ? getPluralName() : getSingularName()))
                .replace("{symbol}", (getSymbol() != null ? getSymbol() : ""));
    }

    public String getAltName() {
        return altName;
    }

    public void setAltName(String altName) {
        this.altName = altName;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public int getDecimalPlacement() {
        return decimalPlacement;
    }

    public void setDecimalPlacement(int decimalPlacement) {
        this.decimalPlacement = decimalPlacement;
    }

    public boolean isRawPlaceholderValue() {
        return rawPlaceholderValue;
    }

    public void setRawPlaceholderValue(boolean rawPlaceholderValue) {
        this.rawPlaceholderValue = rawPlaceholderValue;
    }

    public String getCurrencyName() {
        return currencyName;
    }

    public boolean useFormat() {
        return useFormat;
    }

    public void setUseFormat(boolean useFormat) {
        this.useFormat = useFormat;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}
