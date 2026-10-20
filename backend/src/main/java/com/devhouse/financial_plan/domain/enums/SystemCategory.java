package com.devhouse.financial_plan.domain.enums;

public enum SystemCategory {

    CREDIT_CARD_INVOICE_PAYMENT("Pagamento de Fatura", "Fatura de Cartão", CategoryKind.EXPENSE),
    TRANSFER("Transferência", null, null);

    private final String categoryName;
    private final String subCategoryName;
    private final CategoryKind kind;

    SystemCategory(String categoryName, String subCategoryName, CategoryKind kind) {
        this.categoryName = categoryName;
        this.subCategoryName = subCategoryName;
        this.kind = kind;
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

    public CategoryKind getKind() {
        return kind;
    }

    public boolean hasSubCategory() {
        return subCategoryName != null;
    }
}
