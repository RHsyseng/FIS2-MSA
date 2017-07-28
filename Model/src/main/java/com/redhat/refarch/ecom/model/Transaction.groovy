package com.redhat.refarch.ecom.model

import groovy.transform.EqualsAndHashCode
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@EqualsAndHashCode
@ApiModel(description = "Represents a purchase transaction to be billed")
class Transaction {

    @ApiModelProperty(value = "Credit card number", required = true)
    Long creditCardNumber

    @ApiModelProperty(value = "2-digit month of card expiration", required = true)
    Integer expMonth

    @ApiModelProperty(value = "4-digit year of card expiration", required = true)
    Integer expYear

    @ApiModelProperty(value = "3-digit card verification pin", required = true)
    Integer verificationCode

    @ApiModelProperty(value = "Card holder's billing address", required = true)
    String billingAddress

    @ApiModelProperty(value = "Persistence ID of customer/card holder making transaction", required = true)
    String customerId

    @ApiModelProperty(value = "Card holder's name", required = true)
    String customerName

    @ApiModelProperty(value = "Persistence ID of order being transacted", required = false)
    String orderNumber

    @ApiModelProperty(value = "Amount of transaction to charge to customer", required = true)
    Double amount
}