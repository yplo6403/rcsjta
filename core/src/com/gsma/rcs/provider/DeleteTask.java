/*
 * Copyright (C) 2015 Sony Mobile Communications Inc.
 * Copyright (C) 2010-2016 Orange.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.gsma.rcs.provider;

import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.DatabaseUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import android.database.Cursor;
import android.net.Uri;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A common delete task for service objects stored in the database. By having information about the
 * scope of the deletion, it will retrieve all ids and delete the associated items, one by one or
 * all at once and finally callback on onCompleted. Will retry execution if the last one has
 * results, and if the scope doesnt expect exactly one.
 */
public abstract class DeleteTask<T> implements Runnable {

    private final Uri mContentUri;

    private final String mSelection;

    private final String[] mSelectionArgs;

    private final String[] mProjection;

    private static final Logger sLogger = Logger.getLogger(DeleteTask.class.getName());

    private final String mColumnGroupBy;

    private final boolean mPathAppended;

    private final boolean mPrimaryKey;

    private final String mColumnKey;

    private boolean mDeleteAllAtOnce;

    protected final LocalContentResolver mLocalContentResolver;

    public abstract static class GroupedByContactId extends DeleteTask<ContactId> {

        private final ContactId mContact;

        /**
         * Delete a specific item or all
         * 
         * @param contentResolver the content resolver
         * @param contentUri the content URI
         * @param columnKey the column of the message ID
         * @param primaryKey true if the message ID is a primary key
         * @param columnGroupBy the column to group (either the contact or chat ID)
         * @param singleItem true to delete a single item
         * @param selection the selection scope executed in the database. If null, all are eligible
         *            for delete or one exactly (in that case selectionArgs must have one value,
         *            unique id of the item).
         * @param selectionArgs the selection arguments of the scope
         */
        public GroupedByContactId(LocalContentResolver contentResolver, Uri contentUri,
                String columnKey, boolean primaryKey, String columnGroupBy, boolean singleItem,
                String selection, String... selectionArgs) {
            super(contentResolver, contentUri, columnKey, primaryKey, columnGroupBy, singleItem,
                    selection, selectionArgs);
            mContact = null;
        }

        /**
         * Delete a conversation
         * 
         * @param contentResolver the content resolver
         * @param contentUri the content URI
         * @param columnKey the primary key column
         * @param primaryKey true if key is primary
         * @param columnContact the contact column
         * @param contact the contact
         */
        public GroupedByContactId(LocalContentResolver contentResolver, Uri contentUri,
                String columnKey, boolean primaryKey, String columnContact, ContactId contact) {
            super(contentResolver, contentUri, columnKey, primaryKey, columnContact, false,
                    columnContact + "=?", contact.toString());
            mContact = contact;
        }

        /**
         * Converts the group id as string to ContactId or return the contact if we are in the
         * unique case.
         */
        @Override
        protected ContactId getGroupAsKey(String groupId) {
            if (mContact != null) {
                return mContact;
            }
            return ContactUtil.createContactIdFromTrustedData(groupId);
        }

    }

    public abstract static class GroupedByChatId extends DeleteTask<String> {

        public GroupedByChatId(LocalContentResolver contentResolver, Uri contentUri,
                String columnPrimaryKey, String columnGroupBy, boolean singleItem,
                String selection, String... selectionArgs) {
            super(contentResolver, contentUri, columnPrimaryKey, true, columnGroupBy, singleItem,
                    selection, selectionArgs);
        }

        @Override
        protected String getGroupAsKey(String groupId) {
            return groupId;
        }

    }

    public abstract static class NotGrouped extends DeleteTask<String> {

        private static final String DEFAULT_KEY = "";

        public NotGrouped(LocalContentResolver contentResolver, Uri contentUri, String columnKey,
                boolean primaryKey, boolean singleItem, String selection, String... selectionArgs) {
            super(contentResolver, contentUri, columnKey, primaryKey, null, singleItem, selection,
                    selectionArgs);
        }

        @Override
        protected void onRowDelete(String groupId, String itemId) throws PayloadException {
            onRowDelete(itemId);
        }

        @Override
        protected void onCompleted(String groupId, Set<String> deletedIds) {
            onCompleted(deletedIds);
        }

        protected abstract void onRowDelete(String itemId) throws PayloadException;

        protected abstract void onCompleted(Set<String> deletedIds);

        @Override
        protected String getGroupAsKey(String groupIdfromDatabase) {
            return DEFAULT_KEY;
        }

    }

    /**
     * This constructor requires the scope of the deletion as a database query selection, and its
     * dependencies.
     *
     * @param contentResolver the local content resolver
     * @param contentUri the content uri (not path appended)
     * @param columnKey the key of the item to delete
     * @param primaryKey True if the key is primary
     * @param columnGroupBy the column by which to group (chat id, contact id) the selection of the
     *            scope or null if it doesnt apply
     * @param selection the selection scope executed in the database. If null, all are eligible for
     *            delete or one exactly (in that case selectionArgs must have one value, unique id
     *            of the item).
     * @param selectionArgs the selection arguments of the scope
     */
    public DeleteTask(LocalContentResolver contentResolver, Uri contentUri, String columnKey,
            boolean primaryKey, String columnGroupBy, boolean singleItem, String selection,
            String... selectionArgs) {
        mLocalContentResolver = contentResolver;
        mPrimaryKey = primaryKey;
        if (!primaryKey) {
            mContentUri = contentUri;
            mPathAppended = false;
            mSelection = selection;
            mSelectionArgs = selectionArgs;
            mColumnGroupBy = columnGroupBy;
            mProjection = new String[] {
                    columnKey, columnGroupBy
            };
            if (singleItem) {
                mDeleteAllAtOnce = true;
            }
            mColumnKey = columnKey;

        } else if (selection == null && selectionArgs != null && selectionArgs.length == 1) {
            mContentUri = contentUri.buildUpon().appendPath(selectionArgs[0]).build();
            mPathAppended = true;
            mSelection = null;
            mSelectionArgs = null;
            mColumnGroupBy = columnGroupBy;
            mProjection = new String[] {
                    columnKey, columnGroupBy
            };
            mDeleteAllAtOnce = true;
            mColumnKey = columnKey;

        } else {
            mContentUri = contentUri;
            mPathAppended = false;
            mSelectionArgs = selectionArgs;
            mSelection = selection;
            mColumnGroupBy = columnGroupBy;
            if (mColumnGroupBy == null) {
                mProjection = new String[] {
                    columnKey
                };
            } else {
                mProjection = new String[] {
                        columnKey, columnGroupBy
                };
            }
            mColumnKey = columnKey;
        }

    }

    /**
     * Queries all the ids of the scope and group them by the "group" column.
     *
     * @return the map of the ids grouped by the keys returned by the group column
     */
    private Map<T, Set<String>> getGroupedItemIds() {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(mContentUri, mProjection, mSelection,
                    mSelectionArgs, null);
            CursorUtil.assertCursorIsNotNull(cursor, mContentUri);
            Map<T, Set<String>> result = null;
            while (cursor.moveToNext()) {
                String key = cursor.getString(0);
                T groupId = null;
                if (mColumnGroupBy != null || !mPrimaryKey) {
                    groupId = getGroupAsKey(cursor.getString(1));
                }
                Set<String> ids = null;
                if (result != null) {
                    ids = result.get(groupId);
                }
                if (ids == null) {
                    ids = new HashSet<>();
                    if (result == null) {
                        result = new HashMap<>();
                    }
                    result.put(groupId, ids);
                }
                ids.add(key);
            }
            return result;

        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Execution can be run several times as incoming items can be deleted.
     *
     * @return the result of the execution as map (deleted ids mapped by group column)
     * @throws PayloadException
     */
    private Map<T, Set<String>> tryDelete() throws PayloadException {
        Map<T, Set<String>> items = getGroupedItemIds();
        if (items == null || items.isEmpty()) {
            return null;
        }
        for (T groupId : items.keySet()) {
            for (String itemKey : items.get(groupId)) {
                if (sLogger.isActivated()) {
                    sLogger.debug("onRowDelete groupId=" + groupId + " item=" + itemKey);
                }
                onRowDelete(groupId, itemKey);
                if (!mDeleteAllAtOnce) {
                    if (mPrimaryKey) {
                        mLocalContentResolver.delete(getAppendedPathUri(itemKey), null, null);
                    } else {
                        String[] newSelectionArgs;
                        String newSelection;
                        if (mSelection == null) {
                            newSelection = mColumnKey + "=? AND " + mColumnGroupBy + "=?";
                            newSelectionArgs = DatabaseUtils.appendIdWithSelectionArgs(itemKey,
                                    new String[] {
                                        groupId.toString()
                                    });
                        } else {
                            newSelectionArgs = DatabaseUtils.appendIdWithSelectionArgs(itemKey,
                                    mSelectionArgs);
                            newSelection = mColumnKey + "=? AND " + mSelection;
                        }
                        mLocalContentResolver.delete(mContentUri, newSelection, newSelectionArgs);
                    }
                }
            }
            if (mDeleteAllAtOnce) {
                mLocalContentResolver.delete(mContentUri, mSelection, mSelectionArgs);
            }
        }
        return items;
    }

    protected boolean isSingleRowDelete() {
        return mPathAppended;
    }

    protected Uri getAppendedPathUri(String itemKey) {
        if (mPathAppended) {
            return mContentUri;
        }
        return mContentUri.buildUpon().appendPath(itemKey).build();
    }

    protected abstract T getGroupAsKey(String groupIdfromDatabase);

    /**
     * @param groupId key of the group
     * @param itemId the item ID
     * @throws PayloadException
     */
    protected abstract void onRowDelete(T groupId, String itemId) throws PayloadException;

    /**
     * Called after the delete is completed to report the ids deleted per group chatId or contact.
     *
     * @param groupId the key by which the ids are grouped by.
     * @param deletedIds as a {@link Set} as required by the listeners.
     */
    protected abstract void onCompleted(T groupId, Set<String> deletedIds);

    /**
     * Set to true if delete on all the scope range at once. False is default. If not set, the task
     * will delete each row one by one.
     *
     * @param deleteAllAtOnce true if delete all at once
     */
    public void setAllAtOnce(boolean deleteAllAtOnce) {
        mDeleteAllAtOnce = deleteAllAtOnce;
    }

    @Override
    public void run() {
        Map<T, Set<String>> deletedIds = null;
        try {
            deletedIds = tryDelete();
            if (deletedIds != null && deletedIds.size() > 0 && !mPathAppended) {
                Map<T, Set<String>> deletedIds2 = tryDelete();
                if (deletedIds2 != null) {
                    for (T groupId : deletedIds.keySet()) {
                        if (deletedIds.containsKey(groupId)) {
                            deletedIds.get(groupId).addAll(deletedIds2.get(groupId));
                        } else {
                            deletedIds.put(groupId, deletedIds2.get(groupId));
                        }
                    }
                }
            }
        } catch (PayloadException | RuntimeException e) {
            sLogger.error("Exception occurred while deleting!", e);

        } finally {
            if (deletedIds != null) {
                for (Map.Entry<T, Set<String>> entry : deletedIds.entrySet()) {
                    T groupId = entry.getKey();
                    Set<String> delIds = entry.getValue();
                    if (sLogger.isActivated()) {
                        if (delIds.size() <= 5) {
                            sLogger.debug("onCompleted groupId=" + groupId + " IDs="
                                    + Arrays.toString(delIds.toArray()));
                        } else {
                            sLogger.debug("onCompleted groupId=" + groupId + " IDs size="
                                    + delIds.size());
                        }
                    }
                    onCompleted(groupId, delIds);
                }
            }
        }
    }
}
