# Naming Server API - Registration

**Note**: This API is used once, on startup, by each storage server.

Naming Server Host Name: localhost  
Naming Server Registration Port: 8090  
In all cases, if the API call receives invalid data (there is error when parse the request), always return `400 Bad Request`.

------

## Register

**Description**: Registers a storage server with the naming server.

> The storage server notifies the naming server of the files that it is hosting.  
> Note that the storage server does not notify the naming server of any directories.  
> The naming server attempts to add as many of these files as possible to its directory tree.  
> The naming server then replies to the storage server with a subset of these files that the storage server must delete from its local storage.  

> After the storage server has deleted the files as commanded,  
> it must prune its directory tree by removing all directories under which no files can be found.  
> This includes, for example, directories which contain only empty directories.  

> Registration requires the naming server to lock the root directory for exclusive access.  
> Therefore, it is best done when there is not heavy usage of the filesystem.

### request

**URL** : `/register`

**Method** : `POST`

**Input Data** :

```json
{
    "storage_ip": "localhost",
    "client_port": 1111,
    "command_port": 2222,
    "files": [
        "/file",
        "/storage/fileA",
        "/storage/dir1/fileB",
        "/storage/dir1/dir2/fileC"
    ]
}
```

*storage_ip*: storage server IP address.  
*client_port*: storage server port listening for the requests from client (aka storage port).  
*command_port*: storage server port listening for the requests from the naming server.  
*files*: list of files stored on the storage server. This list is merged with the directory tree already present on the naming server. Duplicate filenames are dropped.  
(Please refer to the corresponding java class jsonhelper/RegisterRequest.java)

### response_1

**Code** : `200 OK`

**Content** :

```json
{
    "files": [
        "/storage/dir1/dir2/fileC",
        "/file"
    ]
}
```

*files*: return a list of duplicate files to delete on the local storage of the registering storage server.  
(Please refer to the corresponding java class jsonhelper/FilesReturn.java)

### response_2

**Code** : `409 Conflict`

**Content** :

```json
{
    "exception_type": "IllegalStateException",
    "exception_info": "This storage client already registered."
}
```

*exception_type*:

1. IllegalStateException. If the path is invalid.

*exception_info*: for your own debug purpose.  
(Please refer to the corresponding java class jsonhelper/ExceptionReturn.java)
