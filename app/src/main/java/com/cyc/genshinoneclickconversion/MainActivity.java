package com.cyc.genshinoneclickconversion;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    
    private static final int REQUEST_CODE_FOR_DIR = 666;
    private static final String PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data";
    private TextView msg;
    private Map<String, DocumentFile> documentFileMap = new HashMap<>();
    private DocumentFile yuanShenPack;
    private boolean state;
    private Context context = MainActivity.this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        XXPermissions.with(this)
                .permission(Permission.MANAGE_EXTERNAL_STORAGE)
                .request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(List<String> permissions, boolean all) {
                        if (all) {
                            Toast.makeText(context, "可以正常使用！", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onDenied(List<String> permissions, boolean never) {
                        if (never) {
                            Toast.makeText(context, "被永久拒绝授权，请手动授予再使用此app", Toast.LENGTH_SHORT).show();
                            // 如果是被永久拒绝就跳转到应用权限系统设置页面
                            XXPermissions.startPermissionActivity(context, permissions);
                            startFor(PATH, MainActivity.this, REQUEST_CODE_FOR_DIR);
                        }
                    }
                });
        if (!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager())) {
            findViewById(R.id.delBili).setOnClickListener(view -> deletePack("com.miHoYo.ys.bilibili"));
            findViewById(R.id.delMiHoYo).setOnClickListener(view -> deletePack("com.miHoYo.Yuanshen"));
            msg = findViewById(R.id.msg);
            DocumentFile dir = DocumentFile.fromTreeUri(context, Uri.parse(fileUriUtils.changeToUri3(PATH)));
            DocumentFile[] dirs = dir.listFiles();
            // 将/Android/data/目录下的所有资源包put到documentFileMap
            for (DocumentFile file : dirs) {
                documentFileMap.put(file.getName(), file);
            }
            // 判断是否有两个资源包
            if (documentFileMap.containsKey("com.miHoYo.ys.bilibili") && documentFileMap.containsKey("com.miHoYo.Yuanshen")) {
                msg.setText("有两个渠道资源包，请删除不需要的资源包");
                return;
            } else {
                if (documentFileMap.containsKey("com.miHoYo.ys.bilibili")) {
                    yuanShenPack = documentFileMap.get("com.miHoYo.ys.bilibili");
                    state = true;
                } else if (documentFileMap.containsKey("com.miHoYo.Yuanshen")) {
                    yuanShenPack = documentFileMap.get("com.miHoYo.Yuanshen");
                    state = true;
                }
            }
            msg.setText(getChannel());
        } else {
            startFor(PATH, this, REQUEST_CODE_FOR_DIR);
        }
    }

    private String getChannel() {
        if (yuanShenPack == null) {
            return "没有原神的资源包";
        }
        return String.format("当前渠道：%s服", yuanShenPack.getName().equals("com.miHoYo.ys.bilibili") ? "b" : "官");
    }

    private void deletePack(String key) {
        new AlertDialog.Builder(this)
                .setTitle("系统提示")
                .setMessage(String.format("是否要删除%s服的资源包吗？", key.equals("com.miHoYo.ys.bilibili") ? "b" : "官"))
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (documentFileMap.containsKey(key)) {
                            if (documentFileMap.get(key).delete()) {
                                Toast.makeText(context, "删除成功！", Toast.LENGTH_SHORT).show();
                                documentFileMap.remove(key);
                            } else {
                                Toast.makeText(context, "删除失败！", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(context, "资源包已经不存在！", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    //返回授权状态
    @SuppressLint("WrongConstant")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri uri;

        if (data == null) {
            return;
        }

        if (requestCode == REQUEST_CODE_FOR_DIR && (uri = data.getData()) != null) {
            getContentResolver().takePersistableUriPermission(uri, data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));//关键是这里，这个就是保存这个目录的访问权限

        }

    }


    //获取指定目录的访问权限
    public static void startFor(String path, Activity context, int REQUEST_CODE_FOR_DIR) {
        String uri = fileUriUtils.changeToUri(path);//调用方法，把path转换成可解析的uri文本，这个方法在下面会公布
        Uri parse = Uri.parse(uri);
        Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT_TREE");
        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, parse);
        }
        context.startActivityForResult(intent, REQUEST_CODE_FOR_DIR);//开始授权
    }


    @Override
    public void onClick(View view) {
        if (state) {
            if (yuanShenPack.getName().equals("com.miHoYo.ys.bilibili")) {
                yuanShenPack.renameTo("com.miHoYo.Yuanshen");
            } else if (yuanShenPack.getName().equals("com.miHoYo.Yuanshen")) {
                yuanShenPack.renameTo("com.miHoYo.ys.bilibili");
            } else {
                Toast.makeText(this, "没有可操作的资源包", Toast.LENGTH_SHORT).show();
            }
            msg.setText(getChannel());
        }
    }

}