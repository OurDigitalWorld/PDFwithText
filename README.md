PDFwithText
===========

This project supports the use of positional text within a PDF file that
displays an image. Our use case is for newspapers but the same approach
could be used for book pages and other types of materials. The PDF handling
has been done with the [iText](http://itextpdf.com/) library, and the
project is built with [Maven](https://maven.apache.org). The pieces can be
pulled together with:

```
mvn assembly:assembly
```

The jar with all of the needed libraries should end up in the _target_
directory and everything is brought together in _PDFwithText-exe.jar_. ODW
uses a simple XML format for OCR text that provides coordinates for
individual terms on an image:

    <word x1="1973" y1="725">november<ends x2="2453" y2="777"/></word>

The command line options are:

    usage: PDFwithText
    -b,--black            set page background colour to black.
    -h,--help             show help.
    -i,--input <arg>      input image (required).
    -o,--output <arg>     output PDF file (default name from image).
    -p,--pagesize <arg>   L - LETTER (default), T - TABLOID, A - A4.
    -v,--verbose          show underlying text on image.
    -x,--xmlfile <arg>    specify XML file (default name from image)

For example:

```
java -jar PDFwithText-exe.jar -b -i 1935-01-03-0001.jpg
```

This puts a black background on the image, and only specifies an input file. In 
this scenario, the XML file has the same name format (_1935-01-03-0001.xml_), 
and the resulting PDF file has a similar pattern (_1935-01-03-0001.pdf_).
The XML file and output PDF file can be specified directly if different
naming conventions are used.

art rhyno [ourdigitalworld/cdigs] (https://github.com/artunit)

