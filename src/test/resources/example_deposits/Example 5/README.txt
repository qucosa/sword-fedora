Example 5: Upload a METS file

Upload a METS file that contains a file section with FLocat links of type URL.

In the client set the following:

File: location of the METS file
File Type: text/xml
Packaging: http://www.loc.gov/METS/

Results:

This will create one datastream for each file specified in the METS, one datastream per dmdSec in the METS, a datastream for the METS and a relationships datastream (as before the dc datastream will be copied from the METS).

RELS-EXT      Relationships to other objects      text/xml
METS          METS as it was deposited            text/xml
test          FILE                                application/msword
DC            Dublin Core Metadata                text/xml
MODS          MODS File                           text/xml
epdcx         epdcx File                          text/xml
