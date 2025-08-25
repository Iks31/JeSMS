import com.mongodb.client.*;
import org.bson.Document;

import java.util.Iterator;

public class MongoTest {
    String uri = "mongodb+srv://ikerdz3101:Pamplona3101@jesmscluster0.9ownp4i.mongodb.net/?retryWrites=true&w=majority&appName=JeSMScluster0";
    static MongoDatabase db;
    private static MongoClient mongoClient;


public static void main(String[] args) {
    connect();
    MongoCollection<Document> collection = db.getCollection("Users");
    displayCollections();
    displayDocuments();
    // now you can
}
    public static void connect()
    {
// Replace with your actual connection string

        try {
         //   mongoClient = MongoClients.create(uri);
            System.out.println("✅ Connected to MongoDB!");

            // List all databases
            for (String dbName : mongoClient.listDatabaseNames()) {
                System.out.println("📁 Database: " + dbName);
            }

            // Optionally connect to a specific database
            db = mongoClient.getDatabase("JeSMS"); // Replace "test" with your DB name
            System.out.println("🔍 Using database: " + db.getName());
        } catch (Exception e) {
            System.err.println("❌ Connection failed");}
    }
    public static MongoCollection<Document> Collection(
            String collectionName) {

        try {
            if (db == null) {
                connect();
            }
            return db.getCollection(collectionName);
            // establishConnections() Code

        }catch (Exception e) {
           System.err.println("failed");
           return null;
        }
    }
    public static void insertADocIntoDb()
    {
        try {
            // establishConnections() Code
            // is defined above
            if (db == null) {
                connect();
            }

            // Creating the document
            // to be inserted

            MongoCollection<Document> collection = Collection("users");
            Document document = new Document("MessageID",
                                            "2")
                    .append("ChatID", "Open-Source database")
                    .append("UserID","")
                    .append("Content","")
                    .append("sentAt","")
                    .append("readBy",new Document("UserID","").append("readAt",""))
                    .append("status","")
                    .append("editedAt","")
                    .append("deleted","");

            // Insert the document

            collection.insertOne(document);

            System.out.println(
                    "Document inserted Successfully");
        }
        catch (Exception e) {
            System.out.println(
                    "Document insertion failed");
            System.out.println(e);
        }
    }

    public static void displayCollections()
    {

        try {
            // establishConnections() Code
            // is defined above
            connect();


            System.out.println(
                    "Displaying the list"
                            + " of all collections");

            MongoCollection<Document> collection
                    = db.getCollection(
                    "users");

            for (String allColl : db
                    .listCollectionNames()) {
                System.out.println(allColl);
            }
        }
        catch (Exception e) {
            System.out.println(
                    "Collections display failed");
            System.out.println(e);
        }
    }

    public static void displayDocuments()
    {

        try {
            // establishConnections() Code
            // is defined above
            connect();

            System.out.println(
                    "Displaying the list"
                            + " of Documents");

            // Get the list of documents from the DB

            FindIterable<Document> iterobj
                    = Collection("users").find();

            // Print the documents using iterators
            Iterator itr = iterobj.iterator();
            while (itr.hasNext()) {
                System.out.println(itr.next());
            }
        }
        catch (Exception e) {
            System.out.println(
                    "Could not find the documents "
                            + "or No document exists");
            System.out.println(e);
        }
    }
}