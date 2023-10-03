package kr.ac.skuniv.cameraimage;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.tensorflow.lite.Interpreter;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

public class GalleryActivity extends AppCompatActivity {
    private static final int OPEN_GALLERY = 1;
    private ImageView imageView2;
    Button gallerybtn, detectbtn2;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        gallerybtn = findViewById(R.id.button_g);
        imageView2 = findViewById(R.id.gallery_image);
        detectbtn2 = findViewById(R.id.button_d);

        gallerybtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, OPEN_GALLERY);
            }
        });

        detectbtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                Bitmap bitmap = ((BitmapDrawable)imageView2.getDrawable()).getBitmap();
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
                    Intent intent = new Intent(GalleryActivity.this, CardboardPaperActivity.class);
                    startActivity(intent);
                } else if(maxInx == 1) {
                    // glass
                    Intent intent = new Intent(GalleryActivity.this, GlassActivity.class);
                    startActivity(intent);
                } else if(maxInx == 2) {
                    // metal
                    Intent intent = new Intent(GalleryActivity.this, MetalActivity.class);
                    startActivity(intent);
                } else if(maxInx == 4) {
                    // plastic
                    Intent intent = new Intent(GalleryActivity.this, PlasticActivity.class);
                    startActivity(intent);
                } else if(maxInx == 5) {
                    // trash
                    Intent intent = new Intent(GalleryActivity.this, TrashActivity.class);
                    startActivity(intent);
                }
            }

        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OPEN_GALLERY) {
            if (resultCode == RESULT_OK) {
                try {
                    InputStream in = getContentResolver().openInputStream(data.getData());

                    Bitmap img = BitmapFactory.decodeStream(in);
                    in.close();

                    imageView2.setImageBitmap(img);
                } catch (Exception e) {

                }
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "사진 선택 취소", Toast.LENGTH_LONG).show();
            }
        }
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
            return new Interpreter(loadModelFile(GalleryActivity.this, modelPath));
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

    // 이미지 회전에서 출력 되는 것 방지
    public Bitmap rotateImage(Bitmap src, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);

        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
    }
}
