package com.hemingway.poionandroid;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;

public class PPTActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_ppt);
//
//        System.setProperty("Dimension",
//                "org.apache.poi.Dimension");
        final View progressBar = findViewById(R.id.progressBar);
        final ImageView ivImage = findViewById(R.id.image);
        new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream inputStream = getResources().openRawResource(R.raw.talkaboutjvm);
//                try {
//                    XMLSlideShow ppt = new XMLSlideShow(inputStream);
//                    Dimension pgsize = ppt.getPageSize();
//
//
//                    List<XSLFSlide> slides = ppt.getSlides();
//
//                    final Bitmap bmp = Bitmap.createBitmap((int) pgsize.getWidth(),
//                            (int) pgsize.getHeight(), Bitmap.Config.RGB_565);
//                    Canvas canvas = new Canvas(bmp);
//                    Paint paint = new Paint();
//                    paint.setColor(android.graphics.Color.WHITE);
//                    paint.setFlags(Paint.ANTI_ALIAS_FLAG);
//                    canvas.drawPaint(paint);
//
//
//                    XSLFSlide xslfShapes = slides.get(0);
//                    CTSlide xmlObject = xslfShapes.getXmlObject();
//
//                    Graphics2D graphice = new Graphics2D(canvas);
//
//                    xslfShapes.draw(graphice);
//                    int size = slides.size();
//
//                    List<XSLFPictureData> pictureData = ppt.getPictureData();
//                    XSLFPictureData xslfPictureData = pictureData.get(0);
//                    byte[] data = xslfPictureData.getData();
//                    final Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
//                    ivImage.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            ivImage.setImageBitmap(bitmap);
//                            progressBar.setVisibility(View.GONE);
//                        }
//                    });

//            ppt.getPageSize();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
            }
        }).start();

    }
}
