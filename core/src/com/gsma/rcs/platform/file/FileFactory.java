/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications AB.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications AB.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.platform.file;

import com.gsma.rcs.platform.FactoryException;

import android.net.Uri;

import java.io.File;

/**
 * File factory
 * 
 * @author jexa7410
 */
public abstract class FileFactory {
    /**
     * Current platform factory
     */
    private static FileFactory mFactory;

    /**
     * Load the factory
     * 
     * @param classname Factory classname
     * @throws FactoryException
     */
    public static void loadFactory(String classname) throws FactoryException {
        if (mFactory != null) {
            return;
        }
        try {
            mFactory = (FileFactory) Class.forName(classname).newInstance();
        } catch (InstantiationException e) {
            throw new FactoryException("Can't load the factory ".concat(classname), e);

        } catch (IllegalAccessException e) {
            throw new FactoryException("Can't load the factory ".concat(classname), e);

        } catch (ClassNotFoundException e) {
            throw new FactoryException("Can't load the factory ".concat(classname), e);
        }
    }

    /**
     * Returns the current factory
     * 
     * @return Factory
     */
    public static FileFactory getFactory() {
        return mFactory;
    }

    /**
     * Returns the description of a file
     * 
     * @param file URI of the file
     * @return File description
     */
    public abstract FileDescription getFileDescription(Uri file);

    /**
     * Update the media storage
     * 
     * @param url New URL to be added
     */
    public abstract void updateMediaStorage(String url);

    /**
     * Returns whether a file exists or not
     * 
     * @param url Url of the file to check
     * @return File existence
     */
    public abstract boolean fileExists(String url);

    /**
     * Create a directory if not already exist
     * 
     * @param path Directory path
     * @return true if the directory exists or is created
     */
    public static boolean createDirectory(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                return false;
            }
        }
        return true;
    }
}
