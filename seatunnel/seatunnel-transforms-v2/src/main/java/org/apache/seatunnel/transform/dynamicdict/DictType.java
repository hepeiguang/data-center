package org.apache.seatunnel.transform.dynamicdict;
import lombok.Getter;

@Getter
public enum DictType {
    NAMESPACE("NAMESPACE", "NAMESPACE"),
    RANGE("RANGE", "RANGE"),
    NUMACCURACY("NUMACCURACY", "NUMACCURACY"),
    TIMESTAMP("TIMESTAMP", "TIMESTAMP");

    private final String code;
    private final String name;
    DictType(String code, String name) {
        this.code = code;
        this.name = name;

    }
    public static DictType fromName(String name) {
        for (DictType dictType : DictType.values()) {
            if (dictType.getName().equals(name)) {
                return dictType;
            }
        }
        throw new IllegalArgumentException("Unknown DictType name: " + name);
    }
}
