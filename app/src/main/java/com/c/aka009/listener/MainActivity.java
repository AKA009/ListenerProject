package com.c.aka009.listener;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener
{

    public MyService myService;
    private ListUnitsAdapter _listUnitsAdapter;

    private boolean _isLoop_B_On;
    private boolean _isPause_B_On;
    private boolean _isRandom_B_On;
    private boolean _isFirstTime;

    private int _currentMusicIndex = 0;

    private List<ListUnits> _MLOLU; // Main list of ListUnits

    private ImageButton P_B_list ;
    private ImageButton P_B_pre ;
    private ImageButton P_B_start ;
    private ImageButton P_B_next ;
    private ImageButton P_B_repeat ;
    private ImageButton P_B_close ;

    private ListView P_LV_1;

    private TextView P_TV_currentMusicName;
    private TextView P_TV_currentMusicPlayerName;
    private TextView P_TV_Debug_State;

    private Uri _mediaUriExternal = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    private Uri _mediaUriInternal = MediaStore.Audio.Media.INTERNAL_CONTENT_URI;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        P_B_list    = (ImageButton) findViewById(R.id.P_B_list);
        P_B_pre     = (ImageButton) findViewById(R.id.P_B_pre);
        P_B_start   = (ImageButton) findViewById(R.id.P_B_start);
        P_B_next    = (ImageButton) findViewById(R.id.P_B_next);
        P_B_repeat  = (ImageButton) findViewById(R.id.P_B_repeat);
        P_B_close   = (ImageButton) findViewById(R.id.P_B_close);

        P_TV_currentMusicName       = (TextView) findViewById(R.id.P_TV_currentMusicName);
        P_TV_currentMusicPlayerName = (TextView) findViewById(R.id.P_TV_currentMusicPlayerName);
        P_TV_Debug_State            = (TextView) findViewById(R.id.P_TV_Debug_State);

        P_LV_1 = (ListView) findViewById(R.id.P_LV_1);

        P_B_list.   setOnClickListener(this);
        P_B_pre.    setOnClickListener(this);
        P_B_start.  setOnClickListener(this);
        P_B_next.   setOnClickListener(this);
        P_B_repeat. setOnClickListener(this);
        P_B_close.  setOnClickListener(this);

        _isLoop_B_On    = false;
        _isPause_B_On   = false;
        _isRandom_B_On  = false;
        _isFirstTime    = true;

        _bindServiceConnection();
        _startNewThreadToGetData();

        P_LV_1.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                on_P_LVIC_Jump_To(position);
            }
        });

    }


    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.P_B_list:
                on_P_B_List_Click();
                break;
            case R.id.P_B_pre:
                on_P_B_Pre_Click();
                break;
            case R.id.P_B_start:
                on_P_B_Start_Click();
                break;
            case R.id.P_B_next:
                on_P_B_Next_Click();
                break;
            case R.id.P_B_repeat:
                on_P_B_Repeat_Click();
                break;
            case R.id.P_B_close:
                on_P_B_Close_Click();
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == event.KEYCODE_BACK)
        {
            moveTaskToBack(false);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void _bindServiceConnection()
    {
        Intent intent = new Intent(MainActivity.this,MyService.class);
        startService(intent);
        bindService(intent,_serviceConnection,this.BIND_AUTO_CREATE);
    }

    private ServiceConnection _serviceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            myService = ((MyService.MyBinder)(service)).getService();

            myService.setMyOnNeedSyncListener(new myIOnSyncListener()       //注册自定义接口来接收需要同步的指令
            {
                @Override
                public void NeedSync()
                {
                    _syncUI();
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            myService = null;
        }
    };

    public Handler handler = new Handler();
    public Runnable runnable = new Runnable()
    {
        @Override
        public void run()
        {
            _syncUI();
            handler.post(runnable);
        }
    };



    private void on_P_B_Start_Click()
    {
        if ( _isPause_B_On == false)
        {
            myService.s_start();
            myService.s_setLoop(_isLoop_B_On);
            P_B_start.setImageResource(R.drawable.p_pause);     //切换为暂停键
            _isPause_B_On = true;
        }
        else if (_isPause_B_On == true)     //按钮显示为暂停时
        {
            myService.s_pause();

            P_B_start.setImageResource(R.drawable.p_start);     //切换为播放键
            _isPause_B_On = false;
        }
        _syncUI();
    }

    private void on_P_B_Repeat_Click()
    {
        if (_isLoop_B_On == false)  //如果单曲循环关闭
        {
            P_B_repeat.setImageResource(R.drawable.p_onrepeat);     //切换为单曲循环状态
            _isLoop_B_On = true;
            myService.s_setLoop(_isLoop_B_On);
        }
        else //如果单曲循环开启
        {
            P_B_repeat.setImageResource(R.drawable.p_offrepeat);     //切换为关闭状态
            _isLoop_B_On = false;
            myService.s_setLoop(_isLoop_B_On);
        }
    }

    private void on_P_B_Next_Click()
    {
        myService.s_next();
        _syncUI();
    }

    private void on_P_B_Pre_Click()
    {
        myService.s_pre();
        _syncUI();
    }

    private void on_P_B_List_Click()
    {
        if (_isRandom_B_On)     //如果已经处于随机状态
        {
            _isRandom_B_On = false;
            P_B_list.setImageResource(R.drawable.p_list);//设置为顺序状态
            myService.s_setRandom(_isRandom_B_On);
        }
        else                    //如果处于顺序状态
        {
            _isRandom_B_On = true;
            P_B_list.setImageResource(R.drawable.p_random);//设置为随机状态
            myService.s_setRandom(_isRandom_B_On);
        }
    }

    private void on_P_B_Close_Click()
    {
        myService.s_stop();
        myService.s_quit();

        handler.removeCallbacks(runnable);
        unbindService(_serviceConnection);
        Intent intent = new Intent(MainActivity.this,MyService.class);
        stopService(intent);
        try
        {
            MainActivity.this.finish();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void on_P_LVIC_Jump_To(int index)
    {
        myService.s_jumpTo(index);
        _syncUI();
    }

    private void _startNewThreadToGetData()
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                List<ListUnits> _TLOLU = new ArrayList<>();

                _TLOLU = _getUriData(_mediaUriInternal, _TLOLU);
                _TLOLU = _getUriData(_mediaUriExternal, _TLOLU);

                final List<ListUnits> final_TLOLU = _TLOLU;
                final String s = "Ready";
                //耗时操作，完成之后提交任务更新UI
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        _MLOLU = final_TLOLU;
                        myService.s_setList(final_TLOLU);

                        _listUnitsAdapter = new ListUnitsAdapter(MainActivity.this, R.layout.list_items_layout, _MLOLU);
                        P_LV_1.setAdapter(_listUnitsAdapter);

                        myService.s_initialize();
                        _syncUI();
                        P_TV_Debug_State.setText(s);
                    }
                });
            }
        }).start();
    }

    private List<ListUnits> _getUriData (Uri uri ,List<ListUnits> _TLOLU)
    {
        String[] projection = {"_data", "_display_name", "duration", "artist"};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null && cursor.moveToFirst())
        {
            cursor.moveToFirst();
            do
            {
                ListUnits tempListUnit = new ListUnits();

                tempListUnit.SetData(cursor.getString(cursor.getColumnIndex("_data")));
                tempListUnit.SetDisplay_name(cursor.getString(cursor.getColumnIndex("_display_name")));
                tempListUnit.SetArtist(cursor.getString(cursor.getColumnIndex("artist")));
                tempListUnit.SetDuration(cursor.getString(cursor.getColumnIndex("duration")));

                _TLOLU.add(tempListUnit);
            }
            while (cursor.moveToNext());
            cursor.close();
        }
        return _TLOLU;
    }

    private void _syncUI()
    {
        __syncList(_currentMusicIndex , true);

        _currentMusicIndex = myService.s_getCurrentMusicIndex();
        P_TV_currentMusicName.setText(_MLOLU.get(_currentMusicIndex).GetName());
        P_TV_currentMusicPlayerName.setText(_MLOLU.get(_currentMusicIndex).GetPlayerName()+"+"+_MLOLU.get(_currentMusicIndex).GetType()+"+"+_MLOLU.get(_currentMusicIndex).GetDuration());

        //ConstraintLayout tempConstraintLayout = (ConstraintLayout) P_LV_1.getAdapter().getView(_currentMusicIndex, null, null);
        //TextView tempInItem_L_TV_Name = (TextView) tempConstraintLayout.getChildAt(0);
        //tempInItem_L_TV_Name.setText("Fuck!!!!!!!!!!!!!!!!!");

        __syncList(_currentMusicIndex,false);

        if (myService.s_getIsPlaying())
        {
            P_TV_Debug_State.setText("Playing");
        }
        else
        {
            P_TV_Debug_State.setText("Stop");
        }
    }

    /**
     * 二级内部函数，控制歌曲列表对应项的文字高亮同步
     * @param index 需要修改的项的索引
     * @param isOld 需要修改的项是不是需要去掉高亮
     */
    private void __syncList(int index , boolean isOld)
    {
        ConstraintLayout tempConstraintLayout = (ConstraintLayout) P_LV_1.getAdapter().getView(index, null, null);

        TextView tempInItem_L_TV_Name = (TextView) tempConstraintLayout.getChildAt(0).findViewById(R.id.L_TV_Name);
        TextView tempInItem_L_TV_PlayerName = (TextView) tempConstraintLayout.getChildAt(1).findViewById(R.id.L_TV_PlayerName);

        if (isOld)
        {
            tempInItem_L_TV_Name.setText(_MLOLU.get(index).GetName());
            tempInItem_L_TV_PlayerName.setText(_MLOLU.get(index).GetPlayerName());
        }
        else
        {
            tempInItem_L_TV_Name.setText(_MLOLU.get(index).GetSpannableName(this));
            tempInItem_L_TV_PlayerName.setText(_MLOLU.get(index).GetSpannablePlayerName(this));
        }
    }


}
