package org.conifer.odw;

/*
    PDFwithText.java
 
    - art rhyno <http://ourdigitalworld.org/>
    (c) Copyright GNU General Public License (GPL)

*/

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;

import com.itextpdf.text.pdf.BaseFont; 
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.Font.FontFamily;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfWriter;

import java.awt.Graphics2D; 

import java.io.BufferedReader; 
import java.io.File;
import java.io.FileWriter;
import java.io.FileInputStream; 
import java.io.FileOutputStream; 
import java.io.InputStream; 
import java.io.IOException; 
import java.io.InputStreamReader; 
import java.io.OutputStream; 

import java.net.URL;

import java.util.StringTokenizer; 

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;
 
public class PDFwithText {

    //smallest font to render
    public static final float fontLimit = 2.0f;

    public static CommandLine cl;
    public static Options options;
 
    /*
        getAttrib() - extract x or y attribute from node
    */
    public static float getAttrib(Node node, String att,
        float imgW, float imgH, 
        float imgScaleW, float imgScaleH, 
        float adjx, float adjy) 
    {
        float adjVal = 0.0f;

        float coord = Float.parseFloat(node.getAttributes().
               getNamedItem(att).getNodeValue() + "f");

        //x - coordinate
        if (att.indexOf("x") != -1) {
            adjVal = adjx + (coord * imgScaleW)/imgW;
        }
        //y - coordinate
        if (att.indexOf("y") != -1) {
            adjVal = imgH - coord;
            adjVal = (adjVal * imgScaleH)/imgH;
            adjVal += adjy;
        }
        return adjVal;
    }//getAttrib

    /*
        sortOutFileName() - return stem of filename
    */
    public static String sortOutFileName(String fileBase) 
    {
        int lastPeriod = fileBase.lastIndexOf('.');

        if (lastPeriod == -1)
             return null;

        return fileBase.substring(0,lastPeriod);
    }//sortOutFileName

    /*
        getUniqueFileName() - use tempfile facility to
            get unique filename, create and delete file
            in case of concurrent use
    */
    public static String getUniqueFileName(String fileType) 
        throws IOException
    {
        File tempFile = File.createTempFile("odw", fileType);
        String fileName = tempFile.getPath();
        FileWriter fileoutput = new FileWriter(tempFile);
        fileoutput.close();
        tempFile.delete();
         
        return fileName;
    }//getUniqueFileName

    /*
       getPdfFont() - how to get a font from resource
           directory, we don't use this approach but
           would be approach for bundling non-standard
           font(s)
    */
    public static BaseFont getPdfFont(String fontName) 
        throws IOException
    {
        URL font_path = Thread.currentThread().getContextClassLoader().
            getResource(fontName);
        FontFactory.register(font_path.toString(), "pdf_font");
        Font pdfFont = FontFactory.getFont("pdf_font","",BaseFont.EMBEDDED);
        return pdfFont.getBaseFont();
    }//getPdfFont

    /*
       getFontSize() - estimate a font width based on
           the width of text
    */
    public static float getFontSize(BaseFont bf, String ocrtext, 
        float ocrwidth) 
    {
        float glyphWidth = bf.getWidth(ocrtext);
        float fontWidth = 1000 * ocrwidth / glyphWidth;

        return fontWidth;
    }//getFontSize

    /*
       help() - invoke apache commons CLI help facility
    */
    public static void help() {
       HelpFormatter formatter = new HelpFormatter();
       formatter.printHelp("PDFwithText", options);
    }//help

    /*
       sortOutCoordsFromXML() - given a PDF file, map corresponding
           coordinates from ODW XML words format
    */
    public static void sortOutCoordsFromXML(String pdfFile, String outFile,
        String xmlFile, float imgW, float imgH, 
        float imgScaleW, float imgScaleH, 
        float adjx, float adjy, boolean verboseFlag) throws IOException, 
        DocumentException, ParserConfigurationException, SAXException
    {
        PdfReader pdfReader = new PdfReader(pdfFile);

        PdfStamper stamper = new PdfStamper(pdfReader, 
            new FileOutputStream(outFile));
        PdfContentByte cb = stamper.getOverContent(1); 

        //get text from ODW OCR file
        File file = new File(xmlFile);
        DocumentBuilderFactory documentBuilderFactory = 
            DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.
            newDocumentBuilder();
        org.w3c.dom.Document xmlDocument = documentBuilder.parse(file);

        NodeList words = xmlDocument.getElementsByTagName("word");
        int numWords = words.getLength();

        //set up font information
        cb.beginText(); 
        if (!verboseFlag)
            cb.setTextRenderingMode(PdfContentByte.TEXT_RENDER_MODE_INVISIBLE); 

        //use Helvetica for font mapping
        BaseFont font = BaseFont.createFont(BaseFont.HELVETICA, 
            BaseFont.WINANSI, 
            BaseFont.NOT_EMBEDDED); 

        //get coordinates
        float llx, lly, urx, ury; 
        for (int i = 0; i < numWords; i++) {
            Node wordNode = words.item(i);
            String word = wordNode.getTextContent(); 
            NodeList childNodes = ((org.w3c.dom.Element) wordNode).
                getElementsByTagName("ends");

            //PDF uses different coordinate system than words
            llx = getAttrib(wordNode,"x1",imgW,imgH,imgScaleW,imgScaleH,
                adjx, adjy);
            lly = getAttrib(childNodes.item(0),"y2",imgW,imgH,imgScaleW,
                imgScaleH,adjx,adjy);
            urx = getAttrib(childNodes.item(0),"x2",imgW,imgH,imgScaleW,
                imgScaleH,adjx,adjy);
            ury = getAttrib(wordNode,"y1",imgW,imgH,imgScaleW,imgScaleH,
                adjx,adjy);

            //useful debugging information
            if (verboseFlag) {
                System.out.println("llx: " + llx + " lly: " + lly + 
                    " urx: " + urx + " ury: " + ury);
                System.out.println("->" + Math.abs(urx - llx));
            }//if

            float fontSize = getFontSize(font, word.trim(), 
                Math.abs(urx - llx));
            if (fontSize > fontLimit) {
                cb.setFontAndSize(font, fontSize);
                cb.showTextAligned(Element.ALIGN_LEFT, word.trim(), 
                    llx, lly, 0); 
            }//if
        }//for
        cb.endText();
        stamper.close();
    }//sortOutCoordsFromXML

    /*
       sortOutPDF() - create a PDF file with supplied image, this
           keeps the aspect ratio by resizing rather than mapping
           directly to the page size
    */
    public static void sortOutPDF(String inputFile, String outputFile,
        String xmlFile, Rectangle pageSize, 
        boolean verboseFlag) throws IOException, DocumentException, 
        ParserConfigurationException, SAXException
    {
        String tempName = getUniqueFileName("pdf");
        Document document = new Document(pageSize,0,0,0,0); 

        PdfWriter writer = PdfWriter.getInstance(document, 
            new FileOutputStream(tempName));
        writer.setCompressionLevel(0);

        document.open(); 
        Image img = Image.getInstance(inputFile);
        float imgW = img.getWidth();
        float imgH = img.getHeight();

        img.scaleToFit(pageSize.getWidth(), pageSize.getHeight());

        float imgScaleW = img.getScaledWidth();
        float imgScaleH = img.getScaledHeight();

        img.setAbsolutePosition(
            (pageSize.getWidth() - imgScaleW) / 2,
            (pageSize.getHeight() - imgScaleH) / 2);

        float adjx = (pageSize.getWidth() - imgScaleW)/2;
        float adjy = (pageSize.getHeight() - imgScaleH)/2;
        writer.getDirectContent().addImage(img);
        document.close();

        sortOutCoordsFromXML(tempName,outputFile,xmlFile,imgW,imgH,
            imgScaleW,imgScaleH,adjx,adjy,verboseFlag);

        //cleanup
        File thisfile = new File(tempName);
        thisfile.delete();
    }//sortOutPDF

    /*
       main() - set up the options and pull together the PDF
    */
    public static void main(String[] args) throws IOException, 
        DocumentException, ParseException, ParserConfigurationException, 
        SAXException
    {

        options = new Options();
        options.addOption("b", "black", false, 
            "set page background colour to black.");
        options.addOption("h", "help", false, "show help.");
        options.addOption("i", "input", true, 
            "input image (required).");
        options.addOption("o", "output", true, 
            "output PDF file (default name from image).");
        options.addOption("p", "pagesize", true, 
            "L - LETTER (default), T - TABLOID, A - A4.");
        options.addOption("x", "xmlfile", true, 
            "specify XML file (default name from image).");
        options.addOption("v", "verbose", false, 
            "show underlying text on image.");

        BasicParser parser = new BasicParser();
        cl = parser.parse(options, args);

        if (cl.hasOption('h')) {
            help();
            System.exit(0);
        }//if

        String inputFile = cl.getOptionValue("i");
        if (inputFile == null) {
            System.out.println("need an input file");
            System.exit(-1);
        }//if

        String fileBase = sortOutFileName(inputFile);
        String outputFile = cl.getOptionValue("o");
        if (outputFile == null && fileBase != null) {
            outputFile = fileBase + ".pdf";
        }//if

        String xmlFile = cl.getOptionValue("x");
        if (xmlFile == null && fileBase != null) {
            xmlFile = fileBase + ".xml";
        }//if

        if (outputFile == null && fileBase != null) {
            outputFile = fileBase + ".pdf";
        }//if

        //this shouldn't happen - but just in case
        if (outputFile == null || xmlFile == null) {
            System.out.println("missing file(s)");
            System.exit(-1);
        }//if

        Rectangle pageSize = new Rectangle(PageSize.LETTER);
        String pageFormat = cl.getOptionValue("p");
        if (pageFormat != null && pageFormat.equals("T"))
             pageSize = new Rectangle(PageSize.TABLOID);
        if (pageFormat != null && pageFormat.equals("A"))
             pageSize = new Rectangle(PageSize.A4);

        //only option is currently black
        if (cl.hasOption('b')) 
            pageSize.setBackgroundColor(BaseColor.BLACK);

        boolean isVerbose = false;
        if (cl.hasOption('v')) 
            isVerbose = true;

        sortOutPDF(inputFile,outputFile,xmlFile,pageSize,isVerbose);

    }//main
}//PDFwithText
