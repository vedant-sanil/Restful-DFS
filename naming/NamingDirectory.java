package naming;

import java.lang.String;
import java.lang.*;
import java.util.Arrays;
import java.util.ArrayList;
import java.io.*;
import java.util.*;


public class NamingDirectory {

    private DirectoryNode root;
    private ArrayList<DirectoryNode> tempChild;

    public NamingDirectory(String[] pathName) {
        // Construct tree with root node
        this.root = new DirectoryNode(pathName[0], null);
        this.tempChild = new ArrayList<DirectoryNode>();
    }

    public boolean addElement(DirectoryNode node, String[] pathName, boolean flag) {
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
//                        StringBuilder builder = new StringBuilder();
//                        for(String s : pathName) {
//                            builder.append(s);
//                            builder.append('/');
//                        }
//                        String joinedString = builder.toString();
//                        System.out.println("REPEATED - "+joinedString);
//                        repeated_list.add(joinedString);
                        flag = false;
                        System.out.println("Flag is "+flag);
                        return flag;
                    } else {
                        // A unique path has come, add it as new child to the current node
                        System.out.println("Some unique node has come " + pathName[0]);
                        this.tempChild.add(new DirectoryNode(pathName[0], node));
                        break;
                    }
                }
            }
            node.getChildren().addAll(tempChild);
            System.out.println("Flag is "+flag);
            return flag;
        }

        if (node.getChildren().size()==0) {
            System.out.println("Some unique node has come " + pathName[0]);
            this.tempChild.add(new DirectoryNode(pathName[0], node));
            flag = addElement(tempChild.get(0), Arrays.copyOfRange(pathName,1,pathName.length), flag);
            System.out.println("Flag is "+flag);
        } else {
            ArrayList<DirectoryNode> children = node.getChildren();
            for (DirectoryNode child : children) {
                if (child.getData().equals(pathName[0])) {
                    // First portion of incoming path exists, continue traversal
                    flag = addElement(child, Arrays.copyOfRange(pathName,1,pathName.length), flag);
                    System.out.println("Flag is "+flag);
                    return flag;
                } else {
                    // A unique path has come, add it as new child to the current node
                    System.out.println("Some unique node has come " + pathName[0]);
                    this.tempChild.add(new DirectoryNode(pathName[0], node));
                    flag = addElement(tempChild.get(0), Arrays.copyOfRange(pathName,1,pathName.length), flag);
                    break;
                }
            }
        }
        node.getChildren().addAll(tempChild);
        System.out.println("Flag is "+flag);
        return flag;
    }

    public DirectoryNode getRoot() {
        return root;
    }
}