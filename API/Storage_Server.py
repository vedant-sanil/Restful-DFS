from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse
import json

from io import BytesIO
import sys, os

class SimpleHTTPRequestHandler(BaseHTTPRequestHandler):

    def do_GET(self):
        self.send_response(200)
        self.end_headers()
        self.wfile.write(b'Hello, world!')

    def do_POST(self):
        content_length = int(self.headers['Content-Length'])        
        body = self.rfile.read(content_length)
        print(self.path)
        d = json.loads(body)
        print(d["path"])
        if "storage_size" in self.path:
            self.storage_size(d)
        else:
            self.send_response(200)
            self.end_headers()
            response = BytesIO()
            response.write(b'This is POST request.\n ')
            response.write(b'Received:\n ')
            response.write(body)
            response.write(b'\n')
            self.wfile.write(response.getvalue())
    
    def storage_create(self, d):
        print("=====================================")
        response = {}
        print("FUNCTION CALL - Storage Size")
        try:
            size = os.path.getsize(d["path"])
            if size:
                print("Size is ",size)
            response["size"] = size
            response_json = json.dumps(response)
            self.send_response(200)
            self.end_headers()
            self.wfile.write(bytes(response_json,"utf-8"))
        except FileNotFoundError: 
            response["exception_type"] = "FileNotFoundException"
            response["exception_info"] = "FileNotFoundException: "+d["path"]+" cannot be found."
            response_json = json.dumps(response)
            print(response_json)
            self.send_response(404)
            self.end_headers()
            self.wfile.write(bytes(response_json,"utf-8"))
        except ValueError:
            response["exception_type"] = "IllegalArgumentException"
            response["exception_info"] = "IllegalArgumentException: "+d["path"]+" is invalid."
            response_json = json.dumps(response)
            self.send_response(404)
            self.end_headers()
            self.wfile.write(bytes(response_json,"utf-8"))
        print("=====================================")

    def storage_size(self, d):
        print("=====================================")
        response = {}
        print("FUNCTION CALL - Storage Size")
        try:
            size = os.path.getsize(d["path"])
            if size:
                print("Size is ",size)
            response["size"] = size
            response_json = json.dumps(response)
            self.send_response(200)
            self.end_headers()
            self.wfile.write(bytes(response_json,"utf-8"))
        except FileNotFoundError: 
            response["exception_type"] = "FileNotFoundException"
            response["exception_info"] = "FileNotFoundException: "+d["path"]+" cannot be found."
            response_json = json.dumps(response)
            print(response_json)
            self.send_response(404)
            self.end_headers()
            self.wfile.write(bytes(response_json,"utf-8"))
        except ValueError:
            response["exception_type"] = "IllegalArgumentException"
            response["exception_info"] = "IllegalArgumentException: "+d["path"]+" is invalid."
            response_json = json.dumps(response)
            self.send_response(404)
            self.end_headers()
            self.wfile.write(bytes(response_json,"utf-8"))
        print("=====================================")


httpd = HTTPServer(('localhost', 8080), SimpleHTTPRequestHandler)
httpd.serve_forever()
