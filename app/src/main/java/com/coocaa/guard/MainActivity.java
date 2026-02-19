package com.coocaa.guard;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;  // 添加日志导入
import android.view.View;
import android.widget.Button;  // 添加 Button 导入（用于监听器中的类型判断）
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String PREFERENCE_NAME = "SaveSet";
    EditText edit1;
    SharedPreferences sharedPreferences;
    private static final String JsonTest = "[{\"packageName\":\"android\",\"versionCode\":0},{\"packageName\":\"com.ant.store.appstore\",\"versionCode\":0},{\"packageName\":\"com.ucbrowser.tv\",\"versionCode\":789}]";
    private EditText edit_package;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        edit1 = findViewById(R.id.edit1);
        edit_package = findViewById(R.id.edit_package);
        try {
            sharedPreferences = getSharedPreferences(PREFERENCE_NAME, getMode());
            edit1.setText(sharedPreferences.getString("installer_whitelist", JsonTest));

        } catch (Exception e) {
            e.printStackTrace();
        }
        if (getMode() == Context.MODE_PRIVATE) {
            Toast.makeText(this, "当前操作系统版本过高,安装器可能无法读取到本配置文件,SDKINT:" + Build.VERSION.SDK_INT + ",期望的版本是小于24", Toast.LENGTH_SHORT).show();
        }

        // 原有点击监听器
        findViewById(R.id.btn_add).setOnClickListener(this);
        findViewById(R.id.btn_delete).setOnClickListener(this);
        findViewById(R.id.btn_select_app).setOnClickListener(this);
        findViewById(R.id.btn_copy_from_macket).setOnClickListener(this);

        // ========== 新增：焦点监听器用于调试 ==========
        View.OnFocusChangeListener focusListener = (v, hasFocus) -> {
            String viewName = "";
            int id = v.getId();
            if (id == R.id.save) viewName = "保存";
            else if (id == R.id.btn_select_app) viewName = "选择应用";
            else if (id == R.id.btn_add) viewName = "添加";
            else if (id == R.id.btn_delete) viewName = "删除";
            else if (id == R.id.btn_copy_from_macket) viewName = "从系统导入";
            else viewName = "未知控件";

            Log.d("FocusTest", viewName + " 焦点: " + hasFocus);

            // 临时用纯色验证：获得焦点时红色，失去焦点时绿色
            if (hasFocus) {
                v.setBackgroundColor(0xFFFF0000); // 红色
            } else {
                v.setBackgroundColor(0xFF00FF00); // 绿色
            }
        };

        // 为所有按钮设置焦点监听器
        findViewById(R.id.save).setOnFocusChangeListener(focusListener);
        findViewById(R.id.btn_select_app).setOnFocusChangeListener(focusListener);
        findViewById(R.id.btn_add).setOnFocusChangeListener(focusListener);
        findViewById(R.id.btn_delete).setOnFocusChangeListener(focusListener);
        findViewById(R.id.btn_copy_from_macket).setOnFocusChangeListener(focusListener);
        // ========== 新增代码结束 ==========
    }

    private int getMode() {
        return Build.VERSION.SDK_INT >= 24 ? Context.MODE_PRIVATE : Context.MODE_WORLD_READABLE;
    }

    public void Ok(View view) {
        try {
            save(String.valueOf(edit1.getText()));
            Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "！EROOR 保存错误！" + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        JSONArray array = null;
        try {
            String value = sharedPreferences.getString("installer_whitelist", JsonTest);
            array = JSON.parseArray(value);
        } catch (Exception e) {
            e.printStackTrace();
        }
        switch (view.getId()) {
            case R.id.btn_select_app:
                Intent intent = new Intent(this, ChooseAppActivity.class);
                startActivityForResult(intent, 1);
                break;
            case R.id.btn_delete: {
                if (TextUtils.isEmpty(edit_package.getText())) {
                    edit_package.requestFocus();
                    Toast.makeText(this, "请输入包名", Toast.LENGTH_SHORT).show();
                    return;
                }
                for (int i = 0; i < array.size(); i++) {
                    String current = array.getString(i);
                    String findPackage = String.valueOf(edit_package.getText());
                    if (current.contains(findPackage)) {
                        JSONObject jsonObject = JSON.parseObject(current);
                        String packageName = jsonObject.getString("packageName") + "";
                        if (packageName.equals(findPackage)) {
                            array.remove(i);
                            edit1.setText(array.toJSONString());
                            save(edit1.getText().toString());
                            Toast.makeText(this, "删除成功" + current, Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                }
                Toast.makeText(this, "没有找到包名", Toast.LENGTH_SHORT).show();
                break;
            }
            case R.id.btn_add:
                for (int i = 0; i < array.size(); i++) {
                    String current = array.getString(i);
                    String findEdit = String.valueOf(edit_package.getText());
                    if (current.contains(findEdit)) {
                        JSONObject jsonObject = JSON.parseObject(current);
                        String packageName = jsonObject.getString("packageName") + "";
                        if (packageName.equals(findEdit)) {
                            Toast.makeText(this, "已存在!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                }
                if (TextUtils.isEmpty(edit_package.getText())) {
                    edit_package.requestFocus();
                    Toast.makeText(this, "请输入包名", Toast.LENGTH_SHORT).show();
                    return;
                }
                JSONObject obj = new JSONObject();
                obj.put("packageName", edit_package.getText().toString());
                obj.put("versionCode", 0);
                assert array != null;
                array.add(obj);
                edit1.setText(array.toJSONString());
                save(edit1.getText().toString());
                Toast.makeText(this, "添加成功", Toast.LENGTH_LONG).show();
                return;
            case R.id.btn_copy_from_macket:
                Context ctx;
            {
                try {
                    ctx = this.createPackageContext("com.tianci.appstore", CONTEXT_IGNORE_SECURITY);
                } catch (PackageManager.NameNotFoundException e2) {
                    e2.printStackTrace();
                    Toast.makeText(this, "应用市场未安装", Toast.LENGTH_LONG).show();
                    return;
                }
                edit1.setText(ctx.getSharedPreferences(PREFERENCE_NAME, getMode()).getString("installer_whitelist", "Null"));
                Toast.makeText(this, "请点击保存", Toast.LENGTH_LONG).show();
            }
            break;
        }
    }

    private void save(String str) {
        if (TextUtils.isEmpty(str)) {
            str = "[]";
        }
        sharedPreferences.edit()
                .putString("installer_whitelist", str)
                .putString("sdialog_whitelist", str)
                .putString("installer_whitelist_tmp", str)
                .putString("thirdapp_whitelist", str)
                .apply();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            edit_package.setText(data.getStringExtra("packagename"));
        }
    }
}