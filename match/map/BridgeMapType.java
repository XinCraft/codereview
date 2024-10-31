package net.xincraft.systems.match.map;

/**
 * Represents the type a bridge map is.
 * <p>
 * Menus in BridgeMapSelector are based on the ordinal value of these enums meaning COMPETITIVE is
 * the first menu, CASUAL is the second, and CUSTOM is the third.
 */
public enum BridgeMapType {
    COMPETITIVE,
    CASUAL,
    CUSTOM,
    ;

    public String formatted() {
        return name().substring(0, 1).toUpperCase() + name().substring(1).toLowerCase();
    }
}
