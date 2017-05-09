/**
 *  Copyright 2005-2017 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package com.redhat.refarch.microservices.billing.service;

import com.redhat.refarch.microservices.billing.model.Result;
import com.redhat.refarch.microservices.billing.model.Result.Status;
import com.redhat.refarch.microservices.billing.model.Transaction;
import org.apache.camel.Consume;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class BillingService {

    private Logger logger = Logger.getLogger(getClass().getName());

    private static final Random random = new Random();

    public Result process(Transaction transaction) {

        Result result = new Result();

        logInfo("Asked to process credit card transaction: " + transaction);
        result.setName(transaction.getCustomerName());
        result.setOrderNumber(transaction.getOrderNumber());
        result.setCustomerId(transaction.getCustomerId());

        Calendar now = Calendar.getInstance();
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(transaction.getExpYear(), transaction.getExpMonth(), 1);

        if (calendar.after(now)) {
            result.setTransactionNumber(random.nextInt(9000000) + 1000000);
            result.setTransactionDate(now.getTime());
            result.setStatus(Status.SUCCESS);
        } else {
            result.setStatus(Status.FAILURE);
        }

        return result;
    }

    public void refund(int transactionNumber) {
        logInfo("Asked to refund credit card transaction: " + transactionNumber);
    }

    private void logInfo(String message) {
        logger.log(Level.INFO, message);
    }
}