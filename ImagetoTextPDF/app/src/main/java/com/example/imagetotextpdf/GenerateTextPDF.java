package com.example.imagetotextpdf;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class GenerateTextPDF extends AppCompatActivity {

    static long GENERATE_NUMBER = 00001;
    ImageView show_image;
    TextView textView;
    Button generate_pdf;
    Bitmap bitmap = null;
    int rotationDegrees;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_text_pdf);
        ActivityCompat.requestPermissions(GenerateTextPDF.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PackageManager.PERMISSION_GRANTED);
        generate_pdf = (Button) findViewById(R.id.generate_pdf);
        generate_pdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generatePDF();
            }
        });

        textView = (TextView) findViewById(R.id.textView);
        show_image = findViewById(R.id.show_image);

        Intent intent = getIntent();
        Uri uri = (Uri) intent.getParcelableExtra("imagePath");
        try {
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            rotationDegrees = (int) intent.getIntExtra("rotationDegrees", 0);
            show_image.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
        detectText();
    }

    private void detectText() {
        InputImage image = InputImage.fromBitmap(bitmap, rotationDegrees);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        Task<Text> result = recognizer.process(image).addOnSuccessListener(new OnSuccessListener<Text>() {
            @Override
            public void onSuccess(@NonNull Text text) {
                StringBuilder result = new StringBuilder();
                for (Text.TextBlock block:text.getTextBlocks()) {
                    String blockText = block.getText();
                    Point[] blockCornerPoint = block.getCornerPoints();
                    Rect blockFrame = block.getBoundingBox();
                    for (Text.Line line: block.getLines()) {
                        String lineText = line.getText();
                        Point[] lineCornerPoint = line.getCornerPoints();
                        Rect lineRect = line.getBoundingBox();
                        for (Text.Element element:line.getElements()) {
                            String elementText = element.getText();
                            result.append(elementText).append(" ");
                        }
                        result.append("\n");
                    }
                    result.trimToSize();
                    textView.setText(result);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(GenerateTextPDF.this,"Fail to detect text from image"+e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void generatePDF() {
        PdfDocument myPDFDocument = new PdfDocument();
        PdfDocument.PageInfo myPageInfo = new PdfDocument.PageInfo.Builder(300, 600, 5).create();
        PdfDocument.Page myPage = myPDFDocument.startPage(myPageInfo);

        Paint myPaint = new Paint();
        String myString = textView.getText().toString();
        int x=10, y=25;

        for (String lines:myString.split("\n")) {
            myPage.getCanvas().drawText(lines, x, y, myPaint);
            y+=myPaint.descent()-myPaint.ascent();
        }

        myPDFDocument.finishPage(myPage);

        String myFilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + "/E" + GENERATE_NUMBER + ".pdf";
        GENERATE_NUMBER++;
        File myFile = new File(myFilePath);

        try {
            myPDFDocument.writeTo(new FileOutputStream(myFile));
            Toast.makeText(this,"File is downloaded in download folder",Toast.LENGTH_LONG).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, "ERROR", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
        }

        myPDFDocument.close();
    }
}