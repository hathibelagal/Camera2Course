package hathibelagal.github.io.mycamera;

import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;

import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImageEmbossFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageGrayscaleFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageHazeFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageSepiaFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageToonFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageView;

public class FilterActivity extends AppCompatActivity {

    private GPUImageView photo;
    private String filename;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

        photo = (GPUImageView)findViewById(R.id.photo);
        filename = getIntent().getStringExtra("FILENAME");

        photo.setImage(new File(filename));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.filter_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.menu_save) {
            saveFile();
        } else {
            final int id = item.getItemId();
            new Thread() {
                @Override
                public void run() {
                    applyFilter(id);
                }
            }.start();
        }
        return true;
    }

    private void applyFilter(int id) {
        switch(id){
            case R.id.filter_emboss:
                photo.setFilter(new GPUImageEmbossFilter());
                break;
            case R.id.filter_grayscale:
                photo.setFilter(new GPUImageGrayscaleFilter());
                break;
            case R.id.filter_haze:
                photo.setFilter(new GPUImageHazeFilter());
                break;
            case R.id.filter_sepia:
                photo.setFilter(new GPUImageSepiaFilter());
                break;
            case R.id.filter_toon:
                photo.setFilter(new GPUImageToonFilter());
                break;
        }
    }

    private void saveFile() {
        String filename = Util.getANewFilename();
        photo.saveToPictures("MyCamera", filename, new GPUImageView.OnPictureSavedListener() {
            @Override
            public void onPictureSaved(Uri uri) {
                Toast.makeText(FilterActivity.this, "Photo saved", Toast.LENGTH_SHORT)
                        .show();
                finish();
            }
        });
    }
}
