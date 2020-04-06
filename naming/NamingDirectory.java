package naming;

import java.lang.String;
import java.util.Arrays;
import java.util.ArrayList;
import storage.StorageServerInfo;
import java.io.IOException;

public class NamingDirectory {

    private DirectoryNode root;
    private boolean isUnique;
    private boolean directoryStatus;
    private boolean fileStatus;

    public NamingDirectory(String[] pathName) {
        // Construct tree with root node
        this.root = new DirectoryNode(pathName[0], null);
        this.isUnique = true;
        this.directoryStatus = false;
        this.fileStatus = false;
    }

    /**  Add a file to a directory */
    public void addElement(DirectoryNode node, String[] pathName) {
        ArrayList<DirectoryNode> tempChild = new ArrayList<DirectoryNode>();
        // Remember, only one unique path per level
        if (pathName.length == 1) {
            // Reached the leaf directory file

            if (node.getChildren().size()==0) {
                // There is a unique leaf node for the parent node
                ArrayList<DirectoryNode> children = node.getChildren();
                DirectoryNode child = new DirectoryNode(pathName[0], node);
                child.isFile = true;
                child.isDir = false;
                children.add(child);
                node.modifyChildren(children);
                this.isUnique = true;
            } else {
                ArrayList<DirectoryNode> children = node.getChildren();
                for (DirectoryNode child : children) {
                    if (child.getData().equals(pathName[0])) {
                        // Entire path exists till the last node.
                        this.isUnique = false;
                        return;
                    }
                }
                // A unique path has come, add it as new child to the current node
                DirectoryNode child = new DirectoryNode(pathName[0], node);
                child.isFile = true;
                child.isDir = false;
                tempChild.add(child);
                this.isUnique = true;
            }
            node.getChildren().addAll(tempChild);
            return;
        }

        if (node.getChildren().size()==0) {
            // Unique node has come in and needs to be added
            DirectoryNode child = new DirectoryNode(pathName[0], node);
            child.isFile = false;
            child.isDir = true;
            tempChild.add(child);

            addElement(tempChild.get(0), Arrays.copyOfRange(pathName,1,pathName.length));
        } else {
            ArrayList<DirectoryNode> children = node.getChildren();
            for (DirectoryNode child : children) {
                if (child.getData().equals(pathName[0])) {
                    // First portion of incoming path exists, continue traversal
                    addElement(child, Arrays.copyOfRange(pathName,1,pathName.length));
                    return;
                }
            }
            // A unique path has come, add it as new child to the current node
            DirectoryNode child = new DirectoryNode(pathName[0], node);
            child.isFile = false;
            child.isDir = true;
            tempChild.add(child);
            addElement(tempChild.get(0), Arrays.copyOfRange(pathName,1,pathName.length));
        }
        node.getChildren().addAll(tempChild);
        return;
    }

    /** Traverse through tree and pass the file node to delete exists */
    public DirectoryNode deleteFile(DirectoryNode node, String[] pathName) {
        if (pathName[0].equals("/")) {
            return node;
        }
        // Remember, only one unique path per level
        if (pathName.length == 1) {
            // Reached the leaf directory file

            if (node.getChildren().size()==0) {
                // Unique leaf node come in. So we cant delete and hence no return of directory
                return null;
            } else {
                ArrayList<DirectoryNode> children = node.getChildren();
                for (DirectoryNode child : children) {
                    if (child.getData().equals(pathName[0]) && child.getChildren().size()==0) {
                        // Path exists till the last node. We return the child whose path matches so that deletion can
                        // be carried out.
                        children.remove(child);
                        return child;
                    } else if (child.getData().equals(pathName[0]) && child.isDir) {
                        // This piece of code is triggered whenever the Path is a directory. We need to return the
                        // node back to the delete function to delete.
                        if (child.isDir && !child.isFile) {
                            children.remove(child);
                            return child;
                        }
                    }
                }
                return null;
            }
        }

        if (node.getChildren().size()==0) {
            return null;
        } else {
            ArrayList<DirectoryNode> children = node.getChildren();
            for (DirectoryNode child : children) {
                if (child.getData().equals(pathName[0])) {
                    // First portion of incoming path exists, continue traversal to delete
                    DirectoryNode r = deleteFile(child, Arrays.copyOfRange(pathName,1,pathName.length));
                    if (r != null) {
                        return r;
                    } else {
                        return null;
                    }
                }
            }
            return null;
        }
    }

    /** Traverse through tree to check if file exists */
    public boolean fileExists(DirectoryNode node, String[] pathName) {
        if (pathName[0].equals("/")) {
            return true;
        }
        // Remember, only one unique path per level
        if (pathName.length == 1) {
            // Reached the leaf directory file

            if (node.getChildren().size()==0) {
                // This is a unique node.
                return false;
            } else {
                ArrayList<DirectoryNode> children = node.getChildren();
                for (DirectoryNode child : children) {
                    if (child.getData().equals(pathName[0]) && child.getChildren().size()==0) {
                        // Path exisits completely.
                        this.fileStatus = child.isFile;
                        this.directoryStatus = child.isDir;
                        return true;
                    }
                }
                return false;
            }
        }

        if (node.getChildren().size()==0) {
            return false;
        } else {
            ArrayList<DirectoryNode> children = node.getChildren();
            for (DirectoryNode child : children) {
                if (child.getData().equals(pathName[0])) {
                    // First portion of incoming path exists, continue traversal
                    if (fileExists(child, Arrays.copyOfRange(pathName,1,pathName.length))) {
                        return true;
                    } else {
                        return false;
                    }
                }
            }
            return false;
        }
    }


    /** Traverse directory to check if it exists */
    public boolean dirExists(DirectoryNode node, String[] pathName) {
        if (pathName[0].equals("/")) {
            return true;
        }
        // Remember, only one unique path per level
        if (pathName.length == 1) {
            // Reached the leaf directory file

            if (node.getChildren().size()==0) {
                return false;
            } else {
                ArrayList<DirectoryNode> children = node.getChildren();
                for (DirectoryNode child : children) {
                    if (child.getData().equals(pathName[0]) && child.isDir) {
                        // Directory exists
                        if (child.isDir && !child.isFile) {
                            this.fileStatus = child.isFile;
                            this.directoryStatus = child.isDir;
                            return true;
                        }
                    }
                }
                return false;
            }
        }

        if (node.getChildren().size()==0) {
            return false;
        } else {
            ArrayList<DirectoryNode> children = node.getChildren();
            for (DirectoryNode child : children) {
                if (child.getData().equals(pathName[0])) {
                    // First portion of incoming path exists, continue traversal
                    if (dirExists(child, Arrays.copyOfRange(pathName,1,pathName.length))) {
                        return true;
                    } else {
                        return false;
                    }
                }
            }
            return false;
        }
    }

    public ArrayList<String> getFiles(DirectoryNode node, String[] pathName) {
        ArrayList<String> tempFiles = new ArrayList<String>();
        if (pathName[0].equals("/")) {
            ArrayList<DirectoryNode> children = node.getChildren();
            for (DirectoryNode child : children) {
                String filename = (String) child.getData();
                tempFiles.add(filename);
            }
            return tempFiles;
        }

        // At the leaf directories
        if (pathName.length == 1) {
            ArrayList<DirectoryNode> children = node.getChildren();
            for (DirectoryNode child : children) {
                if (child.getData().equals(pathName[0])) {
                    ArrayList<DirectoryNode> subchildren = child.getChildren();
                    for (DirectoryNode subchilds : subchildren) {
                            if (subchilds.isFile) {
                            tempFiles.add((String) subchilds.getData());
                            }
                    }
                    return tempFiles;
                }
            }
        }

        if (node.getChildren().size()==0) {
            return tempFiles;
        } else {
            ArrayList<DirectoryNode> children = node.getChildren();
            for (DirectoryNode child : children) {
                if (child.getData().equals(pathName[0])) {
                    return (getFiles(child, Arrays.copyOfRange(pathName,1,pathName.length)));
                }
            }
        }

        return tempFiles;
    }

    /**  Add a directory */
    public void addDirectory(DirectoryNode node, String[] pathName) {
        ArrayList<DirectoryNode> tempChild = new ArrayList<DirectoryNode>();
        // Remember, only one unique path per level
        if (pathName.length == 1) {
            // Reached the leaf directory file
            if (node.getChildren().size()==0) {
                // Unique Directory.
                ArrayList<DirectoryNode> children = node.getChildren();
                DirectoryNode child = new DirectoryNode(pathName[0], node);
                child.isFile = false;
                child.isDir = true;
                children.add(child);
                node.modifyChildren(children);
                this.isUnique = true;
            } else {
                ArrayList<DirectoryNode> children = node.getChildren();
                for (DirectoryNode child : children) {
                    if (child.getData().equals(pathName[0])) {
                        // Exists till the last node
                        this.isUnique = false;
                        return;
                    }
                }
                // A unique path has come, add it as new child to the current node
                DirectoryNode child = new DirectoryNode(pathName[0], node);
                child.isFile = false;
                child.isDir = true;
                tempChild.add(child);
                this.isUnique = true;
            }
            node.getChildren().addAll(tempChild);
            return;
        }

        if (node.getChildren().size()==0) {
            DirectoryNode child = new DirectoryNode(pathName[0], node);
            child.isFile = false;
            child.isDir = true;
            tempChild.add(child);

            addDirectory(tempChild.get(0), Arrays.copyOfRange(pathName,1,pathName.length));
        } else {
            ArrayList<DirectoryNode> children = node.getChildren();
            for (DirectoryNode child : children) {
                if (child.getData().equals(pathName[0])) {
                    // First portion of incoming path exists, continue traversal
                    addDirectory(child, Arrays.copyOfRange(pathName,1,pathName.length));
                    return;
                }
            }
            // A unique path has come, add it as new child to the current node
            DirectoryNode child = new DirectoryNode(pathName[0], node);
            child.isFile = false;
            child.isDir = true;
            tempChild.add(child);
            addDirectory(tempChild.get(0), Arrays.copyOfRange(pathName,1,pathName.length));
        }
        node.getChildren().addAll(tempChild);
        return;
    }

    /**  Add a Lock. Exclusive determines whether it is shared or exclusive
     * regServers help us to iterate through to find which server has what file. */
    public synchronized boolean addLock(DirectoryNode node, String[] pathName, boolean exclusive, int n, ArrayList<StorageServerInfo> regServers, String filepath) throws InterruptedException, IOException
    {
        ArrayList<DirectoryNode> tempChild = new ArrayList<DirectoryNode>();
        // Remember, only one unique path per level
        if (pathName.length == 1) {
            // Reached the leaf directory file
            ArrayList<DirectoryNode> children = node.getChildren();
            for (DirectoryNode child : children) {
                if (child.getData().equals(pathName[0])) {
                    // Path Exists till the last node and you need to lock depending on the flag.
                    if (exclusive == true)
                    {
                        child.lock.getWriteLock(n, regServers, filepath);
                    } else {
                        child.lock.getReadLock(n, regServers, filepath, true);
                    }
                    return true;
                }
            }
            return true;
        }
        ArrayList<DirectoryNode> children = node.getChildren();
        for (DirectoryNode child : children) {
            if (child.getData().equals(pathName[0])) {
                // First portion of incoming path exists, continue traversal
                child.lock.getReadLock(n, regServers, filepath, false);
                addLock(child, Arrays.copyOfRange(pathName,1,pathName.length), exclusive, n, regServers, filepath);
                return true;
            }
        }
        return true;
    }

    /**  Release a Lock. Exclusive determines whether it is shared or exclusive */
    public synchronized boolean releaseLock(DirectoryNode node, String[] pathName, boolean exclusive) throws InterruptedException
    {
        ArrayList<DirectoryNode> tempChild = new ArrayList<DirectoryNode>();
        // Remember, only one unique path per level
        if (pathName.length == 1) {
            // Reached the leaf directory file
            ArrayList<DirectoryNode> children = node.getChildren();
            for (DirectoryNode child : children) {
                if (child.getData().equals(pathName[0])) {
                    // Exists till the last node and now releaseLock
                    if (exclusive == true)
                    {
                        child.lock.releaseWriteLock();
                    }
                    else
                    {
                        child.lock.releaseReadLock();
                    }
                    return true;
                }
            }
            return true;
        }
        ArrayList<DirectoryNode> children = node.getChildren();
        for (DirectoryNode child : children) {
            if (child.getData().equals(pathName[0])) {
                // First portion of incoming path exists, continue traversal
                child.lock.releaseReadLock();
                releaseLock(child, Arrays.copyOfRange(pathName,1,pathName.length), exclusive);
                return true;
            }
        }
        return true;
    }

    public boolean isDirectoryStatus() {
        return this.directoryStatus;
    }

    public boolean isFileStatus() {
        return this.fileStatus;
    }

    public DirectoryNode getRoot() {
        return this.root;
    }

    public boolean isUnique() {
        return this.isUnique;
    }
}