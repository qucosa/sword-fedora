Example 2: Upload to object with Disseminator
Upload a file that is in the supported list for the collection and uses the JpegHandler. For example a jpeg file

In the client set the following:

File: location of the jpg file
File Type: image/jpeg
Packaging: 

(Packaging should be left empty)
(File Type needs to be image/jpeg. A file type of image/jpg won't work)


Results:

This will create an object with a disseminator bdef demo:1 and bmech demo:2

RELS-EXT      Relationships to other objects      text/xml
DC            Dublin Core Metadata                text/xml
uploaded      SWORD Generic File Upload           image/jpeg
