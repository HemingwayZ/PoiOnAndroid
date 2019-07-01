package com.hemingway.poionandroid;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import net.pbdavey.awt.Graphics2D;

import org.apache.poi2.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi2.openxml4j.opc.OPCPackage;
import org.apache.poi2.openxml4j.opc.PackageAccess;
import org.apache.poi2.xslf.usermodel.XMLSlideShow;
import org.apache.poi2.xslf.usermodel.XSLFSlide;

import java.io.StringReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import and.awt.Dimension;

/**
 * 如果出现异常情况
 */
public class PPTActivity extends AppCompatActivity {
    Bitmap bitmap = null;
    int count = 0;
    private View progressBar;
    private ImageView ivImage;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.setProperty("javax.xml.stream.XMLInputFactory",
                "com.sun.xml.stream.ZephyrParserFactory");
        System.setProperty("javax.xml.stream.XMLOutput2Factory",
                "com.sun.xml.stream.ZephyrWriterFactory");
        System.setProperty("javax.xml.stream.XMLEventFactory",
                "com.sun.xml.stream.events.ZephyrEventFactory");

        Thread.currentThread().setContextClassLoader(
                getClass().getClassLoader());

        // some test
        try {
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            XMLEventReader reader = inputFactory
                    .createXMLEventReader(new StringReader(
                            "<doc att=\"value\">some text</doc>"));
            while (reader.hasNext()) {
                XMLEvent e = reader.nextEvent();
                Log.e("HelloStax", "Event:[" + e + "]");
            }
        } catch (XMLStreamException e) {
            Log.e("HelloStax", "Error parsing XML", e);
        }
        //27秒
//        System.setProperty("org.apache.poi.javax.xml.stream.XMLInputFactory", "com.fasterxml.aalto.stax.InputFactoryImpl");
//        System.setProperty("org.apache.poi.javax.xml.stream.XMLOutputFactory", "com.fasterxml.aalto.stax.OutputFactoryImpl");
//        System.setProperty("org.apache.poi.javax.xml.stream.XMLEventFactory", "com.fasterxml.aalto.stax.EventFactoryImpl");
        setContentView(R.layout.activity_ppt);
//
//        System.setProperty("Dimension",
//                "org.apache.poi.Dimension");
        handler = new Handler();
        progressBar = findViewById(R.id.progressBar);
        ivImage = findViewById(R.id.image);


//       loadMultiThread();
        loadSingleThread();

    }

    private void loadSingleThread() {
        count = 0;
        new Thread(new Runnable() {
            @Override
            public void run() {
//                InputStream inputStream = getResources().openRawResource(R.raw.talkaboutjvm);
                String path = "/sdcard/talkaboutjvm.pptx";
                try {

//                    XMLSlideShow ppt = new XMLSlideShow(inputStream);
                    final long before = System.currentTimeMillis();
                    XMLSlideShow ppt = new XMLSlideShow(OPCPackage.open(path,
                            PackageAccess.READ));

                    final Dimension pgsize = ppt.getPageSize();


//                    PPTX2PNG

                    final XSLFSlide[] slides = ppt.getSlides();

//                    long after = System.currentTimeMillis();
//                    Log.d("Hemingway", "init time(s) : " + (after / 1000 - before / 1000));
                    bitmap = Bitmap.createBitmap(1000, 1, Bitmap.Config.ARGB_8888);
                    for (int i = 0; i < slides.length; i++) {
                        Log.d("Hemingway", "================ i = " + i + " ================");
                        XSLFSlide slide = slides[i];
                        final Bitmap bmp = Bitmap.createBitmap((int) pgsize.getWidth(),
                                (int) pgsize.getHeight(), Bitmap.Config.RGB_565);
                        Canvas canvas = new Canvas(bmp);
                        Paint paint = new Paint();
                        paint.setColor(android.graphics.Color.WHITE);
                        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
                        canvas.drawPaint(paint);
                        final Graphics2D graphice = new Graphics2D(canvas);
                        slide.draw(graphice, new AtomicBoolean(false), handler, 0);
                        Bitmap bitmap1 = BitmapUtils.rotateBitmap(bmp, 0, false);
                        bitmap = BitmapUtils.mergeBitmap_TB(bitmap, bitmap1, true);
                        if (i >= 10) {
                            break;
                        }
                    }
                    Log.d("Hemingway", "parse time(s) : " + (System.currentTimeMillis() / 1000 - before / 1000));
                    final Bitmap finalBitmap = bitmap;
                    ivImage.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            ivImage.setImageBitmap(finalBitmap);
                            progressBar.setVisibility(View.GONE);
                        }
                    }, 10);
                    ppt.getPageSize();
                } catch (InvalidFormatException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void loadMultiThread() {
        count = 0;
        new Thread(new Runnable() {
            @Override
            public void run() {
//                InputStream inputStream = getResources().openRawResource(R.raw.talkaboutjvm);
                String path = "/sdcard/talkaboutjvm.pptx";
                try {

//                    XMLSlideShow ppt = new XMLSlideShow(inputStream);
                    final long before = System.currentTimeMillis();
                    XMLSlideShow ppt = new XMLSlideShow(OPCPackage.open(path,
                            PackageAccess.READ));

                    final Dimension pgsize = ppt.getPageSize();


//                    PPTX2PNG

                    final XSLFSlide[] slides = ppt.getSlides();

//                    long after = System.currentTimeMillis();
//                    Log.d("Hemingway", "init time(s) : " + (after / 1000 - before / 1000));
                    bitmap = Bitmap.createBitmap(1000, 1, Bitmap.Config.ARGB_8888);
                    final ExecutorService executorService = Executors.newFixedThreadPool(3);
                    for (int i = 0; i < slides.length; i++) {
                        Log.d("Hemingway", "================ i = " + i + " ================");
                        final int finalI = i;
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                executorService.submit(new Runnable() {
                                    @Override
                                    public void run() {
                                        String threadName = Thread.currentThread().getName();
                                        Log.d("Hemingway",finalI + "_Thread:"+threadName);
                                        XSLFSlide slide = slides[finalI];
                                        final Bitmap bmp = Bitmap.createBitmap((int) pgsize.getWidth(),
                                                (int) pgsize.getHeight(), Bitmap.Config.RGB_565);
                                        Canvas canvas = new Canvas(bmp);
                                        Paint paint = new Paint();
                                        paint.setColor(android.graphics.Color.WHITE);
                                        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
                                        canvas.drawPaint(paint);
                                        final Graphics2D graphice = new Graphics2D(canvas);
                                        slide.draw(graphice, new AtomicBoolean(false), handler, 0);
                                        Bitmap bitmap1 = BitmapUtils.rotateBitmap(bmp, 0, false);
                                        bitmap = BitmapUtils.mergeBitmap_TB(bitmap, bitmap1, true);
                                        count++;
                                        if (count == slides.length) {
                                            long after = System.currentTimeMillis();
                                            Log.d("Hemingway", "parse time(s) : " + (after / 1000 - before / 1000));
                                            final Bitmap finalBitmap = bitmap;
                                            ivImage.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    ivImage.setImageBitmap(finalBitmap);
                                                    progressBar.setVisibility(View.GONE);
                                                }
                                            }, 10);
                                        }
                                    }
                                });
                            }
                        });

                        if (i >= 10) {
                            break;
                        }
                    }
                    ppt.getPageSize();
                } catch (InvalidFormatException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
