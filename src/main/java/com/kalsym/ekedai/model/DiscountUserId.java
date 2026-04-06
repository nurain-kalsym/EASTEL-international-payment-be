package com.kalsym.ekedai.model;

import java.io.Serializable;
import java.util.Objects;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class DiscountUserId implements Serializable {
    private String discountId;
    private String userPhoneNumber;

    // Override equals and hashCode to ensure proper comparison of composite keys
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiscountUserId that = (DiscountUserId) o;
        return Objects.equals(discountId, that.discountId) &&
               Objects.equals(userPhoneNumber, that.userPhoneNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(discountId, userPhoneNumber);
    }
}
