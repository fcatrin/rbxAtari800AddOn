// This string is autogenerated by ChangeAppSettings.sh, do not change spaces amount
package xtvapps.retrobox.atari800;

import java.io.File;
import java.util.ArrayList;

import retrobox.utils.ImmersiveModeSetter;
import retrobox.vinput.GenericGamepad.Analog;
import retrobox.vinput.KeyTranslator;
import retrobox.vinput.Mapper;
import retrobox.vinput.Mapper.ShortCut;
import retrobox.vinput.QuitHandler;
import retrobox.vinput.QuitHandler.QuitHandlerCallback;
import retrobox.vinput.VirtualEvent.MouseButton;
import retrobox.vinput.VirtualEventDispatcher;
import retrobox.vinput.overlay.ExtraButtons;
import retrobox.vinput.overlay.ExtraButtonsController;
import retrobox.vinput.overlay.ExtraButtonsView;
import retrobox.vinput.overlay.GamepadController;
import retrobox.vinput.overlay.GamepadView;
import retrobox.vinput.overlay.Overlay;
import retrobox.vinput.overlay.OverlayExtra;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsoluteLayout;
import android.widget.Toast;

import com.tvi910.android.core.AccelerometerJoystick;
import com.tvi910.android.core.Keymap;
import com.tvi910.android.core.TouchpadJoystick;
import com.tvi910.android.core.VirtualControllerManager;
import com.tvi910.android.core.buttonpanel.ButtonCallback;
import com.tvi910.android.core.buttonpanel.ButtonPanel;
import com.tvi910.android.sdl.AudioThread;
import com.tvi910.android.sdl.LoadLibrary;
import com.tvi910.android.sdl.SDLInterface;
import com.tvi910.android.sdl.SDLKeysym;
import com.tvi910.android.sdl.SDLSurfaceView;

public class MainActivity extends Activity {

    private static MainActivity _instance = null;
    public static Context ctx;
    public static final String TAG = "com.droid800.emulator";
    public static final Overlay overlay = new Overlay();
    
    private static class ButtonInfo {
        String name;
        int colspan;
        ButtonCallback callback;

        ButtonInfo(String name, ButtonCallback callback, int colspan) {
            this.name = name;
            this.colspan = colspan;
            this.callback = callback;
        }
    }

    public static class QuitEmulatorCallback implements ButtonCallback {
        QuitEmulatorCallback() {
        }
        public void onButtonUp() {
            MainActivity.getInstance().uiQuitConfirm();
        }   
    }

    public static class NormalCallback implements ButtonCallback {
        public int _keyCode;
        public boolean _closePanel;
        NormalCallback(int keyCode, boolean closePanel) {
            _keyCode = keyCode;
            _closePanel = closePanel;
        }
        public void onButtonUp() {
            SDLInterface.nativeKeyCycle(_keyCode);
            if (_closePanel) {
                MainActivity.getInstance().hideControlPanel();
            }
        }   
    }

    public static class DelayCallback implements ButtonCallback {
        public int _keyCode;
        public boolean _closePanel;
        DelayCallback(int keyCode, boolean closePanel) {
            _keyCode = keyCode;
            _closePanel = closePanel;
        }
        public void onButtonUp() {
            SDLInterface.nativeKeyCycle(_keyCode, 200);
            if (_closePanel) {
                MainActivity.getInstance().hideControlPanel();
            }
        }   
    }

    /**
     * list of ButtonPanel button attributes and toggles (if there is one) 
     */
    private static final ButtonInfo[] panelButtons = {
        new ButtonInfo("Reset", new DelayCallback(SDLKeysym.SDLK_F5,false), 1),
        null,
        new ButtonInfo("Option", new DelayCallback(SDLKeysym.SDLK_F2,false), 1),
        null,
        new ButtonInfo("Select", new DelayCallback(SDLKeysym.SDLK_F3,false), 1),
        null,
        new ButtonInfo("Start", new DelayCallback(SDLKeysym.SDLK_F4,true), 1),
        null,
        new ButtonInfo("Quit", new QuitEmulatorCallback(), 1),
        null,
        new ButtonInfo("F1", new NormalCallback(SDLKeysym.SDLK_F1,true), 1),
        null,
        new ButtonInfo("Escape", new NormalCallback(SDLKeysym.SDLK_ESCAPE,true), 1),
        null,
        new ButtonInfo("Break", new NormalCallback(SDLKeysym.SDLK_F7,true), 1),
        null,
        null,
        null
    };

    public static MainActivity getInstance() {
        return _instance;
    }

	private static Mapper mapper;
	private VirtualInputDispatcher vinputDispatcher;
    
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Log.d("FOCUS", "onFocusChanged " + hasFocus);
        
        if (hasFocus) ImmersiveModeSetter.postImmersiveMode(new Handler(), getWindow(), useStableLayout());
	}

	private void setImmersiveMode() {
		ImmersiveModeSetter.get().setImmersiveMode(getWindow(), useStableLayout());
	}
	
	private boolean useStableLayout() {
		return false;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
				
		ctx = this;
        _instance = this;

        _lowerMode = false;

        AtariKeys.init();
		super.onCreate(savedInstanceState);
        
        Intent intent = getIntent();
        String romFile = intent.getStringExtra("game");
        String stateDir = intent.getStringExtra("stateDir");
        String stateName = intent.getStringExtra("stateName");
        String machine = intent.getStringExtra("machine");
        String osromDir = intent.getStringExtra("osromDir");
        String videoSystem = intent.getStringExtra("videoSystem");
        boolean keepAspect = intent.getBooleanExtra("keepAspect", true);
        boolean stereo = intent.getBooleanExtra("stereo", false);
        
        for(int i=0; i<2; i++) {
        	String prefix = "j" + (i+1);
        	String deviceDescriptor = intent.getStringExtra(prefix + "DESCRIPTOR");
        	Mapper.registerGamepad(i, deviceDescriptor);
        }
        
        boolean useGamepad = Mapper.hasGamepads();
        
        if (stateDir!=null) new File(stateDir).mkdirs();
		
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
	    SharedPreferences.Editor editor = preferences.edit();
	    editor.putString("osromDir", osromDir);
	    editor.putString("romfile", romFile);
	    editor.putString("stateDir", stateDir);
	    editor.putString("stateName", stateName);
	    editor.putString("machine", machine);
	    editor.putString("videoSystem", videoSystem);
	    editor.putBoolean("keepAspect", keepAspect);
	    editor.putBoolean("stereo", stereo);
	    editor.commit();
	    
	    Log.d(TAG,  "Set machine:" + machine);
	    
		// fullscreen mode
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setImmersiveMode();
		
        // lock orientation 
        boolean landscapeMode =
            PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).getBoolean("landscape", true);
        if (landscapeMode) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
        else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        _keymap = Keymap.getInstance();

        if (_keymap.getNumberOfMappedKeys() == 0) {
        	Log.d(TAG, "Using AtariKeys");
            _keymap.reload(PreferenceManager.getDefaultSharedPreferences(this), AtariKeys.getInstance());
        } else {
        	Log.d(TAG, "NOT Using AtariKeys");
        }
        
        KeyTranslator.init();
        KeyTranslator.addTranslation("ATR_KEY_LEFT", SDLKeysym.SDLK_LEFT);
        KeyTranslator.addTranslation("ATR_KEY_RIGHT", SDLKeysym.SDLK_RIGHT);
        KeyTranslator.addTranslation("ATR_KEY_UP", SDLKeysym.SDLK_UP);
        KeyTranslator.addTranslation("ATR_KEY_DOWN", SDLKeysym.SDLK_DOWN);

        KeyTranslator.addTranslation("ATR_LEFT", SDLKeysym.SDLK_KP4);
        KeyTranslator.addTranslation("ATR_RIGHT", SDLKeysym.SDLK_KP6);
        KeyTranslator.addTranslation("ATR_UP", SDLKeysym.SDLK_KP8);
        KeyTranslator.addTranslation("ATR_DOWN", SDLKeysym.SDLK_KP5);
        KeyTranslator.addTranslation("ATR_RESET", SDLKeysym.SDLK_F5);
        KeyTranslator.addTranslation("ATR_OPTION", SDLKeysym.SDLK_F2);
        KeyTranslator.addTranslation("ATR_SELECT", SDLKeysym.SDLK_F3);
        KeyTranslator.addTranslation("ATR_START", SDLKeysym.SDLK_F4);
        KeyTranslator.addTranslation("ATR_TRIGGER", SDLKeysym.SDLK_KP_PERIOD);
        KeyTranslator.addTranslation("ATR_SPACE", SDLKeysym.SDLK_SPACE);
        KeyTranslator.addTranslation("ATR_ESCAPE", SDLKeysym.SDLK_ESCAPE);
        // second player
        KeyTranslator.addTranslation("ATR_LEFT2", SDLKeysym.SDLK_KP1);
        KeyTranslator.addTranslation("ATR_RIGHT2", SDLKeysym.SDLK_KP3);
        KeyTranslator.addTranslation("ATR_UP2", SDLKeysym.SDLK_KP7);
        KeyTranslator.addTranslation("ATR_DOWN2", SDLKeysym.SDLK_KP9);
        KeyTranslator.addTranslation("ATR_TRIGGER2", SDLKeysym.SDLK_LCTRL);
        
        // haremos esto mejor en otra vida
        for(int i=SDLKeysym.SDLK_a; i<=SDLKeysym.SDLK_z; i++) {
        	String atariKey = "ATR_" + new String(new byte[] {(byte)(i)}).toUpperCase();
        	KeyTranslator.addTranslation(atariKey, i);
        }
        for(int i=SDLKeysym.SDLK_0; i<=SDLKeysym.SDLK_9; i++) {
        	String atariKey = "ATR_" + new String(new byte[] {(byte)(i)}).toUpperCase();
        	KeyTranslator.addTranslation(atariKey, i);
        }
                
        vinputDispatcher = new VirtualInputDispatcher();
        mapper = new Mapper(getIntent(), vinputDispatcher);
        Mapper.initGestureDetector(this);
        
        SDLInterface.setLeftKeycode(SDLKeysym.SDLK_KP4) ;
        SDLInterface.setRightKeycode(SDLKeysym.SDLK_KP6) ;
        SDLInterface.setUpKeycode(SDLKeysym.SDLK_KP8);
        SDLInterface.setDownKeycode(SDLKeysym.SDLK_KP2) ;
        SDLInterface.setTriggerKeycode(SDLKeysym.SDLK_KP_PERIOD) ;

        extraButtonsView = new ExtraButtonsView(this);
        gamepadView = new GamepadView(this, overlay);
        
        if (landscapeMode) {
            _buttonPanel = new ButtonPanel(
                this, // context
                null, /*Typeface.createFromAsset(getAssets(), "fonts/ATARCC__.TTF"),*/ // custom font
                4, // number of grid columns
                2, // number of grid rows
                90, // percent of desired width fill
                50, // percent of desired height fill
                50, // x offset (0 = center)
                10, // y offset (0 = center)
                3f, // aspect ratio (1=square)
                2); 
            _buttonPanel.setPadding(.50f);

            for (int col=0; col<4; col++) {
                for (int row=0; row<2; row++) {
                    final int fcol = col;
                    final int frow = row;
                    ButtonInfo bi = panelButtons[((4*row)+col)*2];
                    ButtonInfo tog = panelButtons[(((4*row)+col)*2)+1];
                    if (null != bi) {
                        _buttonPanel.setButton(fcol,frow,
                            Color.argb(192, 38, 38, 38), 
                            Color.argb(192, 228, 228, 228), 
                            bi.name, 
                            bi.callback,
                            bi.colspan);
                    }
                    if (null != tog) {
                        _buttonPanel.setToggle(fcol,frow,
                            Color.argb(192, 38, 38, 38), 
                            Color.argb(192, 228, 228, 228), 
                            tog.name, 
                            tog.callback);
                    }
                }
            }
        }
        else {
            _buttonPanel = new ButtonPanel(
                this, // context
                null, /*Typeface.createFromAsset(getAssets(), "fonts/ATARCC__.TTF"),*/ // custom font
                3, // number of grid columns
                3, // number of grid rows
                95, // percent of desired width fill
                35, // percent of desired height fill
                50, // x offset (0 = center)
                60, // y offset (0 = center)
                2.5f, // aspect ratio (1=square)
                2); 

            _buttonPanel.setPadding(.85f);

            for (int col=0; col<3; col++) {
                for (int row=0; row<3; row++) {
                    final int fcol = col;
                    final int frow = row;
                    ButtonInfo bi = panelButtons[((3*row)+col)*2];
                    ButtonInfo tog = panelButtons[(((3*row)+col)*2)+1];
                    if (null != bi) {
                        _buttonPanel.setButton(fcol,frow,
                            Color.argb(192, 38, 38, 38), 
                            Color.argb(192, 228, 228, 228), 
                            bi.name, 
                            bi.callback,
                            bi.colspan);
                    }
                    if (null != tog) {
                        _buttonPanel.setToggle(fcol,frow,
                            Color.argb(192, 38, 38, 38), 
                            Color.argb(192, 228, 228, 228), 
                            tog.name, 
                            tog.callback);
                    }
                }
            }
        }
        _buttonPanel.setPanelButtonCallback(buttonPanelCallback);

		mLoadLibraryStub = new LoadLibrary();
		mAudioThread = new AudioThread(this);

        initSDL(landscapeMode, useGamepad);
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        
        ViewTreeObserver observer = mGLView.getViewTreeObserver();
		observer.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

	        public void onGlobalLayout() {
	        	int w = mGLView.getWidth();
	        	int h = mGLView.getHeight();
	        	String overlayConfig = getIntent().getStringExtra("OVERLAY");
				float alpha = getIntent().getFloatExtra("OVERLAY_ALPHA", 0.8f);
	        	if (overlayConfig!=null) overlay.init(overlayConfig, w, h, alpha);

	            ExtraButtons.initExtraButtons(MainActivity.this, getIntent().getStringExtra("buttons"), mGLView.getWidth(), mGLView.getHeight(), true);
	        }
	    });
        
	}

	public void initSDL(boolean landscapeMode, boolean useGamepad)
	{
		if(sdlInited)
			return;
		sdlInited = true;

        Display display = getWindowManager().getDefaultDisplay();

        AbsoluteLayout al = new AbsoluteLayout(this);
        setContentView(al);


        // set up the sdl command line args.
        String systemType = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).getString("machine", "800XL");
        String gameRom = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).getString("romfile","");
        Log.d("com.droid800.emulator", "Archivo a cargar " + gameRom);
        
        if (!systemType.equals("5200")) {
            gameRom = Cartridge.getInstance().prepareCartridge(gameRom);
        }
        String osromDir =  PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).getString("osromDir", "");
        String stateDir =  PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).getString("stateDir",null);
        String stateName =  PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).getString("stateName",null);
        String refreshRate = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).getString("skipFrame", "0");
        boolean showSpeed = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).getBoolean("showspeed", false);
        boolean enableSound = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).getBoolean("sound", true);
        boolean showBorder = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).getBoolean("showBorder", true);
        String sampleRate = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).getString("sampleRate", "44100");
        boolean keepAspect = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).getBoolean("keepAspect", true);
        boolean stereo = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).getBoolean("stereo", false);
        boolean ntscMode = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).getString("videoSystem", "NTSC").equals("NTSC");
        String leftController = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).getString("leftControllerId", "Gamepad");
        setupVirtualControllers(landscapeMode);

        ArrayList<String> arglist = new ArrayList<String>();
        arglist.add("atari800");
        if (showSpeed) {
            arglist.add("-showspeed");
        }
        
        int refreshRateInt = Integer.parseInt(refreshRate);
        if (refreshRateInt > 0) {
            arglist.add("-refresh");
            arglist.add("" + refreshRateInt);
        }

        if (keepAspect) arglist.add("-keepaspectratio");
        if (showBorder) arglist.add("-showborder");

        if (!enableSound) {
            arglist.add("-nosound");
        }
        
        if (stereo) {
        	arglist.add("-stereo");
        }
        
        arglist.add("-dsprate");
        arglist.add(sampleRate);
        arglist.add("-audio16");
      
        if (ntscMode) {
            arglist.add("-ntsc");
        }


        if (systemType.equals("5200")) {
        	String osROM = osromDir + "/5200.ROM";
            arglist.add("-5200");
            arglist.add("-5200_rom");
            arglist.add(osROM);
        }
        else if (systemType.equals("800a")) {
        	String osROM = osromDir + "/ATARIOSA.ROM";
            arglist.add("-atari");
            arglist.add("-osa_rom");
            arglist.add(osROM);
        }
        else if (systemType.equals("800b")) {
        	String osROM = osromDir + "/ATARIOSB.ROM";
            arglist.add("-atari");
            arglist.add("-osb_rom");
            arglist.add(osROM);
        }
        else {
            if (systemType.equals("800XL")) {
                arglist.add("-xl");
            }
            else if (systemType.equals("130XE")) {
                arglist.add("-xe");
            }
            else if (systemType.equals("320XE")) {
                arglist.add("-320xe");
            }
            else if (systemType.equals("RAMBO")) {
                arglist.add("-RAMBO");
            }
            arglist.add("-xlxe_rom");
            
            String osROM = osromDir + "/ATARIXL.ROM";
            arglist.add(osROM);
        }

        arglist.add("-basic_rom"); // this is required or the emaulator will
                                   // try and load teh basic rom too
        arglist.add("none");
        
        if (stateDir!=null && stateName!=null) {
        	arglist.add("-state_dir");
        	arglist.add(stateDir);
        	arglist.add("-state_name");
        	arglist.add(stateName);
        }

        if (!gameRom.equals("")) {
            arglist.add(gameRom);
        }

        Log.d(TAG, "args" + arglist);
        
        mGLView = new SDLSurfaceView(this, arglist);
        al.addView(mGLView);
        
        if (!useGamepad)  {
            gamepadView.addToLayout((ViewGroup)al);
        }

        // _buttonPanel.addToLayout((ViewGroup)al);
        //_keyboardOverlay.getButtonPanel().addToLayout((ViewGroup)al);
        extraButtonsView.addToLayout((ViewGroup)al);
        
//        _buttonPanel.showPanel();

		// Receive keyboard events
		mGLView.setFocusableInTouchMode(true);
		mGLView.setFocusable(true);
		mGLView.requestFocus();
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "Droid800 - do not dim screen");

		if (!useGamepad)  {
			_virtualControllerManager.setActiveController("Gamepad");
		}
	}

    private void setupVirtualControllers(boolean landscapeMode) {
        _virtualControllerManager = new VirtualControllerManager();

        _accelerometerJoystick = AccelerometerJoystick.getInstance(this);
//        _touchPaddle = new TouchPaddle(this,getWindowManager().getDefaultDisplay().getHeight());
        setupTouchpadJoystick();
        //_buttonPanelController = new ButtonPanelController(this, _buttonPanel);
        //_nullController = new NullController(this);
        extraButtonsController = new ExtraButtonsControllerWrapper(this, new ExtraButtonsController(), extraButtonsView);
        gamepadController = new GamepadControllerWrapper(this, new GamepadController(), gamepadView);

        int keyboardAlpha = PreferenceManager.getDefaultSharedPreferences(
            this.getApplicationContext()).getInt("keyboardAlpha", 192);
        //_keyboardOverlay = new KeyboardOverlay(this, landscapeMode, keyboardAlpha);
        //_keyboardOverlay.getButtonPanel().setPanelButtonCallback(keyboardSliderCallback);


        //_virtualControllerManager.add("Tilt Joystick", _accelerometerJoystick);
        //_virtualControllerManager.add("Control Panel", _buttonPanelController);
        //_virtualControllerManager.add("Keymap", _nullController);
        //_virtualControllerManager.add("Virtual Keyboard", _keyboardOverlay);
        //_virtualControllerManager.add("Virtual Joystick", _touchpadJoystick);
        _virtualControllerManager.add("Gamepad", gamepadController);
        _virtualControllerManager.add("Extra Buttons", extraButtonsController);
    }

    private void setupTouchpadJoystick() {

        Display display = getWindowManager().getDefaultDisplay();

        int layout = Integer.parseInt(
            PreferenceManager.getDefaultSharedPreferences(
                this.getApplicationContext()).getString("controlslayout","0"));
        boolean layoutOnTop = false;
        boolean buttonsOnLeft = true;

        switch (layout) {
            case 0:
                layoutOnTop = false;
                buttonsOnLeft = false;
                break;
            case 1:
                layoutOnTop = false;
                buttonsOnLeft = true;
                break;
            case 2:
                layoutOnTop = true;
                buttonsOnLeft = false;
                break;
            case 3:
                layoutOnTop = true;
                buttonsOnLeft = true;
                break;
            default :
                break;
        }

        int controlsSize = Integer.parseInt(
            PreferenceManager.getDefaultSharedPreferences(
                this.getApplicationContext()).getString("touchpadJoystickSize","1"));

        int sz = TouchpadJoystick.MEDIUM;
        switch (controlsSize) {
            case 0 :
                sz = TouchpadJoystick.SMALLEST;
                break;
            case 1 :
                sz = TouchpadJoystick.SMALL;
                break;
            case 2 :
                sz = TouchpadJoystick.MEDIUM;
                break;
            case 3 :
                sz = TouchpadJoystick.LARGE;
                break;
            default :
                sz = TouchpadJoystick.MEDIUM;
                break;
        }


        _touchpadJoystick  = new TouchpadJoystick(
            this, layoutOnTop, buttonsOnLeft,
            display.getWidth(), display.getHeight(), sz);
    }

	@Override
	protected void onPause() {
        Log.v("com.droid800.MainActivity", "Paused");
        /*
		if( wakeLock != null ) {
			wakeLock.release();
        }*/
		super.onPause();
		if( mGLView != null ) {
			mGLView.onPause();
        }
	}

	@Override
	protected void onResume() {
        Log.v("com.droid800.MainActivity", "Resumed");
        /*
		if( wakeLock != null ) {
			wakeLock.acquire();
        }*/
		
        super.onResume();
		
        ImmersiveModeSetter.postImmersiveMode(new Handler(), getWindow(), useStableLayout());
        
		if( mGLView != null ) {
			mGLView.onResume();
        }
	}

	@Override
	protected void onStop()
	{
        Log.v("com.droid800.MainActivity", "Stopped");

		if( mGLView != null )
			mGLView.exitApp();
		super.onStop();
		finish();
	}
	
	private void sendLoadState(boolean down) {
		SDLInterface.nativeKey(SDLKeysym.SDLK_LALT, down?1:0);
		SDLInterface.nativeKey(SDLKeysym.SDLK_l, down?1:0);
	}

	private void sendSaveState(boolean down) {
		SDLInterface.nativeKey(SDLKeysym.SDLK_LALT, down?1:0);
		SDLInterface.nativeKey(SDLKeysym.SDLK_s, down?1:0);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, final KeyEvent event) {
		Log.v("com.droid800.MainActivity", "DOWN keyCode: " + keyCode + ", getUnicodeCHar=" + event.getUnicodeChar());

		if (mapper.handleKeyEvent(event, keyCode, true)) return true;

         final int nativeCode = _keymap.translate(keyCode);
         
         if (nativeCode == SDLKeysym.SDLK_F14) {
        	 sendLoadState(true);
        	 return true;
         }
         if (nativeCode == SDLKeysym.SDLK_F15) {
        	 sendSaveState(true);
        	 return true;
         }
         
         if (nativeCode > 0) {
        	 Log.v("com.droid800.MainActivity", "Send native code " + nativeCode);
             SDLInterface.nativeKey(nativeCode, 1);
             return true;
         }
         else if (keyCode == 4 /*|| keyCode == 84*/) {
            return true;
         }
         else if (keyCode == 67) {
             SDLInterface.nativeKey(8, 1);
             return true;
         }
         else {
        	 Log.v("com.droid800.MainActivity", "autotranslate");
             int charCode = event.getUnicodeChar();
             if (charCode > 0) {
                 if (!_lowerMode) {
                     // clumsy work-around for the weird capslock situation in 
                     // the emulator. This is essentially pressing the "Lowr" 
                     // button on older ataris or toggling caps mode in the newer 
                     // ones.
                     _lowerMode = true;
                     SDLInterface.nativeKeyCycle(SDLKeysym.SDLK_CAPSLOCK);
                     SDLInterface.nativeKeyCycle(SDLKeysym.SDLK_CAPSLOCK);
                 }

                 // flip lower and upper
                 if (charCode >= 65) {
                     if (charCode <= 90) {
                         charCode = charCode + 32;
                     }
                     else if (charCode >= 97 && charCode <= 122) {
                         charCode = charCode - 32;
                     }
                 }

                 switch (charCode) {
                     case (10) : 
                         SDLInterface.nativeKey(13, 1); // sdk return key
                         _lastCharDown = 13;
                         return true;
                     default : 
                         SDLInterface.nativeKey(charCode, 1);
                         _lastCharDown = charCode;
                         return true;
                 }
             }
             else {
                 return false;
             }
         }
	 }

	@Override
	public boolean onKeyUp(int keyCode, final KeyEvent event) {
		Log.v("com.droid800.MainActivity", "UP keyCode: " + keyCode + ", getUnicodeCHar=" + event.getUnicodeChar());

		if (mapper.handleKeyEvent(event, keyCode, false)) return true;

         final int nativeCode = _keymap.translate(keyCode);
         
         if (nativeCode == SDLKeysym.SDLK_F14) {
        	 sendLoadState(false);
        	 return true;
         }
         if (nativeCode == SDLKeysym.SDLK_F15) {
        	 sendSaveState(false);
        	 return true;
         }

         if (nativeCode > 0) {
             SDLInterface.nativeKey(nativeCode, 0);
             return true;
         }
         else if (keyCode == 4) 
           {
        	 /*
            if (_keyboardOverlay.getButtonPanel().isVisible()) {
                hideKeyboard();
                return true;
            }
            else {
                if (_buttonPanel.isVisible()) {
                    hideControlPanel();
                    return true;
                }
            }*/

            openOptionsMenu();
            return true;
         }
         else if (keyCode == 67) {
             SDLInterface.nativeKey(8, 0);
             return true;
         }
         else {
             final int charCode = event.getUnicodeChar();
             if (charCode > 0) {
                 SDLInterface.nativeKey(_lastCharDown, 0);
                 return true;
             }
             else {
                 return false;
             }
         }
//         else if (keyCode == 84) {
//            if (_keyboardOverlay.getButtonPanel().isVisible()) {
//                hideKeyboard();
//                return true;
//            }
//            else {
//                _virtualControllerManager.setActiveController("Virtual Keyboard");
//            }
//            return true;
//         }
	 }

    private void sleep() {
        try {
            Thread.sleep(1);
        }
        catch (Throwable e) {
        }
    }

	@Override
	public boolean dispatchTouchEvent(final MotionEvent ev) {
        if (!super.dispatchTouchEvent(ev)) {
        	if (gamepadController.getIsActive() && gamepadController.onTouchEvent(ev)) {
        		if (Overlay.requiresRedraw) {
            		Overlay.requiresRedraw = false;
        			gamepadView.invalidate();
        		}
        		return true;
        	}
        	if (extraButtonsController.getIsActive() && extraButtonsController.onTouchEvent(ev)) {
        		if (OverlayExtra.requiresRedraw) {
        			OverlayExtra.requiresRedraw = false;
        			extraButtonsController.invalidate();
        		}
        		return true;
        	}
        	
        	mapper.onTouchEvent(ev);
        	
		    if(_touchpadJoystick.getIsActive()) {
			    boolean ret = _touchpadJoystick.onTouchEvent(ev);
                // if we don't sleep here we get way to many motion events.
                sleep();
                return ret;
            }
            else if (_accelerometerJoystick.getIsActive()) {
                final int action = ev.getAction();
                switch (action & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN: {
                        SDLInterface.triggerOn();
                        sleep();
                        return true;
                    }
                    case MotionEvent.ACTION_UP: {   
                        SDLInterface.triggerOff(); 
                        sleep();
                        return true;
                    }
                    default : 
                        return false;
                }       
            }
            else {
                return false;
            }
        }
        else {
            return true;
        }
	}

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    /**
     * A call back for when the user presses the start button
     */
    /*
    ButtonCallback keyboardSliderCallback = new ButtonCallback() {
        public void onButtonUp() {
            if (_keyboardOverlay.getButtonPanel().isVisible()) {
                hideKeyboard();
            }
            else {
                _virtualControllerManager.setActiveController("Virtual Keyboard");
            }
        }
    };
    */

    /**
     * A call back for when the user presses the start button
     */
    ButtonCallback buttonPanelCallback = new ButtonCallback() {
        public void onButtonUp() {
            if (_buttonPanel.isVisible()) {
                hideControlPanel();
            }
            else {
                _virtualControllerManager.setActiveController("Control Panel");
            }
        }
    };

    private void hideControlPanel() {
        _virtualControllerManager.activateLastController();
    }

    private void hideKeyboard() {
        _virtualControllerManager.activateLastController();
    }
    
    private void hideExtraButtons() {
    	extraButtonsController.deactivate();
    }
    
    private void showExtrabuttons() {
    	extraButtonsController.activate();
    }

   
    public void shutdown() {
        MainActivity.this.finish();  
        SDLInterface.nativeQuit();  
    }

    // virtual controllers and manager
    private TouchpadJoystick _touchpadJoystick = null;
//    private TouchPaddle _touchPaddle = null;
    private AccelerometerJoystick _accelerometerJoystick = null;
//    private AtariKeypad _atariKeypad = null;
    //private ButtonPanelController _buttonPanelController = null;
    //private NullController _nullController = null;
    //private KeyboardOverlay _keyboardOverlay;
    private VirtualControllerManager _virtualControllerManager = null;
    private ExtraButtonsControllerWrapper extraButtonsController = null;
    private GamepadControllerWrapper gamepadController = null;


	private SDLSurfaceView mGLView = null;
	private LoadLibrary mLoadLibraryStub = null;
	private AudioThread mAudioThread = null;
	private PowerManager.WakeLock wakeLock = null;
	private boolean sdlInited = false;
    private ButtonPanel _buttonPanel;
    private ExtraButtonsView extraButtonsView;
    private GamepadView gamepadView;
    private Keymap _keymap = null;
    private int _lastCharDown = 0;
    private boolean _lowerMode = false;
	private boolean buttonsVisible = false;
    
    
    protected void toastMessage(String message) {
    	Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
	@Override
	public void onBackPressed() {
		openOptionsMenu();
	}
	
	static final private int CANCEL_ID = Menu.FIRST;
    static final private int LOAD_ID = Menu.FIRST +1;
    static final private int SAVE_ID = Menu.FIRST +2;
    static final private int QUIT_ID = Menu.FIRST +3;
    static final private int BUTTONS_ID = Menu.FIRST +4;
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(0, CANCEL_ID, 0, "Cancel");
        menu.add(0, LOAD_ID, 0, R.string.load_state);
        menu.add(0, SAVE_ID, 0, R.string.save_state);
        menu.add(0, BUTTONS_ID, 0, "Show Buttons");
        menu.add(0, QUIT_ID, 0, R.string.quit);
        
        return true;
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
    	if (item != null) {
	        switch (item.getItemId()) {
	        case LOAD_ID : uiLoadState(); return true;
	        case SAVE_ID : uiSaveState(); return true;
	        case QUIT_ID : uiQuit(); return true;
	        case CANCEL_ID : return true;
	        case BUTTONS_ID : uiToggleButtons(); return true;
	        }
    	}
        return super.onMenuItemSelected(featureId, item);
    }
    
    @Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		onPause();
		return super.onMenuOpened(featureId, menu);
	}

	@Override
	public void onOptionsMenuClosed(Menu menu) {
		onResume();
		super.onOptionsMenuClosed(menu);
	}
	
	protected void uiToggleButtons() {
		buttonsVisible = !buttonsVisible ;
		if (buttonsVisible) showExtrabuttons();
		else hideExtraButtons();
	}
    
    protected void uiLoadState() {
    	sendLoadState(true);
    	new Handler().postDelayed(new Runnable(){
			@Override
			public void run() {
				sendLoadState(false);
				toastMessage("State was restored");
			}
		}, 50);
    }

    protected void uiSaveState() {
    	sendSaveState(true);
    	new Handler().postDelayed(new Runnable(){
			@Override
			public void run() {
				sendSaveState(false);
				toastMessage("State was saved");
			}
		}, 50);
    }

    protected void uiQuitConfirm() {
    	QuitHandler.askForQuit(this, new QuitHandlerCallback() {
			@Override
			public void onQuit() {
				uiQuit();
			}
		});
    }

    protected void uiQuit() {
    	shutdown();
    }

    class VirtualInputDispatcher implements VirtualEventDispatcher {

		@Override
		public void sendKey(int keyCode, boolean down) {
			SDLInterface.nativeKey(keyCode, down?1:0);
		}

		@Override
		public void sendMouseButton(MouseButton button, boolean down) {}

		@Override
		public void sendAnalog(Analog index, double x, double y) {}

		@Override
		public boolean handleShortcut(ShortCut shortcut, boolean down) {
			switch(shortcut) {
			case EXIT: if (!down) uiQuitConfirm(); return true;
			case LOAD_STATE: if (!down) uiLoadState(); return true;
			case SAVE_STATE: if (!down) uiSaveState(); return true;
			case MENU : if (!down) openOptionsMenu(); return true;
			default:
				return false;
			}
		}

    }
    
}
