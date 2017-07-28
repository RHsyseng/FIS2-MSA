package com.redhat.refarch.ecom.model

import groovy.transform.EqualsAndHashCode

import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response

@EqualsAndHashCode
class Error {

    int code
    String message
    String details

    Error(int code, String message, Throwable throwable) {
        this.code = code
        this.message = message ?: throwable.getMessage()
        if (throwable != null) {
            StringWriter writer = new StringWriter()
            throwable.printStackTrace(new PrintWriter(writer))
            this.details = writer.toString()
        }
    }

    Error(int code, String message) {
        this(code, message, null)
    }

    Error(int code, Throwable throwable) {
        this(code, null, throwable)
    }

    WebApplicationException asException() {
        return new WebApplicationException(Response.status(code).entity(this).build())
    }
}