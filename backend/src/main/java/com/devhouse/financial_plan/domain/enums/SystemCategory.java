package com.devhouse.financial_plan.domain.enums;

public enum SystemCategory {

    CREDIT_CARD_INVOICE_PAYMENT("Pagamento de Fatura", "Fatura de Cartão"),
    TRANSFER("Transferência", null);

    private final String categoryName;
    private final String subCategoryName;

    SystemCategory(String categoryName, String subCategoryName) {
        this.categoryName = categoryName;
        this.subCategoryName = subCategoryName;
    }

    public static SystemCategory fromCategoryName(String name) {
        if (name == null) {
            return null;
        }
        for (SystemCategory systemCategory : values()) {
            if (systemCategory.categoryName.equalsIgnoreCase(name.trim())) {
                return systemCategory;
            }
        }
        return null;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getSubCategoryName() {
        return subCategoryName;
    }

    public boolean hasSubCategory() {
        return subCategoryName != null;
    }
}
