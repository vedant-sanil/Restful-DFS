  package naming;

import java.util.ArrayList;

public class DirectoryNode<String> {

    // Contains the directory name
    private String data;

    // Recording the parent as well as the children for the current node.
    private DirectoryNode parent;
    private ArrayList<DirectoryNode> children;

    // Signifies whether the Node is a File or a Directory.
    public boolean isDir;
    public boolean isFile;

    // Lock for each Node.
    public RWLocks lock;

    public DirectoryNode(String data, DirectoryNode parent) {
        this.data = data;
        this.parent = parent;
        this.children = new ArrayList<DirectoryNode>();
        this.isDir = false;
        this.isFile = false;
        this.lock = new RWLocks();
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