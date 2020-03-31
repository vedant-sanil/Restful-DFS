package naming;

import java.util.ArrayList;
import java.io.*;
import java.util.*;
import java.lang.*;

public class DirectoryNode {
    private String data;
    private DirectoryNode parent;
    private ArrayList<DirectoryNode> children;

    public DirectoryNode(String data, DirectoryNode parent) {
        this.data = data;
        this.parent = parent;
        this.children = new ArrayList<DirectoryNode>();
    }

    public String getData() {
        return this.data;
    }

    public ArrayList<DirectoryNode> getChildren() {
        return this.children;
    }

    public DirectoryNode getParent() {
        return this.parent;
    }

    public void modifyChildren(ArrayList<DirectoryNode> children) {
        this.children = children;
    }
}