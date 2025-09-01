package com.github.Iks31.messagingapp.server.db;

import com.mongodb.client.*;
import com.mongodb.MongoException;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import org.bson.Document;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

import java.util.Iterator;

public class MongoTest {
    static String uri = "mongodb+srv://ikerdz3101:<dbpassword>@jesmscluster0.9ownp4i.mongodb.net/?retryWrites=true&w=majority&appName=JeSMScluster0";
    static MongoDatabase db;
    private static MongoClient mongoClient;


public static void main(String[] args) {
    getconversations("user1");
    login("user1", "passwor");
}
    public static void connect()
    {
// Replace with your actual connection string

        try {
            mongoClient = MongoClients.create(uri);
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

    public static String getconversations(String username) {
        if (db == null) {
            connect();
        }
        MongoCollection<Document> collection = Collection("conversations");
        ArrayList<Document> conversations = new ArrayList<>();

        try {
            FindIterable<Document> iterable = collection.find(eq("users", username));
            MongoCursor<Document> mongoCursor = iterable.iterator();
            while (mongoCursor.hasNext()) {
                conversations.add(mongoCursor.next());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String json = conversations.getFirst().toJson();
        System.out.println(json);
        return json;
    }

    public static boolean login(String username, String password) {
        if (db == null) {
            connect();
        }

        MongoCollection<Document> collection = Collection("users");

        try{
            Document user = collection.find(and(eq("user", username),eq("password", password))).first();
            if(user !=null)System.out.println("login success");
            else System.out.println("login failed");
            return user != null;
        } catch (Exception e) {
            System.err.println("login unsuccessful");
            return false;
        }
    }

    //TODO need to change the Document that is inserted into one that fits the format of the conversations documents
    public static void createconversation(String username, String message)
    {
        try {
            if (db == null) {
                connect();
            }

            // Creating the document
            // to be inserted

            MongoCollection<Document> collection = Collection("conversations");
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
            if (db == null) {
                connect();
            }

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

}