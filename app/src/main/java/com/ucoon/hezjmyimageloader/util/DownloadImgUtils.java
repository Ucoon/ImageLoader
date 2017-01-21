package com.ucoon.hezjmyimageloader.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by ZongJie on 2017/1/17.
 */

public class DownloadImgUtils {

    /**
     * 根据url下载图片在指定的文件(前提：当SD卡挂载时，可直接下载到本地文件夹)
     * @param urlStr
     * @param file
     * @return
     */
    public static boolean downloadImgByUrl(String urlStr, File file) {
        FileOutputStream fos = null;
        InputStream is = null;
        try {
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            is = connection.getInputStream();
            fos = new FileOutputStream(file);
            byte[] buf = new byte[512];
            int len = 0;
            while ((len = is.read(buf)) != -1){
                fos.write(buf, 0, len);
            }
            fos.flush();
            connection.disconnect();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (is !=null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    /**
     * 根据url下载图片在指定的文件:网络图片的加载
     * @param urlStr
     * @param imageView
     * @return
     */
    public static Bitmap downloadImgByUrl(String urlStr, ImageView imageView) {
        FileOutputStream fos = null;
        InputStream is = null;
        try {
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            is = new BufferedInputStream(connection.getInputStream());
            is.mark(is.available());
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);

            //获取imageview想要显示的宽和高
            ImageSizeUtil.ImageSize imageSize = ImageSizeUtil.getImageViewSize(imageView);
            options.inSampleSize = ImageSizeUtil.caculateInSampleSize(options,
                            imageSize.width, imageSize.height);
            options.inJustDecodeBounds = false;
            is.reset();
            bitmap = BitmapFactory.decodeStream(is, null, options);
            connection.disconnect();
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (is !=null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     *
     * @param strUrl
     * @param outputStream
     * @return
     */
    public boolean downloadByUrlToStream(String strUrl, OutputStream outputStream) {
        final int IO_BUFFER_SIZE = 512;
        HttpURLConnection connection = null;
        //这边应该用BufferedOutputStream/BufferedInputStream 缓冲流
        BufferedOutputStream bos = null;
        BufferedInputStream bis = null;
        try {
            final URL url = new URL(strUrl);
            connection = (HttpURLConnection) url.openConnection();
            bis = new BufferedInputStream(connection.getInputStream(), IO_BUFFER_SIZE);
            bos = new BufferedOutputStream(outputStream,IO_BUFFER_SIZE);

            int b = 0;
            while ((b = bis.read()) != -1){
                bos.write(b);
            }
            return true;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (connection !=null){
                connection.disconnect();
            }
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bis != null){
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }
}
