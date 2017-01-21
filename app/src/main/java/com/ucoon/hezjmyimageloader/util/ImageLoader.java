package com.ucoon.hezjmyimageloader.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;


import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * 图片加载类
 * Created by ZongJie on 2017/1/17.
 */

public class ImageLoader {
    private static ImageLoader mInstance;

    /**
     * 图片缓存的核心对象
     */
    private LruCache<String, Bitmap> mLruCache;

    /**
     * 线程池
     */
    private ExecutorService mThreadPool;
    private static final int DEFAULT_THREAD_COUNT = 1;
    /**
     * 队列的调度方式？？FIFO && LIFO
     */
    private Type mType = Type.LIFO;

    /**
     * 任务队列
     */
    private LinkedList<Runnable> mTaskQueue;

    /**
     * 后台轮询线程
     */
    private Thread mPoolThread;
    private Handler mPoolThreadHandler;

    /**
     * UI线程中的Handler
     */
    private Handler mUIHandler;
    private Semaphore mSemaphorePoolThreadHandler = new Semaphore(0);
    private Semaphore mSemaphoreThreadPool;

    /**
     * 是否存在SD卡
     */
    private boolean isDiskCacheEnable = true;

    private static final String TAG = "ImageLoader";
    public enum Type{
        FIFO, LIFO;
    }
    private ImageLoader(int threadCount, Type type){
        init(threadCount, type);
    }

    /**
     * 初始化
     * @param threadCount
     * @param type
     */
    private void init(int threadCount, Type type) {
        initBackThread();

        //LruCache典型的初始化过程
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheMemory = maxMemory / 8;
        mLruCache = new LruCache<String, Bitmap>(cacheMemory){
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight() / 1024;
            }
        };

        //创建线程池
        mThreadPool = Executors.newFixedThreadPool(threadCount);
        mTaskQueue = new LinkedList<Runnable>();
        mType = type;
        mSemaphoreThreadPool = new Semaphore(threadCount);
    }

    /**
     * 初始化后台轮询线程
     */
    private void initBackThread() {
        //后台轮询线程
        mPoolThread = new Thread(){
            @Override
            public void run() {
                Looper.prepare();
                mPoolThreadHandler = new Handler(){
                    @Override
                    public void handleMessage(Message msg) {
                        //线程池取出一个任务进行执行
                        mThreadPool.execute(getTask());
                        try {
                            mSemaphoreThreadPool.acquire();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                //释放一个信号量
                mSemaphorePoolThreadHandler.release();
                Looper.loop();
            }
        };
        mPoolThread.start();
    }

    public static ImageLoader getInstance() {
        if (mInstance == null) {
            synchronized (ImageLoader.class){
                if (mInstance == null){
                    mInstance = new ImageLoader(DEFAULT_THREAD_COUNT, Type.LIFO);
                }
            }
        }
        return mInstance;
    }

    public static ImageLoader getInstance(int threadCount, Type type) {
        if (mInstance == null) {
            synchronized (ImageLoader.class){
                if (mInstance == null){
                    mInstance = new ImageLoader(threadCount, type);
                }
            }
        }
        return mInstance;
    }

    /**
     * 根据path为imageview设置图片
     * @param path
     * @param imageView
     * @param isFromNet
     */
    public void loadImage(final String path, final ImageView imageView,
                          final boolean isFromNet) {
        imageView.setTag(path);
        if (mUIHandler == null) {
            mUIHandler = new Handler(){
                @Override
                public void handleMessage(Message msg) {
                    //获取得到图片，为imageview回调设置图片
                    ImgBeanHolder holder = (ImgBeanHolder) msg.obj;
                    Bitmap bm = holder.bitmap;
                    ImageView imageView = holder.imageView;
                    String path = holder.path;
                    //将path与getTag存储的路径进行比较:防止图片错乱
                    if (path.equals(imageView.getTag().toString())){
                        imageView.setImageBitmap(bm);
                    }
                }
            };
        }

        //根据path在缓存中获取bitmap
        Bitmap bm = getBitmapFromLruCache(path);
        if (bm != null) {
            refreashBitmap(path,imageView,bm);
        }else {
            addTask(builadTask(path, imageView, isFromNet));
        }
    }

    private void refreashBitmap(final String path, final ImageView imageView,
                                Bitmap bm) {
        Message message = Message.obtain();
        ImgBeanHolder holder = new ImgBeanHolder();
        holder.bitmap = bm;
        holder.path = path;
        holder.imageView = imageView;
        message.obj = holder;
        mUIHandler.sendMessage(message);
    }

    private synchronized void addTask(Runnable runnable) {
        mTaskQueue.add(runnable);
        try {
            if (mPoolThreadHandler == null){
                mSemaphorePoolThreadHandler.acquire();
            }
        }catch (InterruptedException e){

        }
        mPoolThreadHandler.sendEmptyMessage(0x110);
    }

    /**
     * 根据传入的参数，新建一个任务
     * @param path
     * @param imageView
     * @param isFromNet
     * @return
     */
    private Runnable builadTask(final String path, final ImageView imageView,
                                final boolean isFromNet) {
        return new Runnable() {
            @Override
            public void run() {
                Bitmap bm = null;
                if(isFromNet) {
                    File file = getDiskCacheDir(imageView.getContext(),
                                                md5(path));
                    if (file.exists()){//如果在缓存文件中发现
                        Log.e(TAG, "find image：" + path + "in disk cache. ");
                        bm = loadImageFromLocal(file.getAbsolutePath(),
                                imageView);
                    }else {
                        if (isDiskCacheEnable){//检测是否存在SD卡
                            boolean downloadState = DownloadImgUtils
                                    .downloadImgByUrl(path,file);
                            if (downloadState){//如果下载成功
                                Log.e(TAG, "download image :" +path
                                        + " to disk cache . path is "
                                        + file.getAbsolutePath());
                                bm = loadImageFromLocal(file.getAbsolutePath(),imageView);
                            }
                        }else {//直接从网络下载
                            Log.e(TAG, "download image :" +path + " to memory ");
                            bm = DownloadImgUtils.downloadImgByUrl(path,imageView);
                        }
                    }
                }else {
                    bm = loadImageFromLocal(path, imageView);
                }
                //3、把图片加入到缓存
                addBitmapToLruCache(path, bm);
                refreashBitmap(path, imageView, bm);
                mSemaphoreThreadPool.release();
            }
        };
    }

    private Bitmap loadImageFromLocal(final String path,
                                      final ImageView imageView) {
        Bitmap bm;
        //加载图片
        //图片的压缩
        //1、获得图片需要显示的大小
        ImageSizeUtil.ImageSize imageSize = ImageSizeUtil.getImageViewSize(imageView);
        //2、压缩图片
        bm = decodeSampledBitmapFromPath(path, imageSize.width, imageSize.height);
        return bm;
    }
    /**
     * 从任务队取出一个方法
     * @return
     */
    private Runnable getTask(){
        if (mType == Type.FIFO) {
            return mTaskQueue.removeFirst();
        } else if (mType == Type.LIFO) {
            return mTaskQueue.removeLast();
        }
        return null;
    }

    /**
     * 将图片加入LruCache
     * @param path
     * @param bitmap
     */
    protected void addBitmapToLruCache(String path, Bitmap bitmap){
        if (getBitmapFromLruCache(path) == null){
            if (bitmap != null){
                mLruCache.put(path,bitmap);
            }
        }
    }
    /**
     * 根据图片需要显示的宽和高对图片进行压缩
     * @param path
     * @param width
     * @param height
     * @return
     */
    protected Bitmap decodeSampledBitmapFromPath(String path, int width,
                                                 int height) {
        //获得图片的实际的宽和高，并不把图片加载到内存中
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        options.inSampleSize = ImageSizeUtil.caculateInSampleSize(options, width, height);

        //使用获得到的InSampleSize再次解析图片
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        return bitmap;
    }
    /**
     * 获取缓存图片的地址
     * @param context
     * @param uniqueName
     * @return
     */
    public File getDiskCacheDir(Context context, String uniqueName) {
        String cachePath;
        if (Environment.MEDIA_MOUNTED.equals(Environment
                        .getExternalStorageState())){
            cachePath = context.getExternalCacheDir().getPath();
        }else {
            cachePath = context.getCacheDir().getPath();
        }
        return new File(cachePath + File.separator + uniqueName);
    }
    /**
     * 根据path在缓存中获取bitmap
     * @param key
     * @return
     */
    private Bitmap getBitmapFromLruCache(String key) {
        return mLruCache.get(key);
    }

    /**
     * 利用签名辅助类，将字符串转换成字节数组:之所以要把url转换为key，
     * 是因为图片的url很可能含有特殊字符，这将影响url在Android中直接使用，一般采用url的md5值作为key
     * @param str
     * @return
     */
    public String md5(String str){
        byte[] digest = null;
        try {
            MessageDigest md = MessageDigest.getInstance("md5");
            digest = md.digest(str.getBytes());
            return bytes2hex02(digest);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 方式二
     * @param bytes
     * @return
     */
    public String bytes2hex02(byte[] bytes){
        StringBuilder stringBuilder = new StringBuilder();
        String tmp = null;
        for (byte b : bytes) {
            //将每个字节与0xFF进行运算，然后转化为十进制，然后借助于Integer再转化为16进制
            tmp = Integer.toHexString(0xFF & b);
            if (tmp.length() == 1) {//每个字节为8位，转化为16进制标志，2个16进制位
                tmp = "0" + tmp;
            }
            stringBuilder.append(tmp);
        }
        return stringBuilder.toString();
    }
    private class ImgBeanHolder{
        Bitmap bitmap;
        ImageView imageView;
        String path;
    }
}
