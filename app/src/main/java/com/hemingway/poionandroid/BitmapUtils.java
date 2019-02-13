/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hemingway.poionandroid;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;


/**
 * Utility class that deals with operations with an ImageView.
 */
public class BitmapUtils {
    private static final String TAG = BitmapUtils.class.getSimpleName();
    private static final int DEFAULT_JPEG_QUALITY = 90;
    public static final int UNCONSTRAINED = -1;

    public static final Rect EMPTY_RECT = new Rect();
    public static final RectF EMPTY_RECT_F = new RectF();
    /**
     * Reusable rectangle for general internal usage
     */
    public static final RectF RECT = new RectF();
    /**
     * Reusable point for general internal usage
     */
    public static final float[] POINTS = new float[6];
    /**
     * Reusable point for general internal usage
     */
    public static final float[] POINTS2 = new float[6];
    /**
     * Used to know the max texture size allowed to be rendered
     */
    private static int mMaxTextureSize;
    /**
     * used to save bitmaps during state save and restore so not to reload them.
     */
    public static Pair<String, WeakReference<Bitmap>> mStateBitmap;

    private BitmapUtils() {
    }

    /*
     * Compute the sample size as a function of minSideLength
     * and maxNumOfPixels.
     * minSideLength is used to specify that minimal width or height of a
     * bitmap.
     * maxNumOfPixels is used to specify the maximal size in pixels that is
     * tolerable in terms of memory usage.
     *
     * The function returns a sample size based on the constraints.
     * Both size and minSideLength can be passed in as UNCONSTRAINED,
     * which indicates no care of the corresponding constraint.
     * The functions prefers returning a sample size that
     * generates a smaller bitmap, unless minSideLength = UNCONSTRAINED.
     *
     * Also, the function rounds up the sample size to a power of 2 or multiple
     * of 8 because BitmapFactory only honors sample size this way.
     * For example, BitmapFactory downsamples an image by 2 even though the
     * request is 3. So we round up the sample size to avoid OOM.
     */
    public static int computeSampleSize(int width, int height,
                                        int minSideLength, int maxNumOfPixels) {
        int initialSize = computeInitialSampleSize(
                width, height, minSideLength, maxNumOfPixels);

        return initialSize <= 8
                ? nextPowerOf2(initialSize)
                : (initialSize + 7) / 8 * 8;
    }

    private static int computeInitialSampleSize(int w, int h,
                                                int minSideLength, int maxNumOfPixels) {
        if (maxNumOfPixels == UNCONSTRAINED
                && minSideLength == UNCONSTRAINED) return 1;

        int lowerBound = (maxNumOfPixels == UNCONSTRAINED) ? 1 :
                (int) Math.ceil(Math.sqrt((double) (w * h) / maxNumOfPixels));

        if (minSideLength == UNCONSTRAINED) {
            return lowerBound;
        } else {
            int sampleSize = Math.min(w / minSideLength, h / minSideLength);
            return Math.max(sampleSize, lowerBound);
        }
    }

    // This computes a sample size which makes the longer side at least
    // minSideLength long. If that's not possible, return 1.
    public static int computeSampleSizeLarger(int w, int h,
                                              int minSideLength) {
        int initialSize = Math.max(w / minSideLength, h / minSideLength);
        if (initialSize <= 1) return 1;

        return initialSize <= 8
                ? prevPowerOf2(initialSize)
                : initialSize / 8 * 8;
    }

    // Find the min x that 1 / x >= scale
    public static int computeSampleSizeLarger(float scale) {
        int initialSize = (int) Math.floor(1 / scale);
        if (initialSize <= 1) return 1;

        return initialSize <= 8
                ? prevPowerOf2(initialSize)
                : initialSize / 8 * 8;
    }

    // Find the max x that 1 / x <= scale.
    public static int computeSampleSize(float scale) {
        assertTrue(scale > 0);
        int initialSize = Math.max(1, (int) Math.ceil(1 / scale));
        return initialSize <= 8
                ? nextPowerOf2(initialSize)
                : (initialSize + 7) / 8 * 8;
    }

    public static Bitmap resizeBitmapByScale(
            Bitmap bitmap, float scale, boolean recycle) {
        int width = Math.round(bitmap.getWidth() * scale);
        int height = Math.round(bitmap.getHeight() * scale);
        if (width == bitmap.getWidth()
                && height == bitmap.getHeight()) return bitmap;
        Bitmap target = Bitmap.createBitmap(width, height, getConfig(bitmap));
        Canvas canvas = new Canvas(target);
        canvas.scale(scale, scale);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        if (recycle) bitmap.recycle();
        return target;
    }

    private static Bitmap.Config getConfig(Bitmap bitmap) {
        Bitmap.Config config = bitmap.getConfig();
        if (config == null) {
            config = Bitmap.Config.ARGB_8888;
        }
        return config;
    }

    public static Bitmap resizeDownBySideLength(
            Bitmap bitmap, int maxLength, boolean recycle) {
        int srcWidth = bitmap.getWidth();
        int srcHeight = bitmap.getHeight();
        float scale = Math.min(
                (float) maxLength / srcWidth, (float) maxLength / srcHeight);
        if (scale >= 1.0f) return bitmap;
        return resizeBitmapByScale(bitmap, scale, recycle);
    }

    public static Bitmap resizeToWidth(
            Bitmap bitmap, float width, boolean recycle) {
        if(bitmap==null||bitmap.isRecycled()){
            return bitmap;
        }
        if(width<1){
            return bitmap;
        }
        int srcWidth = bitmap.getWidth();
        if(srcWidth<1){
            return bitmap;
        }
//        if(srcWidth>width){
//            return bitmap;
//        }
        float scale = width/srcWidth;
//        if (scale <= 1.0f) return bitmap;
        return resizeBitmapByScale(bitmap, scale, recycle);
    }
    public static Bitmap resizeAndCropCenter(Bitmap bitmap, int size, boolean recycle) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if (w == size && h == size) return bitmap;

        // scale the image so that the shorter side equals to the target;
        // the longer side will be center-cropped.
        float scale = (float) size / Math.min(w, h);

        Bitmap target = Bitmap.createBitmap(size, size, getConfig(bitmap));
        int width = Math.round(scale * bitmap.getWidth());
        int height = Math.round(scale * bitmap.getHeight());
        Canvas canvas = new Canvas(target);
        canvas.translate((size - width) / 2f, (size - height) / 2f);
        canvas.scale(scale, scale);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        if (recycle) bitmap.recycle();
        return target;
    }

    public static void recycleSilently(Bitmap bitmap) {
        if (bitmap == null) return;
        try {
            bitmap.recycle();
        } catch (Throwable t) {
            Log.w(TAG, "unable recycle bitmap", t);
        }
    }

    public static Bitmap rotateBitmap(Bitmap source, int rotation, boolean recycle) {
        if (rotation == 0) return source;
        int w = source.getWidth();
        int h = source.getHeight();
        Matrix m = new Matrix();
        m.postRotate(rotation);
        Bitmap bitmap = Bitmap.createBitmap(source, 0, 0, w, h, m, true);
        if (recycle) source.recycle();
        return bitmap;
    }

    public static Bitmap createVideoThumbnail(String filePath) {
        // MediaMetadataRetriever is available on API Level 8
        // but is hidden until API Level 10
        Class<?> clazz = null;
        Object instance = null;
        try {
            clazz = Class.forName("android.media.MediaMetadataRetriever");
            instance = clazz.newInstance();

            Method method = clazz.getMethod("setDataSource", String.class);
            method.invoke(instance, filePath);

            // The method name changes between API Level 9 and 10.
            if (Build.VERSION.SDK_INT <= 9) {
                return (Bitmap) clazz.getMethod("captureFrame").invoke(instance);
            } else {
                byte[] data = (byte[]) clazz.getMethod("getEmbeddedPicture").invoke(instance);
                if (data != null) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    if (bitmap != null) return bitmap;
                }
                return (Bitmap) clazz.getMethod("getFrameAtTime").invoke(instance);
            }
        } catch (IllegalArgumentException ex) {
            // Assume this is a corrupt video file
        } catch (RuntimeException ex) {
            // Assume this is a corrupt video file.
        } catch (InstantiationException e) {
            Log.e(TAG, "createVideoThumbnail", e);
        } catch (InvocationTargetException e) {
            Log.e(TAG, "createVideoThumbnail", e);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "createVideoThumbnail", e);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "createVideoThumbnail", e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "createVideoThumbnail", e);
        } finally {
            try {
                if (instance != null) {
                    clazz.getMethod("release").invoke(instance);
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public static byte[] compressToBytes(Bitmap bitmap) {
        return compressToBytes(bitmap, DEFAULT_JPEG_QUALITY);
    }

    public static byte[] compressToBytes(Bitmap bitmap, int quality) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(65536);
        bitmap.compress(CompressFormat.JPEG, quality, baos);
        return baos.toByteArray();
    }

    public static boolean isSupportedByRegionDecoder(String mimeType) {
        if (mimeType == null) return false;
        mimeType = mimeType.toLowerCase();
        return mimeType.startsWith("image/") &&
                (!mimeType.equals("image/gif") && !mimeType.endsWith("bmp"));
    }

    public static boolean isRotationSupported(String mimeType) {
        if (mimeType == null) return false;
        mimeType = mimeType.toLowerCase();
        return mimeType.equals("image/jpeg");
    }


    /**
     * Rotate the given image by reading the Exif value of the image (uri).<br>
     * If no rotation is required the image will not be rotated.<br>
     * New bitmap is created and the old one is recycled.
     */
    public static RotateBitmapResult rotateBitmapByExif(Bitmap bitmap, Context context, Uri uri) {
        try {
            File file = getFileFromUri(context, uri);
            if (file.exists()) {
                ExifInterface ei = new ExifInterface(file.getAbsolutePath());
                return rotateBitmapByExif(bitmap, ei);
            }
        } catch (Exception ignored) {
        }
        return new RotateBitmapResult(bitmap, 0);
    }

    /**
     * Rotate the given image by given Exif value.<br>
     * If no rotation is required the image will not be rotated.<br>
     * New bitmap is created and the old one is recycled.
     */
    public static RotateBitmapResult rotateBitmapByExif(Bitmap bitmap, ExifInterface exif) {
        int degrees;
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                degrees = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                degrees = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                degrees = 270;
                break;
            default:
                degrees = 0;
                break;
        }
        return new RotateBitmapResult(bitmap, degrees);
    }

    /**
     * Decode bitmap from stream using sampling to get bitmap with the requested limit.
     */
    public static BitmapSampled decodeSampledBitmap(Context context, Uri uri, int reqWidth, int reqHeight) {

        try {
            ContentResolver resolver = context.getContentResolver();

            // First decode with inJustDecodeBounds=true to check dimensions
            BitmapFactory.Options options = decodeImageForOption(resolver, uri);

            // Calculate inSampleSize
            options.inSampleSize = Math.max(
                    calculateInSampleSizeByReqestedSize(options.outWidth, options.outHeight, reqWidth, reqHeight),
                    calculateInSampleSizeByMaxTextureSize(options.outWidth, options.outHeight));

            // Decode bitmap with inSampleSize set
            Bitmap bitmap = decodeImage(resolver, uri, options);

            return new BitmapSampled(bitmap, options.inSampleSize);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load sampled bitmap: " + uri + "\r\n" + e.getMessage(), e);
        }
    }

    /**
     * Crop image bitmap from given bitmap using the given points in the original bitmap and the given rotation.<br>
     * if the rotation is not 0,90,180 or 270 degrees then we must first crop a larger area of the image that
     * contains the requires rectangle, rotate and then crop again a sub rectangle.<br>
     * If crop fails due to OOM we scale the cropping image by 0.5 every time it fails until it is small enough.
     */
    public static BitmapSampled cropBitmapObjectHandleOOM(Bitmap bitmap, float[] points, int degreesRotated,
                                                          boolean fixAspectRatio, float aspectRatioX, float aspectRatioY) {
        int scale = 1;
        while (true) {
            try {
                Bitmap cropBitmap = cropBitmapObjectWithScale(bitmap, points, degreesRotated, fixAspectRatio, aspectRatioX, aspectRatioY, 1 / (float) scale);
                return new BitmapSampled(cropBitmap, scale);
            } catch (OutOfMemoryError e) {
                scale *= 2;
                if (scale > 8) {
                    throw e;
                }
            }
        }
    }

    /**
     * Crop image bitmap from given bitmap using the given points in the original bitmap and the given rotation.<br>
     * if the rotation is not 0,90,180 or 270 degrees then we must first crop a larger area of the image that
     * contains the requires rectangle, rotate and then crop again a sub rectangle.
     *
     * @param scale how much to scale the cropped image part, use 0.5 to lower the image by half (OOM handling)
     */
    private static Bitmap cropBitmapObjectWithScale(Bitmap bitmap, float[] points, int degreesRotated,
                                                    boolean fixAspectRatio, float aspectRatioX, float aspectRatioY, float scale) {

        // get the rectangle in original image that contains the required cropped area (larger for non rectangular crop)
        Rect rect = getRectFromPoints(points, bitmap.getWidth(), bitmap.getHeight(), fixAspectRatio, aspectRatioX, aspectRatioY);

        // crop and rotate the cropped image in one operation
        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);
        matrix.postRotate(degreesRotated, bitmap.getWidth() / 2, bitmap.getHeight() / 2);
        Bitmap result = Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height(), matrix, true);

        if (result == bitmap) {
            // corner case when all bitmap is selected, no worth optimizing for it
            result = bitmap.copy(bitmap.getConfig(), false);
        }

        // rotating by 0, 90, 180 or 270 degrees doesn't require extra cropping
        if (degreesRotated % 90 != 0) {

            // extra crop because non rectangular crop cannot be done directly on the image without rotating first
            result = cropForRotatedImage(result, points, rect, degreesRotated, fixAspectRatio, aspectRatioX, aspectRatioY);
        }

        return result;
    }

    /**
     * Crop image bitmap from URI by decoding it with specific width and height to down-sample if required.<br>
     * Additionally if OOM is thrown try to increase the sampling (2,4,8).
     */
    public static BitmapSampled cropBitmap(Context context, Uri loadedImageUri, float[] points,
                                           int degreesRotated, int orgWidth, int orgHeight, boolean fixAspectRatio,
                                           float aspectRatioX, float aspectRatioY, int reqWidth, int reqHeight) {
        int sampleMulti = 1;
        while (true) {
            try {
                // if successful, just return the resulting bitmap
                return cropBitmap(context, loadedImageUri, points,
                        degreesRotated, orgWidth, orgHeight, fixAspectRatio,
                        aspectRatioX, aspectRatioY, reqWidth, reqHeight,
                        sampleMulti);
            } catch (OutOfMemoryError e) {
                // if OOM try to increase the sampling to lower the memory usage
                sampleMulti *= 2;
                if (sampleMulti > 16) {
                    throw new RuntimeException("Failed to handle OOM by sampling (" + sampleMulti + "): " + loadedImageUri + "\r\n" + e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Get left value of the bounding rectangle of the given points.
     */
    public static float getRectLeft(float[] points) {
        return Math.min(Math.min(Math.min(points[0], points[2]), points[4]), points[6]);
    }

    /**
     * Get top value of the bounding rectangle of the given points.
     */
    public static float getRectTop(float[] points) {
        return Math.min(Math.min(Math.min(points[1], points[3]), points[5]), points[7]);
    }

    /**
     * Get right value of the bounding rectangle of the given points.
     */
    public static float getRectRight(float[] points) {
        return Math.max(Math.max(Math.max(points[0], points[2]), points[4]), points[6]);
    }

    /**
     * Get bottom value of the bounding rectangle of the given points.
     */
    public static float getRectBottom(float[] points) {
        return Math.max(Math.max(Math.max(points[1], points[3]), points[5]), points[7]);
    }

    /**
     * Get width of the bounding rectangle of the given points.
     */
    public static float getRectWidth(float[] points) {
        return getRectRight(points) - getRectLeft(points);
    }

    /**
     * Get heightof the bounding rectangle of the given points.
     */
    public static float getRectHeight(float[] points) {
        return getRectBottom(points) - getRectTop(points);
    }

    /**
     * Get horizontal center value of the bounding rectangle of the given points.
     */
    public static float getRectCenterX(float[] points) {
        return (getRectRight(points) + getRectLeft(points)) / 2f;
    }

    /**
     * Get verical center value of the bounding rectangle of the given points.
     */
    public static float getRectCenterY(float[] points) {
        return (getRectBottom(points) + getRectTop(points)) / 2f;
    }

    /**
     * Get a rectangle for the given 4 points (x0,y0,x1,y1,x2,y2,x3,y3) by finding the min/max 2 points that
     * contains the given 4 points and is a stright rectangle.
     */
    public static Rect getRectFromPoints(float[] points, int imageWidth, int imageHeight, boolean fixAspectRatio, float aspectRatioX, float aspectRatioY) {
        int left = Math.round(Math.max(0, getRectLeft(points)));
        int top = Math.round(Math.max(0, getRectTop(points)));
        int right = Math.round(Math.min(imageWidth, getRectRight(points)));
        int bottom = Math.round(Math.min(imageHeight, getRectBottom(points)));

        Rect rect = new Rect(left, top, right, bottom);
        if (fixAspectRatio) {
            fixRectForAspectRatio(rect, aspectRatioX, aspectRatioY);
        }

        return rect;
    }

    /**
     * Fix the given rectangle if it doesn't confirm to aspect ration rule.<br>
     * Make sure that width and height are equal if 1:1 fixed aspect ratio is requested.
     */
    private static void fixRectForAspectRatio(Rect rect, float aspectRatioX, float aspectRatioY) {
        if (aspectRatioX == aspectRatioY && rect.width() != rect.height()) {
            if (rect.height() > rect.width()) {
                rect.bottom -= rect.height() - rect.width();
            } else {
                rect.right -= rect.width() - rect.height();
            }
        }
    }

    /**
     * Write the given bitmap to the given uri using the given compression.
     */
    public static void writeBitmapToUri(Context context, Bitmap bitmap, Uri uri, Bitmap.CompressFormat compressFormat, int compressQuality) throws FileNotFoundException {
        OutputStream outputStream = null;
        try {
            outputStream = context.getContentResolver().openOutputStream(uri);
            bitmap.compress(compressFormat, compressQuality, outputStream);
        } finally {
            closeSafe(outputStream);
        }
    }



    //region: Private methods

    /**
     * Crop image bitmap from URI by decoding it with specific width and height to down-sample if required.
     *
     * @param orgWidth used to get rectangle from points (handle edge cases to limit rectangle)
     * @param orgHeight used to get rectangle from points (handle edge cases to limit rectangle)
     * @param sampleMulti used to increase the sampling of the image to handle memory issues.
     */
    private static BitmapSampled cropBitmap(Context context, Uri loadedImageUri, float[] points,
                                            int degreesRotated, int orgWidth, int orgHeight, boolean fixAspectRatio,
                                            float aspectRatioX, float aspectRatioY, int reqWidth, int reqHeight, int sampleMulti) {

        // get the rectangle in original image that contains the required cropped area (larger for non rectangular crop)
        Rect rect = getRectFromPoints(points, orgWidth, orgHeight, fixAspectRatio, aspectRatioX, aspectRatioY);

        int width = reqWidth > 0 ? reqWidth : rect.width();
        int height = reqHeight > 0 ? reqHeight : rect.height();

        Bitmap result = null;
        int sampleSize = 1;
        try {
            // decode only the required image from URI, optionally sub-sampling if reqWidth/reqHeight is given.
            BitmapSampled bitmapSampled = decodeSampledBitmapRegion(context, loadedImageUri, rect, width, height, sampleMulti);
            result = bitmapSampled.bitmap;
            sampleSize = bitmapSampled.sampleSize;
        } catch (Exception ignored) {
        }

        if (result != null) {
            try {
                // rotate the decoded region by the required amount
                result = rotateBitmapInt(result, degreesRotated);

                // rotating by 0, 90, 180 or 270 degrees doesn't require extra cropping
                if (degreesRotated % 90 != 0) {

                    // extra crop because non rectangular crop cannot be done directly on the image without rotating first
                    result = cropForRotatedImage(result, points, rect, degreesRotated, fixAspectRatio, aspectRatioX, aspectRatioY);
                }
            } catch (OutOfMemoryError e) {
                if (result != null) {
                    result.recycle();
                }
                throw e;
            }
            return new BitmapSampled(result, sampleSize);
        } else {
            // failed to decode region, may be skia issue, try full decode and then crop
            return cropBitmap(context, loadedImageUri, points, degreesRotated, fixAspectRatio, aspectRatioX, aspectRatioY, sampleMulti, rect, width, height);
        }
    }

    /**
     * Crop bitmap by fully loading the original and then cropping it, fallback in case cropping region failed.
     */
    private static BitmapSampled cropBitmap(Context context, Uri loadedImageUri, float[] points,
                                            int degreesRotated, boolean fixAspectRatio, float aspectRatioX, float aspectRatioY,
                                            int sampleMulti, Rect rect, int width, int height) {
        Bitmap result = null;
        int sampleSize;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sampleSize = sampleMulti * calculateInSampleSizeByReqestedSize(rect.width(), rect.height(), width, height);

            Bitmap fullBitmap = decodeImage(context.getContentResolver(), loadedImageUri, options);
            if (fullBitmap != null) {
                try {
                    // adjust crop points by the sampling because the image is smaller
                    float[] points2 = new float[points.length];
                    System.arraycopy(points, 0, points2, 0, points.length);
                    for (int i = 0; i < points2.length; i++) {
                        points2[i] = points2[i] / options.inSampleSize;
                    }

                    result = cropBitmapObjectWithScale(fullBitmap, points2, degreesRotated, fixAspectRatio, aspectRatioX, aspectRatioY, 1);
                } finally {
                    if (result != fullBitmap) {
                        fullBitmap.recycle();
                    }
                }
            }
        } catch (OutOfMemoryError e) {
            if (result != null) {
                result.recycle();
            }
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load sampled bitmap: " + loadedImageUri + "\r\n" + e.getMessage(), e);
        }
        return new BitmapSampled(result, sampleSize);
    }

    /**
     * Decode image from uri using "inJustDecodeBounds" to get the image dimensions.
     */
    private static BitmapFactory.Options decodeImageForOption(ContentResolver resolver, Uri uri) throws FileNotFoundException {
        InputStream stream = null;
        try {
            stream = resolver.openInputStream(uri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(stream, EMPTY_RECT, options);
            options.inJustDecodeBounds = false;
            return options;
        } finally {
            closeSafe(stream);
        }
    }

    /**
     * Decode image from uri using given "inSampleSize", but if failed due to out-of-memory then raise
     * the inSampleSize until success.
     */
    private static Bitmap decodeImage(ContentResolver resolver, Uri uri, BitmapFactory.Options options) throws FileNotFoundException {
        do {
            InputStream stream = null;
            try {
                stream = resolver.openInputStream(uri);
                return BitmapFactory.decodeStream(stream, EMPTY_RECT, options);
            } catch (OutOfMemoryError e) {
                options.inSampleSize *= 2;
            } finally {
                closeSafe(stream);
            }
        } while (options.inSampleSize <= 512);
        throw new RuntimeException("Failed to decode image: " + uri);
    }

    /**
     * Decode specific rectangle bitmap from stream using sampling to get bitmap with the requested limit.
     *
     * @param sampleMulti used to increase the sampling of the image to handle memory issues.
     */
    private static BitmapSampled decodeSampledBitmapRegion(Context context, Uri uri, Rect rect, int reqWidth, int reqHeight, int sampleMulti) {
        InputStream stream = null;
        BitmapRegionDecoder decoder = null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sampleMulti * calculateInSampleSizeByReqestedSize(rect.width(), rect.height(), reqWidth, reqHeight);

            stream = context.getContentResolver().openInputStream(uri);
            decoder = BitmapRegionDecoder.newInstance(stream, false);
            do {
                try {
                    return new BitmapSampled(decoder.decodeRegion(rect, options), options.inSampleSize);
                } catch (OutOfMemoryError e) {
                    options.inSampleSize *= 2;
                }
            } while (options.inSampleSize <= 512);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load sampled bitmap: " + uri + "\r\n" + e.getMessage(), e);
        } finally {
            closeSafe(stream);
            if (decoder != null) {
                decoder.recycle();
            }
        }
        return new BitmapSampled(null, 1);
    }

    /**
     * Special crop of bitmap rotated by not stright angle, in this case the original crop bitmap contains parts
     * beyond the required crop area, this method crops the already cropped and rotated bitmap to the final
     * rectangle.<br>
     * Note: rotating by 0, 90, 180 or 270 degrees doesn't require extra cropping.
     */
    private static Bitmap cropForRotatedImage(Bitmap bitmap, float[] points, Rect rect, int degreesRotated,
                                              boolean fixAspectRatio, float aspectRatioX, float aspectRatioY) {
        if (degreesRotated % 90 != 0) {

            int adjLeft = 0, adjTop = 0, width = 0, height = 0;
            double rads = Math.toRadians(degreesRotated);
            int compareTo = degreesRotated < 90 || (degreesRotated > 180 && degreesRotated < 270) ? rect.left : rect.right;
            for (int i = 0; i < points.length; i += 2) {
                if (points[i] >= compareTo - 1 && points[i] <= compareTo + 1) {
                    adjLeft = (int) Math.abs(Math.sin(rads) * (rect.bottom - points[i + 1]));
                    adjTop = (int) Math.abs(Math.cos(rads) * (points[i + 1] - rect.top));
                    width = (int) Math.abs((points[i + 1] - rect.top) / Math.sin(rads));
                    height = (int) Math.abs((rect.bottom - points[i + 1]) / Math.cos(rads));
                    break;
                }
            }

            rect.set(adjLeft, adjTop, adjLeft + width, adjTop + height);
            if (fixAspectRatio) {
                fixRectForAspectRatio(rect, aspectRatioX, aspectRatioY);
            }

            Bitmap bitmapTmp = bitmap;
            bitmap = Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height());
            if (bitmapTmp != bitmap) {
                bitmapTmp.recycle();
            }
        }
        return bitmap;
    }

    /**
     * Calculate the largest inSampleSize value that is a power of 2 and keeps both
     * height and width larger than the requested height and width.
     */
    private static int calculateInSampleSizeByReqestedSize(int width, int height, int reqWidth, int reqHeight) {
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            while ((height / 2 / inSampleSize) > reqHeight && (width / 2 / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    /**
     * Calculate the largest inSampleSize value that is a power of 2 and keeps both
     * height and width smaller than max texture size allowed for the device.
     */
    private static int calculateInSampleSizeByMaxTextureSize(int width, int height) {
        int inSampleSize = 1;
        if (mMaxTextureSize == 0) {
            mMaxTextureSize = getMaxTextureSize();
        }
        if (mMaxTextureSize > 0) {
            while ((height / inSampleSize) > mMaxTextureSize || (width / inSampleSize) > mMaxTextureSize) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    /**
     * Get {@link File} object for the given Android URI.<br>
     * Use content resolver to get real path if direct path doesn't return valid file.
     */
    private static File getFileFromUri(Context context, Uri uri) {

        // first try by direct path
        File file = new File(uri.getPath());
        if (file.exists()) {
            return file;
        }

        // try reading real path from content resolver (gallery images)
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = context.getContentResolver().query(uri, proj, null, null, null);
            if (cursor != null) {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                String realPath = cursor.getString(column_index);
                file = new File(realPath);
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return file;
    }

    /**
     * Rotate the given bitmap by the given degrees.<br>
     * New bitmap is created and the old one is recycled.
     */
    private static Bitmap rotateBitmapInt(Bitmap bitmap, int degrees) {
        if (degrees > 0) {
            Matrix matrix = new Matrix();
            matrix.setRotate(degrees);
            Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
            if (newBitmap != bitmap) {
                bitmap.recycle();
            }
            return newBitmap;
        } else {
            return bitmap;
        }
    }

    /**
     * Get the max size of bitmap allowed to be rendered on the device.<br>
     * http://stackoverflow.com/questions/7428996/hw-accelerated-activity-how-to-get-opengl-texture-size-limit.
     */
    private static int getMaxTextureSize() {
        // Safe minimum default size
        final int IMAGE_MAX_BITMAP_DIMENSION = 2048;

        try {
            // Get EGL Display
            EGL10 egl = (EGL10) EGLContext.getEGL();
            EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

            // Initialise
            int[] version = new int[2];
            egl.eglInitialize(display, version);

            // Query total number of configurations
            int[] totalConfigurations = new int[1];
            egl.eglGetConfigs(display, null, 0, totalConfigurations);

            // Query actual list configurations
            EGLConfig[] configurationsList = new EGLConfig[totalConfigurations[0]];
            egl.eglGetConfigs(display, configurationsList, totalConfigurations[0], totalConfigurations);

            int[] textureSize = new int[1];
            int maximumTextureSize = 0;

            // Iterate through all the configurations to located the maximum texture size
            for (int i = 0; i < totalConfigurations[0]; i++) {
                // Only need to check for width since opengl textures are always squared
                egl.eglGetConfigAttrib(display, configurationsList[i], EGL10.EGL_MAX_PBUFFER_WIDTH, textureSize);

                // Keep track of the maximum texture size
                if (maximumTextureSize < textureSize[0]) {
                    maximumTextureSize = textureSize[0];
                }
            }

            // Release
            egl.eglTerminate(display);

            // Return largest texture size found, or default
            return Math.max(maximumTextureSize, IMAGE_MAX_BITMAP_DIMENSION);
        } catch (Exception e) {
            return IMAGE_MAX_BITMAP_DIMENSION;
        }
    }

    /**
     * Close the given closeable object (Stream) in a safe way: check if it is null and catch-log
     * exception thrown.
     *
     * @param closeable the closable object to close
     */
    private static void closeSafe(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
            }
        }
    }
    //endregion

    //region: Inner class: BitmapSampled

    /**
     * Holds bitmap instance and the sample size that the bitmap was loaded/cropped with.
     */
    public static final class BitmapSampled {

        /**
         * The bitmap instance
         */
        public final Bitmap bitmap;

        /**
         * The sample size used to lower the size of the bitmap (1,2,4,8,...)
         */
        public final int sampleSize;

        BitmapSampled(Bitmap bitmap, int sampleSize) {
            this.bitmap = bitmap;
            this.sampleSize = sampleSize;
        }
    }
    //endregion

    //region: Inner class: RotateBitmapResult

    /**
     * The result of {@link #rotateBitmapByExif(Bitmap, ExifInterface)}.
     */
    public static final class RotateBitmapResult {

        /**
         * The loaded bitmap
         */
        public final Bitmap bitmap;

        /**
         * The degrees the image was rotated
         */
        public final int degrees;

        RotateBitmapResult(Bitmap bitmap, int degrees) {
            this.bitmap = bitmap;
            this.degrees = degrees;
        }
    }

    // Throws AssertionError if the input is false.
    public static void assertTrue(boolean cond) {
        if (!cond) {
            throw new AssertionError();
        }
    }

    // Returns the next power of two.
    // Returns the input if it is already power of 2.
    // Throws IllegalArgumentException if the input is <= 0 or
    // the answer overflows.
    public static int nextPowerOf2(int n) {
        if (n <= 0 || n > (1 << 30)) throw new IllegalArgumentException("n is invalid: " + n);
        n -= 1;
        n |= n >> 16;
        n |= n >> 8;
        n |= n >> 4;
        n |= n >> 2;
        n |= n >> 1;
        return n + 1;
    }

    // Returns the previous power of two.
    // Returns the input if it is already power of 2.
    // Throws IllegalArgumentException if the input is <= 0
    public static int prevPowerOf2(int n) {
        if (n <= 0) throw new IllegalArgumentException();
        return Integer.highestOneBit(n);
    }

    private static final int BITMAP_MAX_SIZE = 300;//k
    public static String getBase64FromBitmap(Bitmap bitmap) {
        String base64Str = null;
        ByteArrayOutputStream baos = null;
        try {
            if (bitmap != null) {
                baos = new ByteArrayOutputStream();
                int quality = 100;
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
                byte[] e = baos.toByteArray();
                long length = e.length;

                while (length / 1024.0f > BITMAP_MAX_SIZE) {
                    //压缩到500k 以内
                    quality -= 3;
                    if (quality < 5) {
                        break;
                    }
                    baos.reset();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
                    length = baos.toByteArray().length;
                }
                base64Str = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);

                baos.close();
            }
        } catch (IOException var12) {
            var12.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.flush();
                    baos.close();
                }
            } catch (IOException var11) {
                var11.printStackTrace();
            }
        }
        return base64Str;
    }


    /**
     * 把两个位图覆盖合成为一个位图，上下拼接
     * @param topBitmap
     * @param bottomBitmap
     * @param isBaseMax 是否以高度大的位图为准，true则小图等比拉伸，false则大图等比压缩
     * @return
     */
    public static Bitmap mergeBitmap_TB(Bitmap topBitmap, Bitmap bottomBitmap, boolean isBaseMax) {

        if (topBitmap == null || topBitmap.isRecycled()
                || bottomBitmap == null || bottomBitmap.isRecycled()) {
            return null;
        }
        int width = 0;
        if (isBaseMax) {
            width = topBitmap.getWidth() > bottomBitmap.getWidth() ? topBitmap.getWidth() : bottomBitmap.getWidth();
        } else {
            width = topBitmap.getWidth() < bottomBitmap.getWidth() ? topBitmap.getWidth() : bottomBitmap.getWidth();
        }
        Bitmap tempBitmapT = topBitmap;
        Bitmap tempBitmapB = bottomBitmap;

        if (topBitmap.getWidth() != width) {
            tempBitmapT = Bitmap.createScaledBitmap(topBitmap, width, (int)(topBitmap.getHeight()*1f/topBitmap.getWidth()*width), false);
        } else if (bottomBitmap.getWidth() != width) {
            tempBitmapB = Bitmap.createScaledBitmap(bottomBitmap, width, (int)(bottomBitmap.getHeight()*1f/bottomBitmap.getWidth()*width), false);
        }

        int height = tempBitmapT.getHeight() + tempBitmapB.getHeight();

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Rect topRect = new Rect(0, 0, tempBitmapT.getWidth(), tempBitmapT.getHeight());
        Rect bottomRect  = new Rect(0, 0, tempBitmapB.getWidth(), tempBitmapB.getHeight());

        Rect bottomRectT  = new Rect(0, tempBitmapT.getHeight(), width, height);

        canvas.drawBitmap(tempBitmapT, topRect, topRect, null);
        canvas.drawBitmap(tempBitmapB, bottomRect, bottomRectT, null);
        return bitmap;
    }

    public static Bitmap drawWhiteBgBitmap(Bitmap originBitmap){
        return drawBg4Bitmap(0xffffffff,originBitmap);
    }
    public static Bitmap drawBg4Bitmap(int color, Bitmap originBitmap) {
        Paint paint = new Paint();
        paint.setColor(color);
        Bitmap bitmap = Bitmap.createBitmap(originBitmap.getWidth(),
                originBitmap.getHeight(), originBitmap.getConfig());
        Canvas canvas = new Canvas(bitmap);
        canvas.drawRect(0, 0, originBitmap.getWidth(), originBitmap.getHeight(), paint);
        canvas.drawBitmap(originBitmap, 0, 0, paint);
        return bitmap;
    }
}
