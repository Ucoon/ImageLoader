package com.ucoon.hezjmyimageloader.util;

import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.lang.reflect.Field;

/**
 * Created by ZongJie on 2017/1/17.
 */

public class ImageSizeUtil {

    /**
     * 根据需求的宽和高以及图片实际的宽和高计算SampleSize
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public static int caculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int width = options.outWidth;
        int height = options.outHeight;

        int inSampleSize = 1;
        if (width > reqWidth || height > reqHeight) {
            int halfHeight = Math.round(height / 2);
            int halfWidth = Math.round(width / 2);
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth){
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
    /**
     * 根据ImageView获取适当的压缩的宽和高
     * @param imageView
     * @return
     */
    public static ImageSize getImageViewSize(ImageView imageView) {
        ImageSize imageSize = new ImageSize();
        DisplayMetrics displayMetrics = imageView.getContext().getResources()
                            .getDisplayMetrics();
        ViewGroup.LayoutParams layoutParams = imageView.getLayoutParams();
        int width = imageView.getWidth();//获取imageView的实际宽度:
        /*有些时候，这个getWidth返回的是0；那么我们再去看看它有没有在布局文件中书写宽；
        如果布局文件中也没有精确值，那么我们再去看看它有没有设置最大值；
        如果最大值也没设置，那么我们只有拿出我们的终极方案，使用我们的屏幕宽度；
        总之，不能让它任性，我们一定要拿到一个合适的显示值。*/
        if (width <= 0) {
            width = layoutParams.width;//获取imageView在layout中声明的宽度
        }
        if (width <= 0) {
            width = getImageViewFieldValue(imageView, "mMaxWidth");//检查最大值
        }
        if (width <= 0) {
            width = displayMetrics.widthPixels;//使用屏幕宽度
        }
        int height = imageView.getHeight();
        if (height <= 0) {
            height = layoutParams.height;
        }
        if (height <= 0) {
            height = getImageViewFieldValue(imageView, "mMaxHeight");
        }
        if (height <= 0) {
            height = displayMetrics.heightPixels;
        }
        imageSize.width = width;
        imageSize.height = height;
        return imageSize;
    }

    public static class ImageSize {
        int width;
        int height;
    }

    /**
     * 通过反射获取imageView的某个属性值
     * @param object
     * @param fieldName
     * @return
     */
    private static int getImageViewFieldValue(Object object, String fieldName) {
        int value = 0;
        try {
            Field field = ImageView.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            int fieldValue = field.getInt(object);
            if (fieldValue > 0 && fieldValue < Integer.MAX_VALUE) {
                value = fieldValue;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }
}
