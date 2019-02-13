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

        setContentView(R.layout.activity_ppt);
//
//        System.setProperty("Dimension",
//                "org.apache.poi.Dimension");
        final Handler handler = new Handler();
        final View progressBar = findViewById(R.id.progressBar);
        final ImageView ivImage = findViewById(R.id.image);
        new Thread(new Runnable() {
            @Override
            public void run() {
//                InputStream inputStream = getResources().openRawResource(R.raw.talkaboutjvm);
               String path = "/sdcard/test1.pptx";
                try {
//                    XMLSlideShow ppt = new XMLSlideShow(inputStream);
                    XMLSlideShow ppt = new XMLSlideShow(OPCPackage.open(path,
                            PackageAccess.READ));

                    Dimension pgsize = ppt.getPageSize();

                    final Bitmap bmp = Bitmap.createBitmap((int) pgsize.getWidth(),
                            (int) pgsize.getHeight(), Bitmap.Config.RGB_565);
                    Canvas canvas = new Canvas(bmp);
                    Paint paint = new Paint();
                    paint.setColor(android.graphics.Color.WHITE);
                    paint.setFlags(Paint.ANTI_ALIAS_FLAG);
                    canvas.drawPaint(paint);

                    XSLFSlide[] slides = ppt.getSlides();
                    XSLFSlide slide = slides[2];

                    Graphics2D graphice = new Graphics2D(canvas);
                    final AtomicBoolean isCanceled = new AtomicBoolean(false);
                    slide.draw(graphice,isCanceled,handler,0);

                    ivImage.post(new Runnable() {
                        @Override
                        public void run() {
                            ivImage.setImageBitmap(bmp);
                            progressBar.setVisibility(View.GONE);
                        }
                    });

            ppt.getPageSize();
                } catch (InvalidFormatException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }
}
