package com.redhat.refarch.ecom.model

import groovy.transform.EqualsAndHashCode
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.springframework.data.annotation.Id

@EqualsAndHashCode
@ApiModel(description = "Represents a customer in the system")
class Customer {

    @Id
    @ApiModelProperty(value = "Persistence ID of customer", required = false)
    String id

    @ApiModelProperty(value = "Full name of customer", required = true)
    String name

    @ApiModelProperty(value = "Full address of customer (123 Somewhere St, Nowhere, AZ 12345)", required = true)
    String address

    @ApiModelProperty(value = "Primary phone number for customer (+15551234567)", required = true)
    String telephone

    @ApiModelProperty(value = "Email address of customer", required = true)
    String email

    @ApiModelProperty(value = "Username for customer", required = true)
    String username

    @ApiModelProperty(value = "Password for customer", required = true)
    String password
}