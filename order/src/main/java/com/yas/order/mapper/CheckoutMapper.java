package com.yas.order.mapper;

import com.yas.order.model.Checkout;
import com.yas.order.model.CheckoutItem;
import com.yas.order.viewmodel.checkout.CheckoutItemPostVm;
import com.yas.order.viewmodel.checkout.CheckoutItemVm;
import com.yas.order.viewmodel.checkout.CheckoutPostVm;
import com.yas.order.viewmodel.checkout.CheckoutVm;
import java.math.BigDecimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CheckoutMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "checkout", ignore = true)
    @Mapping(target = "productName", ignore = true)
    @Mapping(target = "productPrice", ignore = true)
    @Mapping(target = "taxAmount", ignore = true)
    @Mapping(target = "shipmentFee", ignore = true)
    @Mapping(target = "shipmentTax", ignore = true)
    @Mapping(target = "discountAmount", ignore = true)
    CheckoutItem toModel(CheckoutItemPostVm checkoutItemPostVm);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "checkoutState", ignore = true)
    @Mapping(target = "shippingAddressId", ignore = true)
    Checkout toModel(CheckoutPostVm checkoutPostVm);

    @Mapping(target = "checkoutId", source = "checkout.id")
    CheckoutItemVm toVm(CheckoutItem checkoutItem);

    @Mapping(target = "checkoutItemVms", ignore = true)
    CheckoutVm toVm(Checkout checkout);

    default BigDecimal map(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
