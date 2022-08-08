package com.example.filestoredemo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.nifcloud.mbaas.core.DoneCallback;
import com.nifcloud.mbaas.core.FetchFileCallback;
import com.nifcloud.mbaas.core.NCMB;
import com.nifcloud.mbaas.core.NCMBAcl;
import com.nifcloud.mbaas.core.NCMBException;
import com.nifcloud.mbaas.core.NCMBFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.imageView)
    ImageView imageView;

    public static String APP_KEY = "2bfb444423219ff54256bbe41ff270c5d8c3e81eaa3121c18603363e99b0b673";
    public static String CLIENT_KEY = "2e0167555ae06b73a73a8b2ef1ea9614d566b17cb7c0d191da80797221088bf2";
    Bitmap bitmap = null;
    Uri imageUri;
    int type = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        NCMB.initialize(getApplicationContext(),APP_KEY,CLIENT_KEY);

    }

    private void openCamera(){
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE,"New Pic");
        values.put(MediaStore.Images.Media.DESCRIPTION,"from camera");
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
        startActivityForResult(cameraIntent,1001);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK ){
            imageView.setImageURI(imageUri);

            try {
                UploadImage();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NCMBException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 1000:
                if(grantResults.length>0&& grantResults[0]==
                        PackageManager.PERMISSION_GRANTED){
                    openCamera();
                }
                else{
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
        }
    }

    @OnClick(R.id.post_button)
    void post(){
        if (Build.VERSION.SDK_INT >=Build.VERSION_CODES.M){
            if(checkSelfPermission(Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_DENIED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                            PackageManager.PERMISSION_DENIED){
                String[] permission = {Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE};
                requestPermissions(permission,1000);

            }else{
                openCamera();
            }
        }else{
            openCamera();
        }
    }@OnClick(R.id.get_button)
    void get(){
        try {
            NCMBFile file = new NCMBFile("test_image.png");
            file.fetchInBackground(new FetchFileCallback() {
                @Override
                public void done(byte[] data, NCMBException e) {
                    if (e != null) {
                        Log.d("getError ",e.getMessage());
                        Toast.makeText(MainActivity.this, "Something wrong please try again", Toast.LENGTH_SHORT).show();
                    } else {
                        if(data!=null && data.length>0){
                        Bitmap map = BitmapFactory.decodeByteArray(data,0,data.length);
                        imageView.setImageBitmap(map);
                        }
                    }
                }
            });
        } catch (NCMBException e) {
            e.printStackTrace();
        }

    }
    @OnClick(R.id.delete_button)
    void delete(){
        try {
            NCMBFile file = new NCMBFile("test_image.png");
            file.deleteInBackground(new DoneCallback() {
                @Override
                public void done(NCMBException e) {
                    if (e != null) {
                        Log.d("deleteError", e.getMessage());
                    } else {
                        imageView.setImageResource(R.mipmap.ic_launcher);
                        Toast.makeText(MainActivity.this, "Delete success", Toast.LENGTH_SHORT).show();

                    }
                }
            });
        } catch (NCMBException e) {
            e.printStackTrace();
        }
    }
    private void UploadImage() throws IOException, NCMBException {

            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG,0,byteArrayOutputStream);
            byte[] data = byteArrayOutputStream.toByteArray();

            saveImage(data);



    }
    private void saveImage(byte[] data) throws NCMBException {
        NCMBAcl acl = new NCMBAcl();
        acl.setPublicWriteAccess(true);
        acl.setPublicReadAccess(true);
        NCMBFile file = new NCMBFile("test_image.png", data, acl);
        file.saveInBackground(new DoneCallback() {
            @Override
            public void done(NCMBException e) {
                if (e != null) {
                    Log.d("uploadError",e.getMessage()+e.getCode());
                    if(e.getMessage().equals("java.util.concurrent.ExecutionException: com.nifcloud.mbaas.core.NCMBException: java.io.EOFException")) {
                        try {
                            saveImage(data);
                        } catch (NCMBException ex) {
                            ex.printStackTrace();
                        }
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Post image success", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

}