package app.shake2cool.schloebe.de.shake2cool;

import app.shake2cool.schloebe.de.shake2cool.util.SystemUiHider;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;


/**
 * @see SystemUiHider
 */
public class FullscreenActivity extends Activity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;

    private static final String TEMP_UNIT_C = "°C";
    private static final String TEMP_UNIT_F = "°F";

    float minX = 0.04f;
    float maxX = 0.2f;
    final int alpha = 200;

    String tempUnit = "";
    Double initTemperature;

    private GPSTracker gps;
    private ShakeDetector mShakeDetector;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private static DecimalFormat numberFormat = new DecimalFormat("#.0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        super.onCreate(savedInstanceState);
        Crashlytics.start(this);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_fullscreen);

        final TextView ContentViewVal = (TextView)findViewById(R.id.temperature_content_val);
        final TextView ContentViewUnit = (TextView)findViewById(R.id.temperature_content_unit);

        Typeface typeFace = Typeface.createFromAsset(getAssets(), "fonts/HelveticaNeue-UltraLight.otf");
        Typeface typeFaceBold = Typeface.createFromAsset(getAssets(), "fonts/helveticaneue-webfont.ttf");

        ContentViewVal.setTextColor(Color.argb(alpha, 255, 255, 255));
        ContentViewUnit.setTextColor(Color.argb(alpha, 255, 255, 255));

        ContentViewVal.setTypeface(typeFace);
        ContentViewUnit.setTypeface(typeFaceBold);

        gps = new GPSTracker(this);

        if( gps.canGetLocation() ) {

            double lat = gps.getLatitude();
            double lng = gps.getLongitude();
            //Log.d("Location Updates", Double.toString(lat));
            //Log.d("Location Updates", Double.toString(lng));

            RequestParams params = new RequestParams();
            params.put("lat", lat);
            params.put("lon", lng);
            params.put("lang", Locale.getDefault().getLanguage());
            if( UnitLocale.getDefault() == UnitLocale.Imperial ) {
                params.put("units", "imperial");
            } else {
                params.put("units", "metric");
            }

            WeatherHttpClient.get("", params, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    try {
                        JSONObject mainObj = getObject("main", response);
                        Float temp = getFloat("temp", mainObj);

                        //Log.d("getWeather", "temp: " + temp);

                        ContentViewVal.setText(numberFormat.format(Double.valueOf(temp.toString())));

                        if( UnitLocale.getDefault() == UnitLocale.Imperial ) {
                            tempUnit = TEMP_UNIT_F;
                        } else {
                            tempUnit = TEMP_UNIT_C;
                        }

                        ContentViewUnit.setText(tempUnit);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });

        } else {
            gps.showSettingsAlert();
        }

        // ShakeDetector initialization
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeDetector = new ShakeDetector(new ShakeDetector.OnShakeListener() {
            @Override
            public void onShake() {
                Random rand = new Random();
                //Toast.makeText(getApplicationContext(), rand.toString(), Toast.LENGTH_LONG).show();
                float finalX = (maxX - minX) * rand.nextFloat() + minX;
                //Toast.makeText(getApplicationContext(), "Shake!", Toast.LENGTH_SHORT).show();
                try {
                    initTemperature = Double.parseDouble(ContentViewVal.getText().toString());
                } catch (final NumberFormatException e) {
                    //Toast.makeText(getApplicationContext(), "Value is not valid double!", Toast.LENGTH_LONG).show();
                    if( UnitLocale.getDefault() == UnitLocale.Imperial ) {
                        initTemperature = 80.0;
                    } else {
                        initTemperature = 30.0;
                    }
                }
                //Log.d("Location Updates", "initTemperature: " + initTemperature);
                //Log.d("Location Updates", "temp: " + initTemperature.toString());
                Double newTemperature = (double)(initTemperature-finalX);
                //temperatureTextField.setText(new StringBuilder(newTemperature.toString()).append(unit).toString());
                ContentViewVal.setText(numberFormat.format(newTemperature));

                //Vibrator v = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
                //v.vibrate(300);
            }
        });

        final View controlsView = findViewById(R.id.fullscreen_content_controls);

        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        mSystemUiHider = SystemUiHider.getInstance(this, ContentViewVal, HIDER_FLAGS);
        mSystemUiHider = SystemUiHider.getInstance(this, ContentViewUnit, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider
                .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
                    // Cached values.
                    int mControlsHeight;
                    int mShortAnimTime;

                    @Override
                    public void onVisibilityChange(boolean visible) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                            if (mControlsHeight == 0) {
                                mControlsHeight = controlsView.getHeight();
                            }
                            if (mShortAnimTime == 0) {
                                mShortAnimTime = getResources().getInteger(
                                        android.R.integer.config_shortAnimTime);
                            }
                            controlsView.animate()
                                    .translationY(visible ? 0 : mControlsHeight)
                                    .setDuration(mShortAnimTime);
                        } else {
                            controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
                        }

                        if (visible && AUTO_HIDE) {
                            delayedHide(AUTO_HIDE_DELAY_MILLIS);
                        }
                    }
                });

        // Set up the user interaction to manually show or hide the system UI.
        ContentViewVal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TOGGLE_ON_CLICK) {
                    mSystemUiHider.toggle();
                } else {
                    mSystemUiHider.show();
                }
            }
        });

        // Set up the user interaction to manually show or hide the system UI.
        ContentViewUnit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TOGGLE_ON_CLICK) {
                    mSystemUiHider.toggle();
                } else {
                    mSystemUiHider.show();
                }
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.notice_bottom).setOnTouchListener(mDelayHideTouchListener);
    }


    @Override
    protected void onStart() {
        super.onStart();
    }


    /*
     * Called when the Activity is no longer visible.
     */
    @Override
    protected void onStop() {
        gps.stopUsingGPS();
        super.onStop();
    }


    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(mShakeDetector, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        mSensorManager.unregisterListener(mShakeDetector);
        super.onPause();
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }


    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }


    public static JSONObject getObject(String tagName, JSONObject jObj)
            throws JSONException {
        return jObj.getJSONObject(tagName);
    }

    public static String getString(String tagName, JSONObject jObj)
            throws JSONException {
        return jObj.getString(tagName);
    }

    public static float getFloat(String tagName, JSONObject jObj)
            throws JSONException {
        return (float) jObj.getDouble(tagName);
    }

    public static int getInt(String tagName, JSONObject jObj)
            throws JSONException {
        return jObj.getInt(tagName);
    }
}