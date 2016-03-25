/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.orangelabs.rcs.ri;

import com.orangelabs.rcs.api.connection.utils.RcsActivity;
import com.orangelabs.rcs.api.connection.utils.RcsListActivity;
import com.orangelabs.rcs.ri.messaging.TalkList;
import com.orangelabs.rcs.ri.utils.LogUtils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

/**
 * RI Messaging application
 *
 */
public class RiMessaging extends RcsActivity {

    private static final int PROGRESS_INIT_INCREMENT = 100;

    private Context mContext;

    private ProgressBar mProgressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Set layout */
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.ri_messaging);
        mContext = getApplicationContext();
        mProgressBar = (ProgressBar) findViewById(android.R.id.progress);

        /*
         * The initialization of the connection manager is delayed to avoid non response during
         * application initialization after installation. The application waits until end of
         * connection manager initialization.
         */
        if (!RiApplication.sCnxManagerStarted) {
            new WaitForConnectionManagerStart()
                    .execute(RiApplication.DELAY_FOR_STARTING_CNX_MANAGER);
        } else {
            startActivity(new Intent(this, TalkList.class));
        }
    }

    private class WaitForConnectionManagerStart extends AsyncTask<Long, Integer, Void> {

        @Override
        protected void onProgressUpdate(Integer... progress) {
            mProgressBar.setProgress(progress[0]);
        }

        @Override
        protected Void doInBackground(Long... duration) {
            long delay = (duration[0] / PROGRESS_INIT_INCREMENT);
            for (int i = 0; i < PROGRESS_INIT_INCREMENT; i++) {
                try {
                    Thread.sleep(delay);
                    publishProgress((int) (delay * (i + 1) * 100 / duration[0]));
                    if (RiApplication.sCnxManagerStarted) {
                        break;
                    }
                } catch (InterruptedException ignore) {
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void res) {
            startActivity(new Intent(mContext, TalkList.class));
        }
    }

}
