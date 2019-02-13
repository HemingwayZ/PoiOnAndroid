package com.hemingway.poionandroid;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.RectangleReadOnly;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.parser.ImageRenderInfo;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.RenderListener;
import com.itextpdf.text.pdf.parser.TextRenderInfo;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.model.PicturesTable;
import org.apache.poi.hwpf.usermodel.CharacterRun;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Picture;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;


/**
 * Description:
 * Data：2018/8/27-15:03
 * <p>
 * Author: hemingway
 */
public class OfficeUtils {

    private static final String TAG = OfficeUtils.class.getSimpleName();


    public interface OnReadWordListener {
        void setText(TextModel text);

        void setBitmap(ImageModel bitmap);

        void splitP();

        void onStart();

        void onCompleted();
    }

    public static void readWord(final String path, final float maxImageWidth, final Handler handler, final OnReadWordListener mListener) {
        if (!isWord(path)) {
            return;
        }
        if (mListener != null) {
            mListener.onStart();
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                readWord2007(path, maxImageWidth, handler, mListener);

                if (handler != null && mListener != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            mListener.onCompleted();
                        }
                    });
                }
            }
        }).start();

    }

    private static void readWord2007(String path, float maxImageWidth, Handler handler, final OnReadWordListener mListener) {
        ZipFile zipFile = null;
        try {
            //只读取文本方式
//            POIXMLTextExtractor extractor = new XWPFWordExtractor(opcPackage);
//            final String text = extractor.getText();
//            handler.post(new Runnable() {
//                @Override
//                public void run() {
//                    if (text != null) {
//                        textView.append(text);
//                    }
//                }
//            });
            //检测是否为docx文档
            zipFile = new ZipFile(path);
            ZipEntry sharedStringXML = zipFile.getEntry("word/document.xml");
            if (sharedStringXML == null) {
                zipFile.close();
                return;
            }
            zipFile.close();
            //使用这种方式 小米pad异常

            XWPFDocument document = new XWPFDocument(new FileInputStream(new File(path)));
            //图文读取方式
//            OPCPackage opcPackage = POIXMLDocument.openPackage(path);
//            XWPFDocument document = new XWPFDocument(doc);
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            for (XWPFParagraph paramitem : paragraphs) {

                List<XWPFRun> runs = paramitem.getRuns();
                for (final XWPFRun run :
                        runs) {
                    CTR ctr = run.getCTR();
                    String text = "";
                    if(ctr!=null){
                        int sizeOfTArray = ctr.sizeOfTArray();
                        int textPosition = run.getTextPosition()<0?0:run.getTextPosition();
                        if(textPosition<sizeOfTArray){
                            text = run.getText(textPosition);
                        }
                    }
                    final TextModel textModel = new TextModel();
                    textModel.text = text;
                    textModel.isBold = run.isBold();
                    textModel.isItalic = run.isItalic();
                    textModel.color = run.getColor();
                    textModel.fontSize = run.getFontSize();
                    textModel.fontName = run.getFontName();
                    //process text attr
                    if (handler != null && text != null) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (mListener != null) {
                                    mListener.setText(textModel);
                                }
                            }
                        });
                    }
                    //process image
                    List<XWPFPicture> embeddedPictures = run.getEmbeddedPictures();
                    if (embeddedPictures != null && embeddedPictures.size() > 0) {
                        Log.d(TAG, "position = " + run.getTextPosition());
                        for (XWPFPicture xwpfPicture : embeddedPictures) {
                            if(xwpfPicture==null){
                                continue;
                            }
                            if(xwpfPicture.getPictureData()==null){
                                continue;
                            }
                            byte[] data = xwpfPicture.getPictureData().getData();
                            if(data==null){
                                continue;
                            }
                            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                            if (maxImageWidth > 0) {
//                                bitmap = BitmapUtils.resizeToWidth(bitmap, maxImageWidth, true);
                            }
                            final Bitmap finalBitmap = bitmap;
                            if (handler != null) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (finalBitmap != null) {
                                            if (mListener != null) {
                                                ImageModel imageModel = new ImageModel();
                                                imageModel.bitmap = finalBitmap;
                                                mListener.setBitmap(imageModel);
                                            }
                                        }
                                    }
                                });
                            }

                        }
                    }
                }

                final boolean pageBreak = paramitem.isPageBreak();
                Log.d(TAG, "pageBreak  = " + pageBreak);
                if (handler != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (pageBreak) {
//                                textView.append("\n\n");
                            }
                            if (mListener != null) {
                                mListener.splitP();
                            }
                        }
                    });
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            if (e instanceof ZipException) {
                readWord2003(path, maxImageWidth, handler, mListener);
            }
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void readWord2003(String path, float maxImageWidth, Handler handler, final OnReadWordListener mListener) {
        FileInputStream fis = null;
        try {
            //纯文本
//            fis = new FileInputStream(new File(path));
//                WordExtractor wordExtractor = new WordExtractor(fis);
//                final String text = wordExtractor.getText().toString();
//            handler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        textView.setText(text);
//                        textView.append("\n");
//                    }
//            });

            //图文
            POIFSFileSystem poifsFileSystem = new POIFSFileSystem(new File(path), true);
            HWPFDocument document = new HWPFDocument(poifsFileSystem);
            Range range = document.getRange();

            PicturesTable picturesTable = document.getPicturesTable();
            List<Picture> allPictures = picturesTable.getAllPictures();
            int picIndex = 0;

            int numParagraphs = range.numParagraphs();
            ;
            for (int i = 0; i < numParagraphs; i++) {

                Paragraph paragraph = range.getParagraph(i);
                int numCharacterRuns = paragraph.numCharacterRuns();
                for (int j = 0; j < numCharacterRuns; j++) {
                    CharacterRun characterRun = paragraph.getCharacterRun(j);

                    if (characterRun.getPicOffset() == 0 || characterRun.getPicOffset() >= 1000) {
                        if (picIndex < allPictures.size()) {
                            Picture picture = allPictures.get(picIndex);
                            byte[] data = picture.getContent();
                            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                            bitmap = BitmapUtils.resizeToWidth(bitmap, maxImageWidth, true);
                            final Bitmap finalBitmap = bitmap;
                            if (handler != null) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (finalBitmap != null) {
                                            if (mListener != null) {
                                                ImageModel imageModel = new ImageModel();
                                                imageModel.bitmap = finalBitmap;
                                                mListener.setBitmap(imageModel);
                                            }
                                        }
                                    }
                                });
                            }
                            picIndex++;
                        }

                    } else {
                        final String text = characterRun.text();
                        if (handler != null) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (text != null) {
                                        if (mListener != null) {
                                            TextModel textModel = new TextModel();
                                            textModel.text = text;
                                            mListener.setText(textModel);
                                        }
                                    }
                                }
                            });
                        }
                    }
                }

                if (handler != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mListener != null) {
                                mListener.splitP();
                            }
                        }
                    });
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String convertWord2Pdf(Context context, final String path, final float maxImageWidth) {
        if (!isWord(path)) {
            return "";
        }
        return readWord2007_2(context, path, maxImageWidth, 1);


    }

    public static String convertWord2Pdf(Context context, final String path, final float maxImageWidth, float paperWidthStep) {
        if (!isWord(path)) {
            return "";
        }
        return readWord2007_2(context, path, maxImageWidth, paperWidthStep);
    }

    private static final int DEFAULT_PAPER_WIDTH = 297;
    private static final int DEFAULT_FONT_SIZE = 12;

    private static String readWord2007_2(Context context, String path, float maxImageWidth, float scaleFontSize) {
        scaleFontSize = scaleFontSize <= 0 ? 0.1f : scaleFontSize;
        //通过改变纸张长度来改变字体大小
        final RectangleReadOnly rectangleReadOnly = new RectangleReadOnly(DEFAULT_PAPER_WIDTH, 425);
        File outfile = FileUtils.getMiaoMiaoPrintFile(FileUtils.OFFICE_PDF_TEMP);
        ZipFile zipFile = null;
        try {
            if (outfile != null && !outfile.exists()) {
                boolean newFile = outfile.createNewFile();
                if (!newFile) {
                    return "";
                }
            }
            if (outfile == null) {
                return "";
            }
            FileOutputStream fos = new FileOutputStream(outfile);
            Document pdfDocument = new Document(rectangleReadOnly);//PageSize.A6
            pdfDocument.setMargins(0, 0, 0, 0);
            PdfWriter.getInstance(pdfDocument, fos);

            pdfDocument.open();
            File miaoMiaoFontsFile = FileUtils.getMiaoMiaoFontsFile(FileUtils.OFFICE_FONT_FILE);
            if (miaoMiaoFontsFile == null || !miaoMiaoFontsFile.exists()) {
                boolean newFile = miaoMiaoFontsFile.createNewFile();
                if (newFile) {
                    //字体设置
                    InputStream open = context.getAssets().open("fonts/simhei.ttf");
                    FileOutputStream fileOutputStream = new FileOutputStream(miaoMiaoFontsFile.getPath());
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = open.read(buffer)) > 0) {
                        fileOutputStream.write(buffer, 0, length);
                    }
                    open.close();
                    fileOutputStream.close();
                    miaoMiaoFontsFile = FileUtils.getMiaoMiaoFontsFile(FileUtils.OFFICE_FONT_FILE);
                    if (miaoMiaoFontsFile == null || !miaoMiaoFontsFile.exists()) {
                        return null;
                    }
                }
            }
            Font yaHeiFont = new Font(BaseFont.createFont(miaoMiaoFontsFile.getPath(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED));//中文简体
//            int fontSize = DEFAULT_FONT_SIZE + paperWidthStep;

//            yaHeiFont.setSize(fontSize <= 0 ? 1 : fontSize);
            //只读取文本方式
//            POIXMLTextExtractor extractor = new XWPFWordExtractor(opcPackage);
//            final String text = extractor.getText();
//            handler.post(new Runnable() {
//                @Override
//                public void run() {
//                    if (text != null) {
//                        textView.append(text);
//                    }
//                }
//            });
            //检测是否为docx文档
            zipFile = new ZipFile(path);
            ZipEntry sharedStringXML = zipFile.getEntry("word/document.xml");
            if (sharedStringXML == null) {
                zipFile.close();
                return readWord2003_2(context, path, maxImageWidth, scaleFontSize);
            }
            zipFile.close();
            //使用这种方式 小米pad异常

            XWPFDocument document = new XWPFDocument(new FileInputStream(new File(path)));
//            List<XWPFPictureData> allPictures = document.getAllPictures();
//            List<XWPFPictureData> allPictures = document.getAllPackagePictures();

            //　图片测试代码 wmf图片不能直接转bitmap
//            int pos = 0;
//            for (XWPFPictureData item : allPictures) {
//
//                LogUtil.d("suggestFileExtension:" + item.suggestFileExtension());
//                LogUtil.d("getFileName:" + item.getFileName());
//                byte[] bytes;
//                bytes = item.getData();
//
////                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
////                bytes = BitmapUtils.compressToBytes(bitmap);
//
//
//                Image image = Image.getInstance(bytes);
//                image.setBackgroundColor(BaseColor.BLUE);
//                image.setBorder(30);
//                image.setBorderColor(BaseColor.RED);
//
//                if (image.getWidth() > 300) {
//                    float percent = getPercent(rectangleReadOnly, image.getHeight(), image.getWidth());
//                    image.setScaleToFitHeight(true);
//                    image.scalePercent(percent);
//                }
//
//                pdfDocument.add(new com.itextpdf.text.Paragraph(pos + " --========-- " + item.suggestFileExtension(), yaHeiFont));
//                LogUtil.d(pos + " --============================== : " + image.isImgRaw());
//                pdfDocument.add(image);
//                pos++;
//            }

            //图文读取方式
//            OPCPackage opcPackage = POIXMLDocument.openPackage(path);
//            XWPFDocument document = new XWPFDocument(doc);
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            for (XWPFParagraph paramitem : paragraphs) {
                StringBuffer sb = new StringBuffer();
                List<XWPFRun> runs = paramitem.getRuns();
                for (final XWPFRun run :
                        runs) {
                    CTR ctr = run.getCTR();
                    String text = "";
                    if(ctr!=null){
                        int sizeOfTArray = ctr.sizeOfTArray();
                        int textPosition = run.getTextPosition()<0?0:run.getTextPosition();
                        if(textPosition<sizeOfTArray){
                            text = run.getText(textPosition);
                        }
                    }



                    final TextModel textModel = new TextModel();
                    textModel.text = text;
                    textModel.isBold = run.isBold();
                    textModel.isItalic = run.isItalic();
                    textModel.color = run.getColor();
                    int fontSize = run.getFontSize();
                    //文档没有设置字体 默认-1
                    if (fontSize < 1) {
                        fontSize = DEFAULT_FONT_SIZE;
                    }
                    textModel.fontSize = (int) fontSize * scaleFontSize;
                    textModel.fontName = run.getFontName();

//                    if (!TextUtils.isEmpty(textModel.color)) {
//                        try {
//
//                            int parseColor = Color.parseColor("#" + textModel.color);
//                            yaHeiFont.setColor(new BaseColor(parseColor));
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//
//                    }
                    yaHeiFont.setSize(textModel.fontSize);
                    yaHeiFont.setStyle(
                            (textModel.isBold ? Font.BOLD : Font.NORMAL)
                                    | (textModel.isItalic ? Font.ITALIC : Font.NORMAL)
                    );
                    //process text attr
                    if (text != null) {
                        sb.append(text);
                    }

                    //process image
//                    List<XWPFPicture> embeddedPictures = run.getEmbeddedPictures();
                    List<XWPFPicture> embeddedPictures = run.getEmbeddedPictures();
                    if (embeddedPictures != null && embeddedPictures.size() > 0) {
                        Log.d(TAG, "position = " + run.getTextPosition());
                        for (XWPFPicture xwpfPicture : embeddedPictures) {
                            if (!TextUtils.isEmpty(sb.toString())) {
                                pdfDocument.add(new com.itextpdf.text.Paragraph(sb.toString(), yaHeiFont));
                                sb.delete(0, sb.length());
                            }
                            if(xwpfPicture==null){
                                continue;
                            }
                            if(xwpfPicture.getPictureData()==null){
                                continue;
                            }
                            byte[] data = xwpfPicture.getPictureData().getData();
                            if(data==null){
                                continue;
                            }
                            Image image = Image.getInstance(data);
                            if(image==null){
                                continue;
                            }
//                            image.setAlignment(Image.MIDDLE);
                            float width = image.getWidth();
                            float height = image.getHeight();
                            if (image.getWidth() >= 200) {
                                float percent = getPercent(rectangleReadOnly, height, width);
                                image.setScaleToFitHeight(true);
                                image.scalePercent(percent);
                            }
//                            if (MBConfig.IS_DEBUG) {
//                                yaHeiFont.setSize(12);
//                                yaHeiFont.setColor(BaseColor.BLUE);
//                                pdfDocument.add(new com.itextpdf.text.Paragraph("isImgRaw : " + image.isImgRaw(), yaHeiFont));
//                            }
                            pdfDocument.add(image);

                        }
                    }
                }
                String text = sb.toString();
                if (TextUtils.isEmpty(text)) {
                    text = " ";
                }
                pdfDocument.add(new com.itextpdf.text.Paragraph(text, yaHeiFont));
                final boolean pageBreak = paramitem.isPageBreak();
                if (pageBreak) {

                }

            }
            pdfDocument.close();
        } catch (IOException e) {
            e.printStackTrace();
            if (e instanceof ZipException) {
                return readWord2003_2(context, path, maxImageWidth, scaleFontSize);
            }
        } catch (DocumentException e) {
            e.printStackTrace();
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return outfile.getPath();
    }

    private static float getPercent(RectangleReadOnly rectangleReadOnly, float h, float w) {

        int p = 0;
        float p2 = 0.0f;
        p2 = (PageSize.A6.getWidth() - 30) / w * 100;
        p = Math.round(p2);
        return p;
    }

    private static String readWord2003_2(Context context, String path, float maxImageWidth, float scaleFontSize) {
        FileInputStream fis = null;
        File outfile = FileUtils.getMiaoMiaoPrintFile(FileUtils.OFFICE_PDF_TEMP);
        try {
            if (outfile != null && !outfile.exists()) {
                boolean newFile = outfile.createNewFile();
                if (!newFile) {
                    return "";
                }
            }
            if (outfile == null) {
                return "";
            }
            FileOutputStream fos = new FileOutputStream(outfile);
            Document pdfDocument = new Document(PageSize.A6);
            pdfDocument.setMargins(0, 0, 0, 0);
            PdfWriter.getInstance(pdfDocument, fos);

            pdfDocument.open();
            File miaoMiaoFontsFile = FileUtils.getMiaoMiaoFontsFile(FileUtils.OFFICE_FONT_FILE);
            if (miaoMiaoFontsFile == null || !miaoMiaoFontsFile.exists()) {
                boolean newFile = miaoMiaoFontsFile.createNewFile();
                if (newFile) {
                    //字体设置
                    InputStream open = context.getAssets().open("fonts/simhei.ttf");
                    FileOutputStream fileOutputStream = new FileOutputStream(miaoMiaoFontsFile.getPath());
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = open.read(buffer)) > 0) {
                        fileOutputStream.write(buffer, 0, length);
                    }
                    open.close();
                    fileOutputStream.close();
                    miaoMiaoFontsFile = FileUtils.getMiaoMiaoFontsFile(FileUtils.OFFICE_FONT_FILE);
                    if (miaoMiaoFontsFile == null || !miaoMiaoFontsFile.exists()) {
                        return null;
                    }
                }
            }
            Font yaHeiFont = new Font(BaseFont.createFont(miaoMiaoFontsFile.getPath(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED));//中文简体
            yaHeiFont.setSize(DEFAULT_FONT_SIZE * scaleFontSize);
            //纯文本
//            fis = new FileInputStream(new File(path));
//                WordExtractor wordExtractor = new WordExtractor(fis);
//                final String text = wordExtractor.getText().toString();
//            handler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        textView.setText(text);
//                        textView.append("\n");
//                    }
//            });

            //图文
            POIFSFileSystem poifsFileSystem = new POIFSFileSystem(new File(path), true);
            HWPFDocument document = new HWPFDocument(poifsFileSystem);
            Range range = document.getRange();

            PicturesTable picturesTable = document.getPicturesTable();
            List<Picture> allPictures = picturesTable.getAllPictures();
            int picIndex = 0;

            int numParagraphs = range.numParagraphs();
            ;
            for (int i = 0; i < numParagraphs; i++) {
                StringBuffer sb = new StringBuffer();
                Paragraph paragraph = range.getParagraph(i);
                int numCharacterRuns = paragraph.numCharacterRuns();
                for (int j = 0; j < numCharacterRuns; j++) {
                    CharacterRun characterRun = paragraph.getCharacterRun(j);

                    if (characterRun.getPicOffset() == 0 || characterRun.getPicOffset() >= 1000) {
                        if (picIndex < allPictures.size()) {
                            Picture picture = allPictures.get(picIndex);
                            byte[] data = picture.getContent();
                            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                            if (bitmap == null) {
                                continue;
                            }
                            Image image = Image.getInstance(data);
                            boolean imgRaw = image.isImgRaw();
                            if (!imgRaw) {
                                continue;
                            }
//                            image.setAlignment(Image.MIDDLE);
                            int width = bitmap.getWidth();
                            int height = bitmap.getHeight();
                            if (width > 400) {
                                float percent = getPercent(null, height, width);
                                image.setScaleToFitHeight(true);
                                image.scalePercent(percent);
                            }
                            pdfDocument.add(image);
                            picIndex++;
                        }

                    } else {
                        final String text = characterRun.text();
                        if (text != null) {
                            sb.append(text);
                        }
                    }
                }

                String text = sb.toString();
                if (TextUtils.isEmpty(text)) {
                    text = " ";
                }
                pdfDocument.add(new com.itextpdf.text.Paragraph(text, yaHeiFont));
            }
            pdfDocument.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return outfile.getPath();
    }

    public static String readTxt(File file) throws IOException {
        if (!file.getPath().endsWith(".txt")) {
            return "";
        }
        StringBuffer sBuffer = new StringBuffer();
        FileInputStream fis = new FileInputStream(file);
        InputStreamReader inputStreamReader = new InputStreamReader(fis, getFilecharset(file));
        BufferedReader reader = new BufferedReader(inputStreamReader);
        String strLine;
        while ((strLine = reader.readLine()) != null) {
            sBuffer.append(strLine).append("\n");
        }
        reader.close();
        inputStreamReader.close();
        fis.close();

        return sBuffer.toString();
    }

    public static String convertTxt2Pdf(Context context, File file) {
        return convertTxt2Pdf(context, file, 1);
    }

    public static String convertTxt2Pdf(Context context, File file, float scaleFontSize) {
        scaleFontSize = scaleFontSize <= 0 ? 0.1f : scaleFontSize;
//        RectangleReadOnly rectangleReadOnly = new RectangleReadOnly(DEFAULT_PAPER_WIDTH + paperWidthStep, 420);//A6尺寸
        String readTxt = null;
        File outfile = FileUtils.getMiaoMiaoPrintFile(FileUtils.OFFICE_PDF_TEMP);

        try {
            if (outfile != null && !outfile.exists()) {
                boolean newFile = outfile.createNewFile();
                if (!newFile) {
                    return "";
                }
            }
            if (outfile == null) {
                return "";
            }
            readTxt = readTxt(file);
            if (TextUtils.isEmpty(readTxt)) {
                return null;
            }
            FileOutputStream fos = new FileOutputStream(outfile);
            Document document = new Document(PageSize.A6);
            document.setMargins(0, 0, 0, 0);
            PdfWriter.getInstance(document, fos);
            document.open();
//            String yaHeiFontName = Environment.getExternalStorageDirectory().getPath() + "/simhei.ttf";
            File miaoMiaoFontsFile = FileUtils.getMiaoMiaoFontsFile(FileUtils.OFFICE_FONT_FILE);
            if (miaoMiaoFontsFile == null || !miaoMiaoFontsFile.exists()) {
                boolean newFile = miaoMiaoFontsFile.createNewFile();
                if (newFile) {
                    //字体设置
                    InputStream open = context.getAssets().open("fonts/simhei.ttf");
                    FileOutputStream fileOutputStream = new FileOutputStream(miaoMiaoFontsFile.getPath());
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = open.read(buffer)) > 0) {
                        fileOutputStream.write(buffer, 0, length);
                    }
                    open.close();
                    fileOutputStream.close();
                    miaoMiaoFontsFile = FileUtils.getMiaoMiaoFontsFile(FileUtils.OFFICE_FONT_FILE);
                    if (miaoMiaoFontsFile == null || !miaoMiaoFontsFile.exists()) {
                        return null;
                    }
                }
            }
//            context.getResources().getAssets().open()
            Font yaHeiFont = new Font(BaseFont.createFont(miaoMiaoFontsFile.getPath(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED));//中文简体
//            ByteArrayOutputStream stream = new ByteArrayOutputStream();
//            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
//            Image image = Image.getInstance(stream.toByteArray());
//            document.add(image);
            yaHeiFont.setSize(DEFAULT_FONT_SIZE * scaleFontSize);
            document.add(new com.itextpdf.text.Paragraph(readTxt, yaHeiFont));
            document.close();
        } catch (IOException | DocumentException e) {
            e.printStackTrace();
        }

        return outfile.getPath();
    }

    //判断编码格式方法
    private static String getFilecharset(File sourceFile) {
        String charset = "GBK";
        byte[] first3Bytes = new byte[3];
        try {
            boolean checked = false;
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(sourceFile));
            bis.mark(0);
            int read = bis.read(first3Bytes, 0, 3);
            if (read == -1) {
                return charset; //文件编码为 ANSI
            } else if (first3Bytes[0] == (byte) 0xFF
                    && first3Bytes[1] == (byte) 0xFE) {
                charset = "UTF-16LE"; //文件编码为 Unicode
                checked = true;
            } else if (first3Bytes[0] == (byte) 0xFE
                    && first3Bytes[1] == (byte) 0xFF) {
                charset = "UTF-16BE"; //文件编码为 Unicode big endian
                checked = true;
            } else if (first3Bytes[0] == (byte) 0xEF
                    && first3Bytes[1] == (byte) 0xBB
                    && first3Bytes[2] == (byte) 0xBF) {
                charset = "UTF-8"; //文件编码为 UTF-8
                checked = true;
            }
            bis.reset();
            if (!checked) {
                int loc = 0;
                while ((read = bis.read()) != -1) {
                    loc++;
                    if (read >= 0xF0)
                        break;
                    if (0x80 <= read && read <= 0xBF) // 单独出现BF以下的，也算是GBK
                        break;
                    if (0xC0 <= read && read <= 0xDF) {
                        read = bis.read();
                        if (0x80 <= read && read <= 0xBF) // 双字节 (0xC0 - 0xDF)
                            // (0x80
                            // - 0xBF),也可能在GB编码内
                            continue;
                        else
                            break;
                    } else if (0xE0 <= read && read <= 0xEF) {// 也有可能出错，但是几率较小
                        read = bis.read();
                        if (0x80 <= read && read <= 0xBF) {
                            read = bis.read();
                            if (0x80 <= read && read <= 0xBF) {
                                charset = "UTF-8";
                                break;
                            } else
                                break;
                        } else
                            break;
                    }
                }
            }
            bis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return charset;
    }

    public static boolean isWord(String name) {
        if (TextUtils.isEmpty(name)) {
            return false;
        }
        return isWord2003(name) || isWord2007(name);
    }

    public static boolean isWord2003(String name) {
        if (TextUtils.isEmpty(name)) {
            return false;
        }
        return name.toLowerCase().endsWith(".doc");
    }

    public static boolean isWord2007(String name) {
        if (TextUtils.isEmpty(name)) {
            return false;
        }
        return name.toLowerCase().endsWith(".docx");
    }

    public static boolean isPPT(String name) {
        if (TextUtils.isEmpty(name)) {
            return false;
        }
        return isPPT2003(name) || isPPT2007(name);
    }

    public static boolean isPPT2003(String name) {
        if (TextUtils.isEmpty(name)) {
            return false;
        }
        return name.toLowerCase().endsWith(".ppt");
    }

    public static boolean isPPT2007(String name) {
        if (TextUtils.isEmpty(name)) {
            return false;
        }
        return name.toLowerCase().endsWith(".pptx");
    }

    public static boolean isPDF(String name) {
        if (TextUtils.isEmpty(name)) {
            return false;
        }
        return name.toLowerCase().endsWith(".pdf");
    }

    public static boolean isTxt(String name) {
        if (TextUtils.isEmpty(name)) {
            return false;
        }
        return name.toLowerCase().endsWith(".txt");
    }

    private FileOutputStream output;

    private String htmlBegin = "<html><meta charset=\"utf-8\"><body>";
    private String htmlEnd = "</body></html>";
    private String tableBegin = "<table style=\"border-collapse:collapse\" border=1 bordercolor=\"black\">";
    private String tableEnd = "</table>";
    private String rowBegin = "<tr>", rowEnd = "</tr>";
    private String columnBegin = "<td>", columnEnd = "</td>";
    private String lineBegin = "<p>", lineEnd = "</p>";
    private String centerBegin = "<center>", centerEnd = "</center>";
    private String boldBegin = "<b>", boldEnd = "</b>";
    private String underlineBegin = "<u>", underlineEnd = "</u>";
    private String italicBegin = "<i>", italicEnd = "</i>";
    private String fontSizeTag = "<font size=\"%d\">";
    private String fontColorTag = "<font color=\"%s\">";
    private String fontEnd = "</font>";
    private String spanColor = "<span style=\"color:%s;\">", spanEnd = "</span>";
    private String divRight = "<div align=\"right\">", divEnd = "</div>";

//    private void readDOCX(String docPath) {
//        try {
//            ZipFile docxFile = new ZipFile(new File(docPath));
//            ZipEntry sharedStringXML = docxFile.getEntry("word/document.xml");
//            InputStream inputStream = docxFile.getInputStream(sharedStringXML);
//            XmlPullParser xmlParser = Xml.newPullParser();
//            xmlParser.setInput(inputStream, "utf-8");
//            boolean isTable = false; // 表格
//            boolean isSize = false; // 文字大小
//            boolean isColor = false; // 文字颜色
//            boolean isCenter = false; // 居中对齐
//            boolean isRight = false; // 靠右对齐
//            boolean isItalic = false; // 斜体
//            boolean isUnderline = false; // 下划线
//            boolean isBold = false; // 加粗
//            boolean isRegion = false; // 在那个区域中
//            int pic_ndex = 1; // docx中的图片名从image1开始，所以索引从1开始
//            int event_type = xmlParser.getEventType();
//            while (event_type != XmlPullParser.END_DOCUMENT) {
//                switch (event_type) {
//                    case XmlPullParser.START_TAG: // 开始标签
//                        String tagBegin = xmlParser.getName();
//                        if (tagBegin.equalsIgnoreCase("r")) {
//                            isRegion = true;
//                        }
//                        if (tagBegin.equalsIgnoreCase("jc")) { // 判断对齐方式
//                            String align = xmlParser.getAttributeValue(0);
//                            if (align.equals("center")) {
//                                output.write(centerBegin.getBytes());
//                                isCenter = true;
//                            }
//                            if (align.equals("right")) {
//                                output.write(divRight.getBytes());
//                                isRight = true;
//                            }
//                        }
//                        if (tagBegin.equalsIgnoreCase("color")) { // 判断文字颜色
//                            String color = xmlParser.getAttributeValue(0);
//                            output.write(String.format(spanColor, color).getBytes());
//                            isColor = true;
//                        }
//                        if (tagBegin.equalsIgnoreCase("sz")) { // 判断文字大小
//                            if (isRegion == true) {
//                                int size = getSize(Integer.valueOf(xmlParser.getAttributeValue(0)));
//                                output.write(String.format(fontSizeTag, size).getBytes());
//                                isSize = true;
//                            }
//                        }
//                        if (tagBegin.equalsIgnoreCase("tbl")) { // 检测到表格
//                            output.write(tableBegin.getBytes());
//                            isTable = true;
//                        } else if (tagBegin.equalsIgnoreCase("tr")) { // 表格行
//                            output.write(rowBegin.getBytes());
//                        } else if (tagBegin.equalsIgnoreCase("tc")) { // 表格列
//                            output.write(columnBegin.getBytes());
//                        }
//                        if (tagBegin.equalsIgnoreCase("pic")) { // 检测到图片
//                            ZipEntry pic_entry = FileUtil.getPicEntry(docxFile, pic_ndex);
//                            if (pic_entry != null) {
//                                byte[] pictureBytes = FileUtil.getPictureBytes(docxFile, pic_entry);
//                                writeDocumentPicture(pictureBytes);
//                            }
//                            pic_ndex++; // 转换一张后，索引+1
//                        }
//                        if (tagBegin.equalsIgnoreCase("p") && !isTable) {// 检测到段落，如果在表格中就无视
//                            output.write(lineBegin.getBytes());
//                        }
//                        if (tagBegin.equalsIgnoreCase("b")) { // 检测到加粗
//                            isBold = true;
//                        }
//                        if (tagBegin.equalsIgnoreCase("u")) { // 检测到下划线
//                            isUnderline = true;
//                        }
//                        if (tagBegin.equalsIgnoreCase("i")) { // 检测到斜体
//                            isItalic = true;
//                        }
//                        // 检测到文本
//                        if (tagBegin.equalsIgnoreCase("t")) {
//                            if (isBold == true) { // 加粗
//                                output.write(boldBegin.getBytes());
//                            }
//                            if (isUnderline == true) { // 检测到下划线，输入<u>
//                                output.write(underlineBegin.getBytes());
//                            }
//                            if (isItalic == true) { // 检测到斜体，输入<i>
//                                output.write(italicBegin.getBytes());
//                            }
//                            String text = xmlParser.nextText();
//                            output.write(text.getBytes()); // 写入文本
//                            if (isItalic == true) { // 输入斜体结束标签</i>
//                                output.write(italicEnd.getBytes());
//                                isItalic = false;
//                            }
//                            if (isUnderline == true) { // 输入下划线结束标签</u>
//                                output.write(underlineEnd.getBytes());
//                                isUnderline = false;
//                            }
//                            if (isBold == true) { // 输入加粗结束标签</b>
//                                output.write(boldEnd.getBytes());
//                                isBold = false;
//                            }
//                            if (isSize == true) { // 输入字体结束标签</font>
//                                output.write(fontEnd.getBytes());
//                                isSize = false;
//                            }
//                            if (isColor == true) { // 输入跨度结束标签</span>
//                                output.write(spanEnd.getBytes());
//                                isColor = false;
//                            }
////						if (isCenter == true) { // 输入居中结束标签</center>。要在段落结束之前再输入该标签，因为该标签会强制换行
////							output.write(centerEnd.getBytes());
////							isCenter = false;
////						}
//                            if (isRight == true) { // 输入区块结束标签</div>
//                                output.write(divEnd.getBytes());
//                                isRight = false;
//                            }
//                        }
//                        break;
//                    // 结束标签
//                    case XmlPullParser.END_TAG:
//                        String tagEnd = xmlParser.getName();
//                        if (tagEnd.equalsIgnoreCase("tbl")) { // 输入表格结束标签</table>
//                            output.write(tableEnd.getBytes());
//                            isTable = false;
//                        }
//                        if (tagEnd.equalsIgnoreCase("tr")) { // 输入表格行结束标签</tr>
//                            output.write(rowEnd.getBytes());
//                        }
//                        if (tagEnd.equalsIgnoreCase("tc")) { // 输入表格列结束标签</td>
//                            output.write(columnEnd.getBytes());
//                        }
//                        if (tagEnd.equalsIgnoreCase("p")) { // 输入段落结束标签</p>，如果在表格中就无视
//                            if (isTable == false) {
//                                if (isCenter == true) { // 输入居中结束标签</center>
//                                    output.write(centerEnd.getBytes());
//                                    isCenter = false;
//                                }
//                                output.write(lineEnd.getBytes());
//                            }
//                        }
//                        if (tagEnd.equalsIgnoreCase("r")) {
//                            isRegion = false;
//                        }
//                        break;
//                    default:
//                        break;
//                }
//                event_type = xmlParser.next();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    public static List<OfficeItemModel> readFile(final String[] mimeType, Context context, String searchKey) {
        Uri[] fileUri = null;
        fileUri = new Uri[]{MediaStore.Files.getContentUri("external")};


        String[] colums = new String[]{MediaStore.Files.FileColumns.DATA};
        String[] extension = mimeType;
        List<OfficeItemModel> tempList = new ArrayList<>();
        //构造筛选语句
        String selection = "";
        for (int i = 0; i < extension.length; i++) {
            if (i != 0) {
                selection = selection + " OR ";
            }
            selection = selection + MediaStore.Files.FileColumns.MIME_TYPE + " LIKE '%" + extension[i] + "'";
        }

        //获取内容解析器对象
        ContentResolver resolver = context.getContentResolver();
        //获取游标
        for (int i = 0; i < fileUri.length; i++) {
            Cursor cursor = resolver.query(fileUri[i], colums, selection, null, MediaStore.Files.FileColumns.DATE_MODIFIED);
            if (cursor == null) {
                return null;
            }//游标从最后开始往前递减，以此实现时间递减顺序（最近访问的文件，优先显示）
            long beginTime = System.currentTimeMillis();
            if (cursor.moveToLast()) {

                do {
                    //输出文件的完整路径
                    String data = cursor.getString(0);
                    File file = new File(data);
                    if (!file.exists() || file.length() < 1) {
                        //过滤不存在的文件
                        continue;
                    }
                    if (file.isHidden() || !file.canRead()) {
                        continue;
                    }
                    if (!TextUtils.isEmpty(searchKey)) {
                        if (!file.getPath().toLowerCase().contains(searchKey)) {
                            continue;
                        }
                    }
                    if (file.getName().endsWith(".txt") && file.length() > 1024 * 100) {
                        //txt文件过大 导致无法读取 防止应用奔溃 故过滤
                        continue;
                    }
                    OfficeItemModel item = new OfficeItemModel();
                    item.setName(file.getName());
                    item.setPath(file.getPath());
                    item.setDate(file.lastModified());
                    item.setSize(file.length());
                    tempList.add(item);
                } while (cursor.moveToPrevious());

            }
            cursor.close();
            android.util.Log.e("endTime", System.currentTimeMillis() - beginTime + "");
        }
        Collections.sort(tempList);
        return tempList;
    }

    public static final int READ_TXT_LIMIT = 10000;

    public static String readStrFromPdf(String path) {
        final StringBuffer stringBuffer = new StringBuffer();
        try {

            PdfReader pdfReader = new PdfReader(path);
            PdfReaderContentParser pdfReaderContentParser = new PdfReaderContentParser(pdfReader);
            for (int i = 1; i <= pdfReader.getNumberOfPages(); i++) {
                if (stringBuffer.length() > READ_TXT_LIMIT) {
                    break;
                }
                pdfReaderContentParser.processContent(i, new RenderListener() {
                    @Override
                    public void beginTextBlock() {
//                                        stringBuffer = new StringBuffer();
                    }

                    @Override
                    public void renderText(TextRenderInfo renderInfo) {
                        stringBuffer.append(renderInfo.getText());
                    }

                    @Override
                    public void endTextBlock() {
//                                        LogUtil.d(stringBuffer.toString());
                    }

                    @Override
                    public void renderImage(ImageRenderInfo renderInfo) {

                    }
                });

//                stringBuffer.append("\n");
            }
        } catch (IOException e) {
            return e.getMessage();
        }
        return stringBuffer.toString();
    }

    public static String readStrFromWord(String path) {

        return readStrFromWord07(path);
    }

    public static String readStrFromOffice(String path) {
        String text = "";
        if (isTxt(path)) {
            try {
                text = readTxt(new File(path));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (isWord(path)) {
            text = readStrFromWord(path);
        } else if (isPDF(path)) {
            text = readStrFromPdf(path);
        } else {
            text = "<" + path + ">is a Invalid File";
        }
        if (text.length() > READ_TXT_LIMIT) {
            text = text.substring(0, READ_TXT_LIMIT);
        }
        return text;

    }

    public static String readStrFromWord07(String path) {
        ZipFile zipFile = null;
        try {
            //检测是否为docx文档
            zipFile = new ZipFile(path);
            ZipEntry sharedStringXML = zipFile.getEntry("word/document.xml");
            if (sharedStringXML == null) {
                zipFile.close();
                return readStrFromWord03(path);
            }
            zipFile.close();
            XWPFDocument document = new XWPFDocument(new FileInputStream(new File(path)));
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            StringBuffer sb = new StringBuffer();
            for (XWPFParagraph paramitem : paragraphs) {
                if (sb.length() > READ_TXT_LIMIT) {
                    return sb.substring(0, READ_TXT_LIMIT);
                }
                List<XWPFRun> runs = paramitem.getRuns();
                for (final XWPFRun run : runs) {
                    CTR ctr = run.getCTR();
                    String text = "";
                    if(ctr!=null){
                        int sizeOfTArray = ctr.sizeOfTArray();
                        int textPosition = run.getTextPosition()<0?0:run.getTextPosition();
                        if(textPosition<sizeOfTArray){
                            text = run.getText(textPosition);
                        }
                    }
                    if (text != null) {
                        sb.append(text);
                    }
                }
                sb.append("\n");
            }
            return sb.toString();

        } catch (IOException e) {
            e.printStackTrace();
            if (e instanceof ZipException) {
                return readStrFromWord03(path);
            }
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return "";
    }

    public static String readStrFromWord03(String path) {
        StringBuffer sb = new StringBuffer();
        HWPFDocument document = null;
        try {

            POIFSFileSystem poifsFileSystem = new POIFSFileSystem(new File(path), true);
            document = new HWPFDocument(poifsFileSystem);
            Range range = document.getRange();
            int numParagraphs = range.numParagraphs();

            for (int i = 0; i < numParagraphs; i++) {
                if(sb.length()>READ_TXT_LIMIT){
                    break;
                }
                Paragraph paragraph = range.getParagraph(i);
                int numCharacterRuns = paragraph.numCharacterRuns();
                for (int j = 0; j < numCharacterRuns; j++) {
                    CharacterRun characterRun = paragraph.getCharacterRun(j);
                    final String text = characterRun.text();
                    if (text != null) {
                        sb.append(text);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        return sb.toString();
    }


    public static final String convertPdf2Pdf(Context context, String path) {
        File outfile = FileUtils.getMiaoMiaoPrintFile(FileUtils.OFFICE_PDF_TEMP);
        try {
            final PdfReader pdfReader = new PdfReader(path);
            PdfReaderContentParser pdfReaderContentParser = new PdfReaderContentParser(pdfReader);
            if (outfile != null && !outfile.exists()) {
                boolean newFile = outfile.createNewFile();
                if (!newFile) {
                    return "";
                }
            }
            if (outfile == null) {
                return "";
            }
            if (pdfReaderContentParser != null) {

//                HlhTextRenderListener listener = new HlhTextRenderListener();
                FileOutputStream fos = new FileOutputStream(outfile);
                final Document document = new Document(PageSize.A7);
                document.setMargins(0, 0, 0, 0);
                PdfWriter.getInstance(document, fos);
                document.open();
                File miaoMiaoFontsFile = FileUtils.getMiaoMiaoFontsFile(FileUtils.OFFICE_FONT_FILE);
                if (miaoMiaoFontsFile == null || !miaoMiaoFontsFile.exists()) {
                    boolean newFile = miaoMiaoFontsFile.createNewFile();
                    if (newFile) {
                        //字体设置
                        InputStream open = context.getAssets().open("fonts/simhei.ttf");
                        FileOutputStream fileOutputStream = new FileOutputStream(miaoMiaoFontsFile.getPath());
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = open.read(buffer)) > 0) {
                            fileOutputStream.write(buffer, 0, length);
                        }
                        open.close();
                        fileOutputStream.close();
                        miaoMiaoFontsFile = FileUtils.getMiaoMiaoFontsFile(FileUtils.OFFICE_FONT_FILE);
                        if (miaoMiaoFontsFile == null || !miaoMiaoFontsFile.exists()) {
                            return null;
                        }
                    }
                }
                Font yaHeiFont = new Font(BaseFont.createFont(miaoMiaoFontsFile.getPath(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED));//中文简体

                final StringBuffer stringBuffer = new StringBuffer();
                for (int i = 1; i <= pdfReader.getNumberOfPages(); i++) {
//                                pdfReaderContentParser.processContent(i,listener);
//
//                                // 获取文字的矩形边框
//                                List<Rectangle2D.Float> rectText = listener.rectText;
//                                List<String> textList = listener.textList;
//
//                                List<Map<String, Rectangle2D.Float>> list_text = listener.row_text_rect;
//                                // 文本
//                                StringBuffer stringBuffer = new StringBuffer();
//                                for (int j = 0; j < list_text.size(); j++) {
//                                    Map<String, Rectangle2D.Float> map = list_text.get(j);
//                                    for (Map.Entry<String, Rectangle2D.Float> entry : map.entrySet()) {
//                                        stringBuffer.append(entry.getKey());
//                                    }
//
//                                }
//                                LogUtil.d(stringBuffer.toString());


                    pdfReaderContentParser.processContent(i, new RenderListener() {
                        @Override
                        public void beginTextBlock() {
//                                        stringBuffer = new StringBuffer();
                        }

                        @Override
                        public void renderText(TextRenderInfo renderInfo) {
                            stringBuffer.append(renderInfo.getText());
                        }

                        @Override
                        public void endTextBlock() {
//                                        LogUtil.d(stringBuffer.toString());
                        }

                        @Override
                        public void renderImage(ImageRenderInfo renderInfo) {

                        }
                    });
//                    LogUtil.d(stringBuffer.toString());

                }
                String text = stringBuffer.toString();
                if (TextUtils.isEmpty(text)) {
                    text = "";
                }
                document.add(new com.itextpdf.text.Paragraph(text, yaHeiFont));
                document.close();

            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return outfile.getPath();
    }

}
