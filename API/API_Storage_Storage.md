# Storage Server API - Storage

**Note**: This is the API through which clients access the storage server.

Storage Server Host Name: localhost  
In all cases, if the API call receives invalid data, always return `400 Bad Request`.

------

## Size

**Description**: Returns the length of a file, in bytes.

### request

**URL** : `/storage_size`

**Method** : `POST`

**Input Data** :

```json
{
    "path": "/dir/fileA"
}
```

*path*: path to the file.  
(Please refer to the corresponding java class jsonhelper/PathRequest.java)

### response_1

**Code** : `200 OK`

**Content** :

```json
{
    "size": 1111
}
```

*size*: The length of the file.  
(Please refer to the corresponding java class jsonhelper/SizeReturn.java)

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

1. FileNotFoundException. If the file cannot be found or the path refers to a directory.
2. IllegalArgumentException. If the path is invalid.

*exception_info*: for your own debug purpose.  
(Please refer to the corresponding java class jsonhelper/ExceptionReturn.java)

------

## Read

**Description**: Reads a sequence of bytes from a file.

### request

**URL** : `/storage_read`

**Method** : `POST`

**Input Data** :

```json
 {
    "path": "/dir/fileA",
    "offset": 2222,
    "length": 3333
 }
```

*path*: Path to the file.  
*offset*: Offset into the file to the beginning of the sequence.  
*length*: The number of bytes to be read.  
(Please refer to the corresponding java class jsonhelper/ReadRequest.java)

### response_1

**Code** : `200 OK`

**Content** :

```json
{
    "data": "kaljsdbojackhorsemanklajemke"
}
```

*data*: Bytes read with Base64 encoding. Normally, the return should be a byte array containing the bytes read (To accommodate to JSON, the bytes should be encoded to a Base64 string). If the call succeeds, the number of bytes read is equal to the number of bytes requested.  
(Please refer to the corresponding java class jsonhelper/DataReturn.java)

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
2. IndexOutOfBoundsException. If the sequence specified by `offset` and `length` is outside the bounds of the file, or if `length` is negative.
3. IOException. If the file read cannot be completed on the server.

*exception_info*: for your own debug purpose.  
(Please refer to the corresponding java class jsonhelper/ExceptionReturn.java)

------

## Write

**Description**: Writes bytes to a file

### request

**URL** : `/storage_write`

**Method** : `POST`

**Input Data** :

```json
{
    "path": "/dir/fileA",
    "offset": 2222,
    "data": "kljasdarickandmortyaklsdea"
}
```

*path*: Path at which the directory is to be created.  
*offset*: Offset into the file where data is to be written.  
*data*: Base64 representation of data to be written.  
(Please refer to the corresponding java class jsonhelper/WriteRequest.java)

### response_1

**Code** : `200 OK`

**Content** :

```json
{
    "success": true
}
```

*success*: whether the file is successfully writed.  
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

1. FileNotFoundException. If the file cannot be found or the path refers to a directory.
2. IndexOutOfBoundsException. If `offset` is negative.
3. IOException. If the file write cannot be completed on the server.

*exception_info*: for your own debug purpose.  
(Please refer to the corresponding java class jsonhelper/ExceptionReturn.java)
