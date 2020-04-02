package naming;

import java.lang.String;
import java.util.Arrays;
import java.util.ArrayList;

public class NamingDirectory<String> {

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

    /** Traverse through tree to check if file exists */
    public boolean fileExists(DirectoryNode node, String[] pathName) {
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
                    if (child.getData().equals(pathName[0]) && child.isDir) {
                        // Exists till the last node
                        System.out.println("We have reached the end!");
                        if (child.isDir && !child.isFile) {
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