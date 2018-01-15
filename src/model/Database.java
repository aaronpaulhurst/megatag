package model;

import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.*;

public class Database extends Observable {

    final static String EXTENSIONS[] = {"jpg", "JPG", "jpeg", "JPEG"};

    public static boolean isImageExtension(String file) {
        for(String suffix : EXTENSIONS) {
            if (file.endsWith("." + suffix)) {
                return true;
            }
        }
        return false;
    }

    ArrayList<File> searchRoots = new ArrayList<File>();

    Map<File, Photo> images = new HashMap<File, Photo>();
    boolean modified = false;

    class Deduplication implements Comparable {
        Photo canonical;
        int num;

        Deduplication(Photo p) {
            canonical = p;
            num = 1;
        }

        @Override
        public int compareTo(Object o) {
            if (o instanceof Deduplication) {
                return canonical.getHash() -
                    ((Deduplication)o).canonical.getHash();
            }
            return 0;
        }
    };
    TreeSet<Deduplication> deduplication = new TreeSet<Deduplication>();

    Map<String, Integer> tagCounts = new TreeMap<String, Integer>();

    Object lock = new Object();

    public int size() { return images.size(); }

    public int numDistinct() { return deduplication.size(); }
    public boolean isCanonicalCopy(Photo p) {
        Deduplication lookup = new Deduplication(p);
        Deduplication floor = deduplication.floor(lookup);
        if (floor != null && floor.compareTo(lookup) == 0) {
            return floor.canonical == p;
        }
        return false; // Huh?
    }
    public int numDuplicates(Photo p) {
        Deduplication lookup = new Deduplication(p);
        Deduplication floor = deduplication.floor(lookup);
        if (floor != null && floor.compareTo(lookup) == 0) {
            return floor.num;
        }
        return 0; // Huh?
    }

    public Collection<Photo> get() {
        // Copy to avoid any problems by callers iterating during map modification
        final Vector<Photo> result;
        synchronized(lock) {
            result = new Vector<Photo>(images.values());
        }

        return result;
    }

    public List<String> getTopTags(int num) {
        List<String> rsl = new ArrayList<String>();
        for(String tag : tagCounts.keySet()) {
            rsl.add(tag);
            if (rsl.size() >= num) {
                break;
            }
        }
        return rsl;
    }

    public Map<String, Integer> getAllTags() {
        return tagCounts;
    }

    public void incTag(String tag, int delta) {
        int val = delta;
        if (tagCounts.containsKey(tag)) {
            val += tagCounts.get(tag);
        }
        tagCounts.put(tag, val);
    }

    public void addFiles(List<File> list, @Nullable StringProperty progressMessage) {
        if (list == null) {
            return;
        }

        Task<Void> task = new Task<Void>() {
            @Override public Void call() {
                for(File f : list) {
                    importFile(f);
                }
                return null;
            }
        };

        if (progressMessage != null) {
            progressMessage.unbind();
            progressMessage.bind(task.messageProperty());
        }

        task.setOnSucceeded(e -> {
            // The database is marked changed inside 'importFile'.
            notifyObservers();

            progressMessage.unbind();
            progressMessage.set("Total = " + size() + " images.");
        });

        new Thread(task).start();
    }

    public void addSearchRoot(File root, @Nullable StringProperty progressMessage)
    {
        synchronized(lock) {
            searchRoots.add(root);
        }

        Task<Void> task = new Task<Void>() {
            @Override public Void call() {
                updateMessage("Starting scan...");

                try {
                    Files.walkFileTree(root.toPath(), new SimpleFileVisitor<Path>()
                    {
                        int numFiles = 0;

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            if (attrs.isRegularFile() && isImageExtension(file.toString())) {
                                ++numFiles;
                                updateMessage("Found " + numFiles + " files.  Scanning...");
                                importFile(file.toFile());
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (IOException ex) {
                    // Give up.
                }
                updateMessage("Total = " + size() + " images.");
                return null;
            }
        };

        if (progressMessage != null) {
            progressMessage.unbind();
            progressMessage.bind(task.messageProperty());
        }

        task.setOnSucceeded(e -> {
            setChanged();
            notifyObservers();

            progressMessage.unbind();
            progressMessage.set("Total = " + size() + " images.");
        });

        new Thread(task).start();
    }

    public void deleteItems(Collection<Photo> toDelete) {
        synchronized(lock) {
            for(Photo p: toDelete) {
                p.removeAllTags();
                images.remove(p.getFile());
            }

            // Redo deduplication from scratch
            deduplication.clear();
            for(Photo p : images.values()) {
                addToDeduplication(p);
            }
        }

        setChangedSinceSave();
        setChanged();
        notifyObservers();
    }

    // Repopulate all data from photo files.
    // Useful if any photos have been changed.
    public void refreshAll( @Nullable StringProperty progressMessage )
    {
        Task<Void> task = new Task<Void>() {
            @Override public Void call() {
                updateMessage("Starting refresh...");

                // Copy to avoid holding lock
                final Vector<Photo> copy;
                synchronized(lock) {
                    copy = new Vector<Photo>(images.values());
                }

                int count = 0;
                for(Photo p : copy) {
                    p.readTransient();
                    p.readMetadata();
                    count += 1;
                    updateMessage("Refreshed " + count + " of " + size() + " images");
                }

                // TODO: Need to recompute deduplication if hashes changed.

                return null;
            }
        };

        if (progressMessage != null) {
            progressMessage.unbind();
            progressMessage.bind(task.messageProperty());
        }

        task.setOnSucceeded(e -> {
            setChanged();
            notifyObservers();
        });

        new Thread(task).start();
    }

    private void addToDeduplication(Photo p) {
        Deduplication lookup = new Deduplication(p);
        Deduplication floor = deduplication.floor(lookup);
        if (floor != null && floor.compareTo(lookup) == 0) {
            // Same hash exists
            floor.num += 1;
        } else {
            // Hash not found
            deduplication.add(lookup);
        }
    }

    public void clear() {
        synchronized(lock) {
            deduplication.clear();
            images.clear();
            searchRoots.clear();
            tagCounts.clear();
        }

        setChangedSinceSave();
        setChanged();
        notifyObservers();
    }

    // --- serialization / deserialization ---

    public void write(OutputStream os) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);

        // Copy to avoid holding lock
        final Vector<Photo> copy;
        synchronized(lock) {
            copy = new Vector<Photo>(images.values());
        }

        // TODO: Run in separate thread?

        // Object to JSON in file
        try {
            mapper.writeValue(os, copy);
        } catch (JsonMappingException ex) {
            System.out.println("JSON error: " + ex.toString());
            throw new IOException("JSON Error");
        } catch (JsonGenerationException ex) {
            System.out.println("JSON error: " + ex.toString());
            throw new IOException("JSON Error");
        }
    }

    public void read(InputStream is, Consumer<String> message) throws IOException
    {
        message.accept("Reading database");

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(new JavaTimeModule());

        ArrayList<Photo> readImages = mapper.readValue(is,
                new TypeReference<ArrayList<Photo>>() { });

        System.out.println("Read " + readImages.size() + " images");

        int count = 0;
        for(Photo p : readImages) {
            p.setDatabase(this);
            p.readTransient();
            count += 1;

            synchronized(lock) {
                if (!images.containsKey(p.getFile())) {
                    images.put(p.getFile(), p);
                    addToDeduplication(p);

                    for(String tag : p.getTags()) {
                        incTag(tag, 1);
                    }
                }
            }

            message.accept("Loaded " + count + " of " + readImages.size() + " files");
        }

        setChanged();
        notifyObservers();
    }

    // -- internal --

    // Caller should notify observers.
    private void importFile(File f) {
        synchronized(lock) {
            if (!images.containsKey(f)) {
                Photo p = new Photo(f, this);
                images.put(f, p);
                addToDeduplication(p);

                setChangedSinceSave();
                setChanged();
            }
        }
    }

    private void rescan() {
        // TODO

        notifyObservers();
    }

    // Called if when any image in the database has been modified.

    public void setChangedSinceSave() {
        modified = true;
    }
    public void clearChangedSinceSave() {
        modified = false;
    }
    public boolean wasChangedSinceSave() {
        return modified;
    }
}
