package com.minoon.partialhidelayout.sample;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import com.minoon.partialhidelayout.PartialHideLayout;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PartialHideLayout layout = (PartialHideLayout) findViewById(R.id.list_view);
        layout.setCollapseHeightCalculator(new PartialHideLayout.CollapseHeightCalculator() {
            @Override
            public int getCollapseHeight() {
                return 100;
            }
        });
        layout.hide();
    }
}
