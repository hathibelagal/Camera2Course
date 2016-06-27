package hathibelagal.github.io.mycamera;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;

import java.io.File;

import jp.co.cyberagent.android.gpuimage.GPUImage;
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
}
