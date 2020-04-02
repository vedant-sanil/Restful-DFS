package naming;

import java.lang.String;
import java.util.Arrays;
import java.util.ArrayList;

public class NamingDirectory<String> {

    private DirectoryNode root;
    private boolean isUnique;

    public NamingDirectory(String[] pathName) {
        // Construct tree with root node
        this.root = new DirectoryNode(pathName[0], null);
        this.isUnique = true;
    }

    public void addElement(DirectoryNode node, String[] pathName) {
        ArrayList<DirectoryNode> tempChild = new ArrayList<DirectoryNode>();
        // Remember, only one unique path per level
        if (pathName.length == 1) {
            // Reached the leaf directory file
            System.out.println("We at the leaf directory!");

            if (node.getChildren().size()==0) {
                System.out.println("A unique LEAF node has come when parent node : " + node.getData() + " has NO children : " + pathName[0]);
                ArrayList<DirectoryNode> children = node.getChildren();
                children.add(new DirectoryNode(pathName[0], node));
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
                tempChild.add(new DirectoryNode(pathName[0], node));
                this.isUnique = true;
            }
            node.getChildren().addAll(tempChild);
            return;
        }

        if (node.getChildren().size()==0) {
            System.out.println("A unique NODE has come when parent  : " + node.getData() + "  has zero children : " + pathName[0]);
            tempChild.add(new DirectoryNode(pathName[0], node));
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
            tempChild.add(new DirectoryNode(pathName[0], node));
            addElement(tempChild.get(0), Arrays.copyOfRange(pathName,1,pathName.length));
        }
        node.getChildren().addAll(tempChild);
        return;
    }

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

    public DirectoryNode getRoot() {
        return this.root;
    }

    public boolean isUnique() {
        return this.isUnique;
    }
}