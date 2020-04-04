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
            System.out.println("We at the leaf directory!");

            if (node.getChildren().size()==0) {
                System.out.println("A unique LEAF node has come when parent node : " + node.getData() + " has NO children : " + pathName[0]);
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
                    System.out.println("Here are children for parent node : " + node.getData() + " : " + child.getData());
                    if (child.getData().equals(pathName[0])) {
                        // Exists till the last node
                        System.out.println("The entire path exists, needs to be handled : " + pathName[0]);
                        this.isUnique = false;
                        return;
                    }
                }
                // A unique path has come, add it as new child to the current node
                System.out.println("A unique LEAF node has come when parent : " + node.getData() + "  HAS children : " + pathName[0]);
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
            System.out.println("A unique NODE has come when parent  : " + node.getData() + "  has zero children : " + pathName[0]);
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
                    System.out.println("A child exists for parent directory  : " + node.getData() + "  that already exists : " + pathName[0]);
                    addElement(child, Arrays.copyOfRange(pathName,1,pathName.length));
                    return;
                }
            }
            // A unique path has come, add it as new child to the current node
            System.out.println("A unique NODE has come when parent  : " + node.getData() + "  has children : " + pathName[0]);
            DirectoryNode child = new DirectoryNode(pathName[0], node);
            child.isFile = false;
            child.isDir = true;
            tempChild.add(child);
            addElement(tempChild.get(0), Arrays.copyOfRange(pathName,1,pathName.length));
        }
        node.getChildren().addAll(tempChild);
        return;
    }

    /** Traverse through tree and delete file exists */
    public DirectoryNode deleteFile(DirectoryNode node, String[] pathName) {
        if (pathName[0].equals("/")) {
            return node;
        }
        // Remember, only one unique path per level
        if (pathName.length == 1) {
            // Reached the leaf directory file
            //System.out.println("We at the leaf directory!");

            if (node.getChildren().size()==0) {
                System.out.println("A unique LEAF node has come when parent node : " + node.getData() + " has NO children : " + pathName[0]);
                return null;
            } else {
                ArrayList<DirectoryNode> children = node.getChildren();
                for (DirectoryNode child : children) {
                    System.out.println("Here are children for parent node : " + node.getData() + " : " + child.getData());
                    if (child.getData().equals(pathName[0]) && child.getChildren().size()==0) {
                        // Exists till the last node
                        System.out.println("We have reached the end!");
                        // Exists till the last node
                        children.remove(child);
                        return child;
                    } else if (child.getData().equals(pathName[0]) && child.isDir) {
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
                    // First portion of incoming path exists, continue traversal
                    DirectoryNode r = deleteFile(child, Arrays.copyOfRange(pathName,1,pathName.length));
                    System.out.println(r);
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
            //System.out.println("We at the leaf directory!");

            if (node.getChildren().size()==0) {
                System.out.println("A unique LEAF node has come when parent node : " + node.getData() + " has NO children : " + pathName[0]);
                return false;
            } else {
                ArrayList<DirectoryNode> children = node.getChildren();
                for (DirectoryNode child : children) {
                    System.out.println("Here are children for parent node : " + node.getData() + " : " + child.getData());
                    if (child.getData().equals(pathName[0]) && child.getChildren().size()==0) {
                        // Exists till the last node
                        System.out.println("We have reached the end!");
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
            //System.out.println("We at the leaf directory!");

            if (node.getChildren().size()==0) {
                return false;
            } else {
                ArrayList<DirectoryNode> children = node.getChildren();
                for (DirectoryNode child : children) {
                    System.out.println("Here are children for parent node : " + node.getData() + " : " + child.getData());
                    System.out.println("The file : " + pathName[0] + " is a directory : " + child.isDir);
                    if (child.getData().equals(pathName[0]) && child.isDir) {
                        // Exists till the last node
                        System.out.println("We have reached the end!");
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
            System.out.println("We at the leaf directory!");

            if (node.getChildren().size()==0) {
                System.out.println("A unique LEAF node has come when parent node : " + node.getData() + " has NO children : " + pathName[0]);
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
                    System.out.println("Here are children for parent node : " + node.getData() + " : " + child.getData());
                    if (child.getData().equals(pathName[0])) {
                        // Exists till the last node
                        System.out.println("The entire path exists, needs to be handled : " + pathName[0]);
                        this.isUnique = false;
                        return;
                    }
                }
                // A unique path has come, add it as new child to the current node
                System.out.println("A unique LEAF node has come when parent : " + node.getData() + "  HAS children : " + pathName[0]);
                DirectoryNode child = new DirectoryNode(pathName[0], node);
                System.out.println("WHY DOES THIS NOT PRINT The file : " + pathName[0] + " is a directory : " + child.isDir);
                child.isFile = false;
                child.isDir = true;
                tempChild.add(child);
                this.isUnique = true;
            }
            node.getChildren().addAll(tempChild);
            return;
        }

        if (node.getChildren().size()==0) {
            System.out.println("A unique NODE has come when parent  : " + node.getData() + "  has zero children : " + pathName[0]);
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
                    System.out.println("A child exists for parent directory  : " + node.getData() + "  that already exists : " + pathName[0]);
                    addDirectory(child, Arrays.copyOfRange(pathName,1,pathName.length));
                    return;
                }
            }
            // A unique path has come, add it as new child to the current node
            System.out.println("A unique NODE has come when parent  : " + node.getData() + "  has children : " + pathName[0]);
            DirectoryNode child = new DirectoryNode(pathName[0], node);
            child.isFile = false;
            child.isDir = true;
            tempChild.add(child);
            addDirectory(tempChild.get(0), Arrays.copyOfRange(pathName,1,pathName.length));
        }
        node.getChildren().addAll(tempChild);
        return;
    }

    public synchronized boolean addLock(DirectoryNode node, String[] pathName, boolean exclusive, int n, ArrayList<StorageServerInfo> regServers, String filepath) throws InterruptedException, IOException
    {
        ArrayList<DirectoryNode> tempChild = new ArrayList<DirectoryNode>();
        // Remember, only one unique path per level
        if (pathName.length == 1) {
            // Reached the leaf directory file
            System.out.println("We at the leaf directory!");
            ArrayList<DirectoryNode> children = node.getChildren();
            for (DirectoryNode child : children) {
                System.out.println("Here are children for parent node : " + node.getData() + " : " + child.getData());
                if (child.getData().equals(pathName[0])) {
                    // Exists till the last node
                    System.out.println("The entire path exists, needs to be handled : " + pathName[0]);
                    if (exclusive == true)
                    {
                        System.out.println(pathName[0] + " gets a writeLock");
                        child.lock.getWriteLock(n, regServers, filepath);
                    } else {
                        System.out.println(pathName[0] + " gets a readLock");
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
                System.out.println(pathName[0] + " gets a readLock");
                child.lock.getReadLock(n, regServers, filepath, false);
                addLock(child, Arrays.copyOfRange(pathName,1,pathName.length), exclusive, n, regServers, filepath);
                return true;
            }
        }
        return true;
    }

    public synchronized boolean releaseLock(DirectoryNode node, String[] pathName, boolean exclusive) throws InterruptedException
    {
        ArrayList<DirectoryNode> tempChild = new ArrayList<DirectoryNode>();
        // Remember, only one unique path per level
        if (pathName.length == 1) {
            // Reached the leaf directory file
            System.out.println("We at the leaf directory!");
            ArrayList<DirectoryNode> children = node.getChildren();
            for (DirectoryNode child : children) {
                System.out.println("Here are children for parent node : " + node.getData() + " : " + child.getData());
                if (child.getData().equals(pathName[0])) {
                    // Exists till the last node
                    System.out.println("The entire path exists, needs to be handled : " + pathName[0]);
                    if (exclusive == true)
                    {
                        System.out.println(pathName[0] + " releases a writeLock");
                        child.lock.releaseWriteLock();
                    }
                    else
                    {
                        child.lock.releaseReadLock();
                        System.out.println(pathName[0] + " releases a readLock");
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
                System.out.println("A child exists for parent directory  : " + node.getData() + "  that already exists : " + pathName[0]);
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