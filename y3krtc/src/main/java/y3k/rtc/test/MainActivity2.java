package y3k.rtc.test;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.webrtc.DataChannel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import y3k.rtc.test.announcement.DataChannelAnnouncement;
import y3k.rtc.test.channeldescription.FileChannelDescription;
import y3k.rtc.test.channeldescription.FileStreamChannelDescription;
import y3k.rtc.test.channelreader.ByteArrayChannelReader;
import y3k.rtc.test.channelreader.ChannelReader;
import y3k.rtc.test.channelreader.FileChannelReader;
import y3k.rtc.test.channelreader.FileStreamChannelReader;

public class MainActivity2 extends AppCompatActivity {

    Y3kAppRtcRoom y3KAppRtcRoom;

    Y3kAppRtcRoom.CallBack roomCallback = new Y3kAppRtcRoom.CallBack() {
        @Override
        public void onRoomStatusChanged(Y3kAppRtcRoom room, Y3kAppRtcRoom.RoomStatus currentStatus) {

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
            Log.d(MainActivity2.class.getName(), "onProxyMessage("+message+")");
            MainActivity2.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity2.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        (findViewById(R.id.button1)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MainActivity2.this.y3KAppRtcRoom == null) {
                    y3KAppRtcRoom = new Y3kAppRtcRoom(MainActivity2.this, ((EditText) findViewById(R.id.editText)).getText().toString(), MainActivity2.this.roomCallback);
                    findViewById(R.id.editText).setVisibility(View.INVISIBLE);
                } else {
                    y3KAppRtcRoom.sendMessageThroughProxyChannel("BOOOOOO!!");
//                    final File[] files = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).listFiles();
//                    ListView listView = new ListView(MainActivity2.this);
//                    listView.setAdapter(new BaseAdapter() {
//                        @Override
//                        public int getCount() {
//                            return files.length;
//                        }
//
//                        @Override
//                        public Object getItem(int position) {
//                            return files[position];
//                        }
//
//                        @Override
//                        public long getItemId(int position) {
//                            return 0;
//                        }
//
//                        @Override
//                        public View getView(int position, View convertView, ViewGroup parent) {
//                            TextView textView = new TextView(MainActivity2.this);
//                            textView.setText(files[position].getName());
//                            textView.setTag(files[position]);
//                            return textView;
//                        }
//                    });
//                    final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity2.this)
//                            .setView(listView)
//                            .show();
//                    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//                        @Override
//                        public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
//                            alertDialog.dismiss();
//                            try {
//                                MainActivity2.this.y3KAppRtcRoom.openSendFileStreamAnnouncement(new FileInputStream(files[position]),files[position].getName(),"///",files[position].length());
//                            } catch (FileNotFoundException e) {
//                                e.printStackTrace();
//                            }
////                            MainActivity2.this.y3KAppRtcRoom.openSendFileAnnouncement(files[position]);
//                        }
//                    });
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        y3KAppRtcRoom.onDestroy();
        super.onDestroy();
    }
}
