package model;

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.google.common.hash.*;
import com.google.common.io.*;

import javafx.scene.image.Image;

public class Photo extends Observable {

    public static final int THUMB_WIDTH = 100;
    public static final int THUMB_HEIGHT = 100;
    
    // Serialized
    @JsonProperty File file;
    @JsonProperty boolean favorite = false;
    @JsonProperty long lastModified;
    @JsonProperty int hash;
    @JsonProperty String caption;
    @JsonProperty List<String> tags = new LinkedList<String>();
    @JsonProperty int rotation = 0;
    @JsonProperty long fileSize;

    // Not serialized
    @JsonIgnore boolean invalid = false;
    @JsonIgnore boolean missing = false;    
    @JsonIgnore Image thumbnail;
    @JsonIgnore Database db;
    
    // Deserialization only
    public Photo() { }
    
    public Photo(File file, Database db) {
        this.file = file;
        finishInit(db);
    }
    
    public void finishInit(Database db) {
        this.db = db;
                
        if (!file.exists()) {
            missing = true;
            return;
        }
        
        ByteSource bs = Files.asByteSource(file);

        // Read image
        try(InputStream is = bs.openStream()) {
            thumbnail = new Image(is, THUMB_WIDTH, THUMB_HEIGHT, true, true /*hi quality*/);
        } catch(IOException ex) {
            invalid = true;
        }
        
        // Read hash
        try {
            hash = bs.hash(Hashing.crc32()).asInt();
        } catch (IOException ex) {
            hash = 0;
        }

        lastModified = file.lastModified();
        fileSize = file.length();
    }
    
    public Database getDb() { return db; }
    public File getFile() { return file; }
    public Image getThumbnail() { return thumbnail; }
    public long getLastModified() { return lastModified; }
    public long getFileSize() { return fileSize; }
    public int getHash() { return hash; }
    public boolean isMissing() { return missing; }
    public boolean isInvalid() { return invalid; }

    public String getCaption() { return caption; }
    public  void setCaption(String val) { 
        caption = val;
        setChanged();
        notifyObservers();
        if (db != null) {
            db.setChangedSinceSave();
        }
    }
    
    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean val) { 
        favorite = val;        
        setChanged();
        notifyObservers();
        if (db != null) {
            db.setChangedSinceSave();
        }
    }
    
    public int getRotation() { return rotation; }
    public void setRotation(int angle) { 
        rotation = angle;        
        setChanged();
        notifyObservers();
        if (db != null) {
            db.setChangedSinceSave();
        }
    }
    
    public List<String> getTags() { return tags; }
    public void addTag(String tag) { 
        if (tags.contains(tag)) {
            return;
        }
        db.incTag(tag, 1);
        tags.add(tag);
        setChanged();
        notifyObservers();
        db.setChangedSinceSave();
    }
    public void removeTag(String tag) { 
        if (!tags.contains(tag)) {
            return;
        }
        db.incTag(tag, -1);
        tags.remove(tag);
        setChanged();
        notifyObservers();
        db.setChangedSinceSave();
    }
    public void removeAllTags() {
        for(String tag: tags) {
            db.incTag(tag, -1);  
        }
        tags.clear();
        setChanged();
        notifyObservers();
        db.setChangedSinceSave();
    }
}
