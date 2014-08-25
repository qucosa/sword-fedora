Example 1: Generic Upload

Upload a file that is in the supported list for the collection but dosn't have a special handler. For example a gif file

In the client set the following:

File: location of the gif file
File Type: image/gif
Packaging: 

(Packaging should be left empty)


Results:

This will create an object with one datastream containing the image, one basic dc record and a relationships record

RELS-EXT       Relationships to other objects      text/xml
DC             Dublin Core Metadata                text/xml
uploaded       SWORD Generic File Upload           image/gif
