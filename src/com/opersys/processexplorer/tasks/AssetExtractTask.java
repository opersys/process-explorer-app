/*
* Copyright (C) 2014 Opersys inc.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.opersys.processexplorer.tasks;

import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import java.io.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Author: François-Denis Gonthier (francois-denis.gonthier@opersys.com)
 * Date: 27/03/14
 * Time: 3:30 PM
 */
public abstract class AssetExtractTask extends AsyncTask<AssetExtractTaskParams, Integer, Void> {

    private static final String TAG = "AssetExtractTask";

    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    public static boolean isExtractRequired(AssetExtractTaskParams params) {
        AssetManager assetManager;
        String assetMd5sumPath, assetMd5sum, exMd5sum;
        File exMd5sumFile;
        BufferedReader assetMd5in, exMd5in;

        assetManager = params.assetManager;
        assetMd5sumPath = params.assetMd5sumPath;
        exMd5sumFile = new File(params.extractPath + File.separator + "md5sum");

        try {
            assetMd5in = new BufferedReader(new InputStreamReader(assetManager.open(assetMd5sumPath)));
            exMd5in = new BufferedReader(new FileReader(exMd5sumFile));

            assetMd5sum = assetMd5in.readLine();
            exMd5sum = exMd5in.readLine();

            assetMd5in.close();
            exMd5in.close();

        } catch (IOException e) {
            Log.w(TAG, "Error trying to determine if data extraction is required, assuming it's required", e);

            return true;
        }

        return !(assetMd5sum.trim().equals(exMd5sum.trim()));
    }

    protected void chmod(String mode, String target) {
        Process chmodProcess;
        String[] chmodArr = { null, mode, target }, paths;
        String pathEnv;

        pathEnv = System.getenv("PATH");
        paths = pathEnv.split(":");

        for (String path : paths) {
            File chmodFile = new File(path + "/chmod");

            if (chmodFile.exists() && chmodFile.canExecute()) {
                chmodArr[0] = chmodFile.getAbsolutePath();
                break;
            }
        }

        if (chmodArr[0] == null) {
            Log.e(TAG, "Could not find an executable 'chmod' binary");
            return;
        }

        try {
            chmodProcess = Runtime.getRuntime().exec(chmodArr);
            chmodProcess.waitFor();

        } catch (IOException e) {
            Log.e(TAG, "Failed to chmod " + target + " to " + mode, e);

        } catch (InterruptedException e) {
            Log.w(TAG, "Process.waitFor() interrupted", e);
        }
    }

    protected long copyStream(final InputStream input, final OutputStream output) throws IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int n = 0;
        long count = 0;

        while ((n = input.read(buffer)) != -1) {
            output.write(buffer, 0, n);
            count += n;
        }

        return count;
    }

    @Override
    protected Void doInBackground(AssetExtractTaskParams... params) {
        int totalSize = 0, partialSize = 0;
        File zipFile;
        ZipFile zf;
        InputStream is;
        OutputStream os;
        String archId;
        String assetPath = params[0].assetPath;
        File extractPath = params[0].extractPath;
        AssetManager assetManager = params[0].assetManager;
        ZipEntry zentry = null;

        // Architecture string.
        if (Build.VERSION.SDK_INT > 16)
            archId = "arm_pie";
        else
            archId = "arm";

        zipFile = new File(extractPath + File.separator + assetPath);

        try {
            is = assetManager.open(assetPath);
            os = new FileOutputStream(zipFile);

            copyStream(is, os);

            zf = new ZipFile(zipFile);

            for (Enumeration<? extends ZipEntry> ez = zf.entries(); ez.hasMoreElements();) {
                zentry = ez.nextElement();
                totalSize += zentry.getSize();
            }

            Log.d(TAG, "Total size of entries is: " + totalSize);

            for (Enumeration<? extends ZipEntry> ez = zf.entries(); ez.hasMoreElements();) {
                zentry = ez.nextElement();

                final File outputTarget = new File(extractPath, zentry.getName());

                if (zentry.isDirectory()) {
                    if (zentry.getName().startsWith("_bin"))
                        continue;

                    if (!outputTarget.exists()) {
                        if (!outputTarget.mkdirs()) {
                            String s = String.format("Couldn't create directory %s.", outputTarget.getAbsolutePath());
                            throw new IllegalStateException(s);
                        }
                    }
                } else {
                    final File parentTarget = new File(outputTarget.getParent());

                    // The binaries will be copied in the extraction root based on their name
                    if (zentry.getName().startsWith("_bin")) {
                        if (zentry.getName().endsWith(archId)) {
                            String binPath = zentry.getName().replace("_bin/", "").replace("." + archId, "");
                            File binTarget = new File(extractPath, binPath);
                            OutputStream binFileStream = new FileOutputStream(binTarget);

                            copyStream(zf.getInputStream(zentry), binFileStream);
                        }
                        else continue;

                    } else {
                        // Make the parent directory if it doesn't exists.
                        if (!parentTarget.exists())
                        {
                            if (!parentTarget.mkdirs()) {
                                String s = String.format("Couldn't create directory %s.", parentTarget.toString());
                                throw new IllegalStateException(s);
                            }
                        }

                        OutputStream outputFileStream = new FileOutputStream(outputTarget);
                        copyStream(zf.getInputStream(zentry), outputFileStream);
                        outputFileStream.close();
                    }

                    Log.d(TAG, "Done " + outputTarget.toString() + " (" + zentry.getSize() + ")");

                    partialSize += zentry.getSize();
                    onProgressUpdate((int)(((float)partialSize / (float)totalSize) * 100.0));
                }
            }
        } catch (IOException ex) {
            Log.e(TAG, "Asset decompression error", ex);
        } finally {
            zipFile.delete();
        }

        // FIXME: This is a hack. I need to make up something else eventually.
        chmod("0755", extractPath + File.separator + "node");

        // Copy the md5sum of the asset file to the disk.
        try {
            is = assetManager.open(params[0].assetMd5sumPath);
            os = new FileOutputStream(new File(extractPath + File.separator + "md5sum"));

            copyStream(is, os);

            is.close();
            os.close();

        } catch (IOException e) {
            Log.e(TAG, "Exception with extracting the asset md5sum file", e);
        }

        // No need to return anything since this is a task executed for side effects.
        return null;
    }
}
