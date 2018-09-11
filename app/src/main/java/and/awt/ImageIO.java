package and.awt;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.InputStream;

public class ImageIO {

	public static BufferedImage read(InputStream byteArrayInputStream) {
		Bitmap bm = BitmapFactory.decodeStream(byteArrayInputStream);
		return bm == null ? null : new BufferedImage(bm);
	}

}
