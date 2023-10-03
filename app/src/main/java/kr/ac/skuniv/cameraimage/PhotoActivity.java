package kr.ac.skuniv.cameraimage;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.icu.number.Scale;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import static android.graphics.Bitmap.createBitmap;

import org.tensorflow.lite.Interpreter;

public class PhotoActivity extends AppCompatActivity {
    final private static String TAG = "CLASSIFICATION";
    CameraSurfaceView surfaceView;
    Button detectbtn, photonbtn;
    ImageView imageView;

    public static Context con;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);
        surfaceView = findViewById(R.id.surfaceview);
        imageView = findViewById(R.id.imageview);
        photonbtn = findViewById(R.id.button5);
        detectbtn = findViewById(R.id.button6);

        con = this;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "권한 설정 완료");
            } else {
                Log.d(TAG, "권한 설정 요청");
                ActivityCompat.requestPermissions(PhotoActivity.this, new String[]{Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }

        photonbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                capture();
                detectbtn.setVisibility(imageView.INVISIBLE);
                detectbtn.setVisibility(detectbtn.VISIBLE);
            }
        });

        detectbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                Bitmap bitmap = ((BitmapDrawable)imageView.getDrawable()).getBitmap();
                float scale = (float) (1024/(float)bitmap.getWidth());
                int image_w = (int) (bitmap.getWidth() * scale);
                int image_h = (int) (bitmap.getHeight() * scale);
                Bitmap resize = Bitmap.createScaledBitmap(bitmap, image_w, image_h, true);
                resize.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] byteArray = stream.toByteArray();

                // Intent intent = new Intent(PhotoActivity.this, ResultActivity.class);
                // intent.putExtra("Image")
                // intent.putExtra("image", byteArray);
                // startActivity(intent);

                Bitmap b = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                b.createScaledBitmap(bitmap, 384, 512, false);

                int cx = 384, cy = 512;
                int[] pixels = new int[cx * cy];
                bitmap.getPixels(pixels, 0, cx, 0,0,cx, cy);
                float[][][][] input_img = getInputImage(pixels, cx, cy);

                float[][] result = new float[1][6];

                Interpreter tfLite = getTfliteInterpreter("final_model_CQM.tflite");
                // tfLite.allocateTensors();

                tfLite.run(input_img, result);
                Log.d("predict", Arrays.toString(result[0]));

                float max = result[0][0];
                int maxInx = 0;
                for(int i=1; i < result[0].length; i++){
                    if(max < result[0][i]) {
                        max = result[0][i];
                        maxInx = i;
                    }
                }

                if(maxInx == 0 || maxInx == 3) {
                    // paper , cardboard
                    Intent intent = new Intent(PhotoActivity.this, CardboardPaperActivity.class);
                    startActivity(intent);
                } else if(maxInx == 1) {
                    // glass
                    Intent intent = new Intent(PhotoActivity.this, GlassActivity.class);
                    startActivity(intent);
                } else if(maxInx == 2) {
                    // metal
                    Intent intent = new Intent(PhotoActivity.this, MetalActivity.class);
                    startActivity(intent);
                } else if(maxInx == 4) {
                    // plastic
                    Intent intent = new Intent(PhotoActivity.this, PlasticActivity.class);
                    startActivity(intent);
                } else if(maxInx == 5) {
                    // trash
                    Intent intent = new Intent(PhotoActivity.this, TrashActivity.class);
                    startActivity(intent);
                }
            }

        });

    }

    private float[][][][] getInputImage(int[] pixels, int cx, int cy){
        float [][][][] input_img = new float[1][cx][cy][3];
        int k = 0;
        for(int i = 0; i < cx; i++){
            for(int j = 0; j < cy; j++){
                int pixel = pixels[k++];
                // / (float) 255
                input_img[0][i][j][0] = ((pixel >> 16) & 0xff);
                input_img[0][i][j][1] = ((pixel >> 8) & 0xff);
                input_img[0][i][j][2] = ((pixel >> 0) & 0xff);
            }
        }
        return input_img;
    }

    private Interpreter getTfliteInterpreter(String modelPath) {
        try {
            return new Interpreter(loadModelFile(PhotoActivity.this, modelPath));
        } catch (Exception  e) {
            e.printStackTrace();
        }
        return null;
    }

    private MappedByteBuffer loadModelFile(Activity activity, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public void capture(){
        surfaceView.capture(new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 10;

                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                bitmap = rotateImage(bitmap, 90);
                imageView.setImageBitmap(bitmap);
                camera.startPreview();
            }
        });
    }

    // 이미지 회전에서 출력 되는 것 방지
    public Bitmap rotateImage(Bitmap src, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);

        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
    }

}