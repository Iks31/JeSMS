package com.github.Iks31.messagingapp.server.db;

import com.mongodb.client.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.model.Updates;

import java.lang.reflect.Array;
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

public static void main(String[] args) {
    DBResult log = login("ChickenIker");
}
    public static void connect()
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

    public static DBResult<String> getConversations(String username) {
        try {
            if (db == null) {
                connect();
            }
            MongoCollection<Document> collection = Collection("conversations");
            ArrayList<Document> conversations = new ArrayList<>();

            FindIterable<Document> iterable = collection.find(eq("users", username));
            MongoCursor<Document> mongoCursor = iterable.iterator();
            while (mongoCursor.hasNext()) {
                conversations.add(mongoCursor.next());
            }
            String json = conversations.getFirst().toJson();
            ArrayList<String> list = new ArrayList<>();
            list.add(json);
            return new DBResult<>(true, "successfully retrieved conversations",list);
        } catch (Exception e) {
            return new DBResult<>(false,e);
        }

    }

    public static DBResult<String> login(String username) {
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
                //TODO this is the version when the method returns purely the credentials in the result outside of JSON
                ArrayList<String> credentials = new ArrayList<>();
                String json = user.toJson();
                Pattern pattern = Pattern.compile("\"(.*?)\"");
                Matcher matcher = pattern.matcher(json);
                String temp  ="";
                while (matcher.find()) {
                    System.out.println(matcher.group(1));
                    if(temp.equals("user")){
                        credentials.add(matcher.group(1));
                    }
                    if(temp.equals("password")){
                        credentials.add(matcher.group(1));
                    }
                    temp = matcher.group(1);
                }
               //TODO this is the version if this method returns the JSON back to the server
//                ArrayList<String> list = new ArrayList<>();
//                list.add(json);

                return new DBResult<>(true, "successfully retrieved credentials",credentials);
            }
        } catch (Exception e) {
            System.err.println("login unsuccessful");
            return new DBResult<>(false,e);
        }
    }

    public static DBResult<String> newMessage(String content, String username, ArrayList<String> users)
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
            return new DBResult<>(true, "successfully sent message");
        } catch (Exception e) {
            return new DBResult<>(false,e);
        }
    }

    public static DBResult<String> createConversation(ArrayList<String> users)
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
            Document document = new Document("users", users)
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

    public static DBResult<String> addUser(ArrayList<String> users, String username) {
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

    public static DBResult<String> removeUser(ArrayList<String> users, String username) {
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

    public static DBResult<String> newUser(String username, String password) {
        try{
            if(db == null) {
                connect();
            }
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