package com.hemingway.poionandroid;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;

/**
 * Created by ZQ.X on 2017/1/16.
 * updated by hemingway
 */

public class FileUtils {
    private static final String TAG = FileUtils.class.getSimpleName();
    public static final String IMAGE_FILE = "/images/";
    private static final String PRINT_HISTORY = "/print/";//打印图片历史文件
    public static final String FONTS_FILE = "fonts";
    public static final String IMAGES_FILE = "images";
    public static final String FONTS_TEMP_FILE = ".temp";

    public static final String OFFICE_FONT_FILE = ".android_font.ttf";
    public static final String OFFICE_PDF_TEMP = ".paperang_temp.pdf";
//    /**
//     * android  N 适配 FileProvider，获取uri
//     *
//     * @param context
//     * @param imageFileName
//     * @return
//     */
//    public static Uri getFileProviderUri(Context context, String imageFileName) {
//        if (TextUtils.isEmpty(imageFileName)) {
//            imageFileName = Const.Path.TMP_IMG_PHOTO;
//        }
//        File fileByName = com.lib_utils.FileUtils.getFileByName(Const.Path.APPLICATION, Const.Path.TMP, imageFileName);
//        return FileProvider.getUriForFile(context, Const.FILE_PROVIDER_AUTHORITY, fileByName);
//    }

    public static File getMiaoMiaoImagesFile(String temName) {

        File fileMiaoMiao = new File(Environment.getExternalStorageDirectory(), "APP_CACHE_ROOT_DIR");
        if (!fileMiaoMiao.exists()) {
            if (!fileMiaoMiao.mkdir()) {
                return null;
            }
        }
        File fileImages = new File(fileMiaoMiao, IMAGES_FILE);
        if (!fileImages.exists()) {
            if (!fileImages.mkdir()) {
                return null;
            }
        }
        return new File(fileImages, temName);
    }



    /**
     * 获取miaomiao文件夹下的二级文件夹
     *
     * @param fileInMiaoMiao
     * @return
     */
    public static File getMiaoMiaoFile(String fileInMiaoMiao) {

        File fileMiaoMiao = new File(Environment.getExternalStorageDirectory(),"APP_CACHE_ROOT_DIR");
        if (!fileMiaoMiao.exists()) {
            if (!fileMiaoMiao.mkdir()) {
                return null;
            }
        }
        File fileFonts = new File(fileMiaoMiao, fileInMiaoMiao);
        if (!fileFonts.exists()) {
            if (!fileFonts.mkdir()) {
                return null;
            }
        }
        return fileFonts;
    }

    /**
     * 获取字体文件夹下的字体文件
     *
     * @param printName
     * @return
     */
    public static File getMiaoMiaoPrintFile(String printName) {

        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            return null;
        }
        File externalStorageDirectory = Environment.getExternalStorageDirectory();
        File file = new File(externalStorageDirectory, "APPLICATION_DIR");
        if (!file.exists()) {
            boolean mkdir = file.mkdir();
            if (!mkdir) {
                Log.e(TAG, "APP_CACHE_ROOT_DIR is not exist");
                return null;
            }
        }
        File fileImage = new File(file, PRINT_HISTORY);
        if (!fileImage.exists()) {
            boolean mkdir = fileImage.mkdir();
            if (!mkdir) {
                Log.e(TAG, "IMAGE_FILE is not exist");
                return null;
            }
        }
        //图片存储
        return new File(fileImage, printName);
    }

    /**
     * 获取字体文件夹下的字体文件
     *
     * @param fontName
     * @return
     */
    public static File getMiaoMiaoFontsFile(String fontName) {

        File fileMiaoMiao = new File(Environment.getExternalStorageDirectory(), "APP_CACHE_ROOT_DIR");
        if (!fileMiaoMiao.exists()) {
            if (!fileMiaoMiao.mkdir()) {
                return null;
            }
        }
        File fileImages = new File(fileMiaoMiao, FONTS_FILE);
        if (!fileImages.exists()) {
            if (!fileImages.mkdir()) {
                return null;
            }
        }
        return new File(fileImages, fontName);
    }

    /**
     * 获取下载字体文件夹下的字体文件
     *
     * @param fontTempName
     * @return
     */
    public static File getMiaoMiaoTempFontsFile(String fontTempName) {

        File tempFile = getMiaoMiaoFontsFile(FONTS_TEMP_FILE);
        if (tempFile == null) {
            return null;
        }
        if (!tempFile.exists()) {
            tempFile.mkdir();
        }
        return new File(tempFile, fontTempName);
    }

    /**
     * 保存打印后的图片
     *
     * @param bitmap        需要保存的bitmap
     * @param fileName      需要保存的文件名
     * @param isNeedRecycle bitmap保存后是否需要回收
     * @return
     */
    public static boolean savePrintHistoryImage(Bitmap bitmap, String fileName, boolean isNeedRecycle) {
        if (bitmap == null) {
            return false;
        }
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            return false;
        }
        File externalStorageDirectory = Environment.getExternalStorageDirectory();
        File file = new File(externalStorageDirectory, "APPLICATION_DIR");
        if (!file.exists()) {
            boolean mkdir = file.mkdir();
            if (!mkdir) {
                Log.e(TAG, "APP_CACHE_ROOT_DIR is not exist");
                return false;
            }
        }
        File fileImage = new File(file, PRINT_HISTORY);
        if (!fileImage.exists()) {
            boolean mkdir = fileImage.mkdir();
            if (!mkdir) {
                Log.e(TAG, "IMAGE_FILE is not exist");
                return false;
            }
        }
        //图片存储
        File nameFile = new File(fileImage, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(nameFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        if (isNeedRecycle) {
            bitmap.recycle();
        }
        return true;
    }



    /**
     * 复制单个文件
     *
     * @param oldPath String 原文件路径 如：c:/fqf.txt
     * @param newPath String 复制后路径 如：f:/fqf.txt
     * @return boolean
     */
    public static void copyFile(String oldPath, String newPath) {
        try {
            int byteSum = 0;
            int byteRead = 0;
            File oldFile = new File(oldPath);
            if (!oldFile.exists()) {
                return;
            }
            if (!oldFile.isFile()) {
                return;
            }
            if (!oldFile.canRead()) {
                return;
            }
            if (oldFile.exists()) { //文件存在时
                InputStream inStream = new FileInputStream(oldPath); //读入原文件
                FileOutputStream fs = new FileOutputStream(newPath);
                byte[] buffer = new byte[1024];
                int length;
                while ((byteRead = inStream.read(buffer)) != -1) {
                    byteSum += byteRead; //字节数 文件大小
                    System.out.println(byteSum);
                    fs.write(buffer, 0, byteRead);
                }
                inStream.close();
            }
        } catch (Exception e) {
            System.out.println("Copy File Error");
            e.printStackTrace();

        }

    }

    /**
     * 复制整个文件夹内容
     *
     * @param oldPath String 原文件路径 如：c:/fqf
     * @param newPath String 复制后路径 如：f:/fqf/ff
     * @return boolean
     */
    public static void copyFolder(String oldPath, String newPath) {

        try {
            (new File(newPath)).mkdirs(); //如果文件夹不存在 则建立新文件夹
            File oldFile = new File(oldPath);
            if (!oldFile.exists()) {
                return;
            }
            if (!oldFile.isFile()) {
                return;
            }
            if (!oldFile.canRead()) {
                return;
            }
            String[] file = oldFile.list();
            File temp = null;
            for (int i = 0; i < file.length; i++) {
                if (oldPath.endsWith(File.separator)) {
                    temp = new File(oldPath + file[i]);
                } else {
                    temp = new File(oldPath + File.separator + file[i]);
                }

                if (temp.isFile()) {
                    FileInputStream input = new FileInputStream(temp);
                    FileOutputStream output = new FileOutputStream(newPath + "/" +
                            (temp.getName()).toString());
                    byte[] b = new byte[1024 * 5];
                    int len;
                    while ((len = input.read(b)) != -1) {
                        output.write(b, 0, len);
                    }
                    output.flush();
                    output.close();
                    input.close();
                }
                if (temp.isDirectory()) {//如果是子文件夹
                    copyFolder(oldPath + "/" + file[i], newPath + "/" + file[i]);
                }
            }
        } catch (Exception e) {
            System.out.println("Copy Folder Error");
            e.printStackTrace();
        }

    }

    /*文件大小读取 删除*/

    /**
     * 格式化单位
     *
     * @param size
     */
    public static String getFormatSize(double size) {
        if (size < 1024) {
            return size + "B";
        }
        double kiloByte = size / 1024;
        if (kiloByte < 1) {
            return 0 + "K";
        }

        double megaByte = kiloByte / 1024;
        if (megaByte < 1) {
            BigDecimal result1 = new BigDecimal(Double.toString(kiloByte));
            return result1.setScale(2, BigDecimal.ROUND_HALF_UP)
                    .toPlainString() + "K";
        }

        double gigaByte = megaByte / 1024;
        if (gigaByte < 1) {
            BigDecimal result2 = new BigDecimal(Double.toString(megaByte));
            return result2.setScale(2, BigDecimal.ROUND_HALF_UP)
                    .toPlainString() + "M";
        }

        double teraBytes = gigaByte / 1024;
        if (teraBytes < 1) {
            BigDecimal result3 = new BigDecimal(Double.toString(gigaByte));
            return result3.setScale(2, BigDecimal.ROUND_HALF_UP)
                    .toPlainString() + "G";
        }
        BigDecimal result4 = new BigDecimal(teraBytes);
        return result4.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString()
                + "T";
    }


    // 获取文件
    //Context.getExternalFilesDir() --> SDCard/Android/data/你的应用的包名/files/ 目录，一般放一些长时间保存的数据
    //Context.getExternalCacheDir() --> SDCard/Android/data/你的应用包名/cache/目录，一般存放临时缓存数据
    public static long getFolderSize(File file) throws Exception {
        long size = 0;
        try {
            File[] fileList = file.listFiles();
            for (int i = 0; i < fileList.length; i++) {
                // 如果下面还有文件
                if (fileList[i].isDirectory()) {
                    size = size + getFolderSize(fileList[i]);
                } else {
                    size = size + fileList[i].length();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return size;
    }

    /**
     * 删除指定目录下文件及目录
     *
     * @param deleteThisPath
     * @param file
     * @return
     */
    public static void deleteFolderFile(File file, boolean deleteThisPath) {
        try {
            if (file.isDirectory()) {// 如果下面还有文件
                File files[] = file.listFiles();
                for (int i = 0; i < files.length; i++) {
                    deleteFolderFile(files[i], true);
                }
            }
            if (deleteThisPath) {
                if (!file.isDirectory()) {// 如果是文件，删除
                    file.delete();
                } else {// 目录
                    if (file.listFiles().length == 0) {// 目录下没有文件或者目录，删除
                        file.delete();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 保存图片到指定File
     * @param bmp
     */
    public static void saveBitmap(Bitmap bmp, File file) {
        if(file.exists()) {
            file.delete();
        }

        try {
            FileOutputStream e = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, e);
            e.flush();
            e.close();
        } catch (FileNotFoundException var5) {
            var5.printStackTrace();
        } catch (IOException var6) {
            var6.printStackTrace();
        }

    }


}
