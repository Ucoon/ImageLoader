package com.ucoon.hezjmyimageloader.Activity;

import android.support.v4.app.Fragment;

import com.ucoon.hezjmyimageloader.R;

public class MainActivity extends AbsSingleFragmentActivity {
    @Override
    protected Fragment createFragment() {
        return new ListImgsFragment();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_single_frament;
    }
}
