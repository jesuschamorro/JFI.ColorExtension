package jfi.voronoi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class which manage project resources and temporal files.
 * It will create a random temporal file for storing these files and destroy
 * them when application finishes.
 *
 * @author Míriam Mengíbar Rodríguez (mirismr@correo.ugr.es)
 */
public class ResourceManager {

    /**
     * Root temporal directory.
     */
    private String temporalDir;

    /**
     * Creates a new manager resources initializing the temporal dir with
     * random UUID and adding shutdown hook for delete it.
     */
    public ResourceManager() {
        this.temporalDir = "tmp" + UUID.randomUUID() + File.separator;
        
        // create temporal dir
        new File(temporalDir).mkdirs();
        
        // adding shutdown hook for remove temporal files
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                File file = new File(temporalDir);
                for (File f : file.listFiles()) {
                    f.delete();
                }
                
                file.delete();
            }
        });
    }
    
    /**
     * Set all permissions for a file.
     * @param file a file.
     */
    private void setPermissions777(File file) {
        Set<PosixFilePermission> perms = new HashSet<>();
        perms.add(PosixFilePermission.OTHERS_WRITE);
        perms.add(PosixFilePermission.OTHERS_READ);
        perms.add(PosixFilePermission.OTHERS_EXECUTE);
        perms.add(PosixFilePermission.GROUP_WRITE);
        perms.add(PosixFilePermission.GROUP_READ);
        perms.add(PosixFilePermission.GROUP_EXECUTE);
        perms.add(PosixFilePermission.OWNER_WRITE);
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_EXECUTE);
        try {
            Files.setPosixFilePermissions(file.toPath(), perms);
        } catch (IOException ex) {
            Logger.getLogger(ResourceManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Creates a temporal file given a filename.
     * @param filename a filename.
     * @return the new file with 777 permissions.
     */
    public File createTemporalFile(String filename) {
        File out = new File(this.temporalDir+filename);
        boolean created = false;
        try {
            created = out.createNewFile();
        } catch (IOException ex) {
            Logger.getLogger(ResourceManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (created) {
            setPermissions777(out);
            return out;
        }
       
        return null;
    }
    
    /**
     * Creates and returns a resource file found in resource packages given a
     * resource filename.
     * @param resourceFilename a resource filename.
     * @return a copy of resource file.
     */
    public File getResourceFile(String resourceFilename) {
        String path = this.temporalDir + resourceFilename;
        File out = null;
        try {
            InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceFilename);
            long copied = Files.copy(stream, Paths.get(path), StandardCopyOption.REPLACE_EXISTING);
            if (copied > 0) {
                out = new File(path);
                setPermissions777(out);
            }
        } catch (IOException ex) {
            Logger.getLogger(ResourceManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return out;
    }

}
