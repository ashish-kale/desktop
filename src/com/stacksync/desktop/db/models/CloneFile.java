/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011 Philipp C. Heckel <philipp.heckel@gmail.com> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.stacksync.desktop.db.models;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import javax.persistence.*;
import org.apache.log4j.Logger;
import com.stacksync.desktop.Environment;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.config.profile.Profile;
import com.stacksync.desktop.db.PersistentObject;
import com.stacksync.desktop.logging.LogConfig;
import com.stacksync.desktop.util.FileUtil;

/**
 * Represents a version of a file.
 *
 * @author Philipp C. Heckel
 */
@Entity
@Cacheable(false)
@IdClass(value = CloneFilePk.class)
//@Table( uniqueConstraints={@UniqueConstraint(columnNames={"last_modified", "checksum"})} )
public class CloneFile extends PersistentObject implements Serializable, Cloneable {

    private static final Logger logger = Logger.getLogger(CloneFile.class.getName());
    private static final Config config = Config.getInstance();
    private static final Environment env = Environment.getInstance();
    
    private static final long serialVersionUID = 12314234L;

    /**
     * <ul> <li>UNKNOWN <li>NEW: New file <lI>CHANGED: The file contents have
     * changed. At least one chunk differs. <li>RENAMED: The file path or name
     * has changed. <li>MERGED: The file history has been merged to a different
     * file. </ul>
     */
    public enum Status { UNKNOWN, NEW, CHANGED, RENAMED, DELETED };

    /**
     * LOCAL: The file entry hasn't been propagated to the server yet IN_UPDATE:
     * The file entry should be included in the update-file, but not (yet) in
     * the base file IN_BASE: The file entry should be included in the base-file
     * (= complete DB dump)
     */
    public enum SyncStatus { UNKNOWN, LOCAL, SYNCING, UPTODATE, CONFLICT, REMOTE, UNSYNC };
    /**
     * versionId of the root file; identifies the history of a file
     */
    @Id
    @Column(name = "file_id", nullable = false)
    private Long fileId;
    
    @Id
    @Column(name = "file_version", nullable = false)
    private long version;
    
    @Id
    @Column(name = "profile_id", nullable = false)
    private int profileId;
    
    @Id
    @Column(name = "root_id", nullable = false)
    private String rootId;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated")
    private Date updated;
    
    @Column(name = "checksum")
    private long checksum;
    
    @Transient
    private Profile profile;
    
    @Transient
    private Folder root;
    
    // FILE PROPERTIES
    @Column(name = "is_folder")
    private boolean folder;
    
    @ManyToOne(cascade=CascadeType.REMOVE)
    @JoinColumns({
        @JoinColumn(name = "parent_file_id", referencedColumnName = "file_id"),
        @JoinColumn(name = "parent_file_version", referencedColumnName = "file_version"),
        @JoinColumn(name = "parent_profile_id", referencedColumnName = "profile_id"),
        @JoinColumn(name = "parent_root_id", referencedColumnName = "root_id")
    })
    private CloneFile parent;
    
    /**
     * Locally cached value of the path. Not populated in the update-files.
     */
    @Column(name = "file_path", nullable = false)
    private String filePath;
    
    @Column(name = "name", length = 1024)
    private String name;
    
    @Column(name = "file_size")
    private long fileSize;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_modified")
    private Date lastModified;
    
    @ManyToMany
    @OrderColumn
    private List<CloneChunk> chunks;
   
    @OneToOne
    private Workspace workspace;
        
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status")
    private SyncStatus syncStatus;
    
    // TODO this should be a CloneClient instance
    @Column(name = "client_name")
    private String clientName;
    
    @Column(name = "mimetype")
    private String mimetype;    
    
    //@Column(name="server_uploaded")
    //private boolean serverUploaded;
    
    @Column(name="server_uploaded_ack")
    private boolean serverUploadedAck;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="server_uploaded_time")
    private Date serverUploadedTime;

    public CloneFile() {
        this.fileId = new Random().nextLong();
        this.version = 1;
        this.chunks = new ArrayList<CloneChunk>();
        this.status = Status.UNKNOWN;
        this.syncStatus = SyncStatus.UNKNOWN;

        this.checksum = 0;
        this.name = "(unknown)";
        this.filePath = "(unknown)";
        this.mimetype = "unknown";

        this.clientName = config.getMachineName();
        
        this.serverUploadedAck = false;
        this.serverUploadedTime = null;
    }

    public CloneFile(Folder root, File file) {
        this();

        // Set account
        this.profile = root.getProfile();
        this.profileId = root.getProfile().getId();
        this.root = root;
        this.rootId = root.getRemoteId();

        this.name = file.getName();
        this.filePath = "/" + FileUtil.getRelativeParentDirectory(root.getLocalFile(), file);
        this.filePath = FileUtil.getFilePathCleaned(filePath);
        
        this.fileSize = file.isDirectory() ? 0 : file.length();
        this.lastModified = new Date(file.lastModified());
        this.folder = file.isDirectory();       
              
        this.mimetype = FileUtil.getMimeType(file);
        
        setWorkspaceByPath(filePath);
    }

    public Folder getRoot() {
        if (root == null) {
            root = getProfile().getFolders().get(getRootId());
        }

        return root;
    }

    public void setRoot(Folder root) {
        this.root = root;
        this.rootId = root.getRemoteId();
    }

    public void setParent(CloneFile parent) {
        this.parent = parent;
    }

    public CloneFile getParent() {
        return parent;
    }

    public Profile getProfile() {
        if (profile == null) {
            profile = config.getProfiles().get(getProfileId());
        }

        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
        this.profileId = profile.getId();
    }

    public int getProfileId() {
        return profileId;
    }

    public void setProfileId(int profileId) {
        this.profileId = profileId;
    }

    public String getRootId() {
        return rootId;
    }

    public void setRootId(String rootId) {
        this.rootId = rootId;
    }

    public boolean isFolder() {
        return folder;
    }

    public void setFolder(boolean folder) {
        this.folder = folder;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    public String getPath() {
        return filePath;
    }

    public void setPath(String filePath) {
        this.filePath = filePath;
    }

    public SyncStatus getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(SyncStatus syncStatus) {
        this.updated = new Date();
        this.syncStatus = syncStatus;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFileName() {
        return String.format("file-%s-%020d", getChecksum(), getLastModified().getTime());
    }

    /**
     * Get relative path to the root dir.
     */
    public String getRelativePath() {
        return FileUtil.getRelativePath(getRoot().getLocalFile(), getFile());
    }

    public String getAbsolutePath() {
        return getFile().getAbsolutePath();
    }

    public String getRelativeParentDirectory() {
        return FileUtil.getRelativeParentDirectory(getRoot().getLocalFile(), getFile());
    }

    public String getAbsoluteParentDirectory() {
        return FileUtil.getAbsoluteParentDirectory(getFile());
    }

    public File getFile() {
        return FileUtil.getCanonicalFile(new File(getRoot().getLocalFile() + File.separator + getPath() + File.separator + getName()));
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public long getChecksum() {
        return checksum;
    }

    public void setChecksum(long checksum) {
        this.checksum = checksum;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public CloneFile getPreviousVersion() {
        // If we are the first, there are no others
        if (getVersion() == 1) {
            return null;
        }

        List<CloneFile> pV = getPreviousVersions();

        if (pV.isEmpty()) {
            return null;
        }

        return pV.get(pV.size() - 1);
    }

    public List<CloneFile> getPreviousVersions() {
        // If we are the first, there are no others
        if (getVersion() == 1) {
            return new ArrayList<CloneFile>();
        }

        String queryStr = "select c from CloneFile c where "
                + "     c.profileId = :profileId and "
                + "     c.fileId = :fileId and "
                + "     c.version < :version "
                + "     order by c.version asc";

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneFile.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");        
        
        query.setParameter("profileId", getProfileId());
        query.setParameter("fileId", getFileId());
        query.setParameter("version", getVersion());

        List<CloneFile> list = query.getResultList();
        return list;
    }

    public List<CloneFile> getNextVersions() {
        String queryStr = "select c from CloneFile c where "
                + "     c.profileId = :profileId and "
                + "     c.fileId = :fileId and "
                + "     c.version > :version "
                + "     order by c.version asc";

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneFile.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");        
        
        query.setParameter("profileId", getProfileId());
        query.setParameter("fileId", getFileId());
        query.setParameter("version", getVersion());

        List<CloneFile> list = query.getResultList();
        return list;
    }

    public List<CloneFile> getVersionHistory() {
        List<CloneFile> versions = new ArrayList<CloneFile>();

        versions.addAll(getPreviousVersions());
        versions.add(this);
        versions.addAll(getNextVersions());

        return versions;
    }

    public CloneFile getFirstVersion() {
        String queryStr = "select c from CloneFile c where "
                + "     c.profileId = :profileId and "
                + "     c.fileId = :fileId "
               // + "   and c.version = 1");
                + "     order by c.version asc";

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneFile.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");        
        
        query.setMaxResults(1);
        query.setParameter("profileId", getProfileId());
        query.setParameter("fileId", getFileId());

        try {
            return (CloneFile) query.getSingleResult();
        } catch (NoResultException ex) {
            logger.info(env.getMachineName() + "# No result -> " + ex.getMessage());
            return null;
        }
    }
    
    
    public List<CloneFile> getChildren() {
        
        String queryStr = "select c from CloneFile c where "
                + "     c.parent = :parent";

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneFile.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");        
        
        query.setParameter("parent", this);
        List<CloneFile> list = query.getResultList();
        
        /*for(CloneFile cf: list){
            config.getDatabase().getEntityManager().refresh(cf);
        }*/
        return list;
    }

    public CloneFile getLastVersion() {
        String queryStr = "select c from CloneFile c where "
                + "     c.profileId = :profileId and "
                + "     c.fileId = :fileId "
                + "     order by c.version desc";

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneFile.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");        
        
        query.setMaxResults(1);
        query.setParameter("profileId", getProfileId());
        query.setParameter("fileId", getFileId());

        try {
            return (CloneFile) query.getSingleResult();
        } catch (NoResultException ex) {
            logger.info(env.getMachineName() + "# No result -> " + ex.getMessage());
            return null;
        }
    }

    public Status getStatus() {
        return status;
    }

    /*public void setServerUploaded(boolean serverUploaded){
        this.serverUploaded = serverUploaded;
    }*/
    
    public boolean getServerUploadedAck(){
        return this.serverUploadedAck;
    }
    
    public void setServerUploadedAck(boolean serverUploadedAck) {
        this.serverUploadedAck = serverUploadedAck;
    }
    
    public void setServerUploadedTime(Date serverUploadedTime){
        this.serverUploadedTime = serverUploadedTime;
    }
    
    public void setStatus(Status status) {
        this.status = status;
    }

    public List<CloneChunk> getChunks() {
        return chunks;
    }

    public List<CloneChunk> getChunks(int fromIndex, int toIndex) {
        if (chunks.isEmpty()) {
            return new ArrayList<CloneChunk>();
        }

        if (fromIndex < 0) {
            fromIndex = 0;
        }
        if (toIndex > chunks.size() - 1) {
            toIndex = chunks.size() - 1;
        }

        return chunks.subList(fromIndex, toIndex);
    }

    public CloneChunk getChunk(int index) {
        return chunks.get(index);
    }

    public void setChunks(List<CloneChunk> chunks) {
        this.chunks = new ArrayList<CloneChunk>(chunks);
    }

    public void setChunk(int index, CloneChunk chunk) {
        chunks.remove(index);
        chunks.add(index, chunk);
    }

    public void addChunk(CloneChunk chunk) {
        chunks.add(chunk);
    }

    public void addChunks(List<CloneChunk> chunkList) {
        for (CloneChunk chunk : chunkList) {
            chunks.add(chunk);
        }
    }

    public synchronized void removeChunks(int count) {
        int minIndex = (chunks.size() - count - 1 < 0) ? 0 : chunks.size() - count - 1;
        int maxIndex = (chunks.size() - 1 < 0) ? 0 : chunks.size() - 1;

        for (int i = maxIndex; i == minIndex; i--) {
            chunks.remove(i);
        }
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (fileId != null ? fileId.hashCode() : 0);
        hash += version;
        return hash;
    }

    @Override
    public Object clone() {
        try {
            CloneFile clone = (CloneFile) super.clone();

            clone.fileId = getFileId();
            clone.updated = getUpdated();
            clone.checksum = getChecksum();
            clone.lastModified = new Date(getLastModified().getTime());
            clone.profile = getProfile(); // POINTER; No Copy!
            clone.profileId = getProfileId();
            clone.root = getRoot(); // POINTER; No Copy!
            clone.rootId = getRootId();
            clone.folder = isFolder();
            clone.filePath = getPath();
            clone.name = getName();
            clone.clientName = getClientName();
            clone.fileSize = getFileSize();
            clone.chunks = getChunks(); // POINTER; No Copy!
            clone.status = getStatus(); //TODO: is this ok?
            clone.syncStatus = getSyncStatus(); //TODO: is this ok?
            clone.parent = getParent(); // POINTER
            
            //clone.addChunks(getChunks()); // TODO is this ok??            
            
            //clone.serverUploaded = false;            
            clone.serverUploadedAck = false;
            clone.serverUploadedTime = null;
            
            return clone;
        } catch (Exception ex) {
            logger.error(ex);
            LogConfig.sendErrorLogs();

            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof CloneFile)) {
            return false;
        }

        CloneFile other = (CloneFile) object;

        if (other.fileId == null || this.fileId == null) {
            return false;
        }

        return other.fileId.equals(this.fileId) && other.version == this.version;
    }

    @Override
    public String toString() {
        String strPath = "/";
        if(filePath.compareTo("/") != 0){
            strPath = filePath;
        }
        
        return "CloneFile[id=" + fileId + ", version=" + version + ", name=" + strPath + name + " checksum=" + checksum + ", chunks=" + chunks.size() + ", status=" + status + ", syncStatus=" + syncStatus + ", workspace=" + workspace + "]";
    }

    public long getNewRandom() {
        return new Random().nextLong();
    }
    
    public Workspace getWorkspace() {
        return this.workspace;
    }
    
    public void setWorkspace(Workspace workspace){
        this.workspace = workspace;
    }
    
    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }
    
    public String getMimetype(){
        return this.mimetype;
    }
    
    private void setWorkspaceByPath(String path){
        
        while(this.workspace == null && path.length() > 0){
            try{                
                String queryStr = "select w from Workspace w where "
                      + "     w.pathWorkspace = :path";

                Query query = config.getDatabase().getEntityManager().createQuery(queryStr, Workspace.class);
                query.setHint("javax.persistence.cache.storeMode", "REFRESH");
                query.setHint("eclipselink.cache-usage", "DoNotCheckCache");                
                
                query.setMaxResults(1);
                query.setParameter("path", path);

                Workspace fileWorkspace = (Workspace) query.getSingleResult();
                this.workspace = fileWorkspace;
            } catch (NoResultException e){
                if(path.compareTo("/") != 0){
                    path = path.substring(0, path.lastIndexOf("/"));
                    if(path.isEmpty()){
                        path = "/";
                    }
                }else{
                    path = "";
                }                
            }
        }
    }
}