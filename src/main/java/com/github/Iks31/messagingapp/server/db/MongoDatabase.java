package com.github.Iks31.messagingapp.server.db;

import com.mongodb.client.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.model.Updates;

import java.lang.reflect.Array;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static com.mongodb.client.model.Filters.eq;

import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class MongoDatabase {
    static String uri = "mongodb+srv://ikerdz3101:<dbpassword>@jesmscluster0.9ownp4i.mongodb.net/?retryWrites=true&w=majority&appName=JeSMScluster0";
    static com.mongodb.client.MongoDatabase db;
    private static MongoClient mongoClient;

    public MongoDatabase() {
        connect();
    }

    public void connect()
    {
// Replace with your actual connection string

        try {
            mongoClient = MongoClients.create(uri);
            System.out.println("✅ Connected to MongoDB!");

//            // List all databases
//            for (String dbName : mongoClient.listDatabaseNames()) {
//                System.out.println("📁 Database: " + dbName);
//            }
//
//            // Optionally connect to a specific database
             db = mongoClient.getDatabase("JeSMS");
//            System.out.println("🔍 Using database: " + db.getName());
        } catch (Exception e) {
            System.err.println("❌ Connection failed");}
    }
    public MongoCollection<Document> Collection(
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

    public DBResult<String> getConversations(String username) {
        try {
            if (db == null) {
                connect();
            }
            MongoCollection<Document> collection = Collection("conversations");
            ArrayList<Document> conversations = new ArrayList<>();

            FindIterable<Document> iterable = collection.find(eq("users", username));
            MongoCursor<Document> mongoCursor = iterable.iterator();
            ArrayList<String> list = new ArrayList<>();
            while (mongoCursor.hasNext()) {
                list.add(mongoCursor.next().toJson());
            }
            System.out.println(list);
            return new DBResult<>(true, "successfully retrieved conversations",list);
        } catch (Exception e) {
            return new DBResult<>(false,e);
        }

    }

    public DBResult<String> login(String username) {
        try{
            if (db == null) {
                connect();
            }

            MongoCollection<Document> collection = Collection("users");
            Document user = collection.find(eq("user", username)).first();
            if(user == null){
                return new DBResult<>(false, "user does not exist");
            }
            else{
                ArrayList<String> credentials = new ArrayList<>();
                credentials.add(user.getString("user"));
                credentials.add(user.getString("password"));

                return new DBResult<>(true, "successfully retrieved credentials",credentials);
            }
        } catch (Exception e) {
            System.err.println("login unsuccessful");
            return new DBResult<>(false,e);
        }
    }

    public  DBResult<String> newMessage(String content, String username, ArrayList<String> users)
    {
        try{
            if (db == null) {
                connect();
            }
            MongoCollection<Document> collection = Collection("conversations");
            long date = Instant.now().toEpochMilli();
            Bson update = new Document("content", content)
                            .append("sender", username)
                            .append("timestamp", date)
                            .append("readBy", new ArrayList<String>())
                            .append("edited", false)
                            .append("isDeleted", false);
            collection.updateOne(eq("users",users), Updates.addToSet("messages", update));
            return new DBResult<>(true, "successfully sent message");
        } catch (Exception e) {
            return new DBResult<>(false,e);
        }
    }

    public DBResult<String> createConversation(String conversationName,ArrayList<String> users)
    {
        try {
            if (db == null) {
                connect();
            }

            if(conversationExists(users)){
                System.out.println("conversation already exists");
                return new DBResult<>(false,null,null);
            }
            // Creating the document
            // to be inserted

            MongoCollection<Document> collection = Collection("conversations");
            Document document = new Document("name",conversationName)
                    .append("users", users)
                    .append("messages", new ArrayList<>());


            collection.insertOne(document);

            System.out.println(
                    "Conversation created successfully");
            return new DBResult<>(true, "successfully created conversation");
        }
        catch (Exception e) {
            return new DBResult<>(false,e);
        }

    }

    public boolean conversationExists(ArrayList<String> users){
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

    public DBResult<String> addUser(ArrayList<String> users, String username) {
        try{
            if (db == null) {
                connect();
            }
            MongoCollection<Document> collection = Collection("conversations");
            ArrayList<String> newGroup = (ArrayList<String>)users.clone();
            newGroup.add(username);
            if(conversationExists(newGroup)){
                System.out.println("conversation already exists");
                return new DBResult<>(false,"conversation already exists");
            }
            collection.updateOne(eq("users", users), Updates.addToSet("users", username));
            return new DBResult<>(true, "successfully added user");
        }catch (Exception e) {
            return new DBResult<>(false,e);
        }
    }

    public DBResult<String> removeUser(ArrayList<String> users, String username) {
        try{
            if (db == null) {
                connect();
            }
            MongoCollection<Document> collection = Collection("conversations");
            ArrayList<String> newGroup = (ArrayList<String>)users.clone();
            newGroup.remove(username);
            if(conversationExists(newGroup)){
                System.out.println("conversation already exists");
                return new DBResult<>(false, "conversation already exists");
            }
            collection.updateOne(eq("users", users), Updates.pull("users", username));
            return new DBResult<>(true, "successfully removed user");
        }catch (Exception e) {
            return new DBResult<>(false,e);
        }
    }

    public DBResult<String> newUser(String username, String password) {
        try{
            MongoCollection<Document> collection = Collection("users");

            Document user = collection.find(eq("user", username)).first();
            if(user ==null){
                Document newUser = new Document("user", username).append("password", password);
                collection.insertOne(newUser);
                return new DBResult<>(true, "successfully created user");
            }
            else{
                return new DBResult<>(false, "user already exists");
            }
        }
        catch (Exception e) {
            return new DBResult<>(false,e);
        }
    }

    public DBResult<String> readByUser(ArrayList<String> users, String username) {
        //TODO need to figure a way to Identify a message to update the readBy field
        try{
            MongoCollection<Document> collection = Collection("messages");
            collection.updateOne(eq("users",users), Updates.addToSet("messages.readBy", username));
            return new DBResult<>(true, "successfully read message");
        } catch (Exception e) {
            return new DBResult<>(false,e);
        }
    }
    public void displayCollections()
    {

        try {
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

    public void shutDown(){
        mongoClient.close();
    }
}