package naming;

import java.lang.String;
import java.util.Arrays;
import java.util.ArrayList;

public class NamingDirectory<String> {

    private DirectoryNode root;
    private ArrayList<DirectoryNode> tempChild;

    public NamingDirectory(String[] pathName) {
        // Construct tree with root node
        this.root = new DirectoryNode(pathName[0], null);
        this.tempChild = new ArrayList<DirectoryNode>();
    }

    public void addElement(DirectoryNode node, String[] pathName) {
        // Remember, only one unique path per level
        if (pathName.length == 1) {
            // Reached the leaf directory file
            System.out.println("We at the leaf directory!");

            if (node.getChildren().size()==0) {
                System.out.println("Some unique node has come " + pathName[0]);
                ArrayList<DirectoryNode> children = node.getChildren();
                children.add(new DirectoryNode(pathName[0], node));
                node.modifyChildren(children);
            } else {
                ArrayList<DirectoryNode> children = node.getChildren();
                for (DirectoryNode child : children) {
                    if (child.getData().equals(pathName[0])) {
                        // Exists till the last node
                        System.out.println("The entire path exists, needs to be handled");
                        return;
                    } else {
                        // A unique path has come, add it as new child to the current node
                        System.out.println("Some unique node has come " + pathName[0]);
                        this.tempChild.add(new DirectoryNode(pathName[0], node));
                        break;
                    }
                }
            }
            node.getChildren().addAll(tempChild);
            return;
        }

        if (node.getChildren().size()==0) {
            System.out.println("Some unique node has come " + pathName[0]);
            this.tempChild.add(new DirectoryNode(pathName[0], node));
            addElement(tempChild.get(0), Arrays.copyOfRange(pathName,1,pathName.length));
        } else {
            ArrayList<DirectoryNode> children = node.getChildren();
            for (DirectoryNode child : children) {
                if (child.getData().equals(pathName[0])) {
                    // First portion of incoming path exists, continue traversal
                    addElement(child, Arrays.copyOfRange(pathName,1,pathName.length));
                    return;
                } else {
                    // A unique path has come, add it as new child to the current node
                    System.out.println("Some unique node has come " + pathName[0]);
                    this.tempChild.add(new DirectoryNode(pathName[0], node));
                    addElement(tempChild.get(0), Arrays.copyOfRange(pathName,1,pathName.length));
                    break;
                }
            }
        }
        node.getChildren().addAll(tempChild);
        return;
    }

    public DirectoryNode getRoot() {
        return root;
    }
}