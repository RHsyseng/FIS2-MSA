package com.redhat.refarch.ecom.model

import groovy.transform.EqualsAndHashCode
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.springframework.data.annotation.Id

@EqualsAndHashCode
@ApiModel(description = "Represents a customer's order in the system")
class Order {

    enum Status
    {
        Initial, InProgress, Canceled, Paid, Shipped, Completed
    }

    @Id
    @ApiModelProperty(value = "Persistence ID of order", required = false)
    String id

    @ApiModelProperty(value = "Status of order", required = false)
    Status status

    @ApiModelProperty(value = "Transaction number of order", required = true)
    Long transactionNumber

    @ApiModelProperty(value = "Date of transaction creation", required = true)
    Date transactionDate

    @ApiModelProperty(value = "Persistence ID of customer", required = true)
    String customerId

    @ApiModelProperty(value = "Persistence ID's of OrderItems", required = true)
    List<String> orderItemIds = []
}