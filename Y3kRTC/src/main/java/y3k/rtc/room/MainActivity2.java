package y3k.rtc.room;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.appspot.apprtc.Y3kAppRtcRoomParams;
import org.webrtc.DataChannel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import y3k.rtc.room.announcement.DataChannelAnnouncement;
import y3k.rtc.room.channeldescription.FileChannelDescription;
import y3k.rtc.room.channeldescription.FileStreamChannelDescription;
import y3k.rtc.room.channelreader.ByteArrayChannelReader;
import y3k.rtc.room.channelreader.ChannelReader;
import y3k.rtc.room.channelreader.FileChannelReader;
import y3k.rtc.room.channelreader.FileStreamChannelReader;

public class MainActivity2 extends AppCompatActivity {

    Y3kAppRtcRoom y3KAppRtcRoom;

    Y3kAppRtcRoom.CallBack roomCallback = new Y3kAppRtcRoom.CallBack() {
        @Override
        public void onRoomStatusChanged(Y3kAppRtcRoom room, final Y3kAppRtcRoom.RoomStatus currentStatus) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity2.this, "Room New Status : "+currentStatus.name(), Toast.LENGTH_SHORT).show();
                    if(currentStatus == Y3kAppRtcRoom.RoomStatus.DISCONNECTED){
                        MainActivity2.this.finish();
                    }
                }
            });
        }

        @Override
        public void onDataChannelAnnouncement(final DataChannelAnnouncement dataChannelAnnouncement) {
            switch (dataChannelAnnouncement.getType()) {
                case File:
                    MainActivity2.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new AlertDialog.Builder(MainActivity2.this)
                                    .setTitle("File Receiving")
                                    .setMessage("Receive file \"" + ((FileChannelDescription) dataChannelAnnouncement.getChannelDescription()).getFileName() + "\" ?")
                                    .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dataChannelAnnouncement.decline();
                                        }
                                    })
                                    .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dataChannelAnnouncement.accept();
                                        }
                                    }).show();
                        }
                    });
                    break;
                case FileStream:
                    MainActivity2.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new AlertDialog.Builder(MainActivity2.this)
                                    .setTitle("File Receiving")
                                    .setMessage("Receive file \"" + ((FileStreamChannelDescription) dataChannelAnnouncement.getChannelDescription()).getFileName() + "\" ?")
                                    .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dataChannelAnnouncement.decline();
                                        }
                                    })
                                    .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dataChannelAnnouncement.accept();
                                        }
                                    }).show();
                        }
                    });
                    break;
                default:
            }
        }

        @Override
        public FileChannelReader onCreateFileChannelReader(DataChannel dataChannel, final FileChannelDescription fileChannelDescription) {
            Log.d(MainActivity2.class.getName(), "onCreateFileChannelReader()");
            final File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileChannelDescription.getFileName());
            final ProgressDialog[] progressDialog = new ProgressDialog[1];
            MainActivity2.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog[0] = ProgressDialog.show(MainActivity2.this, "Receiving", "0 out of " + fileChannelDescription.getFileLength() + " bytes received...", true, false);
                }
            });
            try {
                if (file.exists()) {
                    file.delete();
                }
                file.createNewFile();
                file.setWritable(true);
                return (FileChannelReader) new FileChannelReader(dataChannel, file).withCallback(new ChannelReader.Callback<File>() {
                    long totalRead = 0;

                    @Override
                    public boolean onReadBytes(long count) {
                        totalRead += count;
                        MainActivity2.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (progressDialog[0] != null) {
                                    progressDialog[0].setMessage(totalRead + " out of " + fileChannelDescription.getFileLength() + " bytes received...");
                                }
                            }
                        });
                        return true;
                    }

                    @Override
                    public void onFinished(final Exception e, final File result) {
                        Log.d("DataChannel.File", result.getName());
                        MainActivity2.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (progressDialog[0] != null) {
                                    progressDialog[0].dismiss();
                                }
                                if (e != null) {
                                    Toast.makeText(MainActivity2.this, "File receive error!!\n" + e.getMessage(), Toast.LENGTH_LONG).show();
                                } else {
                                    new AlertDialog.Builder(MainActivity2.this)
                                            .setTitle("File Received")
                                            .setMessage("You'd like to open " + result.getName() + " ?")
                                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    Intent openFileIntent = new Intent(Intent.ACTION_VIEW);
                                                    openFileIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                    if (result.getName().contains(".") && result.getName().lastIndexOf(".") != result.getName().length()) {
                                                        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.getName().substring(file.getName().lastIndexOf(".") + 1));
                                                        Log.d(MainActivity2.class.getName(), "mimeType = " + mimeType);
                                                        openFileIntent.setDataAndType(Uri.fromFile(result), mimeType);
                                                    } else {
                                                        openFileIntent.setData(Uri.fromFile(result));
                                                    }
                                                    try {
                                                        MainActivity2.this.startActivity(openFileIntent);
                                                    } catch (Exception e1) {
                                                        e1.printStackTrace();
                                                        Toast.makeText(MainActivity2.this, e1.getMessage(), Toast.LENGTH_LONG).show();
                                                    }
                                                }
                                            })
                                            .setNegativeButton("No", null)
                                            .show();
                                }
                            }
                        });
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public FileStreamChannelReader onCreateFileStreamChannelReader(DataChannel channel, final FileStreamChannelDescription channelDescription) {
            Log.d(MainActivity2.class.getName(), "onCreateFileStreamChannelReader()");
            final File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), channelDescription.getFileName());
            final ProgressDialog[] progressDialog = new ProgressDialog[1];
            MainActivity2.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog[0] = ProgressDialog.show(MainActivity2.this, "Receiving", "0 out of " + channelDescription.getFileLength() + " bytes received...", true, false);
                }
            });
            try {
                if (file.exists()) {
                    file.delete();
                }
                file.createNewFile();
                file.setWritable(true);
                return (FileStreamChannelReader) new FileStreamChannelReader(channel, new FileOutputStream(file)).withCallback(new ChannelReader.Callback<OutputStream>() {
                    long totalRead = 0;

                    @Override
                    public boolean onReadBytes(long count) {
                        totalRead += count;
                        MainActivity2.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (progressDialog[0] != null) {
                                    progressDialog[0].setMessage(totalRead + " out of " + channelDescription.getFileLength() + " bytes received...");
                                }
                            }
                        });
                        return true;
                    }

                    @Override
                    public void onFinished(final Exception e, final OutputStream result) {
                        Log.d("DataChannel.File", file.getName());
                        MainActivity2.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (progressDialog[0] != null) {
                                    progressDialog[0].dismiss();
                                }
                                if (e != null) {
                                    Toast.makeText(MainActivity2.this, "File receive error!!\n" + e.getMessage(), Toast.LENGTH_LONG).show();
                                } else {
                                    new AlertDialog.Builder(MainActivity2.this)
                                            .setTitle("File Received")
                                            .setMessage("You'd like to open " + file.getName() + " ?")
                                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    Intent openFileIntent = new Intent(Intent.ACTION_VIEW);
                                                    openFileIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                    if (file.getName().contains(".") && file.getName().lastIndexOf(".") != file.getName().length()) {
                                                        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.getName().substring(file.getName().lastIndexOf(".") + 1));
                                                        Log.d(MainActivity2.class.getName(), "mimeType = " + mimeType);
                                                        openFileIntent.setDataAndType(Uri.fromFile(file), mimeType);
                                                    } else {
                                                        openFileIntent.setData(Uri.fromFile(file));
                                                    }
                                                    try {
                                                        MainActivity2.this.startActivity(openFileIntent);
                                                    } catch (Exception e1) {
                                                        e1.printStackTrace();
                                                        Toast.makeText(MainActivity2.this, e1.getMessage(), Toast.LENGTH_LONG).show();
                                                    }
                                                }
                                            })
                                            .setNegativeButton("No", null)
                                            .show();
                                }
                            }
                        });
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public ByteArrayChannelReader onCreateByteArrayChannelReader(DataChannel channel) {
            return null;
        }

        @Override
        public void onProxyMessage(final String message) {
            Log.d(MainActivity2.class.getName(), "onProxyMessage(" + message + ")");
            MainActivity2.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity2.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    EditText editTextRoomName, editTextMessage;
    Button buttonConnect, buttonSendMessage, buttonSendFile;
    TextView textViewRoomName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.editTextRoomName = (EditText) findViewById(R.id.edittext_roomname);
        this.editTextMessage = (EditText) findViewById(R.id.edittext_message);
        this.buttonConnect = (Button) findViewById(R.id.button_connect_room);
        this.buttonSendMessage = (Button) findViewById(R.id.button_send_message);
        this.buttonSendFile = (Button) findViewById(R.id.button_send_file);
        this.textViewRoomName = (TextView) findViewById(R.id.textview_roomname);
        this.buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String roomName = MainActivity2.this.editTextRoomName.getText().toString();
                if (roomName.length() < 8) {
                    Toast.makeText(MainActivity2.this, "RoomName's length must be longer than 8.", Toast.LENGTH_SHORT).show();
                } else {
                    final CheckBox isOrderedCheckBox = new CheckBox(v.getContext());
                    isOrderedCheckBox.setChecked(Y3kAppRtcRoomParams.isOrdered);
                    final EditText maxRetransmitTimeMsEditText = new EditText(v.getContext());
                    maxRetransmitTimeMsEditText.setText(""+Y3kAppRtcRoomParams.maxRetransmitTimeMs);
                    maxRetransmitTimeMsEditText.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_SIGNED);
                    final EditText maxRetransmitEditText = new EditText(v.getContext());
                    maxRetransmitEditText.setText(""+Y3kAppRtcRoomParams.maxRetransmits);
                    maxRetransmitEditText.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_SIGNED);
                    final EditText protocolEditText = new EditText(v.getContext());
                    protocolEditText.setText(Y3kAppRtcRoomParams.protocol);
                    final CheckBox isNegotiatedCheckBox = new CheckBox(v.getContext());
                    isNegotiatedCheckBox.setChecked(Y3kAppRtcRoomParams.isNegotiated);
                    final EditText channelIdAppRtcDataEditText = new EditText(v.getContext());
                    channelIdAppRtcDataEditText.setText(""+Y3kAppRtcRoomParams.channelIdAppRtcData);
                    channelIdAppRtcDataEditText.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_SIGNED);
                    final EditText channelIdManageEditText = new EditText(v.getContext());
                    channelIdManageEditText.setText(""+Y3kAppRtcRoomParams.channelIdManage);
                    channelIdManageEditText.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_SIGNED);
                    final EditText channelIdMessageProxyEditText = new EditText(v.getContext());
                    channelIdMessageProxyEditText.setText(""+Y3kAppRtcRoomParams.channelIdMessageProxy);
                    channelIdMessageProxyEditText.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_SIGNED);
                    final CheckBox isIosRemoteCheckBox = new CheckBox(v.getContext());
                    isIosRemoteCheckBox.setChecked(Y3kAppRtcRoomParams.isIosRemote);

                    View[] viewArray = {
                            isIosRemoteCheckBox,
                            isOrderedCheckBox,
                            maxRetransmitTimeMsEditText,
                            maxRetransmitEditText,
                            protocolEditText,
                            isNegotiatedCheckBox,
                            channelIdAppRtcDataEditText,
                            channelIdManageEditText,
                            channelIdMessageProxyEditText
                    };

                    for(View view : viewArray){
                        if(view instanceof EditText){
                            ((EditText) view).setSingleLine(true);
                        }
                    }

                    String [] viewIntroArray = {
                            "isIosRemote = ",
                            "isOrdered = ",
                            "maxRetransmitTimeMs = ",
                            "maxRetransmits = ",
                            "protocol = ",
                            "isNegotiated = ",
                            "\"ApprtcDemo data\" ID = ",
                            "\"Manage\" ID = ",
                            "\"MessageProxy\" ID = "
                    };

                    LinearLayout dialogLayout = new LinearLayout(v.getContext());
                    dialogLayout.setOrientation(LinearLayout.VERTICAL);

                    for(int i=0;i<viewIntroArray.length;i++){
                        LinearLayout columnLayout = new LinearLayout(v.getContext());
                        columnLayout.setOrientation(LinearLayout.HORIZONTAL);
                        TextView viewIntroTextView = new TextView(v.getContext());
                        viewIntroTextView.setText(viewIntroArray[i]);
                        columnLayout.addView(viewIntroTextView);
                        columnLayout.addView(viewArray[i]);
                        dialogLayout.addView(columnLayout);
                    }

                    new AlertDialog.Builder(v.getContext())
                            .setTitle("Connect Params")
                            .setView(dialogLayout)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Y3kAppRtcRoomParams.isOrdered = isOrderedCheckBox.isChecked();
                                    Y3kAppRtcRoomParams.maxRetransmitTimeMs = Integer.valueOf(maxRetransmitTimeMsEditText.getText().toString());
                                    Y3kAppRtcRoomParams.maxRetransmits = Integer.valueOf(maxRetransmitEditText.getText().toString());
                                    Y3kAppRtcRoomParams.isNegotiated = isNegotiatedCheckBox.isChecked();
                                    Y3kAppRtcRoomParams.protocol = protocolEditText.getText().toString();
                                    Y3kAppRtcRoomParams.channelIdAppRtcData = Integer.valueOf(channelIdAppRtcDataEditText.getText().toString());
                                    Y3kAppRtcRoomParams.channelIdManage = Integer.valueOf(channelIdManageEditText.getText().toString());
                                    Y3kAppRtcRoomParams.channelIdMessageProxy = Integer.valueOf(channelIdMessageProxyEditText.getText().toString());
                                    Y3kAppRtcRoomParams.isIosRemote = isIosRemoteCheckBox.isChecked();
                                    final ProgressDialog progressDialog = ProgressDialog.show(MainActivity2.this, "AppRTC", "Connecting to room \"" + roomName + "\"", true, true, new DialogInterface.OnCancelListener() {
                                        @Override
                                        public void onCancel(DialogInterface dialog) {
                                            MainActivity2.this.finish();
                                        }
                                    });
                                    MainActivity2.this.y3KAppRtcRoom = new Y3kAppRtcRoom(MainActivity2.this, roomName, Y3kAppRtcRoomParams.isIosRemote, new Y3kAppRtcRoom.CallBack() {
                                        @Override
                                        public void onRoomStatusChanged(final Y3kAppRtcRoom room, Y3kAppRtcRoom.RoomStatus currentStatus) {
                                            if (currentStatus == Y3kAppRtcRoom.RoomStatus.ROOM_CONNECTED) {
                                                y3KAppRtcRoom.setCallback(MainActivity2.this.roomCallback);
                                            }
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    progressDialog.dismiss();
                                                    MainActivity2.this.textViewRoomName.setVisibility(View.VISIBLE);
                                                    MainActivity2.this.textViewRoomName.setText("Room Name = [" + room.getRoomId() + "]");
                                                    MainActivity2.this.editTextRoomName.setVisibility(View.GONE);
                                                    MainActivity2.this.buttonConnect.setVisibility(View.GONE);
                                                    MainActivity2.this.editTextMessage.setVisibility(View.VISIBLE);
                                                    MainActivity2.this.buttonSendMessage.setVisibility(View.VISIBLE);
                                                    MainActivity2.this.buttonSendFile.setVisibility(View.VISIBLE);
                                                }
                                            });
                                        }

                                        @Override
                                        public void onDataChannelAnnouncement(DataChannelAnnouncement dataChannelAnnouncement) {

                                        }

                                        @Override
                                        public FileChannelReader onCreateFileChannelReader(DataChannel channel, FileChannelDescription channelDescription) {
                                            return null;
                                        }

                                        @Override
                                        public FileStreamChannelReader onCreateFileStreamChannelReader(DataChannel channel, FileStreamChannelDescription channelDescription) {
                                            return null;
                                        }

                                        @Override
                                        public ByteArrayChannelReader onCreateByteArrayChannelReader(DataChannel channel) {
                                            return null;
                                        }

                                        @Override
                                        public void onProxyMessage(String message) {

                                        }
                                    });
                                }
                            }).show();
                }
            }
        });
        this.buttonSendFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final File[] files = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).listFiles();
                ListView listView = new ListView(MainActivity2.this);
                listView.setAdapter(new BaseAdapter() {
                    @Override
                    public int getCount() {
                        return files.length;
                    }

                    @Override
                    public Object getItem(int position) {
                        return files[position];
                    }

                    @Override
                    public long getItemId(int position) {
                        return 0;
                    }

                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        TextView textView = new TextView(MainActivity2.this);
                        textView.setText(files[position].getName());
                        textView.setTag(files[position]);
                        return textView;
                    }
                });
                final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity2.this)
                        .setView(listView)
                        .show();
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                        alertDialog.dismiss();
//                        try {
//                            MainActivity2.this.y3KAppRtcRoom.openSendFileStreamAnnouncement(new FileInputStream(files[position]), files[position].getName(), "///", files[position].length());
//                        } catch (FileNotFoundException e) {
//                            e.printStackTrace();
//                        }
                        MainActivity2.this.y3KAppRtcRoom.openSendFileAnnouncement(files[position]);
                    }
                });
            }
        });
        this.buttonSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MainActivity2.this.editTextMessage.getText() != null && !MainActivity2.this.editTextMessage.getText().toString().equals("")) {
                    MainActivity2.this.y3KAppRtcRoom.sendMessageThroughProxyChannel(MainActivity2.this.editTextMessage.getText().toString());
                    MainActivity2.this.editTextMessage.getText().clear();
                }
            }
        });
        if (Build.VERSION.SDK_INT >= 23) {
            this.requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, 666);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                this.finish();
                return;
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onDestroy() {
        if (y3KAppRtcRoom != null) {
            y3KAppRtcRoom.onDestroy();
        }
        super.onDestroy();
    }
}
