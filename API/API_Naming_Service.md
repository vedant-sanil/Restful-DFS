# Naming Server API - Service

**Note**: This is the API through which clients access the naming server.

Naming Server Host Name: localhost  
Naming Server Service Port: 8080  
In all cases, if the API call receives invalid data, always return `400 Bad Request`.

------

## IsValidPath

**Description**: Given a Path string, return whether it is a valid path.

> The path string should be a sequence of components delimited with forward slashes. Empty components are dropped. The string must begin with a forward slash. And the string must not contain any colon character.

### request

**URL** : `/is_valid_path`

**Method** : `POST`

**Input Data** :

```json
{
    "path": "/storage/fileA"
}
```

*path*: The path string.  
(Please refer to the corresponding java class jsonhelper/PathRequest.java)

### response_1

**Code** : `200 OK`

**Content** :

```json
{
    "success": true
}
```

*success*: `true` if the path is valid. `false` if the path is invalid.  
(Please refer to the corresponding java class jsonhelper/BooleanReturn.java)

------

## GetStorage

**Description**: Returns the IP and port info of the storage server that hosting the file.

> If the client intends to perform calls only to `read` or `size` after obtaining the storage server stub,
> it should lock the file for shared access before making this call.
> If it intends to perform calls to `write`, it should lock the file for exclusive access.  

### request

**URL** : `/getstorage`

**Method** : `POST`

**Input Data** :

```json
{
    "path": "/storage/fileA"
}
```

*path*: path to the file.  
(Please refer to the corresponding java class jsonhelper/PathRequest.java)

### response_1

**Code** : `200 OK`

**Content** :

```json
{
    "server_ip": "localhost",
    "server_port": 1111
}
```

*server_ip*: IP of the storage server that hosting the file.  
*server_port*: storage port of the storage server that hosting the file. (client could use this port to make request to the storage server to do things like read, write, size, ... )  
(Please refer to the corresponding java class jsonhelper/ServerInfo.java)

### response_2

**Code** : `404 Not Found`

**Content** :

```json
{
    "exception_type": "FileNotFoundException",
    "exception_info": "File/path cannot be found."
}
```

*exception_type*:

1. FileNotFoundException. If the file does not exist.
2. IllegalArgumentException. If the path is invalid.

*exception_info*: for your own debug purpose.  
(Please refer to the corresponding java class jsonhelper/ExceptionReturn.java)

------

## Delete

**Description**: Deletes a file or directory.  

> The parent directory should be locked for exclusive access before this operation is performed.  

### request

**URL** : `/delete`

**Method** : `POST`

**Input Data** :

```json
{
    "path": "/path/to/a/file/or/dir"
}
```

*path*: Path to the file or directory to be deleted.  
(Please refer to the corresponding java class jsonhelper/PathRequest.java)

### response_1

**Code** : `200 OK`

**Content** :

```json
{
    "success": true
}
```

*success*: return `true` if the file or directory is successfully deleted, return `false` otherwise. The root directory cannot be deleted.  
(Please refer to the corresponding java class jsonhelper/BooleanReturn.java)

### response_2

**Code** : `404 Not Found`

**Content** :

```json
{
    "exception_type": "FileNotFoundException",
    "exception_info": "the object or parent directory does not exist."
}
```

*exception_type*:

1. FileNotFoundException. If the file or parent directory does not exist.
2. IllegalArgumentException. If the given path is invalid.

*exception_info*: for your own debug purpose.  
(Please refer to the corresponding java class jsonhelper/ExceptionReturn.java)

------

## CreateDirectory

**Description**: Creates the given directory, if it does not exist.

> The parent directory should be locked for exclusive access before this operation is performed.

### request

**URL** : `/create_directory`

**Method** : `POST`

**Input Data** :

```json
{
    "path": "/path/to/dir"
}
```

*path*: Path at which the directory is to be created.  
(Please refer to the corresponding java class jsonhelper/PathRequest.java)

### response_1

**Code** : `200 OK`

**Content** :

```json
{
    "success": true
}
```

*success*: `true` if the directory is created successfully, `false` otherwise. The directory is not created if a file or directory with the given name already exists.  
(Please refer to the corresponding java class jsonhelper/BooleanReturn.java)

### response_2

**Code** : `404 Not Found`

**Content** :

```json
{
    "exception_type": "FileNotFoundException",
    "exception_info": "parent directory does not exist."
}
```

*exception_type*:

1. FileNotFoundException. If the parent directory does not exist.
2. IllegalArgumentException. If the given path is invalid.

*exception_info*: for your own debug purpose.  
(Please refer to the corresponding java class jsonhelper/ExceptionReturn.java)

------

## CreateFile

**Description**: Creates the given file, if it does not exist.  

> The parent directory should be locked for exclusive access before this operation is performed.

### request

**URL** : `/create_file`

**Method** : `POST`  

**Input Data** :

```json  
{
    "path": "/path/to/file"
}
```

*path*:  Path at which the file is to be created.  
(Please refer to the corresponding java class jsonhelper/PathRequest.java)

### response_1

**Code** : `200 OK`

**Content** :

```json
{
    "success": true
}
```

*success*: `true` if the file is created successfully. `false` otherwise. The file is not created if a file or directory with the given name already exists.  
(Please refer to the corresponding java class jsonhelper/BooleanReturn.java)

### response_2

**Code** : `404 Not Found`

**Content** :

```json
{
    "exception_type": "FileNotFoundException",
    "exception_info": "parent directory does not exist"
}
```

*exception_type*:

1. FileNotFoundException. If the parent dir does not exist.
2. IllegalArgumentException. If the given path is invalid.

*exception_info*: for your own debug purpose.  
(Please refer to the corresponding java class jsonhelper/ExceptionReturn.java)

### response_3

**Code** : `409 Conflict`

**Content** :

```json
{
    "exception_type": "IllegalStateException",
    "exception_info": "no storage servers are connected to the naming server."
}
```

*exception_type*:

1. IllegalStateException. If no storage servers are connected to the naming server.

*exception_info*: for your own debug purpose.  
(Please refer to the corresponding java class jsonhelper/ExceptionReturn.java)

------

## List

**Description**: Lists the contents of a directory.

> The directory should be locked for shared access before this operation is performed,
> because this operation reads the directory's child list.  

### request

**URL** : `/list`

**Method** : `POST`

**Input Data** :

```json  
{
    "path": "/path/to/dir"
}
```

*path*: the directory to be listed.  
(Please refer to the corresponding java class jsonhelper/PathRequest.java)

### response_1

**Code** : `200 OK`

**Content** :

```json
{
    "files": [
        "/dir/file1",
        "/dir/file2",
        "/dir/file3"
    ]
}
```

*files*: An array of the directory entries. The entries are not guaranteed to be in any particular order.  
(Please refer to the corresponding java class jsonhelper/FilesReturn.java)

### response_2

**Code** : `404 Not Found`

**Content** :

```json
{
    "exception_type": "FileNotFoundException",
    "exception_info": "given path does not refer to a directory."
}
```

*exception_type*:

1. FileNotFoundException. If the given path does not refer to a directory.
2. IllegalArgumentException. If the given path is invalid.

*exception_info*: for your own debug purpose.  
(Please refer to the corresponding java class jsonhelper/ExceptionReturn.java)

------

## IsDirectory

**Description**: Determines whether a path refers to a directory.  

> The parent directory should be locked for shared access before this operation is performed.
> This is to prevent the object in question from being deleted or re-created while this call is in progress.

### request

**URL** : `/is_directory`

**Method** : `POST`

**Input Data** :

```json  
{
    "path": "/path/to/be/checked"
}
```

*path*: the path to be checked.  
(Please refer to the corresponding java class jsonhelper/PathRequest.java)

### response_1

**Code** : `200 OK`

**Content** :

```json
{
    "success": true
}
```

*success*: `true` if the path is a directory. `false` if the path is a file.  
(Please refer to the corresponding java class jsonhelper/BooleanReturn.java)

### response_2

**Code** : `404 Not Found`

**Content** :

```json
{
    "exception_type": "FileNotFoundException",
    "exception_info": "File/path cannot be found."
}
```

*exception_type*:

1. FileNotFoundException. If the object specified by path cannot be found.
2. IllegalArgumentException. If the given path is invalid.

*exception_info*: for your own debug purpose.  
(Please refer to the corresponding java class jsonhelper/ExceptionReturn.java)

------

## Unlock

**Description**: Unlocks a file or directory.

### request

**URL** : `/unlock`

**Method** : `POST`

**Input Data** :

```json  
{
    "path": "/path/to/dir/or/file",
    "exclusive": true
}
```

*path*: The file or directory to be unlocked.  
*exclusive*: Must be `true` if the object was locked for exclusive access, and `false` if it was locked for shared access.  
(Please refer to the corresponding java class jsonhelper/LockRequest.java)

### response_1

**Code** : `200 OK`

**Content** : empty (If unlock successfully, the response body should be empty.)

### response_2

**Code** : `404 Not Found`

**Content** :

```json
{
    "exception_type": "IllegalArgumentException",
    "exception_info": "path cannot be found."
}
```

*exception_type*:

1. IllegalArgumentException. If the path is invalid or cannot be cound. This is a client programming error, as the path must have previously been locked, and cannot be removed while it is locked.

*exception_info*: for your own debug purpose.  
(Please refer to the corresponding java class jsonhelper/ExceptionReturn.java)

------

## Lock

**Description**: Locks a file or directory for either shared or exclusive access.

> An object locked for **exclusive** access cannot be locked by any other user until the exclusive lock is released.  
> An object should be locked for exclusive access when operations performed by the user will change the object's state.

> An object locked for **shared** access can be locked by other users for shared access at the same time,  
> but cannot be simultaneously locked by users requesting exclusive access.  
> This kind of lock should be obtained when the object's state will be consulted, but not modified,  
> and to prevent the object from being modified by another user.

> Wherever there is a requirement that an object be locked for shared access, it is acceptable to lock the object for exclusive access  
> instead: exclusive access is more "safe" than shared access.  
> However, it is best to avoid this unless absolutely necessary, to permit as many users simultaneous access to the object as safely possible.

> Locking a file for shared access is considered by the naming server to be a read request, and may cause the file to be replicated.  
> Locking a file for exclusive access is considered to be a write request, and causes all copies of the file but one to be deleted.  
> This latter process is called invalidation.  
> The naming server must treat lock actions as read or write requests because it cannot monitor the true read and write requests - those go to the storage servers.

> When any object is locked for either kind of access, all objects along the path up to, but not including, the object itself,  
> are locked for shared access to prevent their modification or deletion by other users.  
> For example, if one user locks `/etc/scripts/startup.sh` for exclusive access in order to write to it,  
> then `/`, `/etc`, `/etc/scripts` will all be locked for shared access to prevent other users from, say, deleting them.

> An object can be considered to be **effectively locked** for exclusive access if one of the directories on the path to it is already locked for exclusive access:  
> this is because no user will be able to obtain any kind of lock on the object until the exclusive lock on the directory is released.  
> This is a direct consequence of the locking order described in the previous paragraph.  
> As a result, if a directory is locked for exclusive access, the entire subtree under that directory can also be considered to be locked for exclusive access.  
> If a client takes advantage of this fact to lock a directory and then perform several accesses to the files under it,  
> it should take care not to access files for writing: this may cause the naming server to miss true write requests to those files,  
> and cause the naming server to fail to request that stale copies of the file be invalidated.

> A minimal amount of fairness is guaranteed with locking: users are served in first-come first-serve order, with a slight modification:  
> users requesting shared access are granted the lock simultaneously.  
> As a consequence of the lock service order, if at least one exclusive user is already waiting for the lock,  
> subsequent users requesting shared access must wait until that user has released the lock - even if the lock is currently taken for shared access.  
> For example, suppose users `A` and `B` both currently hold the lock with shared access. User `C` arrives and requests exclusive access.  
> User `C` is then placed in a queue. If another user, `D`, arrives and requests shared access, he is not permitted to take the lock immediately,  
> even though it is currently taken by `A` and `B` for shared access. User `D` must wait until `C` is done with the lock.

### request

**URL** : `/lock`

**Method** : `POST`

**Input Data** :

```json  
{
    "path": "/path/to/file/or/dir",
    "exclusive": true
}
```

*path*: The file or directory to be locked.
*exclusive*: If `true`, the object is to be locked for exclusive access. Otherwise, it is to be locked for shared access.  
(Please refer to the corresponding java class jsonhelper/LockRequest.java)

### response_1

**Code** : `200 OK`

**Content** : empty (If lock successfully, the response body should be empty.)

### response_2

**Code** : `404 Not Found`

**Content** :

```json
{
    "exception_type": "FileNotFoundException",
    "exception_info": "path cannot be found."
}
```

*exception_type*:

1. FileNotFoundException. If the object specified by path cannot be found.
2. IllegalArgumentException. If the path is invalid.

*exception_info*: for your own debug purpose.  
(Please refer to the corresponding java class jsonhelper/ExceptionReturn.java)
