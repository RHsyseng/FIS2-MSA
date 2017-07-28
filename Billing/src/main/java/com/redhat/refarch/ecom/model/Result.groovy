package com.redhat.refarch.ecom.model

import groovy.transform.EqualsAndHashCode
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@EqualsAndHashCode
@ApiModel(description = "Represents a transaction result in the system")
class Result {

    enum Status {

        SUCCESS, FAILURE
    }

    @ApiModelProperty(value = "Status of transaction", required = true)
    Status status

    @ApiModelProperty(value = "Name of customer on transaction", required = true)
    String name

    @ApiModelProperty(value = "Persistence ID of customer on transaction", required = true)
    String customerId

    @ApiModelProperty(value = "Order number of transaction processed", required = true)
    String orderNumber

    @ApiModelProperty(value = "Date of transaction", required = true)
    Long transactionDate

    @ApiModelProperty(value = "Persistence ID of transaction processed", required = true)
    Integer transactionNumber
}