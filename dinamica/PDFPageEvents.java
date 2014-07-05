package dinamica;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;


/**
 * PDF reports utility class.<br>
 * Generic class to intercept IText page events and
 * print custom footer text and Page X of Y using IText
 * template object.<br>
 * Footer looks like:<br>
 * [FooterText - DateTime] ...... [X of Y]
 * Creation date: 2006-12-28<br>
 * (c) 2006 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * Dinamica Framework - http://www.martincordova.com<br>
 * @author Martin Cordova (martin.cordova@gmail.com)
 */
public class PDFPageEvents extends PdfPageEventHelper 
{

	String footerLeft = "";
	String footerRight = "";
	String pageXofY = "";
    PdfTemplate tpl = null;
    BaseFont bf = null;
    PdfContentByte cb = null;
    
    /**
     * Initializes object with required parameters
     * @param footerLeft Text for letf-side footer
     * @param pageXofY Text between "X" and "Y" numbers, might be " of " or " de ", according to language
     * @param tpl Template used to print "X of Y" right-side footer
     * @param bf Base font
     * @param cb Content byte
     * @param reportDateTime Formatted date/time for left-side footer
     * @throws Throwable
     */
    public PDFPageEvents(
				String footerLeft, 
				String pageXofY, 
				PdfTemplate tpl,
				BaseFont bf,
				PdfContentByte cb,
				String reportDateTime
				) throws Throwable
	{
		this.pageXofY = pageXofY;
		this.tpl = tpl;
		this.bf = bf;
		this.cb = cb;
		this.footerLeft = footerLeft + " - " + reportDateTime;
	}
	
    /**
     * Print value on empty template (total number of pages), this
     * will update footer on every page where the template was inserted
     */
    public void onCloseDocument(PdfWriter writer, Document document)
    {
        	// print total number of pages into template
        	// this will update footer on every page
            int pageNum = writer.getPageNumber() - 1;
            tpl.beginText();
            tpl.setFontAndSize(bf, 10);
            tpl.showText(String.valueOf(pageNum));
            tpl.endText();
    }        
    
    /**
     * Create custom-made footer on every page, this
     * will place the empty template on every page
     */
    public void onEndPage(PdfWriter writer, Document document)
    {

        // print (x of ...) on every page (bottom right)
        // append template at the end of the footer, like:
        // 1 of [template] - template is an empty box
        String footer = writer.getPageNumber() + this.pageXofY;
        float tWidth = bf.getWidthPoint(footer, 10);
        float extraSpace = bf.getWidthPoint("00", 10);
        float ty = document.bottom() - 12; //below bottom margin
        float tx = document.right() - tWidth - extraSpace; //x coordinate for the footer
        
        // print "X of " on right-side and footer + date on left side
        cb.beginText();
        cb.setFontAndSize(bf, 10);
        cb.showTextAligned(PdfContentByte.ALIGN_LEFT, footer, tx, ty, 0);
        cb.showTextAligned(PdfContentByte.ALIGN_LEFT, footerLeft, document.left(), ty, 0);
        cb.endText();

        // now append empty template after "X of " 
        cb.addTemplate(tpl, document.right() - extraSpace, ty);
        
    }
       
	
}
