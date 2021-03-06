package com.stacksync.desktop.index.requests;

import com.stacksync.desktop.chunker.ChunkEnumeration;
import com.stacksync.desktop.chunker.FileChunk;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.config.profile.Account;
import com.stacksync.desktop.db.models.CloneChunk;
import com.stacksync.desktop.db.models.CloneFile;
import com.stacksync.desktop.db.models.CloneWorkspace;
import com.stacksync.desktop.gui.tray.Tray;
import com.stacksync.desktop.logging.RemoteLogs;
import com.stacksync.desktop.util.FileUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import org.apache.log4j.Logger;

public class NewIndexSharedRequest extends SingleRootIndexRequest {
    
    private final Logger logger = Logger.getLogger(NewIndexRequest.class.getName());
    
    private File file;
    private long checksum;
    private CloneFile previousVersion;

    public NewIndexSharedRequest(Folder root, File file, CloneFile previousVersion, long checksum) {
        super(root);
  
        this.file = file;        
        this.checksum = checksum;
        this.previousVersion = previousVersion;
    }

    @Override
    public void process() {
        logger.info("Indexer: Indexing new share file "+file+" ...");
        
        // Find file in DB
        CloneFile dbFile = db.getFileOrFolder(root, file);
        if(dbFile != null){
            if(dbFile.getChecksum() == checksum){
                logger.warn("Indexer: Error already Indexed this version. Ignoring.");
                return;
            }
            
            if(dbFile.isFolder()){
                logger.warn("Indexer: Error already NewIndexRequest this folder. Ignoring.");
                return;
            }
        }
        
        this.tray.setStatusIcon(this.processName, Tray.StatusIcon.UPDATING);
        
        // Create DB entry
        CloneFile newVersion = (previousVersion == null) ? addNewVersion() : addChangedVersion();

        File parentFile = FileUtil.getCanonicalFile(file.getParentFile());
        CloneFile parentCF = db.getFolder(root, parentFile);
        newVersion.setParent(parentCF);
        
        CloneWorkspace workspace = parentCF.getWorkspace();
        newVersion.setWorkspace(workspace);
        
        // IMPORTANT Since a shared folder is created without using watcher+indexer
        // it should always exist a parent.
        // This will check if the file is inside a folder that isn't created.
        /*if (newVersion.getParent() == null && !newVersion.getPath().equals("/")) {
            Indexer.getInstance().queueNewIndex(root, file, null, checksum);
            return;
        }*/
        
        newVersion.setFolder(file.isDirectory());
        newVersion.setSize(file.length());
        
        newVersion.setLastModified(new Date(file.lastModified()));
        newVersion.setSyncStatus(CloneFile.SyncStatus.LOCAL);
        newVersion.merge();
        
        if (newVersion.isFolder())
            processFolder(newVersion);
        else {
            processFile(newVersion);
        }
        
        this.tray.setStatusIcon(this.processName, Tray.StatusIcon.UPTODATE);
    }

    private CloneFile addNewVersion() {
        CloneFile newVersion = new CloneFile(root, file);
        
        newVersion.setVersion(1);
        newVersion.setStatus(CloneFile.Status.NEW);
        
        return newVersion;
    }
    
    private CloneFile addChangedVersion() {        
        CloneFile newVersion = (CloneFile) previousVersion.clone();
        
        if (newVersion.getSyncStatus() == CloneFile.SyncStatus.UNSYNC
                && previousVersion.getStatus() != CloneFile.Status.RENAMED) {
            if (previousVersion.getVersion() == 1) {
                previousVersion.remove();
                newVersion = this.addNewVersion();
            } else {
                newVersion.setStatus(CloneFile.Status.CHANGED);
                newVersion.setChunks(new ArrayList<CloneChunk>()); // otherwise we would append!
                newVersion.setMimetype(FileUtil.getMimeType(newVersion.getFile()));
                previousVersion.remove();
            }
        } else {
            newVersion.setVersion(previousVersion.getVersion()+1);
            newVersion.setStatus(CloneFile.Status.CHANGED);
            newVersion.setChunks(new ArrayList<CloneChunk>()); // otherwise we would append!
            newVersion.setMimetype(FileUtil.getMimeType(newVersion.getFile()));
        }

        return newVersion;
    }
    
    private void processFolder(CloneFile cf) {        
        // Add rest of the DB stuff 
        cf.setChecksum(0);
        
        if (FileUtil.checkIllegalName(cf.getName())
                || FileUtil.checkIllegalName(cf.getPath().replace("/", ""))){
            cf.setSyncStatus(CloneFile.SyncStatus.UNSYNC);
        } else {
            cf.setSyncStatus(CloneFile.SyncStatus.UPTODATE);
        }
        cf.merge();
        
        // Not necessary yet. Shared folders don't contain files.
        // Analyze file tree (if directory) -- RECURSIVELY!!
        /*logger.info("Indexer: Indexing CHILDREN OF "+file+" ...");
        for (File child: file.listFiles()) {
            // Ignore .ignore files
            if (FileUtil.checkIgnoreFile(root, child)) {
                continue; 
            }
            
            // Do it!
            logger.info("Indexer: Parent: "+file+" / CHILD "+child+" ...");
            Indexer.getInstance().queueNewIndex(root, child, null, -1);
        }*/
    }
    
    private void processFile(CloneFile cf) {
        try {
            // 1. Chunk it!
            FileChunk chunkInfo = null;

            //ChunkEnumeration chunks = chunker.createChunks(file, root.getProfile().getRepository().getChunkSize());
            ChunkEnumeration chunks = chunker.createChunks(file);
            while (chunks.hasMoreElements()) {
                chunkInfo = chunks.nextElement();                

                // create chunk in DB (or retrieve it)
                String chunkName = "chk-"+chunkInfo.getChecksum()+"-"+cf.getId();
                CloneChunk chunk = db.getChunk(chunkInfo.getChecksum(), CloneChunk.CacheStatus.CACHED, chunkName);
                
                // write encrypted chunk (if it does not exist)
                File chunkCacheFile = config.getCache().getCacheChunk(chunk);

                byte[] packed = FileUtil.pack(chunkInfo.getContents(), root.getProfile().getEncryption(cf.getWorkspace().getId()));
                if (!chunkCacheFile.exists()) {
                    FileUtil.writeFile(packed, chunkCacheFile);
                } else{
                    if(chunkCacheFile.length() != packed.length){
                        FileUtil.writeFile(packed, chunkCacheFile);
                    }
                }
                
                cf.addChunk(chunk);
            }      
            
            logger.info("Indexer: saving chunks...");
            cf.merge();
            logger.info("Indexer: chunks saved...");
            
            // 2. Add the rest to the DB, and persist it
            if (chunkInfo != null) { // The last chunk holds the file checksum                
                cf.setChecksum(chunkInfo.getFileChecksum()); 
            } else {
                cf.setChecksum(checksum);
            }
            cf.merge();
            chunks.closeStream();
            
            // 3a. Check storage free space
            Account account = this.config.getProfile().getAccount();
            Long availableQuota = account.getQuota() - account.getQuotaUsed();
            if (availableQuota < cf.getSize()) {
                cf.setSyncStatus(CloneFile.SyncStatus.UNSYNC);
                cf.merge();
                return;
            }
            
            // 3b. Upload it
            if (FileUtil.checkIllegalName(cf.getName())
                    || FileUtil.checkIllegalName(cf.getPath().replace("/", ""))){
                logger.info("This filename contains illegal characters.");
                cf.setSyncStatus(CloneFile.SyncStatus.UNSYNC);
                cf.merge();
            } else {
                logger.info("Indexer: Added to DB. Now Q file "+file+" at uploader ...");
                root.getProfile().getUploader().queue(cf);
            }
            
        } catch (Exception ex) {
            logger.error("Could not index new file "+file+". IGNORING.", ex);
            RemoteLogs.getInstance().sendLog(ex);
        } 
    }
    
    @Override
    public String toString() {
        return NewIndexSharedRequest.class.getSimpleName() + "[" + "file=" + file + "]";
    }    
    
}
