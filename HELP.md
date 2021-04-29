#DirContentsApplication

- A CLI tool to read files and directories. A bit like  'ls' on Linux.
- A web API that does the same thing. You design this API how you want. It does not necessarily have a link with the CLI tool.
- A small UI application that consumes your web API.

## Web API
Run com.qohash.dirContents.DirContentsApplication in and call it with:

http://[server]:[port]/displayContents?dir=[encodedDir]

 The web API is in the form of [server]:[port]/displayContents?dir=[encodedDir]
 @param dir needs to be encoded
 @return a List of strings with the contents of the file

eg:

http://localhost:8080/displayContents?dir=%2Fhome%2Fmarco%2Fdev%2Fqohash%2Ftest

### DirListerTotalSize

This class implements the functionality to list a directory.
User java -jar DirListerTotalSize to access the CLI.
the jar o the class can be added to a given machine and an sh, eg. dirLister.sh
and this dirLister could be added to a util dir  that is referenced by the PATH
environment var, the sh should have the following call:
/////
java -jar DirListerTotalSize $1
////
then executing:

$ dirLister.sh [someDir]

would immediately produce the dir listing. Also

$dirLister.sh -ui

would call a UI file chooser component

$dirLister.sh -cli

will list all the files and request to type the file that we want to access at this level

