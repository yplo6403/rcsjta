
package com.gsma.rcs.cms.toolkit.xms;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageView;

import com.gsma.rcs.R;

public class XmsImageView extends Activity {

    private final static String EXTRA_IMAGE_URI= "imageUri";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rcs_cms_toolkit_xms_image_view);
        Uri imageUri = Uri.parse(getIntent().getStringExtra(EXTRA_IMAGE_URI));
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            int nh = (int) ( bitmap.getHeight() * (512.0 / bitmap.getWidth()) );
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 512, nh, true);
            ((ImageView) findViewById(R.id.rcs_cms_toolkit_xms_image_view)).setImageBitmap(scaled);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Intent forgeIntentToStart(Context context, Uri imageUri) {
        Intent intent = new Intent(context, XmsImageView.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRA_IMAGE_URI, imageUri.toString());
        return intent;
    }
}
