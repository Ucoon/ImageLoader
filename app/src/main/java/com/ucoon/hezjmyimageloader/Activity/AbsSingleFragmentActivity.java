package com.ucoon.hezjmyimageloader.Activity;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import com.ucoon.hezjmyimageloader.R;

/**
 * Created by ZongJie on 2017/1/21.
 */

public abstract class AbsSingleFragmentActivity extends FragmentActivity {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.id_fragmentContainer);
        if (fragment == null) {
            fragment = createFragment();
            fragmentManager.beginTransaction().add(R.id.id_fragmentContainer,fragment)
                    .commit();
        }
    }

    protected abstract Fragment createFragment();
    protected abstract int getLayoutId();
}
