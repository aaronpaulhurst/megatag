package model;

import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

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

public class Database extends Observable {
    
    final static String EXTENSIONS[] = {"jpg"};
    
    public static boolean isImageExtension(String file) {
        for(String suffix : EXTENSIONS) {
            if (file.endsWith("." + suffix)) {
                return true;
            }
        }
        return false;
    }
    
    ArrayList<File> searchRoots = new ArrayList<File>();
    
    Collection<Photo> images = new HashSet<Photo>();
    
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
        return images;
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
    
    public void addFiles(List<File> list) {
        if (list == null) {
            return;
        }
        
        for(File f : list) {
            importFile(f);
        }
        
        notifyObservers();
    }
    
    public void addSearchRoot(File root, StringProperty progressMessage) {
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
                updateMessage("Total = " + images.size() + " images.");
                return null;
            }
        };
  
        progressMessage.unbind();
        progressMessage.bind(task.messageProperty());        

        task.setOnSucceeded(e -> { notifyObservers(); });
        
        new Thread(task).start();
    }
   
    public void deleteItems(Collection<Photo> toDelete) {
        synchronized(lock) {
            for(Photo p: toDelete) {
                p.removeAllTags();            
                images.remove(p);
            }
            
            // Redo deduplication from scratch
            deduplication.clear();
            for(Photo p : images) {
                addToDeduplication(p);
            }
        }
               
        setChanged();
        notifyObservers();
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

    // --- serialization / deserialization ---
    
    public void write(OutputStream os) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);

        //Object to JSON in file
        try {
            mapper.writeValue(os, images);
        } catch (JsonMappingException ex) {
            System.out.println("JSON error: " + ex.toString());
            throw new IOException("JSON Error");
        } catch (JsonGenerationException ex) {
            System.out.println("JSON error: " + ex.toString());
            throw new IOException("JSON Error");
        }
    }
    
    public void read(InputStream is) throws IOException {
        ArrayList<Photo> readImages = 
                new ObjectMapper().readValue(is, 
                        new TypeReference<ArrayList<Photo>>() { });
        
        System.out.println("Read " + readImages.size() + " images");
        
        for(Photo p : readImages) {
            p.finishInit(this);

            synchronized(lock) {
                images.add(p);
                addToDeduplication(p);

                for(String tag : p.getTags()) {
                    incTag(tag, 1);
                }
            }
        }
        
        setChanged();
        notifyObservers();
    }
    
    // -- internal --
    
    // Caller should notify observers.
    private void importFile(File f) {
        synchronized(lock) {
            Photo p = new Photo(f, this);
            images.add(p);  
            addToDeduplication(p);
        }
        setChanged();
    }
    
    private void rescan() {
        // TODO
        
        notifyObservers();
    }
}