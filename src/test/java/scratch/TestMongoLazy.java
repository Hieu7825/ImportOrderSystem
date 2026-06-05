package scratch;
import com.importorder.config.MongoConfig;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

public class TestMongoLazy {
    public static void main(String[] args) {
        System.out.println("Starting...");
        MongoCollection<Document> collection = MongoConfig.getDatabase().getCollection("test");
        System.out.println("Got collection, no crash.");
    }
}
