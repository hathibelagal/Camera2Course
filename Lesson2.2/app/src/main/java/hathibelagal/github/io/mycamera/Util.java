package hathibelagal.github.io.mycamera;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Hathibelagal on 17/6/16.
 */
public class Util {
    public static String getANewFilename() {
        SimpleDateFormat dateFormat
                = new SimpleDateFormat("dd-MM-yyyy-HH-mm-ss");
        return "photo_" +
                dateFormat.format(new Date()) +
                ".jpg";
    }
}
