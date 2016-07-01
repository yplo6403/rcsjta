/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.ri.extension;

import com.gsma.rcs.ri.R;
import com.gsma.rcs.ri.extension.messaging.InitiateMessagingSession;
import com.gsma.rcs.ri.extension.messaging.MessagingSessionList;
import com.gsma.rcs.ri.extension.streaming.InitiateStreamingSession;
import com.gsma.rcs.ri.extension.streaming.StreamingSessionList;

import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * MM session API
 * 
 * @author Jean-Marc AUFFRET
 */
public class TestMultimediaSessionApi extends ListActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Set items
        String[] items = {
                getString(R.string.menu_initiate_messaging_session),
                getString(R.string.menu_messaging_sessions_list),
                getString(R.string.menu_initiate_streaming_session),
                getString(R.string.menu_streaming_sessions_list),
                getString(R.string.menu_instant_message)

        };
        setListAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items));
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        switch (position) {
            case 0:
                startActivity(new Intent(this, InitiateMessagingSession.class));
                break;

            case 1:
                startActivity(new Intent(this, MessagingSessionList.class));
                break;

            case 2:
                startActivity(new Intent(this, InitiateStreamingSession.class));
                break;

            case 3:
                startActivity(new Intent(this, StreamingSessionList.class));
                break;

            case 4:
                startActivity(new Intent(this, SendInstantMessage.class));
                break;
        }
    }
}
