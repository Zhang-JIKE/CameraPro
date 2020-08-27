package com.jike.camerapro;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.daily.flexui.util.AppContextUtils;
import com.daily.flexui.view.SwitchButton;
import com.jike.camerapro.cameradata.CamRates;

public class LabActivity extends AppCompatActivity {

    private SwitchButton swtForce60Fps;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppContextUtils.setAppActivity(this);
        AppContextUtils.setAppContext(this);
        setContentView(R.layout.activity_lab);

        swtForce60Fps = findViewById(R.id.switch_force_60fps);

        swtForce60Fps.setOnSwitchChangedListner(new SwitchButton.OnSwitchChangedListner() {
            @Override
            public void onSwitchChanged(boolean isChecked) {
                CamRates.setIsForcedOpen60Fps(isChecked);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        swtForce60Fps.setChecked(CamRates.isForcedOpen60Fps);
    }
}
