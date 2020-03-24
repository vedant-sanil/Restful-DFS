package test.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

public class Path {
    private String path;

    public Path(String path) {
        this.path = path;
    }

    public Path() {
        this.path = "";
    }

    public Path(Path path, String component) {
        this.path = path.toString() + "/" + component;
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public boolean equals(Object o) {
       if (!(o instanceof Path)) {
           return false;
       }
       Path other = (Path) o;
       return other.path.equals(path);
    }

    @Override
    public String toString() {
       return path;
    }

    /** Lists the paths of all files in a directory tree on the local
        filesystem.

        @param directory The root directory of the directory tree.
        @return An array of relative paths, one for each file in the directory
                tree.
        @throws FileNotFoundException If the root directory does not exist.
        @throws IllegalArgumentException If <code>directory</code> exists but
                                         does not refer to a directory.
     */
    public static Path[] list(File directory) throws FileNotFoundException
    {
        ArrayList<Path> result = new ArrayList<Path>();
        Path.path_traversal(directory, new Path(), result);
        return result.toArray(new Path[0]);
    }
    
    private static void path_traversal(File directory, Path curr_path, ArrayList<Path> pathList) throws FileNotFoundException {
    	File[] files = directory.listFiles();
    	for (File file : files) {
    		if (file.isFile()) {
    			pathList.add(new Path(curr_path, file.getName()));
    		}
    		else {
    			path_traversal(file, new Path(curr_path, file.getName()), pathList);
    		}
    	}
    }
}
