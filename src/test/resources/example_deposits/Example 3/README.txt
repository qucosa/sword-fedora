Example 3: ZIP Upload

Upload a zip file.

In the client set the following:

File: location of the zip file
File Type: application/zip
Packaging:

(Packaging should be left empty)


Results:

This will create one datastream per file in the zip deposit plus a basic dc record and a relationship record

mets         deposit/mets.xml                   text/xml
RELS-EXT     Relationships to other objects     text/xml
DC           Dublin Core Metadata               text/xml
test         deposit/test.doc                   application/msword
