Example 4: ZIP Upload with METS Manifest

Upload a zip file that contains a METS file called mets.xml.

In the client set the following:

File: location of the zip file
File Type: application/zip
Packaging: http://www.loc.gov/METS/


Results:

This will create one datastream per file in the METS document, one datastream per dmdSec in the METS, a datastream for the METS document and a relationships datastream (the dc datastream will be copied from the METS).

RELS-EXT      Relationships to other objects   text/xml
METS          METS as it was deposited         text/xml
test          FILE                             application/msword
DC            Dublin Core Metadata             text/xml
MODS          MODS File                        text/xml
epdcx         epdcx File                       text/xml
