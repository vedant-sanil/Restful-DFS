package naming;

import java.util.ArrayList;

public class DirectoryNode<String> {
    private String data;
    private DirectoryNode parent;
    private ArrayList<DirectoryNode> children;
    public boolean isDir;
    public boolean isFile;

    public DirectoryNode(String data, DirectoryNode parent) {
        this.data = data;
        this.parent = parent;
        this.children = new ArrayList<DirectoryNode>();
        this.isDir = false;
        this.isFile = false;
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