package org.example;

import java.math.BigDecimal;

public class Material {
    private final int id;
    private final String materialName;
    private final BigDecimal unitCost; // 👈 This fixes the unaccessed/local variable warnings

    public Material(int id, String materialName, BigDecimal unitCost) {
        this.id = id;
        this.materialName = materialName;
        this.unitCost = unitCost;
    }

    public int getId() {
        return id;
    }

    public String getMaterialName() {
        return materialName;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }
}