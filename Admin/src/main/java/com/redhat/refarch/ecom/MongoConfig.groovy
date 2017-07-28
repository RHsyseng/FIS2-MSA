package com.redhat.refarch.ecom

import com.mongodb.Mongo
import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.config.AbstractMongoConfiguration

/***
 * @author jary@redhat.com
 */
@Configuration
class MongoConfig extends AbstractMongoConfiguration {

    @Value('${mongo.host:127.0.0.1}')
    String mongoHost

    @Value('${mongo.port:27017}')
    String  mongoPort

    @Value('${mongo.db:ecom.admin.admin}')
    String mongoDb

    @Value('${mongo.username:mongouser}')
    String mongoUser

    @Value('${mongo.password:password}')
    String mongoPass

    @Override
    String getDatabaseName() {
        return mongoDb
    }

    @Override
    Mongo mongo() throws Exception {

        MongoClientURI uri = new MongoClientURI(
                "mongodb://${mongoUser}:${mongoPass}@${mongoHost}:${mongoPort}" +
                "/?authSource=${mongoDb}&authMechanism=SCRAM-SHA-1")
        println uri.toString()

        return new MongoClient(uri)
    }

    @Override
    String getMappingBasePackage() {
        return "com.redhat.refarch.ecom.admin.admin.repository"
    }
}