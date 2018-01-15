package model;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.*;
import com.google.common.hash.*;
import com.google.common.io.*;

import javafx.scene.image.Image;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.*;

public class Photo extends Observable {

    public static final int THUMB_WIDTH = 100;
    public static final int THUMB_HEIGHT = 100;

    public static final DateTimeFormatter EXIF_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");

    // Serialized
    @JsonProperty File file;
    @JsonProperty boolean favorite = false;
    @JsonProperty long lastModified;
    @JsonProperty int hash;
    @JsonProperty String caption;
    @JsonProperty List<String> tags = new LinkedList<String>();
    @JsonProperty int rotation = 0;
    @JsonProperty long fileSize;
    @JsonProperty LocalDateTime originalDate;
    @JsonProperty long width = -1, height = -1;

    // Not serialized
    @JsonIgnore boolean invalid = false;
    @JsonIgnore boolean missing = false;
    @JsonIgnore Image thumbnail;
    @JsonIgnore Database db;

    // Deserialization only
    public Photo() { }

    public Photo(File file, Database db) {
        this.file = file;
        this.db = db;
        readTransient();
        readMetadata();
    }

    public void setDatabase(Database db) {
        this.db = db;
    }

    // Read data from disk that is not persisted as part of database
    public void readTransient() {
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
    public long getWidth() { return width; }
    public long getHeight() { return height; }
    public int getHash() { return hash; }
    public LocalDateTime getOriginalDate() { return originalDate; }
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

    public void readMetadata() {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);

            for (Directory directory : metadata.getDirectories()) {

                //
                // Each Directory stores values in Tag objects
                //
                for (Tag tag : directory.getTags()) {
                    try {
                        if (tag.getTagName().equals("Image Height")) {
                            final Pattern pixels = Pattern.compile("(\\d+) pixels");
                            Matcher m = pixels.matcher(tag.getDescription());
                            if (m.matches()) {
                                height = Integer.parseInt(m.group(1));
                            }
                        }
                        if (tag.getTagName().equals("Image Width")) {
                            final Pattern pixels = Pattern.compile("(\\d+) pixels");
                            Matcher m = pixels.matcher(tag.getDescription());
                            if (m.matches()) {
                                width = Integer.parseInt(m.group(1));
                            }
                        }
                        if (tag.getTagName().equals("Orientation")) {
                            // Example: "Right side, top (Rotate 90 CW)"

                            // Clockwise rotations
                            final Pattern cwRotation = Pattern.compile(".*\\(Rotate (\\d+) CW\\).*");
                            Matcher m = cwRotation.matcher(tag.getDescription());
                            if (m.matches()) {
                                rotation = Integer.parseInt(m.group(1));
                            }

                            // Counter-clockwise rotations
                            final Pattern ccwRotation = Pattern.compile(".*\\(Rotate (\\d+) CCW\\).*");
                            m = ccwRotation.matcher(tag.getDescription());
                            if (m.matches()) {
                                rotation = -Integer.parseInt(m.group(0));
                            }

                            // TODO: mirroring?
                        }
                        if (tag.getTagName().equals("Date/Time")) {
                            // Example: "2016:10:03 16:00:04"
                            originalDate = LocalDateTime.parse(tag.getDescription(), EXIF_DATE_FORMAT);
                        }
                        // Debugging/testing:
                        // System.out.println(tag.toString());
                    }
                    catch (Exception e) {
                        System.out.println("WARNING: Exception " + e.toString() + " for tag: " + tag.toString());
                    }
                }

            }
        } catch (Exception e) {
            System.out.println("WARNING: Could not read metadata: " + file);
        }
    }
}
