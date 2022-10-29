package kitchenpos.application.request.product;

import java.math.BigDecimal;

public class ProductRequest {

    private String name;
    private BigDecimal price;

    public ProductRequest(final String name, final BigDecimal price) {
        this.name = name;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }
}
