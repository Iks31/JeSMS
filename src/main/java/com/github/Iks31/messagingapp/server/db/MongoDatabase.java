package com.github.Iks31.messagingapp.server.db;

import com.mongodb.client.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.model.Updates;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static com.mongodb.client.model.Filters.eq;

import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class MongoDatabase {
    static String uri = "mongodb+srv://ikerdz3101:<dbpassword>@jesmscluster0.9ownp4i.mongodb.net/?retryWrites=true&w=majority&appName=JeSMScluster0";
    static com.mongodb.client.MongoDatabase db;
    private static MongoClient mongoClient;


public static void main(String[] args) {

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
            db = mongoClient.getDatabase("JeSMS");
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

    public static String getConversations(String username) {
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

    public static void newMessage(String content, String username, ArrayList<String> users)
    {
        try{
            if (db == null) {
                connect();
            }
            MongoCollection<Document> collection = Collection("conversations");
            Date time = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
            Bson update = new Document("content", content)
                            .append("sender", username)
                            .append("timestamp", time)
                            .append("readBy", new ArrayList<String>())
                            .append("edited", false)
                            .append("isDeleted", false);
            collection.updateOne(eq("users",users), Updates.addToSet("messages", update));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static void createConversation(ArrayList<String> users)
    {
        try {
            if (db == null) {
                connect();
            }

            if(conversationExists(users)){
                System.out.println("conversation already exists");
                return;
            }
            // Creating the document
            // to be inserted

            MongoCollection<Document> collection = Collection("conversations");
            Document document = new Document("users", users)
                    .append("messages", new ArrayList<>());


            collection.insertOne(document);

            System.out.println(
                    "Conversation created successfully");
        }
        catch (Exception e) {
            System.out.println(
                    "Conversation unsuccessful");
            System.out.println(e);
        }
    }

    public static boolean conversationExists(ArrayList<String> users){
        try{
            if (db == null) {
                connect();
            }
            MongoCollection<Document> collection = Collection("conversations");
            Document user = collection.find(eq("users", users)).first();
            if(user !=null)return true;
            else return false;
        }
        catch (Exception e) {

        }
        return true;
    }

    public static void addUser(ArrayList<String> users, String username) {
        try{
            if (db == null) {
                connect();
            }
            MongoCollection<Document> collection = Collection("conversations");
            ArrayList<String> newGroup = (ArrayList<String>)users.clone();
            newGroup.add(username);
            if(conversationExists(newGroup)){
                System.out.println("conversation already exists");
                return;
            }
            collection.updateOne(eq("users", users), Updates.addToSet("users", username));
        }catch (Exception e) {

        }
    }

    public static void removeUser(ArrayList<String> users, String username) {
        try{
            if (db == null) {
                connect();
            }
            MongoCollection<Document> collection = Collection("conversations");
            ArrayList<String> newGroup = (ArrayList<String>)users.clone();
            newGroup.remove(username);
            if(conversationExists(newGroup)){
                System.out.println("conversation already exists");
                return;
            }
            collection.updateOne(eq("users", users), Updates.pull("users", username));

        }catch (Exception e) {

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