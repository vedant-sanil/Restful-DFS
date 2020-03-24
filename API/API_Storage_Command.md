# Storage Server API - Command

**Note**: The naming server uses this interface to communicate commands to the storage server.

Storage Server Host Name: localhost  
In all cases, if the API call receives invalid data, always return `400 Bad Request`.

------

## Create

**Description**: Creates a file on the storage server.

### request

**URL** : `/storage_create`

**Method** : `POST`

**Input Data** :

```json
{
    "path": "/dir/fileA"
}
```

*path*: Path to the file to be created. The parent directory will be created if it does not exist. This path may not be the root directory.  
(Please refer to the corresponding java class jsonhelper/PathRequest.java)

### response_1

**Code** : `200 OK`

**Content** :

```json
{
    "success": true
}
```

*success*: `true` if the file is created; `false` if it cannot be create.  
(Please refer to the corresponding java class jsonhelper/BooleanReturn.java)

### response_2

**Code** : `404 Not Found`

**Content** :

```json
{
    "exception_type": "IllegalArgumentException",
    "exception_info": "IllegalArgumentException: path invalid."
}
```

*exception_type*:

1. IllegalArgumentException. If the path is invalid.

*exception_info*: for your own debug purpose.  
(Please refer to the corresponding java class jsonhelper/ExceptionReturn.java)

------

## Delete

**Description**: Deletes a file or directory on the storage server.

> If the file is a directory and cannot be deleted, some, all, or none of its contents may be deleted by this operation.

### request

**URL** : `/storage_delete`

**Method** : `POST`

**Input Data** :

```json
{
    "path": "/storage/fileA"
}
```

*path*: Path to the file or directory to be deleted. The root directory cannot be deleted.  
(Please refer to the corresponding java class jsonhelper/PathRequest.java)

### response_1

**Code** : `200 OK`

**Content** :

```json
{
    "success": true
}
```

*success*: `true` if the file is deleted; `false` if it cannot be deleted.  
(Please refer to the corresponding java class jsonhelper/BooleanReturn.java)

### response_2

**Code** : `404 Not Found`

**Content** :

```json
{
    "exception_type": "IllegalArgumentException",
    "exception_info": "IllegalArgumentException: path invalid."
}
```

*exception_type*:

1. IllegalArgumentException. If the path is invalid

*exception_info*: for your own debug purpose.  
(Please refer to the corresponding java class jsonhelper/ExceptionReturn.java)

------

## Copy

**Description**: Copies a file from another storage server.

### request

**URL** : `/storage_copy`

**Method** : `POST`

**Input Data** :

```json
{
    "path": "/storage/fileA",
    "server_ip": "localhost",
    "server_port": 1111
}
```

*path*: Path to the file to be copied.  
*server_ip*: IP of the storage server that hosting the file.  
*server_port*: storage port of the storage server that hosting the file.  
(Please refer to the corresponding java class jsonhelper/CopyRequest.java)

### response_1

**Code** : `200 OK`

**Content** :

```json
{
    "success": true
}
```

*success*: `true` if the file is successfully copied; `false` otherwise.  
(Please refer to the corresponding java class jsonhelper/BooleanReturn.java)

### response_2

**Code** : `404 Not Found`

**Content** :

```json
{
    "exception_type": "FileNotFoundException",
    "exception_info": "FileNotFoundException: File/path cannot be found."
}
```

*exception_type*:

1. FileNotFoundException. If the file cannot be found or the path refers to a directory
2. IllegalArgumentException. If the path is invalid
3. IOException. If an I/O exception occurs either on the remote or on this storage server.

*exception_info*: for your own debug purpose.  
(Please refer to the corresponding java class jsonhelper/ExceptionReturn.java)
