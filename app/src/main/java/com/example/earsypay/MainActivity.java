package com.example.earsypay;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.earsypay.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    private ImageView imageView;
    private boolean photoTaken = false;
    private static final int DESIRED_WIDTH = 1920; // Задайте нужные значения
    private static final int DESIRED_HEIGHT = 1080;
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 1001;


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_CAMERA) {
            // Проверяем, было ли разрешение предоставлено
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Разрешение на использование камеры получено, запускаем активность камеры
                dispatchTakePictureIntent();
            } else {
                // Разрешение на использование камеры не было предоставлено, выводим сообщение об ошибке
                Toast.makeText(this, "Для сделки снимка необходимо разрешение на использование камеры", Toast.LENGTH_SHORT).show();
            }
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        Button captureButton = findViewById(R.id.captureButton);
        Button uploadButton = findViewById(R.id.uploadButton);

        // Вызываем камеру при запуске приложения
        dispatchTakePictureIntent();

        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Проверяем, было ли уже сделано фото
                if (!photoTaken) {
                    dispatchTakePictureIntent();
                } else {
                    Toast.makeText(MainActivity.this, "Фото уже сделано", Toast.LENGTH_SHORT).show();
                }
            }
        });

        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadImage();
            }
        });
    }

    private File saveBitmapToFile(Bitmap bitmap) {
        try {
            // Создаем временный файл во внешнем хранилище
            File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "temp_image.jpg");

            // Открываем поток для записи в файл
            FileOutputStream fos = new FileOutputStream(file);

            // Сжимаем изображение и записываем в файл
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);

            // Закрываем поток
            fos.close();

            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CameraHelper.REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            Bitmap originalBitmap = CameraHelper.handleActivityResult(data);
            Bitmap resizedBitmap = resizeBitmap(originalBitmap, DESIRED_WIDTH, DESIRED_HEIGHT);

            // Сохраняем измененное изображение во временный файл
            File tempFile = saveBitmapToFile(resizedBitmap);

            imageView.setImageBitmap(resizedBitmap);
            // Устанавливаем флаг, что фото сделано
            photoTaken = true;

            // Отправляем изображение на сервер
            uploadImageToServer(tempFile);
        }
    }

    private Bitmap resizeBitmap(Bitmap originalBitmap, int desiredWidth, int desiredHeight) {
        // Определение оригинальных размеров изображения
        int width = originalBitmap.getWidth();
        int height = originalBitmap.getHeight();

        // Определение коэффициентов масштабирования для ширины и высоты
        float widthScale = (float) desiredWidth / width;
        float heightScale = (float) desiredHeight / height;

        // Используем меньший коэффициент масштабирования, чтобы сохранить все изображение
        float scaleFactor = Math.min(widthScale, heightScale);

        // Вычисляем новые размеры изображения
        int newWidth = Math.round(scaleFactor * width);
        int newHeight = Math.round(scaleFactor * height);

        // Масштабируем изображение с высоким качеством интерполяции
        return Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true);
    }

    private void dispatchTakePictureIntent() {
        CameraHelper.dispatchTakePictureIntent(MainActivity.this);
    }

    private void uploadImageToServer(File file) {
        if (file != null) {
            // Преобразование файла в Bitmap
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());

            // Вызов метода для загрузки изображения
            uploadImageToServer(bitmap);
        }
    }

    private void uploadImageToServer(Bitmap bitmap) {
        if (bitmap != null) {
            // Преобразование изображения в массив байтов
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
            byte[] imageBytes = byteArrayOutputStream.toByteArray();

            // Создание объекта RequestBody для отправки изображения
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), imageBytes);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", "image.jpg", requestFile);

            // Создаем  клиент OkHttp с установленным таймаутом
            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                    .callTimeout(30, TimeUnit.SECONDS); // Установка таймаута в 30 секунд
            OkHttpClient client = clientBuilder.build();

            // Создание Retrofit-интерфейса
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("https://d7f3-2a00-1fa0-672-2206-4ce5-a32d-877b-478f.ngrok-free.app")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            ApiService apiService = retrofit.create(ApiService.class);

            // Отправка изображения на сервер
            Call<ServerResponse> call = apiService.uploadImage(body);
            call.enqueue(new Callback<ServerResponse>() {
                @Override
                public void onResponse(Call<ServerResponse> call, Response<ServerResponse> response) {
                    if (response.isSuccessful()) {
                        // Получаем текстовое сообщение из JSON-ответа
                        String message = response.body().getMessage();

                        // Ваш код для обработки текстового сообщения
                        Toast.makeText(MainActivity.this, "Сообщение от сервера: " + message, Toast.LENGTH_SHORT).show();

                        // Копируем распознанный текст в буфер обмена
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("Распознанный текст " + response.toString(), message);
                        clipboard.setPrimaryClip(clip);
                    } else {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://apps.rustore.ru/app/ru.sberbankmobile")));
                        Toast.makeText(MainActivity.this, "Ошибка загрузки файла", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ServerResponse> call, Throwable t) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://apps.rustore.ru/app/ru.sberbankmobile")));
                    Toast.makeText(MainActivity.this, "Ошибка загрузки файла1: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

            // Сбрасываем флаг после загрузки изображения
            photoTaken = false;
        }
    }



    private void uploadImage() {
        if (imageView.getDrawable() != null) {
            Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
            uploadImageToServer(bitmap);
        } else {
            Toast.makeText(this, "Сначала сделайте снимок", Toast.LENGTH_SHORT).show();
        }
    }

}