rm NamingServer*
rm StorageServer*
git add .
git commit -m "$1"
git push "https://$2:$3@github.com/sharath-sri-chellappa/Restful-DFS.git"
