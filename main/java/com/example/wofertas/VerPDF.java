package com.example.wofertas;

import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class VerPDF extends AppCompatActivity {

    private static final String TAG = "VerPDF_AndroidX";
    private ImageView imageViewPdfPage;
    private Button btnPaginaAnterior, btnProximaPagina;
    private TextView textViewContadorPagina;
    private ProgressBar progressBar;

    private String pdfUrl;
    private File pdfFile;
    private PdfRenderer pdfRenderer;
    private PdfRenderer.Page currentPage;
    private ParcelFileDescriptor parcelFileDescriptor;
    private int currentPageIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ver_pdf);

        Toolbar toolbar = findViewById(R.id.toolbar_ver_pdf);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        imageViewPdfPage = findViewById(R.id.imageViewPdfPage);
        btnPaginaAnterior = findViewById(R.id.btnPaginaAnterior);
        btnProximaPagina = findViewById(R.id.btnProximaPagina);
        textViewContadorPagina = findViewById(R.id.textViewContadorPagina);
        progressBar = findViewById(R.id.progressBarVerPDF);

        btnPaginaAnterior.setOnClickListener(v -> showPage(currentPageIndex - 1));
        btnProximaPagina.setOnClickListener(v -> showPage(currentPageIndex + 1));

        if (getIntent() != null && getIntent().hasExtra("oferta_pdf_url")) {
            pdfUrl = getIntent().getStringExtra("oferta_pdf_url");
            String nomeOferta = getIntent().getStringExtra("oferta_nome");
            if (nomeOferta != null && !nomeOferta.isEmpty() && getSupportActionBar() != null) {
                getSupportActionBar().setTitle(nomeOferta);
            }

            if (pdfUrl != null && !pdfUrl.isEmpty()) {
                downloadAndOpenPdf(pdfUrl);
            } else {
                handleError("URL do PDF inválida.");
            }
        } else {
            handleError("Nenhuma URL de PDF fornecida.");
        }
    }

    private void downloadAndOpenPdf(String urlString) {
        progressBar.setVisibility(ProgressBar.VISIBLE);
        new DownloadPdfTask().execute(urlString);
    }

    private class DownloadPdfTask extends AsyncTask<String, Void, File> {
        @Override
        protected File doInBackground(String... strings) {
            String urlString = strings[0];
            InputStream inputStream = null;
            FileOutputStream fileOutputStream = null;
            HttpURLConnection urlConnection = null;
            File tempFile = null;

            try {
                URL url = new URL(urlString);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.connect();

                if (urlConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "Server returned HTTP " + urlConnection.getResponseCode()
                            + " " + urlConnection.getResponseMessage());
                    return null;
                }

                tempFile = new File(getCacheDir(), "tempPdfToRender.pdf");
                fileOutputStream = new FileOutputStream(tempFile);
                inputStream = urlConnection.getInputStream();

                byte[] buffer = new byte[4096];
                int len1;
                while ((len1 = inputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, len1);
                }
                return tempFile;
            } catch (Exception e) {
                Log.e(TAG, "Erro ao baixar PDF: " + e.getMessage(), e);
                return null;
            } finally {
                try {
                    if (inputStream != null) inputStream.close();
                    if (fileOutputStream != null) fileOutputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Erro ao fechar streams: " + e.getMessage(), e);
                }
                if (urlConnection != null) urlConnection.disconnect();
            }
        }

        @Override
        protected void onPostExecute(File result) {
            if (result != null) {
                pdfFile = result;
                try {
                    openRenderer();
                    showPage(currentPageIndex);
                } catch (IOException e) {
                    progressBar.setVisibility(ProgressBar.GONE);
                    handleError("Erro ao abrir o renderizador de PDF: " + e.getMessage());
                }
            } else {
                progressBar.setVisibility(ProgressBar.GONE);
                handleError("Falha ao baixar o PDF.");
            }
        }
    }

    private void openRenderer() throws IOException {
        if (pdfFile == null || !pdfFile.exists()) {
            throw new IOException("Arquivo PDF não encontrado ou inválido.");
        }
        parcelFileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
        if (parcelFileDescriptor != null) {
            pdfRenderer = new PdfRenderer(parcelFileDescriptor);
        }
    }

    private void closeRenderer() {
        try {
            if (currentPage != null) {
                currentPage.close();
                currentPage = null;
            }
            if (pdfRenderer != null) {
                pdfRenderer.close();
                pdfRenderer = null;
            }
            if (parcelFileDescriptor != null) {
                parcelFileDescriptor.close();
                parcelFileDescriptor = null;
            }
            if (pdfFile != null && pdfFile.exists()) {
                pdfFile.delete();
            }
        } catch (IOException e) {
            Log.e(TAG, "Erro ao fechar renderizador: " + e.getMessage(), e);
        }
    }

    private void showPage(int index) {
        if (pdfRenderer == null || pdfRenderer.getPageCount() <= 0) {
            progressBar.setVisibility(ProgressBar.GONE);
            return;
        }
        if (index < 0 || index >= pdfRenderer.getPageCount()) {
            progressBar.setVisibility(ProgressBar.GONE);
            return;
        }

        if (currentPage != null) {
            currentPage.close();
        }

        currentPage = pdfRenderer.openPage(index);
        currentPageIndex = index;

        int viewWidth = imageViewPdfPage.getWidth();
        int viewHeight = imageViewPdfPage.getHeight();

        if (viewWidth <= 0 || viewHeight <= 0) {
            imageViewPdfPage.post(() -> showPage(currentPageIndex));
            return;
        }

        Bitmap bitmap = Bitmap.createBitmap(
                currentPage.getWidth(), currentPage.getHeight(), Bitmap.Config.ARGB_8888);
        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        imageViewPdfPage.setImageBitmap(bitmap);
        progressBar.setVisibility(ProgressBar.GONE);

        updateUi();
    }

    private void updateUi() {
        if (pdfRenderer == null) return;
        int pageCount = pdfRenderer.getPageCount();
        btnPaginaAnterior.setEnabled(currentPageIndex > 0);
        btnProximaPagina.setEnabled(currentPageIndex < pageCount - 1);
        textViewContadorPagina.setText(String.format(Locale.getDefault(),
                "Página %d / %d", currentPageIndex + 1, pageCount));
    }

    private void handleError(String message) {
        progressBar.setVisibility(ProgressBar.GONE);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.e(TAG, message);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeRenderer();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
