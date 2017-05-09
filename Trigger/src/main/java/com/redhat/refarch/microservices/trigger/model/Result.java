package com.redhat.refarch.microservices.trigger.model;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;

@XmlRootElement
public class Result {

    public enum Status {
        SUCCESS, FAILURE
    }

    private Status status;
    private String name;
    private Long customerId;
    private Long orderNumber;
    private Date transactionDate;
    private Integer transactionNumber;

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(Long orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Date getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(Date transactionDate) {
        this.transactionDate = transactionDate;
    }

    public Integer getTransactionNumber() {
        return transactionNumber;
    }

    public void setTransactionNumber(Integer transactionNumber) {
        this.transactionNumber = transactionNumber;
    }

    @Override
    public String toString() {
        return "Result [status=" + status + ", name=" + name + ", orderNumber=" + orderNumber + ", transactionDate=" + transactionDate
                + ", transactionNumber=" + transactionNumber + "]";
    }
}