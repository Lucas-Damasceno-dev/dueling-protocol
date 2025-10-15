package model;

/**
 * Defines the thematic resource types for different character classes.
 * Each resource has a display name and an associated color for the UI.
 */
public enum ResourceType {
    MANA("Mana", "#00A9FF"),
    FURIA("Fúria", "#D80032"),
    ENERGIA("Energia", "#F7E987"),
    FOCO("Foco", "#A8DF8E");

    public final String displayName;
    public final String colorHex;

    ResourceType(String displayName, String colorHex) {
        this.displayName = displayName;
        this.colorHex = colorHex;
    }
}
