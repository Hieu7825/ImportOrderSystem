package com.importorder.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import io.github.cdimascio.dotenv.Dotenv;

public class MongoConfig {

    private static MongoClient mongoClient;
    private static MongoDatabase database;

    static {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String uri = dotenv.get("MONGO_URI", "mongodb://localhost:27017");
        String dbName = dotenv.get("DB_NAME", "import_order_db");
        mongoClient = MongoClients.create(uri);
        database = mongoClient.getDatabase(dbName);
    }

    public static MongoDatabase getDatabase() {
        return database;
    }

    public static void close() {
        if (mongoClient != null) mongoClient.close();
    }
}